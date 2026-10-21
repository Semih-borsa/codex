package com.butcetakip.app;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.net.URL;
final class GeminiWire {
    static void validateKey(String key) {
        if(key==null||!key.matches("[A-Za-z0-9_-]{20,256}"))throw new IllegalArgumentException("INVALID_KEY");
    }
    static URL endpoint(String action,String model) throws IOException {
        if("models".equals(action))return new URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000");
        if(!"generate".equals(action)||model==null||!model.matches("gemini-[a-z0-9.-]{1,80}"))throw new IOException("INVALID_MODEL");
        return new URL("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent");
    }
    static String read(InputStream in) throws IOException {
        if(in==null)return "";
        try(InputStream stream=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] bytes=new byte[4096];int n;
            while((n=stream.read(bytes))!=-1){if(out.size()+n>262144)throw new IOException("RESPONSE_TOO_LARGE");out.write(bytes,0,n);}
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}
