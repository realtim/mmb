package ru.mmb.datacollector.transport.importer;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import ru.mmb.datacollector.db.SQLiteDatabaseAdapter;
import ru.mmb.datacollector.model.meta.MetaTable;

/**
 * Data synchronization disabled.<br>
 * All records MUST be removed from table before import.<br>
 * All rows from import package will be imported without any checks.<br>
 * <p/>
 * But import with synchronization features can be restored at any moment.
 *
 * @author yweiss
 */
public class DataSaver {
    private MetaTable currentTable = null;
    private final SQLiteDatabase db;

    public DataSaver() {
        // DatacollectorDB.getRawInstance() will never be null, but db can be null.
        db = SQLiteDatabaseAdapter.getRawInstance().getDb();
    }

    public void setCurrentTable(MetaTable metaTable) {
        currentTable = metaTable;
    }

    public void saveRecordToDB(JSONObject tableRow) throws JSONException {
        if (currentTable == null) return;
        if (tableRow == null) return;

        // If table is cleared before import, then no PK violation possible.
        if (currentTable.needClearBeforeImport()) {
            insertRecord(tableRow);
            return;
        }

        ImportToDBAction action = getImportToDBAction(tableRow);
        if (action == ImportToDBAction.UPDATE) {
            updateRecord(tableRow);
        } else if (action == ImportToDBAction.INSERT) {
            insertRecord(tableRow);
        }
    }

    private ImportToDBAction getImportToDBAction(JSONObject tableRow) throws JSONException {
        if (isRecordExists(tableRow)) {
            return ImportToDBAction.UPDATE;
        } else {
            return ImportToDBAction.INSERT;
        }
    }

    private boolean isRecordExists(JSONObject tableRow) throws JSONException {
        String sql = currentTable.generateCheckExistsSQL(tableRow);
        Cursor cursor = db.rawQuery(sql, null);
        try {
            cursor.moveToFirst();
            return cursor.getInt(0) == 1;
        } finally {
            cursor.close();
        }
    }

    private void updateRecord(JSONObject tableRow) throws JSONException {
        String sql = currentTable.generateUpdateSQL(tableRow);
        db.execSQL(sql);
    }

    private void insertRecord(JSONObject tableRow) throws JSONException {
        String sql = currentTable.generateInsertSQL(tableRow);
        db.execSQL(sql);
    }

    public void clearCurrentTable() {
        String sql = currentTable.generateDeleteAllRowsSQL();
        db.execSQL(sql);
    }

    public void beginTransaction() {
        db.beginTransaction();
    }

    public void setTransactionSuccessful() {
        db.setTransactionSuccessful();
    }

    public void endTransaction() {
        db.endTransaction();
    }
}
