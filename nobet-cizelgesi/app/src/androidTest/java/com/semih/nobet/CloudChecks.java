package com.semih.nobet;

import static com.semih.nobet.Roster.*;

import android.app.Activity;
import android.content.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;

/** Test fixtures simulate transport; these tests do not claim a live Supabase connection. */
final class CloudChecks {
  interface Condition {
    boolean get() throws Exception;
  }

  static void await(SmokeRunner runner, Condition condition) throws Exception {
    long end = android.os.SystemClock.uptimeMillis() + 20000;
    boolean[] value = {false};
    while (!value[0] && android.os.SystemClock.uptimeMillis() < end) {
      runner.runOnMainSync(
          () -> {
            try {
              value[0] = condition.get();
            } catch (Exception e) {
              throw new AssertionError(e);
            }
          });
      if (!value[0]) Thread.sleep(100);
    }
    runner.truth(value[0]);
  }

  static final String A = "30000000-0000-0000-0000-000000000001",
      B = "30000000-0000-0000-0000-000000000002",
      TEAM = "40000000-0000-0000-0000-000000000001";

  static CloudApi.Config config() throws Exception {
    return new CloudApi.Config(
        "https://synthetic-nobet.supabase.co", "sb_publishable_synthetic_test_public_key");
  }

  static CloudApi.Session session(String user, String access, long expires) throws Exception {
    return new CloudApi.Session(
        new JSONObject()
            .put("access_token", access)
            .put("refresh_token", "synthetic-refresh")
            .put("expires_at", expires)
            .put("user", new JSONObject().put("id", user).put("email", "synthetic@example.test")),
        null);
  }

  static final class Host implements CloudController.Host {
    State state = new State();
    String notice = "";
    boolean draft = false;

    public State cloudState() {
      return state;
    }

    public void cloudState(State state) {
      this.state = state;
      draft = false;
    }

    public boolean cloudDraft() {
      return draft;
    }

    public State localState() {
      return new State();
    }

    public void cloudRender() {}

    public void cloudNotice(String message) {
      notice = message;
    }
  }

  static final class Server implements CloudApi.Transport {
    long revision = 0;
    JSONObject payload;
    boolean bAdmin = false, loseNextAck = false;
    final Set<String> receipts = new HashSet<>();
    int posts = 0;

    Server() throws Exception {
      payload = new JSONObject(Store.encode(new State()));
    }

    JSONObject snapshot(String token) throws Exception {
      return new JSONObject()
          .put("id", TEAM)
          .put("name", "Sentetik Ekip")
          .put("revision", revision)
          .put("role", token.equals("synthetic-a") || bAdmin ? "admin" : "viewer")
          .put("payload", new JSONObject(payload.toString()));
    }

    public synchronized CloudApi.Response post(String path, String token, JSONObject args)
        throws Exception {
      posts++;
      if (path.endsWith("nobet_head")) {
        JSONObject head = snapshot(token);
        head.remove("payload");
        return new CloudApi.Response(200, head.toString());
      }
      if (path.endsWith("nobet_snapshot"))
        return new CloudApi.Response(200, snapshot(token).toString());
      if (path.endsWith("nobet_publish")) {
        if (!token.equals("synthetic-a") && !bAdmin)
          return new CloudApi.Response(403, "NOBET_FORBIDDEN");
        String id = args.getString("p_request");
        if (!receipts.contains(id)) {
          if (args.getLong("p_expected") != revision)
            return new CloudApi.Response(409, "NOBET_CONFLICT");
          payload = new JSONObject(args.getJSONObject("p_payload").toString());
          revision++;
          receipts.add(id);
          if (loseNextAck) {
            loseNextAck = false;
            throw new IOException("Synthetic lost acknowledgement");
          }
        }
        return new CloudApi.Response(200, snapshot(token).toString());
      }
      throw new AssertionError(path);
    }
  }

  static void run(SmokeRunner runner, Context isolated) {
    runner.check(
        "Cloud configuration rejects cleartext foreign hosts and secret keys",
        () -> {
          runner.truth(config().url.equals("https://synthetic-nobet.supabase.co"));
          String[] invalid = {
            "http://synthetic-nobet.supabase.co",
            "https://example.com",
            "https://user@synthetic-nobet.supabase.co",
            "https://synthetic-nobet.supabase.co/path",
            "https://synthetic-nobet.supabase.co?token=x"
          };
          for (String url : invalid) {
            boolean refused = false;
            try {
              new CloudApi.Config(url, config().key);
            } catch (Exception expected) {
              refused = true;
            }
            runner.truth(refused);
          }
          for (String key :
              new String[] {
                "sb_secret_never_use_in_apk",
                "invalid",
                "e30."
                    + java.util.Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                            "{\"role\":\"service_role\"}".getBytes(StandardCharsets.UTF_8))
                    + ".test"
              }) {
            boolean refused = false;
            try {
              new CloudApi.Config(config().url, key);
            } catch (Exception expected) {
              refused = true;
            }
            runner.truth(refused);
          }
          String anon =
              "e30."
                  + java.util.Base64.getUrlEncoder()
                      .withoutPadding()
                      .encodeToString("{\"role\":\"anon\"}".getBytes(StandardCharsets.UTF_8))
                  + ".test";
          runner.truth(new CloudApi.Config(config().url, anon).key.equals(anon));
        });
    runner.check(
        "Cloud auth refresh rotates session and does not treat auth metadata as role",
        () -> {
          int[] refreshes = {0};
          CloudApi api =
              new CloudApi(
                  config(),
                  session(A, "old", 0),
                  (path, token, body) -> {
                    if (path.contains("grant_type=refresh_token")) {
                      refreshes[0]++;
                      return new CloudApi.Response(
                          200,
                          new JSONObject()
                              .put("access_token", "renewed")
                              .put("refresh_token", "rotated")
                              .put("expires_in", 3600)
                              .put(
                                  "user",
                                  new JSONObject()
                                      .put("id", A)
                                      .put("email", "a@example.test")
                                      .put("user_metadata", new JSONObject().put("role", "admin")))
                              .toString());
                    }
                    runner.truth(token.equals("renewed"));
                    return new CloudApi.Response(200, "[]");
                  });
          int[] saved = {0};
          api.changed =
              value -> {
                runner.truth(value.refresh.equals("rotated"));
                saved[0]++;
              };
          runner.truth(api.rpc("nobet_my_teams", new JSONObject()) instanceof JSONArray);
          runner.truth(refreshes[0] == 1 && saved[0] == 1);
          CloudApi forbidden =
              new CloudApi(
                  config(),
                  session(A, "x", Long.MAX_VALUE),
                  (path, token, body) -> new CloudApi.Response(403, "NOBET_FORBIDDEN"));
          boolean denied = false;
          try {
            forbidden.snapshot(TEAM);
          } catch (CloudApi.Failure expected) {
            denied = expected.kind.equals("forbidden");
          }
          runner.truth(denied);
          CloudApi smtp =
              new CloudApi(
                  config(),
                  null,
                  (path, token, body) ->
                      new CloudApi.Response(403, "email_address_not_authorized"));
          boolean setup = false;
          try {
            smtp.signUp("Deneme", "test@example.test", "synthetic-password");
          } catch (CloudApi.Failure expected) {
            setup = expected.kind.equals("setup");
          }
          runner.truth(setup);
        });
    runner.check(
        "Cloud signup verification uses email code and persists authenticated session",
        () -> {
          int[] verified = {0};
          CloudApi api =
              new CloudApi(
                  config(),
                  null,
                  (path, token, body) -> {
                    runner.truth(token == null);
                    if (path.equals("/auth/v1/signup")) {
                      runner.truth(
                          body.getJSONObject("data").getString("full_name").equals("Deneme"));
                      return new CloudApi.Response(200, "{\"user\":{\"id\":\"" + A + "\"}}");
                    }
                    if (path.equals("/auth/v1/resend")) {
                      runner.truth(body.getString("type").equals("signup"));
                      return new CloudApi.Response(200, "{}");
                    }
                    runner.truth(
                        path.equals("/auth/v1/verify")
                            && body.getString("token").equals("123456")
                            && body.getString("type").equals("signup"));
                    return new CloudApi.Response(
                        200, session(A, "verified", Long.MAX_VALUE).json().toString());
                  });
          api.changed =
              value -> {
                verified[0]++;
              };
          runner.truth(!api.signUp("Deneme", "test@example.test", "synthetic-password"));
          api.resend("test@example.test");
          runner.truth(api.verifyEmail("test@example.test", "123456").user.equals(A));
          runner.truth(verified[0] == 1);
        });
    runner.check(
        "Cloud vault encrypts tokens detects tampering and never writes plaintext fallback",
        () -> {
          CloudVault vault = new CloudVault(isolated);
          String name = "synthetic-test-session";
          try {
            JSONObject value =
                new JSONObject().put("access_token", "synthetic-access-do-not-persist-plain");
            vault.write(name, value);
            runner.truth(
                vault.read(name).getString("access_token").equals(value.getString("access_token")));
            File file = new File(isolated.getFilesDir(), "cloud/" + name + ".bin");
            byte[] first = java.nio.file.Files.readAllBytes(file.toPath());
            runner.truth(
                !new String(first, StandardCharsets.ISO_8859_1).contains("synthetic-access"));
            vault.write(name, value);
            byte[] second = java.nio.file.Files.readAllBytes(file.toPath());
            runner.truth(!Arrays.equals(first, second));
            second[second.length - 1] ^= 1;
            java.nio.file.Files.write(file.toPath(), second);
            boolean denied = false;
            try {
              vault.read(name);
            } catch (Exception expected) {
              denied = true;
            }
            runner.truth(denied);
          } finally {
            vault.delete(name);
          }
        });
    runner.check(
        "Cloud two simulated clients enforce viewer role sync updates conflict and retry safely",
        () -> {
          Activity activity =
              runner.startActivitySync(
                  new Intent(runner.getTargetContext(), MainActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
          Host a = new Host(), b = new Host();
          CloudController[] clients = new CloudController[2];
          Server server = new Server();
          try {
            runner.waitForIdleSync();
            runner.runOnMainSync(
                () -> {
                  try {
                    for (int i = 0; i < 2; i++) {
                      CloudController c = new CloudController(activity, i == 0 ? a : b);
                      clients[i] = c;
                      c.api =
                          new CloudApi(
                              config(),
                              session(
                                  i == 0 ? A : B,
                                  i == 0 ? "synthetic-a" : "synthetic-b",
                                  Long.MAX_VALUE),
                              server);
                      c.team = TEAM;
                      c.teamName = "Sentetik Ekip";
                      c.role = i == 0 ? "admin" : "viewer";
                      c.online = true;
                      c.confirmed = new JSONObject(Store.encode(new State()));
                    }
                    runner.truth(!clients[1].canEdit());
                    runner.truth(!clients[1].stage(b.state));
                    runner.truth(server.posts == 0);
                    a.state.people.add(new Person("test-person", "Sentetik Personel"));
                    runner.truth(clients[0].stage(a.state));
                    a.draft = true; // Visibility save can be followed immediately by a new schedule
                    // draft.
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            await(runner, () -> clients[0].pending == null && clients[0].revision == 1);
            runner.runOnMainSync(
                () -> {
                  runner.truth(a.draft);
                  a.draft = false;
                });
            java.lang.reflect.Method fetch =
                CloudController.class.getDeclaredMethod("fetch", boolean.class);
            fetch.setAccessible(true);
            runner.runOnMainSync(
                () -> {
                  try {
                    fetch.invoke(clients[1], false);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            await(runner, () -> clients[1].revision == 1);
            runner.truth(b.state.people.size() == 1);
            server.bAdmin = true;
            runner.runOnMainSync(
                () -> {
                  try {
                    fetch.invoke(clients[1], false);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            await(runner, () -> clients[1].canEdit());
            runner.runOnMainSync(
                () -> {
                  a.state.people.get(0).name = "Birinci Yönetici";
                  runner.truth(clients[0].stage(a.state));
                });
            await(runner, () -> clients[0].revision == 2 && clients[0].pending == null);
            runner.runOnMainSync(
                () -> {
                  b.state.people.get(0).name = "İkinci Yönetici";
                  runner.truth(clients[1].stage(b.state));
                });
            await(runner, () -> clients[1].conflict && !clients[1].publishing);
            runner.truth(
                clients[1].pending != null
                    && server.revision == 2
                    && server
                        .payload
                        .getJSONArray("people")
                        .getJSONObject(0)
                        .getString("name")
                        .equals("Birinci Yönetici"));
            runner.truth(
                new CloudVault(activity)
                    .read("team-" + B + "-" + TEAM)
                    .getJSONObject("pending")
                    .getJSONArray("people")
                    .getJSONObject(0)
                    .getString("name")
                    .equals("İkinci Yönetici"));
            server.loseNextAck = true;
            runner.runOnMainSync(
                () -> {
                  a.state.people.get(0).name = "Yanıtı Kaybolan Kayıt";
                  runner.truth(clients[0].stage(a.state));
                });
            await(runner, () -> clients[0].pending != null && !clients[0].publishing);
            runner.truth(server.revision == 3);
            java.lang.reflect.Method upload = CloudController.class.getDeclaredMethod("upload");
            upload.setAccessible(true);
            runner.runOnMainSync(
                () -> {
                  try {
                    upload.invoke(clients[0]);
                  } catch (Exception e) {
                    throw new AssertionError(e);
                  }
                });
            await(runner, () -> clients[0].pending == null && clients[0].revision == 3);
            runner.truth(server.revision == 3);
          } finally {
            runner.runOnMainSync(
                () -> {
                  for (CloudController c : clients) if (c != null) c.destroy();
                  activity.finish();
                });
            CloudVault vault = new CloudVault(activity);
            vault.delete("team-" + A + "-" + TEAM);
            vault.delete("team-" + B + "-" + TEAM);
          }
        });
  }
}
