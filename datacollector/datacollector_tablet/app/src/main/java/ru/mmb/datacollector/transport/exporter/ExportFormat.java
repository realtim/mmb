package ru.mmb.datacollector.transport.exporter;

public enum ExportFormat {
    TXT,

    JSON,

    TXT_TO_SITE;

    public String getFileExtension() {
        if ((this == ExportFormat.TXT) || (this == ExportFormat.TXT_TO_SITE)) {
            return "txt";
        } else {
            return "json";
        }
    }
}
