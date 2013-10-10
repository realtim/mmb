package ru.mmb.terminal.activity.settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.util.JSONUtils;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class DeviceJsonImporter
{
	private static final String FIELD_DEVICES = "Devices";

	private static final String FIELD_DEVICE_ID = "device_id";
	private static final String FIELD_DEVICE_NAME = "device_name";
	private static final String FIELD_USER_ID = "user_id";
	private static final String FIELD_USER_PASSWORD = "user_password";

	private final SettingsActivity settingsActivity;
	private JSONArray deviceRecords;

	public DeviceJsonImporter(SettingsActivity settingsActivity)
	{
		this.settingsActivity = settingsActivity;
	}

	public boolean prepareJsonObjects(String deviceJsonName)
	{
		try
		{
			String jsonString = JSONUtils.readFromFile(deviceJsonName, 8192);
			JSONObject packageObject = new JSONObject(jsonString);
			deviceRecords = packageObject.getJSONArray(FIELD_DEVICES);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public void showImportDialog()
	{
		try
		{
			if (deviceRecords.length() >= 1)
			{
				CharSequence[] deviceNames = getDeviceNames();
				DialogFragment dialog = new DevicesAlertDialogFragment(this, deviceNames);
				dialog.show(settingsActivity.getSupportFragmentManager(), "settings_device_dialog");
			}
		}
		catch (Exception e)
		{
			// do nothing
		}
	}

	private CharSequence[] getDeviceNames() throws JSONException
	{
		CharSequence[] result = new CharSequence[deviceRecords.length()];
		for (int i = 0; i < deviceRecords.length(); i++)
		{
			JSONObject deviceObject = deviceRecords.getJSONObject(i);
			String deviceName = deviceObject.getString(FIELD_DEVICE_NAME);
			result[i] = deviceName;
		}
		return result;
	}

	public void importDeviceRecord(int deviceRecordIndex)
	{
		try
		{
			JSONObject deviceObject = deviceRecords.getJSONObject(deviceRecordIndex);
			Settings.getInstance().setDeviceId(deviceObject.getString(FIELD_DEVICE_ID));
			Settings.getInstance().setUserId(deviceObject.getString(FIELD_USER_ID));
			Settings.getInstance().setTranspUserId(deviceObject.getString(FIELD_USER_ID));
			Settings.getInstance().setTranspUserPassword(deviceObject.getString(FIELD_USER_PASSWORD));
		}
		catch (Exception e)
		{
			// do nothing
		}
	}

	private class DevicesAlertDialogFragment extends DialogFragment
	{
		private final DeviceJsonImporter creator;
		private final CharSequence[] items;

		public DevicesAlertDialogFragment(DeviceJsonImporter creator, CharSequence[] items)
		{
			super();
			this.creator = creator;
			this.items = items;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.settings_json_dialog_title);
			builder.setItems(items, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					creator.importDeviceRecord(which);
					((SettingsActivity) getActivity()).refreshState();
				}
			});
			return builder.create();
		}
	}
}
