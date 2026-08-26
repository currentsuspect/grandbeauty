package com.grandbeauty;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;

public class OnboardingActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_onboarding);

        findViewById(R.id.start_btn).setOnClickListener(v -> {
            EditText name = findViewById(R.id.owner_name);
            Fmt.setOwnerName(this, name.getText().toString().trim());
            Fmt.setOnboarded(this);
            Notif.channels(this);
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 43);
            } else {
                enter();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        enter();
    }

    private void enter() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
