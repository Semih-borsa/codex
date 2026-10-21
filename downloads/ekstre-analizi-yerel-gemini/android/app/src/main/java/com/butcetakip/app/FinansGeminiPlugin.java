package com.butcetakip.app;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import com.getcapacitor.*;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.security.KeyStore;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONObject;

@CapacitorPlugin(name="FinansGemini")
public class FinansGeminiPlugin extends Plugin {
    private static final String ALIAS="finora_gemini_key_v1";
    private final AtomicBoolean busy=new AtomicBoolean(false);
    private final AtomicReference<HttpsURLConnection> active=new AtomicReference<>();
    private SharedPreferences prefs(){return getContext().getSharedPreferences("finora_secrets_v1",Context.MODE_PRIVATE);}
    private synchronized SecretKey key(boolean create) throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
        if(!create)throw new IllegalStateException("KEY_UNAVAILABLE");
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
        return gen.generateKey();
    }
    private String readKey() throws Exception {
        String value=prefs().getString("encrypted",null);
        if(value==null)throw new IllegalStateException("KEY_MISSING");
        String plain=GeminiKeyCodec.open(key(false),Base64.decode(value,Base64.NO_WRAP));GeminiWire.validateKey(plain);return plain;
    }
    @PluginMethod public void status(PluginCall call){getBridge().execute(()->{
        JSObject r=new JSObject();try{readKey();r.put("configured",true);}catch(Exception e){r.put("configured",false);r.put("unreadable",prefs().contains("encrypted"));}call.resolve(r);
    });}
    @PluginMethod public void saveKey(PluginCall call){String value=call.getString("key");getBridge().execute(()->{
        try{GeminiWire.validateKey(value);String encoded=Base64.encodeToString(GeminiKeyCodec.seal(key(true),value),Base64.NO_WRAP);
            if(!prefs().edit().putString("encrypted",encoded).commit())throw new IllegalStateException();
            if(!value.equals(readKey()))throw new IllegalStateException();
            JSObject r=new JSObject();r.put("configured",true);call.resolve(r);
        }catch(Exception e){call.reject("Anahtar güvenli olarak kaydedilemedi; tekrar dene.","KEY_SAVE_FAILED");}
    });}
    @PluginMethod public void deleteKey(PluginCall call){getBridge().execute(()->{
        try{if(!prefs().edit().remove("encrypted").commit())throw new IllegalStateException();call.resolve();}
        catch(Exception e){call.reject("Anahtar silinemedi; tekrar dene.","KEY_DELETE_FAILED");}
    });}
    @PluginMethod public void cancel(PluginCall call){HttpsURLConnection c=active.get();if(c!=null)c.disconnect();call.resolve();}
    @PluginMethod public void request(PluginCall call){
        String action=call.getString("action"),model=call.getString("model"),body=call.getString("body");
        if(!busy.compareAndSet(false,true)){call.reject("Önceki istek sürüyor.","BUSY");return;}
        getBridge().execute(()->{
            HttpsURLConnection connection=null;
            try{
                String secret;try{secret=readKey();}catch(Exception e){call.reject("Anahtar bulunamadı veya okunamıyor. Ayarlardan yeniden gir.","KEY_MISSING");return;}
                if("generate".equals(action)){
                    if(body==null||body.length()>100000)throw new IllegalArgumentException();
                    JSONObject payload=new JSONObject(body);if(!payload.has("contents"))throw new IllegalArgumentException();
                }
                connection=(HttpsURLConnection)GeminiWire.endpoint(action,model).openConnection();active.set(connection);
                connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(15000);connection.setReadTimeout(35000);
                connection.setRequestProperty("x-goog-api-key",secret);connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
                if("generate".equals(action)){
                    connection.setRequestMethod("POST");connection.setDoOutput(true);byte[] data=body.getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(data.length);
                    try(OutputStream out=connection.getOutputStream()){out.write(data);}
                }else connection.setRequestMethod("GET");
                int status=connection.getResponseCode();
                String text=GeminiWire.read(status>=200&&status<300?connection.getInputStream():connection.getErrorStream());
                JSObject r=new JSObject();r.put("status",status);r.put("body",text);call.resolve(r);
            }catch(SocketTimeoutException e){call.reject("Gemini isteği zaman aşımına uğradı.","TIMEOUT");}
            catch(Exception e){call.reject("Gemini bağlantısı kurulamadı veya yanıt okunamadı.","NETWORK");}
            finally{if(connection!=null){active.compareAndSet(connection,null);connection.disconnect();}busy.set(false);}
        });
    }
}
