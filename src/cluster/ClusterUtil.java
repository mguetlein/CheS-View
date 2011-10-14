package cluster;

import javax.vecmath.Vector3f;

public class ClusterUtil
{

	public static Vector3f[] getModelPositions(Cluster c)
	{
		return getModelPositions(c, true);
	}

	public static Vector3f[] getModelPositions(Cluster c, boolean scaled)
	{
		Vector3f list[] = new Vector3f[c.size()];
		int i = 0;
		for (Model m : c.getModels())
			list[i++] = m.getPosition(scaled);
		return list;
	}
}
