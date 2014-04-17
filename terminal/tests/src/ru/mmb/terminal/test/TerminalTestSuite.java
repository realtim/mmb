package ru.mmb.terminal.test;

import junit.framework.TestSuite;
import ru.mmb.terminal.test.activity.input.data.checkpoints.CheckedStateTest;
import ru.mmb.terminal.test.model.history.DataStorageTest;
import ru.mmb.terminal.test.model.history.TeamLevelPointsStorageTest;
import ru.mmb.terminal.test.model.history.ScanPointRecordsTest;

public class TerminalTestSuite extends TestSuite
{
	public static TestSuite suite()
	{
		TestSuite suite = new TestSuite("Terminal test suite");
		suite.addTestSuite(CheckedStateTest.class);
		suite.addTestSuite(ScanPointRecordsTest.class);
		suite.addTestSuite(TeamLevelPointsStorageTest.class);
		suite.addTestSuite(DataStorageTest.class);
		return suite;
	}
}
