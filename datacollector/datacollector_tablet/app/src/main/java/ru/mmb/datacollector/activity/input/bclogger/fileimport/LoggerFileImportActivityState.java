package ru.mmb.datacollector.activity.input.bclogger.fileimport;

import ru.mmb.datacollector.activity.ActivityStateWithScanPointAndBTDevice;

public class LoggerFileImportActivityState extends ActivityStateWithScanPointAndBTDevice {
    public static final int STATE_NO_FILE_SELECTED = 1;
    public static final int STATE_FILE_SELECTED = 2;
    public static final int STATE_IMPORT_RUNNING = 3;

    private int state = STATE_NO_FILE_SELECTED;
    private String fileName = null;

    public LoggerFileImportActivityState() {
        super("input.bclogger.fileimport");
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
