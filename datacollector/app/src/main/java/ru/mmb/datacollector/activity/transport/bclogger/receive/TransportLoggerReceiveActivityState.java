package ru.mmb.datacollector.activity.transport.bclogger.receive;

import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;

public class TransportLoggerReceiveActivityState extends ActivityStateWithTeamAndScanPoint {
    public static final int STATE_IDLE = 1;
    public static final int STATE_LISTENING = 2;
    public static final int STATE_RECEIVING = 3;
    public static final int STATE_ADAPTER_DISABLED = 4;

    private int state = STATE_ADAPTER_DISABLED;

    public TransportLoggerReceiveActivityState() {
        super("transport.bclogger.receive");
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
