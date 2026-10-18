package com.semih.nova;

final class NativeBridge {
  static {
    System.loadLibrary("nova");
  }

  interface Callback {
    void onBytes(byte[] text);
  }

  static native byte[] generate(String path, byte[] prompt, Callback callback);

  static native byte[] transcribe(String path, float[] audio);

  static native byte[] modelInfo();

  static native void reset();

  static native void stop();

  static native void release();
}
