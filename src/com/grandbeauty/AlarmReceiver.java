package com.grandbeauty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        long id = in.getLongExtra("id", -1);
        if (id <= 0) return;
        Appt a = Db.get(ctx).appt(id);
        if (a == null || !Appt.UP.equals(a.status)) return;
        String body = Fmt.fullDateTime(a.start) + " · " + a.service;
        if (a.clientPhone != null && !a.clientPhone.isEmpty()) body += "\n" + a.clientPhone;
        Notif.notify(ctx, Notif.CH_APPTS, (int) id,
                ctx.getString(R.string.notif_reminder_title, a.clientName), body,
                EditApptActivity.class, id);
    }
}
