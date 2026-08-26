package com.grandbeauty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class DailyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        Calendar c = Calendar.getInstance();
        Fmt.clearTime(c);
        long dayStart = c.getTimeInMillis();
        long dayEnd = dayStart + 86_400_000L;
        long now = System.currentTimeMillis();

        Db db = Db.get(ctx);
        List<Appt> all = db.appts(null, null);

        int today = 0, attention = 0;
        double expected = 0;
        String first = null;
        StringBuilder names = new StringBuilder();

        for (Appt a : all) {
            boolean past = a.start < dayStart;
            if (Appt.UP.equals(a.status) && past) attention++;
            if (a.start >= dayStart && a.start < dayEnd && Appt.UP.equals(a.status)) {
                today++;
                if (first == null)
                    first = Fmt.timeShort(a.start) + " — " + a.clientName;
                if (a.amount != null) expected += a.amount;
                if (names.length() > 0) names.append(", ");
                names.append(Fmt.timeShort(a.start)).append(" ").append(a.clientName);
            }
        }

        StringBuilder body = new StringBuilder();
        if (today == 0) {
            body.append("No bookings today — a quiet day.");
        } else {
            body.append(ctx.getString(R.string.notif_daily_expect,
                    Fmt.money(expected), today));
            if (first != null) body.append('\n').append(first);
        }
        if (attention > 0) {
            body.append('\n').append(ctx.getResources()
                    .getQuantityString(R.plurals.attention_count, attention, attention));
        }
        double owed = db.totalOwed();
        if (owed >= 1) {
            body.append('\n').append(ctx.getString(R.string.notif_daily_owed,
                    Fmt.money(owed), db.owedByClient().size()));
        }

        Notif.notify(ctx, Notif.CH_DAILY, 999_998,
                ctx.getString(R.string.notif_daily_title), body.toString(),
                MainActivity.class, -1);

        if (Fmt.regularsOn(ctx) && !Fmt.nudgedToday(ctx) && Notif.canNotify(ctx)) {
            Client reg = db.regularToNudge();
            if (reg != null) {
                long last = db.stats(reg.id).last;
                int weeks = (int) ((now - last) / (7L * 86_400_000L));
                if (weeks >= 5) {
                    Notif.notify(ctx, Notif.CH_CHECKINS, 999_997,
                            ctx.getString(R.string.notif_checkin_title),
                            ctx.getString(R.string.notif_checkin_body, reg.name, weeks),
                            ClientDetailActivity.class, reg.id);
                }
                Fmt.markNudged(ctx);
            }
        }

        Alarms.scheduleDaily(ctx);
    }
}
