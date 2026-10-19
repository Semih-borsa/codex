package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class MainActivity extends Activity {
  private State state;
  private Store store;
  private LinearLayout root, body, tabs;
  private LocalDate selected = monday(LocalDate.now());
  private int screen = 0;
  private TextView notice;
  private String noticeText = "";
  private int noticeToken;
  private boolean failed = false;
  private Week draft;
  private ArrayList<String> weekendOrderDraft;
  private int ink = Color.rgb(19, 38, 60),
      teal = Color.rgb(42, 111, 177),
      muted = Color.rgb(85, 105, 124),
      surface = Color.rgb(237, 245, 253),
      cardColor = Color.WHITE,
      warningColor = Color.rgb(155, 83, 0);
  private int themeMode;
  private final DateTimeFormatter shortDate = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  @Override
  public void onCreate(Bundle b) {
    themeMode = getSharedPreferences("appearance", 0).getInt("theme", 0);
    boolean dark =
        themeMode == 2
            || (themeMode == 0
                && (getResources().getConfiguration().uiMode
                        & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES);
    setTheme(dark ? R.style.AppThemeDark : R.style.AppTheme);
    if (dark) {
      ink = Color.rgb(231, 238, 247);
      teal = Color.rgb(137, 187, 235);
      muted = Color.rgb(174, 189, 209);
      surface = Color.rgb(16, 27, 43);
      cardColor = Color.rgb(26, 42, 62);
      warningColor = Color.rgb(255, 194, 112);
    }
    super.onCreate(b);
    store = new Store(this);
    try {
      state = store.load();
    } catch (Exception e) {
      state = new State();
      failed = true;
    }
    if (b != null) {
      try {
        if (b.containsKey("draft")) draft = Store.decode(b.getString("draft")).weeks.get(0);
      } catch (Exception ignored) {
      }
      selected = LocalDate.parse(b.getString("week", selected.toString()));
      screen = b.getInt("screen", 0);
      weekendOrderDraft = b.getStringArrayList("weekendOrderDraft");
    }
    render();
    if (failed)
      new AlertDialog.Builder(this)
          .setTitle("Veriler Okunamadı")
          .setMessage(
              "Kayıt dosyası açılamadı. Üzerine yazılmasını önlemek için düzenleme kapatıldı."
                  + " Uygulama verilerini silmeyin.")
          .setPositiveButton("Tamam", null)
          .show();
  }

  @Override
  public void onSaveInstanceState(Bundle b) {
    super.onSaveInstanceState(b);
    b.putString("week", selected.toString());
    b.putInt("screen", screen);
    if (weekendOrderDraft != null) b.putStringArrayList("weekendOrderDraft", weekendOrderDraft);
    if (draft != null)
      try {
        State temp = new State();
        temp.weeks.add(draft);
        b.putString("draft", Store.encode(temp));
      } catch (Exception ignored) {
      }
  }

  private int dp(float n) {
    return (int) (getResources().getDisplayMetrics().density * n + .5f);
  }

  private TextView text(String str, int size, int color) {
    TextView v = new TextView(this);
    v.setText(str);
    v.setTextSize(size);
    v.setTextColor(color);
    v.setPadding(0, dp(5), 0, dp(5));
    return v;
  }

  private void heading(String s) {
    TextView v = text(s, 22, ink);
    v.setTypeface(null, Typeface.BOLD);
    body.addView(v);
  }

  private void note(String s) {
    body.addView(text(s, 14, muted));
  }

  private Button button(String s, Runnable action) {
    Button b = new Button(this);
    b.setText(s);
    b.setTextSize(14);
    b.setTextColor(Color.WHITE);
    b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(42, 111, 177)));
    b.setAllCaps(false);
    b.setOnClickListener(
        v -> {
          if (failed) {
            toast("Kayıtlar okunamadığı için değişiklikler kapalı.");
            return;
          }
          action.run();
        });
    return b;
  }

  private void action(String s, Runnable r) {
    body.addView(button(s, r), new LinearLayout.LayoutParams(-1, dp(52)));
  }

  private void toast(String s) {
    noticeText = s;
    int token = ++noticeToken;
    if (notice != null) {
      notice.setText(s);
      notice.setVisibility(View.VISIBLE);
    }
    new Handler(Looper.getMainLooper())
        .postDelayed(
            () -> {
              if (token == noticeToken) {
                noticeText = "";
                if (notice != null) notice.setVisibility(View.GONE);
              }
            },
            9000);
  }

  private void confirm(String title, String message, Runnable r) {
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton("Onayla", (d, w) -> r.run())
        .show();
  }

  private boolean save() {
    try {
      store.save(state);
      return true;
    } catch (Exception e) {
      failed = true;
      toast("Kaydedilemedi. Verileri korumak için düzenleme durduruldu; uygulamayı yeniden açın.");
      return false;
    }
  }

  private Week saved() {
    return state.weeks.stream().filter(w -> w.monday.equals(selected)).findFirst().orElse(null);
  }

  private String range() {
    return selected.format(shortDate) + " – " + selected.plusDays(6).format(shortDate);
  }

  private void render() {
    root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(surface);
    root.setPadding(dp(18), dp(12), dp(18), dp(8));
    root.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          root.setPadding(
              dp(18),
              dp(12) + insets.getSystemWindowInsetTop(),
              dp(18),
              dp(8) + insets.getSystemWindowInsetBottom());
          return insets;
        });
    TextView logo = text("Nöbet Çizelgesi", 27, ink);
    logo.setTypeface(null, Typeface.BOLD);
    root.addView(logo);
    root.addView(text("Planla • Onayla • Adil Dağıt", 13, muted));
    root.addView(
        button(
            "Görünüm: " + (themeMode == 0 ? "Sistem" : themeMode == 1 ? "Açık" : "Koyu"),
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Kurumsal Renk Teması")
                    .setSingleChoiceItems(
                        new String[] {"Sistem Teması", "Açık • Lacivert", "Koyu • Lacivert"},
                        themeMode,
                        (dialog, which) -> {
                          getSharedPreferences("appearance", 0)
                              .edit()
                              .putInt("theme", which)
                              .apply();
                          dialog.dismiss();
                          recreate();
                        })
                    .setNegativeButton("Vazgeç", null)
                    .show()));
    tabs = new LinearLayout(this);
    String[] titles = {"Çizelge", "Personel", "Mazeret", "İstatistik", "Ayarlar"};
    for (int i = 0; i < titles.length; i++) {
      final int idx = i;
      Button b =
          button(
              titles[i],
              () -> {
                screen = idx;
                render();
              });
      b.setTextSize(11);
      b.setMinWidth(0);
      b.setMinimumWidth(0);
      b.setPadding(dp(3), 0, dp(3), 0);
      if (i == screen) {
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(Color.WHITE);
      }
      tabs.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
    }
    root.addView(tabs);
    ScrollView scroll = new ScrollView(this);
    body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(0, dp(12), 0, dp(30));
    scroll.setFillViewport(true);
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    notice = text(noticeText, 14, Color.WHITE);
    notice.setPadding(dp(12), dp(10), dp(12), dp(10));
    GradientDrawable noticeBg = new GradientDrawable();
    noticeBg.setColor(Color.rgb(42, 111, 177));
    noticeBg.setCornerRadius(dp(12));
    notice.setBackground(noticeBg);
    notice.setVisibility(noticeText.isEmpty() ? View.GONE : View.VISIBLE);
    root.addView(notice, new LinearLayout.LayoutParams(-1, -2));
    setContentView(root);
    if (screen == 0) program();
    else if (screen == 1) people();
    else if (screen == 2) leaves();
    else if (screen == 3) stats();
    else settings();
  }

  private void program() {
    heading(range());
    LinearLayout nav = new LinearLayout(this);
    nav.addView(
        button(
            "‹ Önceki",
            () -> {
              selected = selected.minusWeeks(1);
              draft = null;
              render();
            }),
        new LinearLayout.LayoutParams(0, dp(52), 1));
    nav.addView(
        button(
            "Hafta Seç",
            () ->
                date(
                    selected,
                    d -> {
                      selected = monday(d);
                      draft = null;
                      render();
                    })),
        new LinearLayout.LayoutParams(0, dp(52), 1));
    nav.addView(
        button(
            "Sonraki ›",
            () -> {
              selected = selected.plusWeeks(1);
              draft = null;
              render();
            }),
        new LinearLayout.LayoutParams(0, dp(52), 1));
    body.addView(nav);
    note(
        "Görev yerlerinin günleri Ayarlar bölümünden belirlenir. Ana yerde seçili hafta sonu"
            + " günleri aynı kişiye verilir; bu kişi perşembe/cuma tüm yerlerde izinli olur.");
    action("Çizelgede Gösterilecek Şubeler", () -> chooseVisiblePlaces(null));
    note("Gizlenen şubeler yalnız çizelge/PDF'de görünmez; atamaları ve istatistikleri korunur.");
    if (state.people.stream().noneMatch(p -> p.active)) {
      note(
          "Başlamak için Personel bölümüne 5 kişinin adını ekleyin. Sonradan ekleyebilir, pasife"
              + " alabilir veya yeniden etkinleştirebilirsiniz.");
      action(
          "Personel Ekle",
          () -> {
            screen = 1;
            render();
          });
      return;
    }
    action(
        "Otomatik Dağıtım Öner",
        () ->
            chooseVisiblePlaces(
                () -> {
                  try {
                    draft = generate(state, selected);
                    render();
                  } catch (IllegalArgumentException e) {
                    toast(e.getMessage());
                  }
                }));
    action(
        "Boş Çizelge Aç • Manuel Oluştur",
        () ->
            chooseVisiblePlaces(
                () ->
                    confirm(
                        "Manuel Çizelge",
                        "Bu hafta için boş bir öneri açılacak. Mevcut kayıt, siz onaylayana kadar"
                            + " değişmeyecek. Günlerdeki işyeri/şube alanlarına dokunarak personel"
                            + " seçin.",
                        () -> {
                          draft = new Week(selected);
                          draft.rotationRevision = state.rotationRevision;
                          Week previous =
                              state.weeks.stream()
                                  .filter(x -> x.monday.isBefore(selected))
                                  .max(Comparator.comparing(x -> x.monday))
                                  .orElse(null);
                          if (previous != null) {
                            if (previous.rotationRevision == state.rotationRevision)
                              draft.cursor = previous.cursor;
                            for (Debt d : previous.debts)
                              draft.debts.add(new Debt(d.person, d.end));
                          }
                          draft.warnings.add(
                              "Manuel çizelge: boş alanları ve mazeretleri kontrol ederek"
                                  + " onaylayın.");
                          render();
                        })));
    Week w = draft != null ? draft : saved();
    if (w == null) {
      note("Bu hafta için henüz onaylı çizelge yok.");
      return;
    }
    boolean preview = draft != null;
    heading(preview ? "Onay Bekleyen Öneri" : "Onaylı Çizelge");
    if (!preview)
      action(
          "Kaydedilmiş Listeyi Düzenle",
          () -> {
            draft = copy(w);
            render();
            toast("Gün/yer alanlarına dokunup düzenleyin; ardından değişiklikleri kaydedin.");
          });
    LocalDate sat =
        w.duties.stream()
            .filter(d -> d.place.equals(WORK) && weekend(d.date))
            .map(d -> d.date)
            .min(Comparator.naturalOrder())
            .orElse(selected.plusDays(5));
    String weekendPerson = PdfExport.person(state, w, sat, WORK);
    note("Hafta Sonu: " + weekendPerson + "\nPerşembe / Cuma Dinlenme: " + weekendPerson);
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM EEEE", new Locale("tr", "TR"));
    for (int i = 0; i < 7; i++) {
      LocalDate day = selected.plusDays(i);
      LinearLayout card = new LinearLayout(this);
      card.setOrientation(LinearLayout.VERTICAL);
      card.setPadding(dp(14), dp(10), dp(14), dp(10));
      GradientDrawable bg = new GradientDrawable();
      bg.setColor(cardColor);
      bg.setCornerRadius(dp(14));
      card.setBackground(bg);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
      lp.setMargins(0, dp(7), 0, dp(3));
      body.addView(card, lp);
      TextView dayTitle = text(day.format(fmt), 16, ink);
      dayTitle.setTypeface(null, Typeface.BOLD);
      card.addView(dayTitle);
      for (Place location : displayPlaces(state, w)) {
        String place = location.id;
        if ((location.deleted || !location.on(day))
            && w.duties.stream().noneMatch(d -> d.date.equals(day) && d.place.equals(place)))
          continue;
        String label = location.name + ": " + PdfExport.person(state, w, day, place);
        if (location.deleted) card.addView(text(label + " (Silindi)", 14, ink));
        else card.addView(button(label + "  ✎", () -> edit(day, place)));
      }
      if (day.getDayOfWeek().getValue() == 4 || day.getDayOfWeek().getValue() == 5)
        card.addView(text("Dinlenme: " + weekendPerson, 13, muted));
    }
    for (String warning : w.warnings) body.addView(text("• " + warning, 14, warningColor));
    if (preview) {
      action(
          saved() != null ? "Değişiklikleri Kaydet" : "Dağıtımı Onayla Ve Kaydet",
          () -> approveDraft());
      action(
          "Öneriyi İptal Et",
          () -> {
            draft = null;
            render();
          });
    }
    action("Hafta Sonu Nöbetçisini Atla / Mazeret Gir", () -> skip(w));
    action("Bu Hafta " + state.placeName(BRANCH) + " Muafiyeti", () -> branchExcuse());
    if (!preview)
      action(
          "PDF Oluştur Ve Paylaş",
          () -> {
            if (!validate(state, w).isEmpty()) {
              toast("Çizelgede personel/mazeret çakışması var; yeniden dağıtın.");
              return;
            }
            try {
              PdfExport.share(this, PdfExport.create(this, state, w));
            } catch (Exception e) {
              toast("PDF oluşturulamadı veya paylaşım uygulaması bulunamadı.");
            }
          });
    List<Debt> waiting = preview ? w.debts : pendingDebts(state, selected.plusWeeks(1));
    if (!waiting.isEmpty()) {
      heading("Bekleyen Hafta Sonu Sırası");
      for (Debt d : waiting)
        note(
            state.name(d.person)
                + " • mazeret sonu "
                + d.end.format(shortDate)
                + " • ilk uygun hafta sonu öncelikli");
    }
    note(
        "Onay, çizelge planını kaydeder. İstatistikte geçmiş/bugünkü nöbetler ile gelecek atamalar"
            + " ayrı gösterilir. Gerçekleşmeyen nöbetleri çizelgede düzeltin.");
  }

  private void approveDraft() {
    if (draft == null) return;
    List<String> errors = validate(state, draft);
    if (!errors.isEmpty()) {
      toast(String.join("\n", errors));
      return;
    }
    long missing = Math.max(0, expectedSlots(state) - draft.duties.size());
    String msg =
        "Atamalar Ayarlar'daki seçili günlere ve haftalık sınırlara göre yapılır. Ana yerde iki"
            + " hafta sonu günü de seçiliyse aynı kişi atanır; bu kişi perşembe/cuma izinlidir."
            + " Gizli şubelerin atamaları ve istatistikleri korunur.\n\n"
            + (missing > 0
                ? missing + " boş nöbet alanı var. Eksik çizelge olarak kaydedilecek.\n\n"
                : "")
            + String.join("\n", draft.warnings);
    if (state.weeks.stream().anyMatch(w -> w.monday.isAfter(selected)))
      msg +=
          "\n\n"
              + "Yalnız bu hafta güncellenecek; diğer onaylı haftalar korunacak. İstatistikler"
              + " güncel çizelgelerden yeniden hesaplanır.";
    confirm(
        "Dağıtımı Teyit Edin",
        msg,
        () -> {
          try {
            approve(state, draft);
            if (save()) {
              draft = null;
              render();
              toast("Çizelge onaylandı.");
            }
          } catch (IllegalArgumentException e) {
            toast(e.getMessage());
          }
        });
  }

  private void people() {
    heading("Personel");
    note(
        "Hafta sonu sırasını Ayarlar → Hafta Sonu Nöbet Sırası bölümünden düzenleyin. Pasif"
            + " personel yeni atamalarda kullanılmaz.");
    action("+ Personel Ekle", () -> personDialog(null));
    action(
        "Nöbet Sırasını Düzenle",
        () -> {
          screen = 4;
          render();
        });
    for (Person p : state.people) {
      heading(p.name + (p.active ? "" : " · Pasif"));
      LinearLayout row = new LinearLayout(this);
      row.addView(button("Adı Düzenle", () -> personDialog(p)));
      row.addView(
          button(
              p.active ? "Personeli Çıkar" : "Yeniden Etkinleştir",
              () ->
                  confirm(
                      "Personel Durumu",
                      p.active
                          ? "Personel pasife alınacak; geçmişi korunacak. Bugün ve sonraki"
                              + " atamaları kaldırılacak; yeniden dağıtım gerekecek."
                          : "Personel yeniden atamalara katılacak.",
                      () -> {
                        p.active = !p.active;
                        if (!p.active) {
                          for (Week wk : state.weeks) {
                            wk.duties.removeIf(
                                d -> d.person.equals(p.id) && !d.date.isBefore(LocalDate.now()));
                            wk.debts.removeIf(d -> d.person.equals(p.id));
                          }
                        }
                        if (save()) {
                          draft = null;
                          render();
                        }
                      })));
      body.addView(row);
    }
    note(
        "Veriler yalnız bu cihazda tutulur. Uygulamanın kaldırılması veya verilerinin silinmesi"
            + " kayıtları siler.");
  }

  private void personDialog(Person p) {
    EditText input = new EditText(this);
    input.setSingleLine(true);
    input.setHint("Ad Soyad");
    if (p != null) input.setText(p.name);
    LinearLayout fields = new LinearLayout(this);
    fields.setOrientation(LinearLayout.VERTICAL);
    fields.setPadding(dp(18), 0, dp(18), 0);
    fields.addView(input);
    List<String> ids = p == null ? editableWeekendIds() : activeWeekendIds();
    EditText position = new EditText(this);
    position.setSingleLine(true);
    position.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    position.setHint("Hafta Sonu Sıra Numarası");
    position.setText(
        getString(R.string.limit_value, p == null ? ids.size() + 1 : ids.indexOf(p.id) + 1));
    if (p == null) {
      fields.addView(text("Hafta Sonu Nöbet Sırası (1–" + (ids.size() + 1) + ")", 14, ink));
      fields.addView(position);
      fields.addView(
          text(
              "Yeni sıra sonraki otomatik öneride 1. sıradan başlar. Bekleyen mazeret hakları"
                  + " korunur.",
              13,
              muted));
    }
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(p == null ? "Personel Ekle" : "Adı Düzenle")
            .setView(fields)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaydet", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String name = input.getText().toString().trim().replaceAll("\\s+", " ");
                      if (name.isEmpty() || name.length() > 60) {
                        input.setError("1–60 karakter girin.");
                        return;
                      }
                      if (state.people.stream()
                          .anyMatch(person -> person != p && person.name.equalsIgnoreCase(name))) {
                        input.setError("Bu isim zaten var; ayırt edici bir ad girin.");
                        return;
                      }
                      if (p == null) {
                        int rank;
                        try {
                          rank = Integer.parseInt(position.getText().toString());
                          if (rank < 1 || rank > ids.size() + 1) throw new NumberFormatException();
                        } catch (NumberFormatException e) {
                          position.setError("1–" + (ids.size() + 1) + " arasında sıra girin.");
                          return;
                        }
                        Person added = new Person(UUID.randomUUID().toString(), name);
                        state.people.add(added);
                        ids.add(rank - 1, added.id);
                        setWeekendOrder(state, ids);
                        weekendOrderDraft = null;
                      } else p.name = name;
                      if (save()) {
                        dialog.dismiss();
                        draft = null;
                        render();
                      }
                    }));
    dialog.show();
  }

  private void leaves() {
    heading("Mazeret Ve Kısıtlar");
    note(
        "Aynı personele birden fazla mazeret veya kısıt ekleyebilirsiniz. Tarih aralığı ya da her"
            + " hafta tekrarlanan günler seçin. Genel mazeret tüm yerleri, yer kısıtı yalnız"
            + " seçtiğiniz yeri engeller.");
    action(
        "+ Mazeret / Kısıt Ekle",
        () -> leaveDialog(null, ALL, selected, selected.plusDays(6), "Senelik izin"));
    for (Leave l : state.leaves) {
      TextView t =
          text(
              state.name(l.person)
                  + " • "
                  + l.reason
                  + "\n"
                  + leavePeriod(l)
                  + " • "
                  + state.placeName(l.place),
              16,
              ink);
      body.addView(t);
      LinearLayout row = new LinearLayout(this);
      row.addView(button("Düzenle", () -> leaveDialog(l, l.place, l.start, l.end, l.reason)));
      row.addView(
          button(
              "Sil",
              () ->
                  confirm(
                      "Mazereti Sil",
                      "Bu mazeret kaldırılacak. Mevcut çizelgeleri yeniden üretmeden atamalar"
                          + " değişmez.",
                      () -> {
                        state.leaves.remove(l);
                        for (Week wk : state.weeks)
                          for (Debt debt : wk.debts)
                            if (debt.person.equals(l.person) && debt.end.equals(l.end))
                              debt.end = LocalDate.now().minusDays(1);
                        if (save()) {
                          draft = null;
                          render();
                        }
                      })));
      body.addView(row);
    }
  }

  private void date(LocalDate initial, java.util.function.Consumer<LocalDate> consumer) {
    new DatePickerDialog(
            this,
            (v, y, m, d) -> consumer.accept(LocalDate.of(y, m + 1, d)),
            initial.getYear(),
            initial.getMonthValue() - 1,
            initial.getDayOfMonth())
        .show();
  }

  private void dateTitled(
      String title, LocalDate initial, java.util.function.Consumer<LocalDate> consumer) {
    DatePickerDialog picker =
        new DatePickerDialog(
            this,
            (v, y, m, d) -> consumer.accept(LocalDate.of(y, m + 1, d)),
            initial.getYear(),
            initial.getMonthValue() - 1,
            initial.getDayOfMonth());
    picker.setTitle(title);
    picker.show();
  }

  private Spinner spinner(String[] values) {
    Spinner s = new Spinner(this);
    s.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values));
    s.setMinimumHeight(dp(52));
    return s;
  }

  private String leavePeriod(Leave leave) {
    String period =
        leave.start.format(shortDate)
            + " – "
            + (leave.end.equals(LocalDate.MAX) ? "Süresiz" : leave.end.format(shortDate));
    if (leave.days == 0) return period;
    String[] names = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
    List<String> selectedDays = new ArrayList<>();
    for (int i = 0; i < 7; i++)
      if ((leave.days & (1 << i)) != 0) selectedDays.add("Her " + names[i]);
    return String.join(", ", selectedDays)
        + (leave.end.equals(LocalDate.MAX) ? "" : " • Eski Tarihli Kayıt");
  }

  private final class WeeklyRestrictionRow {
    final LinearLayout view = new LinearLayout(MainActivity.this);
    final Spinner location;
    final CheckBox[] days = new CheckBox[7];

    WeeklyRestrictionRow(List<Place> locations, String selectedPlace, int mask) {
      view.setOrientation(LinearLayout.VERTICAL);
      view.setTag("weekly-restriction-row");
      view.setPadding(0, dp(6), 0, dp(6));
      List<String> names = new ArrayList<>();
      names.add("Tüm Görev Yerleri");
      for (Place p : locations) names.add(p.name);
      location = spinner(names.toArray(new String[0]));
      for (int i = 0; i < locations.size(); i++)
        if (locations.get(i).id.equals(selectedPlace)) location.setSelection(i + 1);
      view.addView(location);
      String[] weekdays = {
        "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
      };
      for (int i = 0; i < 7; i += 2) {
        LinearLayout line = new LinearLayout(MainActivity.this);
        for (int j = i; j < Math.min(i + 2, 7); j++) {
          days[j] = new CheckBox(MainActivity.this);
          days[j].setText(getString(R.string.each_weekday, weekdays[j]));
          days[j].setTextSize(13);
          days[j].setTextColor(ink);
          days[j].setTag("leave-day-" + j);
          days[j].setChecked((mask & (1 << j)) != 0);
          line.addView(days[j], new LinearLayout.LayoutParams(0, -2, 1));
        }
        view.addView(line);
      }
    }

    int mask() {
      int value = 0;
      for (int i = 0; i < days.length; i++) if (days[i].isChecked()) value |= 1 << i;
      return value;
    }
  }

  private AlertDialog leaveDialog(
      Leave existing, String place, LocalDate start, LocalDate end, String reason) {
    List<Person> list =
        state.people.stream()
            .filter(p -> p.active || existing != null && p.id.equals(existing.person))
            .collect(java.util.stream.Collectors.toList());
    if (list.isEmpty()) {
      toast("Önce personel ekleyin.");
      return null;
    }
    LinearLayout box = new LinearLayout(this);
    box.setPadding(dp(20), dp(8), dp(20), dp(8));
    box.setOrientation(LinearLayout.VERTICAL);
    Spinner who = spinner(list.stream().map(p -> p.name).toArray(String[]::new));
    box.addView(who);
    if (existing != null)
      for (int i = 0; i < list.size(); i++)
        if (list.get(i).id.equals(existing.person)) who.setSelection(i);
    List<String> types = new ArrayList<>(Arrays.asList("Senelik İzin", "Rapor", "Diğer Mazeret"));
    List<Place> locations =
        state.places.stream()
            .filter(p -> !p.deleted || existing != null && p.id.equals(existing.place))
            .collect(java.util.stream.Collectors.toList());
    for (Place location : locations) types.add("Yalnız " + location.name + " kısıtı");
    Spinner type = spinner(types.toArray(new String[0]));
    int index = reason.equals("Rapor") ? 1 : reason.equalsIgnoreCase("Senelik izin") ? 0 : 2;
    if (!place.equals(ALL))
      for (int i = 0; i < locations.size(); i++)
        if (locations.get(i).id.equals(place)) index = 3 + i;
    type.setSelection(index);
    box.addView(type);
    LocalDate[] dates = {start, end};
    Button from = button("Başlangıç: " + start.format(shortDate), () -> {}),
        to = button("Bitiş: " + end.format(shortDate), () -> {});
    from.setOnClickListener(
        v ->
            date(
                dates[0],
                d -> {
                  dates[0] = d;
                  from.setText(getString(R.string.starts, d.format(shortDate)));
                }));
    to.setOnClickListener(
        v ->
            date(
                dates[1],
                d -> {
                  dates[1] = d;
                  to.setText(getString(R.string.ends, d.format(shortDate)));
                }));
    CheckBox weekly = new CheckBox(this);
    weekly.setText(R.string.weekly_repeat);
    weekly.setTextColor(ink);
    weekly.setTag("leave-weekly");
    weekly.setChecked(existing != null && existing.days != 0);
    box.addView(weekly);
    LinearLayout recurring = new LinearLayout(this);
    recurring.setOrientation(LinearLayout.VERTICAL);
    LinearLayout rowsBox = new LinearLayout(this);
    rowsBox.setOrientation(LinearLayout.VERTICAL);
    recurring.addView(rowsBox);
    List<WeeklyRestrictionRow> rows = new ArrayList<>();
    java.util.function.BiConsumer<String, Integer> addRow =
        (selectedPlace, mask) -> {
          WeeklyRestrictionRow row = new WeeklyRestrictionRow(locations, selectedPlace, mask);
          Button remove =
              button(
                  "− Kısıtı Kaldır",
                  () -> {
                    if (rows.size() == 1) {
                      toast("En az bir kısıt satırı kalmalı.");
                      return;
                    }
                    rows.remove(row);
                    rowsBox.removeView(row.view);
                  });
          remove.setTag("weekly-remove-row");
          row.view.addView(remove);
          rows.add(row);
          rowsBox.addView(row.view);
        };
    addRow.accept(place, existing == null ? 0 : existing.days);
    Button plus = button("+ Kısıt Ekle", () -> addRow.accept(ALL, 0));
    plus.setTag("weekly-add-row");
    recurring.addView(plus);
    box.addView(recurring);
    box.addView(from);
    box.addView(to);
    from.setTag("leave-start-date");
    to.setTag("leave-end-date");
    Runnable updateMode =
        () -> {
          recurring.setVisibility(weekly.isChecked() ? View.VISIBLE : View.GONE);
          type.setVisibility(weekly.isChecked() ? View.GONE : View.VISIBLE);
          from.setVisibility(weekly.isChecked() ? View.GONE : View.VISIBLE);
          to.setVisibility(weekly.isChecked() ? View.GONE : View.VISIBLE);
        };
    weekly.setOnCheckedChangeListener(
        (view, checked) -> {
          if (!checked && dates[1].equals(LocalDate.MAX)) {
            dates[0] = LocalDate.now();
            dates[1] = dates[0].plusDays(6);
            from.setText(getString(R.string.starts, dates[0].format(shortDate)));
            to.setText(getString(R.string.ends, dates[1].format(shortDate)));
          }
          updateMode.run();
        });
    updateMode.run();
    ScrollView fields = new ScrollView(this);
    fields.addView(box);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Mazeret / Kısıt")
            .setView(fields)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Teyit Et", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String person = list.get(who.getSelectedItemPosition()).id;
                      List<Leave> newLeaves = new ArrayList<>();
                      if (weekly.isChecked()) {
                        for (int i = 0; i < rows.size(); i++) {
                          WeeklyRestrictionRow row = rows.get(i);
                          if (row.mask() == 0) {
                            toast((i + 1) + ". kısıt için en az bir gün seçin.");
                            return;
                          }
                          int selectedLocation = row.location.getSelectedItemPosition();
                          String target =
                              selectedLocation == 0 ? ALL : locations.get(selectedLocation - 1).id;
                          newLeaves.add(
                              new Leave(
                                  person,
                                  target,
                                  LocalDate.now(),
                                  LocalDate.MAX,
                                  "Haftalık Kısıt",
                                  row.mask()));
                        }
                      } else {
                        if (dates[1].isBefore(dates[0])) {
                          toast("Bitiş tarihi başlangıçtan önce olamaz.");
                          return;
                        }
                        int t = type.getSelectedItemPosition();
                        String target = t >= 3 ? locations.get(t - 3).id : ALL;
                        newLeaves.add(
                            new Leave(
                                person,
                                target,
                                dates[0],
                                dates[1],
                                type.getSelectedItem().toString()));
                      }
                      StringBuilder summary = new StringBuilder(state.name(person));
                      for (Leave leave : newLeaves)
                        summary
                            .append("\n\n")
                            .append(leavePeriod(leave))
                            .append("\n")
                            .append(state.placeName(leave.place));
                      confirm(
                          "Mazereti Kaydet",
                          summary
                              + "\n\n"
                              + "Seçili günlerde atanmayacak. Yalnız çakışan atamalar kaldırılır;"
                              + " boşalan yerleri yeniden planlayın.",
                          () -> {
                            if (existing != null) {
                              state.leaves.remove(existing);
                              for (Week wk : state.weeks)
                                for (Debt debt : wk.debts)
                                  if (debt.person.equals(existing.person)
                                      && debt.end.equals(existing.end))
                                    debt.end = LocalDate.now().minusDays(1);
                            }
                            for (Leave leave : newLeaves) {
                              state.leaves.add(leave);
                              removeConflicts(leave);
                            }
                            if (save()) {
                              dialog.dismiss();
                              draft = null;
                              render();
                              toast("Mazeret kaydedildi. Boşalan günler için dağıtım önerin.");
                            }
                          });
                    }));
    dialog.show();
    return dialog;
  }

  private void removeConflicts(Leave l) {
    for (Week w : state.weeks) {
      boolean affectedWeekend =
          w.duties.stream()
              .anyMatch(
                  d ->
                      weekend(d.date)
                          && d.place.equals(WORK)
                          && l.blocks(d.person, d.place, d.date));
      if (affectedWeekend) {
        String id =
            w.duties.stream()
                .filter(
                    d ->
                        weekend(d.date)
                            && d.place.equals(WORK)
                            && l.blocks(d.person, d.place, d.date))
                .map(d -> d.person)
                .findFirst()
                .orElse("");
        w.duties.removeIf(d -> weekend(d.date) && d.person.equals(id) && d.place.equals(WORK));
        if (w.debts.stream().noneMatch(d -> d.person.equals(id))) w.debts.add(new Debt(id, l.end));
      }
      w.duties.removeIf(d -> l.blocks(d.person, d.place, d.date));
    }
  }

  private void branchExcuse() {
    List<Person> list =
        state.people.stream().filter(p -> p.active).collect(java.util.stream.Collectors.toList());
    new AlertDialog.Builder(this)
        .setTitle("Bu Hafta Şubeye Gitmeyecek Kişi")
        .setItems(
            list.stream().map(p -> p.name).toArray(String[]::new),
            (d, i) ->
                confirm(
                    "Şube Muafiyetini Teyit Edin",
                    list.get(i).name
                        + " "
                        + range()
                        + " haftasında şubeye hiç atanmayacak. İşyeri için uygunluğu devam eder."
                        + " Yalnız çakışan şube atamaları"
                        + " kaldırılır.",
                    () -> {
                      Leave leave =
                          new Leave(
                              list.get(i).id,
                              BRANCH,
                              selected,
                              selected.plusDays(6),
                              "Haftalık şube muafiyeti");
                      state.leaves.add(leave);
                      removeConflicts(leave);
                      if (save()) {
                        draft = generate(state, selected);
                        render();
                      }
                    }))
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  private void skip(Week w) {
    String id =
        w.duties.stream()
            .filter(d -> weekend(d.date) && d.place.equals(WORK))
            .map(d -> d.person)
            .findFirst()
            .orElse("");
    if (id.isEmpty()) {
      toast("Atlanacak hafta sonu nöbetçisi yok.");
      return;
    }
    LocalDate start =
        w.duties.stream()
            .filter(d -> weekend(d.date) && d.place.equals(WORK))
            .map(d -> d.date)
            .min(Comparator.naturalOrder())
            .orElse(selected.plusDays(5));
    dateTitled(
        "Mazeret Bitiş Tarihini Seçin",
        selected.plusDays(6),
        end -> {
          if (end.isBefore(start)) {
            toast("Bitiş ilk nöbet gününden önce olamaz.");
            return;
          }
          confirm(
              "Hafta Sonu Sırasını Atla",
              state.name(id)
                  + " için "
                  + start.format(shortDate)
                  + " – "
                  + end.format(shortDate)
                  + " tüm görev yerlerinde mazeret kaydedilsin mi?\n"
                  + "Mazeret bittikten sonraki ilk uygun hafta sonu öncelikli atanacak. Diğer"
                  + " haftaların çakışmayan atamaları korunur.",
              () -> {
                Leave leave = new Leave(id, ALL, start, end, "Hafta Sonu mazereti");
                state.leaves.add(leave);
                removeConflicts(leave);
                if (save()) {
                  draft = generate(state, selected);
                  render();
                }
              });
        });
  }

  private void edit(LocalDate day, String place) {
    Week original = draft != null ? draft : saved();
    if (original == null) return;
    if (weekend(day) && place.equals(WORK)) {
      replaceWeekend(original);
      return;
    }
    Week edited = copy(original);
    List<Person> options = new ArrayList<>();
    options.add(new Person("", "Boş Bırak"));
    options.addAll(
        state.people.stream().filter(p -> p.active).collect(java.util.stream.Collectors.toList()));
    new AlertDialog.Builder(this)
        .setTitle(day.format(shortDate) + " • " + state.placeName(place))
        .setItems(
            options.stream().map(p -> p.name).toArray(String[]::new),
            (dialog, i) -> {
              edited.duties.removeIf(d -> d.date.equals(day) && d.place.equals(place));
              Person p = options.get(i);
              if (!p.id.isEmpty()) edited.duties.add(new Duty(day, place, p.id));
              Runnable apply =
                  () -> {
                    List<String> invalid = validate(state, edited);
                    if (!invalid.isEmpty()) {
                      toast(String.join("\n", invalid));
                      return;
                    }
                    edited.warnings.add(
                        "Elle düzenleme: "
                            + day.format(shortDate)
                            + " "
                            + state.placeName(place)
                            + " → "
                            + p.name);
                    draft = edited;
                    render();
                  };
              Place location = state.place(place);
              List<String> assigned = new ArrayList<>();
              for (Duty duty : edited.duties)
                if (duty.place.equals(place) && duty.person.equals(p.id) && !duty.date.equals(day))
                  assigned.add(duty.date.format(shortDate));
              if (!p.id.isEmpty() && location.limit > 0 && assigned.size() >= location.limit) {
                confirm(
                    "Tekrar Atama Onayı",
                    p.name
                        + " daha önce "
                        + String.join(", ", assigned)
                        + " tarihinde "
                        + location.name
                        + " görevine atandı. Tekrar atamak istiyor musun?",
                    () -> {
                      edited.limitOverrides.add(place + "|" + p.id);
                      apply.run();
                    });
              } else apply.run();
            })
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  private void replaceWeekend(Week original) {
    String old =
        original.duties.stream()
            .filter(d -> weekend(d.date) && d.place.equals(WORK))
            .map(d -> d.person)
            .findFirst()
            .orElse("");
    List<Person> options =
        state.people.stream()
            .filter(p -> p.active && !p.id.equals(old))
            .collect(java.util.stream.Collectors.toList());
    if (options.isEmpty()) {
      toast("Başka aktif personel yok.");
      return;
    }
    List<LocalDate> weekendDays = workWeekendDays(state, selected);
    if (weekendDays.isEmpty()) {
      toast("Ana görev yerinde hafta sonu günü seçili değil; Ayarlar'dan düzenleyin.");
      return;
    }
    if (old.isEmpty()) {
      new AlertDialog.Builder(this)
          .setTitle("Seçili Hafta Sonu Günleri • Manuel Atama")
          .setItems(
              options.stream().map(p -> p.name).toArray(String[]::new),
              (dialog, i) -> {
                Week manual = copy(original);
                Person p = options.get(i);
                manual.duties.removeIf(d -> weekend(d.date) && d.place.equals(WORK));
                for (LocalDate day : weekendDays) manual.duties.add(new Duty(day, WORK, p.id));
                manual.cursor = p.id;
                manual.debts.removeIf(d -> d.person.equals(p.id));
                List<String> errors = validate(state, manual);
                if (!errors.isEmpty()) {
                  toast(String.join("\n", errors) + " Perşembe/cuma alanlarını önce boşaltın.");
                  return;
                }
                draft = manual;
                render();
              })
          .setNegativeButton("Vazgeç", null)
          .show();
      return;
    }
    new AlertDialog.Builder(this)
        .setTitle("Seçili Hafta Sonu Günlerini Devralacak Kişi")
        .setItems(
            options.stream().map(p -> p.name).toArray(String[]::new),
            (dialog, i) -> {
              Person replacement = options.get(i);
              if (!weekendAvailable(state, replacement.id, selected)) {
                toast("Seçilen kişinin hafta sonunda mazereti var.");
                return;
              }
              dateTitled(
                  "Devreden Kişinin Mazeret Bitişi",
                  selected.plusDays(6),
                  end -> {
                    if (end.isBefore(weekendDays.get(0))) {
                      toast("Bitiş ilk seçili nöbet gününden önce olamaz.");
                      return;
                    }
                    confirm(
                        "Hafta Sonu Değişimini Teyit Edin",
                        state.name(old)
                            + " yerine "
                            + replacement.name
                            + " seçili hafta sonu günlerine atanacak. Devreden kişi için mazeret"
                            + " bitişi: "
                            + end.format(shortDate)
                            + ". İki yerde de mazeret kaydedilir. Perşembe/cuma izinleri ve hafta"
                            + " içi atamalar yeniden hesaplanır. Diğer haftalar korunur; öneri"
                            + " ayrıca onayınızı ister.",
                        () -> {
                          if (!old.isEmpty()) {
                            Leave leave =
                                new Leave(old, ALL, weekendDays.get(0), end, "Hafta Sonu devri");
                            state.leaves.add(leave);
                            removeConflicts(leave);
                          }
                          if (save()) {
                            try {
                              draft = generate(state, selected, replacement.id);
                              render();
                            } catch (IllegalArgumentException e) {
                              toast(e.getMessage());
                            }
                          }
                        });
                  });
            })
        .setNegativeButton("Vazgeç", null)
        .show();
  }

  private void chooseVisiblePlaces(Runnable after) {
    List<Place> choices = statisticPlaces(state);
    if (choices.isEmpty()) {
      if (after != null) after.run();
      return;
    }
    boolean[] checked = new boolean[choices.size()];
    String[] names = new String[choices.size()];
    for (int i = 0; i < choices.size(); i++) {
      Place p = choices.get(i);
      checked[i] = !state.hiddenPlaces.contains(p.id);
      names[i] = p.name + (p.deleted ? " (Silindi)" : "");
    }
    new AlertDialog.Builder(this)
        .setTitle("Çizelge/PDF'de Gösterilecek Şubeler")
        .setMultiChoiceItems(names, checked, (dialog, which, value) -> checked[which] = value)
        .setNegativeButton("Vazgeç", null)
        .setPositiveButton(
            "Görünümü Kaydet",
            (dialog, which) -> {
              for (int i = 0; i < choices.size(); i++) {
                String id = choices.get(i).id;
                if (checked[i]) state.hiddenPlaces.remove(id);
                else state.hiddenPlaces.add(id);
              }
              if (save()) {
                if (after != null) after.run();
                else render();
              }
            })
        .show();
  }

  private List<String> activeWeekendIds() {
    List<String> ids = new ArrayList<>();
    for (Person person : weekendOrder(state)) if (person.active) ids.add(person.id);
    return ids;
  }

  private List<String> editableWeekendIds() {
    List<String> active = activeWeekendIds();
    if (weekendOrderDraft != null
        && (weekendOrderDraft.size() != active.size()
            || !new HashSet<>(weekendOrderDraft).equals(new HashSet<>(active))))
      weekendOrderDraft = null;
    return weekendOrderDraft == null ? active : new ArrayList<>(weekendOrderDraft);
  }

  private void moveWeekendPerson(String id, int delta) {
    List<String> ids = editableWeekendIds();
    int from = ids.indexOf(id), to = from + delta;
    if (from < 0 || to < 0 || to >= ids.size()) return;
    weekendOrderDraft = new ArrayList<>(ids);
    Collections.swap(weekendOrderDraft, from, to);
    render();
  }

  private void confirmWeekendOrder(List<String> ids) {
    StringBuilder preview = new StringBuilder();
    for (int n = 0; n < ids.size(); n++)
      preview.append(n + 1).append(". Sıra • ").append(state.name(ids.get(n))).append("\n");
    confirm(
        "Hafta Sonu Nöbet Sırasını Kaydet",
        preview
            + "\n"
            + "Sonraki otomatik dağıtım 1. sıradan başlar. Onaylı listeler değişmez; bekleyen"
            + " mazeret hakları korunur.",
        () -> {
          setWeekendOrder(state, ids);
          if (save()) {
            weekendOrderDraft = null;
            draft = null;
            render();
          }
        });
  }

  private void weekendSettings() {
    heading("Hafta Sonu Nöbet Sırası");
    note(
        "↑ / ↓ ile personelleri taşıyın, ardından Sırayı Kaydet'e dokunun. Yeni sıra sonraki"
            + " otomatik öneride 1. sıradan başlar; onaylı listeler ve bekleyen mazeret hakları"
            + " korunur.");
    List<String> ids = editableWeekendIds();
    for (int i = 0; i < ids.size(); i++) {
      String id = ids.get(i);
      LinearLayout row = new LinearLayout(this);
      Button name =
          button(
              (i + 1) + ". Sıra • " + state.name(id),
              () -> {
                String[] positions = new String[ids.size()];
                for (int n = 0; n < positions.length; n++) positions[n] = (n + 1) + ". Sıra";
                new AlertDialog.Builder(this)
                    .setTitle(state.name(id) + " • Yeni Sıra")
                    .setSingleChoiceItems(
                        positions,
                        ids.indexOf(id),
                        (dialog, which) -> {
                          dialog.dismiss();
                          List<String> reordered = new ArrayList<>(ids);
                          reordered.remove(id);
                          reordered.add(which, id);
                          confirmWeekendOrder(reordered);
                        })
                    .setNegativeButton("Vazgeç", null)
                    .show();
              });
      name.setTextSize(13);
      name.setMinWidth(0);
      name.setMinimumWidth(0);
      name.setPadding(dp(5), 0, dp(5), 0);
      row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
      for (int delta : new int[] {-1, 1}) {
        Button arrow = button(delta < 0 ? "↑" : "↓", () -> moveWeekendPerson(id, delta));
        arrow.setTag((delta < 0 ? "weekend-up-" : "weekend-down-") + id);
        arrow.setContentDescription(
            state.name(id) + (delta < 0 ? " • Yukarı Taşı" : " • Aşağı Taşı"));
        arrow.setMinWidth(0);
        arrow.setMinimumWidth(0);
        arrow.setPadding(0, 0, 0, 0);
        arrow.setEnabled(delta < 0 ? i > 0 : i < ids.size() - 1);
        if (!arrow.isEnabled()) arrow.setAlpha(.45f);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(44), dp(48)));
      }
      body.addView(row);
    }
    if (weekendOrderDraft != null) {
      action("Sırayı Kaydet", () -> confirmWeekendOrder(new ArrayList<>(weekendOrderDraft)));
      action(
          "Sıra Değişikliklerini İptal Et",
          () -> {
            weekendOrderDraft = null;
            render();
          });
    }
    action("+ Sıraya Yeni Personel Ekle", () -> personDialog(null));
  }

  private void settings() {
    weekendSettings();
    heading("Ayarlar • Görev Yerleri");
    note(
        "Yer adını değiştirmek geçmiş nöbetleri silmez. Çizelge, PDF ve istatistiklerde yeni ad"
            + " görünür. İlk iki yerin de günlerini değiştirebilirsiniz; hiç gün seçmezseniz yeni"
            + " planlarda atama yapılmaz. Ana yerde seçili iki hafta sonu günü varsa aynı kişi"
            + " atanır.");
    action("+ Yeni Şube / Görev Yeri Ekle", () -> placeDialog(null));
    for (Place place : state.places) {
      if (place.deleted) continue;
      heading(place.name);
      StringBuilder days = new StringBuilder();
      String[] names = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};
      for (int i = 0; i < 7; i++)
        if ((place.days & (1 << i)) != 0) days.append(names[i]).append(" ");
      if (days.length() == 0) days.append("Kapalı (gün seçilmedi)");
      note(
          days
              + " • Kişi başı haftalık: "
              + (place.limit == 0 ? "Sınırsız" : place.limit)
              + " • "
              + (place.automatic ? "Otomatik Dağıtım" : "Yalnız Manuel"));
      action("Adı / Kuralları Düzenle", () -> placeDialog(place));
      if (!place.id.equals(WORK) && !place.id.equals(BRANCH))
        action(
            "Şubeyi Sil",
            () ->
                confirm(
                    "Görev Yerini Sil",
                    place.name
                        + " yeni dağıtımlardan çıkarılacak. Geçmiş ve bugünkü"
                        + " nöbetler/istatistikler korunur; yarın ve sonraki atamaları kaldırılır."
                        + " Silinen yerler bölümünden geri alabilirsiniz.",
                    () -> {
                      deletePlace(state, place.id, LocalDate.now());
                      if (save()) {
                        draft = null;
                        render();
                        toast("Şube silindi; geçmiş kayıtlar korundu.");
                      }
                    }));
    }
    List<Place> deleted = new ArrayList<>();
    for (Place p : state.places) if (p.deleted) deleted.add(p);
    if (!deleted.isEmpty())
      action(
          "Silinen Görev Yerleri (" + deleted.size() + ")",
          () ->
              new AlertDialog.Builder(this)
                  .setTitle("Geri Alınacak Görev Yerini Seçin")
                  .setItems(
                      deleted.stream().map(p -> p.name).toArray(String[]::new),
                      (d, i) -> {
                        Place p = deleted.get(i);
                        confirm(
                            "Şubeyi Geri Al",
                            p.name
                                + " yeniden kullanılabilecek. Kaldırılmış gelecek atamaları"
                                + " kendiliğinden geri gelmez.",
                            () -> {
                              p.deleted = false;
                              if (save()) {
                                draft = null;
                                render();
                              }
                            });
                      })
                  .setNegativeButton("Vazgeç", null)
                  .show());
    note(
        "Yeni bir yer eklendiğinde eski listelere kendiliğinden personel yazılmaz. Kaydedilmiş"
            + " haftayı açıp yer alanlarına dokunarak düzenleyebilir veya yeni öneri"
            + " oluşturabilirsiniz.");
  }

  private void placeDialog(Place current) {
    if (current == null && state.places.stream().filter(p -> !p.deleted).count() >= 20) {
      toast("En fazla 20 görev yeri eklenebilir.");
      return;
    }
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(dp(20), dp(8), dp(20), dp(8));
    EditText name = new EditText(this);
    name.setSingleLine(true);
    name.setHint("Görev Yeri Adı");
    if (current != null) name.setText(current.name);
    box.addView(name);
    boolean fixed = current != null && (current.id.equals(WORK) || current.id.equals(BRANCH));
    CheckBox[] days = new CheckBox[7];
    String[] labels = {"Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"};
    for (int i = 0; i < 7; i++) {
      days[i] = new CheckBox(this);
      days[i].setText(labels[i]);
      days[i].setChecked(((current == null ? 31 : current.days) & (1 << i)) != 0);
      days[i].setEnabled(true);
      box.addView(days[i]);
    }
    TextView limitLabel = text("Kişi Başı Haftalık Üst Sınır (0 = Sınırsız)", 14, ink);
    box.addView(limitLabel);
    EditText limit = new EditText(this);
    limit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    limit.setText(getString(R.string.limit_value, current == null ? 1 : current.limit));
    limit.setEnabled(!fixed);
    box.addView(limit);
    CheckBox automatic = new CheckBox(this);
    automatic.setText(R.string.auto_place);
    automatic.setChecked(current == null || current.automatic);
    automatic.setEnabled(!fixed);
    box.addView(automatic);
    ScrollView scroll = new ScrollView(this);
    scroll.addView(box);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(current == null ? "Yeni Görev Yeri" : "Görev Yeri Ayarları")
            .setView(scroll)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Kaydet", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      String value = name.getText().toString().trim().replaceAll("\\s+", " ");
                      if (value.isEmpty() || value.length() > 40) {
                        name.setError("1–40 karakter girin.");
                        return;
                      }
                      if (state.places.stream()
                          .anyMatch(p -> p != current && p.name.equalsIgnoreCase(value))) {
                        name.setError(
                            "Bu ad kayıtlı. Silindiyse Silinen görev yerleri bölümünden geri"
                                + " alın.");
                        return;
                      }
                      int mask = 0;
                      for (int i = 0; i < 7; i++) if (days[i].isChecked()) mask |= 1 << i;
                      int max;
                      try {
                        max = Integer.parseInt(limit.getText().toString());
                        if (max < 0 || max > 7) throw new NumberFormatException();
                      } catch (NumberFormatException e) {
                        limit.setError("0–7 arasında girin.");
                        return;
                      }

                      if (current == null) {
                        Place p = new Place(UUID.randomUUID().toString(), value, mask, max);
                        p.automatic = automatic.isChecked();
                        state.places.add(p);
                      } else {
                        current.name = value;
                        current.days = mask;
                        current.limit = max;
                        current.automatic = automatic.isChecked();
                      }
                      if (save()) {
                        draft = null;
                        dialog.dismiss();
                        render();
                        toast(
                            "Kaydedildi. Seçili günler yeni dağıtımlarda uygulanır; mevcut listeler"
                                + " ve geçmiş korunur.");
                      }
                    }));
    dialog.show();
  }

  private void stats() {
    heading("Nöbet İstatistikleri");
    action(
        "İstatistikleri PDF Olarak Paylaş",
        () -> {
          try {
            PdfExport.share(this, PdfExport.createStatistics(this, state, LocalDate.now()));
          } catch (Exception e) {
            toast("İstatistik PDF'si oluşturulamadı veya paylaşım uygulaması bulunamadı.");
          }
        });
    heading("Bugün Ve Öncesi");
    statisticsTable(false);
    List<StatRow> planned = statistics(state, LocalDate.now(), true);
    boolean any = false;
    for (StatRow row : planned) for (long count : row.counts) if (count > 0) any = true;
    if (any) {
      heading("Gelecek Atamalar");
      statisticsTable(true);
    }
  }

  private void statisticsTable(boolean future) {
    List<String> labels = PdfExport.statisticsLabels(state);
    List<StatRow> rows = statistics(state, LocalDate.now(), future);
    // Extra branches continue vertically to keep every column readable on narrow screens.
    float density = getResources().getDisplayMetrics().density;
    int available = (int) (getResources().getDisplayMetrics().widthPixels / density) - 36;
    int columns = Math.max(2, Math.min(5, (available - 116) / 62));
    for (int first = 0; first < labels.size(); first += columns) {
      int end = Math.min(labels.size(), first + columns);
      LinearLayout table = new LinearLayout(this);
      table.setOrientation(LinearLayout.VERTICAL);
      table.setTag("statistics-table");
      LinearLayout header = new LinearLayout(this);
      addStatCell(header, "Personel", true, true, false);
      for (int i = first; i < end; i++) addStatCell(header, labels.get(i), false, true, false);
      table.addView(header);
      int index = 0;
      for (StatRow row : rows) {
        LinearLayout line = new LinearLayout(this);
        boolean stripe = index++ % 2 != 0;
        addStatCell(
            line, row.person.name + (row.person.active ? "" : " · Pasif"), true, false, stripe);
        for (int i = first; i < end; i++)
          addStatCell(line, Long.toString(row.counts[i]), false, false, stripe);
        table.addView(line);
      }
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
      params.bottomMargin = dp(8);
      body.addView(table, params);
    }
    if (rows.isEmpty()) note("Henüz Personel Eklenmedi.");
  }

  private void addStatCell(
      LinearLayout row, String value, boolean name, boolean header, boolean stripe) {
    TextView cell = text(value, header ? 12 : 13, header ? Color.WHITE : ink);
    cell.setPadding(dp(5), dp(7), dp(5), dp(7));
    cell.setMinWidth(0);
    cell.setGravity((name ? Gravity.START : Gravity.CENTER_HORIZONTAL) | Gravity.CENTER_VERTICAL);
    cell.setBackgroundColor(header ? Color.rgb(42, 111, 177) : stripe ? surface : cardColor);
    if (header) cell.setTypeface(null, Typeface.BOLD);
    row.addView(cell, new LinearLayout.LayoutParams(0, -1, name ? 1.85f : 1f));
  }
}
