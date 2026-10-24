package com.semih.sicil360;

import android.os.Bundle;
import android.view.*;
import android.widget.ScrollView;
import java.util.*;

/** Screen-specific viewport memory. UI refreshes never imply navigation to the top. */
final class ScrollMemory {
  static final class Mark {
    String anchor = "";
    int offset, y;
  }

  final LinkedHashMap<String, Mark> positions = new LinkedHashMap<>(32, .75f, true);
  ScrollView active;
  String key = "";
  Mark pending;

  void remember() {
    if (active == null || active.getChildCount() == 0 || active.getHeight() == 0) return;
    if (pending != null
        && !pending.anchor.isEmpty()
        && active.findViewWithTag(pending.anchor) == null
        && active.getChildAt(0).getHeight() < pending.y + active.getHeight()) return;
    Mark mark = new Mark();
    mark.y = active.getScrollY();
    View anchor = findVisible(active.getChildAt(0));
    if (anchor != null) {
      mark.anchor = (String) anchor.getTag();
      mark.offset = relativeTop(anchor);
    }
    positions.put(key, mark);
    while (positions.size() > 32) positions.remove(positions.keySet().iterator().next());
  }

  int relativeTop(View child) {
    int[] a = new int[2], b = new int[2];
    child.getLocationInWindow(a);
    active.getLocationInWindow(b);
    return a[1] - b[1];
  }

  View findVisible(View view) {
    int top = relativeTop(view);
    if (top + view.getHeight() <= 0 || top >= active.getHeight()) return null;
    if (view.getTag() instanceof String && view.getHeight() > 0) return view;
    if (view instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        View found = findVisible(group.getChildAt(i));
        if (found != null) return found;
      }
    }
    return null;
  }

  void attach(ScrollView view, String context) {
    active = view;
    key = context;
    pending = positions.get(context);
    view.getChildAt(0)
        .addOnLayoutChangeListener(
            (v, l, t, r, b, ol, ot, or, ob) -> {
              if (active == view) apply();
            });
    view.getViewTreeObserver()
        .addOnPreDrawListener(
            new ViewTreeObserver.OnPreDrawListener() {
              @Override
              public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);
                if (active == view) apply();
                return true;
              }
            });
    view.post(
        () -> {
          if (active == view) apply();
        });
  }

  void apply() {
    if (active == null || pending == null || active.getChildCount() == 0) return;
    int desired = pending.y;
    View anchor = pending.anchor.isEmpty() ? null : active.findViewWithTag(pending.anchor);
    if (anchor != null && anchor.getHeight() > 0)
      desired = active.getScrollY() + relativeTop(anchor) - pending.offset;
    active.scrollTo(0, Math.max(0, desired));
  }

  void userInput() {
    pending = null;
  }

  void save(Bundle out) {
    remember();
    Bundle entries = new Bundle();
    for (Map.Entry<String, Mark> entry : positions.entrySet()) {
      Bundle mark = new Bundle();
      mark.putString("anchor", entry.getValue().anchor);
      mark.putInt("offset", entry.getValue().offset);
      mark.putInt("y", entry.getValue().y);
      entries.putBundle(entry.getKey(), mark);
    }
    out.putBundle("scroll-memory", entries);
  }

  void load(Bundle state) {
    if (state == null) return;
    Bundle entries = state.getBundle("scroll-memory");
    if (entries == null) return;
    for (String context : entries.keySet()) {
      if (positions.size() >= 32) break;
      Bundle data = entries.getBundle(context);
      if (data == null) continue;
      Mark mark = new Mark();
      mark.anchor = data.getString("anchor", "");
      mark.offset = data.getInt("offset");
      mark.y = Math.max(0, data.getInt("y"));
      positions.put(context, mark);
    }
  }
}
