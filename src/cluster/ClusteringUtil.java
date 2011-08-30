package cluster;

import javax.vecmath.Vector3f;

public class ClusteringUtil
{
	public static Vector3f[] getClusterPositions(Clustering c, boolean orig)
	{
		Vector3f list[] = new Vector3f[c.getNumClusters()];
		int i = 0;
		for (Cluster cc : c.getClusters())
			if (orig)
				list[i++] = cc.getOrigPosition();
			else
				list[i++] = cc.getPosition();
		return list;
	}
}
