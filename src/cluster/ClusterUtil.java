package cluster;

import javax.vecmath.Vector3f;

import util.Vector3fUtil;

public class ClusterUtil
{
	public static float DENSITY = 1f;

	/**
	 * scale factor is used to scale the original 3d mapping values
	 * 
	 * @param v
	 * @return
	 */
	public static float getScaleFactor(Vector3f[] v)
	{
		// the average min distance mean distance of each compount to its closest neighbor compound
		float d = Vector3fUtil.avgMinDist(v);
		// the smaller the distance, the higher the scale factor
		// the neigbhor should be on average 30units away
		float s = 1 / d * 30;
		//System.out.println("d: " + d + ", s: " + (1 / d * 30));

		// we want to set a max dist of 350units
		float max_scale = 350 / Vector3fUtil.maxDist(v);
		//System.out.println("max_s: " + max_scale);
		s = Math.min(s, max_scale);

		// scale is multiplied with the DENSITY, which is configurable by the user
		return s * DENSITY;
	}

	public static Vector3f[] getModelPositions(Cluster c, boolean orig)
	{
		Vector3f list[] = new Vector3f[c.size()];
		int i = 0;
		for (Model m : c.getModels())
			if (orig)
				list[i++] = m.getOrigPosition();
			else
				list[i++] = m.getPosition();
		return list;
	}
}
