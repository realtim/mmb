package ru.mmb.terminal.test.activity.input.data.checkpoints;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.TestCase;
import ru.mmb.terminal.model.Checkpoint;
import ru.mmb.terminal.model.LevelPoint;
import ru.mmb.terminal.model.PointType;
import ru.mmb.terminal.util.DateFormat;

public class CheckedStateTest extends TestCase
{
	private LevelPoint levelPoint;
	private final Map<Integer, Checkpoint> checkpointsMap = new TreeMap<Integer, Checkpoint>();

	@Override
	protected void setUp() throws Exception
	{
		levelPoint =
		    new LevelPoint(154, PointType.CHANGE_MAPS, 33, 8, 7, DateFormat.parse("201404010100"), DateFormat.parse("201404012359"));
		List<String> levelPointNames =
		    Arrays.asList(new String[] { "КП1", "КП2", "КП3", "КП4", "КП5" });
		List<Integer> levelPointPenalties = Arrays.asList(new Integer[] { 120, 120, 120, 120, 60 });
		levelPoint.addCheckpoints(levelPointNames, levelPointPenalties);
		for (Checkpoint checkpoint : levelPoint.getCheckpoints())
		{
			checkpointsMap.put(checkpoint.getCheckpointOrder(), checkpoint);
		}
	}

	public void testSetUp()
	{
		assertEquals("КП3", checkpointsMap.get(new Integer(3)).getCheckpointName());
		assertEquals(120, checkpointsMap.get(new Integer(4)).getCheckpointPenalty());
		assertEquals(5, checkpointsMap.size());

		assertEquals(5, levelPoint.getCheckpoints().size());
		assertEquals("КП1", levelPoint.getCheckpointByOrderNum(1).getCheckpointName());
		assertEquals(60, levelPoint.getCheckpointByOrderNum(5).getCheckpointPenalty());
	}
}
