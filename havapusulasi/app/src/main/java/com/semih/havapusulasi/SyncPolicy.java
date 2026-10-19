package com.semih.havapusulasi;

final class SyncPolicy {
  static int minutes(int requested) {
    return requested == 30 || requested == 60 || requested == 180 ? requested : 30;
  }

  static long interval(int requested) {
    return minutes(requested) * 60_000L;
  }

  static long flex(int requested) {
    return Math.min(30 * 60_000L, interval(requested) / 6);
  }
}
