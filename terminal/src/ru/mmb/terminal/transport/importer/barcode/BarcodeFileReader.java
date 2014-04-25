package ru.mmb.terminal.transport.importer.barcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.ScanPoint;
import ru.mmb.terminal.model.Team;
import ru.mmb.terminal.model.registry.ScanPointsRegistry;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.model.registry.TeamsRegistry;
import ru.mmb.terminal.transport.importer.ImportState;
import ru.mmb.terminal.transport.importer.Importer;
import ru.mmb.terminal.transport.model.datatype.DataTypes;

public class BarcodeFileReader
{
	private static final SimpleDateFormat BARCODE_DATE_FORMAT =
	    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private final String fileName;
	private final int scanPointId;
	private final ImportState importState;

	private int recordsAdded = 0;

	public BarcodeFileReader(String fileName, int scanPointId, ImportState importState)
	{
		this.fileName = fileName;
		this.scanPointId = scanPointId;
		this.importState = importState;
	}

	public JSONObject readBarCodeScanData() throws Exception
	{
		BufferedReader reader =
		    new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "US-ASCII"));
		try
		{
			JSONArray records = new JSONArray();
			String inputLine = reader.readLine();
			while (inputLine != null)
			{
				//Log.d("ImportBarcodes", "inputLine: " + inputLine);
				if (inputLine.trim().length() > 0)
				{
					addLineToJSONArray(inputLine, records);
				}
				inputLine = reader.readLine();
			}
			JSONObject mainContainer = new JSONObject();
			mainContainer.put(Importer.TABLE_BAR_CODE_SCANS, records);
			importState.appendMessage("Parsed " + recordsAdded
			        + " records for selected scan point.");
			return mainContainer;
		}
		finally
		{
			reader.close();
		}
	}

	private void addLineToJSONArray(String inputLine, JSONArray records) throws JSONException
	{
		try
		{
			ParsedBarcode parsedBarcode = parseInputLine(inputLine);
			//Log.d("ImportBarcodes", parsedBarcode.toString());

			// Ignore lines from other scan point.
			if (parsedBarcode.scanPointId != scanPointId) return;
			if (!parsedBarcode.prepareTeamAndLevelPoint())
			{
				if (parsedBarcode.errorMessage != null)
				{
					importState.appendMessage(parsedBarcode.errorMessage);
				}
			}
			JSONObject recordObject = parsedBarcode.convertToJSON();
			if (recordObject != null)
			{
				records.put(recordObject);
				recordsAdded++;
			}
		}
		catch (ParseException e)
		{
			importState.appendMessage(e.getMessage());
		}
	}

	private ParsedBarcode parseInputLine(String inputLine) throws ParseException
	{
		ParsedBarcode result = new ParsedBarcode();
		String[] inputParts = inputLine.split(",");
		result.scanPointId = Integer.parseInt(inputParts[0].trim());
		result.teamNumber = Integer.parseInt(inputParts[1].trim().substring(2, 6));
		String dateString = inputParts[3].trim() + " " + inputParts[2].trim();
		result.barcodeScanDate = BARCODE_DATE_FORMAT.parse(dateString);
		return result;
	}

	private class ParsedBarcode
	{
		private int scanPointId;
		private int teamNumber;
		private Date barcodeScanDate;
		private String errorMessage = null;

		private ScanPoint scanPoint = null;
		private Team team = null;
		private LevelPoint levelPoint = null;

		private JSONObject convertToJSON() throws JSONException
		{
			if (team == null || levelPoint == null) return null;

			JSONObject result = new JSONObject();
			result.put("levelpoint_id", Integer.toString(levelPoint.getLevelPointId()));
			result.put("team_id", Integer.toString(team.getTeamId()));
			result.put("device_id", Integer.toString(Settings.getInstance().getDeviceId()));
			String dateString = DataTypes.LONG_DATE_DATA_TYPE.encodeString(barcodeScanDate);
			//Log.d("ImportBarcodes", "dateToJSON: " + dateString);
			result.put("barcodescan_date", dateString);
			result.put("teamlevelpoint_datetime", dateString);
			return result;
		}

		public boolean prepareTeamAndLevelPoint()
		{
			scanPoint = ScanPointsRegistry.getInstance().getScanPointById(scanPointId);
			if (scanPoint == null)
			{
				errorMessage = "Scan point not found by ID: " + scanPointId;
				return false;
			}
			team = TeamsRegistry.getInstance().getTeamByNumber(teamNumber);
			if (team == null)
			{
				errorMessage = "Team not found by number: " + teamNumber;
				return false;
			}
			levelPoint = scanPoint.getLevelPointByDistance(team.getDistanceId());
			if (levelPoint == null)
			{
				errorMessage =
				    "Level point not found by scan point [" + scanPoint.getScanPointName()
				            + "] and team [" + teamNumber + "].";
				return false;
			}
			return true;
		}

		@Override
		public String toString()
		{
			return "ParsedBarcode [scanPointId=" + scanPointId + ", teamNumber=" + teamNumber
			        + ", barcodeScanDate=" + barcodeScanDate + "]";
		}
	}
}
