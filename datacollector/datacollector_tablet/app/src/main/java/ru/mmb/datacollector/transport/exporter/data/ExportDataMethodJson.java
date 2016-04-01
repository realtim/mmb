package ru.mmb.datacollector.transport.exporter.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;

import ru.mmb.datacollector.model.meta.MetaTable;
import ru.mmb.datacollector.model.registry.MetaTablesRegistry;
import ru.mmb.datacollector.transport.exporter.DataExtractorToJson;
import ru.mmb.datacollector.transport.exporter.ExportState;

public class ExportDataMethodJson implements ExportDataMethod {
    private final ExportState exportState;
    private final DataExtractorToJson dataExtractor;
    private final BufferedWriter writer;

    public ExportDataMethodJson(ExportState exportState, DataExtractorToJson dataExtractor, BufferedWriter writer) {
        this.exportState = exportState;
        this.dataExtractor = dataExtractor;
        this.writer = writer;
    }

    @Override
    public void exportData() throws Exception {
        JSONObject mainContainer = new JSONObject();
        if (exportState.isTerminated()) return;
        exportTable("RawLoggerData", mainContainer);
        if (exportState.isTerminated()) return;
        exportTable("RawTeamLevelDismiss", mainContainer);
        if (exportState.isTerminated()) return;
        exportTable("RawTeamLevelPoints", mainContainer);
        if (exportState.isTerminated()) return;
        writer.write(mainContainer.toString());
    }

    private void exportTable(String tableName, JSONObject mainContainer) throws Exception {
        MetaTable table = MetaTablesRegistry.getInstance().getTableByName(tableName);
        table.setExportWhereAppendix("");
        JSONArray records = new JSONArray();
        dataExtractor.setTargetRecords(records);
        dataExtractor.setCurrentTable(table);
        if (exportState.isTerminated()) return;
        dataExtractor.exportAllRecords(exportState);
        mainContainer.put(tableName, records);
    }
}
