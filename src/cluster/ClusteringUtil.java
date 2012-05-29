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

	public static Vector3f[] getModelPositions(Cluster c)
	{
		Vector3f list[] = new Vector3f[c.size()];
		int i = 0;
		for (Model m : c.getModels())
			list[i++] = m.getPosition();
		return list;
	}

	public static Vector3f[] getModelPositions(Clustering c)
	{
		return getModelPositions(c, true);
	}

	private static Vector3f[] getModelPositions(Clustering c, boolean scale)
	{
		Vector3f list[] = new Vector3f[c.getNumCompounds()];
		int i = 0;
		for (Model m : c.getModels())
			list[i++] = m.getPosition(scale);
		return list;
	}

	public static float DENSITY = 1f;
	public static float SCALE = 0;

	/**
	 * scale factor is used to scale the original 3d mapping values
	 * 
	 * @param v
	 * @return
	 */
	public static void updateScaleFactor(Clustering c)
	{
		Vector3f[] v = ClusteringUtil.getModelPositions(c, false);

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

		// scale is multiplied with the DENSITY, which is configurable by the user
		SCALE = s * DENSITY;
	}

}
