package com.grandbeauty;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ClientDetailActivity extends Activity {
    private Db db;
    private long id = -1;
    private Adapters.ApptsAdapter adapter;
    private View header;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_client_detail);
        db = Db.get(this);
        id = getIntent().getLongExtra("id", -1);

        header = getLayoutInflater().inflate(R.layout.detail_header, null, false);
        ListView list = findViewById(R.id.list);
        adapter = new Adapters.ApptsAdapter(this, null, true, new Adapters.QuickDone() {
            @Override public void onQuickDone(Appt a) {
                Db.get(ClientDetailActivity.this).setStatus(a.id, Appt.DONE);
                Alarms.cancelAppt(ClientDetailActivity.this, a.id);
                reload();
            }
        });
        list.setAdapter(adapter);
        list.addHeaderView(header, null, false);
        list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override public void onItemClick(android.widget.AdapterView<?> p, View v,
                                              int pos, long lid) {
                Object o = p.getItemAtPosition(pos);
                if (o instanceof Appt) {
                    startActivity(new Intent(ClientDetailActivity.this, EditApptActivity.class)
                            .putExtra("id", ((Appt) o).id));
                }
            }
        });

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.edit_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(ClientDetailActivity.this, EditClientActivity.class)
                        .putExtra("id", id));
            }
        });
        findViewById(R.id.delete_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(ClientDetailActivity.this)
                        .setMessage(R.string.confirm_delete_client)
                        .setPositiveButton(R.string.delete,
                                new AlertDialog.OnClickListener() {
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        db.deleteClientCascade(id);
                                        Toast.makeText(ClientDetailActivity.this,
                                                R.string.deleted, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null).show();
            }
        });
        header.findViewById(R.id.settle_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                db.settleClient(id);
                Toast.makeText(ClientDetailActivity.this, R.string.settled,
                        Toast.LENGTH_SHORT).show();
                reload();
            }
        });
        header.findViewById(R.id.call_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { call(); }
        });
        header.findViewById(R.id.sms_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { ImageViewTint.sms(ClientDetailActivity.this, phone()); }
        });
        header.findViewById(R.id.wa_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { ImageViewTint.whatsapp(ClientDetailActivity.this, phone()); }
        });
    }

    private String phone() {
        Client x = db.client(id);
        return x == null ? "" : x.phone;
    }

    private void call() {
        String p = phone();
        if (p.trim().isEmpty()) {
            Toast.makeText(this, R.string.no_phone, Toast.LENGTH_SHORT).show();
            return;
        }
        ImageViewTint.dial(this, p);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        Client x = db.client(id);
        if (x == null) { finish(); return; }

        ((TextView) header.findViewById(R.id.initials)).setText(Fmt.initials(x.name));
        TextView nameV = header.findViewById(R.id.name);
        nameV.setText(x.name);
        TextView phoneV = header.findViewById(R.id.phone);
        if (!x.note.trim().isEmpty()) {
            phoneV.setText(x.note.trim());
        } else if (!x.phone.trim().isEmpty()) {
            phoneV.setText(x.phone);
        } else {
            phoneV.setText(R.string.no_phone);
        }

        Db.Stats s = db.stats(id);
        ((TextView) header.findViewById(R.id.stat_visits)).setText(String.valueOf(s.visits));
        ((TextView) header.findViewById(R.id.stat_total)).setText(Fmt.money(s.total).isEmpty()
                ? "—" : Fmt.money(s.total));
        TextView balanceV = header.findViewById(R.id.stat_balance);
        double bal = s.balance;
        balanceV.setText(bal >= 1 ? Fmt.money(bal) : "—");
        balanceV.setTextColor(getResources().getColor(bal >= 1
                ? R.color.secondary : R.color.text));

        TextView settle = header.findViewById(R.id.settle_btn);
        if (bal >= 1) {
            settle.setText(getString(R.string.settle, Fmt.money(bal)));
            settle.setVisibility(View.VISIBLE);
        } else {
            settle.setVisibility(View.GONE);
        }

        adapter.rebuild(db.appts(null, id));
    }
}
