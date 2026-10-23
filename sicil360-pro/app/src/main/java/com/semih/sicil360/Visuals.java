package com.semih.sicil360;

import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;

/** Offline Canvas artwork, adapted from the repository's budget dashboard design language. */
final class Visuals {
  static GradientDrawable surface(int color, float radius, int border) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(radius);
    if (border != 0) d.setStroke(1, border);
    return d;
  }

  static Drawable touch(int color, float radius, int border) {
    return new RippleDrawable(
        ColorStateList.valueOf(0x244ACFA8), surface(color, radius, border), null);
  }

  static Drawable whatsappButton(float radius, boolean dark) {
    GradientDrawable fill =
        new GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            dark ? new int[] {0xFF00AD85, 0xFF008B68} : new int[] {0xFF08AC82, 0xFF009B74});
    fill.setCornerRadius(radius);
    return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), fill, null);
  }

  /** Full-width black separator remains legible in light and dark table rows. */
  static final class TableRow extends Drawable {
    final Paint paint = new Paint();
    final int color, thickness;

    TableRow(int color, int thickness) {
      this.color = color;
      this.thickness = thickness;
    }

    @Override
    public void draw(Canvas canvas) {
      Rect r = getBounds();
      paint.setColor(color);
      canvas.drawRect(r, paint);
      paint.setColor(Color.BLACK);
      canvas.drawRect(r.left, r.bottom - thickness, r.right, r.bottom, paint);
    }

    @Override
    public void setAlpha(int alpha) {
      paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.OPAQUE;
    }
  }

  static final class Hero extends Drawable {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final float radius;

    Hero(float radius) {
      this.radius = radius;
    }

    @Override
    public void draw(Canvas c) {
      RectF area = new RectF(getBounds());
      p.setShader(
          new LinearGradient(
              area.left,
              area.top,
              area.right,
              area.bottom,
              new int[] {0xFF123E44, 0xFF006A70, 0xFF138B78},
              null,
              Shader.TileMode.CLAMP));
      p.setStyle(Paint.Style.FILL);
      c.drawRoundRect(area, radius, radius, p);
      p.setShader(null);
      Path clip = new Path();
      clip.addRoundRect(area, radius, radius, Path.Direction.CW);
      c.save();
      c.clipPath(clip);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1);
      p.setColor(0x22FFFFFF);
      for (int i = 0; i < 4; i++)
        c.drawCircle(
            area.right + area.width() * .18f,
            area.top + area.height() * .28f,
            area.width() * (.32f + i * .16f),
            p);
      c.restore();
    }

    @Override
    public void setAlpha(int value) {
      p.setAlpha(value);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      p.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }

  static final class Icon extends Drawable {
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final String name;
    final int color, size;

    Icon(String name, int color, int size) {
      this.name = name;
      this.color = color;
      this.size = size;
      setBounds(0, 0, size, size);
    }

    @Override
    public int getIntrinsicWidth() {
      return size;
    }

    @Override
    public int getIntrinsicHeight() {
      return size;
    }

    @Override
    public void draw(Canvas c) {
      c.save();
      c.translate(getBounds().left, getBounds().top);
      c.scale(getBounds().width() / 24f, getBounds().height() / 24f);
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1.7f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
      switch (name) {
        case "search":
          c.drawCircle(10, 10, 6, p);
          line(c, 15, 15, 21, 21);
          break;
        case "whatsapp":
          Path bubble = new Path();
          bubble.moveTo(5, 18);
          bubble.cubicTo(-1, 10, 5, 1, 13, 2);
          bubble.cubicTo(23, 2, 26, 15, 18, 20);
          bubble.cubicTo(14, 23, 9, 22, 7, 20);
          bubble.lineTo(2, 22);
          bubble.close();
          c.drawPath(bubble, p);
          Path handset = new Path();
          handset.moveTo(8, 7);
          handset.cubicTo(5, 8, 8, 15, 14, 17);
          handset.cubicTo(17, 18, 19, 15, 16, 14);
          handset.lineTo(14, 15);
          handset.cubicTo(11, 14, 10, 12, 9, 10);
          handset.lineTo(10, 9);
          handset.close();
          c.drawPath(handset, p);
          break;
        case "chat":
          c.drawRoundRect(3, 4, 21, 18, 3, 3, p);
          line(c, 7, 18, 5, 22);
          line(c, 5, 22, 12, 18);
          line(c, 7, 9, 17, 9);
          line(c, 7, 13, 14, 13);
          break;
        case "calendar":
          c.drawRoundRect(4, 5, 20, 21, 3, 3, p);
          line(c, 4, 10, 20, 10);
          line(c, 8, 3, 8, 7);
          line(c, 16, 3, 16, 7);
          line(c, 8, 14, 10, 14);
          line(c, 14, 17, 16, 17);
          break;
        case "people":
          c.drawCircle(9, 8, 3, p);
          c.drawArc(3, 13, 15, 24, 180, 180, false, p);
          c.drawArc(14, 5, 20, 11, -80, 160, false, p);
          c.drawArc(13, 14, 22, 24, -90, 90, false, p);
          break;
        case "leave":
          c.drawRoundRect(5, 4, 19, 21, 2, 2, p);
          line(c, 9, 3, 15, 3);
          line(c, 9, 10, 15, 10);
          line(c, 9, 14, 15, 14);
          line(c, 9, 18, 12, 18);
          break;
        case "chart":
          line(c, 4, 20, 21, 20);
          line(c, 7, 15, 7, 10);
          line(c, 12, 15, 12, 4);
          line(c, 17, 15, 17, 7);
          break;
        case "settings":
          c.drawCircle(12, 12, 3, p);
          c.drawCircle(12, 12, 7, p);
          for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            line(
                c,
                12 + (float) Math.cos(a) * 7,
                12 + (float) Math.sin(a) * 7,
                12 + (float) Math.cos(a) * 10,
                12 + (float) Math.sin(a) * 10);
          }
          break;
        case "moon":
          Path moon = new Path();
          moon.moveTo(19.5f, 15.4f);
          moon.cubicTo(12, 18, 6, 11, 9.5f, 4);
          moon.cubicTo(3, 6, 2, 15, 8, 19);
          moon.cubicTo(12, 22, 18, 20, 19.5f, 15.4f);
          moon.close();
          c.drawPath(moon, p);
          break;
        case "sun":
          c.drawCircle(12, 12, 4, p);
          for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            line(
                c,
                12 + (float) Math.cos(angle) * 7,
                12 + (float) Math.sin(angle) * 7,
                12 + (float) Math.cos(angle) * 10,
                12 + (float) Math.sin(angle) * 10);
          }
          break;
        case "chevron-down":
          line(c, 6, 9, 12, 15);
          line(c, 12, 15, 18, 9);
          break;
        case "chevron-up":
          line(c, 6, 15, 12, 9);
          line(c, 12, 9, 18, 15);
          break;
        case "edit":
          line(c, 5, 16, 15, 6);
          line(c, 15, 6, 19, 10);
          line(c, 19, 10, 9, 20);
          line(c, 9, 20, 4, 21);
          line(c, 4, 21, 5, 16);
          line(c, 14, 7, 18, 11);
          break;
        case "building":
          c.drawRoundRect(5, 3, 19, 21, 2, 2, p);
          for (int y = 7; y < 16; y += 4) {
            line(c, 9, y, 10, y);
            line(c, 14, y, 15, y);
          }
          line(c, 10, 21, 10, 17);
          line(c, 14, 21, 14, 17);
          break;
        case "plus":
          line(c, 12, 5, 12, 19);
          line(c, 5, 12, 19, 12);
          break;
        case "check":
          line(c, 5, 12, 10, 17);
          line(c, 10, 17, 20, 6);
          break;
        case "pdf":
          c.drawRoundRect(5, 3, 19, 21, 2, 2, p);
          line(c, 9, 16, 15, 16);
          line(c, 12, 7, 12, 13);
          line(c, 9, 10, 12, 13);
          line(c, 12, 13, 15, 10);
          break;
        case "clock":
          c.drawCircle(12, 12, 9, p);
          line(c, 12, 7, 12, 12);
          line(c, 12, 12, 16, 14);
          break;
        case "spark":
          path(c, new float[] {12, 3, 14, 9, 21, 12, 14, 15, 12, 21, 10, 15, 3, 12, 10, 9}, true);
          break;
        case "left":
          path(c, new float[] {15, 5, 8, 12, 15, 19}, false);
          break;
        case "right":
          path(c, new float[] {9, 5, 16, 12, 9, 19}, false);
          break;
        default:
          c.drawCircle(12, 12, 8, p);
          line(c, 12, 10, 12, 16);
          line(c, 12, 7, 12, 7);
          break;
      }
      c.restore();
    }

    void line(Canvas c, float a, float b, float x, float y) {
      c.drawLine(a, b, x, y, p);
    }

    void path(Canvas c, float[] v, boolean close) {
      Path path = new Path();
      path.moveTo(v[0], v[1]);
      for (int i = 2; i < v.length; i += 2) path.lineTo(v[i], v[i + 1]);
      if (close) path.close();
      c.drawPath(path, p);
    }

    @Override
    public void setAlpha(int value) {
      p.setAlpha(value);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      p.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
}
