using ClosedXML.Excel;
namespace Veresiye;
public sealed class WorkbookService
{
    public string Path {get;}
    public List<Person> Payroll {get;}=[];
    public List<Person> Deductions {get;}=[];
    public List<Person> Personnel {get;}=[];
    public List<Person> Contacts {get;}=[];
    public List<Decision> Results {get;private set;}=[];
    public List<Decision> Bank {get;private set;}=[];
    public List<Decision> ContactResults {get;private set;}=[];
    public List<Decision> Issues {get;private set;}=[];
    static string Text(IXLWorksheet s,int r,int c)=>s.Cell(r,c).GetFormattedString().Trim();
    static decimal Money(IXLWorksheet s,int r,int c)
    {
        var cell=s.Cell(r,c);
        if(cell.TryGetValue<decimal>(out var v)) return v;
        if(decimal.TryParse(cell.GetString(),System.Globalization.NumberStyles.Number,System.Globalization.CultureInfo.GetCultureInfo("tr-TR"),out v)) return v;
        throw new InvalidDataException($"{s.Name}, satır {r}: tutar geçersiz veya boş.");
    }
    public WorkbookService(string path)
    {
        Path=System.IO.Path.GetFullPath(path);
        using var w=new XLWorkbook(path);
        foreach(var name in new[]{"Sayfa1","Kesinti Listesi","Personel Listesi","İrtibat","İş Bankası Formatı","Mutemet Format"})
            if(!w.TryGetWorksheet(name,out _)) throw new InvalidDataException($"Gerekli sayfa eksik: {name}");
        var payroll=w.Worksheet("Sayfa1");
        int payrollCode=Matching.Key(Text(payroll,1,4)) is "cari kodu" or "cari kod" or "sicil" ? 4 : 0;
        Read(payroll,Payroll,2,1,2,3,payrollCode);
        Read(w.Worksheet("Kesinti Listesi"),Deductions,2,4,5,6,2,3);
        Read(w.Worksheet("İrtibat"),Contacts,2,4,5,0,2,3,6);
        // Actual workbook has no personnel header. B is VBA join key, C/D name, E TC.
        Read(w.Worksheet("Personel Listesi"),Personnel,1,3,4,0,0,2,0,5);
    }
    static void Read(IXLWorksheet s,List<Person> list,int start,int name,int surname,int amount,int code=0,int title=0,int phone=0,int identity=0)
    {
        int end=s.LastRowUsed()?.RowNumber()??0;
        if(end>100000) throw new InvalidDataException("En fazla 100.000 satır destekleniyor.");
        for(int r=start;r<=end;r++)
        {
            string Get(int c)=>c==0?"":Text(s,r,c);
            if(new[]{name,surname,code,title,amount,phone,identity}.Where(c=>c>0).All(c=>s.Cell(r,c).IsEmpty())) continue;
            list.Add(new(s.Name,r,Get(code),Get(title),Get(name),Get(surname),amount>0?Money(s,r,amount):0,Get(phone),Get(identity)));
        }
    }
    public void Process()
    {
        Results=Matching.Run(Payroll,Deductions);
        Results=Results.Select(d=>d.Match!=null && d.Person.Amount!=d.Match.Amount ? d with {Status="Kontrol Gerekli",Match=null,Reason="Sayfa1 tutarı ile kesinti bakiyesi farklı; eksik rapor makrosu nedeniyle otomatik yorumlanmadı"} : d).ToList();
        // V3 bank join uses Title rather than Code: personnel B is not a documented cari code.
        Bank=Matching.Run(Deductions.Select(p=>p with {Code=""}).ToList(),Personnel,true);
        ContactResults=Matching.Run(Results.Where(d=>d.Status!="Eşleşti").Select(d=>d.Person).ToList(),Contacts);
        Issues=Results.Concat(Bank).Concat(ContactResults).Where(d=>d.Status!="Eşleşti").ToList();
        foreach(var list in new[]{Payroll,Deductions,Contacts,Personnel})
            foreach(var group in list.Where(p=>p.Code.Length>0).GroupBy(p=>Matching.Key(p.Code)).Where(g=>g.Count()>1))
                Issues.AddRange(group.Select(p=>new Decision(p,"Kontrol Gerekli",null,"Mükerrer cari kod")));
    }
    public void Export(string destination)
    {
        if(System.IO.Path.GetFullPath(destination).Equals(Path,StringComparison.OrdinalIgnoreCase) || File.Exists(destination))
            throw new IOException("Orijinal dosyanın veya mevcut bir dosyanın üzerine yazılamaz. Yeni dosya adı seçin.");
        using var w=new XLWorkbook(Path);
        Sheet(w,"Kesinti Yapılanlar",Results.Where(d=>d.Status=="Eşleşti").ToList());
        Sheet(w,"Kesinti Yapılmayanlar",Results.Where(d=>d.Status!="Eşleşti").ToList());
        Sheet(w,"Kontrol Gerektiren Kayıtlar",Issues);
        var bank=w.Worksheet("İş Bankası Formatı");
        var mut=w.Worksheet("Mutemet Format");
        int count=Bank.Count;
        // Preserve totals, signature merges, row styles and footer by expanding existing data region.
        if(count>298) mut.Row(301).InsertRowsAbove(count-298);
        int total=301+Math.Max(0,count-298);
        bank.Range(2,1,Math.Max(2,bank.LastRowUsed()?.RowNumber()??2),6).Clear(XLClearOptions.Contents);
        mut.Range(3,1,total-1,7).Clear(XLClearOptions.Contents);
        for(int i=0;i<count;i++)
        {
            var d=Bank[i]; var p=d.Person; int r=i+2;
            // Unresolved identities remain visible, as in VBA; bold and explicitly listed for review.
            string id=d.Match?.Identity??p.Title;
            bank.Cell(r,1).SetValue(id); bank.Cell(r,1).Style.NumberFormat.Format="@";
            bank.Cell(r,2).SetValue(d.Match?.FullName??p.FullName);
            bank.Cell(r,6).SetValue(p.Amount);
            bank.Range(r,1,r,6).Style.Font.Bold=d.Status!="Eşleşti";
            bank.Range(r,1,r,6).Style.Border.OutsideBorder=XLBorderStyleValues.Thin;
            mut.Cell(r+1,1).SetValue(i+1);
            for(int c=1;c<=6;c++) mut.Cell(r+1,c+1).Value=bank.Cell(r,c).Value;
            mut.Cell(r+1,2).Style.NumberFormat.Format="@";
            mut.Cell(r+1,7).Style.NumberFormat.Format="#,##0.00";
        }
        mut.Cell(total,7).FormulaA1=$"SUM(G3:G{total-1})";
        bank.Cell(count+2,2).SetValue("TOPLAM"); bank.Cell(count+2,6).SetValue(Bank.Sum(d=>d.Person.Amount));
        var contact=New(w,"Kesinti Yapılmayanlar İrtibat");
        string[] headers=["Sıra No","Cari Kod","Ticari Unvan","Ad","Soyad","Cep Telefonu","Kesinti Yapılamayan Tutar","Eşleşme Durumu"];
        for(int c=0;c<headers.Length;c++) contact.Cell(1,c+1).SetValue(headers[c]);
        int row=2;
        foreach(var d in ContactResults)
        {
            var p=d.Match??d.Person;
            contact.Cell(row,1).SetValue(row-1); contact.Cell(row,2).SetValue(p.Code); contact.Cell(row,3).SetValue(p.Title);
            contact.Cell(row,4).SetValue(p.Name); contact.Cell(row,5).SetValue(p.Surname);
            contact.Cell(row,6).Style.NumberFormat.Format="@";
            contact.Cell(row,6).SetValue(d.Match?.Phone??""); contact.Cell(row,7).SetValue(d.Person.Amount);
            contact.Cell(row,8).SetValue(d.Status=="Eşleşmedi"?"İrtibat Bulunamadı":d.Status); row++;
        }
        contact.Cell(row,1).SetValue(ContactResults.Count); contact.Cell(row,4).SetValue("TOPLAM PERSONEL / TUTAR");
        contact.Cell(row,7).SetValue(ContactResults.Sum(d=>d.Person.Amount));
        contact.Columns().AdjustToContents(); contact.Row(1).Style.Font.Bold=true;
        // Reserve output atomically; never overwrite an existing file.
        using var output=new FileStream(destination,FileMode.CreateNew,FileAccess.ReadWrite);
        w.SaveAs(output);
    }
    static IXLWorksheet New(XLWorkbook w,string name) {if(w.TryGetWorksheet(name,out var old))old.Delete();return w.AddWorksheet(name);}
    static void Sheet(XLWorkbook w,string name,List<Decision> rows)
    {
        var s=New(w,name); string[] h=["Kaynak","Satır","Cari Kod","Ad","Soyad","Tutar","Durum","Açıklama"];
        for(int c=0;c<h.Length;c++)s.Cell(1,c+1).SetValue(h[c]);
        int r=2; foreach(var d in rows){var p=d.Person;s.Cell(r,1).SetValue(p.Source);s.Cell(r,2).SetValue(p.Row);s.Cell(r,3).SetValue(p.Code);s.Cell(r,4).SetValue(p.Name);s.Cell(r,5).SetValue(p.Surname);s.Cell(r,6).SetValue(p.Amount);s.Cell(r,7).SetValue(d.Status);s.Cell(r,8).SetValue(d.Reason);r++;}
        s.Cell(r,1).SetValue("TOPLAM");s.Cell(r,2).SetValue(rows.Count);s.Cell(r,6).SetValue(rows.Sum(d=>d.Person.Amount));s.Columns().AdjustToContents();s.Row(1).Style.Font.Bold=true;
    }
}
