package cluster;

import javax.vecmath.Vector3f;

import util.Vector3fUtil;

public class ClusteringUtil
{
	public static Vector3f[] getClusterPositions(Clustering c)
	{
		Vector3f list[] = new Vector3f[c.getNumClusters()];
		int i = 0;
		for (Cluster cc : c.getClusters())
			list[i++] = cc.getCenter(true);
		return list;
	}

	public static Vector3f[] getCompoundPositions(Cluster c)
	{
		Vector3f list[] = new Vector3f[c.size()];
		int i = 0;
		for (Compound m : c.getCompounds())
			list[i++] = m.getPosition();
		return list;
	}

	public static Vector3f[] getCompoundPositions(Clustering c)
	{
		return getCompoundPositions(c, true);
	}

	private static Vector3f[] getCompoundPositions(Clustering c, boolean scale)
	{
		Vector3f list[] = new Vector3f[c.getNumCompounds(true)];
		int i = 0;
		for (Compound m : c.getCompounds(true))
			list[i++] = m.getPosition(scale);
		return list;
	}

	public static int COMPOUND_SIZE_MAX = 40;
	public static int COMPOUND_SIZE = 20;
	public static float SCALE = 0;

	/**
	 * scale factor is used to scale the original 3d mapping values
	 * 
	 * @param v
	 * @return
	 */
	public static void updateScaleFactor(Clustering c)
	{
		Vector3f[] v = ClusteringUtil.getCompoundPositions(c, false);

		// the average min distance mean distance of each compound to its closest neighbor compound
		float d = Vector3fUtil.avgMinDist(v);
		// the smaller the distance, the higher the scale factor
		// the neigbhor should be on average 30units away
		float s = 1 / d * 30;
		//Settings.LOGGER.println("d: " + d + ", s: " + (1 / d * 30));

		// we want to set a max dist of 350units
		float max_scale = 100 / Vector3fUtil.maxDist(v);
		//Settings.LOGGER.println("max_s: " + max_scale);
		s = Math.min(s, max_scale);

		if (COMPOUND_SIZE < 0 || COMPOUND_SIZE > COMPOUND_SIZE_MAX)
			throw new Error("illegal compound size");
		// convert "int range 0 - COMPOUND_SIZE_MAX" to "float range 4.0 - 0.1"  
		float density = (float) (((1 - COMPOUND_SIZE / ((double) COMPOUND_SIZE_MAX)) * 3.9f) + 0.1f);
		//		System.err.println(ClusteringUtil.COMPOUND_SIZE + " -> " + density);

		// scale is multiplied with the DENSITY, which is configurable by the user
		SCALE = s * density;
	}

}
