package com.grandbeauty;

import android.content.Context;
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

    public interface QuickDone { void onQuickDone(Appt a); }

    public static class ApptsAdapter extends BaseAdapter {
        private final Context ctx;
        private final LayoutInflater inf;
        private final List<Object> items = new ArrayList<>();
        private final boolean flat;
        private final QuickDone quickDone;

        public ApptsAdapter(Context ctx, List<Appt> src, boolean flat, QuickDone qd) {
            this.ctx = ctx;
            this.inf = LayoutInflater.from(ctx);
            this.flat = flat;
            this.quickDone = qd;
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

            if (!attention.isEmpty()) {
                items.add(ctx.getString(R.string.section_attention)
                        .toUpperCase(Locale.getDefault()));
                Collections.sort(attention, new Comparator<Appt>() {
                    public int compare(Appt a, Appt b) { return Long.compare(b.start, a.start); }
                });
                items.addAll(attention);
            }

            String lastLabel = null;
            for (Appt a : future) {
                String label = Fmt.dayLabel(a.start).toUpperCase(Locale.getDefault());
                if (!label.equals(lastLabel)) {
                    items.add(label);
                    lastLabel = label;
                }
                items.add(a);
            }

            if (!history.isEmpty()) {
                items.add(ctx.getString(R.string.section_history)
                        .toUpperCase(Locale.getDefault()));
                Collections.sort(history, new Comparator<Appt>() {
                    public int compare(Appt a, Appt b) { return Long.compare(b.start, a.start); }
                });
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
            if (o instanceof String) {
                HeaderH h = cv == null ? null : (HeaderH) cv.getTag();
                if (h == null) {
                    cv = inf.inflate(R.layout.row_header, parent, false);
                    h = new HeaderH(cv);
                    cv.setTag(h);
                }
                h.label.setText((String) o);
                return cv;
            }

            final Appt a = (Appt) o;
            H h = cv == null || !(cv.getTag() instanceof H) ? null : (H) cv.getTag();
            if (h == null) {
                cv = inf.inflate(R.layout.row_appt, parent, false);
                h = new H(cv);
                cv.setTag(h);
            }
            boolean past = a.start < System.currentTimeMillis();

            h.time.setText(a.historyView
                    ? Fmt.dayLabel(a.start) : Fmt.timeShort(a.start));
            h.ampm.setText(a.historyView
                    ? Fmt.monthDay(a.start) : Fmt.ampm(a.start).toUpperCase(Locale.getDefault()));
            h.name.setText(a.clientName);

            String meta = a.historyView
                    ? Fmt.monthDay(a.start) + "  ·  " + a.service : a.service;
            String money = Fmt.money(a.amount);
            if (!money.isEmpty()) meta += "  ·  " + money;
            String bal = Fmt.money(a.balance());
            if (!bal.isEmpty() && !"0".equals(bal)) meta += "  ·  " + bal + " owed";
            h.meta.setText(meta);

            boolean showCheck = Appt.UP.equals(a.status);
            h.doneBtn.setVisibility(showCheck ? View.VISIBLE : View.GONE);
            h.doneBtn.setOnClickListener(showCheck ? new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (quickDone != null) quickDone.onQuickDone(a);
                }
            } : null);

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
            return cv;
        }

        private int res(int id) { return ctx.getResources().getColor(id); }

        static class HeaderH {
            final TextView label;
            HeaderH(View v) { label = v.findViewById(R.id.text1); }
        }

        static class H {
            final TextView time, ampm, name, meta, pill;
            final View doneBtn;
            H(View v) {
                time = v.findViewById(R.id.time);
                ampm = v.findViewById(R.id.ampm);
                name = v.findViewById(R.id.client_name);
                meta = v.findViewById(R.id.meta);
                pill = v.findViewById(R.id.status_pill);
                doneBtn = v.findViewById(R.id.done_btn);
            }
        }
    }

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
