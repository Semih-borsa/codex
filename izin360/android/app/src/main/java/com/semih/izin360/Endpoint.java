package com.semih.izin360;

import java.net.URI;

final class Endpoint {
  static String validate(String text) {
    String value = text.trim().replaceAll("/+$", "");
    if (value.isEmpty()) return "";
    URI uri;
    try {
      uri = URI.create(value);
    } catch (Exception e) {
      throw new IllegalArgumentException("Sunucu adresi geçersiz.");
    }
    String host = uri.getHost();
    if (host == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || !(uri.getPath().isEmpty() || uri.getPath().equals("/"))
        || uri.getPort() > 65535) throw new IllegalArgumentException("Sunucunun kök adresini gir.");
    if ("https".equalsIgnoreCase(uri.getScheme())) return value;
    if (!"http".equalsIgnoreCase(uri.getScheme()) || !local(host))
      throw new IllegalArgumentException(
          "İnternet için HTTPS gerekli. HTTP yalnızca özel yerel ağ adreslerinde kullanılabilir.");
    return value;
  }

  static boolean local(String host) {
    if (host.equals("localhost")) return true;
    String[] parts = host.split("\\.");
    if (parts.length != 4) return false;
    int[] n = new int[4];
    try {
      for (int i = 0; i < 4; i++) {
        if (!parts[i].matches("[0-9]{1,3}")) return false;
        n[i] = Integer.parseInt(parts[i]);
        if (n[i] > 255) return false;
      }
    } catch (Exception e) {
      return false;
    }
    return n[0] == 10
        || n[0] == 127
        || n[0] == 192 && n[1] == 168
        || n[0] == 172 && n[1] >= 16 && n[1] <= 31;
  }
}
