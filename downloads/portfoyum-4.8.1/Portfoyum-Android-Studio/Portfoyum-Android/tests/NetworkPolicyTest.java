import com.semih.portfoyum.NetworkPolicy;

public final class NetworkPolicyTest {
    private static int checks;
    private static void accepted(String url, String method) {
        NetworkPolicy.validate(url, method); checks++;
    }
    private static void rejected(String url, String method) {
        try { NetworkPolicy.validate(url, method); }
        catch (IllegalArgumentException expected) { checks++; return; }
        throw new AssertionError("Unsafe URL accepted: " + url);
    }
    public static void main(String[] args) {
        accepted("https://www.tefas.gov.tr/tr/fon-detayli-analiz/THF", "GET");
        accepted("https://www.tefas.gov.tr/tr/fon-detayli-analiz/THF/", "GET");
        accepted("https://www.tefas.gov.tr/tr/fon-verileri?fundType=YAT&startDate=2026-08-20&endDate=2026-09-01", "GET");
        accepted("https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir", "POST");
        accepted("https://altin.doviz.com/gram-altin", "GET");
        accepted("https://altin.doviz.com/gumus", "GET");
        accepted("https://altin.doviz.com/ceyrek-altin", "GET");
        accepted("https://altin.doviz.com/yarim-altin", "GET");
        accepted("https://altin.doviz.com/tam-altin", "GET");
        accepted("https://altin.doviz.com/cumhuriyet-altini", "GET");
        accepted("https://altin.doviz.com/resat-altini", "GET");
        accepted("https://altin.doviz.com/22-ayar-bilezik", "GET");
        accepted("https://altin.doviz.com/18-ayar-altin", "GET");
        accepted("https://altin.doviz.com/ziraat-bankasi/gram-altin", "GET");
        accepted("https://altin.doviz.com/ziraat-bankasi/gumus/", "GET");
        accepted("https://altin.doviz.com/akbank/gram-altin", "GET");
        accepted("https://altin.doviz.com/isbankasi/gram-altin", "GET");
        accepted("https://altin.doviz.com/garanti-bbva/gumus", "GET");
        accepted("https://altin.doviz.com/serbest-piyasa/gram-altin", "GET");
        accepted("https://altin.doviz.com/serbest-piyasa/gumus", "GET");
        accepted("https://m.doviz.com/altin/banka", "GET");
        accepted("https://m.doviz.com/altin/gumus/", "GET");
        accepted("https://altin.app/", "GET");
        accepted("https://canlialtinfiyatlari.com/altin/banka", "GET");
        accepted("https://borsa.doviz.com/hisseler/altins1-darphane-altin-sertifikasi", "GET");
        accepted("https://borsa.doviz.com/hisseler", "GET");
        accepted("https://m.netdania.com/equities/altin-s1-ib/idc-dla", "GET");
        accepted("https://finans.truncgil.com/today.json", "GET");
        accepted("https://query1.finance.yahoo.com/v8/finance/chart/THYAO.IS?interval=1d&range=5d", "GET");
        rejected("http://www.tefas.gov.tr/FonAnaliz.aspx?FonKod=THF", "GET");
        rejected("https://www.tefas.gov.tr.evil.test/FonAnaliz.aspx?FonKod=THF", "GET");
        rejected("https://www.tefas.gov.tr@evil.test/FonAnaliz.aspx?FonKod=THF", "GET");
        rejected("https://127.0.0.1/today.json", "GET");
        rejected("file:///etc/passwd", "GET");
        rejected("https://finans.truncgil.com:8443/today.json", "GET");
        rejected("https://altin.doviz.com/gram-altin", "POST");
        rejected("https://altin.doviz.com/%2e%2e/secret", "GET");
        rejected("https://altin.doviz.com/gram-altin?redirect=evil", "GET");
        rejected("https://www.tefas.gov.tr/api/DB/BindHistoryInfo", "POST");
        rejected("https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir", "GET");
        rejected("https://www.tefas.gov.tr/tr/fon-verileri?fundType=YAT&startDate=2026-08-20&endDate=2026-09-01&x=1", "GET");
        rejected("https://www.tefas.gov.tr/FonAnaliz.aspx?FonKod=THF", "GET");
        rejected("https://www.tefas.gov.tr/FonAnaliz.aspx?FonKod=THF&x=1", "GET");
        rejected("https://www.tefas.gov.tr/tr/fon-detayli-analiz/THF%2Fbad", "GET");
        rejected("https://m.doviz.com/altin/bitcoin", "GET");
        rejected("https://altin.app/?target=other", "GET");
        rejected("https://canlialtinfiyatlari.com/altin/gumus", "GET");
        rejected("https://borsa.doviz.com/hisseler/THYAO", "GET");
        rejected("https://m.netdania.com/equities/another/idc-dla", "GET");
        if (NetworkPolicy.safeFileName("../../test.json").contains("/")) throw new AssertionError("Unsafe name");
        checks++;
        System.out.println(checks + " native network/file policy checks passed.");
    }
}
