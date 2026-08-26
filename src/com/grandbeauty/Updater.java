package com.grandbeauty;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class Updater {
    public static final String REPO = "https://github.com/currentsuspect/grandbeauty";
    private static final String API = "https://api.github.com/repos/currentsuspect/grandbeauty/releases/latest";

    public static class Info {
        public int code;
        public String name = "";
        public String apkUrl = "";
    }

    public interface Progress { void onProgress(int pct); }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        c.setRequestProperty("User-Agent", "GrandBeauty-app");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        return c;
    }

    /** latest release info, or null on any failure */
    public static Info latest() {
        try {
            HttpURLConnection c = open(API);
            int sc = c.getResponseCode();
            if (sc != 200) { c.disconnect(); return null; }
            StringBuilder sb = new StringBuilder();
            InputStream in = c.getInputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            in.close();
            c.disconnect();

            JSONObject rel = new JSONObject(sb.toString());
            Info info = new Info();
            info.name = rel.optString("tag_name", "").replaceFirst("^v", "");
            try { info.code = Integer.parseInt(info.name.split("\\.")[0]); }
            catch (Exception e) { info.code = -1; }
            org.json.JSONArray assets = rel.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject a = assets.getJSONObject(i);
                    if ("GrandBeauty.apk".equals(a.optString("name"))) {
                        info.apkUrl = a.optString("browser_download_url", "");
                    }
                }
            }
            if (info.apkUrl.isEmpty()) return null;
            return info;
        } catch (Exception e) {
            return null;
        }
    }

    public static File download(Context ctx, Info info, Progress p) throws Exception {
        HttpURLConnection c = open(info.apkUrl);
        int sc = c.getResponseCode();
        if (sc != 200) { c.disconnect(); throw new Exception("HTTP " + sc); }
        long total = c.getContentLengthLong();
        File out = new File(ctx.getCacheDir(), "update.apk");
        FileOutputStream fos = new FileOutputStream(out);
        InputStream in = c.getInputStream();
        byte[] buf = new byte[16384];
        long done = 0;
        int n, lastPct = -1;
        while ((n = in.read(buf)) > 0) {
            fos.write(buf, 0, n);
            done += n;
            if (total > 0 && p != null) {
                int pct = (int) (done * 100 / total);
                if (pct != lastPct) { lastPct = pct; p.onProgress(pct); }
            }
        }
        fos.close();
        in.close();
        c.disconnect();
        return out;
    }

    public static void install(Context ctx, File apk) throws Exception {
        PackageInstaller pi = ctx.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        int sessionId = pi.createSession(params);
        PackageInstaller.Session session = pi.openSession(sessionId);
        OutputStream os = session.openWrite("grandbeauty", 0, apk.length());
        InputStream in = new java.io.FileInputStream(apk);
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
        session.fsync(os);
        in.close();
        os.close();

        Intent intent = new Intent(ctx, UpdateReceiver.class);
        PendingIntent pi2 = PendingIntent.getBroadcast(ctx, 7777, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        session.commit(pi2.getIntentSender());
        session.close();
    }

    public static int currentCode(Context ctx) {
        try {
            return ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String currentName(Context ctx) {
        try {
            return ctx.getPackageManager()
                    .getPackageInfo(ctx.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
