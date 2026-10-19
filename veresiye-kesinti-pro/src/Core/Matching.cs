using System.Text.RegularExpressions;
namespace Veresiye;
public record Person(string Source, int Row, string Code, string Title, string Name, string Surname, decimal Amount = 0, string Phone = "", string Identity = "")
{
    public string FullName => Name + " " + Surname;
}
public record Decision(Person Person, string Status, Person? Match, string Reason);
public static class Matching
{
    // Deliberately retain rank/title tokens: removing them can conflate different people.
    public static string Key(string text) => Regex.Replace(text.Replace('\u00a0',' ').Replace('İ','i').Replace('I','i').Replace('ı','i').ToLowerInvariant(), @"\s+", " ").Trim();
    public static Decision Find(Person p, IReadOnlyList<Person> pool, bool title = false)
    {
        if (Key(p.Name).Length==0 || Key(p.Surname).Length==0) return new(p,"Kontrol Gerekli",null,"Ad veya soyad eksik");
        if (p.Code.Length > 0)
        {
            var byCode = pool.Where(x => Key(x.Code)==Key(p.Code)).ToList();
            if (byCode.Count>1) return new(p,"Kontrol Gerekli",null,"Mükerrer cari kod");
            if (byCode.Count==1)
            {
                if (Key(p.FullName).Length>0 && Key(byCode[0].FullName).Length>0 && Key(p.FullName)!=Key(byCode[0].FullName))
                    return new(p,"Kontrol Gerekli",null,"Kod ve ad soyad çelişkisi");
                return new(p,"Eşleşti",byCode[0],"Cari kod");
            }
            return new(p,"Eşleşmedi",null,"Cari kod bulunamadı; adla kodun üzerine yazılmadı");
        }
        var key=Key(title?p.Title:p.FullName);
        var candidates=pool.Where(x=>key.Length>0 && Key(title?x.Title:x.FullName)==key).ToList();
        return candidates.Count switch {
            1=>new(p,"Eşleşti",candidates[0],title?"VBA ticari unvan eşitliği":"Tekil ad soyad"),
            0=>new(p,"Eşleşmedi",null,"Kayıt bulunamadı"),
            _=>new(p,"Kontrol Gerekli",null,"Birden fazla aday")};
    }
    public static List<Decision> Run(IReadOnlyList<Person> source, IReadOnlyList<Person> pool, bool title=false)
    {
        var result=source.Select(p=>Find(p,pool,title)).ToList();
        var repeatedTargets=result.Where(d=>d.Match!=null).GroupBy(d=>d.Match!).Where(g=>g.Count()>1).Select(g=>g.Key).ToHashSet();
        for(int i=0;i<result.Count;i++)
        {
            var d=result[i];
            if ((d.Person.Code.Length>0 && source.Count(p=>Key(p.Code)==Key(d.Person.Code))>1) ||
                (d.Match!=null && repeatedTargets.Contains(d.Match)))
                result[i]=d with {Status="Kontrol Gerekli",Match=null,Reason="Mükerrer kaynak kimlik veya aynı hedefe birden çok kayıt"};
        }
        return result;
    }
}
