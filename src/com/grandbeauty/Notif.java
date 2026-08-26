package com.grandbeauty;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class Notif {
    public static final String CH_APPTS = "appts";
    public static final String CH_DAILY = "daily";
    public static final String CH_CHECKINS = "checkins";

    public static void channels(Context ctx) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        NotificationChannel a = new NotificationChannel(CH_APPTS,
                ctx.getString(R.string.notif_channel_appts), NotificationManager.IMPORTANCE_HIGH);
        a.enableVibration(true);
        nm.createNotificationChannel(a);
        NotificationChannel d = new NotificationChannel(CH_DAILY,
                ctx.getString(R.string.notif_channel_daily), NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(d);
        NotificationChannel k = new NotificationChannel(CH_CHECKINS,
                ctx.getString(R.string.notif_channel_checkins), NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(k);
    }

    public static void notify(Context ctx, String channel, int id, String title, String body, Class<?> open, long extra) {
        channels(ctx);
        Intent i = new Intent(ctx, open);
        i.putExtra("id", extra);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(ctx, id, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new android.app.Notification.Builder(ctx, channel);
        } else {
            b = new android.app.Notification.Builder(ctx);
            b.setPriority(channel.equals(CH_APPTS)
                    ? android.app.Notification.PRIORITY_HIGH
                    : android.app.Notification.PRIORITY_DEFAULT);
        }
        b.setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(0xFF0E5E54)
                .setContentIntent(pi)
                .setAutoCancel(true);
        if (body.length() > 40) b.setStyle(new android.app.Notification.BigTextStyle().bigText(body));
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(id, b.build());
    }

    public static boolean canNotify(Context ctx) {
        if (Build.VERSION.SDK_INT < 33) return true;
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.areNotificationsEnabled();
    }
}
