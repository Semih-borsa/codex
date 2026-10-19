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
  private boolean failed = false;
  private Week draft;
  private int ink = Color.rgb(19, 38, 60),
      teal = Color.rgb(31, 90, 137),
      muted = Color.rgb(85, 105, 124),
      surface = Color.rgb(242, 246, 250),
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
    }
    render();
    if (failed)
      new AlertDialog.Builder(this)
          .setTitle("Veriler okunamadı")
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
    b.setTextColor(teal);
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
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
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
    root.addView(text("Planla • Onayla • Adil dağıt", 13, muted));
    root.addView(
        button(
            "Görünüm: " + (themeMode == 0 ? "Sistem" : themeMode == 1 ? "Açık" : "Koyu"),
            () ->
                new AlertDialog.Builder(this)
                    .setTitle("Kurumsal renk teması")
                    .setSingleChoiceItems(
                        new String[] {"Sistem teması", "Açık • Lacivert", "Koyu • Lacivert"},
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
    String[] titles = {"Çizelge", "Personel", "Mazeret", "İstatistik"};
    for (int i = 0; i < titles.length; i++) {
      final int idx = i;
      Button b =
          button(
              titles[i],
              () -> {
                screen = idx;
                render();
              });
      b.setTextSize(12);
      if (i == screen) {
        b.setTypeface(null, Typeface.BOLD);
        b.setTextColor(ink);
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
    setContentView(root);
    if (screen == 0) program();
    else if (screen == 1) people();
    else if (screen == 2) leaves();
    else stats();
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
            "Hafta seç",
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
        "İşyeri her gün • Şube hafta içi, kişi başı en fazla 1 gün. Hafta sonu aynı kişi; perşembe"
            + " ve cuma iki yerde de izinli.");
    if (state.people.stream().noneMatch(p -> p.active)) {
      note(
          "Başlamak için Personel bölümüne 5 kişinin adını ekleyin. Sonradan ekleyebilir, pasife"
              + " alabilir veya yeniden etkinleştirebilirsiniz.");
      action(
          "Personel ekle",
          () -> {
            screen = 1;
            render();
          });
      return;
    }
    action(
        "Otomatik dağıtım öner",
        () -> {
          try {
            draft = generate(state, selected);
            render();
          } catch (IllegalArgumentException e) {
            toast(e.getMessage());
          }
        });
    action(
        "Boş çizelge aç • manuel oluştur",
        () ->
            confirm(
                "Manuel çizelge",
                "Bu hafta için boş bir öneri açılacak. Mevcut kayıt, siz onaylayana kadar"
                    + " değişmeyecek. Günlerdeki işyeri/şube alanlarına dokunarak personel seçin.",
                () -> {
                  draft = new Week(selected);
                  Week previous =
                      state.weeks.stream()
                          .filter(x -> x.monday.isBefore(selected))
                          .max(Comparator.comparing(x -> x.monday))
                          .orElse(null);
                  if (previous != null) {
                    draft.cursor = previous.cursor;
                    for (Debt d : previous.debts) draft.debts.add(new Debt(d.person, d.end));
                  }
                  draft.warnings.add(
                      "Manuel çizelge: boş alanları ve mazeretleri kontrol ederek onaylayın.");
                  render();
                }));
    Week w = draft != null ? draft : saved();
    if (w == null) {
      note("Bu hafta için henüz onaylı çizelge yok.");
      return;
    }
    boolean preview = draft != null;
    heading(preview ? "Onay bekleyen öneri" : "Onaylı çizelge");
    LocalDate sat = selected.plusDays(5);
    String weekendPerson = PdfExport.person(state, w, sat, WORK);
    note("Hafta sonu: " + weekendPerson + "\nPerşembe/cuma dinlenme: " + weekendPerson);
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
      for (String place : new String[] {WORK, BRANCH}) {
        if (weekend(day) && place.equals(BRANCH)) continue;
        String label = place + ": " + PdfExport.person(state, w, day, place);
        card.addView(button(label + "  ✎", () -> edit(day, place)));
      }
      if (day.getDayOfWeek().getValue() == 4 || day.getDayOfWeek().getValue() == 5)
        card.addView(text("Dinlenme: " + weekendPerson, 13, muted));
    }
    for (String warning : w.warnings) body.addView(text("• " + warning, 14, warningColor));
    if (preview) {
      action("Dağıtımı onayla ve kaydet", () -> approveDraft());
      action(
          "Öneriyi iptal et",
          () -> {
            draft = null;
            render();
          });
    }
    action("Hafta sonu nöbetçisini atla / mazeret gir", () -> skip(w));
    action("Bu hafta şubeye gitmeyecek personel", () -> branchExcuse());
    if (!preview)
      action(
          "PDF oluştur ve paylaş",
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
    if (!w.debts.isEmpty()) {
      heading("Bekleyen hafta sonu sırası");
      for (Debt d : w.debts)
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
    long missing = 12 - draft.duties.size();
    String msg =
        "Atama şekli: işyeri her gün, şube hafta içi kişi başı en fazla 1 gün; hafta sonu nöbetçisi"
            + " perşembe/cuma izinli.\n\n"
            + (missing > 0
                ? missing + " boş nöbet alanı var. Eksik çizelge olarak kaydedilecek.\n\n"
                : "")
            + String.join("\n", draft.warnings);
    if (state.weeks.stream().anyMatch(w -> w.monday.isAfter(selected)))
      msg +=
          "\n\n"
              + "Bu tarihten sonraki onaylı haftalar silinip yeniden planlanacak; sıra ve"
              + " istatistikler değişecek.";
    confirm(
        "Dağıtımı teyit edin",
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
        "Sıra, ilk hafta sonu döngüsünün sırasıdır. Pasife alınan kişi geçmiş kayıtlarda tutulur ve"
            + " yeni atamalarda kullanılmaz.");
    action("+ Personel ekle", () -> personDialog(null));
    for (Person p : state.people) {
      heading(p.name + (p.active ? "" : " · Pasif"));
      LinearLayout row = new LinearLayout(this);
      row.addView(button("Adı düzenle", () -> personDialog(p)));
      row.addView(
          button(
              p.active ? "Personeli çıkar" : "Yeniden etkinleştir",
              () ->
                  confirm(
                      "Personel durumu",
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
    input.setHint("Ad soyad");
    if (p != null) input.setText(p.name);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle(p == null ? "Personel ekle" : "Adı düzenle")
            .setView(input)
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
                      if (p == null)
                        state.people.add(new Person(UUID.randomUUID().toString(), name));
                      else p.name = name;
                      if (save()) {
                        dialog.dismiss();
                        draft = null;
                        render();
                      }
                    }));
    dialog.show();
  }

  private void leaves() {
    heading("Mazeret ve kısıtlar");
    note(
        "Senelik izin, rapor ve diğer genel mazeretler her iki yerde de atamayı engeller. Yalnız"
            + " şube/işyeri kısıtları ayrıca seçilebilir. Başlangıç ve bitiş günleri dahildir.");
    action(
        "+ Tarihli mazeret / kısıt ekle",
        () -> leaveDialog(null, ALL, selected, selected.plusDays(6), "Senelik izin"));
    for (Leave l : state.leaves) {
      TextView t =
          text(
              state.name(l.person)
                  + " • "
                  + l.reason
                  + "\n"
                  + l.start.format(shortDate)
                  + " – "
                  + l.end.format(shortDate)
                  + " • "
                  + l.place,
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
                      "Mazereti sil",
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

  private void leaveDialog(
      Leave existing, String place, LocalDate start, LocalDate end, String reason) {
    List<Person> list =
        state.people.stream()
            .filter(p -> p.active || existing != null && p.id.equals(existing.person))
            .collect(java.util.stream.Collectors.toList());
    if (list.isEmpty()) {
      toast("Önce personel ekleyin.");
      return;
    }
    LinearLayout box = new LinearLayout(this);
    box.setPadding(dp(20), dp(8), dp(20), dp(8));
    box.setOrientation(LinearLayout.VERTICAL);
    Spinner who = spinner(list.stream().map(p -> p.name).toArray(String[]::new));
    box.addView(who);
    if (existing != null)
      for (int i = 0; i < list.size(); i++)
        if (list.get(i).id.equals(existing.person)) who.setSelection(i);
    Spinner type =
        spinner(
            new String[] {
              "Senelik izin", "Rapor", "Diğer mazeret", "Yalnız şube kısıtı", "Yalnız işyeri kısıtı"
            });
    int index =
        reason.equals("Rapor")
            ? 1
            : reason.equals("Senelik izin")
                ? 0
                : place.equals(BRANCH) ? 3 : place.equals(WORK) ? 4 : 2;
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
    box.addView(from);
    box.addView(to);
    AlertDialog dialog =
        new AlertDialog.Builder(this)
            .setTitle("Tarihli mazeret")
            .setView(box)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Teyit et", null)
            .create();
    dialog.setOnShowListener(
        x ->
            dialog
                .getButton(-1)
                .setOnClickListener(
                    v -> {
                      if (dates[1].isBefore(dates[0])) {
                        toast("Bitiş tarihi başlangıçtan önce olamaz.");
                        return;
                      }
                      String person = list.get(who.getSelectedItemPosition()).id;
                      int t = type.getSelectedItemPosition();
                      String target = t == 3 ? BRANCH : t == 4 ? WORK : ALL;
                      String label = type.getSelectedItem().toString();
                      confirm(
                          "Mazereti kaydet",
                          state.name(person)
                              + "\n"
                              + dates[0].format(shortDate)
                              + " – "
                              + dates[1].format(shortDate)
                              + "\n"
                              + target
                              + " için atanmayacak.\n"
                              + "Çakışan mevcut atamalar kaldırılacak. Etkilenen ilk haftadan"
                              + " sonraki planlar sıra tutarlılığı için silinecek; yeniden dağıtım"
                              + " onayınızı ister.",
                          () -> {
                            if (existing != null) {
                              state.leaves.remove(existing);
                              for (Week wk : state.weeks)
                                for (Debt debt : wk.debts)
                                  if (debt.person.equals(existing.person)
                                      && debt.end.equals(existing.end)) debt.end = dates[1];
                            }
                            Leave l = new Leave(person, target, dates[0], dates[1], label);
                            state.leaves.add(l);
                            removeConflicts(l);
                            if (save()) {
                              dialog.dismiss();
                              draft = null;
                              render();
                              toast("Mazeret kaydedildi. Boşalan günler için dağıtım önerin.");
                            }
                          });
                    }));
    dialog.show();
  }

  private void removeConflicts(Leave l) {
    LocalDate earliest =
        state.weeks.stream()
            .filter(w -> w.duties.stream().anyMatch(d -> l.blocks(d.person, d.place, d.date)))
            .map(w -> w.monday)
            .min(Comparator.naturalOrder())
            .orElse(null);
    if (earliest != null) state.weeks.removeIf(w -> w.monday.isAfter(earliest));
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
        .setTitle("Bu hafta şubeye gitmeyecek kişi")
        .setItems(
            list.stream().map(p -> p.name).toArray(String[]::new),
            (d, i) ->
                confirm(
                    "Şube muafiyetini teyit edin",
                    list.get(i).name
                        + " "
                        + range()
                        + " haftasında şubeye hiç atanmayacak. İşyeri için uygunluğu devam eder."
                        + " Çakışan şube atamaları ve etkilenen haftadan sonraki planlar"
                        + " kaldırılır.",
                    () -> {
                      Leave leave =
                          new Leave(
                              list.get(i).id,
                              BRANCH,
                              selected,
                              selected.plusDays(4),
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
            .filter(d -> d.date.equals(selected.plusDays(5)) && d.place.equals(WORK))
            .map(d -> d.person)
            .findFirst()
            .orElse("");
    if (id.isEmpty()) {
      toast("Atlanacak hafta sonu nöbetçisi yok.");
      return;
    }
    dateTitled(
        "Mazeret bitiş tarihini seçin",
        selected.plusDays(6),
        end -> {
          if (end.isBefore(selected.plusDays(5))) {
            toast("Bitiş en az cumartesi olmalı.");
            return;
          }
          confirm(
              "Hafta sonu sırasını atla",
              state.name(id)
                  + " için "
                  + selected.plusDays(5).format(shortDate)
                  + " – "
                  + end.format(shortDate)
                  + " her iki yerde mazeret kaydedilsin mi?\n"
                  + "Mazeret bittikten sonraki ilk uygun hafta sonu öncelikli atanacak. Etkilenen"
                  + " haftadan sonraki planlar yeniden onaylanmak üzere kaldırılır.",
              () -> {
                Leave leave = new Leave(id, ALL, selected.plusDays(5), end, "Hafta sonu mazereti");
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
    if (weekend(day)) {
      replaceWeekend(original);
      return;
    }
    Week edited = copy(original);
    List<Person> options = new ArrayList<>();
    options.add(new Person("", "BOŞ bırak"));
    options.addAll(
        state.people.stream().filter(p -> p.active).collect(java.util.stream.Collectors.toList()));
    new AlertDialog.Builder(this)
        .setTitle(day.format(shortDate) + " • " + place)
        .setItems(
            options.stream().map(p -> p.name).toArray(String[]::new),
            (dialog, i) -> {
              edited.duties.removeIf(d -> d.date.equals(day) && d.place.equals(place));
              Person p = options.get(i);
              if (!p.id.isEmpty()) edited.duties.add(new Duty(day, place, p.id));
              List<String> invalid = validate(state, edited);
              if (!invalid.isEmpty()) {
                toast(String.join("\n", invalid));
                return;
              }
              edited.warnings.add(
                  "Elle düzenleme: " + day.format(shortDate) + " " + place + " → " + p.name);
              draft = edited;
              render();
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
    if (old.isEmpty()) {
      new AlertDialog.Builder(this)
          .setTitle("Cumartesi + pazar • manuel atama")
          .setItems(
              options.stream().map(p -> p.name).toArray(String[]::new),
              (dialog, i) -> {
                Week manual = copy(original);
                Person p = options.get(i);
                manual.duties.removeIf(d -> weekend(d.date) && d.place.equals(WORK));
                manual.duties.add(new Duty(selected.plusDays(5), WORK, p.id));
                manual.duties.add(new Duty(selected.plusDays(6), WORK, p.id));
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
        .setTitle("Cumartesi + pazar devralacak kişi")
        .setItems(
            options.stream().map(p -> p.name).toArray(String[]::new),
            (dialog, i) -> {
              Person replacement = options.get(i);
              if (!available(state, replacement.id, WORK, selected.plusDays(5))
                  || !available(state, replacement.id, WORK, selected.plusDays(6))) {
                toast("Seçilen kişinin hafta sonunda mazereti var.");
                return;
              }
              dateTitled(
                  "Devreden kişinin mazeret bitişi",
                  selected.plusDays(6),
                  end -> {
                    if (end.isBefore(selected.plusDays(5))) {
                      toast("Bitiş cumartesiden önce olamaz.");
                      return;
                    }
                    confirm(
                        "Hafta sonu değişimini teyit edin",
                        state.name(old)
                            + " yerine "
                            + replacement.name
                            + " iki gün atanacak. Devreden kişi için mazeret bitişi: "
                            + end.format(shortDate)
                            + ". İki yerde de mazeret kaydedilir. Perşembe/cuma izinleri ve hafta"
                            + " içi atamalar yeniden hesaplanır. Etkilenen sonraki planlar"
                            + " kaldırılır; öneri ayrıca onayınızı ister.",
                        () -> {
                          if (!old.isEmpty()) {
                            Leave leave =
                                new Leave(old, ALL, selected.plusDays(5), end, "Hafta sonu devri");
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

  private void stats() {
    heading("Nöbet istatistikleri");
    note(
        "Onaylı çizelgelerde bugün ve önceki tarihler sayılır; gelecek atamalar ayrıca gösterilir."
            + " Boş alanlar ve taslaklar sayılmaz. Hafta sonu döngüsü bir blok, istatistikte"
            + " cumartesi+pazar iki nöbet günüdür.");
    long weeks = state.weeks.size();
    note(
        weeks
            + " onaylı hafta • "
            + state.people.stream().filter(p -> p.active).count()
            + " aktif personel");
    for (Person p : state.people) {
      heading(p.name + (p.active ? "" : " · Pasif"));
      for (String place : new String[] {WORK, BRANCH}) {
        long weekday = 0, weekend = 0, future = 0;
        for (Week w : state.weeks)
          for (Duty d : w.duties)
            if (d.person.equals(p.id) && d.place.equals(place)) {
              if (d.date.isAfter(LocalDate.now())) future++;
              else if (weekend(d.date)) weekend++;
              else weekday++;
            }
        note(
            place
                + " → Hafta içi: "
                + weekday
                + "  |  Hafta sonu: "
                + weekend
                + "\nGerçekleşme tarihine ulaşan toplam: "
                + (weekday + weekend)
                + " gün  •  Gelecek: "
                + future);
      }
    }
  }
}
