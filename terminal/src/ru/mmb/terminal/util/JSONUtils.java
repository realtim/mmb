package ru.mmb.terminal.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class JSONUtils
{
	public static String readFromInputStream(InputStreamReader reader, int bufferSize)
	        throws IOException
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			char[] buffer = new char[bufferSize];

			int charsRead = reader.read(buffer);
			while (charsRead != -1)
			{
				sb.append(buffer, 0, charsRead);
				charsRead = reader.read(buffer);
			}
			return sb.toString();
		}
		finally
		{
			reader.close();
		}
	}

	public static String readFromFile(String fileName, int bufferSize) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(new FileInputStream(fileName), "UTF8");
		return readFromInputStream(reader, bufferSize);
	}
}
