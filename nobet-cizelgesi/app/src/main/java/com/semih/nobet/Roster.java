package com.semih.nobet;

import java.time.*;
import java.util.*;

/** Pure scheduling model. No Android, networking, or persistent mutations. */
public final class Roster {
  public static final String WORK = "İşyeri", BRANCH = "Şube", ALL = "Her iki yer";

  // Place IDs never change when the user renames a location.
  public static final class Place {
    public String id, name;
    public int days, limit;
    public boolean automatic = true;
    public boolean deleted = false;

    public Place(String id, String name, int days, int limit) {
      this.id = id;
      this.name = name;
      this.days = days;
      this.limit = limit;
    }

    public boolean on(LocalDate date) {
      return (days & (1 << (date.getDayOfWeek().getValue() - 1))) != 0;
    }
  }

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
    public String id = UUID.randomUUID().toString();
    public LocalDate start, end;
    public int days; // 0: dated leave; otherwise Monday=bit 0 ... Sunday=bit 6.

    public Leave(String person, String place, LocalDate start, LocalDate end, String reason) {
      this(person, place, start, end, reason, 0);
    }

    public Leave(
        String person, String place, LocalDate start, LocalDate end, String reason, int days) {
      if (days < 0 || days > 127) throw new IllegalArgumentException("Geçersiz tekrar günleri.");
      this.days = days;
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
          && !date.isAfter(end)
          && (days == 0 || (days & (1 << (date.getDayOfWeek().getValue() - 1))) != 0);
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
    public int rotationRevision = 0;
    public List<Duty> duties = new ArrayList<>();
    public List<Debt> debts = new ArrayList<>();
    public Set<String> limitOverrides = new HashSet<>();
    public Set<String> simultaneousOverrides = new HashSet<>();
    public Set<String> leaveOverrides = new HashSet<>();
    public List<String> warnings = new ArrayList<>();

    public Week(LocalDate monday) {
      this.monday = monday;
    }
  }

  public static final class State {
    public List<Holidays.Holiday> holidays = new ArrayList<>();
    public Set<String> hiddenPlaces = new HashSet<>();
    public List<String> weekendOrder = new ArrayList<>();
    public int rotationRevision = 0;
    public List<Place> places =
        new ArrayList<>(
            Arrays.asList(new Place(WORK, WORK, 127, 0), new Place(BRANCH, BRANCH, 31, 1)));
    public List<Person> people = new ArrayList<>();
    public List<Leave> leaves = new ArrayList<>();
    public List<Week> weeks = new ArrayList<>();

    public Place place(String id) {
      return places.stream()
          .filter(p -> p.id.equals(id))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("Görev yeri bulunamadı."));
    }

    public String placeName(String id) {
      if (ALL.equals(id)) return "Tüm görev yerleri";
      return places.stream().filter(p -> p.id.equals(id)).map(p -> p.name).findFirst().orElse(id);
    }

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

  public static List<Debt> pendingDebts(State s, LocalDate before) {
    List<Week> history = new ArrayList<>();
    for (Week w : s.weeks) if (w.monday.isBefore(before)) history.add(w);
    history.sort(Comparator.comparing(w -> w.monday));
    Set<String> visited = new HashSet<>();
    Map<String, Debt> result = new LinkedHashMap<>();
    for (Week origin : history)
      for (Debt d : origin.debts) {
        String key = d.person + "|" + d.end;
        if (!visited.add(key)) continue;
        boolean served =
            history.stream()
                .filter(w -> !w.monday.isBefore(origin.monday))
                .anyMatch(
                    w ->
                        w.duties.stream()
                            .anyMatch(
                                x ->
                                    x.person.equals(d.person)
                                        && x.place.equals(WORK)
                                        && weekend(x.date)
                                        && (x.date.isAfter(d.end)
                                            || w.leaveOverrides.stream()
                                                .anyMatch(
                                                    approvalKey ->
                                                        approvalKey.startsWith(
                                                            x.date + "|" + x.person + "|" + x.place
                                                                + "|")))));
        if (!served) {
          Debt old = result.get(d.person);
          if (old == null || d.end.isAfter(old.end))
            result.put(d.person, new Debt(d.person, d.end));
        }
      }
    return new ArrayList<>(result.values());
  }

  static void debt(Week w, String p, LocalDate end) {
    Debt old = w.debts.stream().filter(d -> d.person.equals(p)).findFirst().orElse(null);
    if (old == null) w.debts.add(new Debt(p, end));
    else if (end.isAfter(old.end)) old.end = end;
  }

  public static List<LocalDate> workWeekendDays(State s, LocalDate monday) {
    List<LocalDate> result = new ArrayList<>();
    for (int i = 5; i < 7; i++) {
      LocalDate d = monday.plusDays(i);
      if (s.place(WORK).on(d)) result.add(d);
    }
    return result;
  }

  public static boolean weekendAvailable(State s, String person, LocalDate monday) {
    for (LocalDate d : workWeekendDays(s, monday)) if (!available(s, person, WORK, d)) return false;
    return true;
  }

  public static void deletePlace(State s, String id, LocalDate today) {
    if (id.equals(WORK) || id.equals(BRANCH))
      throw new IllegalArgumentException(
          "İlk iki yer silinmez; adını/günlerini düzenleyebilirsiniz.");
    Place p = s.place(id);
    p.deleted = true;
    for (Week w : s.weeks) w.duties.removeIf(d -> d.place.equals(id) && d.date.isAfter(today));
  }

  public static List<Place> displayPlaces(State s, Week w) {
    List<Place> list = new ArrayList<>();
    for (Place p : s.places)
      if ((!p.deleted || w.duties.stream().anyMatch(d -> d.place.equals(p.id)))
          && !s.hiddenPlaces.contains(p.id)) list.add(p);
    return list;
  }

  public static List<Person> weekendOrder(State s) {
    List<Person> result = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (String id : s.weekendOrder)
      for (Person p : s.people) if (p.id.equals(id) && seen.add(id)) result.add(p);
    for (Person p : s.people) if (seen.add(p.id)) result.add(p);
    return result;
  }

  public static void setWeekendOrder(State s, List<String> ids) {
    Set<String> active = new HashSet<>();
    for (Person p : s.people) if (p.active) active.add(p.id);
    if (ids.size() != active.size() || !new HashSet<>(ids).equals(active))
      throw new IllegalArgumentException("Her aktif personel sırada bir kez bulunmalı.");
    s.weekendOrder = new ArrayList<>(ids);
    for (Person p : weekendOrder(s)) if (!p.active) s.weekendOrder.add(p.id);
    s.rotationRevision++;
  }

  public static Week generate(State s, LocalDate date) {
    return generate(s, date, null);
  }

  public static Week generate(State s, LocalDate date, String replacement) {
    return generate(s, date, replacement, false);
  }

  public static Week generateManualReplacement(State s, LocalDate date, String replacement) {
    return generate(s, date, replacement, true);
  }

  private static Week generate(State s, LocalDate date, String replacement, boolean manualLeave) {
    Week w = new Week(monday(date));
    w.rotationRevision = s.rotationRevision;
    List<Person> active =
        s.people.stream().filter(p -> p.active).collect(java.util.stream.Collectors.toList());
    if (active.isEmpty())
      throw new IllegalArgumentException("Önce en az bir aktif personel ekleyin.");
    Week previous =
        s.weeks.stream()
            .filter(x -> x.monday.isBefore(w.monday))
            .max(Comparator.comparing(x -> x.monday))
            .orElse(null);
    if (previous != null && previous.rotationRevision == s.rotationRevision) {
      w.cursor = previous.cursor;
    }
    w.debts.addAll(pendingDebts(s, w.monday));
    // Reserve the weekend block first; an owed turn never consumes the regular rotation.
    LocalDate sat = w.monday.plusDays(5), sun = sat.plusDays(1);
    List<LocalDate> weekendDates = workWeekendDays(s, w.monday);
    if (!weekendDates.isEmpty()) {
      LocalDate firstWeekend = weekendDates.get(0);
      Person chosen = null;
      List<Debt> due = new ArrayList<>(w.debts);
      due.sort(Comparator.comparing(d -> d.end));
      for (Debt d : due)
        if (d.end.isBefore(firstWeekend)) {
          Person p = active.stream().filter(x -> x.id.equals(d.person)).findFirst().orElse(null);
          if (p != null && weekendAvailable(s, p.id, w.monday)) {
            chosen = p;
            break;
          }
        }
      if (chosen != null && due.stream().filter(d -> d.end.isBefore(firstWeekend)).count() > 1)
        w.warnings.add(
            "Birden fazla bekleyen hafta sonu var. En erken mazereti biten uygun kişi öncelikli;"
                + " diğerleri sırada tutuluyor.");
      if (chosen == null) {
        List<Person> order = weekendOrder(s);
        int last = -1;
        for (int i = 0; i < order.size(); i++) if (order.get(i).id.equals(w.cursor)) last = i;
        for (int n = 1; n <= order.size(); n++) {
          Person p = order.get((last + n) % order.size());
          if (!p.active) continue;
          w.cursor = p.id;
          if (weekendAvailable(s, p.id, w.monday)) {
            chosen = p;
            break;
          }
          LocalDate until =
              s.leaves.stream()
                  .filter(l -> weekendDates.stream().anyMatch(d -> l.blocks(p.id, WORK, d)))
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
        if (!manualLeave && !weekendAvailable(s, requested.id, w.monday))
          throw new IllegalArgumentException("Seçilen kişinin hafta sonunda mazereti var.");
        if (chosen != null && !chosen.id.equals(requested.id)) {
          // Manual cover does not erase the proposed person's turn; retain it as a pending turn.
          debt(w, chosen.id, sun);
        }
        chosen = requested;
        w.warnings.add("Hafta sonu elle seçildi; yerini devreden personelin sırası korunur.");
      }
      if (chosen != null) {
        for (LocalDate day : weekendDates) w.duties.add(new Duty(day, WORK, chosen.id));
        String id = chosen.id;
        w.debts.removeIf(d -> d.person.equals(id));
      } else w.warnings.add("Hafta sonu uygun personel yok; işyeri iki gün BOŞ.");
    }
    assignPlace(s, w, s.place(BRANCH), active);
    assignPlace(s, w, s.place(WORK), active);
    for (Place place : s.places)
      if (!place.id.equals(WORK) && !place.id.equals(BRANCH)) assignPlace(s, w, place, active);
    for (Person p : active)
      if (s.place(BRANCH).days != 0
          && w.duties.stream().noneMatch(d -> d.person.equals(p.id) && d.place.equals(BRANCH)))
        w.warnings.add(
            p.name
                + " bu hafta şubeye atanmadı ("
                + s.placeName(BRANCH)
                + ", mazeret/kapasite). Onayınız gerekiyor.");
    w.warnings.addAll(validate(s, w));
    return w;
  }

  /** Maximum coverage, then weekly fairness, then history. A constrained day is never bypassed. */
  static void assignPlace(State s, Week w, Place place, List<Person> people) {
    if (place.deleted || !place.automatic) return;
    List<LocalDate> days = new ArrayList<>();
    for (int i = 0; i < (place.id.equals(WORK) ? 5 : 7); i++) {
      LocalDate day = w.monday.plusDays(i);
      if (place.on(day)
          && w.duties.stream().noneMatch(d -> d.place.equals(place.id) && d.date.equals(day)))
        days.add(day);
    }
    if (days.isEmpty()) return;
    // Fewest alternatives first, for both dates and people. The residual network can move an
    // earlier flexible assignment instead of stranding a person who has only one suitable day.
    days.sort(
        Comparator.<LocalDate>comparingLong(
                day -> people.stream().filter(p -> free(s, w, p, place.id, day)).count())
            .thenComparing(day -> day));
    List<Person> ordered = new ArrayList<>(people);
    ordered.sort(
        Comparator.comparingLong(
            p -> days.stream().filter(d -> free(s, w, p, place.id, d)).count()));
    List<Long> histories = new ArrayList<>();
    for (Person p : ordered)
      histories.add(
          count(s, w, p.id, place.id, false)
              + (place.id.equals(WORK) ? 0 : count(s, w, p.id, place.id, true)));
    List<Long> ranks = new ArrayList<>(new TreeSet<>(histories));
    int source = 0,
        firstPerson = 1,
        firstDay = firstPerson + ordered.size(),
        sink = firstDay + days.size();
    DutyFlow flow = new DutyFlow(sink + 1);
    long weekWeight = days.size() * (ordered.size() * 8L + 8 + s.places.size() * 7L) + 1;
    long coverageBonus = weekWeight * 225; // More than every possible seven-day repeat cost.
    List<DutyFlow.Edge> assignments = new ArrayList<>();
    List<Duty> choices = new ArrayList<>();
    for (int i = 0; i < ordered.size(); i++) {
      Person person = ordered.get(i);
      int existingAll =
          (int)
              w.duties.stream()
                  .filter(d -> d.place.equals(place.id) && d.person.equals(person.id))
                  .count();
      int existing =
          (int)
              w.duties.stream()
                  .filter(
                      d ->
                          d.place.equals(place.id)
                              && d.person.equals(person.id)
                              && (!place.id.equals(WORK) || !weekend(d.date)))
                  .count();
      int capacity = place.limit == 0 ? days.size() : Math.max(0, place.limit - existingAll);
      int opportunity = (int) days.stream().filter(d -> free(s, w, person, place.id, d)).count();
      long tie = opportunity * (ordered.size() + 1L) + ranks.indexOf(histories.get(i));
      for (int n = 0; n < Math.min(capacity, opportunity); n++) {
        int weeklyCount = existing + n;
        flow.add(
            source,
            firstPerson + i,
            1,
            weeklyCount == 0 ? -coverageBonus + tie : weeklyCount * weekWeight + tie);
      }
      for (int j = 0; j < days.size(); j++)
        if (free(s, w, person, place.id, days.get(j))) {
          long reserve = 0;
          LocalDate candidate = days.get(j);
          for (Place other : s.places)
            if (!other.id.equals(place.id)
                && !other.deleted
                && other.automatic
                && other.on(candidate)
                && w.duties.stream()
                    .noneMatch(d -> d.place.equals(other.id) && d.date.equals(candidate))) {
              int alternatives = 0;
              for (int n = 0; n < (other.id.equals(WORK) ? 5 : 7); n++) {
                LocalDate d = w.monday.plusDays(n);
                if (other.on(d)
                    && free(s, w, person, other.id, d)
                    && w.duties.stream()
                        .noneMatch(slot -> slot.place.equals(other.id) && slot.date.equals(d)))
                  alternatives++;
              }
              if (free(s, w, person, other.id, candidate) && alternatives > 0)
                reserve += 7 - alternatives;
            }
          assignments.add(flow.add(firstPerson + i, firstDay + j, 1, reserve));
          choices.add(new Duty(days.get(j), place.id, person.id));
        }
    }
    for (int j = 0; j < days.size(); j++) flow.add(firstDay + j, sink, 1, 0);
    flow.fill(source, sink);
    for (int i = 0; i < assignments.size(); i++)
      if (assignments.get(i).capacity == 0) w.duties.add(choices.get(i));
    for (LocalDate day : days)
      if (w.duties.stream().noneMatch(d -> d.place.equals(place.id) && d.date.equals(day)))
        w.warnings.add(day + " " + place.name + " BOŞ: uygun personel veya haftalık kapasite yok.");
  }

  static boolean belowLimit(Week w, String id, Place p) {
    return p.limit == 0
        || w.duties.stream().filter(d -> d.place.equals(p.id) && d.person.equals(id)).count()
            < p.limit;
  }

  /** Successive shortest augmenting paths; at most seven units, no search cutoff or fallback. */
  private static final class DutyFlow {
    static final class Edge {
      final int to, reverse;
      final long cost;
      int capacity;

      Edge(int to, int reverse, int capacity, long cost) {
        this.to = to;
        this.reverse = reverse;
        this.capacity = capacity;
        this.cost = cost;
      }
    }

    final List<List<Edge>> graph = new ArrayList<>();

    DutyFlow(int size) {
      for (int i = 0; i < size; i++) graph.add(new ArrayList<>());
    }

    Edge add(int from, int to, int capacity, long cost) {
      Edge edge = new Edge(to, graph.get(to).size(), capacity, cost);
      Edge reverse = new Edge(from, graph.get(from).size(), 0, -cost);
      graph.get(from).add(edge);
      graph.get(to).add(reverse);
      return edge;
    }

    void fill(int source, int sink) {
      while (true) {
        long[] distance = new long[graph.size()];
        Arrays.fill(distance, Long.MAX_VALUE);
        int[] previous = new int[graph.size()], via = new int[graph.size()];
        boolean[] queued = new boolean[graph.size()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        distance[source] = 0;
        queue.add(source);
        queued[source] = true;
        while (!queue.isEmpty()) {
          int from = queue.removeFirst();
          queued[from] = false;
          for (int i = 0; i < graph.get(from).size(); i++) {
            Edge e = graph.get(from).get(i);
            if (e.capacity > 0 && distance[e.to] > distance[from] + e.cost) {
              distance[e.to] = distance[from] + e.cost;
              previous[e.to] = from;
              via[e.to] = i;
              if (!queued[e.to]) {
                queue.addLast(e.to);
                queued[e.to] = true;
              }
            }
          }
        }
        if (distance[sink] == Long.MAX_VALUE) return;
        for (int to = sink; to != source; to = previous[to]) {
          Edge e = graph.get(previous[to]).get(via[to]);
          e.capacity--;
          graph.get(to).get(e.reverse).capacity++;
        }
      }
    }
  }

  public static long expectedSlots(State s) {
    long count = 0;
    for (Place p : s.places) if (!p.deleted) count += Integer.bitCount(p.days);
    return count;
  }

  public static long[] totals(State s, String person, String place, LocalDate today) {
    long[] result = new long[3];
    for (Week w : s.weeks)
      for (Duty d : w.duties)
        if (d.person.equals(person) && d.place.equals(place)) {
          if (d.date.isAfter(today)) result[2]++;
          else result[weekend(d.date) ? 1 : 0]++;
        }
    return result;
  }

  public static final class StatRow {
    public Person person;
    public long[] counts;

    public StatRow(Person person, int size) {
      this.person = person;
      counts = new long[size];
    }
  }

  public static List<Place> statisticPlaces(State s) {
    List<Place> places = new ArrayList<>();
    for (Place p : s.places)
      if (!p.id.equals(WORK)
          && (!p.deleted
              || s.weeks.stream()
                  .anyMatch(w -> w.duties.stream().anyMatch(d -> d.place.equals(p.id)))))
        places.add(p);
    return places;
  }

  public static List<StatRow> statistics(State s, LocalDate today, boolean future) {
    List<Place> places = statisticPlaces(s);
    List<Person> people = new ArrayList<>(s.people);
    java.text.Collator collator = java.text.Collator.getInstance(new Locale("tr", "TR"));
    people.sort(
        (a, b) -> {
          int n = collator.compare(a.name, b.name);
          return n != 0 ? n : a.id.compareTo(b.id);
        });
    List<StatRow> result = new ArrayList<>();
    for (Person person : people) {
      StatRow row = new StatRow(person, places.size() + 2);
      for (Week week : s.weeks)
        for (Duty duty : week.duties)
          if (duty.person.equals(person.id) && duty.date.isAfter(today) == future) {
            if (duty.place.equals(WORK)) row.counts[weekend(duty.date) ? 1 : 0]++;
            else
              for (int i = 0; i < places.size(); i++)
                if (places.get(i).id.equals(duty.place)) row.counts[i + 2]++;
          }
      result.add(row);
    }
    return result;
  }

  public static String leaveOverrideKey(Duty duty, Leave leave) {
    return duty.date + "|" + duty.person + "|" + duty.place + "|" + leave.id;
  }

  public static String simultaneousKey(LocalDate date, String person, String a, String b) {
    return date + "|" + person + "|" + (a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a);
  }

  public static List<String> validate(State s, Week w) {
    List<String> errors = new ArrayList<>();
    Set<String> slots = new HashSet<>();
    Map<String, List<String>> persons = new HashMap<>();
    Map<String, Integer> branch = new HashMap<>();
    for (Duty d : w.duties) {
      if (d.date.isBefore(w.monday) || d.date.isAfter(w.monday.plusDays(6)))
        errors.add("Hafta dışı tarih.");
      if (!slots.add(d.date + d.place)) errors.add("Aynı yerde birden fazla kişi.");
      List<String> places =
          persons.computeIfAbsent(d.date + "|" + d.person, key -> new ArrayList<>());
      for (String other : places) {
        if (!other.equals(d.place)
            && !w.simultaneousOverrides.contains(simultaneousKey(d.date, d.person, other, d.place)))
          errors.add(s.name(d.person) + " aynı gün farklı görev yerlerinde; teyit gerekli.");
      }
      places.add(d.place);
      if (rest(w, d.person, d.date))
        errors.add(s.name(d.person) + " hafta sonu nöbetinden önce perşembe/cuma izinli olmalı.");
      if (s.people.stream().noneMatch(p -> p.id.equals(d.person) && p.active))
        errors.add(s.name(d.person) + " pasif veya bulunamadı.");
      if (s.leaves.stream()
          .anyMatch(
              leave ->
                  leave.blocks(d.person, d.place, d.date)
                      && !w.leaveOverrides.contains(leaveOverrideKey(d, leave))))
        errors.add(s.name(d.person) + " için mazeret/kısıt istisnası teyidi gerekli.");
      Place location = s.places.stream().filter(p -> p.id.equals(d.place)).findFirst().orElse(null);
      if (location == null) {
        errors.add("Bilinmeyen görev yeri.");
        continue;
      }
      boolean recorded =
          s.weeks.stream()
              .filter(old -> old.monday.equals(w.monday))
              .flatMap(old -> old.duties.stream())
              .anyMatch(
                  old ->
                      old.place.equals(d.place)
                          && old.person.equals(d.person)
                          && old.date.equals(d.date));
      if (location.deleted && !recorded)
        errors.add(location.name + " silinmiş; yeni atama yapılamaz.");
      if (!location.on(d.date) && !recorded) errors.add(location.name + " seçilen gün kapalı.");
      if (location.limit > 0) {
        String key = d.place + "|" + d.person;
        int n = branch.merge(key, 1, Integer::sum);
        if (n > location.limit && !w.limitOverrides.contains(key))
          errors.add(location.name + " kişi başı haftalık sınırı aşıldı.");
      }
    }

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
    if ((workWeekendDays(s, w.monday).size() == 2 || (!sat.isEmpty() && !sun.isEmpty()))
        && !sat.equals(sun)) errors.add("Cumartesi ve pazar aynı kişi olmalı.");
    return errors;
  }

  public static void approve(State s, Week w) {
    List<String> invalid = validate(s, w);
    if (!invalid.isEmpty()) throw new IllegalArgumentException(String.join("\n", invalid));
    s.weeks.removeIf(x -> x.monday.equals(w.monday));
    s.weeks.add(w);
    s.weeks.sort(Comparator.comparing(x -> x.monday));
  }

  public static Week copy(Week w) {
    Week c = new Week(w.monday);
    c.cursor = w.cursor;
    c.rotationRevision = w.rotationRevision;
    for (Duty d : w.duties) c.duties.add(new Duty(d.date, d.place, d.person));
    for (Debt d : w.debts) c.debts.add(new Debt(d.person, d.end));
    c.warnings.addAll(w.warnings);
    c.limitOverrides.addAll(w.limitOverrides);
    c.simultaneousOverrides.addAll(w.simultaneousOverrides);
    c.leaveOverrides.addAll(w.leaveOverrides);
    return c;
  }
}
