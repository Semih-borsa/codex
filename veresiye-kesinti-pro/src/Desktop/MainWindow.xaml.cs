using System.Windows;
using System.Windows.Media;
using Microsoft.Win32;
namespace Veresiye.Desktop;
public partial class MainWindow:Window
{
    WorkbookService? workbook; bool dark;
    public MainWindow()=>InitializeComponent();
    async void Load(object sender,RoutedEventArgs e)
    {
        var dialog=new OpenFileDialog{Filter="Excel çalışma kitabı|*.xlsm;*.xlsx"};
        if(dialog.ShowDialog()!=true)return;
        ProcessButton.IsEnabled=false;ExportButton.IsEnabled=false;workbook=null;
        try {
            var loaded=await Task.Run(()=>new WorkbookService(dialog.FileName));workbook=loaded;
            PayrollGrid.ItemsSource=loaded.Payroll;PersonnelGrid.ItemsSource=loaded.Personnel;DeductionGrid.ItemsSource=loaded.Deductions;
            ResultGrid.ItemsSource=null;ContactGrid.ItemsSource=null;IssueGrid.ItemsSource=null;
            Summary.Text=$"Sayfa1: {loaded.Payroll.Count} • Personel: {loaded.Personnel.Count} • Kesinti: {loaded.Deductions.Count} • İrtibat: {loaded.Contacts.Count}";
            ProcessButton.IsEnabled=true;
        } catch { MessageBox.Show("Dosya okunamadı. Dosyanın geçerli Excel biçiminde, gerekli altı sayfayı ve sayısal tutarları içerdiğini kontrol edin.","Dosya hatası"); }
    }
    void Process(object sender,RoutedEventArgs e)
    {
        if(workbook==null)return;workbook.Process();ResultGrid.ItemsSource=View(workbook.Results);ContactGrid.ItemsSource=View(workbook.ContactResults);IssueGrid.ItemsSource=View(workbook.Issues);
        Summary.Text=$"Toplam personel: {workbook.Results.Count} • Eşleşen: {workbook.Results.Count(x=>x.Status=="Eşleşti")} • Eşleşmeyen: {workbook.Results.Count(x=>x.Status=="Eşleşmedi")} • Şüpheli: {workbook.Results.Count(x=>x.Status=="Kontrol Gerekli")} • Tüm kontrol kayıtları: {workbook.Issues.Count}";
        ExportButton.IsEnabled=true;
    }
    async void Export(object sender,RoutedEventArgs e)
    {
        if(workbook==null)return;
        var dialog=new SaveFileDialog{Filter="Excel çalışma kitabı|*.xlsx",FileName=$"Veresiye_Sonuc_{DateTime.Now:yyyyMMdd_HHmmss}_{Guid.NewGuid().ToString()[..6]}.xlsx",OverwritePrompt=true};
        if(dialog.ShowDialog()!=true)return;
        ExportButton.IsEnabled=false;IsEnabled=false;
        try {await Task.Run(()=>workbook.Export(dialog.FileName));MessageBox.Show("Yeni sonuç dosyası oluşturuldu. Kontrol listesini inceleyin.");}
        catch {MessageBox.Show("Dosya kaydedilemedi. Yeni ve kullanılmayan bir dosya adı seçin; klasörün yazılabilir olduğunu kontrol edin.");}
        finally {ExportButton.IsEnabled=true;IsEnabled=true;}
    }
    static System.Collections.IEnumerable View(IEnumerable<Decision> rows)=>rows.Select(d=>new {Kaynak=d.Person.Source,Satır=d.Person.Row,CariKod=d.Match?.Code??d.Person.Code,Ad=d.Person.Name,Soyad=d.Person.Surname,Tutar=d.Person.Amount,Durum=d.Status=="Eşleşmedi" && d.Person.Source=="Sayfa1" ? "Eşleşmedi / İrtibatı kontrol edin" : d.Status,Açıklama=d.Reason,Telefon=d.Match?.Phone??""}).ToList();
    void Theme(object sender,RoutedEventArgs e){dark=!dark;Resources["Surface"]=new SolidColorBrush((Color)ColorConverter.ConvertFromString(dark?"#182235":"#F3F6FA"));Resources["Ink"]=new SolidColorBrush((Color)ColorConverter.ConvertFromString(dark?"#EDF3FA":"#17243B"));}
}
