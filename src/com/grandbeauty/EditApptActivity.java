package com.grandbeauty;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class EditApptActivity extends Activity {

    private Db db;
    private long id = -1;
    private List<Client> clients = new ArrayList<>();
    private long clientId = -1;
    private final Calendar cal = Calendar.getInstance();
    private String status = Appt.UP;
    private boolean remind = true;

    private Spinner spinner;
    private TextView dateBtn, timeBtn, delBtn, hintPast;
    private EditText service, amount, paid, notes;
    private View statusRow;
    private android.widget.Switch remindSwitch;
    private TextView stUp, stDone, stCanc;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_edit_appt);
        db = Db.get(this);

        id = getIntent().getLongExtra("id", -1);
        long preClient = getIntent().getLongExtra("client_id", -1);

        spinner = findViewById(R.id.client_spinner);
        dateBtn = findViewById(R.id.date_btn);
        timeBtn = findViewById(R.id.time_btn);
        service = findViewById(R.id.service);
        amount = findViewById(R.id.amount);
        paid = findViewById(R.id.paid);
        notes = findViewById(R.id.notes);
        hintPast = findViewById(R.id.hint_past);
        statusRow = findViewById(R.id.status_row);
        remindSwitch = findViewById(R.id.remind);
        delBtn = findViewById(R.id.delete_btn);
        stUp = findViewById(R.id.st_up);
        stDone = findViewById(R.id.st_done);
        stCanc = findViewById(R.id.st_canc);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.new_client_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(EditApptActivity.this,
                        EditClientActivity.class), 7);
            }
        });

        dateBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cal_s, 0, 0, 0);
        timeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clock_s, 0, 0, 0);

        cal.set(Calendar.SECOND, 0);
        if (id > 0) {
            Appt a = db.appt(id);
            if (a == null) { finish(); return; }
            cal.setTimeInMillis(a.start);
            clientId = a.clientId;
            service.setText(a.service);
            amount.setText(a.amount == null ? "" : Fmt.money(a.amount));
            paid.setText(a.paid == null ? "" : Fmt.money(a.paid));
            notes.setText(a.note);
            remind = a.remind;
            status = a.status;
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.edit));
            delBtn.setVisibility(View.VISIBLE);
            statusRow.setVisibility(View.VISIBLE);
        } else {
            // default: tomorrow at the next half-hour slot after now
            cal.setTimeInMillis(System.currentTimeMillis());
            cal.add(Calendar.DAY_OF_YEAR, 1);
            int min = cal.get(Calendar.MINUTE);
            cal.set(Calendar.MINUTE, min <= 30 ? 30 : 0);
            if (min > 30) cal.add(Calendar.HOUR_OF_DAY, 1);
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.new_appt));
        }
        if (preClient > 0) clientId = preClient;
        remindSwitch.setChecked(remind);

        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Pickers.datePicker(EditApptActivity.this, (Calendar) cal.clone(),
                        picked -> {
                            cal.set(Calendar.YEAR, picked.get(Calendar.YEAR));
                            cal.set(Calendar.MONTH, picked.get(Calendar.MONTH));
                            cal.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH));
                            refreshLabels();
                        });
            }
        });
        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Pickers.timePicker(EditApptActivity.this, (Calendar) cal.clone(),
                        picked -> {
                            cal.set(Calendar.HOUR_OF_DAY, picked.get(Calendar.HOUR_OF_DAY));
                            cal.set(Calendar.MINUTE, picked.get(Calendar.MINUTE));
                            refreshLabels();
                        });
            }
        });
        remindSwitch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { remind = remindSwitch.isChecked(); }
        });

        View.OnClickListener chip = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (v == stDone) status = Appt.DONE;
                else if (v == stCanc) status = Appt.CANC;
                else status = Appt.UP;
                paintChips();
            }
        };
        stUp.setOnClickListener(chip);
        stDone.setOnClickListener(chip);
        stCanc.setOnClickListener(chip);

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(); }
        });
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(EditApptActivity.this)
                        .setMessage(R.string.confirm_delete_appt)
                        .setPositiveButton(R.string.delete,
                                new AlertDialog.OnClickListener() {
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        db.deleteAppt(id);
                                        Alarms.cancelAppt(EditApptActivity.this, id);
                                        Toast.makeText(EditApptActivity.this,
                                                R.string.deleted, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null).show();
            }
        });

        loadClients();
        paintChips();
        refreshLabels();
    }

    private void loadClients() {
        clients = db.clients(null);
        List<String> names = new ArrayList<>();
        if (clients.isEmpty()) names.add(getString(R.string.empty_clients_title));
        for (Client c : clients) names.add(c.name);
        final int offset = clients.isEmpty() ? 1 : 0;
        ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                R.layout.spinner_item, names);
        ad.setDropDownViewResource(R.layout.spinner_item);
        spinner.setAdapter(ad);
        int sel = -1;
        for (int i = 0; i < clients.size(); i++)
            if (clients.get(i).id == clientId) sel = i + offset;
        if (sel >= 0) spinner.setSelection(sel, false);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long lid) {
                int idx = pos - offset;
                clientId = (idx >= 0 && idx < clients.size()) ? clients.get(idx).id : -1;
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 7 && res == RESULT_OK && data != null) {
            clientId = data.getLongExtra("id", -1);
            loadClients();
        }
    }

    private void paintChips() {
        styleChip(stUp, status.equals(Appt.UP));
        styleChip(stDone, status.equals(Appt.DONE));
        styleChip(stCanc, status.equals(Appt.CANC));
    }

    private void styleChip(TextView t, boolean on) {
        t.setBackgroundResource(on ? R.drawable.bg_chip_on : R.drawable.bg_chip_off);
        t.setTextColor(getResources().getColor(on
                ? R.color.on_primary_container : R.color.text_sub));
    }

    private void refreshLabels() {
        dateBtn.setText(Fmt.dayLabel(cal.getTimeInMillis()));
        timeBtn.setText(Fmt.timeShort(cal.getTimeInMillis()) + " "
                + Fmt.ampm(cal.getTimeInMillis()));
        Calendar today = Calendar.getInstance();
        Fmt.clearTime(today);
        hintPast.setVisibility(cal.getTimeInMillis() < today.getTimeInMillis()
                ? View.VISIBLE : View.GONE);
    }

    private void save() {
        if (clientId <= 0 || spinner.getSelectedItemPosition() < 0) {
            Toast.makeText(this, R.string.err_need_client, Toast.LENGTH_SHORT).show();
            return;
        }
        if (service.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, R.string.err_need_service, Toast.LENGTH_SHORT).show();
            return;
        }
        Appt a = new Appt();
        a.id = id;
        a.clientId = clientId;
        a.start = cal.getTimeInMillis();
        a.service = service.getText().toString().trim();
        String amt = amount.getText().toString().trim();
        try { a.amount = amt.isEmpty() ? null : Double.parseDouble(amt); }
        catch (NumberFormatException e) { a.amount = null; }
        String pd = paid.getText().toString().trim();
        try { a.paid = pd.isEmpty() ? null : Double.parseDouble(pd); }
        catch (NumberFormatException e) { a.paid = null; }
        a.note = notes.getText().toString().trim();
        a.remind = remindSwitch.isChecked();
        a.status = status;

        if (id <= 0) {
            for (Appt other : db.appts(null, clientId)) {
                if (other.id != id && Appt.UP.equals(other.status)
                        && Math.abs(other.start - a.start) < 60_000L) {
                    Toast.makeText(this, getString(R.string.already_booked, a.clientName),
                            Toast.LENGTH_LONG).show();
                    break;
                }
            }
        }

        a.id = db.putAppt(a);
        Alarms.scheduleFor(this, a);
        Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
        finish();
    }
}
