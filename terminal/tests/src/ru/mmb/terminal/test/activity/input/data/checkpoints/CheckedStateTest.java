package ru.mmb.terminal.test.activity.input.data.checkpoints;

import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;
import ru.mmb.terminal.model.Checkpoint;
import ru.mmb.terminal.model.Level;
import ru.mmb.terminal.model.StartType;
import ru.mmb.terminal.util.DateFormat;

public class CheckedStateTest extends TestCase
{
	private Level level;
	private final Map<Integer, Checkpoint> checkpointsMap = new TreeMap<Integer, Checkpoint>();

	@Override
	protected void setUp() throws Exception
	{
		level =
		    new Level(1, 1, "Старт - СК", 1, StartType.WHEN_READY, DateFormat.parse("201205152000"), DateFormat.parse("201205160000"), DateFormat.parse("201205152200"), DateFormat.parse("201205170000"));
		level.addCheckpoints("КП1,КП2,КП3,КП4,КП5,КП6,КП7,КП8,КП9,А1,А2,А3,А4,А5", "120,120,120,120,120,120,120,120,120,30,30,30,30,30");
		for (Checkpoint checkpoint : level.getCheckpoints())
		{
			checkpointsMap.put(checkpoint.getCheckpointOrder(), checkpoint);
		}
	}

	public void testSetUp()
	{
		assertEquals("КП3", checkpointsMap.get(new Integer(2)).getCheckpointName());
		assertEquals(120, checkpointsMap.get(new Integer(8)).getCheckpointPenalty());
		assertEquals(14, checkpointsMap.size());

		assertEquals(14, level.getCheckpoints().size());
		assertEquals("А1", level.getCheckpointByOrderNum(9).getCheckpointName());
		assertEquals(30, level.getCheckpointByOrderNum(9).getCheckpointPenalty());
	}
}
