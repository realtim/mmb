package ru.mmb.datacollector.model.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.Distance;
import ru.mmb.datacollector.model.LevelPoint;
import ru.mmb.datacollector.model.LevelPointDiscount;
import ru.mmb.datacollector.model.ScanPoint;

public class ScanPointsRegistry {
    private static ScanPointsRegistry instance = null;

    private List<ScanPoint> scanPoints = null;
    private List<LevelPoint> levelPoints = null;
    private DistancesRegistry distancesRegistry;

    public static synchronized ScanPointsRegistry getInstance() {
        if (instance == null) {
            instance = new ScanPointsRegistry();
        }
        return instance;
    }

    private ScanPointsRegistry() {
        refresh();
    }

    public void refresh() {
        try {
            // Init distances registry first.
            distancesRegistry = DistancesRegistry.getInstance();

            // Now load scanPoints and levelPoints.
            scanPoints = SQLiteDatabaseAdapter.getConnectedInstance().loadScanPoints(getCurrentRaidId());
            Collections.sort(scanPoints);

            levelPoints = SQLiteDatabaseAdapter.getConnectedInstance().loadLevelPoints(getCurrentRaidId());
            updateDistanceForLevelPoints(levelPoints);
            for (ScanPoint scanPoint : scanPoints) {
                addLevelPointsToScanPoint(scanPoint, levelPoints);
            }

            // Load levelPointDiscounts.
            List<LevelPointDiscount> discounts = SQLiteDatabaseAdapter.getConnectedInstance().loadLevelPointDiscounts(
                    getCurrentRaidId());
            addDiscountsToLevelPoints(discounts);
        } catch (Exception e) {
            e.printStackTrace();
            scanPoints = new ArrayList<ScanPoint>();
        }
    }

    private int getCurrentRaidId() {
        return Settings.getInstance().getCurrentRaidId();
    }

    private void updateDistanceForLevelPoints(List<LevelPoint> levelPoints) {
        for (LevelPoint levelPoint : levelPoints) {
            Distance distance = distancesRegistry.getDistanceById(levelPoint.getDistanceId());
            levelPoint.setDistance(distance);
        }
    }

    private void addLevelPointsToScanPoint(ScanPoint scanPoint, List<LevelPoint> levelPoints) {
        for (LevelPoint levelPoint : levelPoints) {
            if (levelPoint.getScanPointId() == scanPoint.getScanPointId()) {
                scanPoint.addLevelPoint(levelPoint);
                levelPoint.setScanPoint(scanPoint);
            }
        }
    }

    private void addDiscountsToLevelPoints(List<LevelPointDiscount> discounts) {
        Map<Integer, List<LevelPoint>> distanceLevelPoints = groupLevelPointsByDistance();
        for (LevelPointDiscount discount : discounts) {
            int distanceId = discount.getDistanceId();
            List<LevelPoint> levelPointsGroup = distanceLevelPoints.get(distanceId);
            if (levelPointsGroup == null)
                continue;
            for (LevelPoint levelPoint : levelPointsGroup) {
                if (levelPoint.containsLevelPointDiscount(discount)) {
                    levelPoint.addLevelPointDiscount(discount);
                }
            }
        }
    }

    private Map<Integer, List<LevelPoint>> groupLevelPointsByDistance() {
        Map<Integer, List<LevelPoint>> result = new HashMap<Integer, List<LevelPoint>>();
        for (LevelPoint levelPoint : levelPoints) {
            List<LevelPoint> distancePoints = result.get(levelPoint.getDistanceId());
            if (distancePoints == null) {
                distancePoints = new ArrayList<LevelPoint>();
                result.put(levelPoint.getDistanceId(), distancePoints);
            }
            distancePoints.add(levelPoint);
        }
        return result;
    }

    public List<ScanPoint> getScanPoints() {
        return Collections.unmodifiableList(scanPoints);
    }

    public ScanPoint getScanPointByIndex(int index) {
        return scanPoints.get(index);
    }

    public ScanPoint getScanPointById(int id) {
        for (ScanPoint scanPoint : scanPoints) {
            if (scanPoint.getScanPointId() == id)
                return scanPoint;
        }
        return null;
    }

    public String[] getScanPointNamesArray() {
        String[] result = new String[scanPoints.size()];
        for (int i = 0; i < scanPoints.size(); i++) {
            result[i] = scanPoints.get(i).getScanPointName();
        }
        return result;
    }

    public int getScanPointIndex(ScanPoint scanPoint) {
        return scanPoints.indexOf(scanPoint);
    }

    public ScanPoint getScanPointByOrder(int scanPointOrder) {
        for (ScanPoint scanPoint : scanPoints) {
            if (scanPoint.getScanPointOrder() == scanPointOrder)
                return scanPoint;
        }
        return null;
    }

    public ScanPoint getScanPointByLevelPointId(int levelPointId) {
        for (LevelPoint levelPoint : levelPoints) {
            if (levelPoint.getLevelPointId() == levelPointId) {
                return levelPoint.getScanPoint();
            }
        }
        return null;
    }
}
