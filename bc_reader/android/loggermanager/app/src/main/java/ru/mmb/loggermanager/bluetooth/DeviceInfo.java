package ru.mmb.loggermanager.bluetooth;

import java.io.Serializable;

public class DeviceInfo implements Serializable, Comparable<DeviceInfo> {
    private String deviceName;
    private String deviceBTAddress;

    public DeviceInfo() {
    }

    public DeviceInfo(String deviceName, String deviceBTAddress) {
        this.deviceName = deviceName;
        this.deviceBTAddress = deviceBTAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceBTAddress() {
        return deviceBTAddress;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setDeviceBTAddress(String deviceBTAddress) {
        this.deviceBTAddress = deviceBTAddress;
    }

    public String saveToString() {
        return deviceName + "|" + deviceBTAddress;
    }

    public void loadFromString(String source) {
        int separatorPos = source.indexOf("|");
        deviceName = source.substring(0, separatorPos);
        deviceBTAddress = source.substring(separatorPos + 1, source.length());
    }

    @Override
    public int compareTo(DeviceInfo another) {
        return deviceName.compareTo(another.deviceName);
    }

    @Override
    public String toString() {
        return "LoggerInfo{" +
                "deviceName='" + deviceName + '\'' +
                ", deviceBTAddress='" + deviceBTAddress + '\'' +
                '}';
    }
}
