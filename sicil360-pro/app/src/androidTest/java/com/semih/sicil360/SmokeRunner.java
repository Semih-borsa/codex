package com.semih.sicil360;

import static com.semih.sicil360.Model.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Uses only synthetic contacts, isolated private storage and intercepted intents; never sends. */
public final class SmokeRunner extends Instrumentation {
  int passed, failed;
  File output;
  Context isolated;

  interface Check {
    void run() throws Exception;
  }

  interface Condition {
    boolean get();
  }

  @Override
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  void truth(boolean v) {
    if (!v) throw new AssertionError("condition failed");
  }

  void check(String name, Check run) {
    Bundle b = new Bundle();
    b.putString("test", name);
    try {
      run.run();
      passed++;
      b.putString("status", "PASS");
    } catch (Throwable e) {
      failed++;
      StringWriter s = new StringWriter();
      e.printStackTrace(new PrintWriter(s));
      b.putString("status", "FAIL");
      b.putString("trace", s.toString());
    }
    sendStatus(0, b);
  }

  void await(Condition condition) throws Exception {
    long deadline = SystemClock.uptimeMillis() + 20000;
    boolean[] ok = {false};
    while (SystemClock.uptimeMillis() < deadline) {
      runOnMainSync(() -> ok[0] = condition.get());
      if (ok[0]) return;
      Thread.sleep(80);
    }
    truth(false);
  }

  TextView find(View v, String text) {
    if (v instanceof TextView && ((TextView) v).getText().toString().equals(text))
      return (TextView) v;
    if (v instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
        TextView found = find(((ViewGroup) v).getChildAt(i), text);
        if (found != null) return found;
      }
    return null;
  }

  Person sample(String id) {
    Person p = new Person();
    p.sicil = id;
    p.ad = "İpek";
    p.soyad = "Işık";
    p.telefon = "05320000000";
    p.birim = "Örnek Birim";
    return p;
  }

  MainActivity open() throws Exception {
    MainActivity a =
        (MainActivity)
            startActivitySync(
                new Intent(getTargetContext(), MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    await(() -> a.state != null && !a.busy);
    return a;
  }

  void snapshot(MainActivity a, String file) throws Exception {
    waitForIdleSync();
    Thread.sleep(250);
    runOnMainSync(
        () -> {
          View view = a.getWindow().getDecorView();
          Bitmap bitmap =
              Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
          view.draw(new Canvas(bitmap));
          try (FileOutputStream out = new FileOutputStream(new File(output, file))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
          } catch (Exception e) {
            throw new AssertionError(e);
          } finally {
            bitmap.recycle();
          }
        });
  }

  void setup(MainActivity a, State s) throws Exception {
    Vault v = new Vault(isolated);
    State current = v.load();
    State saved = v.commit(s, current.revision, false);
    runOnMainSync(
        () -> {
          a.vault = v;
          a.state = saved;
          a.selected = "";
          a.query = "";
          a.screen = 0;
          a.render();
        });
  }

  void close(MainActivity a) {
    runOnMainSync(
        () -> {
          for (android.app.Dialog d : new ArrayList<>(a.dialogs)) d.dismiss();
          a.finish();
        });
  }

  @Override
  public void onStart() {
    Context target = getTargetContext();
    output = new File(target.getCacheDir(), "validation");
    output.mkdirs();
    File files = new File(output, "isolated");
    files.mkdirs();
    isolated =
        new ContextWrapper(target) {
          @Override
          public File getFilesDir() {
            return files;
          }
        };
    check(
        "Device encryption atomic revision rejection reopen undo and preserved history",
        () -> {
          Vault v = new Vault(isolated);
          new android.util.AtomicFile(new File(files, "sicil360.enc")).delete();
          new android.util.AtomicFile(v.undo).delete();
          State s = new State();
          Person p = sample("000123");
          s.people.put(p.sicil, p);
          State first = v.commit(s, 0, true);
          truth(first.revision == 1);
          truth(v.load().people.get("000123").telefon.equals(p.telefon));
          byte[] a = java.nio.file.Files.readAllBytes(v.file.getBaseFile().toPath());
          truth(!new String(a, java.nio.charset.StandardCharsets.ISO_8859_1).contains(p.telefon));
          State next = Codec.copy(first);
          next.people.clear();
          State second = v.commit(next, 1, true);
          Event event = new Event();
          event.sicil = "000123";
          second.history.add(event);
          State third = v.commit(second, 2, false);
          State restored = v.restoreUndo(third.revision);
          truth(restored.people.size() == 1 && restored.history.size() == 1 && !v.undo.exists());
          boolean rejected = false;
          try {
            v.commit(first, 0, false);
          } catch (IOException expected) {
            rejected = true;
          }
          truth(rejected);
          byte[] ciphertext = v.encrypt(Codec.encode(restored));
          truth(!Arrays.equals(ciphertext, v.encrypt(Codec.encode(restored))));
          ciphertext[ciphertext.length - 1] ^= 1;
          boolean corrupt = false;
          try {
            v.decrypt(ciphertext);
          } catch (Exception expected) {
            corrupt = true;
          }
          truth(corrupt);
        });
    check(
        "Android XLSX parser export import first row zeros Turkish and DTD rejection",
        () -> {
          File f = new File(output, "people.xlsx");
          Person p = sample("000123");
          try (OutputStream out = new FileOutputStream(f)) {
            Excel.export(out, Arrays.asList(p));
          }
          Excel.Table table = Excel.load(f, Excel.sheets(f).get(0));
          truth(table.value(2, 0).equals("000123"));
          truth(table.value(2, 3).equals("05320000000"));
          truth(table.value(2, 1).equals("İpek"));
          List<Row> rows = table.mapped(1, new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8}, false);
          truth(rows.size() == 1);
          truth(plan(new State(), rows, ImportMode.KEEP, Collections.emptyMap()).added == 1);
          File evil = new File(output, "evil.xlsx");
          try (java.util.zip.ZipOutputStream z =
              new java.util.zip.ZipOutputStream(new FileOutputStream(evil))) {
            Excel.entry(z, "[Content_Types].xml", "<Types/>");
            Excel.entry(
                z,
                "xl/workbook.xml",
                "<!DOCTYPE a [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><a>&e;</a>");
            Excel.entry(z, "xl/_rels/workbook.xml.rels", "<Relationships/>");
          }
          boolean denied = false;
          try {
            Excel.sheets(evil);
          } catch (Exception expected) {
            denied = true;
          }
          truth(denied);
        });
    check(
        "Portable encrypted backup Android password wrong password tamper restore",
        () -> {
          State s = new State();
          s.people.put("0001", sample("0001"));
          byte[] encoded = Codec.encode(s);
          char[] password = "test-password-123".toCharArray();
          byte[] encrypted = Backup.seal(encoded, password);
          truth(Codec.decode(Backup.open(encrypted, password)).people.containsKey("0001"));
          boolean rejected = false;
          try {
            Backup.open(encrypted, "different-password".toCharArray());
          } catch (Exception expected) {
            rejected = true;
          }
          truth(rejected);
          encrypted[encrypted.length - 1] ^= 1;
          rejected = false;
          try {
            Backup.open(encrypted, password);
          } catch (Exception expected) {
            rejected = true;
          }
          truth(rejected);
        });
    check(
        "UI exact identity literal template confirmation cancel normal Business and absent routes",
        () -> {
          MainActivity a = open();
          try {
            State s = new State();
            s.people.put("000123", sample("000123"));
            s.people.put("000124", sample("000124"));
            Template t = new Template();
            t.name = "İşlem Bilgisi";
            t.text = "Merhaba, belgeyi yarın getirebilir misiniz? {AD}";
            s.templates.add(t);
            setup(a, s);
            runOnMainSync(
                () -> {
                  a.search.setText("İpek");
                });
            await(() -> a.results.getChildCount() == 2);
            truth(a.selected.isEmpty());
            runOnMainSync(() -> a.select("000123"));
            await(() -> !a.busy && a.selected.equals("000123"));
            runOnMainSync(() -> a.templateSelect.setSelection(1));
            await(() -> a.message.equals(t.text));
            truth(a.message.equals(t.text));
            List<Intent> opened = new ArrayList<>();
            runOnMainSync(
                () -> {
                  a.gateway =
                      new MainActivity.Gateway() {
                        public List<String> packages() {
                          return Arrays.asList("com.whatsapp");
                        }

                        public void open(Intent intent) {
                          opened.add(intent);
                        }
                      };
                  a.prepareMessage();
                });
            await(() -> !a.busy && a.lastDialog != null && a.lastDialog.isShowing());
            truth(opened.isEmpty());
            runOnMainSync(() -> a.lastDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick());
            await(() -> !a.busy && a.state.history.get(0).status == Status.CANCELLED);
            truth(opened.isEmpty());
            for (String pkg : new String[] {"com.whatsapp", "com.whatsapp.w4b"}) {
              runOnMainSync(
                  () -> {
                    a.gateway =
                        new MainActivity.Gateway() {
                          public List<String> packages() {
                            return Arrays.asList(pkg);
                          }

                          public void open(Intent intent) {
                            opened.add(intent);
                          }
                        };
                    a.prepareMessage();
                  });
              await(() -> !a.busy && a.lastDialog.isShowing());
              runOnMainSync(() -> a.lastDialog.getButton(-1).performClick());
              await(
                  () ->
                      !a.busy
                          && opened.size() > 0
                          && pkg.equals(opened.get(opened.size() - 1).getPackage()));
              Intent intent = opened.get(opened.size() - 1);
              truth(intent.getData().getHost().equals("wa.me"));
              truth(intent.getData().getPath().equals("/905320000000"));
              truth(intent.getData().getQueryParameter("text").equals(t.text));
              truth(a.state.history.get(0).status == Status.ATTEMPT);
              runOnMainSync(
                  () -> {
                    a.pendingEvent = "";
                  });
            }
            int before = opened.size();
            runOnMainSync(
                () -> {
                  a.gateway =
                      new MainActivity.Gateway() {
                        public List<String> packages() {
                          return Collections.emptyList();
                        }

                        public void open(Intent intent) {
                          opened.add(intent);
                        }
                      };
                  a.prepareMessage();
                });
            await(() -> !a.busy && a.lastDialog.isShowing());
            runOnMainSync(() -> a.lastDialog.getButton(-1).performClick());
            await(() -> a.lastDialog.getButton(-1).getText().toString().equals("Tarayıcıda Aç"));
            truth(opened.size() == before);
            runOnMainSync(() -> a.lastDialog.getButton(-2).performClick());
            await(() -> !a.busy && a.state.history.get(0).status == Status.CANCELLED);
            truth(opened.size() == before);
            truth(
                target.checkSelfPermission("android.permission.INTERNET")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED);
            truth(
                (a.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SECURE)
                    != 0);
          } finally {
            close(a);
          }
        });
    check(
        "UI XLSX mapping conflict choice preview commit and automatic encrypted undo",
        () -> {
          MainActivity a = open();
          try {
            setup(a, new State());
            File f = new File(output, "duplicates.xlsx");
            Person first = sample("000001"), second = sample("000001");
            first.ad = "Birinci";
            second.ad = "Seçilen";
            try (OutputStream out = new FileOutputStream(f)) {
              Excel.export(out, Arrays.asList(first, second));
            }
            runOnMainSync(() -> a.loadImport(android.net.Uri.fromFile(f)));
            await(
                () ->
                    !a.busy
                        && a.lastDialog != null
                        && a.lastDialog.isShowing()
                        && a.lastDialog.getListView() != null);
            runOnMainSync(
                () -> {
                  ListView list = a.lastDialog.getListView();
                  list.performItemClick(list.getAdapter().getView(0, null, list), 0, 0);
                });
            await(() -> !a.busy && a.mappingWindow != null && a.mappingWindow.isShowing());
            runOnMainSync(
                () ->
                    find(a.lastDialog.getWindow().getDecorView(), "İçe Aktarma Önizlemesi")
                        .performClick());
            await(() -> !a.busy && a.importPlan != null);
            truth(a.importPlan.conflicts.size() == 1 && a.state.people.isEmpty());
            runOnMainSync(
                () ->
                    find(a.lastDialog.getWindow().getDecorView(), "Çakışmayı Çöz · Sicil 000001")
                        .performClick());
            runOnMainSync(
                () -> {
                  ListView list = a.lastDialog.getListView();
                  list.performItemClick(list.getAdapter().getView(2, null, list), 2, 2);
                });
            await(() -> !a.busy && a.importPlan.conflicts.isEmpty());
            truth(a.importPlan.added == 1 && a.importPlan.skipped == 1);
            runOnMainSync(
                () ->
                    find(a.lastDialog.getWindow().getDecorView(), "Onayla Ve İçe Aktar")
                        .performClick());
            runOnMainSync(() -> a.lastDialog.getButton(-1).performClick());
            await(() -> !a.busy && a.state.people.size() == 1);
            truth(a.state.people.get("000001").ad.equals("Seçilen"));
            truth(a.vault.undo.exists());
            truth(a.vault.load().people.get("000001").telefon.equals("05320000000"));
          } finally {
            close(a);
          }
        });
    check(
        "Sequential selection deduplicates never auto opens and stale identity blocks messaging",
        () -> {
          MainActivity a = open();
          try {
            State state = new State();
            state.people.put("001", sample("001"));
            state.people.put("002", sample("002"));
            setup(a, state);
            List<Intent> opened = new ArrayList<>();
            runOnMainSync(
                () -> {
                  a.gateway =
                      new MainActivity.Gateway() {
                        public List<String> packages() {
                          return Arrays.asList("com.whatsapp");
                        }

                        public void open(Intent i) {
                          opened.add(i);
                        }
                      };
                  a.selectedPeople.add("001");
                  a.selectedPeople.add("001");
                  a.selectedPeople.add("002");
                  a.startQueue();
                });
            truth(a.queue.size() == 2 && a.selected.equals("001"));
            runOnMainSync(
                () -> {
                  a.preview.setText("Kendi Mesajım");
                  a.search.setText("002");
                  a.prepareMessage();
                });
            truth(a.state.history.isEmpty() && opened.isEmpty());
            runOnMainSync(a::nextQueue);
            runOnMainSync(() -> a.lastDialog.getButton(-1).performClick());
            truth(a.selected.equals("002") && opened.isEmpty());
            runOnMainSync(
                () -> {
                  a.queue.clear();
                  a.selectedPeople.clear();
                  a.render();
                });
            truth(a.queue.isEmpty());
          } finally {
            close(a);
          }
        });
    check(
        "Light dark five screens secure lock and small screen geometry synthetic previews",
        () -> {
          android.content.SharedPreferences prefs = target.getSharedPreferences("settings", 0);
          int previous = prefs.getInt("theme", 0);
          boolean lock = prefs.getBoolean("lock", false);
          try {
            prefs.edit().putBoolean("lock", false).commit();
            for (int mode : new int[] {1, 2}) {
              prefs.edit().putInt("theme", mode).commit();
              MainActivity a = open();
              try {
                State s = new State();
                s.people.put("000123", sample("000123"));
                Template t = new Template();
                t.name = "Evrak Hatırlatma";
                t.category = "Hatırlatma";
                t.text =
                    "Merhaba, uygun olduğunuzda evraklarınızı teslim edebilir misiniz?"
                        + " Teşekkürler.";
                s.templates.add(t);
                setup(a, s);
                for (int screen = 0; screen < 5; screen++) {
                  final int page = screen;
                  runOnMainSync(
                      () -> {
                        a.screen = page;
                        a.selected = "000123";
                        a.query = "000123";
                        a.selectedTemplate = t.id;
                        a.message = t.text;
                        a.render();
                      });
                  waitForIdleSync();
                  Thread.sleep(300);
                  snapshot(a, "screen-" + mode + "-" + screen + ".png");
                  runOnMainSync(
                      () -> {
                        truth(a.nav.getChildCount() == 5);
                        for (int i = 0; i < 5; i++) {
                          View v = a.nav.getChildAt(i);
                          truth(v.getWidth() > 0 && v.getRight() <= a.nav.getWidth());
                        }
                      });
                }
                runOnMainSync(
                    () -> {
                      prefs.edit().putBoolean("lock", true).commit();
                      a.confirm("Özel Bilgi", "Sentetik içerik", () -> {});
                      a.onStop();
                      a.onResume();
                      truth(a.locked);
                      truth(find(a.root, "Uygulama Kilitli") != null);
                      truth(a.dialogs.isEmpty());
                      prefs.edit().putBoolean("lock", false).commit();
                      a.locked = false;
                    });
              } finally {
                close(a);
              }
            }
          } finally {
            prefs.edit().putInt("theme", previous).putBoolean("lock", lock).commit();
          }
        });
    Bundle result = new Bundle();
    result.putString(
        "stream", "Sicil360 Android checks: " + passed + " passed, " + failed + " failed\n");
    result.putInt("passed", passed);
    result.putInt("failed", failed);
    finish(failed == 0 ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
  }
}
