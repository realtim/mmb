package ru.mmb.terminal.activity.input.data;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.R;
import ru.mmb.terminal.model.Checkpoint;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class CheckpointPanel
{
	private final InputDataActivity inputDataActivity;
	private final InputDataActivityState currentState;

	private final LinearLayout checkpointsTopPanel;
	private final TableLayout checkpointsPanel;
	private final Button btnCheckAll;
	private final Button btnCheckNothing;

	private final Map<Checkpoint, CheckBox> checkpointBoxes = new HashMap<Checkpoint, CheckBox>();
	private final Map<CheckBox, Checkpoint> boxCheckpoints = new HashMap<CheckBox, Checkpoint>();

	public CheckpointPanel(InputDataActivity context, InputDataActivityState currentState)
	{
		this.inputDataActivity = context;
		this.currentState = currentState;

		checkpointsTopPanel =
		    (LinearLayout) context.findViewById(R.id.inputData_checkpointsTopPanel);
		checkpointsPanel = (TableLayout) context.findViewById(R.id.inputData_checkpointsPanel);
		btnCheckAll = (Button) context.findViewById(R.id.inputData_checkAllButton);
		btnCheckNothing = (Button) context.findViewById(R.id.inputData_checkNothingButton);

		init();

		initCheckboxesState();

		btnCheckAll.setOnClickListener(new CheckAllClickListener());
		btnCheckNothing.setOnClickListener(new CheckNothingClickListener());
	}

	private void init()
	{
		if (!currentState.needInputCheckpoints())
		{
			checkpointsTopPanel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
		}
		else
		{
			createCheckpointBoxes();
		}
	}

	private void createCheckpointBoxes()
	{
		int colCount = 5;
		if (inputDataActivity.getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
		{
			colCount = 7;
		}

		List<TableRow> rows = new ArrayList<TableRow>();

		List<Checkpoint> checkpoints = currentState.getCurrentLap().getCheckpoints();
		int rowCount = (int) Math.round(Math.ceil(((double) checkpoints.size()) / colCount));

		for (int i = 0; i < rowCount; i++)
		{
			TableRow tableRow = new TableRow(inputDataActivity);
			rows.add(tableRow);
			checkpointsPanel.addView(tableRow);
		}

		for (int i = 0; i < checkpoints.size(); i++)
		{
			int rowIndex = i / colCount;
			TableRow tableRow = rows.get(rowIndex);
			CheckBox checkpointBox = new CheckBox(inputDataActivity);
			TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
			layoutParams.weight = 1;
			checkpointBox.setLayoutParams(layoutParams);
			checkpointBox.setText(checkpoints.get(i).getName());
			checkpointBox.setChecked(false);
			checkpointBox.setOnClickListener(new CheckpointBoxClickListener());
			tableRow.addView(checkpointBox);
			checkpointBoxes.put(checkpoints.get(i), checkpointBox);
			boxCheckpoints.put(checkpointBox, checkpoints.get(i));

			if (i == checkpoints.size() - 1)
			{
				if (checkpoints.size() % colCount != 0)
				{
					addDummyControls(tableRow, colCount, checkpoints.size());
				}
			}
		}
	}

	private void addDummyControls(TableRow tableRow, int colCount, int checkpointsSize)
	{
		int countToAdd = colCount - (checkpointsSize % colCount);
		for (int i = 0; i < countToAdd; i++)
		{
			TextView dummy = new TextView(inputDataActivity);
			dummy.setText("");
			TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
			layoutParams.weight = 1;
			dummy.setLayoutParams(layoutParams);
			tableRow.addView(dummy);
		}
	}

	private void initCheckboxesState()
	{
		if (!currentState.needInputCheckpoints()) return;

		List<Checkpoint> checkpoints = currentState.getCurrentLap().getCheckpoints();
		for (Checkpoint checkpoint : checkpoints)
		{
			CheckBox checkBox = checkpointBoxes.get(checkpoint);
			checkBox.setChecked(currentState.isChecked(checkpoint));
		}
	}

	private class CheckpointBoxClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			Checkpoint checkpoint = boxCheckpoints.get(v);
			currentState.setChecked(checkpoint, ((CheckBox) v).isChecked());
		}
	}

	private class CheckAllClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			for (Map.Entry<Checkpoint, CheckBox> entry : checkpointBoxes.entrySet())
			{
				entry.getValue().setChecked(true);
			}
			currentState.checkAll();
		}
	}

	private class CheckNothingClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			for (Map.Entry<Checkpoint, CheckBox> entry : checkpointBoxes.entrySet())
			{
				entry.getValue().setChecked(false);
			}
			currentState.uncheckAll();
		}
	}
}
