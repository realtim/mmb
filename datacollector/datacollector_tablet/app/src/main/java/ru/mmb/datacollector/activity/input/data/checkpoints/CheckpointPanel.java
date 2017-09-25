package ru.mmb.datacollector.activity.input.data.checkpoints;

import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.R;
import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.PointType;

import static ru.mmb.datacollector.model.PointType.CHECKPOINT;
import static ru.mmb.datacollector.model.PointType.MUST_VISIT;

class CheckpointPanel {
    private static final boolean IS_OKP = true;
    private static final boolean CHECKED = true;

    private final InputDataActivity inputDataActivity;
    private final InputDataActivityState currentState;

    private final LinearLayout checkpointsTopPanel;
    private final TableLayout checkpointsPanel;

    private final Map<Checkpoint, ToggleButton> checkpointToggles = new HashMap<>();
    private final Map<ToggleButton, Checkpoint> toggleCheckpoints = new HashMap<>();

    CheckpointPanel(InputDataActivity context, InputDataActivityState currentState) {
        this.inputDataActivity = context;
        this.currentState = currentState;

        checkpointsTopPanel =
                (LinearLayout) context.findViewById(R.id.inputData_checkpointsTopPanel);
        checkpointsPanel = (TableLayout) context.findViewById(R.id.inputData_checkpointsPanel);
        Button btnCheckAll = (Button) context.findViewById(R.id.inputData_checkAllButton);
        Button btnCheckNothing = (Button) context.findViewById(R.id.inputData_checkNothingButton);
        Button btnCheckAllOkp = (Button) context.findViewById(R.id.inputData_checkAllOkpButton);
        Button btnCheckNothingOkp = (Button) context.findViewById(R.id.inputData_checkNothingOkpButton);

        init();

        initCheckpointsState();

        btnCheckAll.setOnClickListener(new SetAllCheckedClickListener(CHECKED, !IS_OKP));
        btnCheckNothing.setOnClickListener(new SetAllCheckedClickListener(!CHECKED, !IS_OKP));
        btnCheckAllOkp.setOnClickListener(new SetAllCheckedClickListener(CHECKED, IS_OKP));
        btnCheckNothingOkp.setOnClickListener(new SetAllCheckedClickListener(!CHECKED, IS_OKP));
    }

    private void init() {
        if (!currentState.needInputCheckpoints()) {
            checkpointsTopPanel.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
        } else {
            createCheckpointsTable();
        }
    }

    private void createCheckpointsTable() {
        List<Checkpoint> checkpoints = currentState.getLevelPointForTeam().getCheckpoints();
        int checkpointsCount = checkpoints.size();
        int colCount = checkpointsCount / 2;
        if (checkpointsCount % 2 != 0) colCount += 1;

        int rowCount = 5;
        List<TableRow> rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            TableRow tableRow = new TableRow(inputDataActivity);
            rows.add(tableRow);
            checkpointsPanel.addView(tableRow);
        }

        buildCheckpointsRow(rows, 0, checkpoints, colCount);
        buildSeparatorRow(rows.get(2));
        buildCheckpointsRow(rows, 1, checkpoints, colCount);
    }

    private void buildSeparatorRow(TableRow tableRow) {
        TextView dummyText = new TextView(inputDataActivity);
        dummyText.setText(" ");
        dummyText.setMinHeight(35);
        tableRow.addView(dummyText);
    }

    private void buildCheckpointsRow(List<TableRow> rows, int rowIndex,
                                     List<Checkpoint> checkpoints, int colCount) {
        int checkIndex = (rowIndex == 0) ? 0 : 4;
        int textIndex = (rowIndex == 0) ? 1 : 3;

        for (int i = 0; i < colCount; i++) {
            int checkpointIndex = (rowIndex == 0) ? i : i + colCount;
            TableRow checkRow = rows.get(checkIndex);
            TableRow textRow = rows.get(textIndex);
            if (checkpointIndex < checkpoints.size()) {
                Checkpoint checkpoint = checkpoints.get(checkpointIndex);
                if (checkpoint.getLevelPoint().getPointType() == PointType.MUST_VISIT) {
                    addMustVisitCheckpointControls(checkpoint, checkRow, textRow);
                } else {
                    addOrdinaryCheckpointControls(checkpoint, checkRow, textRow);
                }
            } else {
                addDummyControls(checkRow, textRow);
            }
        }
    }

    private void addMustVisitCheckpointControls(Checkpoint checkpoint, TableRow checkRow, TableRow textRow) {
        addCheckpointToggle(checkpoint, checkRow, IS_OKP);
        addCheckpointNameControl(checkpoint, textRow, IS_OKP);
    }

    private void addOrdinaryCheckpointControls(Checkpoint checkpoint, TableRow checkRow, TableRow textRow) {
        addCheckpointToggle(checkpoint, checkRow);
        addCheckpointNameControl(checkpoint, textRow);
    }

    private void addDummyControls(TableRow checkRow, TableRow textRow) {
        addEmptyCell(checkRow);
        addEmptyCell(textRow);
    }

    private void addEmptyCell(TableRow tableRow) {
        TextView dummy = new TextView(inputDataActivity);
        dummy.setText("");
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
        layoutParams.weight = 1;
        dummy.setLayoutParams(layoutParams);
        tableRow.addView(dummy);
    }

    private void addCheckpointNameControl(Checkpoint checkpoint, TableRow textRow) {
        addCheckpointNameControl(checkpoint, textRow, !IS_OKP);
    }

    private void addCheckpointNameControl(Checkpoint checkpoint, TableRow tableRow, boolean isOkp) {
        TextView checkNameText = new TextView(inputDataActivity);
        checkNameText.setText(checkpoint.getCheckpointName());
        checkNameText.setTextAppearance(inputDataActivity, android.R.style.TextAppearance_Large);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
        layoutParams.weight = 1;
        checkNameText.setGravity(Gravity.CENTER_HORIZONTAL);
        checkNameText.setLayoutParams(layoutParams);
        if (isOkp) {
            checkNameText.setTextColor(inputDataActivity.getResources().getColor(R.color.Salmon));
        }
        tableRow.addView(checkNameText);
        addSeparator(tableRow);
    }

    private void addSeparator(TableRow tableRow) {
        TextView separator = new TextView(inputDataActivity);
        separator.setText("");
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
        layoutParams.width = 5;
        separator.setLayoutParams(layoutParams);
        tableRow.addView(separator);
    }

    private void addCheckpointToggle(Checkpoint checkpoint, TableRow tableRow) {
        addCheckpointToggle(checkpoint, tableRow, !IS_OKP);
    }

    private void addCheckpointToggle(Checkpoint checkpoint, TableRow tableRow, boolean isOkp) {
        ToggleButton checkpointToggle = new ToggleButton(inputDataActivity);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams();
        layoutParams.weight = 1;
        checkpointToggle.setLayoutParams(layoutParams);
        checkpointToggle.setTextOn("X");
        checkpointToggle.setTextOff(" ");
        checkpointToggle.setOnClickListener(new CheckpointToggleClickListener());
        if (isOkp) {
            checkpointToggle.setBackgroundDrawable(inputDataActivity.getResources().getDrawable(R.drawable.must_visit_toggle));
        } else {
            checkpointToggle.setBackgroundDrawable(inputDataActivity.getResources().getDrawable(R.drawable.ordinary_toggle));
        }
        tableRow.addView(checkpointToggle);
        addSeparator(tableRow);
        checkpointToggles.put(checkpoint, checkpointToggle);
        toggleCheckpoints.put(checkpointToggle, checkpoint);
    }

    private void initCheckpointsState() {
        if (!currentState.needInputCheckpoints()) return;

        List<Checkpoint> checkpoints = currentState.getLevelPointForTeam().getCheckpoints();
        for (Checkpoint checkpoint : checkpoints) {
            PointType pointType = checkpoint.getLevelPoint().getPointType();
            if (pointType == CHECKPOINT || pointType == MUST_VISIT) {
                ToggleButton toggleButton = checkpointToggles.get(checkpoint);
                toggleButton.setChecked(currentState.isChecked(checkpoint));
            }
        }
    }

    private class CheckpointToggleClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Checkpoint checkpoint = toggleCheckpoints.get(v);
            currentState.setChecked(checkpoint, ((ToggleButton) v).isChecked());
        }
    }

    private class SetAllCheckedClickListener implements OnClickListener {
        private final boolean checkedValue;
        private final boolean isOkp;

        public SetAllCheckedClickListener(boolean checkedValue, boolean isOkp) {
            this.checkedValue = checkedValue;
            this.isOkp = isOkp;
        }

        @Override
        public void onClick(View v) {
            for (Map.Entry<Checkpoint, ToggleButton> entry : checkpointToggles.entrySet()) {
                PointType pointType = entry.getKey().getLevelPoint().getPointType();
                boolean needSet = isOkp ? (pointType == MUST_VISIT) : (pointType != MUST_VISIT);
                if (needSet) {
                    entry.getValue().setChecked(checkedValue);
                }
            }
            if (checkedValue) {
                currentState.checkAll(isOkp);
            } else {
                currentState.uncheckAll(isOkp);
            }
        }
    }
}
