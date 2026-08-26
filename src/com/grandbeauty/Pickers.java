package com.grandbeauty;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

public final class Pickers {
    public interface DateCb { void onPick(Calendar cal); }
    public interface TimeCb { void onPick(Calendar cal); }

    private static int dp(Activity a, float v) {
        return Math.round(v * a.getResources().getDisplayMetrics().density);
    }

    private static TextView text(Activity a, String s, float size, int color, boolean bold) {
        TextView t = new TextView(a);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    // ---------- date ----------

    public static void datePicker(final Activity act, Calendar initial, final DateCb cb) {
        final CalendarPickerView view = new CalendarPickerView(act, initial);
        new AlertDialog.Builder(act)
                .setView(wrap(act, view))
                .setPositiveButton(R.string.save, (dialog, w) -> cb.onPick(view.selected()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static View wrap(Activity act, View inner) {
        FrameLayout f = new FrameLayout(act);
        int pad = dp(act, 20);
        f.setPadding(pad, dp(act, 12), pad, dp(act, 4));
        f.addView(inner, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return f;
    }

    static class CalendarPickerView extends LinearLayout {
        private final Activity act;
        private final Calendar month;
        private final Calendar sel;
        private TextView title;
        private GridView grid;

        CalendarPickerView(Activity a, Calendar initial) {
            super(a);
            act = a;
            setOrientation(VERTICAL);
            sel = (Calendar) initial.clone();
            month = (Calendar) initial.clone();
            month.set(Calendar.DAY_OF_MONTH, 1);

            LinearLayout header = new LinearLayout(a);
            header.setOrientation(HORIZONTAL);
            header.setGravity(Gravity.CENTER_VERTICAL);

            TextView prev = text(a, "‹", 22, a.getColor(R.color.gold), true);
            TextView next = text(a, "›", 22, a.getColor(R.color.gold), true);
            title = text(a, "", 16, a.getColor(R.color.text), true);
            title.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LayoutParams.WRAP_CONTENT, 1f));
            prev.setPadding(dp(a, 16), dp(a, 8), dp(a, 16), dp(a, 8));
            next.setPadding(dp(a, 16), dp(a, 8), dp(a, 16), dp(a, 8));
            prev.setOnClickListener(v -> { month.add(Calendar.MONTH, -1); fill(); });
            next.setOnClickListener(v -> { month.add(Calendar.MONTH, 1); fill(); });
            header.addView(prev);
            header.addView(title);
            header.addView(next);
            addView(header);

            LinearLayout week = new LinearLayout(a);
            String[] days = {"S", "M", "T", "W", "T", "F", "S"};
            int sub = a.getColor(R.color.text_sub);
            for (String d : days) {
                week.addView(text(a, d, 12, sub, true), new LinearLayout.LayoutParams(
                        0, LayoutParams.WRAP_CONTENT, 1f));
            }
            addView(week, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

            grid = new GridView(a);
            grid.setNumColumns(7);
            grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
            grid.setVerticalSpacing(dp(a, 2));
            addView(grid, new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(a, 6 * 44)));

            fill();
        }

        Calendar selected() { return sel; }

        private void fill() {
            title.setText(new java.text.SimpleDateFormat("MMMM yyyy",
                    Locale.getDefault()).format(month.getTime()));

            java.util.ArrayList<Integer> cells = new java.util.ArrayList<>();
            int lead = month.get(Calendar.DAY_OF_WEEK) - 1;
            for (int i = 0; i < lead; i++) cells.add(0);
            int max = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= max; i++) cells.add(i);
            while (cells.size() % 7 != 0) cells.add(0);

            final Calendar m = month;
            grid.setAdapter(new BaseAdapter() {
                @Override public int getCount() { return cells.size(); }
                @Override public Object getItem(int p) { return cells.get(p); }
                @Override public long getItemId(int p) { return p; }
                @Override
                public View getView(int p, View cv, ViewGroup parent) {
                    final int day = cells.get(p);
                    TextView t;
                    if (cv == null) {
                        t = text(act, "", 15, act.getColor(R.color.text), false);
                        t.setLayoutParams(new AbsListView.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 44)));
                    } else {
                        t = (TextView) cv;
                    }
                    t.setText(day == 0 ? "" : String.valueOf(day));
                    boolean isSel = day != 0 && sameDay(sel, day, m);
                    boolean isToday = day != 0 && sameDay(now(), day, m);
                    t.setBackground(isSel ? act.getDrawable(R.drawable.bg_dot_day)
                            : isToday ? act.getDrawable(R.drawable.bg_today) : null);
                    t.setTextColor(isSel ? act.getColor(R.color.on_primary)
                            : act.getColor(R.color.text));
                    t.setOnClickListener(day == 0 ? null : v -> {
                        sel.set(Calendar.YEAR, m.get(Calendar.YEAR));
                        sel.set(Calendar.MONTH, m.get(Calendar.MONTH));
                        sel.set(Calendar.DAY_OF_MONTH, day);
                        fill();
                    });
                    return t;
                }
            });
        }

        private static Calendar now() { return Calendar.getInstance(); }

        private static boolean sameDay(Calendar c, int day, Calendar m) {
            return c.get(Calendar.DAY_OF_MONTH) == day
                    && c.get(Calendar.MONTH) == m.get(Calendar.MONTH)
                    && c.get(Calendar.YEAR) == m.get(Calendar.YEAR);
        }
    }

    // ---------- time ----------

    private static class Wheel extends ListView {
        final Activity act;
        final int[] values;
        final int rowH;
        int selectedIdx;

        Wheel(Activity a, int[] vals, int initialIdx) {
            super(a);
            act = a;
            values = vals;
            selectedIdx = initialIdx;
            rowH = dp(a, 44);
            setVerticalScrollBarEnabled(false);
            setSelector(new android.graphics.drawable.ColorDrawable(0));
            setCacheColorHint(0);
            setAdapter(new BaseAdapter() {
                @Override public int getCount() { return values.length; }
                @Override public Object getItem(int p) { return values[p]; }
                @Override public long getItemId(int p) { return p; }
                @Override
                public View getView(int p, View cv, ViewGroup parent) {
                    TextView t = cv == null
                            ? text(act, "", 22, act.getColor(R.color.text), true)
                            : (TextView) cv;
                    String label = p == selectedIdx ? label(values[p]) : label(values[p]);
                    t.setText(label);
                    t.setAlpha(p == selectedIdx ? 1f : 0.35f);
                    t.setLayoutParams(new AbsListView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, rowH));
                    return t;
                }
            });
            setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override public void onScrollStateChanged(AbsListView v, int state) {
                    if (state == SCROLL_STATE_IDLE) snap();
                }
                @Override public void onScroll(AbsListView v, int a, int b, int c) {}
            });
            final int init = initialIdx;
            post(() -> setSelectionFromTop(init, getHeight() / 2 - rowH / 2));
        }

        private String label(int v) {
            if (v == -1) return "AM";
            if (v == -2) return "PM";
            return v < 10 ? "0" + v : String.valueOf(v);
        }

        private void snap() {
            if (getFirstVisiblePosition() < 0 || getChildAt(0) == null) return;
            int center = getHeight() / 2;
            int idx = centerIndex();
            setSelectionFromTop(idx, center - rowH / 2);
            if (idx != selectedIdx) {
                selectedIdx = idx;
                invalidateViews();
            }
        }

        int centerIndex() {
            if (getFirstVisiblePosition() < 0 || getChildAt(0) == null)
                return Math.max(0, Math.min(selectedIdx, values.length - 1));
            int center = getHeight() / 2;
            int first = getFirstVisiblePosition();
            int idx = first + (center - getChildAt(0).getTop()) / rowH;
            return Math.max(0, Math.min(idx, values.length - 1));
        }
    }

    public static void timePicker(final Activity act, Calendar initial, final TimeCb cb) {
        int h24 = initial.get(Calendar.HOUR_OF_DAY);
        final boolean[] pm = {h24 >= 12};
        int h12 = h24 % 12;
        if (h12 == 0) h12 = 12;

        int[] hours = new int[12];
        for (int i = 0; i < 12; i++) hours[i] = i + 1;
        int[] minutes = new int[12];
        for (int i = 0; i < 12; i++) minutes[i] = i * 5;

        final Wheel hourW = new Wheel(act, hours, h12 - 1);
        final Wheel minW = new Wheel(act, minutes, initial.get(Calendar.MINUTE) / 5);

        FrameLayout wheelBox = new FrameLayout(act);
        int wheelH = dp(act, 5 * 44);

        View band = new View(act);
        band.setBackgroundResource(R.drawable.bg_band);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(act, 44), Gravity.CENTER_VERTICAL);
        wheelBox.addView(band, bp);

        LinearLayout wheels = new LinearLayout(act);
        wheels.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(
                0, wheelH, 1f);
        wheels.addView(hourW, wp);
        wheels.addView(minW, wp);
        wheelBox.addView(wheels, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, wheelH, Gravity.CENTER_VERTICAL));

        LinearLayout ampm = new LinearLayout(act);
        ampm.setOrientation(LinearLayout.VERTICAL);
        ampm.setGravity(Gravity.CENTER);
        final TextView am = text(act, "AM", 15, act.getColor(R.color.text_sub), true);
        final TextView tx = text(act, "PM", 15, act.getColor(R.color.text_sub), true);
        am.setPadding(dp(act, 18), dp(act, 10), dp(act, 18), dp(act, 10));
        tx.setPadding(dp(act, 18), dp(act, 10), dp(act, 18), dp(act, 10));
        View.OnClickListener ampmClick = v -> {
            pm[0] = (v == tx);
            paintAmpm(am, tx, pm[0], act);
        };
        am.setOnClickListener(ampmClick);
        tx.setOnClickListener(ampmClick);
        paintAmpm(am, tx, pm[0], act);
        ampm.addView(am);
        ampm.addView(tx);

        LinearLayout row = new LinearLayout(act);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(wheelBox, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(ampm);

        new AlertDialog.Builder(act)
                .setView(wrap(act, row))
                .setPositiveButton(R.string.save, (dialog, w) -> {
                    Calendar out = (Calendar) initial.clone();
                    int h = hourW.values[hourW.centerIndex()] % 12;
                    if (pm[0]) h += 12;
                    out.set(Calendar.HOUR_OF_DAY, h);
                    out.set(Calendar.MINUTE, minW.values[minW.centerIndex()]);
                    out.set(Calendar.SECOND, 0);
                    cb.onPick(out);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void paintAmpm(TextView am, TextView pmT, boolean isPm, Activity a) {
        am.setBackgroundResource(isPm ? 0 : R.drawable.bg_chip_on);
        am.setTextColor(a.getColor(isPm ? R.color.text_sub : R.color.on_primary_container));
        pmT.setBackgroundResource(isPm ? R.drawable.bg_chip_on : 0);
        pmT.setTextColor(a.getColor(isPm ? R.color.on_primary_container : R.color.text_sub));
    }
}
