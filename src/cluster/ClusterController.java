package cluster;

public interface ClusterController
{
	public void clearClusterActive(boolean animate, boolean clearCompoundActive);

	public void clearClusterWatched();

	public void clearCompoundActive(boolean animate);

	public void clearCompoundWatched();

	public void setClusterActive(Cluster c, boolean animate, boolean clearCompoundActive);

	public void setClusterWatched(Cluster c);

	public void setCompoundActive(Compound c, boolean animate);

	public void setCompoundActive(Compound[] c, boolean animate);

	public void toggleCompoundActive(Compound c);

	public void setCompoundWatched(Compound... c);

	public CompoundFilter getCompoundFilter();

	public void setCompoundFilter(CompoundFilter filter, boolean animate);

	public void useSelectedCompoundsAsFilter(String filterDescription, boolean animate);

	public void removeCompounds(Compound[] c);

	public void removeCluster(Cluster... c);

	public void chooseClustersToRemove();

	public void chooseCompoundsToRemove();

	public void newClustering();

}
