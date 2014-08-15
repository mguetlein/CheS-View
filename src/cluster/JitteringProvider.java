package cluster;

import gui.LaunchCheSMapper;
import gui.MessagePanel;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.vecmath.Vector3f;

import jitter.Jitterable;
import jitter.Jittering;
import main.Settings;
import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.MathUtil;
import util.SetUtil;
import util.SwingUtil;
import dataInterface.CompoundData;

public class JitteringProvider
{
	/**
	 * stores the compound-set that has been used for this jittering level
	 */
	private List<Set<Compound>> sets = new ArrayList<Set<Compound>>();
	/**
	 * stores the level that has last been used for a compound-set
	 */
	private HashMap<Set<Compound>, Integer> levels = new HashMap<Set<Compound>, Integer>();
	private DoubleKeyHashMap<CompoundData, Integer, Vector3f> positions = new DoubleKeyHashMap<CompoundData, Integer, Vector3f>();
	private int currentLevel;
	private float[] minDistances;
	private static JitteringProvider staticInstance;
	public static final int STEPS = 10;

	public JitteringProvider(Clustering c)
	{
		staticInstance = null;

		/*
		 * jittering in ches-mapper has fixed number of steps
		 * each step has a min-distance that compounds should have
		 * the min-distance is computed based on the entire data, within interval:
		 * [ min-min-dist , min-min-dist + delta(max-min-distance,min-min-dist)/2 ]
		 * the interval chunks are not equi-distant but using a log-scale  
		 */
		minDistances = new float[STEPS + 1];
		Jittering j = create(c.getCompounds(), null, 1);
		float dist = j.getMinMinDist();
		float add = (j.getMaxMinDist() - j.getMinMinDist()) * 0.5f;
		double log[] = ArrayUtil.toPrimitiveDoubleArray(MathUtil.logBinning(STEPS, 1.2));
		for (int i = 1; i <= STEPS; i++)
			minDistances[i] = dist + add * (float) log[i];
		Settings.LOGGER
				.info("Initiated min-distances per level for jittering: " + ArrayUtil.toNiceString(minDistances));
	}

	public Vector3f getPosition(CompoundData c, int level)
	{
		if (level == 0)
			return c.getPosition();
		if (positions.containsKeyPair(c, level))
			return positions.get(c, level);
		else
			return c.getPosition();
	}

	public static Vector3f getPosition(CompoundData c)
	{
		if (staticInstance == null)
			return c.getPosition();
		else
			return staticInstance.getPosition(c, staticInstance.currentLevel);
	}

	private Jittering create(List<Compound> compounds, int level)
	{
		List<CompoundData> d = new ArrayList<CompoundData>();
		List<Vector3f[]> o = new ArrayList<Vector3f[]>();
		for (Compound c : compounds)
		{
			d.add(c.getCompoundData());
			o.add(new Vector3f[] { new Vector3f(0, 0, 0) });
		}
		return create(d, o, level);
	}

	private Jittering create(List<CompoundData> d, List<Vector3f[]> o, final int level)
	{
		class MyJitterable implements Jitterable
		{
			Vector3f p;
			Vector3f o[];

			public MyJitterable(CompoundData d, Vector3f o[])
			{
				if (level <= 0)
					throw new IllegalArgumentException();
				else if (level == 1)
					p = new Vector3f(d.getPosition());
				else
					p = new Vector3f(JitteringProvider.this.getPosition(d, level - 1));
				this.o = o;
			}

			@Override
			public Vector3f getPosition()
			{
				return p;
			}

			@Override
			public Vector3f[] getOffsets()
			{
				return o;
			}
		}
		Jitterable jObjects[] = new Jitterable[d.size()];
		for (int i = 0; i < jObjects.length; i++)
			jObjects[i] = new MyJitterable(d.get(i), (o != null ? o.get(i) : null));
		return new Jittering(jObjects, new Random());
	}

	public void updateJittering(int level, Set<Compound> compounds)
	{
		Settings.LOGGER.info("Set jittering level to " + level);
		if (level == 0)
		{
			//do nothing
		}
		else if (sets.size() > level && SetUtil.isSubSet(sets.get(level), compounds))
		{
			//System.err.println("jittering positions cached");
		}
		else
		{
			jitter(compounds, level);
			while (sets.size() < level + 1)
				sets.add(null);
			sets.set(level, compounds);
		}
		while (sets.size() > level + 1)
			sets.remove(level + 1);
		levels.put(compounds, level);

		staticInstance = this;
		currentLevel = level;
	}

	private void jitter(Set<Compound> c, int level)
	{
		jitter(new ArrayList<Compound>(c), level);
	}

	private void jitter(List<Compound> c, final int level)
	{
		Settings.LOGGER.info("Compute jittering for " + c.size() + " compounds for level " + level);
		Jittering j = create(c, level);
		j.setMinDist(minDistances[level]);
		j.jitter();
		for (int i = 0; i < c.size(); i++)
			positions.put(c.get(i).getCompoundData(), level, j.getPosition(i));
	}

	public int getJitteringResetLevel(Set<Compound> compounds)
	{
		int currentLevel = Math.max(0, (sets.size() - 1));
		//		System.err.println("current jittering level is " + currentLevel);
		int lastLevelForSet = levels.containsKey(compounds) ? levels.get(compounds) : 0;
		//		System.err.println("last jittering level for this set is " + lastLevelForSet);
		if (lastLevelForSet == currentLevel)
		{
			if (lastLevelForSet == 0)
				return -1;
			//			System.err.println("current jittering level is equal, check if sets are complient");
			if (SetUtil.isSubSet(sets.get(currentLevel), compounds))
			{
				//				System.err.println("yes, nothing todo");
				return -1;
			}
			else
			{
				//				System.err.println("no, check lower level");
				currentLevel = -1;
			}
		}
		//		else
		//			System.err.println("current jittering level differs");
		for (int j = currentLevel; j >= 1; j--)
		{
			//			System.err.println("check if this set is subset of level " + j);
			if (SetUtil.isSubSet(sets.get(j), compounds))
			{
				//				System.err.println("yes, use level " + j);
				return j;
			}
		}
		//		System.err.println("not compatible, reset to 0");
		return 0;
	}

	private static boolean showWarning = true;

	public static void showJitterWarning()
	{
		if (showWarning)
		{
			MessagePanel p = new MessagePanel();
			p.addWarning(Settings.text("spread.warning"), Settings.text("spread.warning.details"));
			SwingUtil.showInDialog(p, "Warning", new Dimension(600, 300), null, Settings.TOP_LEVEL_FRAME);
			showWarning = false;
		}
	}

	public static void main(String[] args)
	{
		LaunchCheSMapper.init();
		showJitterWarning();
	}
}
