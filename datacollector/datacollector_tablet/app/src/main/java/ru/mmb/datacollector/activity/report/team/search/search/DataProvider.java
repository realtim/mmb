package ru.mmb.datacollector.activity.report.team.search.search;

import java.util.ArrayList;
import java.util.List;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.ScanPoint;

public class DataProvider {
    public List<TeamListRecord> getTeams(ScanPoint scanPoint) {
        List<TeamListRecord> result = new ArrayList<TeamListRecord>();
        for (RawLoggerData rawLoggerData : SQLiteDatabaseAdapter.getConnectedInstance().loadRawLoggerData(scanPoint)) {
            result.add(new TeamListRecord(rawLoggerData));
        }
        return result;
    }
}
