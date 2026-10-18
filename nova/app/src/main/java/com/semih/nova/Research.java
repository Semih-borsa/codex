package com.semih.nova;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;

final class Research {
  static final class Source {
    final String title, text, url;

    Source(String t, String x, String u) {
      title = t;
      text = x;
      url = u;
    }
  }

  interface Fetcher {
    String get(String url) throws Exception;
  }

  static boolean matches(String q) {
    String s = WeatherQuery.normalize(q);
    return s.contains("arastir")
        || s.matches("(?s).*\\b(internette|webde|web'de|wikipedia)\\b.*\\b(ara|bul|bak)\\b.*")
        || s.contains("kaynak bul");
  }

  static String query(String q) {
    String s =
        q.replaceAll(
                "(?iu)\\b(?:internette|webde|web'de|wikipedia'da|wikipediada|bana|lütfen|lutfen)\\b",
                " ")
            .replaceAll(
                "(?iu)\\b(?:araştırır mısın|arastirir misin|araştır|arastir|araştırma yap|arastirma"
                    + " yap|kaynak bul|hakkında kaynak bul)\\b",
                " ")
            .replaceAll("\\s+", " ")
            .trim();
    return Prompt.cut(s, 200);
  }

  static String fetch(String url) throws Exception {
    HttpURLConnection connection = Models.connection(url);
    try (InputStream in = connection.getInputStream()) {
      return new String(Documents.bounded(in, 1024 * 1024), StandardCharsets.UTF_8);
    } finally {
      connection.disconnect();
    }
  }

  static String safeLink(String url) {
    try {
      URI u = new URI(url);
      if (!"https".equalsIgnoreCase(u.getScheme())
          || u.getHost() == null
          || u.getUserInfo() != null) return "";
      String host = u.getHost().toLowerCase(Locale.ROOT);
      if (host.equals("localhost")
          || host.endsWith(".local")
          || !host.contains(".")
          || host.contains(":")
          || host.matches("[0-9.]+")) return "";
      return u.toASCIIString();
    } catch (Exception e) {
      return "";
    }
  }

  static List<Source> parseSearch(String html) {
    Document d = Jsoup.parse(html, "https://html.duckduckgo.com/");
    List<Source> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (Element item : d.select(".result")) {
      Element link = item.selectFirst("a.result__a"),
          snippet = item.selectFirst(".result__snippet");
      if (link == null) continue;
      String url = link.absUrl("href");
      try {
        URI raw = URI.create(url);
        if (raw.getHost() != null
            && raw.getHost().endsWith("duckduckgo.com")
            && raw.getRawQuery() != null) {
          for (String pair : raw.getRawQuery().split("&"))
            if (pair.startsWith("uddg=")) url = URLDecoder.decode(pair.substring(5), "UTF-8");
        }
      } catch (Exception ignored) {
        continue;
      }
      url = safeLink(url);
      if (url.isEmpty() || !seen.add(url)) continue;
      result.add(
          new Source(
              Prompt.cut(link.text(), 150),
              snippet == null ? "Özet sağlanmamış." : Prompt.cut(snippet.text(), 500),
              url));
      if (result.size() == 5) break;
    }
    return result;
  }

  static List<Source> wiki(String q, Fetcher fetcher) throws Exception {
    String base = "https://tr.wikipedia.org/w/api.php?format=json&utf8=1&action=query";
    JSONObject search =
        new JSONObject(
            fetcher.get(base + "&list=search&srlimit=3&srsearch=" + URLEncoder.encode(q, "UTF-8")));
    JSONArray hits = search.getJSONObject("query").getJSONArray("search");
    List<Source> out = new ArrayList<>();
    for (int i = 0; i < Math.min(2, hits.length()); i++) {
      JSONObject hit = hits.getJSONObject(i);
      long id = hit.getLong("pageid");
      if (id <= 0) continue;
      JSONObject data =
          new JSONObject(
              fetcher.get(
                  base + "&prop=extracts&exintro=1&explaintext=1&exchars=1800&pageids=" + id));
      JSONObject article =
          data.getJSONObject("query").getJSONObject("pages").getJSONObject("" + id);
      String extract = article.optString("extract").trim();
      if (!extract.isEmpty())
        out.add(
            new Source(
                article.getString("title"),
                Prompt.cut(extract, 1800),
                "https://tr.wikipedia.org/?curid=" + id));
    }
    return out;
  }

  static String find(String q) throws Exception {
    return find(q, Research::fetch);
  }

  static String find(String q, Fetcher fetcher) throws Exception {
    if (q.trim().length() < 2) throw new IOException("Araştırılacak konuyu açıkça yaz.");
    List<Source> web = Collections.emptyList(), wiki = Collections.emptyList();
    try {
      web =
          parseSearch(
              fetcher.get("https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(q, "UTF-8")));
    } catch (Exception unavailable) {
    }
    try {
      wiki = wiki(q, fetcher);
    } catch (Exception unavailable) {
    }
    if (web.isEmpty() && wiki.isEmpty())
      throw new IOException(
          "Kaynaklara ulaşılamadı veya sonuç bulunamadı. İnterneti kontrol et; istersen tarayıcı"
              + " araması aç. Sonuç uydurmayacağım.");
    StringBuilder out = new StringBuilder("Araştırma: " + q + "\n\n");
    if (!wiki.isEmpty()) {
      out.append("Wikipedia'dan kaynak metni (kısa giriş, alıntı):\n");
      for (Source source : wiki)
        out.append(source.title)
            .append("\n")
            .append(source.text)
            .append("\nKaynak: ")
            .append(source.url)
            .append("\n\n");
    }
    if (!web.isEmpty()) {
      out.append("Web arama sonuçları (arama özetleri; sayfaların tamamı okunmadı):\n");
      for (Source source : web)
        out.append(source.title)
            .append("\n")
            .append(source.text)
            .append("\nKaynak: ")
            .append(source.url)
            .append("\n\n");
    } else
      out.append(
          "Genel web aramasından sonuç alınamadı; yukarıdaki ansiklopedi kaynaklarıyla sınırlı.\n");
    out.append("Sorgu zamanı: ")
        .append(
            java.time.ZonedDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm XXX")))
        .append(
            ". Kaynaklar eksik veya güncel olmayabilir; önemli bilgiyi asıl sayfadan kontrol et."
                + " Wikipedia metni CC BY-SA koşullarına tabidir.");
    return Prompt.cut(out.toString(), 7000);
  }
}
