package ru.mmb.terminal.notused;

public class ProgressDialog
{
	//static final int LOAD_PROGRESS_DIALOG = 1;

	// private ProgressDialog loadProgressDialog;

	/*@Override
	protected Dialog onCreateDialog(int id, Bundle args)
	{
		switch (id)
		{
			case LOAD_PROGRESS_DIALOG:
				loadProgressDialog = new ProgressDialog(this);
				loadProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				loadProgressDialog.setMessage(getResources().getString(R.string.main_progress_loading));
				loadProgressDialog.setCancelable(false);
				loadProgressDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
				{
					@Override
					public void onDismiss(DialogInterface dialog)
					{
						if (!Model.getLoadResult().isSuccess())
						{
							showToast(Model.getLoadResult().getErrorMessage());
						}
					}
				});
				return loadProgressDialog;
			case LOGIN_DIALOG:
				loginContainer =
				    new LoginDialogContainer(this, new OnLoginCancelListener(), new OnLoginSuccessListener());
				return loginContainer.getDialog();
			default:
				return null;
		}
	}*/

	/*@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		switch (id)
		{
			case LOGIN_DIALOG:
				loginContainer.refreshState();
				break;
			default:
				super.onPrepareDialog(id, dialog);
		}
	}*/
}
