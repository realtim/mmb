package ru.mmb.terminal.model.registry;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractRegistry
{
	@SuppressWarnings("unchecked")
	protected <T> List<T> loadStoredElements(String fileName, Class<T> storedElementType)
	        throws Exception
	{
		BufferedReader reader =
		    new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "Cp1251"));
		List<T> result = new ArrayList<T>();
		Method parseMethod = storedElementType.getMethod("parse", String.class);
		try
		{
			String elementString = reader.readLine();
			while (elementString != null)
			{
				T element = (T) parseMethod.invoke(null, elementString);
				if (element != null) result.add(element);
				elementString = reader.readLine();
			}
		}
		finally
		{
			reader.close();
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	protected <T1, T2> List<T1> loadStoredElements(String fileName, Class<T1> storedElementType,
	        Map<Integer, List<T2>> children) throws Exception
	{
		BufferedReader reader =
		    new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "Cp1251"));
		List<T1> result = new ArrayList<T1>();
		Method parseMethod = storedElementType.getMethod("parse", String.class, Map.class);
		try
		{
			String elementString = reader.readLine();
			while (elementString != null)
			{
				T1 element = (T1) parseMethod.invoke(null, elementString, children);
				if (element != null) result.add(element);
				elementString = reader.readLine();
			}
		}
		finally
		{
			reader.close();
		}
		return result;
	}

	protected <T extends Parented> Map<Integer, List<T>> groupChildrenByParentId(List<T> children)
	{
		Map<Integer, List<T>> result = new HashMap<Integer, List<T>>();
		for (T child : children)
		{
			Integer parentId = child.getParentId();
			if (!result.containsKey(parentId)) result.put(parentId, new ArrayList<T>());
			result.get(parentId).add(child);
		}
		return result;
	}
}
