package ru.mmb.datacollector.transport.exporter.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.Checkpoint;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.RawLoggerData;
import ru.mmb.datacollector.model.RawTeamLevelDismiss;
import ru.mmb.datacollector.model.RawTeamLevelPoints;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.transport.exporter.ExportState;
import ru.mmb.datacollector.util.TransportDateFormat;

public class ExportDataMethodTxtToSite implements ExportDataMethod {
    private final ExportState exportState;
    private final BufferedWriter writer;

    public ExportDataMethodTxtToSite(ExportState exportState, BufferedWriter writer) {
        this.exportState = exportState;
        this.writer = writer;
    }

    @Override
    public void exportData() throws Exception {
        writeHeader();
        exportDismissTable();
        exportLevelPointsTable();
        writeFooter();
    }

    private void writeHeader() throws IOException {
        writer.write(Integer.toString(Settings.getInstance().getTranspUserId()));
        writer.newLine();
        writer.write(Settings.getInstance().getTranspUserPassword());
        writer.newLine();
    }

    private void exportDismissTable() throws Exception {
        if (exportState.isTerminated()) return;
        writer.write("---TeamLevelDismiss");
        writer.newLine();
        List<RawTeamLevelDismiss> records = SQLiteDatabaseAdapter.getConnectedInstance().loadAllDismissedMembersForDevice();
        for (RawTeamLevelDismiss record : records) {
            exportDismissRecord(record);
        }
        writer.flush();
    }

    private void exportDismissRecord(RawTeamLevelDismiss record) throws Exception {
        StringBuilder sb = new StringBuilder();
        // userId
        sb.append("\"").append(record.getUserId()).append("\";");
        // levelPointId
        LevelPoint levelPoint = record.getScanPoint().getLevelPointByTeam(record.getTeam());
        sb.append("\"").append(levelPoint.getLevelPointId()).append("\";");
        // teamId
        sb.append("\"").append(record.getTeamId()).append("\";");
        // teamUserId
        sb.append("\"").append(record.getTeamUserId()).append("\";");
        // recordDateTime
        String formattedDate = TransportDateFormat.formatLong(record.getRecordDateTime());
        sb.append("\"").append(formattedDate).append("\";");
        // deviceId
        sb.append("\"").append(record.getDeviceId()).append("\"");
        writer.write(sb.toString());
        writer.newLine();
    }

    private void exportLevelPointsTable() throws Exception {
        if (exportState.isTerminated()) return;
        writer.write("---TeamLevelPoints");
        writer.newLine();
        List<RawLoggerData> loggerData = SQLiteDatabaseAdapter.getConnectedInstance().loadAllRawLoggerDataForDevice();
        for (RawLoggerData record : loggerData) {
            exportLoggerDataRecord(record);
        }
        List<RawTeamLevelPoints> levelPoints = SQLiteDatabaseAdapter.getConnectedInstance().loadAllLevelPointsForDevice();
        for (RawTeamLevelPoints record : levelPoints) {
            exportLevelPointsRecord(record);
        }
        writer.flush();
    }

    private void exportLoggerDataRecord(RawLoggerData record) throws Exception {
        StringBuilder sb = new StringBuilder();
        // userId
        sb.append("\"").append(record.getUserId()).append("\";");
        // levelPointId
        LevelPoint levelPoint = record.getScanPoint().getLevelPointByTeam(record.getTeam());
        sb.append("\"").append(levelPoint.getLevelPointId()).append("\";");
        // teamId
        sb.append("\"").append(record.getTeamId()).append("\";");
        // recordDateTime
        String formattedDate = TransportDateFormat.formatLong(record.getRecordDateTime());
        sb.append("\"").append(formattedDate).append("\";");
        // deviceId
        sb.append("\"").append(record.getDeviceId()).append("\";");
        // teamlevelpoint_datetime
        formattedDate = TransportDateFormat.formatLong(record.getScannedDateTime());
        sb.append("\"").append(formattedDate).append("\"");
        writer.write(sb.toString());
        writer.newLine();
    }

    private void exportLevelPointsRecord(RawTeamLevelPoints record) throws Exception {
        for (Checkpoint checkpoint : record.getTakenCheckpoints()) {
            exportTakenCheckpoint(checkpoint, record);
        }
    }

    private void exportTakenCheckpoint(Checkpoint checkpoint, RawTeamLevelPoints record) throws Exception {
        StringBuilder sb = new StringBuilder();
        // userId
        sb.append("\"").append(record.getUserId()).append("\";");
        // levelPointId
        LevelPoint levelPoint = checkpoint.getLevelPoint();
        sb.append("\"").append(levelPoint.getLevelPointId()).append("\";");
        // teamId
        sb.append("\"").append(record.getTeamId()).append("\";");
        // recordDateTime
        String formattedDate = TransportDateFormat.formatLong(record.getRecordDateTime());
        sb.append("\"").append(formattedDate).append("\";");
        // deviceId
        sb.append("\"").append(record.getDeviceId()).append("\";");
        // teamlevelpoint_datetime set NULL
        sb.append("\"NULL\"");
        writer.write(sb.toString());
        writer.newLine();
    }

    private void writeFooter() throws IOException {
        writer.write("end");
        writer.newLine();
    }
}
