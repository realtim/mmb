package ru.mmb.datacollector.activity;

public class Constants {
    public static final boolean UPDATE_FROM_SAVED_BUNDLE = true;
    public static final boolean UPDATE_FOR_FIRST_LAUNCH = false;

    public static final String KEY_CURRENT_SCAN_POINT = "current.scanpoint";
    public static final String KEY_CURRENT_TEAM_NUMBER = "current.team.number";
    public static final String KEY_CURRENT_TEAM = "current.team";
    public static final String KEY_CURRENT_DEVICE_INFO = "current.device.info";
    public static final String KEY_CURRENT_BLUETOOTH_FILTER_JUST_LOGGERS = "current.bluetooth.filter.just.loggers";
    public static final String KEY_CURRENT_INPUT_CHECKPOINTS_STATE =
            "current.input.checkpoint.state";
    public static final String KEY_CURRENT_INPUT_CHECKED_DATE =
            "current.input.checked.date";
    public static final String KEY_CURRENT_INPUT_LOGGER_DATA_EXISTS = "current.input.logger.data.exists";
    public static final String KEY_CURRENT_INPUT_WITHDRAWN_CHECKED =
            "current.input.withdrawn.checked";
    public static final String KEY_CURRENT_INPUT_WITHDRAW_SCAN_POINT = "current.input.withdraw.scanpoint";
    public static final String KEY_EXPORT_RESULT_MESSAGE = "export.result.message";
    public static final String KEY_TEAM_FILTER_STATE = "team.filter.state";
    public static final String KEY_TEAM_FILTER_NUMBER_EXACT = "team.filter.number.exact";
    public static final String KEY_TEAM_FILTER_NUMBER = "team.filter.number";
    public static final String KEY_TEAM_FILTER_TEAM = "team.filter.team";
    public static final String KEY_TEAM_FILTER_MEMBER = "team.filter.member";

    public static final int REQUEST_CODE_DEFAULT_ACTIVITY = -1;
    public static final int REQUEST_CODE_SETTINGS_ACTIVITY = 2;
    public static final int REQUEST_CODE_INPUT_HISTORY_ACTIVITY = 4;
    public static final int REQUEST_CODE_INPUT_DATA_ACTIVITY = 5;
    public static final int REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY = 6;
    public static final int REQUEST_CODE_FILE_DIALOG = 7;
    public static final int REQUEST_CODE_SETTINGS_DB_FILE_DIALOG = 9;
    public static final int REQUEST_CODE_SETTINGS_DEVICE_JSON_DIALOG = 10;
    public static final int REQUEST_CODE_SCAN_POINT_ACTIVITY = 11;
    public static final int REQUEST_CODE_INPUT_BCLOGGER_START_ACTIVITY = 12;
    public static final int REQUEST_CODE_LAUNCH_BLUETOOTH_ACTIVITY = 13;
    public static final int REQUEST_CODE_BLUETOOTH_DEVICE_SELECT_ACTIVITY = 14;
    public static final int REQUEST_CODE_INPUT_BCLOGGER_SETTINGS_ACTIVITY = 15;
    public static final int REQUEST_CODE_INPUT_BCLOGGER_DATALOAD_ACTIVITY = 16;
    public static final int REQUEST_CODE_TEAM_SEARCH_ACTIVITY = 17;
    public static final int REQUEST_CODE_INPUT_BCLOGGER_FILEIMPORT_ACTIVITY = 18;
}
