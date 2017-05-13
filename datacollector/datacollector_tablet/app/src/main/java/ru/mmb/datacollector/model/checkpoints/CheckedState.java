package ru.mmb.datacollector.model.checkpoints;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;

public class CheckedState implements Serializable {
    private static final long serialVersionUID = -1671058824332140814L;

    private final Map<Integer, Boolean> checkedMap = new LinkedHashMap<>();
    private LevelPoint levelPoint;

    public CheckedState() {
    }

    public void setLevelPoint(LevelPoint levelPoint) {
        if (levelPoint != null) {
            if (this.levelPoint == null || this.levelPoint.getLevelPointId() != levelPoint.getLevelPointId())
                rebuildCheckedMap(levelPoint);
        } else {
            checkedMap.clear();
        }
        this.levelPoint = levelPoint;
    }

    private void rebuildCheckedMap(LevelPoint levelPoint) {
        checkedMap.clear();
        for (Checkpoint checkpoint : levelPoint.getCheckpoints()) {
            checkedMap.put(checkpoint.getCheckpointOrder(), false);
        }
    }

    public LevelPoint getLevelPoint() {
        return levelPoint;
    }

    public void setChecked(int orderNum, boolean checked) {
        checkedMap.put(orderNum, checked);
    }

    public boolean isChecked(int orderNum) {
        if (checkedMap.containsKey(orderNum)) {
            return checkedMap.get(orderNum);
        }
        return false;
    }

    public String getTakenCheckpointsText() {
        return getCheckpointsText(getCheckedList());
    }

    public String getTakenCheckpointsRawText() {
        StringBuilder sb = new StringBuilder();
        for (Integer checkpointOrderNum : getCheckedList()) {
            sb.append(levelPoint.getCheckpointByOrderNum(checkpointOrderNum).getCheckpointName());
            sb.append(",");
        }
        return (sb.length() == 0) ? "" : sb.toString().substring(0, sb.length() - 1);
    }

    public String getMissedCheckpointsText() {
        return getCheckpointsText(getNotCheckedList());
    }

    private String getCheckpointsText(List<Integer> checkpoints) {
        StringBuilder sb = new StringBuilder();
        List<Interval> intervals = new BuildIntervalsMethod(checkpoints).execute();
        for (Interval interval : intervals) {
            sb.append(levelPoint.getCheckpointByOrderNum(interval.getBeginNum()).getCheckpointName());
            if (!interval.isSingleElement())
                sb.append("-").append(levelPoint.getCheckpointByOrderNum(interval.getEndNum()).getCheckpointName());
            sb.append(",");
        }
        //Log.d("missed checkpoints", sb.toString());
        return (sb.length() == 0) ? "-" : sb.toString().substring(0, sb.length() - 1);
    }

    private List<Integer> getCheckedList() {
        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Integer, Boolean> checkedEntry : checkedMap.entrySet()) {
            if (checkedEntry.getValue() == true) {
                result.add(checkedEntry.getKey());
            }
        }
        return result;
    }

    private List<Integer> getNotCheckedList() {
        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Integer, Boolean> checkedEntry : checkedMap.entrySet()) {
            if (checkedEntry.getValue() == false) {
                result.add(checkedEntry.getKey());
            }
        }
        return result;
    }

    public void checkAll() {
        setAllChecked(true);
    }

    public void uncheckAll() {
        setAllChecked(false);
    }

    private void setAllChecked(boolean value) {
        for (Integer orderNum : checkedMap.keySet()) {
            checkedMap.put(orderNum, value);
        }
    }

    public void loadTakenCheckpoints(Map<Integer, Boolean> checkedMap) {
        for (Integer checkpointOrder : checkedMap.keySet()) {
            setChecked(checkpointOrder, checkedMap.get(checkpointOrder));
        }
    }

    public void loadTakenCheckpoints(List<Checkpoint> takenCheckpoints) {
        for (Checkpoint checkpoint : takenCheckpoints) {
            setChecked(checkpoint.getCheckpointOrder(), true);
        }
    }
}
