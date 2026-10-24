package com.semih.portfoyum;

import java.net.URI;
import java.util.Locale;

/** Only the known read-only market endpoints may be requested by the bundled UI. */
public final class NetworkPolicy {
    private NetworkPolicy() {}

    public static URI validate(String raw, String method) {
        URI uri;
        try { uri = URI.create(raw); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Fiyat adresi geçersiz."); }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                || uri.getFragment() != null || (uri.getPort() != -1 && uri.getPort() != 443)) {
            throw new IllegalArgumentException("Yalnızca izin verilen HTTPS fiyat adresleri kullanılabilir.");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String query = uri.getRawQuery();
        boolean allowed = false;
        if (host.equals("www.tefas.gov.tr")) {
            boolean detail = "GET".equals(method) && query == null
                    && path.matches("/tr/fon-detayli-analiz/[A-Z0-9]{2,6}/?");
            boolean portal = "GET".equals(method) && path.matches("/tr/fon-verileri/?")
                    && (query == null || query.matches("fundType=(YAT|EMK|BYF)&startDate=\\d{4}-\\d{2}-\\d{2}&endDate=\\d{4}-\\d{2}-\\d{2}"));
            boolean api = "POST".equals(method) && query == null
                    && path.equals("/api/funds/fonGnlBlgSiraliGetir");
            allowed = detail || portal || api;
        } else if (host.equals("altin.doviz.com")) {
            allowed = "GET".equals(method) && query == null && (path.matches("/(gram-altin|gumus|ceyrek-altin|yarim-altin|tam-altin|cumhuriyet-altini|resat-altini|22-ayar-bilezik|18-ayar-altin)/?")
                    || path.matches("/(akbank|albaraka-turk|alternatif-bank|anadolubank|denizbank|dunya-katilim|emlak-katilim|enpara|fibabanka|garanti-bbva|halkbank|hsbc|ing|isbankasi|kuveyt-turk|odeabank|qnb|sekerbank|teb|hadi|turkiye-finans|vakifbank|vakif-katilim|yapi-kredi|ziraat-bankasi|ziraat-katilim|serbest-piyasa)/(gram-altin|gumus)/?"));
        } else if (host.equals("m.doviz.com")) {
            allowed = "GET".equals(method) && query == null && path.matches("/altin/(banka|gumus)/?");
        } else if (host.equals("altin.app")) {
            allowed = "GET".equals(method) && query == null && path.equals("/");
        } else if (host.equals("canlialtinfiyatlari.com")) {
            allowed = "GET".equals(method) && query == null && path.matches("/altin/banka/?");
        } else if (host.equals("borsa.doviz.com")) {
            allowed = "GET".equals(method) && query == null
                    && path.matches("/hisseler(?:/altins1-darphane-altin-sertifikasi)?/?");
        } else if (host.equals("m.netdania.com")) {
            allowed = "GET".equals(method) && query == null
                    && path.matches("/equities/altin-s1-ib/idc-dla/?");
        } else if (host.equals("finans.truncgil.com")) {
            allowed = path.equals("/today.json") && "GET".equals(method) && query == null;
        } else if (host.equals("query1.finance.yahoo.com")) {
            allowed = path.matches("/v8/finance/chart/[A-Z0-9]{2,12}\\.IS") && "GET".equals(method)
                    && "interval=1d&range=5d".equals(query);
        }
        if (!allowed) throw new IllegalArgumentException("Bu adres fiyat kaynakları arasında değil.");
        return uri;
    }

    public static String safeFileName(String input) {
        String name = input == null ? "Portfoyum-yedek.json" : input;
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
        if (name.trim().isEmpty()) name = "Portfoyum-yedek.json";
        return name.substring(0, Math.min(120, name.length()));
    }
}
