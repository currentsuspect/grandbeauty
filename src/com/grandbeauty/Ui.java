package com.grandbeauty;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Switch;

public final class Ui {
    private Ui() {}

    /** recoil + darken + haptic on press; does not consume touches */
    public static void press(View v) {
        final GradientDrawable dim = new GradientDrawable();
        dim.setShape(GradientDrawable.RECTANGLE);
        dim.setCornerRadius(16 * v.getResources().getDisplayMetrics().density);
        dim.setColor(0x14000000);
        v.setOnTouchListener((v2, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v2.animate().scaleX(0.94f).scaleY(0.94f).setDuration(70).start();
                    if (v2 instanceof ImageView)
                        ((ImageView) v2).setColorFilter(
                                v2.getResources().getColor(R.color.primary));
                    else
                        v2.setForeground(dim);
                    v2.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v2.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(new OvershootInterpolator(2f)).start();
                    if (v2 instanceof ImageView) ((ImageView) v2).clearColorFilter();
                    else v2.setForeground(null);
                    break;
            }
            return false;
        });
    }

    public static void tintSwitch(Switch s) {
        int primary = s.getResources().getColor(R.color.primary);
        int sub = s.getResources().getColor(R.color.text_sub);
        int trackOn = android.graphics.Color.argb(
                100, android.graphics.Color.red(primary),
                android.graphics.Color.green(primary),
                android.graphics.Color.blue(primary));
        s.setThumbTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}},
                new int[]{primary, sub}));
        s.setTrackTintList(new ColorStateList(
                new int[][]{new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}},
                new int[]{trackOn, 0x33808080}));
    }
}
