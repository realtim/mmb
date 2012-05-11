package ru.mmb.terminal.activity;

public class Constants
{
	public static final boolean UPDATE_FROM_SAVED_BUNDLE = true;
	public static final boolean UPDATE_FOR_FIRST_LAUNCH = false;

	public static final String KEY_CURRENT_DISTANCE = "current.distance";
	public static final String KEY_CURRENT_LEVEL = "current.level";
	public static final String KEY_CURRENT_INPUT_MODE = "current.input_mode";
	public static final String KEY_CURRENT_TEAM_SORT_COLUMN = "current.team.sort_column";
	public static final String KEY_CURRENT_TEAM_SORT_ORDER = "current.team.sort_order";
	public static final String KEY_CURRENT_TEAM_FILTER_STATE = "current.team.filter_state";
	public static final String KEY_CURRENT_TEAM_FILTER_NUMBER_EXACT =
	    "current.team.filter_number_exact";
	public static final String KEY_CURRENT_TEAM_FILTER_NUMBER = "current.team.filter_number";
	public static final String KEY_CURRENT_TEAM_FILTER_TEAM = "current.team.filter_team";
	public static final String KEY_CURRENT_TEAM_FILTER_MEMBER = "current.team.filter_member";
	public static final String KEY_CURRENT_TEAM = "current.team";
	public static final String KEY_SEARCH_TEAM_ACTIVITY_MODE = "search.team.activity_mode";
	public static final String KEY_CURRENT_INPUT_CHECKPOINTS_STATE =
	    "current.input.checkpoint.state";
	public static final String KEY_CURRENT_INPUT_CHECKED_DATE = "current.input.checked.date";
	public static final String KEY_CURRENT_INPUT_WITHDRAWN_CHECKED =
	    "current.input.withdrawn.checked";
	public static final String KEY_EXPORT_RESULT_MESSAGE = "export.result.message";

	public static final int REQUEST_CODE_DEFAULT_ACTIVITY = -1;
	public static final int REQUEST_CODE_INPUT_LEVEL_ACTIVITY = 2;
	public static final int REQUEST_CODE_INPUT_TEAM_ACTIVITY = 3;
	public static final int REQUEST_CODE_SEARCH_TEAM_ACTIVITY = 4;
	public static final int REQUEST_CODE_INPUT_DATA_ACTIVITY = 5;
	public static final int REQUEST_CODE_WITHDRAW_MEMBER_ACTIVITY = 6;
	public static final int REQUEST_CODE_FILE_DIALOG_ACTIVITY = 7;
}
