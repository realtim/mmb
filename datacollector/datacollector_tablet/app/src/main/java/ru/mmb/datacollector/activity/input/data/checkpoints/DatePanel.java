package ru.mmb.datacollector.activity.input.data.checkpoints;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

import java.util.Date;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.registry.Settings;

public class DatePanel {
    private static final boolean FOCUSABLE = true;
    private static final boolean NOT_FOCUSABLE = false;

    private final InputDataActivityState currentState;

    private final DatePicker datePicker;
    private final LinearLayout timePanel;
    private final Button btnEditDate;

    private boolean refreshingControls = false;

    private final TimePicker timePicker;

    public DatePanel(InputDataActivity context, InputDataActivityState currentState) {
        this.currentState = currentState;

        datePicker = (DatePicker) context.findViewById(R.id.inputData_datePicker);
        timePanel = (LinearLayout) context.findViewById(R.id.inputData_timePanel);
        timePicker = new TimePicker(context);
        timePicker.setIs24HourView(true);
        timePanel.addView(timePicker);

        hookEditTextChildren(datePicker, NOT_FOCUSABLE);
        hookEditTextChildren(timePicker, FOCUSABLE);

        btnEditDate = (Button) context.findViewById(R.id.inputData_editDateButton);
        btnEditDate.setOnClickListener(new EditDateClickListener());

        initDate();
    }

    /**
     * Time and date pickers bug in API < 10. EditText focus not lost on DONE
     * event.<br>
     * When NumberPicker editor is activated change event is generated only when
     * focus is lost.<br>
     * Now user must press DONE on keyboard, then focus is cleared and change
     * event is generated.<br>
     * Children browsing is used because of some problems with finding view IDs
     * in com.android.internal.
     */
    private void hookEditTextChildren(ViewGroup parent, boolean focusable) {
        if (parent.getVisibility() != View.VISIBLE) return;

        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                hookEditTextChildren((ViewGroup) child, focusable);
            } else if (child instanceof EditText) {
                EditText editText = (EditText) child;
                if (!focusable) {
                    editText.setFocusable(false);
                    editText.setEnabled(false);
                } else {
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    editText.setOnEditorActionListener(new NumberInputEditorActionListener());
                }
            }
        }
    }

    private void initDate() {
        if (currentState.isCommonStart()) {
            currentState.initInputDateFromCommonStart();
            btnEditDate.setVisibility(View.GONE);
        } else if (!Settings.getInstance().isCanEditScantime()) {
            btnEditDate.setVisibility(View.GONE);
        } else if (!currentState.isLoggerDataExists()) {
            Date currentDate = new Date();
            currentState.setInputDate(currentDate);
            currentState.setPrevDateTime(currentDate);
        }
        setControlsEnabled(false);
        initDateControls();
    }

    private void setControlsEnabled(boolean enabled) {
        datePicker.setEnabled(enabled);
        timePicker.setEnabled(enabled);
    }

    private void initDateControls() {
        datePicker.init(currentState.getInputDate().getYear(), currentState.getInputDate().getMonth(), currentState.getInputDate().getDay(), new DatePickerChangeListener());
        timePicker.setCurrentHour(currentState.getInputDate().getHour());
        timePicker.setCurrentMinute(currentState.getInputDate().getMinute());
        timePicker.setOnTimeChangedListener(new TimeChangedListener());
    }

    void refreshDateControls() {
        refreshingControls = true;
        datePicker.updateDate(currentState.getInputDate().getYear(), currentState.getInputDate().getMonth(), currentState.getInputDate().getDay());
        timePicker.setCurrentHour(currentState.getInputDate().getHour());
        timePicker.setCurrentMinute(currentState.getInputDate().getMinute());
        refreshingControls = false;
    }

    private class NumberInputEditorActionListener implements OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView view, int action, KeyEvent event) {
            if (action == EditorInfo.IME_ACTION_DONE) {
                view.clearFocus();
            }
            return false;
        }
    }

    private class TimeChangedListener implements OnTimeChangedListener {
        @Override
        public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
            if (refreshingControls) return;
            currentState.setInputDateTimePart(hourOfDay, minute);
        }
    }

    private class DatePickerChangeListener implements OnDateChangedListener {
        @Override
        public void onDateChanged(DatePicker view, int year, int month, int dayOfMonth) {
            if (refreshingControls) return;
            currentState.setInputDateDatePart(year, month, dayOfMonth);
        }
    }

    private class EditDateClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            setControlsEnabled(true);
            btnEditDate.setVisibility(View.GONE);
        }
    }
}
