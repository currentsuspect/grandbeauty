package com.grandbeauty;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int TAB_BOOK = 0, TAB_CLIENTS = 1;

    private ListView list;
    private View empty;
    private TextView emptyTitle, emptyBody, greeting, title, emptyCta;
    private EditText search;
    private Adapters.ApptsAdapter apptAdapter;
    private Adapters.ClientsAdapter clientAdapter;
    private int tab = TAB_BOOK;
    private String query = "";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!Fmt.onboarded(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        emptyTitle = findViewById(R.id.empty_title);
        emptyBody = findViewById(R.id.empty_body);
        emptyCta = findViewById(R.id.empty_cta);
        greeting = findViewById(R.id.greeting);
        title = findViewById(R.id.title);
        search = findViewById(R.id.search);

        findViewById(R.id.empty_cta).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, tab == TAB_BOOK
                        ? EditApptActivity.class : EditClientActivity.class));
            }
        });

        apptAdapter = new Adapters.ApptsAdapter(this, null, false, new Adapters.QuickDone() {
            @Override public void onQuickDone(Appt a) {
                Db.get(MainActivity.this).setStatus(a.id, Appt.DONE);
                Alarms.cancelAppt(MainActivity.this, a.id);
                reload();
            }
        });
        clientAdapter = new Adapters.ClientsAdapter(this,
                new HashMap<Long, Long>(), new HashMap<Long, Double>());

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            public void afterTextChanged(Editable s) {
                query = s.toString();
                reload();
            }
        });

        list.setAdapter(apptAdapter);
        registerForContextMenu(list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = p.getItemAtPosition(pos);
                if (tab == TAB_BOOK && o instanceof Appt) {
                    Intent i = new Intent(MainActivity.this, EditApptActivity.class);
                    i.putExtra("id", ((Appt) o).id);
                    startActivity(i);
                } else if (tab == TAB_CLIENTS && o instanceof Client) {
                    Intent i = new Intent(MainActivity.this, ClientDetailActivity.class);
                    i.putExtra("id", ((Client) o).id);
                    startActivity(i);
                }
            }
        });

        findViewById(R.id.fab).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (tab == TAB_BOOK) {
                    startActivity(new Intent(MainActivity.this, EditApptActivity.class));
                } else {
                    startActivity(new Intent(MainActivity.this, EditClientActivity.class));
                }
            }
        });

        bindNav();
        requestNotifPermIfNeeded();
    }

    private void bindNav() {
        int[][] map = {{R.id.nav_bookings}, {R.id.nav_clients}, {R.id.nav_settings}};
        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (v.getId() == R.id.nav_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return;
                }
                setTab(v.getId() == R.id.nav_bookings ? TAB_BOOK : TAB_CLIENTS);
            }
        };
        for (int[] m : map) findViewById(m[0]).setOnClickListener(l);
        setNavColors();
    }

    private void setTab(int t) {
        tab = t;
        query = "";
        search.setText("");
        setNavColors();
        reload();
    }

    private void setNavColors() {
        icon(findViewById(R.id.nav_bookings),
                R.drawable.ic_tab_cal_off, R.drawable.ic_tab_cal_on, tab == TAB_BOOK);
        icon(findViewById(R.id.nav_clients),
                R.drawable.ic_tab_people_off, R.drawable.ic_tab_people_on, tab == TAB_CLIENTS);
        icon(findViewById(R.id.nav_settings),
                R.drawable.ic_tab_gear_off, R.drawable.ic_tab_gear_off, false);
        label(findViewById(R.id.nav_bookings), tab == TAB_BOOK);
        label(findViewById(R.id.nav_clients), tab == TAB_CLIENTS);
        label(findViewById(R.id.nav_settings), false);
    }

    private void icon(View nav, int off, int on, boolean active) {
        android.view.ViewGroup g = (android.view.ViewGroup) nav;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof android.widget.ImageView)
                ((android.widget.ImageView) c).setImageResource(active ? on : off);
        }
    }

    private void label(View nav, boolean active) {
        android.view.ViewGroup g = (android.view.ViewGroup) nav;
        for (int i = 0; i < g.getChildCount(); i++) {
            View c = g.getChildAt(i);
            if (c instanceof TextView)
                ((TextView) c).setTextColor(getResources().getColor(active
                        ? R.color.primary : R.color.text_sub));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        greeting.setText(Fmt.greeting(this));
        Db db = Db.get(this);
        List<Appt> appts;
        Map<Long, Long> nextMap = new HashMap<>();

        if (tab == TAB_BOOK) {
            title.setText(Fmt.dateLong(System.currentTimeMillis()));
            appts = db.appts(query.isEmpty() ? null : query, null);
            apptAdapter.rebuild(appts);
            list.setAdapter(apptAdapter);
            boolean isEmpty = appts.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (isEmpty) {
                if (query.isEmpty()) {
                    emptyTitle.setText(R.string.empty_bookings_title);
                    emptyBody.setText(R.string.empty_bookings_body);
                    emptyCta.setText(R.string.empty_bookings_cta);
                } else {
                    emptyTitle.setText(R.string.empty_results_title);
                    emptyBody.setText(R.string.empty_results_body);
                    emptyCta.setVisibility(View.INVISIBLE);
                }
            }
            if (!isEmpty) emptyCta.setVisibility(View.VISIBLE);
        } else {
            title.setText(getString(R.string.tab_clients));
            List<Client> clients = db.clients(query.isEmpty() ? null : query);
            for (Appt a : db.appts(null, null)) {
                if (Appt.UP.equals(a.status) && a.start >= System.currentTimeMillis()) {
                    Long cur = nextMap.get(a.clientId);
                    if (cur == null || a.start < cur) nextMap.put(a.clientId, a.start);
                }
            }
            clientAdapter.rebuild(clients, nextMap, db.owedByClient());
            list.setAdapter(clientAdapter);
            boolean isEmpty = clients.isEmpty();
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            if (isEmpty) {
                if (query.isEmpty()) {
                    emptyTitle.setText(R.string.empty_clients_title);
                    emptyBody.setText(R.string.empty_clients_body);
                    emptyCta.setText(R.string.empty_clients_cta);
                    emptyCta.setVisibility(View.VISIBLE);
                } else {
                    emptyTitle.setText(R.string.empty_results_title);
                    emptyBody.setText(R.string.empty_results_body);
                    emptyCta.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    private void requestNotifPermIfNeeded() {
        Notif.channels(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                && !Fmt.askedNotif(this)) {
            Fmt.setAskedNotif(this);
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 41);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo mi) {
        super.onCreateContextMenu(menu, v, mi);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) mi;
        Object o = list.getItemAtPosition(info.position);
        if (o instanceof Appt) {
            Appt a = (Appt) o;
            if (a.clientPhone != null && !a.clientPhone.trim().isEmpty())
                menu.add(0, 1, 0, getString(R.string.call) + " " + a.clientName);
            menu.add(0, 2, 0, R.string.edit);
            if (Appt.UP.equals(a.status)) {
                menu.add(0, 3, 0, R.string.mark_done);
                menu.add(0, 4, 0, R.string.mark_cancelled);
            } else {
                menu.add(0, 5, 0, R.string.reopen);
            }
            menu.add(0, 6, 0, R.string.delete);
        } else if (o instanceof Client) {
            Client x = (Client) o;
            if (!x.phone.trim().isEmpty()) menu.add(0, 7, 0, getString(R.string.call) + " " + x.name);
            menu.add(0, 8, 0, R.string.edit);
            menu.add(0, 9, 0, R.string.delete);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Object o = list.getItemAtPosition(info.position);
        Db db = Db.get(this);
        switch (item.getItemId()) {
            case 1: { // call appt's client
                Appt a = (Appt) o;
                ImageViewTint.dial(this, a.clientPhone);
                return true;
            }
            case 2: {
                Appt a = (Appt) o;
                startActivity(new Intent(this, EditApptActivity.class).putExtra("id", a.id));
                return true;
            }
            case 3: {
                db.setStatus(((Appt) o).id, Appt.DONE);
                Alarms.cancelAppt(this, ((Appt) o).id);
                reload();
                return true;
            }
            case 4: {
                db.setStatus(((Appt) o).id, Appt.CANC);
                Alarms.cancelAppt(this, ((Appt) o).id);
                reload();
                return true;
            }
            case 5: {
                db.setStatus(((Appt) o).id, Appt.UP);
                Alarms.scheduleFor(this, db.appt(((Appt) o).id));
                reload();
                return true;
            }
            case 6: {
                confirmDelete(getString(R.string.confirm_delete_appt), new Runnable() {
                    public void run() {
                        Appt a = (Appt) list.getItemAtPosition(
                                ((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).position);
                        Db.get(MainActivity.this).deleteAppt(a.id);
                        Alarms.cancelAppt(MainActivity.this, a.id);
                        Toast.makeText(MainActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                        reload();
                    }
                });
                return true;
            }
            case 7: {
                ImageViewTint.dial(this, ((Client) o).phone);
                return true;
            }
            case 8: {
                startActivity(new Intent(this, EditClientActivity.class)
                        .putExtra("id", ((Client) o).id));
                return true;
            }
            case 9: {
                final Client cx = (Client) o;
                confirmDelete(getString(R.string.confirm_delete_client), new Runnable() {
                    public void run() {
                        Db.get(MainActivity.this).deleteClientCascade(cx.id);
                        Toast.makeText(MainActivity.this, R.string.deleted, Toast.LENGTH_SHORT).show();
                        reload();
                    }
                });
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    private void confirmDelete(String msg, final Runnable yes) {
        new android.app.AlertDialog.Builder(this)
                .setMessage(msg)
                .setPositiveButton(R.string.delete, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) { yes.run(); }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
