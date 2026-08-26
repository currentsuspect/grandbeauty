package com.grandbeauty;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final int[] LEAD_IDS = {R.id.lead_15, R.id.lead_30,
            R.id.lead_60, R.id.lead_120, R.id.lead_1440};
    private static final int[] LEAD_MIN = {15, 30, 60, 120, 1440};

    private TextView exactText;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        ((EditText) findViewById(R.id.owner_name)).setText(Fmt.ownerName(this));

        View.OnClickListener leadClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                for (int i = 0; i < LEAD_IDS.length; i++) {
                    if (v.getId() == LEAD_IDS[i]) {
                        Fmt.setLead(SettingsActivity.this, LEAD_MIN[i]);
                        Alarms.scheduleAll(SettingsActivity.this);
                        paintLead();
                    }
                }
            }
        };
        for (int id : LEAD_IDS) findViewById(id).setOnClickListener(leadClick);

        Switch daily = findViewById(R.id.daily_switch);
        daily.setChecked(Fmt.dailyOn(this));
        daily.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Fmt.setDaily(SettingsActivity.this, daily.isChecked());
                Alarms.scheduleDaily(SettingsActivity.this);
            }
        });

        Switch regulars = findViewById(R.id.regulars_switch);
        regulars.setChecked(Fmt.regularsOn(this));
        regulars.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Fmt.setRegulars(SettingsActivity.this, regulars.isChecked());
            }
        });

        exactText = findViewById(R.id.exact_text);
        findViewById(R.id.exact_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 31 && !Alarms.canExact(SettingsActivity.this)) {
                    try {
                        startActivity(new Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
                    } catch (Exception ignored) {}
                }
            }
        });

        findViewById(R.id.export_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { exportFlow(); }
        });

        findViewById(R.id.share_row).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { shareWeek(); }
        });
    }

    private void paintLead() {
        int cur = Fmt.leadMinutes(this);
        for (int i = 0; i < LEAD_IDS.length; i++) {
            TextView t = findViewById(LEAD_IDS[i]);
            boolean on = cur == LEAD_MIN[i];
            t.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
            t.setTextColor(getResources().getColor(on
                    ? R.color.on_primary_container : R.color.text_sub));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        paintLead();
        if (Build.VERSION.SDK_INT >= 31 && !Alarms.canExact(this)) {
            exactText.setText(R.string.set_exact_no);
            exactText.setTextColor(getResources().getColor(R.color.danger));
        } else {
            exactText.setText(R.string.set_exact_ok);
            exactText.setTextColor(getResources().getColor(R.color.text_sub));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        EditText e = findViewById(R.id.owner_name);
        Fmt.setOwnerName(this, e.getText().toString().trim());
        Alarms.scheduleAll(this);
    }

    private void exportFlow() {
        if (Build.VERSION.SDK_INT < 29
                && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 42);
            return;
        }
        doExport();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == 42) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) doExport();
            else Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void doExport() {
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final String path = Csv.export(SettingsActivity.this);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(SettingsActivity.this,
                                    getString(R.string.exported_to, path),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(SettingsActivity.this,
                                    getString(R.string.export_failed) + " (" + e.getMessage() + ")",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void shareWeek() {
        long now = System.currentTimeMillis();
        long end = now + 7L * 86_400_000L;
        StringBuilder sb = new StringBuilder("My salon bookings — next 7 days:\n");
        int n = 0;
        SimpleDateFormat df = new SimpleDateFormat("EEE d MMM", Locale.getDefault());
        for (Appt a : Db.get(this).appts(null, null)) {
            if (a.start >= now && a.start <= end && Appt.UP.equals(a.status)) {
                sb.append(df.format(new Date(a.start)))
                  .append(" · ").append(Fmt.timeShort(a.start))
                  .append(" · ").append(a.clientName)
                  .append(" · ").append(a.service).append('\n');
                n++;
            }
        }
        if (n == 0) sb.append("nothing booked yet");
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(i, getString(R.string.set_share)));
    }
}
