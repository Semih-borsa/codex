package com.butcetakip.app;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BackupDocumentIOTest {
    private final byte[] json = "{\"format\":\"finora-v2\",\"data\":{\"Türkçe\":\"₺ öykü\"}}".getBytes(StandardCharsets.UTF_8);
    @Test public void writesRealFileAndReadsIdenticalUtf8() throws Exception {
        File file = File.createTempFile("finans-backup-", ".json");
        try {
            long size = BackupDocumentIO.writeAndVerify(json, () -> new FileOutputStream(file), () -> new FileInputStream(file));
            assertEquals(json.length, size); assertArrayEquals(json, Files.readAllBytes(file.toPath()));
        } finally { file.delete(); }
    }
    @Test public void openFailureIsNotSuccess() throws Exception {
        try { BackupDocumentIO.writeAndVerify(json, () -> { throw new IOException("permission"); }, () -> new ByteArrayInputStream(json)); fail(); } catch(IOException expected) {}
    }
    @Test public void nullOutputIsNotSuccess() throws Exception {
        try { BackupDocumentIO.writeAndVerify(json, () -> null, () -> new ByteArrayInputStream(json)); fail(); } catch(IOException expected) {}
    }
    @Test public void shortReadIsNotSuccess() throws Exception {
        try { BackupDocumentIO.writeAndVerify(json, ByteArrayOutputStream::new, () -> new ByteArrayInputStream(new byte[1])); fail(); } catch(IOException expected) {}
    }
    @Test public void sameLengthWrongBytesAreNotSuccess() throws Exception {
        byte[] wrong=json.clone(); wrong[0]='!';
        try { BackupDocumentIO.writeAndVerify(json, ByteArrayOutputStream::new, () -> new ByteArrayInputStream(wrong)); fail(); } catch(IOException expected) {}
    }
    @Test public void extraBytesAreNotSuccess() throws Exception {
        try { BackupDocumentIO.writeAndVerify(json, ByteArrayOutputStream::new, () -> new ByteArrayInputStream(new byte[json.length+1])); fail(); } catch(IOException expected) {}
    }
    @Test public void closeFailureIsNotSuccess() throws Exception {
        try { BackupDocumentIO.writeAndVerify(json, () -> new ByteArrayOutputStream(){@Override public void close() throws IOException {throw new IOException("close");}}, () -> new ByteArrayInputStream(json)); fail(); } catch(IOException expected) {}
    }
    @Test public void readPermissionFailureIsNotSuccess() throws Exception {
        try { BackupDocumentIO.writeAndVerify(json, ByteArrayOutputStream::new, () -> {throw new IOException("read denied");}); fail(); } catch(IOException expected) {}
    }
    @Test public void midWriteFailureClosesStream() throws Exception {
        boolean[] closed={false};
        try { BackupDocumentIO.writeAndVerify(json, () -> new OutputStream(){public void write(int b)throws IOException{throw new IOException("full");}public void close(){closed[0]=true;}}, () -> new ByteArrayInputStream(json)); fail(); }catch(IOException expected){}
        assertTrue(closed[0]);
    }
    @Test public void emptyPayloadCannotBecomeSuccessfulBackup() throws Exception {
        try{BackupDocumentIO.writeAndVerify(new byte[0],ByteArrayOutputStream::new,()->new ByteArrayInputStream(new byte[0]));fail();}catch(IOException expected){}
    }
}
