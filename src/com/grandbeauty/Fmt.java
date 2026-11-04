package com.grandbeauty;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class Fmt {
    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    }

    public static int leadMinutes(Context c) { return p(c).getInt("lead_minutes", 120); }
    public static void setLead(Context c, int m) { p(c).edit().putInt("lead_minutes", m).apply(); }
    public static boolean dailyOn(Context c) { return p(c).getBoolean("daily", true); }
    public static void setDaily(Context c, boolean v) { p(c).edit().putBoolean("daily", v).apply(); }
    public static String ownerName(Context c) { return p(c).getString("owner_name", ""); }
    public static void setOwnerName(Context c, String v) { p(c).edit().putString("owner_name", v).apply(); }
    public static boolean askedNotif(Context c) { return p(c).getBoolean("asked_notif", false); }
    public static void setAskedNotif(Context c) { p(c).edit().putBoolean("asked_notif", true).apply(); }

    public static boolean onboarded(Context c) { return p(c).getBoolean("onboarded", false); }
    public static void setOnboarded(Context c) { p(c).edit().putBoolean("onboarded", true).apply(); }

    public static boolean regularsOn(Context c) { return p(c).getBoolean("regulars", true); }
    public static void setRegulars(Context c, boolean v) { p(c).edit().putBoolean("regulars", v).apply(); }

    public static boolean nudgedToday(Context c) {
        return p(c).getInt("nudge_day", -1) == dayIndex();
    }
    public static void markNudged(Context c) {
        p(c).edit().putInt("nudge_day", dayIndex()).apply();
    }
    private static int dayIndex() {
        return (int) (System.currentTimeMillis() / 86_400_000L);
    }

    public static void rememberPrice(Context c, String service, double amount) {
        String k = service.trim().toLowerCase(Locale.getDefault());
        if (k.isEmpty()) return;
        p(c).edit().putFloat("price_" + k, (float) amount).apply();
    }

    public static Double priceFor(Context c, String service) {
        String k = service.trim().toLowerCase(Locale.getDefault());
        if (k.isEmpty()) return null;
        float v = p(c).getFloat("price_" + k, -1f);
        return v < 0 ? null : (double) v;
    }

    public static String timeShort(long ms) {
        return new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date(ms));
    }

    public static String ampm(long ms) {
        return new SimpleDateFormat("a", Locale.getDefault()).format(new Date(ms));
    }

    public static String dayLabel(long ms) {
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ms);
        clearTime(then);
        Calendar today = Calendar.getInstance();
        clearTime(today);
        long diff = (then.getTimeInMillis() - today.getTimeInMillis()) / 86400000L;
        if (diff == 0) return "Today";
        if (diff == 1) return "Tomorrow";
        if (diff == -1) return "Yesterday";
        SimpleDateFormat f = diff >= 0 && diff < 7
                ? new SimpleDateFormat("EEEE", Locale.getDefault())
                : new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
        return f.format(new Date(ms));
    }

    public static String monthDay(long ms) {
        return new SimpleDateFormat("d MMM", Locale.getDefault()).format(new Date(ms));
    }

    public static String dateLong(long ms) {
        return new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date(ms));
    }

    public static String fullDateTime(long ms) {
        return new SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault()).format(new Date(ms));
    }

    public static String greeting(Context c) {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = h < 12 ? "Good morning" : h < 17 ? "Good afternoon" : "Good evening";
        String n = ownerName(c);
        return n.isEmpty() ? g + "," : g + ", " + n + ",";
    }

    public static String money(Double amt) {
        if (amt == null || amt <= 0) return "";
        if (amt == Math.floor(amt)) return String.format(Locale.getDefault(), "%,d", (long) (double) amt);
        return String.format(Locale.getDefault(), "%,.2f", amt);
    }

    public static String initials(String name) {
        StringBuilder sb = new StringBuilder();
        for (String w : name.trim().split("\\s+")) {
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0)));
            if (sb.length() >= 2) break;
        }
        return sb.length() == 0 ? "?" : sb.toString();
    }

    public static String phoneDigits(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9+]", "");
    }

    public static void clearTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
