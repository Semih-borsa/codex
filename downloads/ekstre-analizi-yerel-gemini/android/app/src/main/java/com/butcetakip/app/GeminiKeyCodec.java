package com.butcetakip.app;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class GeminiKeyCodec {
    static byte[] seal(SecretKey key, String text) throws GeneralSecurityException {
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key);
        byte[] iv=c.getIV(), data=c.doFinal(text.getBytes(StandardCharsets.UTF_8));
        if(iv.length!=12)throw new GeneralSecurityException("IV");
        byte[] all=new byte[iv.length+data.length];System.arraycopy(iv,0,all,0,iv.length);System.arraycopy(data,0,all,iv.length,data.length);return all;
    }
    static String open(SecretKey key, byte[] all) throws GeneralSecurityException {
        if(all==null||all.length<29)throw new GeneralSecurityException("Ciphertext");
        byte[] iv=new byte[12];System.arraycopy(all,0,iv,0,12);
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
        return new String(c.doFinal(all,12,all.length-12),StandardCharsets.UTF_8);
    }
}
