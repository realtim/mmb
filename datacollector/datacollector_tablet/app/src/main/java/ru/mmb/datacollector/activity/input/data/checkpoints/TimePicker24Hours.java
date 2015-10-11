package ru.mmb.datacollector.activity.input.data.checkpoints;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TimePicker;

public class TimePicker24Hours extends TimePicker {

    public TimePicker24Hours(Context context) {
        super(context);
        init();
    }

    public TimePicker24Hours(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimePicker24Hours(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setIs24HourView(true);
    }
}
