package ru.mmb.datacollector.activity.transport.http.receive;

import android.os.Bundle;

import ru.mmb.datacollector.activity.CurrentState;

public class TransportHttpReceiveActivityState extends CurrentState {
    public static final int STATE_IDLE = 1;
    public static final int STATE_HTTP_RECEIVING = 2;

    private int state = STATE_IDLE;

    public TransportHttpReceiveActivityState() {
        super("transport.http.receive");
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
