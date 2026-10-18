package com.semih.nova;

final class SpeechBoundary {
  private int total, voiced, quiet;
  private boolean heard;

  boolean update(short[] pcm, int n) {
    double power = 0;
    for (int i = 0; i < n; i++) {
      double x = pcm[i] / 32768.0;
      power += x * x;
    }
    double rms = Math.sqrt(power / Math.max(1, n));
    total += n;
    if (rms > .012) {
      voiced += n;
      quiet = 0;
      if (voiced >= 2400) heard = true;
    } else {
      quiet += n;
      if (!heard) voiced = 0;
    }
    return (heard && quiet >= 22400) || (!heard && total >= 128000);
  }

  boolean heard() {
    return heard;
  }
}
