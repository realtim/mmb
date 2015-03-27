package ru.mmb.datacollector.activity.transport.bclogger.send;

import ru.mmb.datacollector.activity.ActivityStateWithScanPointAndBTDevice;

public class TransportLoggerSendActivityState extends ActivityStateWithScanPointAndBTDevice {
    public static final int STATE_ADAPTER_DISABLED = 1;
    public static final int STATE_DEVICE_NOT_SELECTED = 2;
    public static final int STATE_DEVICE_SELECTED = 3;
    public static final int STATE_SENDING = 4;

    private int state = STATE_ADAPTER_DISABLED;

    public TransportLoggerSendActivityState() {
        super("transport.bclogger.send");
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
}
