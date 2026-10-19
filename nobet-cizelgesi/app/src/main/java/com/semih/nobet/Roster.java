package com.semih.nobet;

import java.time.*;
import java.util.*;

/** Pure scheduling model. No Android, networking, or persistent mutations. */
public final class Roster {
  public static final String WORK = "İşyeri", BRANCH = "Şube", ALL = "Her iki yer";

  public static final class Person {
    public String id, name;
    public boolean active = true;

    public Person(String id, String name) {
      this.id = id;
      this.name = name;
    }
  }

  public static final class Leave {
    public String person, place, reason;
    public LocalDate start, end;

    public Leave(String person, String place, LocalDate start, LocalDate end, String reason) {
      if (end.isBefore(start))
        throw new IllegalArgumentException("Bitiş başlangıçtan önce olamaz.");
      this.person = person;
      this.place = place;
      this.start = start;
      this.end = end;
      this.reason = reason;
    }

    public boolean blocks(String p, String where, LocalDate date) {
      return person.equals(p)
          && (place.equals(ALL) || place.equals(where))
          && !date.isBefore(start)
          && !date.isAfter(end);
    }
  }

  public static final class Duty {
    public LocalDate date;
    public String place, person;

    public Duty(LocalDate date, String place, String person) {
      this.date = date;
      this.place = place;
      this.person = person;
    }
  }

  public static final class Debt {
    public String person;
    public LocalDate end;

    public Debt(String person, LocalDate end) {
      this.person = person;
      this.end = end;
    }
  }

  public static final class Week {
    public LocalDate monday;
    public String cursor = "";
    public List<Duty> duties = new ArrayList<>();
    public List<Debt> debts = new ArrayList<>();
    public List<String> warnings = new ArrayList<>();

    public Week(LocalDate monday) {
      this.monday = monday;
    }
  }

  public static final class State {
    public List<Person> people = new ArrayList<>();
    public List<Leave> leaves = new ArrayList<>();
    public List<Week> weeks = new ArrayList<>();

    public String name(String id) {
      return people.stream()
          .filter(p -> p.id.equals(id))
          .map(p -> p.name)
          .findFirst()
          .orElse("Kaldırılmış personel");
    }
  }

  public static LocalDate monday(LocalDate date) {
    return date.minusDays(date.getDayOfWeek().getValue() - 1);
  }

  public static boolean weekend(LocalDate date) {
    return date.getDayOfWeek().getValue() >= 6;
  }

  public static boolean available(State s, String p, String place, LocalDate date) {
    return s.people.stream().anyMatch(x -> x.id.equals(p) && x.active)
        && s.leaves.stream().noneMatch(l -> l.blocks(p, place, date));
  }

  static boolean free(State s, Week w, Person p, String place, LocalDate date) {
    return !rest(w, p.id, date)
        && available(s, p.id, place, date)
        && w.duties.stream().noneMatch(d -> d.date.equals(date) && d.person.equals(p.id));
  }

  public static boolean rest(Week w, String person, LocalDate date) {
    return (date.equals(w.monday.plusDays(3)) || date.equals(w.monday.plusDays(4)))
        && w.duties.stream()
            .anyMatch(d -> d.person.equals(person) && d.place.equals(WORK) && weekend(d.date));
  }

  static long count(State s, Week w, String id, String place, boolean weekend) {
    return java.util.stream.Stream.concat(
            s.weeks.stream()
                .filter(x -> x.monday.isBefore(w.monday))
                .flatMap(x -> x.duties.stream()),
            w.duties.stream())
        .filter(d -> d.person.equals(id) && d.place.equals(place) && weekend(d.date) == weekend)
        .count();
  }

  static void debt(Week w, String p, LocalDate end) {
    Debt old = w.debts.stream().filter(d -> d.person.equals(p)).findFirst().orElse(null);
    if (old == null) w.debts.add(new Debt(p, end));
    else if (end.isAfter(old.end)) old.end = end;
  }

  public static Week generate(State s, LocalDate date) {
    return generate(s, date, null);
  }

  public static Week generate(State s, LocalDate date, String replacement) {
    Week w = new Week(monday(date));
    List<Person> active =
        s.people.stream().filter(p -> p.active).collect(java.util.stream.Collectors.toList());
    if (active.isEmpty())
      throw new IllegalArgumentException("Önce en az bir aktif personel ekleyin.");
    Week previous =
        s.weeks.stream()
            .filter(x -> x.monday.isBefore(w.monday))
            .max(Comparator.comparing(x -> x.monday))
            .orElse(null);
    if (previous != null) {
      w.cursor = previous.cursor;
      for (Debt d : previous.debts) w.debts.add(new Debt(d.person, d.end));
    }
    // Reserve the weekend block first; an owed turn never consumes the regular rotation.
    LocalDate sat = w.monday.plusDays(5), sun = sat.plusDays(1);
    Person chosen = null;
    List<Debt> due = new ArrayList<>(w.debts);
    due.sort(Comparator.comparing(d -> d.end));
    for (Debt d : due)
      if (d.end.isBefore(sat)) {
        Person p = active.stream().filter(x -> x.id.equals(d.person)).findFirst().orElse(null);
        if (p != null && available(s, p.id, WORK, sat) && available(s, p.id, WORK, sun)) {
          chosen = p;
          break;
        }
      }
    if (chosen != null && due.stream().filter(d -> d.end.isBefore(sat)).count() > 1)
      w.warnings.add(
          "Birden fazla bekleyen hafta sonu var. En erken mazereti biten uygun kişi öncelikli;"
              + " diğerleri sırada tutuluyor.");
    if (chosen == null) {
      int last = -1;
      for (int i = 0; i < s.people.size(); i++) if (s.people.get(i).id.equals(w.cursor)) last = i;
      for (int n = 1; n <= s.people.size(); n++) {
        Person p = s.people.get((last + n) % s.people.size());
        if (!p.active) continue;
        w.cursor = p.id;
        if (available(s, p.id, WORK, sat) && available(s, p.id, WORK, sun)) {
          chosen = p;
          break;
        }
        LocalDate until =
            s.leaves.stream()
                .filter(l -> l.blocks(p.id, WORK, sat) || l.blocks(p.id, WORK, sun))
                .map(l -> l.end)
                .max(Comparator.naturalOrder())
                .orElse(sun);
        debt(w, p.id, until);
        w.warnings.add(
            s.name(p.id)
                + " hafta sonu atlandı; mazeret sonu "
                + until
                + ". İlk uygun hafta sonuna öncelikli.");
      }
    }
    if (replacement != null) {
      Person requested =
          active.stream()
              .filter(p -> p.id.equals(replacement))
              .findFirst()
              .orElseThrow(() -> new IllegalArgumentException("Aktif personel seçin."));
      if (!available(s, requested.id, WORK, sat) || !available(s, requested.id, WORK, sun))
        throw new IllegalArgumentException("Seçilen kişinin hafta sonunda mazereti var.");
      if (chosen != null && !chosen.id.equals(requested.id)) {
        // Manual cover does not erase the proposed person's turn; retain it as a pending turn.
        debt(w, chosen.id, sun);
      }
      chosen = requested;
      w.warnings.add("Hafta sonu elle seçildi; yerini devreden personelin sırası korunur.");
    }
    if (chosen != null) {
      w.duties.add(new Duty(sat, WORK, chosen.id));
      w.duties.add(new Duty(sun, WORK, chosen.id));
      String id = chosen.id;
      w.debts.removeIf(d -> d.person.equals(id));
    } else w.warnings.add("Hafta sonu uygun personel yok; işyeri iki gün BOŞ.");
    // Branch: at most one weekday per person. Constrained days are assigned first.
    List<LocalDate> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) days.add(w.monday.plusDays(i));
    Set<String> used = new HashSet<>();
    if (!branchSearch(s, w, days, used, active, 0)) {
      w.duties.removeIf(d -> d.place.equals(BRANCH));
      used.clear();
      days.sort(
          Comparator.comparingLong(
              d -> active.stream().filter(p -> available(s, p.id, BRANCH, d)).count()));
      for (LocalDate d : days) {
        Person p =
            active.stream()
                .filter(x -> !used.contains(x.id) && free(s, w, x, BRANCH, d))
                .min(Comparator.comparingLong(x -> count(s, w, x.id, BRANCH, false)))
                .orElse(null);
        if (p != null) {
          used.add(p.id);
          w.duties.add(new Duty(d, BRANCH, p.id));
        } else w.warnings.add(d + " şube BOŞ: tek gün/personel kuralıyla uygun personel yok.");
      }
    }
    for (int i = 0; i < 5; i++) {
      LocalDate d = w.monday.plusDays(i);
      Person p =
          active.stream()
              .filter(x -> free(s, w, x, WORK, d))
              .min(
                  Comparator.<Person>comparingLong(x -> count(s, w, x.id, WORK, false))
                      .thenComparingLong(x -> count(s, w, x.id, BRANCH, false)))
              .orElse(null);
      if (p != null) w.duties.add(new Duty(d, WORK, p.id));
      else w.warnings.add(d + " işyeri BOŞ: uygun personel yok.");
    }
    for (Person p : active)
      if (w.duties.stream().noneMatch(d -> d.person.equals(p.id) && d.place.equals(BRANCH)))
        w.warnings.add(
            p.name + " bu hafta şubeye atanmadı (mazeret/kapasite). Onayınız gerekiyor.");
    w.warnings.addAll(validate(s, w));
    return w;
  }

  static boolean branchSearch(
      State s, Week w, List<LocalDate> days, Set<String> used, List<Person> active, int index) {
    if (index == 5) return true;
    LocalDate date = days.get(index);
    List<Person> options = new ArrayList<>(active);
    options.sort(Comparator.comparingLong(p -> count(s, w, p.id, BRANCH, false)));
    for (Person p : options)
      if (!used.contains(p.id) && free(s, w, p, BRANCH, date)) {
        used.add(p.id);
        Duty d = new Duty(date, BRANCH, p.id);
        w.duties.add(d);
        if (branchSearch(s, w, days, used, active, index + 1)) return true;
        w.duties.remove(d);
        used.remove(p.id);
      }
    return false;
  }

  public static List<String> validate(State s, Week w) {
    List<String> errors = new ArrayList<>();
    Set<String> slots = new HashSet<>(), persons = new HashSet<>();
    Map<String, Integer> branch = new HashMap<>();
    for (Duty d : w.duties) {
      if (d.date.isBefore(w.monday) || d.date.isAfter(w.monday.plusDays(6)))
        errors.add("Hafta dışı tarih.");
      if (!slots.add(d.date + d.place)) errors.add("Aynı yerde birden fazla kişi.");
      if (!persons.add(d.date + d.person)) errors.add("Aynı kişi aynı gün iki yerde.");
      if (rest(w, d.person, d.date))
        errors.add(s.name(d.person) + " hafta sonu nöbetinden önce perşembe/cuma izinli olmalı.");
      if (!available(s, d.person, d.place, d.date))
        errors.add(s.name(d.person) + " için mazeret veya pasif personel çakışması.");
      if (d.place.equals(BRANCH)) {
        if (weekend(d.date)) errors.add("Hafta sonu şube nöbeti olamaz.");
        branch.merge(d.person, 1, Integer::sum);
      }
    }
    if (branch.values().stream().anyMatch(n -> n > 1))
      errors.add("Bir kişi haftada birden fazla şube gününe atanamaz.");
    String sat =
        w.duties.stream()
            .filter(d -> d.date.equals(w.monday.plusDays(5)) && d.place.equals(WORK))
            .map(d -> d.person)
            .findFirst()
            .orElse("");
    String sun =
        w.duties.stream()
            .filter(d -> d.date.equals(w.monday.plusDays(6)) && d.place.equals(WORK))
            .map(d -> d.person)
            .findFirst()
            .orElse("");
    if (!sat.equals(sun)) errors.add("Cumartesi ve pazar aynı kişi olmalı.");
    return errors;
  }

  public static void approve(State s, Week w) {
    List<String> invalid = validate(s, w);
    if (!invalid.isEmpty()) throw new IllegalArgumentException(String.join("\n", invalid));
    s.weeks.removeIf(x -> !x.monday.isBefore(w.monday));
    s.weeks.add(w);
  }

  public static Week copy(Week w) {
    Week c = new Week(w.monday);
    c.cursor = w.cursor;
    for (Duty d : w.duties) c.duties.add(new Duty(d.date, d.place, d.person));
    for (Debt d : w.debts) c.debts.add(new Debt(d.person, d.end));
    c.warnings.addAll(w.warnings);
    return c;
  }
}
