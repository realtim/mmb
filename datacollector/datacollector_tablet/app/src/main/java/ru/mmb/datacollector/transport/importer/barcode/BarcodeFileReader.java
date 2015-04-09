package ru.mmb.datacollector.transport.importer.barcode;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;
import ru.mmb.datacollector.transport.importer.ImportState;
import ru.mmb.datacollector.transport.importer.Importer;
import ru.mmb.datacollector.transport.model.datatype.DataTypes;

public class BarcodeFileReader
{
	private static final SimpleDateFormat BARCODE_DATE_FORMAT =
	    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	private final String fileName;
	private final int scanPointOrder;
	private final ImportState importState;

	private int recordsAdded = 0;

	public BarcodeFileReader(String fileName, ScanPoint scanPoint, ImportState importState)
	{
		this.fileName = fileName;
		this.scanPointOrder = scanPoint.getScanPointOrder();
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
			if (parsedBarcode.scanPointOrder != scanPointOrder) return;
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
		catch (Exception e)
		{
			importState.appendMessage("PARSE ERROR: " + e.getMessage());
		}
	}

	private ParsedBarcode parseInputLine(String inputLine) throws ParseException
	{
		ParsedBarcode result = new ParsedBarcode();
		String[] inputParts = inputLine.split(",");
		// New field added: LOGGER_ID
		// String loggerId = inputParts[0].trim();
		result.scanPointOrder = Integer.parseInt(inputParts[1].trim());
		result.teamNumber = Integer.parseInt(inputParts[2].trim().substring(2, 6));
		String dateString = inputParts[4].trim() + " " + inputParts[3].trim();
		result.barcodeScanDate = BARCODE_DATE_FORMAT.parse(dateString);
		return result;
	}

	private class ParsedBarcode
	{
		private int scanPointOrder;
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
			scanPoint = ScanPointsRegistry.getInstance().getScanPointByOrder(scanPointOrder);
			if (scanPoint == null)
			{
				errorMessage = "Scan point not found by order: " + scanPointOrder;
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
			return "ParsedBarcode [scanPointOrder=" + scanPointOrder + ", teamNumber=" + teamNumber
			        + ", barcodeScanDate=" + barcodeScanDate + ", errorMessage=" + errorMessage
			        + "]";
		}
	}
}
