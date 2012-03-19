package ru.mmb.terminal.activity.input.data;

import java.util.Date;

import ru.mmb.terminal.R;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

public class DatePanel
{
	private final InputDataActivityState currentState;

	private final DatePicker editDate;
	private final TimePicker editTime;
	private final Button btnSetNow;

	private boolean refreshingControls = false;

	public DatePanel(InputDataActivity context, InputDataActivityState currentState)
	{
		this.currentState = currentState;

		editDate = (DatePicker) context.findViewById(R.id.inputData_datePicker);
		editTime = (TimePicker) context.findViewById(R.id.inputData_timePicker);
		btnSetNow = (Button) context.findViewById(R.id.inputData_setNowButton);

		initDate();

		btnSetNow.setOnClickListener(new SetNowClickListener());
	}

	private void initDate()
	{
		if (currentState.isCommonStart())
		{
			currentState.initInputDateFromCommonStart();
			disableControls();
		}
		initDateControls();
	}

	private void disableControls()
	{
		editDate.setEnabled(false);
		editTime.setEnabled(false);
		btnSetNow.setEnabled(false);
	}

	private void initDateControls()
	{
		editDate.init(currentState.getInputDate().getYear(), currentState.getInputDate().getMonth(), currentState.getInputDate().getDay(), new EditDateChangeListener());
		editTime.setCurrentHour(currentState.getInputDate().getHour());
		editTime.setCurrentMinute(currentState.getInputDate().getMinute());
		editTime.setIs24HourView(true);
		editTime.setOnTimeChangedListener(new EditTimeChangeListener());
	}

	private void refreshDateControls()
	{
		refreshingControls = true;
		editDate.updateDate(currentState.getInputDate().getYear(), currentState.getInputDate().getMonth(), currentState.getInputDate().getDay());
		editTime.setCurrentHour(currentState.getInputDate().getHour());
		editTime.setCurrentMinute(currentState.getInputDate().getMinute());
		refreshingControls = false;
	}

	private class SetNowClickListener implements OnClickListener
	{
		@Override
		public void onClick(View v)
		{
			currentState.setInputDate(new Date());
			refreshDateControls();
		}
	}

	private class EditTimeChangeListener implements OnTimeChangedListener
	{
		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute)
		{
			if (refreshingControls) return;
			currentState.setInputDateTimePart(hourOfDay, minute);
		}
	}

	private class EditDateChangeListener implements OnDateChangedListener
	{
		@Override
		public void onDateChanged(DatePicker view, int year, int month, int dayOfMonth)
		{
			if (refreshingControls) return;
			currentState.setInputDateDatePart(year, month, dayOfMonth);
		}
	}
}
