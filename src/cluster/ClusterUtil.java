package cluster;

import javax.vecmath.Vector3f;

import util.Vector3fUtil;

public class ClusterUtil
{
	public static float getScaleFactor(Vector3f[] v)
	{
		float d = Vector3fUtil.avgMinDist(v);
		return 1 / d * 30;
	}
}
