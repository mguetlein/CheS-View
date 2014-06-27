package cluster;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.List;

import weka.Predictor.PredictionResult;
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

	Compound getActiveCompound();

	int[] getActiveCompoundsJmolIdx();

	Compound getWatchedCompound();

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

	String getSDFile();

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

	double getSpecificity(CompoundSelection sel, CompoundProperty p);

	int getNumCompounds(boolean includingMultiClusteredCompounds);

	int getNumUnfilteredCompounds(boolean includingMultiClusteredCompounds);

	String getEmbedAlgorithm();

	String getEmbedQuality();

	List<CompoundProperty> getAdditionalProperties();

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

	boolean isBMBFRealEndpointDataset(boolean b);

	CompoundProperty addDistanceToCompoundFeature(Compound c);

	CompoundProperty addSALIFeatures(CompoundProperty c);

	void predict();

	void addPredictionFeature(CompoundProperty clazz, PredictionResult p);

	public CompoundSelection getCompoundSelection(Compound[] c);

	boolean isRandomEmbedding();

	CompoundProperty getHighlightProperty();

	Color getHighlightColorText();

	Double getFeatureDistance(int origIndex, int origIndex2);

	boolean isSkippingRedundantFeatures();

	boolean isBigDataMode();

	void computeAppDomain();

}
