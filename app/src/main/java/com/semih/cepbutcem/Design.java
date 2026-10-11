package com.semih.cepbutcem;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.time.LocalDate;
import java.util.*;

/** Native visuals: no web runtime, fonts or network required. Values have text equivalents. */
public final class Design {
  private Design() {}

  public static int spendingColor(double ratio, boolean light) {
    switch (Finance.spendingBand(ratio)) {
      case 0:
        return light ? 0xFF007F65 : 0xFF35D6AE;
      case 1:
        return light ? 0xFF9D5700 : 0xFFF6BD60;
      case 2:
        return light ? 0xFFB42343 : 0xFFFF8096;
      default:
        return light ? 0xFF5552DF : 0xFFA5AEFF;
    }
  }

  /** Runs on the UI thread. Shared by the gauge and real Android animation tests. */
  public static android.animation.ValueAnimator ratioAnimator(
      double target, boolean enabled, java.util.function.DoubleConsumer update) {
    if (!enabled || Double.isNaN(target)) {
      update.accept(target);
      return null;
    }
    android.animation.ValueAnimator animation = android.animation.ValueAnimator.ofFloat(0, 1);
    animation.setDuration(1000);
    animation.setInterpolator(new android.view.animation.DecelerateInterpolator());
    animation.addUpdateListener(a -> update.accept(target * (Float) a.getAnimatedValue()));
    return animation;
  }

  /** App.jsx's easeOutCubic, kept separate from the older compact meters. */
  public static android.animation.ValueAnimator gaugeAnimator(
      double target, boolean enabled, java.util.function.DoubleConsumer update) {
    if (!enabled || !Double.isFinite(target)) {
      update.accept(target);
      return null;
    }
    android.animation.ValueAnimator a = android.animation.ValueAnimator.ofFloat(0, 1);
    a.setDuration(3500);
    a.setInterpolator(t -> 1 - (1 - t) * (1 - t) * (1 - t));
    a.addUpdateListener(v -> update.accept(Math.max(0, target) * (Float) v.getAnimatedValue()));
    return a;
  }

  /** A decorative card surface; arcs are branding, not a financial chart. */
  public static final class BalanceSurface extends android.graphics.drawable.Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final int[] START = {0xFF143C35, 0xFF4A351B, 0xFF4D2432, 0xFF293256};
    private static final int[] END = {0xFF087866, 0xFF986620, 0xFFA13B53, 0xFF465F9D};
    private final RectF area = new RectF();
    private final Path clip = new Path();
    private LinearGradient gradient;
    private int alpha = 255;
    private final int band;
    private final boolean light;
    private final float radius;

    public BalanceSurface(int band, float radius) {
      this(band, radius, false);
    }

    public BalanceSurface(int band, float radius, boolean light) {
      this.band = band;
      this.radius = radius;
      this.light = light;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      area.set(bounds);
      clip.reset();
      clip.addRoundRect(area, radius, radius, Path.Direction.CW);
      int index = band < 0 || band > 2 ? 3 : band;
      gradient =
          new LinearGradient(
              bounds.left,
              bounds.top,
              bounds.right,
              bounds.bottom,
              light
                  ? new int[] {0xFF583B2C, 0xFF60432C, 0xFF63332D, 0xFF593B32}[index]
                  : START[index],
              light
                  ? new int[] {0xFFA74E32, 0xFFA16B32, 0xFFAB4240, 0xFF886052}[index]
                  : END[index],
              Shader.TileMode.CLAMP);
    }

    @Override
    public void draw(Canvas canvas) {
      Rect bounds = getBounds();
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(Color.WHITE);
      paint.setAlpha(alpha);
      paint.setShader(gradient);
      canvas.drawRoundRect(area, radius, radius, paint);
      paint.setShader(null);
      canvas.save();
      canvas.clipPath(clip);
      float size = bounds.width();
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(1.5f);
      paint.setColor(Color.WHITE);
      paint.setAlpha(alpha * 32 / 255);
      for (int i = 0; i < 4; i++)
        canvas.drawCircle(
            bounds.right + size * .12f,
            bounds.top + bounds.height() * .2f,
            size * (.28f + i * .13f),
            paint);
      canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
      this.alpha = alpha;
      invalidateSelf();
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      paint.setColorFilter(filter);
      invalidateSelf();
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }

  /** Direct Canvas port of App.jsx HarcamaGauge's 320 × 235 SVG. */
  public static final class BudgetGauge extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path needle = new Path();
    private final RectF arc = new RectF(43, 28, 277, 262);
    private final Typeface normal = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);
    private final double ratio;
    private final int textColor, mutedColor, surfaceColor, tickColor;
    private double displayed;
    private android.animation.ValueAnimator animator;

    public BudgetGauge(Context context) {
      this(context, Double.NaN, true);
    }

    public BudgetGauge(Context context, double ratio, boolean light) {
      this(
          context,
          ratio,
          light ? 0xFF302821 : 0xFFF8FAFC,
          light ? 0xFF645447 : 0xFF9EACC1,
          light ? 0xFFF5EAE1 : 0xFF182238,
          light ? 0xFF726052 : 0xFFA4B2C7);
    }

    public BudgetGauge(
        Context context, double ratio, int textColor, int mutedColor, int surfaceColor) {
      this(context, ratio, textColor, mutedColor, surfaceColor, mutedColor);
    }

    private BudgetGauge(
        Context context,
        double ratio,
        int textColor,
        int mutedColor,
        int surfaceColor,
        int tickColor) {
      super(context);
      this.ratio = Double.isFinite(ratio) ? Math.max(0, ratio) : Double.NaN;
      this.textColor = textColor;
      this.mutedColor = mutedColor;
      this.surfaceColor = surfaceColor;
      this.tickColor = tickColor;
      setContentDescription(
          Double.isNaN(this.ratio)
              ? "Bütçe kullanımı: gelir gerekli"
              : String.format(Finance.TR, "Kullanılan bütçe oranı yüzde %.1f", this.ratio));
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
      int width =
          resolveSize(Math.round(350 * getResources().getDisplayMetrics().density), widthSpec);
      setMeasuredDimension(
          width,
          resolveSize(
              Math.round(
                  Math.min(width, 350 * getResources().getDisplayMetrics().density) * 235 / 320f),
              heightSpec));
    }

    @Override
    protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      displayed = 0;
      animator =
          gaugeAnimator(
              ratio,
              android.animation.ValueAnimator.areAnimatorsEnabled(),
              v -> {
                displayed = v;
                invalidate();
              });
      if (animator != null) animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
      if (animator != null) animator.cancel();
      animator = null;
      super.onDetachedFromWindow();
    }

    private float x(double angle, double radius) {
      return (float) (160 + radius * Math.cos(Math.toRadians(angle)));
    }

    private float y(double angle, double radius) {
      return (float) (145 - radius * Math.sin(Math.toRadians(angle)));
    }

    private void label(
        Canvas c, String value, float x, float y, float size, int color, boolean strong) {
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(color);
      paint.setTypeface(strong ? bold : normal);
      paint.setTextSize(size);
      paint.setTextAlign(Paint.Align.CENTER);
      c.drawText(value, x, y, paint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float scale = Math.min(getWidth() / 320f, getHeight() / 235f);
      canvas.save();
      canvas.translate((getWidth() - 320 * scale) / 2, (getHeight() - 235 * scale) / 2);
      canvas.scale(scale, scale);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(14);
      paint.setStrokeCap(Paint.Cap.BUTT);
      for (int i = 0; i < 10; i++) {
        paint.setColor(i < 2 ? 0xFF059669 : i < 5 ? 0xFF10B981 : i < 8 ? 0xFFF59E0B : 0xFFEF4444);
        canvas.drawArc(arc, 180 + i * 18 + .7f, 16.6f, false, paint);
      }
      for (int n = 0; n <= 20; n++) {
        double angle = 180 - n * 9;
        boolean major = n % 5 == 0;
        paint.setColor(tickColor);
        paint.setAlpha(major ? 204 : 89);
        paint.setStrokeWidth(major ? 1.8f : 1);
        canvas.drawLine(
            x(angle, major ? 94 : 99),
            y(angle, major ? 94 : 99),
            x(angle, 105),
            y(angle, 105),
            paint);
      }
      paint.setAlpha(255);
      label(canvas, "0", 33, 168, 12, tickColor, false);
      label(canvas, "100", 287, 168, 12, tickColor, false);
      double angle = 180 - Math.min(100, Double.isNaN(displayed) ? 0 : displayed) * 1.8;
      needle.reset();
      needle.moveTo(x(angle + 90, 5), y(angle + 90, 5));
      needle.lineTo(x(angle, 89), y(angle, 89));
      needle.lineTo(x(angle - 90, 5), y(angle - 90, 5));
      needle.close();
      paint.setStyle(Paint.Style.FILL);
      paint.setColor(textColor);
      canvas.drawPath(needle, paint);
      canvas.drawCircle(160, 145, 8, paint);
      paint.setColor(surfaceColor);
      canvas.drawCircle(160, 145, 3, paint);
      int color =
          Double.isNaN(displayed)
              ? mutedColor
              : displayed < 50 ? 0xFF10B981 : displayed < 80 ? 0xFFF59E0B : 0xFFEF4444;
      label(
          canvas,
          Double.isNaN(displayed) ? "—" : "%" + Math.round(displayed),
          160,
          195,
          35,
          color,
          true);
      label(
          canvas,
          Double.isNaN(displayed) ? "Oran için gelir gerekli" : "Kullanılan bütçe oranı",
          160,
          218,
          12,
          mutedColor,
          false);
      canvas.restore();
    }
  }

  /** Current App.jsx's 128 × 128 segmented budget ring. */
  public static final class BudgetCircle extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area = new RectF(13, 13, 115, 115);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD),
        normal = Typeface.create("sans-serif", Typeface.NORMAL);
    private final double ratio;
    private final boolean light;
    private double displayed;
    private android.animation.ValueAnimator animator;

    public BudgetCircle(Context context) {
      this(context, Double.NaN, false);
    }

    public BudgetCircle(Context context, double ratio, boolean light) {
      super(context);
      this.ratio = Double.isFinite(ratio) ? Math.max(0, ratio) : Double.NaN;
      this.light = light;
      displayed = this.ratio;
      setContentDescription(
          Double.isNaN(this.ratio)
              ? "Bütçe oranı için henüz gelir yok"
              : String.format(Finance.TR, "Kullanılan bütçe oranı yüzde %.1f", this.ratio));
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      displayed = 0;
      animator =
          gaugeAnimator(
              ratio,
              android.animation.ValueAnimator.areAnimatorsEnabled(),
              v -> {
                displayed = v;
                invalidate();
              });
      if (animator != null) animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
      if (animator != null) animator.cancel();
      animator = null;
      super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      float scale = Math.min(getWidth(), getHeight()) / 128f;
      canvas.save();
      canvas.translate((getWidth() - 128 * scale) / 2, (getHeight() - 128 * scale) / 2);
      canvas.scale(scale, scale);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(10);
      paint.setStrokeCap(Paint.Cap.BUTT);
      paint.setColor(light ? 0x29FFFFFF : 0x1FFFFFFF);
      canvas.drawOval(area, paint);
      int[] starts = {0, 50, 80},
          lengths = {50, 30, 20},
          colors = {0xFF10B981, 0xFFF59E0B, 0xFFEF4444};
      for (int i = 0; i < 3; i++) {
        paint.setColor(colors[i]);
        paint.setAlpha(56);
        canvas.drawArc(area, -90 + starts[i] * 3.6f, lengths[i] * 3.6f, false, paint);
        paint.setAlpha(255);
        if (!Double.isNaN(displayed) && displayed > starts[i])
          canvas.drawArc(
              area,
              -90 + starts[i] * 3.6f,
              (float) Math.min(lengths[i], Math.max(0, Math.min(100, displayed) - starts[i]))
                  * 3.6f,
              false,
              paint);
      }
      paint.setStyle(Paint.Style.FILL);
      paint.setTextAlign(Paint.Align.CENTER);
      paint.setColor(Color.WHITE);
      paint.setTypeface(bold);
      String number = Double.isNaN(displayed) ? "" : Long.toString(Math.round(displayed));
      paint.setTextSize(Math.min(26, 88f / (number.length() + 1) / .65f));
      canvas.drawText(
          Double.isNaN(displayed) ? "—" : "%" + number,
          64,
          66 - (paint.ascent() + paint.descent()) / 2,
          paint);
      paint.setTypeface(normal);
      paint.setTextSize(9);
      paint.setAlpha(191);
      canvas.drawText("bütçe kullanımı", 64, 86, paint);
      paint.setAlpha(255);
      canvas.restore();
    }
  }

  public static final class SpendingMeter extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final double ratio;
    private final boolean light;
    private final boolean onAccent;
    private double displayed;
    private android.animation.ValueAnimator animator;

    public SpendingMeter(Context context) {
      this(context, Double.NaN, false);
    }

    public SpendingMeter(Context context, double ratio, boolean light) {
      this(context, ratio, light, false);
    }

    public SpendingMeter(Context context, double ratio, boolean light, boolean onAccent) {
      super(context);
      this.onAccent = onAccent;
      this.ratio = ratio;
      this.light = light;
      displayed = Double.isNaN(ratio) ? Double.NaN : 0;
      setContentDescription(
          Double.isNaN(ratio)
              ? "Harcama oranı için gelir kaydı gerekli"
              : "Gelirin harcanan kısmı yüzde " + String.format(Finance.TR, "%.1f", ratio));
    }

    private float dp(float value) {
      return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
      return value * getResources().getDisplayMetrics().scaledDensity;
    }

    @Override
    protected void onMeasure(int width, int height) {
      setMeasuredDimension(MeasureSpec.getSize(width), (int) (sp(42) + dp(40)));
    }

    @Override
    protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      animator =
          ratioAnimator(
              ratio,
              android.animation.ValueAnimator.areAnimatorsEnabled(),
              value -> {
                displayed = value;
                invalidate();
              });
      if (animator != null) animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
      if (animator != null) {
        animator.cancel();
        animator = null;
      }
      super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      int accent = onAccent ? Color.WHITE : spendingColor(ratio, light);
      paint.setColor(accent);
      paint.setStyle(Paint.Style.FILL);
      paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
      paint.setTextSize(sp(30));
      String number =
          Double.isNaN(displayed) ? "—" : String.format(Finance.TR, "%%%.1f", displayed);
      float numberWidth = paint.measureText(number);
      if (numberWidth > getWidth()) paint.setTextSize(sp(30) * getWidth() / numberWidth);
      canvas.drawText(number, 0, sp(36), paint);
      float textWidth = paint.measureText(number);
      paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
      paint.setTextSize(sp(11));
      paint.setColor(onAccent ? 0xFFDCF4EF : light ? 0xFF657286 : 0xFF9AA8BA);
      if (textWidth + dp(12) + paint.measureText("gelirin harcanan kısmı") < getWidth())
        canvas.drawText("gelirin harcanan kısmı", textWidth + dp(12), sp(35), paint);
      paint.setColor(onAccent ? 0x40FFFFFF : light ? 0xFFE5E9EE : 0xFF2B3745);
      canvas.drawRoundRect(0, sp(42) + dp(15), getWidth(), sp(42) + dp(22), dp(4), dp(4), paint);
      if (!Double.isNaN(displayed) && displayed > 0) {
        paint.setColor(accent);
        canvas.drawRoundRect(
            0,
            sp(42) + dp(15),
            (float) (getWidth() * Math.min(1, displayed / 100)),
            sp(42) + dp(22),
            dp(4),
            dp(4),
            paint);
      }
    }
  }

  public static final class Icon extends View {
    private static final float[] POINTS_0 =
        new float[] {12, 2, 15, 9, 22, 12, 15, 15, 12, 22, 9, 15, 2, 12, 9, 9};
    private static final float[] POINTS_1 =
        new float[] {5, 3, 19, 3, 19, 21, 16, 19, 12, 21, 8, 19, 5, 21};
    private static final float[] POINTS_2 = new float[] {6, 18, 18, 6, 8, 6};
    private static final float[] POINTS_3 = new float[] {6, 6, 18, 18, 8, 18};
    private static final float[] POINTS_4 = new float[] {4, 8, 7, 5, 18, 5, 21, 8, 21, 12};
    private static final float[] POINTS_5 = new float[] {20, 16, 17, 19, 6, 19, 3, 16, 3, 12};
    private static final float[] POINTS_6 = new float[] {14, 5, 7, 12, 14, 19};
    private final Path pathBuffer = new Path();

    public Icon(Context context) {
      this(context, "wallet", 0xFF35D6AE);
    }

    private final String name;
    private final int color;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Icon(Context c, String name, int color) {
      super(c);
      this.name = name;
      this.color = color;
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      canvas.save();
      canvas.translate(
          (getWidth() - Math.min(getWidth(), getHeight())) / 2f,
          (getHeight() - Math.min(getWidth(), getHeight())) / 2f);
      canvas.scale(
          Math.min(getWidth(), getHeight()) / 24f, Math.min(getWidth(), getHeight()) / 24f);
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1.7f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
      switch (name) {
        case "home":
          for (int i = 0; i < 4; i++) {
            float x = 4 + (i % 2) * 9, y = 4 + (i / 2) * 9;
            canvas.drawRoundRect(x, y, x + 6, y + 6, 1.5f, 1.5f, p);
          }
          break;
        case "list":
          for (int i = 0; i < 3; i++) {
            canvas.drawCircle(4, 6 + i * 6, .6f, p);
            canvas.drawLine(9, 6 + i * 6, 20, 6 + i * 6, p);
          }
          break;
        case "chart":
          canvas.drawLine(4, 20, 21, 20, p);
          canvas.drawLine(7, 15, 7, 9, p);
          canvas.drawLine(12, 15, 12, 4, p);
          canvas.drawLine(17, 15, 17, 7, p);
          break;
        case "target":
          canvas.drawCircle(12, 12, 8, p);
          canvas.drawCircle(12, 12, 4, p);
          canvas.drawLine(12, 12, 21, 3, p);
          canvas.drawLine(17, 3, 21, 3, p);
          canvas.drawLine(21, 3, 21, 7, p);
          break;
        case "settings":
          canvas.drawCircle(12, 12, 3, p);
          canvas.drawCircle(12, 12, 7, p);
          for (int i = 0; i < 8; i++) {
            double r = i * Math.PI / 4;
            canvas.drawLine(
                12 + (float) Math.cos(r) * 7,
                12 + (float) Math.sin(r) * 7,
                12 + (float) Math.cos(r) * 10,
                12 + (float) Math.sin(r) * 10,
                p);
          }
          break;
        case "calendar":
          canvas.drawRoundRect(4, 5, 20, 21, 3, 3, p);
          canvas.drawLine(4, 10, 20, 10, p);
          canvas.drawLine(8, 3, 8, 7, p);
          canvas.drawLine(16, 3, 16, 7, p);
          canvas.drawLine(8, 14, 10, 14, p);
          canvas.drawLine(14, 17, 16, 17, p);
          break;
        case "wallet":
          canvas.drawRoundRect(3, 6, 21, 20, 3, 3, p);
          canvas.drawLine(5, 6, 17, 3, p);
          canvas.drawRoundRect(15, 11, 22, 16, 1.5f, 1.5f, p);
          canvas.drawCircle(18, 13.5f, .5f, p);
          break;
        case "card":
          canvas.drawRoundRect(3, 5, 21, 19, 3, 3, p);
          canvas.drawLine(3, 10, 21, 10, p);
          canvas.drawLine(6, 15, 10, 15, p);
          break;
        case "spark":
          path(canvas, POINTS_0, true);
          break;
        case "receipt":
          path(canvas, POINTS_1, true);
          canvas.drawLine(8, 8, 16, 8, p);
          canvas.drawLine(8, 12, 14, 12, p);
          break;
        case "up":
          path(canvas, POINTS_2, false);
          canvas.drawLine(18, 6, 18, 16, p);
          break;
        case "down":
          path(canvas, POINTS_3, false);
          canvas.drawLine(18, 18, 18, 8, p);
          break;
        case "repeat":
          path(canvas, POINTS_4, false);
          path(canvas, POINTS_5, false);
          canvas.drawLine(17, 8, 21, 8, p);
          canvas.drawLine(3, 16, 7, 16, p);
          break;
        case "back":
          path(canvas, POINTS_6, false);
          break;
        default:
          canvas.drawCircle(12, 12, 7, p);
      }
      canvas.restore();
    }

    private void path(Canvas c, float[] points, boolean closed) {
      Path path = pathBuffer;
      path.reset();
      path.moveTo(points[0], points[1]);
      for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
      if (closed) path.close();
      c.drawPath(path, p);
    }
  }

  public static final class Gauge extends View {
    private final RectF arc = new RectF();

    public Gauge(Context context) {
      this(context, Double.NaN, false);
    }

    private final double ratio;
    private double displayedRatio;
    private android.animation.ValueAnimator animator;
    private final boolean light;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public Gauge(Context c, double ratio, boolean light) {
      super(c);
      this.ratio = ratio;
      this.displayedRatio = Double.isNaN(ratio) ? Double.NaN : 0;
      this.light = light;
      setContentDescription(
          Double.isNaN(ratio)
              ? "Harcama oranı için gelir kaydı gerekli"
              : "Gelirin harcanan kısmı yüzde " + String.format(Finance.TR, "%.1f", ratio));
    }

    @Override
    protected void onAttachedToWindow() {
      super.onAttachedToWindow();
      if (Double.isNaN(ratio)) return;
      animator =
          ratioAnimator(
              ratio,
              android.animation.ValueAnimator.areAnimatorsEnabled(),
              value -> {
                displayedRatio = value;
                invalidate();
              });
      if (animator != null) animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
      if (animator != null) {
        animator.cancel();
        animator = null;
      }
      super.onDetachedFromWindow();
    }

    private float dp(float x) {
      return x * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int w, int h) {
      setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(158));
    }

    @Override
    protected void onDraw(Canvas c) {
      float width = Math.min(getWidth() - dp(26), dp(250)),
          cx = getWidth() / 2f,
          cy = dp(128),
          radius = width / 2;
      RectF r = arc;
      r.set(cx - radius, cy - radius, cx + radius, cy + radius);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeWidth(dp(13));
      p.setColor(light ? 0xFFE5EBF3 : 0xFF26334D);
      c.drawArc(r, 180, 180, false, p);
      int color = spendingColor(ratio, light);
      if (!Double.isNaN(displayedRatio) && displayedRatio > 0) {
        p.setColor(color);
        c.drawArc(r, 180, (float) Math.min(100, displayedRatio) * 1.8f, false, p);
      }
      p.setStyle(Paint.Style.FILL);
      p.setTextAlign(Paint.Align.CENTER);
      p.setColor(light ? 0xFF151A2D : 0xFFF8FAFC);
      p.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      p.setTextSize(dp(34));
      String value =
          Double.isNaN(displayedRatio) ? "—" : String.format(Finance.TR, "%%%.1f", displayedRatio);
      float length = p.measureText(value);
      if (length > width * .8f) p.setTextSize(dp(34) * width * .8f / length);
      c.drawText(value, cx, cy - dp(23), p);
      p.setTypeface(Typeface.DEFAULT);
      p.setTextSize(dp(11));
      p.setColor(light ? 0xFF68728A : 0xFF9EACC1);
      c.drawText("GELİRİN HARCANAN KISMI", cx, cy - dp(4), p);
      p.setTextSize(dp(10));
      c.drawText("%0", cx - radius, cy + dp(24), p);
      c.drawText("%100", cx + radius, cy + dp(24), p);
    }
  }

  public static final class Heatmap extends View {
    private static final String[] WEEKDAYS = {"Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"};

    public Heatmap(Context context) {
      this(
          context,
          LocalDate.now(),
          LocalDate.now(),
          LocalDate.now(),
          Collections.emptyMap(),
          false,
          date -> {});
    }

    private final LocalDate start, end, today;
    private final Map<LocalDate, Long> amounts;
    private final boolean light;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final java.util.function.Consumer<LocalDate> selected;

    public Heatmap(
        Context c,
        LocalDate start,
        LocalDate end,
        LocalDate today,
        Map<LocalDate, Long> amounts,
        boolean light,
        java.util.function.Consumer<LocalDate> selected) {
      super(c);
      this.start = start;
      this.end = end;
      this.today = today;
      this.amounts = amounts;
      this.light = light;
      this.selected = selected;
      setContentDescription(
          "Maaş dönemi harcama haritası. Gün özetleri takvimin altındaki Gün seç listesinden"
              + " okunabilir.");
    }

    private float dp(float x) {
      return x * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int w, int h) {
      setMeasuredDimension(MeasureSpec.getSize(w), (int) dp(194));
    }

    @Override
    protected void onDraw(Canvas c) {
      int offset = start.getDayOfWeek().getValue() - 1;
      long max = 1;
      for (long n : amounts.values()) max = Math.max(max, n);
      float cell = getWidth() / 7f, top = dp(27);
      p.setTypeface(Typeface.DEFAULT);
      p.setTextSize(dp(10));
      p.setTextAlign(Paint.Align.CENTER);
      p.setStyle(Paint.Style.FILL);
      String[] labels = WEEKDAYS;
      for (int i = 0; i < 7; i++) {
        p.setColor(light ? 0xFF68728A : 0xFF9EACC1);
        c.drawText(labels[i], cell * (i + .5f), dp(13), p);
      }
      int i = 0;
      for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1), i++) {
        int index = offset + i;
        float x = cell * (index % 7), y = top + (index / 7) * dp(27);
        long amount = amounts.getOrDefault(d, 0L);
        boolean future = d.isAfter(today);
        int base = light ? 0xFFF1F4F9 : 0xFF202C43;
        if (amount > 0 && !future) {
          float strength = .2f + .8f * (float) Math.sqrt(amount / (double) max);
          int accent = light ? 0xFF00876C : 0xFF35D6AE;
          base = blend(base, accent, strength);
        }
        p.setColor(base);
        c.drawRoundRect(x + dp(3), y, x + cell - dp(3), y + dp(23), dp(5), dp(5), p);
        p.setColor(
            future
                ? (light ? 0xFF8490A7 : 0xFF7F90AA)
                : (amount > 0
                    ? (Color.green(base) > 140 ? 0xFF102B27 : Color.WHITE)
                    : (light ? 0xFF323A51 : 0xFFDBE3EF)));
        c.drawText(Integer.toString(d.getDayOfMonth()), x + cell / 2, y + dp(15), p);
      }
    }

    private int blend(int a, int b, float t) {
      return Color.rgb(
          Math.round(Color.red(a) * (1 - t) + Color.red(b) * t),
          Math.round(Color.green(a) * (1 - t) + Color.green(b) * t),
          Math.round(Color.blue(a) * (1 - t) + Color.blue(b) * t));
    }

    @Override
    public boolean onTouchEvent(android.view.MotionEvent e) {
      if (e.getAction() == android.view.MotionEvent.ACTION_UP) {
        int col = (int) (e.getX() / (getWidth() / 7f)), row = (int) ((e.getY() - dp(27)) / dp(27));
        if (e.getY() >= dp(27) && col >= 0 && col < 7 && row >= 0) {
          int day = row * 7 + col - (start.getDayOfWeek().getValue() - 1);
          LocalDate d = start.plusDays(day);
          if (!d.isBefore(start) && !d.isAfter(end)) selected.accept(d);
        }
        performClick();
        return true;
      }
      return true;
    }

    @Override
    public boolean performClick() {
      super.performClick();
      return true;
    }
  }
}
