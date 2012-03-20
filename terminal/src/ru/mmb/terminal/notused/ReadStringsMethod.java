package ru.mmb.terminal.notused;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ReadStringsMethod
{
	private final InputStreamReader reader;
	private final char separator;

	public ReadStringsMethod(InputStreamReader reader, char separator)
	{
		this.reader = reader;
		this.separator = separator;
	}

	public List<String> execute() throws IOException
	{
		List<String> result = new ArrayList<String>();

		String currentString = "";
		char inputChar;
		int readResult = reader.read();

		while (readResult != -1)
		{
			inputChar = (char) readResult;
			if (inputChar == separator)
			{
				if (!"".equals(currentString.trim())) result.add(currentString.trim());
				currentString = "";
			}
			else
			{
				currentString += inputChar;
			}
			readResult = reader.read();
		}

		if (!"".equals(currentString.trim())) result.add(currentString.trim());

		return result;
	}
}
