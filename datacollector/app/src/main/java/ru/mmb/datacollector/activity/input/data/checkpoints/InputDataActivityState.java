package ru.mmb.datacollector.activity.input.data.checkpoints;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.text.ParseException;
import java.util.Date;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.ActivityStateWithTeamAndScanPoint;
import ru.mmb.datacollector.activity.LevelPointType;
import ru.mmb.datacollector.db.DatacollectorDB;
import ru.mmb.datacollector.db.TeamResultRecord;
import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.TeamResult;
import ru.mmb.datacollector.model.checkpoints.CheckedState;
import ru.mmb.datacollector.model.history.DataStorage;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.util.PrettyDateFormat;

import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_INPUT_CHECKED_DATE;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_INPUT_CHECKPOINTS_STATE;
import static ru.mmb.datacollector.activity.Constants.KEY_CURRENT_INPUT_EXISTING_RECORD;

public class InputDataActivityState extends ActivityStateWithTeamAndScanPoint
{
	private CheckedState checkedState = new CheckedState();
	private DateRecord inputDate = new DateRecord();
	private boolean editingExistingRecord = false;

	public InputDataActivityState()
	{
		super("input.data");
	}

	public void setChecked(Checkpoint checkpoint, boolean checked)
	{
		checkedState.setChecked(checkpoint.getCheckpointOrder(), checked);
		fireStateChanged();
	}

	public boolean isChecked(Checkpoint checkpoint)
	{
		return checkedState.isChecked(checkpoint.getCheckpointOrder());
	}

	public void setInputDate(int year, int month, int day, int hour, int minute)
	{
		inputDate = new DateRecord(year, month, day, hour, minute);
		fireStateChanged();
	}

	public void setInputDateDatePart(int year, int month, int day)
	{
		inputDate.setYear(year);
		inputDate.setMonth(month);
		inputDate.setDay(day);
		fireStateChanged();
	}

	public void setInputDateTimePart(int hour, int minute)
	{
		inputDate.setHour(hour);
		inputDate.setMinute(minute);
		// Log.d("input data activity", "set hour: " + hour + " and minute: " + minute);
		fireStateChanged();
	}

	public DateRecord getInputDate()
	{
		return inputDate;
	}

	@Override
	public void save(Bundle savedInstanceState)
	{
		super.save(savedInstanceState);
		savedInstanceState.putSerializable(KEY_CURRENT_INPUT_CHECKPOINTS_STATE, checkedState);
		savedInstanceState.putSerializable(KEY_CURRENT_INPUT_CHECKED_DATE, inputDate);
		savedInstanceState.putSerializable(KEY_CURRENT_INPUT_EXISTING_RECORD, new Boolean(editingExistingRecord));
	}

	@Override
	public void load(Bundle savedInstanceState)
	{
		super.load(savedInstanceState);
		if (savedInstanceState.containsKey(KEY_CURRENT_INPUT_CHECKPOINTS_STATE))
		    checkedState =
		        (CheckedState) savedInstanceState.getSerializable(KEY_CURRENT_INPUT_CHECKPOINTS_STATE);
		if (savedInstanceState.containsKey(KEY_CURRENT_INPUT_CHECKED_DATE))
		    inputDate =
		        (DateRecord) savedInstanceState.getSerializable(KEY_CURRENT_INPUT_CHECKED_DATE);
		if (savedInstanceState.containsKey(KEY_CURRENT_INPUT_EXISTING_RECORD))
		    editingExistingRecord =
		        (Boolean) savedInstanceState.getSerializable(KEY_CURRENT_INPUT_EXISTING_RECORD);
	}

	@Override
	public void saveToSharedPreferences(SharedPreferences preferences)
	{
		super.saveToSharedPreferences(preferences);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(getPrefix() + "." + KEY_CURRENT_INPUT_CHECKED_DATE, inputDate.saveToString());
		editor.commit();
	}

	@Override
	public void loadFromSharedPreferences(SharedPreferences preferences)
	{
		super.loadFromSharedPreferences(preferences);
		try
		{
			inputDate =
			    DateRecord.parseString(preferences.getString(getPrefix() + "."
			            + KEY_CURRENT_INPUT_CHECKED_DATE, ""));
		}
		catch (ParseException e)
		{
			inputDate = new DateRecord();
		}
	}

	public boolean needInputCheckpoints()
	{
		return getCurrentLevelPointType() == LevelPointType.FINISH;
	}

	private LevelPointType getCurrentLevelPointType()
	{
		LevelPoint levelPoint = getLevelPointForTeam();
		return levelPoint.getPointType().isStart() ? LevelPointType.START : LevelPointType.FINISH;
	}

	public boolean isCommonStart()
	{
		if (getCurrentLevelPointType() == LevelPointType.FINISH) return false;

		LevelPoint levelPoint = getLevelPointForTeam();
		return levelPoint.isCommonStart();
	}

	public void setInputDate(Date date)
	{
		inputDate = new DateRecord(date);
		fireStateChanged();
	}

	public void initInputDateFromCommonStart()
	{
		inputDate = new DateRecord(getLevelPointForTeam().getLevelPointMinDateTime());
	}

	public String getResultText(Activity context)
	{
		StringBuilder sb = new StringBuilder();
		appendDateText(context, sb);
		appendTakenCheckpointsText(context, sb);
		return sb.toString();
	}

	private void appendDateText(Activity context, StringBuilder sb)
	{
		if (getCurrentLevelPointType() == LevelPointType.START)
			sb.append(context.getResources().getString(R.string.input_data_res_start_time));
		else
			sb.append(context.getResources().getString(R.string.input_data_res_finish_time));
		sb.append(" ");
		sb.append(inputDate.toPrettyString());
	}

	@SuppressWarnings("unused")
	private void appendMissedCheckpointsText(Activity context, StringBuilder sb)
	{
		if (getCurrentLevelPointType() == LevelPointType.FINISH)
		{
			sb.append("\n");
			sb.append(context.getResources().getString(R.string.input_data_res_missed));
			sb.append(" ");
			sb.append(checkedState.getMissedCheckpointsText());
		}
	}

	private void appendTakenCheckpointsText(Activity context, StringBuilder sb)
	{
		if (getCurrentLevelPointType() == LevelPointType.FINISH)
		{
			sb.append("\n");
			sb.append(context.getResources().getString(R.string.input_data_res_taken));
			sb.append(" ");
			sb.append(checkedState.getTakenCheckpointsText());
		}
	}

	@Override
	protected void update(boolean fromSavedBundle)
	{
		super.update(fromSavedBundle);
		LevelPoint levelPoint = getLevelPointForTeam();
		checkedState.setLevelPoint(levelPoint);
		if (!fromSavedBundle)
		{
			TeamResultRecord previousRecord =
			    DatacollectorDB.getConnectedInstance().getExistingTeamResultRecord(levelPoint, getCurrentTeam());
			if (previousRecord != null)
			{
				checkedState.loadTakenCheckpoints(previousRecord.getCheckedMap());
				inputDate = new DateRecord(previousRecord.getCheckDateTime());
				editingExistingRecord = true;
			}
		}
	}

	public void checkAll()
	{
		checkedState.checkAll();
		fireStateChanged();
	}

	public void uncheckAll()
	{
		checkedState.uncheckAll();
		fireStateChanged();
	}

	public void saveInputDataToDB(Date recordDateTime)
	{
		DatacollectorDB.getConnectedInstance().saveTeamResult(getLevelPointForTeam(), getCurrentTeam(), inputDate.toDate(), checkedState.getTakenCheckpointsRawText(), recordDateTime);
	}

	public void putTeamLevelPointToDataStorage(Date recordDateTime)
	{
		TeamResult teamResult =
		    new TeamResult(getCurrentTeam().getTeamId(), Settings.getInstance().getUserId(), Settings.getInstance().getDeviceId(), getCurrentScanPoint().getScanPointId(), checkedState.getTakenCheckpointsRawText(), inputDate.toDate(), recordDateTime);
		teamResult.setTeam(getCurrentTeam());
		teamResult.setScanPoint(getCurrentScanPoint());
		teamResult.initTakenCheckpoints();
		DataStorage.putTeamResult(teamResult);
	}

	public boolean isEditingExistingRecord()
	{
		return editingExistingRecord;
	}

	public boolean checkDateCorrect()
	{
		LevelPoint levelPoint = getLevelPointForTeam();
		Date editedDate = inputDate.toDate();
		// Check intervals slightly shifted (-10 min before start and +10 min after finish).
		// Let server scripts decide, approve such times or not.
		Date shiftedStart = new Date(editedDate.getTime() + 10 * 60 * 1000);
		Date shiftedEnd = new Date(editedDate.getTime() - 10 * 60 * 1000);
		return shiftedStart.after(levelPoint.getLevelPointMinDateTime())
		        && shiftedEnd.before(levelPoint.getLevelPointMaxDateTime());
	}

	public String buildDateErrorMessage(InputDataActivity context)
	{
		LevelPoint levelPoint = getLevelPointForTeam();
		String message = context.getResources().getString(R.string.input_data_date_error_message);
		message = message.replace("${inputDate}", inputDate.toPrettyString());
		message =
		    message.replace("${dateFrom}", PrettyDateFormat.format(levelPoint.getLevelPointMinDateTime()));
		message =
		    message.replace("${dateTo}", PrettyDateFormat.format(levelPoint.getLevelPointMaxDateTime()));
		return message;
	}
}
