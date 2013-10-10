package ru.mmb.terminal.activity.transport.transpexport;

import static ru.mmb.terminal.activity.Constants.KEY_EXPORT_RESULT_MESSAGE;
import ru.mmb.terminal.R;
import ru.mmb.terminal.activity.ActivityStateWithTeamAndLevel;
import ru.mmb.terminal.model.registry.Settings;
import ru.mmb.terminal.transport.exporter.ExportMode;
import ru.mmb.terminal.transport.exporter.ExportState;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ExportBarCodesPanel
{
	private final TransportExportActivity activity;

	private final TextView labLevelPoint;
	private final Button btnSelectLevelPoint;
	private final TextView labLastExportDate;
	private final Button btnFullExport;
	private final Button btnIncrementalExport;

	private final Handler exportFinishHandler;

	public ExportBarCodesPanel(TransportExportActivity activity)
	{
		this.activity = activity;

		labLevelPoint =
		    (TextView) activity.findViewById(R.id.transpExportBarcode_levelPointTextView);
		btnSelectLevelPoint =
		    (Button) activity.findViewById(R.id.transpExportBarcode_selectLevelPointBtn);
		labLastExportDate =
		    (TextView) activity.findViewById(R.id.transpExportBarcode_lastExportTextView);
		btnFullExport = (Button) activity.findViewById(R.id.transpExportBarcode_fullExportBtn);
		btnIncrementalExport =
		    (Button) activity.findViewById(R.id.transpExportBarcode_incrementalExportBtn);

		btnSelectLevelPoint.setOnClickListener(new SelectLevelPointClickListener());
		btnFullExport.setOnClickListener(new FullClickListener());
		btnIncrementalExport.setOnClickListener(new IncrementalClickListener());

		exportFinishHandler = new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				if (getExportState() != null && !getExportState().isTerminated())
				{
					String resultMessage = msg.getData().getString(KEY_EXPORT_RESULT_MESSAGE);
					Toast.makeText(getActivity().getApplicationContext(), resultMessage, Toast.LENGTH_LONG).show();
				}
				setExportState(null);
				getActivity().refreshState();
			}
		};
	}

	private TransportExportActivity getActivity()
	{
		return activity;
	}

	private ExportState getExportState()
	{
		return activity.getExportState();
	}

	private void setExportState(ExportState exportState)
	{
		activity.setExportState(exportState);
	}

	private ActivityStateWithTeamAndLevel getCurrentState()
	{
		return activity.getCurrentState();
	}

	public void refreshState()
	{
		refreshLevelPointLabel();
		refreshLastExportDate();
		buttonsSetEnabled();
	}

	private void refreshLevelPointLabel()
	{
		labLevelPoint.setText(getCurrentState().getLevelPointText(activity));
	}

	private void refreshLastExportDate()
	{
		String lastExportString = "";
		if (getCurrentState().isLevelSelected())
		{
			Integer levelPointId =
			    new Integer(getCurrentState().getCurrentLevelPoint().getLevelPointId());
			lastExportString = Settings.getInstance().getBarCodeLastExportDate(levelPointId);
		}
		if ("".equals(lastExportString))
		{
			labLastExportDate.setText(activity.getResources().getText(R.string.transp_export_barcode_no_last_export));
		}
		else
		{
			labLastExportDate.setText(lastExportString);
		}
	}

	private void buttonsSetEnabled()
	{
		btnSelectLevelPoint.setEnabled(!isExportRunning());
		btnFullExport.setEnabled(isExportEnabled());
		btnIncrementalExport.setEnabled(isExportEnabled());
	}

	private boolean isExportEnabled()
	{
		return getCurrentState().isLevelSelected() && !isExportRunning();
	}

	private boolean isExportRunning()
	{
		return getExportState() != null;
	}

	private void runExport(ExportMode exportMode)
	{
		setExportState(new ExportState());
		getActivity().refreshState();

		ExportBarCodesThread thread =
		    new ExportBarCodesThread(getActivity(), exportFinishHandler, exportMode, getExportState());
		thread.start();
	}

	private class FullClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			runExport(ExportMode.FULL);
		}
	}

	private class IncrementalClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			runExport(ExportMode.INCREMENTAL);
		}
	}

	private class SelectLevelPointClickListener implements OnClickListener
	{
		@Override
		public void onClick(View arg0)
		{
			activity.selectLevelPoint();
		}
	}
}
