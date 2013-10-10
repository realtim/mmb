package ru.mmb.terminal.activity.input.barcode.list;

import ru.mmb.terminal.R;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class BarCodeRow extends LinearLayout
{
	public BarCodeRow(Context context)
	{
		super(context);
		LayoutInflater.from(context).inflate(R.layout.input_barcode_row, this, true);
	}
}
