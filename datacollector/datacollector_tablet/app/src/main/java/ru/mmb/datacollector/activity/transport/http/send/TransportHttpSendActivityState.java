package ru.mmb.datacollector.activity.transport.http.send;

import android.os.Bundle;

import ru.mmb.datacollector.activity.CurrentState;

public class TransportHttpSendActivityState extends CurrentState {
    public static final int STATE_IDLE = 1;
    public static final int STATE_HTTP_SENDING = 2;

    private int state = STATE_IDLE;

    public TransportHttpSendActivityState() {
        super("transport.http.send");
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    @Override
    public void save(Bundle savedInstanceState) {
    }

    @Override
    public void load(Bundle savedInstanceState) {
    }

    @Override
    protected void update(boolean fromSavedBundle) {
    }
}
