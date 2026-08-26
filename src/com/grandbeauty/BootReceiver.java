package com.grandbeauty;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent in) {
        Alarms.scheduleAll(ctx);
        Alarms.scheduleDaily(ctx);
    }
}
