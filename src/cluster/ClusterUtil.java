package cluster;

import javax.vecmath.Vector3f;

import util.Vector3fUtil;

public class ClusterUtil
{
	public static float DENSITY = 1f;

	public static float getScaleFactor(Vector3f[] v)
	{
		float d = Vector3fUtil.avgMinDist(v);
		return 1 / d * 30 * DENSITY;
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
