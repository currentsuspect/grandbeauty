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
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int TAB_BOOK = 0, TAB_CLIENTS = 1;

    private ListView list;
    private View empty;
    private TextView emptyTitle, emptyBody, greeting, title, emptyCta;
    private EditText search;
    private View stats, undoBar;
    private TextView sTodayV, sTakeV, sOwedV, undoMsg;
    private Appt undoAppt;
    private String undoPrev;
    private float undoShift;
    private final android.os.Handler ui = new android.os.Handler(
            android.os.Looper.getMainLooper());
    private final Runnable undoHide = new Runnable() {
        @Override public void run() {
            undoBar.animate().alpha(0f).setDuration(200)
                    .withEndAction(new Runnable() {
                        @Override public void run() {
                            undoBar.setVisibility(View.GONE);
                        }
                    }).start();
            findViewById(R.id.fab).animate().translationYBy(undoShift)
                    .setDuration(200).start();
            undoAppt = null;
        }
    };
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
        stats = findViewById(R.id.stats);
        sTodayV = findViewById(R.id.s_today_v);
        sTakeV = findViewById(R.id.s_take_v);
        sOwedV = findViewById(R.id.s_owed_v);
        undoBar = findViewById(R.id.undo_bar);
        undoMsg = findViewById(R.id.undo_msg);

        findViewById(R.id.undo_btn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (undoAppt == null) return;
                Db db = Db.get(MainActivity.this);
                db.setStatus(undoAppt.id, undoPrev);
                if (Appt.UP.equals(undoPrev))
                    Alarms.scheduleFor(MainActivity.this, db.appt(undoAppt.id));
                else
                    Alarms.cancelAppt(MainActivity.this, undoAppt.id);
                ui.removeCallbacks(undoHide);
                undoHide.run();
                reload();
            }
        });

        findViewById(R.id.s_owed_cell).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setTab(TAB_CLIENTS); }
        });

        findViewById(R.id.empty_cta).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, tab == TAB_BOOK
                        ? EditApptActivity.class : EditClientActivity.class));
                overridePendingTransition(R.anim.slide_in, R.anim.fade_out);
            }
        });

        apptAdapter = new Adapters.ApptsAdapter(this, null, false,
                new Adapters.RowMenu() {
                    @Override public void onRowMenu(Appt a, View anchor) {
                        Adapters.apptMenu(MainActivity.this, anchor, a, host);
                    }
                });
        clientAdapter = new Adapters.ClientsAdapter(this,
                new HashMap<Long, Long>(), new HashMap<Long, Double>());

        search.addTextChangedListener(new TextWatcher() {
            private final android.os.Handler h = new android.os.Handler();
            private final Runnable run = () -> {
                query = search.getText().toString();
                reload();
            };
            public void beforeTextChanged(CharSequence s, int a, int b2, int c) {}
            public void onTextChanged(CharSequence s, int a, int b2, int c) {}
            public void afterTextChanged(Editable s) {
                h.removeCallbacks(run);
                h.postDelayed(run, 160);
            }
        });

        list.setAdapter(apptAdapter);
        list.setLayoutAnimation(new android.view.animation.LayoutAnimationController(
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.item_fall),
                0.04f));
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Object o = p.getItemAtPosition(pos);
                if (tab == TAB_CLIENTS && o instanceof Client) {
                    startActivity(new Intent(MainActivity.this, ClientDetailActivity.class)
                            .putExtra("id", ((Client) o).id));
                    overridePendingTransition(R.anim.slide_in, R.anim.fade_out);
                }
            }
        });

        View fab = findViewById(R.id.fab);
        fab.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, android.view.MotionEvent ev) {
                switch (ev.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).start();
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(150)
                                .setInterpolator(new android.view.animation
                                        .OvershootInterpolator(2f)).start();
                        break;
                }
                return false;
            }
        });
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, tab == TAB_BOOK
                        ? EditApptActivity.class : EditClientActivity.class));
                overridePendingTransition(R.anim.slide_in, R.anim.fade_out);
            }
        });

        bindNav();
        requestNotifPermIfNeeded();
    }

    private final Adapters.RowMenuHost host = new Adapters.RowMenuHost() {
        @Override public void onMenuItem(final Appt a, int action) {
            Db db = Db.get(MainActivity.this);
            switch (action) {
                case 1: ImageViewTint.dial(MainActivity.this, a.clientPhone); break;
                case 2:
                    startActivity(new Intent(MainActivity.this, EditApptActivity.class)
                            .putExtra("id", a.id));
                    break;
                case 3: changeStatus(a, Appt.DONE, R.string.marked_done); break;
                case 4: changeStatus(a, Appt.CANC, R.string.marked_cancelled); break;
                case 5: changeStatus(a, Appt.UP, R.string.status_reopened); break;
                case 6:
                    db.settleClient(a.clientId);
                    Toast.makeText(MainActivity.this, R.string.settled,
                            Toast.LENGTH_SHORT).show();
                    reload();
                    break;
                case 7:
                    new android.app.AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.confirm_delete_appt)
                            .setPositiveButton(R.string.delete,
                                    new android.content.DialogInterface.OnClickListener() {
                                        public void onClick(android.content.DialogInterface d,
                                                            int w) {
                                            Db.get(MainActivity.this).deleteAppt(a.id);
                                            Alarms.cancelAppt(MainActivity.this, a.id);
                                            Toast.makeText(MainActivity.this,
                                                    R.string.deleted, Toast.LENGTH_SHORT).show();
                                            reload();
                                        }
                                    })
                            .setNegativeButton(R.string.cancel, null).show();
                    break;
            }
        }
    };

    private void bindNav() {
        int[][] map = {{R.id.nav_bookings}, {R.id.nav_clients}, {R.id.nav_settings}};
        View.OnClickListener l = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (v.getId() == R.id.nav_settings) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    overridePendingTransition(R.anim.slide_in, R.anim.fade_out);
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
            bindStats(db, appts);
            apptAdapter.rebuild(appts);
            list.setAdapter(apptAdapter);
            list.startLayoutAnimation();
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
            stats.setVisibility(View.GONE);
            List<Client> clients = db.clients(query.isEmpty() ? null : query);
            for (Appt a : db.appts(null, null)) {
                if (Appt.UP.equals(a.status) && a.start >= System.currentTimeMillis()) {
                    Long cur = nextMap.get(a.clientId);
                    if (cur == null || a.start < cur) nextMap.put(a.clientId, a.start);
                }
            }
            clientAdapter.rebuild(clients, nextMap, db.owedByClient());
            list.setAdapter(clientAdapter);
            list.startLayoutAnimation();
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

    private void changeStatus(Appt a, String to, int msgRes) {
        Db db = Db.get(this);
        undoPrev = a.status;
        db.setStatus(a.id, to);
        if (Appt.UP.equals(to)) Alarms.scheduleFor(this, db.appt(a.id));
        else Alarms.cancelAppt(this, a.id);
        undoAppt = db.appt(a.id);
        undoMsg.setText(msgRes);
        undoBar.animate().cancel();
        undoBar.setAlpha(1f);
        undoBar.setVisibility(View.VISIBLE);
        final View fab = findViewById(R.id.fab);
        undoBar.post(new Runnable() {
            @Override public void run() {
                undoShift = undoBar.getHeight() + Math.round(
                        12 * getResources().getDisplayMetrics().density);
                fab.animate().translationYBy(-undoShift).setDuration(200).start();
            }
        });
        ui.removeCallbacks(undoHide);
        ui.postDelayed(undoHide, 4500);
        reload();
    }

    private void bindStats(Db db, List<Appt> appts) {
        boolean show = tab == TAB_BOOK && query.isEmpty() && !appts.isEmpty();
        stats.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;

        Calendar c = Calendar.getInstance();
        Fmt.clearTime(c);
        long dayStart = c.getTimeInMillis();
        long dayEnd = dayStart + 86_400_000L;

        int today = 0;
        double expected = 0;
        for (Appt a : appts) {
            if (a.start >= dayStart && a.start < dayEnd && Appt.UP.equals(a.status)) {
                today++;
                if (a.amount != null) expected += a.amount;
            }
        }

        sTodayV.setText(String.valueOf(today));
        sTakeV.setText(Fmt.money(expected).isEmpty()
                ? "0" : Fmt.money(expected));
        double owed = db.totalOwed();
        sOwedV.setText(owed < 1 ? "—" : Fmt.money(owed));
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

}
