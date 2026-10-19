package com.semih.havapusulasi;

import android.content.Context;
import android.graphics.*;
import android.view.View;

final class WeatherArt extends View {
  private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final boolean wet;

  public WeatherArt(Context c) {
    this(c, null);
  }

  WeatherArt(Context c, Integer code) {
    super(c);
    wet = code != null && code >= 51;
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
  }

  @Override
  protected void onDraw(Canvas c) {
    super.onDraw(c);
    c.save();
    float scale = Math.min(getWidth() / 180f, getHeight() / 150f);
    c.translate((getWidth() - 180 * scale) / 2, (getHeight() - 150 * scale) / 2);
    c.scale(scale, scale);
    p.setColor(0x22F5CA7D);
    c.drawCircle(112, 49, 44, p);
    p.setColor(0xFFF5CA7D);
    c.drawCircle(112, 49, 28, p);
    p.setColor(0xFFE2EEFF);
    c.drawCircle(63, 79, 26, p);
    c.drawCircle(91, 65, 32, p);
    c.drawCircle(122, 85, 21, p);
    c.drawRoundRect(45, 75, 140, 108, 18, 18, p);
    if (wet) {
      p.setColor(0xFF91BDFF);
      p.setStrokeWidth(5);
      p.setStrokeCap(Paint.Cap.ROUND);
      for (int i = 0; i < 3; i++) c.drawLine(65 + i * 25, 119, 59 + i * 25, 132, p);
    }
    c.restore();
  }
}
