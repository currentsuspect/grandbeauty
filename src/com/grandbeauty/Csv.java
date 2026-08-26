package com.grandbeauty;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class Csv {
    private Csv() {}

    public static String export(Context ctx) throws Exception {
        List<Appt> appts = Db.get(ctx).appts(null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("date,start_time,client,phone,service,amount,paid,balance,status,notes\n");
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat tf = new SimpleDateFormat("HH:mm", Locale.US);
        for (Appt a : appts) {
            sb.append(df.format(new Date(a.start))).append(',');
            sb.append(tf.format(new Date(a.start))).append(',');
            sb.append(esc(a.clientName)).append(',');
            sb.append(esc(a.clientPhone == null ? "" : a.clientPhone)).append(',');
            sb.append(esc(a.service)).append(',');
            sb.append(a.amount == null ? "" : String.valueOf(a.amount)).append(',');
            sb.append(a.paid == null ? "" : String.valueOf(a.paid)).append(',');
            sb.append(a.balance() <= 0 ? "" : String.valueOf(a.balance())).append(',');
            sb.append(a.status).append(',');
            sb.append(esc(a.note)).append('\n');
        }
        String name = "bookings_" +
                new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".csv";
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Bookings");
            v.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = ctx.getContentResolver()
                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new IllegalStateException("MediaStore insert failed");
            OutputStream os = ctx.getContentResolver().openOutputStream(uri);
            os.write(bytes);
            os.close();
            v.clear();
            v.put(MediaStore.MediaColumns.IS_PENDING, 0);
            ctx.getContentResolver().update(uri, v, null, null);
            return "Downloads/Bookings/" + name;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "Bookings");
            if (!dir.exists() && !dir.mkdirs())
                throw new IllegalStateException("cannot mkdirs " + dir);
            File f = new File(dir, name);
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(bytes);
            fos.close();
            return f.getAbsolutePath().replace("/storage/emulated/0", "/sdcard");
        }
    }

    private static String esc(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return '"' + s.replace("\"", "\"\"") + '"';
        return s;
    }
}
