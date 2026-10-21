package com.butcetakip.app;
import org.junit.Test;
import static org.junit.Assert.*;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.*;
import java.util.Arrays;
public class GeminiSafetyTest {
 private SecretKey key()throws Exception{KeyGenerator g=KeyGenerator.getInstance("AES");g.init(256);return g.generateKey();}
 @Test public void encryptRoundTrip()throws Exception{SecretKey k=key();String s="synthetic_key_12345678901234567890";byte[] sealed=GeminiKeyCodec.seal(k,s);assertEquals(s,GeminiKeyCodec.open(k,sealed));assertFalse(new String(sealed,java.nio.charset.StandardCharsets.UTF_8).contains(s));}
 @Test public void randomIvPerSave()throws Exception{SecretKey k=key();assertFalse(Arrays.equals(GeminiKeyCodec.seal(k,"same"),GeminiKeyCodec.seal(k,"same")));}
 @Test public void tamperingRejected()throws Exception{SecretKey k=key();byte[] b=GeminiKeyCodec.seal(k,"synthetic-key");b[b.length-1]^=1;try{GeminiKeyCodec.open(k,b);fail();}catch(java.security.GeneralSecurityException expected){}}
 @Test public void wrongKeyRejected()throws Exception{byte[] b=GeminiKeyCodec.seal(key(),"synthetic-key");try{GeminiKeyCodec.open(key(),b);fail();}catch(java.security.GeneralSecurityException expected){}}
 @Test public void shortCipherRejected()throws Exception{try{GeminiKeyCodec.open(key(),new byte[12]);fail();}catch(java.security.GeneralSecurityException expected){}}
 @Test public void fixedGoogleHost()throws Exception{assertEquals("generativelanguage.googleapis.com",GeminiWire.endpoint("generate","gemini-test").getHost());assertEquals("https",GeminiWire.endpoint("models",null).getProtocol());assertFalse(GeminiWire.endpoint("models",null).toString().contains("key="));}
 @Test public void invalidModelCannotInjectUrl()throws Exception{for(String m:new String[]{"https://evil.invalid","gemini-x/../x","gemini-x?key=a"}){try{GeminiWire.endpoint("generate",m);fail();}catch(IOException expected){}}}
 @Test public void invalidActionRejected()throws Exception{try{GeminiWire.endpoint("other","gemini-x");fail();}catch(IOException expected){}}
 @Test public void responseBounded()throws Exception{try{GeminiWire.read(new ByteArrayInputStream(new byte[262145]));fail();}catch(IOException expected){}}
 @Test public void keyValidation() {for(String s:new String[]{"", "secret with space 1234567890", "https://invalid.example/key"}){try{GeminiWire.validateKey(s);fail();}catch(IllegalArgumentException expected){}}GeminiWire.validateKey("synthetic_key_12345678901234567890");}
}
