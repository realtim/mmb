package ru.mmb.datacollector.activity.transport.transpimport;

import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_FILE_DIALOG;
import static ru.mmb.datacollector.activity.Constants.REQUEST_CODE_SCAN_POINT_ACTIVITY;
import ru.mmb.datacollector.R;
import ru.mmb.datacollector.activity.scanpoint.SelectScanPointActivity;
import ru.mmb.datacollector.model.registry.Settings;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.filedialog.FileDialog;
import com.filedialog.SelectionMode;

public class TransportImportBarcodeActivity extends TransportImportActivity
{
	private Button btnSelectScanPoint;
	private TextView labScanPointName;

	@Override
	protected int getFormLayoutResourceId()
	{
		return R.layout.transp_import_barcode;
	}

	@Override
	protected String getCurrentStatePrefix()
	{
		return "transport.import.barcode";
	}

	@Override
	protected void initVisualElementVariables()
	{
		super.initVisualElementVariables();

		btnSelectScanPoint = (Button) findViewById(R.id.transpImport_barcodeSelectScanPoint);
		labScanPointName = (TextView) findViewById(R.id.transpImport_barcodeScanPointName);

		btnSelectFile.setOnClickListener(new SelectFileClickListener());
		btnSelectScanPoint.setOnClickListener(new SelectScanPointClickListener());
	}

	@Override
	protected void setTitle()
	{
		setTitle(getResources().getString(R.string.transp_import_barcode_title));
	}

	@Override
	protected void refreshAll()
	{
		refreshScanPointName();
		super.refreshAll();
	}

	@Override
	protected void refreshState()
	{
		super.refreshState();
		btnSelectScanPoint.setEnabled(!isImportRunning());
	}

	@Override
	protected boolean isImportPossible()
	{
		return super.isImportPossible() && currentState.getCurrentScanPoint() != null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		switch (requestCode)
		{
			case REQUEST_CODE_SCAN_POINT_ACTIVITY:
				if (resultCode == RESULT_OK)
				{
					currentState.loadFromIntent(data);
					refreshScanPointName();
					refreshState();
				}
				break;
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void refreshScanPointName()
	{
		if (currentState.getCurrentScanPoint() != null)
		{
			labScanPointName.setText(currentState.getCurrentScanPoint().getScanPointName());
		}
		else
		{
			labScanPointName.setText(getResources().getString(R.string.transp_import_barcode_no_scan_point));
		}
	}

	private class SelectFileClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getBaseContext(), FileDialog.class);
			intent.putExtra(FileDialog.START_PATH, Settings.getInstance().getImportDir());
			intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
			intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { ".TXT" });
			startActivityForResult(intent, REQUEST_CODE_FILE_DIALOG);
		}
	}

	private class SelectScanPointClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Intent intent = new Intent(getApplicationContext(), SelectScanPointActivity.class);
			currentState.prepareStartActivityIntent(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
			startActivityForResult(intent, REQUEST_CODE_SCAN_POINT_ACTIVITY);
		}
	}
}
