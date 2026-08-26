package com.grandbeauty;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class EditClientActivity extends Activity {
    private Db db;
    private long id = -1;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_edit_client);
        db = Db.get(this);
        id = getIntent().getLongExtra("id", -1);

        EditText name = findViewById(R.id.name);
        EditText phone = findViewById(R.id.phone);
        EditText notes = findViewById(R.id.notes);
        TextView delBtn = findViewById(R.id.delete_btn);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        if (id > 0) {
            Client x = db.client(id);
            if (x == null) { finish(); return; }
            name.setText(x.name);
            phone.setText(x.phone);
            notes.setText(x.note);
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.edit));
            delBtn.setVisibility(View.VISIBLE);
        } else {
            ((TextView) findViewById(R.id.title)).setText(getString(R.string.new_client));
        }

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) {
                    name.setError("");
                    return;
                }
                Client x = new Client();
                x.id = id;
                x.name = n;
                x.phone = phone.getText().toString().trim();
                x.note = notes.getText().toString().trim();
                x.id = db.putClient(x);
                setResult(RESULT_OK, new Intent().putExtra("id", x.id));
                Toast.makeText(EditClientActivity.this, R.string.saved, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                new AlertDialog.Builder(EditClientActivity.this)
                        .setMessage(R.string.confirm_delete_client)
                        .setPositiveButton(R.string.delete,
                                new AlertDialog.OnClickListener() {
                                    public void onClick(android.content.DialogInterface d, int w) {
                                        db.deleteClientCascade(id);
                                        Toast.makeText(EditClientActivity.this,
                                                R.string.deleted, Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                })
                        .setNegativeButton(R.string.cancel, null).show();
            }
        });
    }
}
