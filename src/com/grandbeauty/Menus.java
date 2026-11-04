package com.grandbeauty;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public final class Menus {

    public static class Item {
        final int icon;
        final String label;
        final int colorRes;
        final int action;

        public Item(int icon, String label, int colorRes, int action) {
            this.icon = icon;
            this.label = label;
            this.colorRes = colorRes;
            this.action = action;
        }
    }

    public interface OnPick { void onPick(int action); }

    public static void actionSheet(final Activity act, List<Item> items, final OnPick cb) {
        final Dialog[] holder = new Dialog[1];

        LinearLayout box = new LinearLayout(act);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(act, 10), dp(act, 8), dp(act, 10), dp(act, 6));

        for (final Item it : items) {
            LinearLayout row = new LinearLayout(act);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.bg_row_ripple);
            row.setPadding(dp(act, 18), 0, dp(act, 18), 0);
            row.setMinimumHeight(dp(act, 54));

            ImageView ic = new ImageView(act);
            ic.setImageResource(it.icon);
            ic.setImageTintList(android.content.res.ColorStateList.valueOf(
                    act.getColor(it.colorRes)));
            row.addView(ic, new LinearLayout.LayoutParams(dp(act, 22), dp(act, 22)));

            TextView tv = new TextView(act);
            tv.setText(it.label);
            tv.setTextSize(15.5f);
            tv.setTypeface(null, android.graphics.Typeface.BOLD);
            tv.setTextColor(act.getColor(it.colorRes));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            tp.leftMargin = dp(act, 16);
            tv.setLayoutParams(tp);
            row.addView(tv);

            row.setOnClickListener(v -> {
                if (holder[0] != null) holder[0].dismiss();
                cb.onPick(it.action);
            });

            box.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 54)));
        }

        holder[0] = show(act, box);
    }

    /** bottom sheet hosting arbitrary content; returns the shown dialog */
    public static Dialog show(Activity act, View content) {
        FrameLayout f = new FrameLayout(act);
        f.setBackgroundResource(R.drawable.bg_sheet);
        int p = dp(act, 12);
        f.setPadding(p, p, p, dp(act, 16));

        View handle = new View(act);
        handle.setBackgroundResource(R.drawable.bg_handle);
        FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(
                dp(act, 36), dp(act, 4), Gravity.CENTER_HORIZONTAL);
        hp.topMargin = dp(act, 2);
        f.addView(handle, hp);

        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cp.topMargin = dp(act, 10);
        f.addView(content, cp);

        Dialog d = new Dialog(act);
        d.setContentView(f);
        if (d.getWindow() != null) {
            d.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            d.getWindow().setGravity(Gravity.BOTTOM);
            d.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            d.getWindow().setWindowAnimations(R.style.SheetAnim);
        }
        d.show();
        return d;
    }

    private static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }
}
