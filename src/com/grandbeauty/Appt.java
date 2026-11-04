package com.grandbeauty;

public class Appt {
    public static final String UP = "up";
    public static final String DONE = "done";
    public static final String CANC = "canc";

    public long id;
    public long clientId;
    public long start;
    public String service = "";
    public Double amount;
    public Double paid;
    public String note = "";
    public boolean remind = true;
    public String status = UP;
    public String clientName = "";
    public String clientPhone = "";
    public boolean historyView;
    public boolean nextUp;

    public double balance() {
        if (amount == null || CANC.equals(status)) return 0;
        return amount - (paid == null ? amount : paid);
    }
}
