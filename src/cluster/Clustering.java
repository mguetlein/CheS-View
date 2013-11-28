package cluster;

import java.beans.PropertyChangeListener;
import java.util.List;

import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertyOwner;

public interface Clustering extends CompoundPropertyOwner
{
	void addListener(PropertyChangeListener propertyChangeListener);

	int getNumClusters();

	int numClusters();

	List<Cluster> getClusters();

	boolean isClusterActive();

	boolean isClusterWatched();

	Cluster getCluster(int i);

	int indexOf(Cluster cluster);

	Cluster getActiveCluster();

	Cluster getWatchedCluster();

	int getActiveClusterIdx();

	int getWatchedClusterIdx();

	boolean isCompoundActive();

	boolean isCompoundWatched();

	boolean isCompoundActive(Compound c);

	Compound[] getActiveCompounds();

	int[] getActiveCompoundsJmolIdx();

	Compound[] getWatchedCompounds();

	int[] getWatchedCompoundsJmolIdx();

	Cluster getClusterForCompound(Compound c);

	List<CompoundProperty> getPropertiesAndFeatures();

	List<CompoundProperty> getProperties();

	List<CompoundProperty> getFeatures();

	List<Compound> getCompounds(boolean includingMultiClusteredCompounds);

	Cluster getUniqueClusterForCompounds(Compound[] c);

	String getOrigLocalPath();

	String getOrigSDFile();

	boolean isClusterAlgorithmDisjoint();

	String getClusterAlgorithm();

	int getClusterIndexForCompound(Compound m);

	List<CompoundData> getCompounds();

	void chooseClustersToExport(CompoundProperty compoundDescriptor);

	void chooseCompoundsToExport(CompoundProperty compoundDescriptor);

	Double[] getDoubleValues(CompoundProperty p);

	String[] getStringValues(CompoundProperty p, Compound m);

	String getSummaryStringValue(CompoundProperty p, boolean b);

	int numMissingValues(CompoundProperty p);

	String getName();

	Double getNormalizedLogDoubleValue(CompoundPropertyOwner m, CompoundProperty p);

	Double getNormalizedDoubleValue(CompoundPropertyOwner m, CompoundProperty p);

	double getSpecificity(Compound compound, CompoundProperty p);

	double getSpecificity(Cluster cluster, CompoundProperty p);

	int getNumCompounds(boolean b);

	String getEmbedAlgorithm();

	String getEmbedQuality();

	CompoundProperty getEmbeddingQualityProperty();

	Compound getCompoundWithJmolIndex(int convertRowIndexToModel);

	int numDistinctValues(CompoundProperty p);

	public void addSelectionListener(SelectionListener l);

	public abstract static class SelectionListener
	{
		public void clusterActiveChanged(Cluster c)
		{
		}

		public void clusterWatchedChanged(Cluster c)
		{
		}

		public void compoundActiveChanged(Compound c[])
		{
		}

		public void compoundWatchedChanged(Compound c[])
		{
		}
	}

}
