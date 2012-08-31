package ru.mmb.terminal.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class EditTextWithSoftKeyboardSupport extends EditText
{
	private OnEditorActionListener editorActionListener = null;

	public EditTextWithSoftKeyboardSupport(Context context)
	{
		super(context);
	}

	public EditTextWithSoftKeyboardSupport(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	public EditTextWithSoftKeyboardSupport(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event)
	{
		if (editorActionListener != null
		        && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)
		        && event.getAction() == KeyEvent.ACTION_UP)
		{
			editorActionListener.onEditorAction(this, EditorInfo.IME_ACTION_NONE, event);
			return false;
		}
		return super.dispatchKeyEvent(event);
	}

	public void setSoftKeyboardBackListener(OnEditorActionListener editorActionListener)
	{
		this.editorActionListener = editorActionListener;
	}
}
