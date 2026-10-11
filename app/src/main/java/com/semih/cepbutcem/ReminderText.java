package com.semih.cepbutcem;

import java.time.LocalDate;
import java.util.*;

/** Turkish digest text, limited to payments whose actual due date is today. */
public final class ReminderText {
  private ReminderText() {}

  public static String today(List<Reminders.Due> payments, LocalDate today) {
    Set<String> unique = new LinkedHashSet<>();
    for (Reminders.Due p : payments)
      if (p.date.equals(today)) {
        String label = p.label == null ? "" : p.label.replaceAll("\\s+", " ").trim();
        if (!label.isEmpty()) unique.add(label);
      }
    if (unique.isEmpty()) return null;
    List<String> names = new ArrayList<>(unique);
    if (names.size() > 10) {
      int others = names.size() - 10;
      names = new ArrayList<>(names.subList(0, 10));
      names.add("diğer " + others + " ödeme");
    }
    String joined =
        names.size() == 1
            ? names.get(0)
            : String.join(", ", names.subList(0, names.size() - 1))
                + " ve "
                + names.get(names.size() - 1);
    return "Bugün " + joined + " son ödeme günü lütfen kontrol ediniz!";
  }
}
