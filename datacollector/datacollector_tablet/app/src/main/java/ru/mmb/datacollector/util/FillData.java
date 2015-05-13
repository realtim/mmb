package ru.mmb.datacollector.util;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.Participant;
import ru.mmb.datacollector.model.ScanPoint;
import ru.mmb.datacollector.model.Team;
import ru.mmb.datacollector.model.registry.DistancesRegistry;
import ru.mmb.datacollector.model.registry.ScanPointsRegistry;
import ru.mmb.datacollector.model.registry.Settings;
import ru.mmb.datacollector.model.registry.TeamsRegistry;

public class FillData {
    private static List<ScanPoint> scanPoints;

    private static BufferedWriter writer = null;
    private static int lineNumber = 0;

    private static String[] scanPointDateTimes = {"00:00:00, 2015/04/27", "15:00:00, 2015/04/27", "23:00:00, 2015/04/27", "10:00:00, 2015/04/28", "20:00:00, 2015/05/17"};

    public static void execute() {
        try {
            generateDatalogFile();
            // fillRawTables();
        } catch (Exception e) {
            Log.e("FILL_DATA", "error", e);
        }
    }

    private static void generateDatalogFile() {
        try {
            File datalogFile = new File(
                    Settings.getInstance().getMMBPathFromDBFile() + "/DATALOG.TXT");
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(datalogFile, false), "US-ASCII"));
            fillDatalogFile();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            Log.e("FILL_DATA", "error", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e("FILL_DATA", "writer close FAIL", e);
                }
            }
        }
    }

    private static void fillDatalogFile() throws IOException {
        List<Distance> distances = DistancesRegistry.getInstance().getDistances();
        scanPoints = ScanPointsRegistry.getInstance().getScanPoints();
        for (Distance distance : distances) {
            generateDatalogLines(distance);
        }
    }

    private static void generateDatalogLines(Distance distance) throws IOException {
        String loggerId = "06";
        List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());
        int currentIndex = 0;
        for (ScanPoint scanPoint : scanPoints) {
            LevelPoint levelPoint = scanPoint.getLevelPointByDistance(distance.getDistanceId());
            String scanPointOrder = "0" + scanPoint.getScanPointOrder();
            if (levelPoint.getPointType().isStart()) {
                Date startDate = levelPoint.getLevelPointMinDateTime();
                writeLinesForAllTeams(loggerId, scanPointOrder, scanPointDateTimes[currentIndex], teams);
            } else if (levelPoint.getPointType().isFinish()) {
                Date finishDate = levelPoint.getLevelPointMaxDateTime();
                writeLinesForAllTeams(loggerId, scanPointOrder, scanPointDateTimes[currentIndex], teams);
            }
            currentIndex++;
        }
    }

    private static void writeLinesForAllTeams(String loggerId, String scanPointOrder, String dateString, List<Team> teams) throws IOException {
        for (Team team : teams) {
            int teamNum = team.getTeamNum();
            String teamInfo = "88" + String.format("%04d", teamNum) + "88";
            String dataLine =
                    loggerId + ", " + scanPointOrder + ", " + teamInfo + ", " + dateString;
            writer.write(dataLine);
            writer.write("\\r\\n");
            lineNumber++;
        }
    }

    private static int crc8(String stringData) {
        byte crc = 0x00;
        for (int i = 0; i < stringData.length(); i++) {
            byte extract = (byte) stringData.charAt(i);
            for (byte tempI = 8; tempI > 0; tempI--) {
                byte sum = (byte) ((crc & 0xFF) ^ (extract & 0xFF));
                sum = (byte) ((sum & 0xFF) & 0x01);
                crc = (byte) ((crc & 0xFF) >>> 1);
                if (sum != 0) {
                    crc = (byte) ((crc & 0xFF) ^ 0x8C);
                }
                extract = (byte) ((extract & 0xFF) >>> 1);
            }
        }
        return (int) (crc & 0xFF);
    }

    private static void fillRawTables() throws ParseException {
        List<Distance> distances = DistancesRegistry.getInstance().getDistances();
        scanPoints = ScanPointsRegistry.getInstance().getScanPoints();
        for (Distance distance : distances) {
            generatePoints(distance);
            generateDismiss(distance);
        }
    }

    private static void generatePoints(Distance distance) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss, yyyy/MM/dd");
        List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());
        int currentIndex = 0;
        for (ScanPoint scanPoint : scanPoints) {
            LevelPoint levelPoint = scanPoint.getLevelPointByDistance(distance.getDistanceId());
            if (levelPoint.getPointType().isFinish()) {
                String takenCheckpoints = levelPoint.getCheckpoints().get(0).getCheckpointName();
                String finishDateString = scanPointDateTimes[currentIndex];
                Date finishDate = sdf.parse(finishDateString);
                for (Team team : teams) {
                    SQLiteDatabaseAdapter.getConnectedInstance().saveRawTeamLevelPoints(scanPoint, team, takenCheckpoints, finishDate);
                }
                Log.d("FILL_DATA", "levelPoint data filled: " + scanPoint.getScanPointName());
            }
            currentIndex++;
        }
    }

    private static void generateDismiss(Distance distance) {
        List<Team> teams = TeamsRegistry.getInstance().getTeams(distance.getDistanceId());

        ScanPoint finishPoint = scanPoints.get(scanPoints.size() - 1);
        for (Team team : teams) {
            List<Participant> members = team.getMembers();
            if (members.size() == 1) continue;
            List<Participant> withdrawnMembers = new ArrayList<Participant>();
            for (int i = 0; i < members.size(); i++) {
                if (i == 0) continue;
                withdrawnMembers.add(members.get(i));
            }
            SQLiteDatabaseAdapter.getConnectedInstance().saveDismissedMembers(finishPoint, team, withdrawnMembers, new Date());
        }
        Log.d("FILL_DATA", "levelDismiss data filled");
    }
}
