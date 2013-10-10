package ru.mmb.terminal.activity.input.barcode;

import java.util.Date;

import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.ActivityStateWithTeamAndLevel;
import ru.mmb.terminal.activity.input.barcode.list.BarCodeAdapter;
import ru.mmb.terminal.activity.input.barcode.list.BarCodeFilter;
import ru.mmb.terminal.activity.input.barcode.list.DataProvider;
import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.model.BarCodeScan;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.barcode.DataStorage;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import android.app.Activity;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

public class BarCodeActivity extends Activity
{
	private ActivityStateWithTeamAndLevel currentState;

	private BarCodeAdapter barCodeAdapter;
	private DataStorage dataStorage;

	private EditText editBarCode;
	private TextView labScansCount;
	private ListView lvScans;

	private Team team;
	private LevelPoint levelPoint;
	private Date currentTime;
	private boolean enterJustPressed = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Settings.getInstance().setCurrentContext(this);

		currentState = new ActivityStateWithTeamAndLevel("input.barcode");
		currentState.initialize(this, savedInstanceState);

		setContentView(R.layout.input_barcode);

		editBarCode = (EditText) findViewById(R.id.inputBarCode_barCodeEdit);
		editBarCode.setText("");
		editBarCode.setOnKeyListener(new BarCodeChangeListener());
		// Never show software keyboard when working with barcode editor.
		editBarCode.setInputType(InputType.TYPE_NULL);

		labScansCount = (TextView) findViewById(R.id.inputBarCode_scansCountTextView);

		dataStorage = DataStorage.getInstance(currentState.getCurrentLevelPoint());

		lvScans = (ListView) findViewById(R.id.inputBarCode_scansList);
		barCodeAdapter = new BarCodeAdapter(this, R.layout.input_barcode_row);
		lvScans.setAdapter(barCodeAdapter);

		setTitle(currentState.getLevelPointText(this));

		refreshState();
	}

	private void refreshState()
	{
		((BarCodeFilter) barCodeAdapter.getFilter()).reset(DataProvider.getBarCodeScanRecords(dataStorage));
		barCodeAdapter.getFilter().filter("");

		String scansCountText =
		    String.format(getResources().getString(R.string.input_barcode_scans_count), dataStorage.size());
		labScansCount.setText(scansCountText);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		currentState.save(outState);
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		currentState.saveToSharedPreferences(getPreferences(MODE_PRIVATE));
	}

	public void processScannedBarCode(String scannedBarCode)
	{
		currentTime = new Date();
		team = getBarCodeTeam(scannedBarCode);
		levelPoint = currentState.getCurrentLevelPoint();

		saveDataToDB();
		putDataToStorage();
		refreshState();
	}

	private Team getBarCodeTeam(String scannedBarCode)
	{
		int teamNumber = extractTeamNumber(scannedBarCode);
		return TeamsRegistry.getInstance().getTeamByNumber(currentState.getCurrentDistance().getDistanceId(), teamNumber);
	}

	private int extractTeamNumber(String scannedBarCode)
	{
		String teamNumberString = scannedBarCode.substring(0, 4);
		return Integer.parseInt(teamNumberString);
	}

	private void saveDataToDB()
	{
		TerminalDB.getConnectedInstance().saveBarCodeScan(levelPoint, team, currentTime, currentTime);
	}

	private void putDataToStorage()
	{
		BarCodeScan barCodeScan =
		    new BarCodeScan(team.getTeamId(), Settings.getInstance().getDeviceId(), levelPoint.getLevelPointId(), currentTime, currentTime);
		barCodeScan.setTeam(team);
		barCodeScan.setLevelPoint(levelPoint);
		DataStorage.putBarCodeScan(barCodeScan);
	}

	private class BarCodeChangeListener implements OnKeyListener
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event)
		{
			// Check ENTER pressed only on KEY_UP.
			if (event.getAction() == KeyEvent.ACTION_UP)
			{
				if (keyCode == KeyEvent.KEYCODE_ENTER)
				{
					String scannedBarCode = editBarCode.getText().toString();
					processScannedBarCode(scannedBarCode);
					// Leave text in editor after scan.
					enterJustPressed = true;
					return true;
				}
			}
			// Check ordinary key pressed before any processing. On KEY_DOWN.
			if (event.getAction() == KeyEvent.ACTION_DOWN)
			{
				// If new barcode is scanned now, clear previous text.
				if (keyCode != KeyEvent.KEYCODE_ENTER && enterJustPressed)
				{
					editBarCode.setText("");
					enterJustPressed = false;
				}
			}
			return false;
		}
	}
}
