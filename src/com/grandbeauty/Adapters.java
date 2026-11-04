package com.grandbeauty;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Adapters {

    public interface RowMenu { void onRowMenu(Appt a, View anchor); }

    public static class Section {
        public final String label, meta;
        public final boolean warn;
        public final long dateMs;
        public Section(String label, String meta, boolean warn, long dateMs) {
            this.label = label; this.meta = meta; this.warn = warn; this.dateMs = dateMs;
        }
    }

    public static class ApptsAdapter extends BaseAdapter {
        private final Context ctx;
        private final LayoutInflater inf;
        private final List<Object> items = new ArrayList<>();
        private final boolean flat;
        private final RowMenu rowMenu;

        public ApptsAdapter(Context ctx, List<Appt> src, boolean flat, RowMenu rm) {
            this.ctx = ctx;
            this.inf = LayoutInflater.from(ctx);
            this.flat = flat;
            this.rowMenu = rm;
            rebuild(src);
        }

        public void rebuild(List<Appt> src) {
            items.clear();
            if (src != null) {
                if (flat) {
                    items.addAll(src);
                } else {
                    sectioned(src);
                }
            }
            notifyDataSetChanged();
        }

        private void sectioned(List<Appt> src) {
            Calendar c = Calendar.getInstance();
            Fmt.clearTime(c);
            long todayStart = c.getTimeInMillis();
            long histCut = todayStart - 60L * 86_400_000L;

            List<Appt> attention = new ArrayList<>();
            List<Appt> future = new ArrayList<>();
            List<Appt> history = new ArrayList<>();

            for (Appt a : src) {
                a.nextUp = false;
                if (a.start >= todayStart) {
                    a.historyView = false;
                    future.add(a);
                } else if (Appt.UP.equals(a.status)) {
                    a.historyView = true;
                    attention.add(a);
                } else if (a.start >= histCut) {
                    a.historyView = true;
                    history.add(a);
                }
            }

            Collections.sort(attention, new Comparator<Appt>() {
                public int compare(Appt a, Appt b) { return Long.compare(b.start, a.start); }
            });
            Collections.sort(history, new Comparator<Appt>() {
                public int compare(Appt a, Appt b) { return Long.compare(b.start, a.start); }
            });

            for (Appt a : future) {
                if (Appt.UP.equals(a.status)) { a.nextUp = true; break; }
            }

            if (!attention.isEmpty()) {
                items.add(new Section(ctx.getString(R.string.section_attention)
                        .toUpperCase(Locale.getDefault()),
                        ctx.getResources().getQuantityString(
                                R.plurals.attention_count, attention.size(), attention.size()),
                        true, 0));
                items.addAll(attention);
            }

            java.util.LinkedHashMap<String, int[]> dayAgg = new java.util.LinkedHashMap<>();
            for (Appt a : future) {
                if (Appt.CANC.equals(a.status)) continue;
                String day = Fmt.dayLabel(a.start).toUpperCase(Locale.getDefault());
                int[] agg = dayAgg.get(day);
                if (agg == null) { agg = new int[2]; dayAgg.put(day, agg); }
                agg[0]++;
                if (a.amount != null && a.start >= todayStart)
                    agg[1] += (int) Math.round(a.amount);
            }

            String lastDay = null;
            for (Appt a : future) {
                String day = Fmt.dayLabel(a.start).toUpperCase(Locale.getDefault());
                if (!day.equals(lastDay)) {
                    int[] agg = dayAgg.get(day);
                    StringBuilder m = new StringBuilder(
                            ctx.getResources().getQuantityString(R.plurals.bookings_count,
                                    agg[0], agg[0]));
                    if (agg[1] > 0) m.append(" · ").append(Fmt.money((double) agg[1]));
                    items.add(new Section(day, m.toString(), false, a.start));
                    lastDay = day;
                }
                items.add(a);
            }

            if (!history.isEmpty()) {
                items.add(new Section(ctx.getString(R.string.section_history)
                        .toUpperCase(Locale.getDefault()),
                        ctx.getResources().getQuantityString(
                                R.plurals.visits_count, history.size(), history.size()),
                        false, 0));
                items.addAll(history);
            }
        }

        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int pos) { return items.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int pos) { return items.get(pos) instanceof Appt ? 1 : 0; }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            Object o = items.get(pos);
            if (o instanceof Section) {
                Section s = (Section) o;
                HeaderH h = cv == null || !(cv.getTag() instanceof HeaderH)
                        ? null : (HeaderH) cv.getTag();
                if (h == null) {
                    cv = inf.inflate(R.layout.row_header, parent, false);
                    h = new HeaderH(cv);
                    cv.setTag(h);
                }
                h.label.setText(s.label);
                int color = res(s.warn ? R.color.danger : R.color.primary);
                h.label.setTextColor(color);
                if (s.meta != null) {
                    h.meta.setVisibility(View.VISIBLE);
                    h.meta.setText(s.meta);
                    h.meta.setTextColor(res(s.warn ? R.color.danger : R.color.text_sub));
                } else {
                    h.meta.setVisibility(View.GONE);
                }
                if (s.dateMs > 0) {
                    final long date = s.dateMs;
                    cv.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            ctx.startActivity(new android.content.Intent(ctx,
                                    EditApptActivity.class)
                                    .putExtra("date", date)
                                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                        }
                    });
                    Ui.press(cv);
                } else {
                    cv.setOnClickListener(null);
                    cv.setOnTouchListener(null);
                }
                return cv;
            }

            final Appt a = (Appt) o;
            H h = cv == null || !(cv.getTag() instanceof H) ? null : (H) cv.getTag();
            if (h == null) {
                cv = inf.inflate(R.layout.row_appt, parent, false);
                h = new H(cv);
                cv.setTag(h);
            }

            h.card.setAlpha(a.historyView ? 0.45f : 1f);
            h.dots.setAlpha(a.historyView ? 0.45f : 1f);
            h.railLine.setAlpha(a.historyView ? 0.45f : 1f);
            boolean trailEnd = pos == items.size() - 1 || !(items.get(pos + 1) instanceof Appt);
            h.railLine.setVisibility(trailEnd ? View.INVISIBLE : View.VISIBLE);
            h.nextPill.setVisibility(!a.historyView && a.nextUp ? View.VISIBLE : View.GONE);
            h.time.setText(a.historyView
                    ? Fmt.monthDay(a.start) + "  ·  " + Fmt.timeShort(a.start)
                    : Fmt.timeShort(a.start) + " " + Fmt.ampm(a.start));
            h.name.setText(a.clientName);

            String meta = a.service;
            String money = Fmt.money(a.amount);
            if (!money.isEmpty()) meta += "  ·  " + money;
            String bal = Fmt.money(a.balance());
            if (!bal.isEmpty() && !"0".equals(bal)) meta += "  ·  " + bal + " owed";
            h.meta.setText(meta);

            boolean past = a.start < System.currentTimeMillis();
            if (Appt.DONE.equals(a.status)) {
                h.pill.setVisibility(View.VISIBLE);
                h.pill.setBackgroundResource(R.drawable.bg_chip_done);
                h.pill.setTextColor(res(R.color.on_done_container));
                h.pill.setText(ctx.getString(R.string.status_done));
            } else if (Appt.CANC.equals(a.status)) {
                h.pill.setVisibility(View.VISIBLE);
                h.pill.setBackgroundResource(R.drawable.bg_chip_canc);
                h.pill.setTextColor(res(R.color.danger));
                h.pill.setText(ctx.getString(R.string.status_cancelled));
            } else if (past) {
                h.pill.setVisibility(View.VISIBLE);
                h.pill.setBackgroundResource(R.drawable.bg_chip_warn);
                h.pill.setTextColor(res(R.color.on_warn_container));
                h.pill.setText("Past");
            } else {
                h.pill.setVisibility(View.GONE);
            }

            final View cardV = h.card;
            h.card.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (rowMenu != null) rowMenu.onRowMenu(a, cardV);
                }
            });
            if (rowMenu != null) {
                h.dots.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) { rowMenu.onRowMenu(a, v); }
                });
            } else {
                h.dots.setOnClickListener(null);
            }
            Ui.press(h.card);
            Ui.press(h.dots);
            return cv;
        }

        private int res(int id) { return ctx.getResources().getColor(id); }

        private int dp(float v) {
            return Math.round(v * ctx.getResources().getDisplayMetrics().density);
        }

        static class HeaderH {
            final TextView label, meta;
            HeaderH(View v) {
                label = v.findViewById(R.id.text1);
                meta = v.findViewById(R.id.text2);
            }
        }

        static class H {
            final TextView time, name, meta, pill, nextPill;
            final View card, dots, railLine;
            H(View v) {
                time = v.findViewById(R.id.time);
                name = v.findViewById(R.id.client_name);
                meta = v.findViewById(R.id.meta);
                pill = v.findViewById(R.id.status_pill);
                nextPill = v.findViewById(R.id.next_pill);
                card = v.findViewById(R.id.card);
                dots = v.findViewById(R.id.more);
                railLine = v.findViewById(R.id.rail_line);
            }
        }
    }

    public static void apptMenu(final Context ctx, View anchor, final Appt a,
                                final RowMenuHost host) {
        Activity act = (Activity) ctx;
        List<Menus.Item> items = new ArrayList<>();
        if (a.clientPhone != null && !a.clientPhone.trim().isEmpty())
            items.add(new Menus.Item(R.drawable.ic_phone_s,
                    ctx.getString(R.string.call) + " " + a.clientName,
                    R.color.text, 1));
        items.add(new Menus.Item(R.drawable.ic_edit_s,
                ctx.getString(R.string.edit), R.color.text, 2));
        if (Appt.UP.equals(a.status)) {
            items.add(new Menus.Item(R.drawable.ic_check_d,
                    ctx.getString(R.string.mark_done), R.color.text, 3));
            items.add(new Menus.Item(R.drawable.ic_close_d,
                    ctx.getString(R.string.mark_cancelled), R.color.danger, 4));
        } else {
            items.add(new Menus.Item(R.drawable.ic_clock_s,
                    ctx.getString(R.string.reopen), R.color.text, 5));
        }
        if (a.balance() >= 1)
            items.add(new Menus.Item(R.drawable.ic_check_d,
                    ctx.getString(R.string.settle, Fmt.money(a.balance())),
                    R.color.secondary, 6));
        items.add(new Menus.Item(R.drawable.ic_trash_d,
                ctx.getString(R.string.delete), R.color.danger, 7));

        Menus.actionSheet(act, items, action -> host.onMenuItem(a, action));
    }

    public interface RowMenuHost { void onMenuItem(Appt a, int action); }

    public static class ClientsAdapter extends BaseAdapter {
        private final Context ctx;
        private final LayoutInflater inf;
        private final List<Client> items = new ArrayList<>();
        private final Map<Long, Long> nextByClient;
        private final Map<Long, Double> owedByClient;

        public ClientsAdapter(Context ctx, Map<Long, Long> next,
                              Map<Long, Double> owed) {
            this.ctx = ctx;
            this.inf = LayoutInflater.from(ctx);
            this.nextByClient = next;
            this.owedByClient = owed;
        }

        public void rebuild(List<Client> src, Map<Long, Long> nextMap,
                            Map<Long, Double> owedMap) {
            items.clear();
            items.addAll(src);
            nextByClient.clear();
            nextByClient.putAll(nextMap);
            owedByClient.clear();
            owedByClient.putAll(owedMap);
            notifyDataSetChanged();
        }

        @Override public int getCount() { return items.size(); }
        @Override public Client getItem(int pos) { return items.get(pos); }
        @Override public long getItemId(int pos) { return items.get(pos).id; }

        @Override
        public View getView(int pos, View cv, ViewGroup parent) {
            H h = cv == null ? null : (H) cv.getTag();
            if (h == null) {
                cv = inf.inflate(R.layout.row_client, parent, false);
                h = new H(cv);
                cv.setTag(h);
            }
            Client x = getItem(pos);
            h.initials.setText(Fmt.initials(x.name));
            h.name.setText(x.name);

            Double owed = owedByClient.get(x.id);
            if (owed != null && owed >= 1) {
                h.sub.setText(ctx.getString(R.string.owes, Fmt.money(owed)));
                h.sub.setTextColor(ctx.getResources().getColor(R.color.secondary));
                return cv;
            }
            h.sub.setTextColor(ctx.getResources().getColor(R.color.text_sub));
            Long next = nextByClient.get(x.id);
            if (next != null) {
                h.sub.setText("Next · " + Fmt.dayLabel(next) + " " + Fmt.timeShort(next));
            } else if (!x.phone.trim().isEmpty()) {
                h.sub.setText(x.phone);
            } else {
                h.sub.setText("");
            }
            return cv;
        }

        static class H {
            final TextView initials, name, sub;
            H(View v) {
                initials = v.findViewById(R.id.initials);
                name = v.findViewById(R.id.c_name);
                sub = v.findViewById(R.id.c_sub);
            }
        }
    }
}
