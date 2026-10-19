using Veresiye;
using ClosedXML.Excel;
int passed=0;
void Check(bool condition,string label){if(!condition)throw new Exception(label);passed++;Console.WriteLine("PASS "+label);}
Person P(string n="Ali",string s="IŞIK",string code="",decimal amount=50)=>new("test",1,code,"Emn. Müd. "+n+" "+s,n,s,amount);
Check(Matching.Key(" İSMAİL  IŞIK\u00a0")==Matching.Key("ismail ışık"),"Turkish case/whitespace");
Check(Matching.Find(P(),[P(),P(code:"2")]).Status=="Kontrol Gerekli","Homonyms ambiguous");
Check(Matching.Find(P(code:"1"),[P(code:"1"),P(code:"1")]).Status=="Kontrol Gerekli","Duplicate code");
Check(Matching.Find(P(code:"1"),[P(code:"2")]).Match==null,"Conflicting codes do not fall back");
Check(Matching.Find(P(code:"1"),[P("Veli",code:"1")]).Status=="Kontrol Gerekli","Code/name conflict");
Check(Matching.Find(P(),[P() with {Title="Kom. Yrd. Ali IŞIK"}],true).Match==null,"Titles retained");
Check(Matching.Run([P(),P()],[P()]).All(x=>x.Status=="Kontrol Gerekli"),"Repeated source target");
string template=args[0]; string dir=System.IO.Path.Combine(System.IO.Path.GetTempPath(),"veresiye-test-"+Guid.NewGuid());Directory.CreateDirectory(dir);
string source=System.IO.Path.Combine(dir,"input.xlsx");
using(var w=new XLWorkbook(template)) {
 var s=w.Worksheet("Sayfa1");s.Cell(2,1).Value="Ali";s.Cell(2,2).Value="IŞIK";s.Cell(2,3).Value=50;s.Cell(4,1).Value="Ayşe";s.Cell(4,2).Value="ÖZTÜRK";s.Cell(4,3).Value=75;
 var k=w.Worksheet("Kesinti Listesi");k.Cell(2,2).Value="001";k.Cell(2,3).Value="Emn. Müd. Ali IŞIK";k.Cell(2,4).Value="Ali";k.Cell(2,5).Value="IŞIK";k.Cell(2,6).Value=50;
 var p=w.Worksheet("Personel Listesi");p.Cell(1,2).Value="Emn. Müd. Ali IŞIK";p.Cell(1,3).Value="Ali";p.Cell(1,4).Value="IŞIK";p.Cell(1,5).Value="00000000000";
 var c=w.Worksheet("İrtibat");c.Cell(2,2).Value="002";c.Cell(2,4).Value="Ayşe";c.Cell(2,5).Value="ÖZTÜRK";c.Cell(2,6).Style.NumberFormat.Format="@";c.Cell(2,6).Value="05000000000";w.SaveAs(source);
}
var hash=System.Security.Cryptography.SHA256.HashData(File.ReadAllBytes(source));
var service=new WorkbookService(source);service.Process();
Check(service.Personnel.Count==1 && service.Personnel[0].Row==1,"Personnel first row");
Check(service.Payroll.Count==2,"Blank rows excluded");Check(service.Results.Count(d=>d.Status=="Eşleşti")==1,"Deduction matching");
Check(service.Bank[0].Match!=null,"Actual V3 join");
string output=System.IO.Path.Combine(dir,"result.xlsx");service.Export(output);
using(var w=new XLWorkbook(output)) {
 Check(w.Worksheets.Last().Name=="Kesinti Yapılmayanlar İrtibat","Contacts last sheet");
 var c=w.Worksheet("Kesinti Yapılmayanlar İrtibat");Check(c.Cell(2,6).GetString()=="05000000000" && c.Cell(2,6).Style.NumberFormat.Format=="@","Phone text round trip");
 Check(c.Cell(3,7).GetValue<decimal>()==75,"Contact total");
 Check(w.Worksheet("Mutemet Format").MergedRanges.Any(r=>r.RangeAddress.ToString()=="A307:D307"),"Signature merge retained");
 Check(w.Worksheet("İş Bankası Formatı").Cell(2,1).GetString()=="00000000000","Identity remains text");
}
Check(hash.SequenceEqual(System.Security.Cryptography.SHA256.HashData(File.ReadAllBytes(source))),"Source unchanged");
bool rejected=false;try{service.Export(output);}catch(IOException){rejected=true;}Check(rejected,"No overwrite");
var empty=new WorkbookService(template);empty.Process();Check(empty.Payroll.Count==0 && empty.Personnel.Count==0,"Real blank template loaded");
Check(Matching.Find(P(),[]).Status=="Eşleşmedi","Missing contact retained as unmatched");
var missingSource=System.IO.Path.Combine(dir,"missing.xlsx");
using(var w=new XLWorkbook(source)){w.Worksheet("İrtibat").Cell(2,4).Value="Başka";w.SaveAs(missingSource);}
var missing=new WorkbookService(missingSource);missing.Process();var missingOutput=System.IO.Path.Combine(dir,"missing-result.xlsx");missing.Export(missingOutput);
using(var w=new XLWorkbook(missingOutput)){var c=w.Worksheet("Kesinti Yapılmayanlar İrtibat");Check(c.Cell(2,8).GetString()=="İrtibat Bulunamadı" && c.Cell(2,6).GetString()=="","Missing contact exported");}
var large=System.IO.Path.Combine(dir,"large.xlsx");
using(var w=new XLWorkbook(source)){var k=w.Worksheet("Kesinti Listesi");for(int r=3;r<=302;r++){k.Cell(r,2).Value="K"+r;k.Cell(r,3).Value="Unvan "+r;k.Cell(r,4).Value="Ad"+r;k.Cell(r,5).Value="Soyad";k.Cell(r,6).Value=1;}w.SaveAs(large);}
var expanded=new WorkbookService(large);expanded.Process();var expandedOutput=System.IO.Path.Combine(dir,"expanded.xlsx");expanded.Export(expandedOutput);
using(var w=new XLWorkbook(expandedOutput)){var m=w.Worksheet("Mutemet Format");Check(m.Cell(310,1).GetString()=="DÜZENLEYEN","Signature shifted beyond 298 records");Check(m.Cell(304,7).GetValue<decimal>()==350,"Expanded Mutemet total");}
Console.WriteLine($"{passed} checks passed");Directory.Delete(dir,true);
