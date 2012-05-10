package ru.mmb.terminal.transport.registry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.mmb.terminal.db.TerminalDB;
import ru.mmb.terminal.transport.model.MetaTable;

public class MetaTablesRegistry
{
	private static MetaTablesRegistry instance = null;

	private List<MetaTable> tables = null;
	private final Map<String, MetaTable> tablesByName = new HashMap<String, MetaTable>();

	public static MetaTablesRegistry getInstance()
	{
		if (instance == null)
		{
			instance = new MetaTablesRegistry();
		}
		return instance;
	}

	private MetaTablesRegistry()
	{
		load();
	}

	private void load()
	{
		try
		{
			tables = TerminalDB.getInstance().loadMetaTables();
			fillTablesByName();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Meta tables list load failed.", e);
		}
	}

	private void fillTablesByName()
	{
		for (MetaTable metaTable : tables)
		{
			tablesByName.put(metaTable.getTableName().toUpperCase(), metaTable);
		}
	}

	public List<MetaTable> getTables()
	{
		return tables;
	}

	public MetaTable getTableByName(String tableName)
	{
		return tablesByName.get(tableName.toUpperCase());
	}
}
