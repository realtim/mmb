package ru.mmb.datacollector.transport.exporter;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.meta.MetaTable;

public abstract class DataExtractor {
    private MetaTable currentTable = null;
    private final SQLiteDatabase db;

    protected abstract void exportRow(Cursor cursor) throws Exception;

    public DataExtractor() {
        // DatacollectorDB.getRawInstance() will never be null, but db can be null.
        this.db = SQLiteDatabaseAdapter.getRawInstance().getDb();
    }

    public void setCurrentTable(MetaTable metaTable) {
        currentTable = metaTable;
    }

    public MetaTable getCurrentTable() {
        return currentTable;
    }

    public void exportAllRecords(ExportState exportState) throws Exception {
        String selectSql = currentTable.generateSelectAllRecordsSQL();
        Cursor cursor = db.rawQuery(selectSql, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast() && !exportState.isTerminated()) {
            exportRow(cursor);
            cursor.moveToNext();
        }
        cursor.close();
    }
}
