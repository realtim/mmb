package ru.mmb.datacollector.activity.input.data.checkpoints;

import android.app.Activity;
import android.os.Bundle;

import java.util.Date;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.datacollector.activity.LevelPointType;
import ru.mmb.datacollector.db.RawTeamLevelPointsRecord;
import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.checkpoints.CheckedState;
import ru.mmb.datacollector.model.history.DataStorage;
import ru.mmb.datacollector.model.registry.Settings;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_INPUT_CHECKED_DATE;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_INPUT_CHECKPOINTS_STATE;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_INPUT_LOGGER_DATA_EXISTS;

public class InputDataActivityState extends ActivityStateWithTeamAndScanPoint {
    private CheckedState checkedState = new CheckedState();
    private DateRecord inputDate = new DateRecord();
    private boolean loggerDataExists = false;
    private DateRecord prevDateTime = null;

    public InputDataActivityState() {
        super("input.data");
    }

    public void setChecked(Checkpoint checkpoint, boolean checked) {
        checkedState.setChecked(checkpoint.getCheckpointOrder(), checked);
        fireStateChanged();
    }

    public boolean isChecked(Checkpoint checkpoint) {
        return checkedState.isChecked(checkpoint.getCheckpointOrder());
    }

    public void setPrevDateTime(Date prevDateTime) {
        this.prevDateTime = new DateRecord(prevDateTime);
    }

    public void setInputDateDatePart(int year, int month, int day) {
        inputDate.setYear(year);
        inputDate.setMonth(month);
        inputDate.setDay(day);
        fireStateChanged();
    }

    public void setInputDateTimePart(int hour, int minute) {
        inputDate.setHour(hour);
        inputDate.setMinute(minute);
        // Log.d("input data activity", "set hour: " + hour + " and minute: " + minute);
        fireStateChanged();
    }

    public DateRecord getInputDate() {
        return inputDate;
    }

    @Override
    public void save(Bundle savedInstanceState) {
        super.save(savedInstanceState);
        savedInstanceState.putSerializable(KEY_CURRENT_INPUT_CHECKPOINTS_STATE, checkedState);
        savedInstanceState.putSerializable(KEY_CURRENT_INPUT_CHECKED_DATE, inputDate);
        savedInstanceState.putSerializable(KEY_CURRENT_INPUT_LOGGER_DATA_EXISTS, loggerDataExists);
    }

    @Override
    public void load(Bundle savedInstanceState) {
        super.load(savedInstanceState);
        if (savedInstanceState.containsKey(KEY_CURRENT_INPUT_CHECKPOINTS_STATE))
            checkedState =
                    (CheckedState) savedInstanceState.getSerializable(KEY_CURRENT_INPUT_CHECKPOINTS_STATE);
        if (savedInstanceState.containsKey(KEY_CURRENT_INPUT_CHECKED_DATE))
            inputDate =
                    (DateRecord) savedInstanceState.getSerializable(KEY_CURRENT_INPUT_CHECKED_DATE);
        if (savedInstanceState.containsKey(KEY_CURRENT_INPUT_LOGGER_DATA_EXISTS))
            loggerDataExists =
                    (Boolean) savedInstanceState.getSerializable(KEY_CURRENT_INPUT_LOGGER_DATA_EXISTS);
    }

    public boolean needInputCheckpoints() {
        return getCurrentLevelPointType() == LevelPointType.FINISH;
    }

    private LevelPointType getCurrentLevelPointType() {
        LevelPoint levelPoint = getLevelPointForTeam();
        return levelPoint.getPointType().isStart() ? LevelPointType.START : LevelPointType.FINISH;
    }

    public boolean isCommonStart() {
        if (getCurrentLevelPointType() == LevelPointType.FINISH) return false;

        LevelPoint levelPoint = getLevelPointForTeam();
        return levelPoint.isCommonStart();
    }

    public void setInputDate(Date date) {
        inputDate = new DateRecord(date);
        fireStateChanged();
    }

    public void initInputDateFromCommonStart() {
        inputDate = new DateRecord(getLevelPointForTeam().getLevelPointMinDateTime());
    }

    public String getResultText(Activity context) {
        StringBuilder sb = new StringBuilder();
        appendDateText(context, sb);
        appendTakenCheckpointsText(context, sb);
        return sb.toString();
    }

    private void appendDateText(Activity context, StringBuilder sb) {
        if (getCurrentLevelPointType() == LevelPointType.START)
            sb.append(context.getResources().getString(R.string.input_data_res_start_time));
        else
            sb.append(context.getResources().getString(R.string.input_data_res_finish_time));
        sb.append(" ");
        sb.append(inputDate.toPrettyString());
    }

    @SuppressWarnings("unused")
    private void appendMissedCheckpointsText(Activity context, StringBuilder sb) {
        if (getCurrentLevelPointType() == LevelPointType.FINISH) {
            sb.append("\n");
            sb.append(context.getResources().getString(R.string.input_data_res_missed));
            sb.append(" ");
            sb.append(checkedState.getMissedCheckpointsText());
        }
    }

    private void appendTakenCheckpointsText(Activity context, StringBuilder sb) {
        if (getCurrentLevelPointType() == LevelPointType.FINISH) {
            sb.append("\n");
            sb.append(context.getResources().getString(R.string.input_data_res_taken));
            sb.append(" ");
            sb.append(checkedState.getTakenCheckpointsText());
        }
    }

    @Override
    protected void update(boolean fromSavedBundle) {
        super.update(fromSavedBundle);
        LevelPoint levelPoint = getLevelPointForTeam();
        checkedState.setLevelPoint(levelPoint);
        if (!fromSavedBundle) {
            RawTeamLevelPointsRecord previousRecord =
                    SQLiteDatabaseAdapter.getConnectedInstance().getExistingTeamResultRecord(getCurrentScanPoint(), getCurrentTeam());
            if (previousRecord != null) {
                checkedState.loadTakenCheckpoints(previousRecord.getCheckedMap());
            }
            if (!isCommonStart()) {
                RawLoggerData rawLoggerData =
                        SQLiteDatabaseAdapter.getConnectedInstance().getExistingLoggerRecord(getCurrentScanPoint(), getCurrentTeam());
                if (rawLoggerData != null) {
                    inputDate = new DateRecord(rawLoggerData.getScannedDateTime());
                    prevDateTime = new DateRecord(rawLoggerData.getScannedDateTime());
                    loggerDataExists = true;
                }
            } else {
                inputDate = new DateRecord(levelPoint.getLevelPointMinDateTime());
                loggerDataExists = true;
            }
        }
    }

    public void checkAll() {
        checkedState.checkAll();
        fireStateChanged();
    }

    public void uncheckAll() {
        checkedState.uncheckAll();
        fireStateChanged();
    }

    public void saveInputDataToDB(Date recordDateTime) {
        SQLiteDatabaseAdapter.getConnectedInstance().saveRawTeamLevelPoints(getCurrentScanPoint(), getCurrentTeam(), checkedState.getTakenCheckpointsRawText(), recordDateTime);
        if (prevDateTime != null && !prevDateTime.equals(inputDate)) {
            SQLiteDatabaseAdapter.getConnectedInstance().saveRawLoggerDataManual(getCurrentScanPoint(), getCurrentTeam(), inputDate.toDate(), recordDateTime);
        }
    }

    public void putTeamLevelPointToDataStorage(Date recordDateTime) {
        RawTeamLevelPoints rawTeamLevelPoints =
                new RawTeamLevelPoints(getCurrentTeam().getTeamId(), Settings.getInstance().getUserId(), Settings.getInstance().getDeviceId(), getCurrentScanPoint().getScanPointId(), checkedState.getTakenCheckpointsRawText(), recordDateTime);
        rawTeamLevelPoints.setTeam(getCurrentTeam());
        rawTeamLevelPoints.setScanPoint(getCurrentScanPoint());
        rawTeamLevelPoints.initTakenCheckpoints();
        DataStorage.putRawTeamLevelPoints(rawTeamLevelPoints);
    }

    public boolean isLoggerDataExists() {
        return loggerDataExists;
    }
}
