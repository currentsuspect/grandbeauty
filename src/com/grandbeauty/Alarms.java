package com.grandbeauty;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

public class Alarms {

    private static PendingIntent pi(Context ctx, long apptId) {
        Intent i = new Intent(ctx, AlarmReceiver.class);
        i.putExtra("id", apptId);
        return PendingIntent.getBroadcast(ctx, (int) apptId, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean canExact(Context ctx) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        return am != null && am.canScheduleExactAlarms();
    }

    public static void scheduleFor(Context ctx, Appt a) {
        cancelAppt(ctx, a.id);
        if (!a.remind || !Appt.UP.equals(a.status)) return;
        long fireAt = a.start - Fmt.leadMinutes(ctx) * 60000L;
        if (fireAt <= System.currentTimeMillis() + 30_000L) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent p = pi(ctx, a.id);
        if (canExact(ctx)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, p);
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, fireAt, 15 * 60000L, p);
        }
    }

    public static void cancelAppt(Context ctx, long apptId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi(ctx, apptId));
    }

    public static void scheduleAll(Context ctx) {
        List<Appt> all = Db.get(ctx).appts(null, null);
        int n = 0;
        for (Appt a : all) {
            if (n > 200) break;
            if (a.start > System.currentTimeMillis()) {
                scheduleFor(ctx, a);
                n++;
            }
        }
    }

    private static PendingIntent dailyPi(Context ctx) {
        return PendingIntent.getBroadcast(ctx, 999_999,
                new Intent(ctx, DailyReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void scheduleDaily(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        am.cancel(dailyPi(ctx));
        if (!Fmt.dailyOn(ctx)) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        PendingIntent p = dailyPi(ctx);
        if (canExact(ctx)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), p);
        } else {
            am.setWindow(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 20 * 60000L, p);
        }
    }
}
