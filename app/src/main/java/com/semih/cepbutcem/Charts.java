package com.semih.cepbutcem;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.*;

/** Small native charts: the same figures also appear in accessible text rows. */
public final class Charts extends View {
  public static final int[] COLORS = {
    0xFF35D6AE, 0xFF739FFF, 0xFFF6BD69, 0xFFDF8CF5, 0xFFFF7B8B, 0xFF7FD2EF, 0xFFA9C774, 0xFFB0B8C7
  };
  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final List<Long> a, b;
  private final List<String> labels;
  private final boolean ring;
  private boolean light;

  public Charts(Context context) {
    this(context, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false);
  }

  public Charts(Context context, List<Long> a, List<Long> b, List<String> labels, boolean ring) {
    super(context);
    this.a = a;
    this.b = b;
    this.labels = labels;
    this.ring = ring;
    light = context instanceof MainActivity && ((MainActivity) context).isLightTheme();
    setMinimumHeight(dp(ring ? 200 : 210));
    setContentDescription(
        ring
            ? "Kategori dağılımı grafiği; tutarlar aşağıdaki listede."
            : "Gelir ve gider eğilimi grafiği; aylık tutarlar aşağıdaki listede.");
  }

  private int dp(float n) {
    return Math.round(n * getResources().getDisplayMetrics().density);
  }

  @Override
  protected void onMeasure(int w, int h) {
    setMeasuredDimension(MeasureSpec.getSize(w), dp(ring ? 200 : 210));
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (a.isEmpty()) return;
    if (ring) drawRing(canvas);
    else drawBars(canvas);
  }

  private void drawRing(Canvas c) {
    long sum = 0;
    for (long n : a) sum += n;
    if (sum == 0) return;
    float size = dp(150), x = getWidth() / 2f, y = getHeight() / 2f;
    RectF r = new RectF(x - size / 2, y - size / 2, x + size / 2, y + size / 2);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(dp(24));
    paint.setStrokeCap(Paint.Cap.BUTT);
    float start = -90;
    for (int i = 0; i < a.size(); i++) {
      float sweep = 360f * a.get(i) / sum;
      paint.setColor(COLORS[i % COLORS.length]);
      c.drawArc(r, start, Math.max(0, sweep - 2), false, paint);
      start += sweep;
    }
    paint.setStyle(Paint.Style.FILL);
    paint.setColor(light ? 0xFF536477 : 0xFF96A6B4);
    paint.setTextSize(dp(12));
    paint.setTextAlign(Paint.Align.CENTER);
    c.drawText("TOPLAM GİDER", x, y - dp(8), paint);
    paint.setColor(light ? 0xFF17283A : 0xFFF0F5F7);
    paint.setTextSize(dp(18));
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    c.drawText(compact(sum), x, y + dp(17), paint);
    paint.setTypeface(Typeface.DEFAULT);
  }

  private String compact(long cents) {
    if (cents >= 100_000_000) return String.format(Finance.TR, "%.1f mn ₺", cents / 100_000_000d);
    if (cents >= 100_000) return String.format(Finance.TR, "%.1f bin ₺", cents / 100_000d);
    return Finance.money(cents);
  }

  private void drawBars(Canvas c) {
    long max = 1;
    for (long n : a) max = Math.max(max, n);
    for (long n : b) max = Math.max(max, n);
    float left = dp(46), right = getWidth() - dp(6), top = dp(16), bottom = getHeight() - dp(32);
    paint.setStrokeWidth(dp(1));
    paint.setStyle(Paint.Style.FILL);
    paint.setTypeface(Typeface.DEFAULT);
    paint.setTextSize(dp(10));
    for (int i = 0; i <= 3; i++) {
      float y = bottom - (bottom - top) * i / 3;
      paint.setColor(light ? 0xFFD7E0E8 : 0xFF2B3A47);
      c.drawLine(left, y, right, y, paint);
      paint.setColor(light ? 0xFF536477 : 0xFF96A6B4);
      paint.setTextAlign(Paint.Align.RIGHT);
      c.drawText(compact(max * i / 3).replace(" ₺", ""), left - dp(6), y + dp(3), paint);
    }
    float group = (right - left) / a.size(), width = Math.min(dp(14), group * .27f);
    for (int i = 0; i < a.size(); i++) {
      float x = left + group * (i + .5f);
      paint.setColor(COLORS[0]);
      c.drawRoundRect(
          x - width - dp(2),
          bottom - (bottom - top) * a.get(i) / max,
          x - dp(2),
          bottom,
          dp(3),
          dp(3),
          paint);
      paint.setColor(COLORS[4]);
      c.drawRoundRect(
          x + dp(2),
          bottom - (bottom - top) * b.get(i) / max,
          x + width + dp(2),
          bottom,
          dp(3),
          dp(3),
          paint);
      paint.setColor(light ? 0xFF536477 : 0xFF96A6B4);
      paint.setTextAlign(Paint.Align.CENTER);
      c.drawText(labels.get(i), x, bottom + dp(20), paint);
    }
  }
}
