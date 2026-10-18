package com.semih.nova;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.Button;

final class HoldButton extends Button {
  private final Runnable start, finish, toggle;
  private boolean touchClick;

  public HoldButton(Context c) {
    this(c, () -> {}, () -> {}, () -> {});
  }

  HoldButton(Context c, Runnable start, Runnable finish, Runnable toggle) {
    super(c);
    this.start = start;
    this.finish = finish;
    this.toggle = toggle;
    setClickable(true);
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (e.getActionMasked() == MotionEvent.ACTION_DOWN) {
      setPressed(true);
      start.run();
      return true;
    }
    if (e.getActionMasked() == MotionEvent.ACTION_UP) {
      setPressed(false);
      finish.run();
      touchClick = true;
      performClick();
      touchClick = false;
      return true;
    }
    if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) {
      setPressed(false);
      finish.run();
      return true;
    }
    return true;
  }

  @Override
  public boolean performClick() {
    super.performClick();
    if (!touchClick) toggle.run();
    return true;
  }
}
