package com.grandbeauty;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public final class ImageViewTint {
    private ImageViewTint() {}

    public static void dial(Context c, String phone) {
        String p = Fmt.phoneDigits(phone);
        if (p.isEmpty()) return;
        c.startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p)));
    }

    public static void sms(Context c, String phone) {
        String p = Fmt.phoneDigits(phone);
        if (p.isEmpty()) return;
        c.startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + p)));
    }

    public static void whatsapp(Context c, String phone) {
        String p = Fmt.phoneDigits(phone).replace("+", "");
        if (p.isEmpty()) return;
        try {
            c.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + p)));
            return;
        } catch (Exception ignored) {}
        sms(c, phone);
    }
}
