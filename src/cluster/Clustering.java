package cluster;

import gui.CheckBoxSelectDialog;
import gui.View;
import gui.Zoomable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Vector;

import javax.vecmath.Vector3f;

import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.FileUtil;
import util.SelectionModel;
import util.Vector3fUtil;
import util.VectorUtil;
import data.ClusteringData;
import dataInterface.ClusterData;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyUtil;
import dataInterface.SubstructureSmartsType;

public class Clustering implements Zoomable
{
	private Vector<Cluster> clusters;
	private ClusteringData clusteringData;

	SelectionModel clusterActive;
	SelectionModel clusterWatched;
	SelectionModel compoundActive;
	SelectionModel compoundWatched;

	boolean suppresAddEvent = false;
	Vector<PropertyChangeListener> listeners;

	public static String CLUSTER_ADDED = "cluster_added";
	public static String CLUSTER_REMOVED = "cluster_removed";
	public static String CLUSTER_MODIFIED = "cluster_modified";
	public static String CLUSTER_NEW = "cluster_new";
	public static String CLUSTER_CLEAR = "cluster_clear";

	boolean dirty = true;
	int numCompounds = -1;
	private boolean superimposed = false;
	float superimposedDiameter;
	float nonSuperimposedDiameter;

	BitSet bitSetAll;
	HashMap<Integer, Compound> compoundIndexToCompound;
	HashMap<Integer, Cluster> compoundIndexToCluster;
	HashMap<CompoundProperty, Integer> numMissingValues;
	HashMap<CompoundProperty, Integer> numDistinctValues;

	List<Compound> compoundList;
	List<Compound> compoundListIncludingMultiClusteredCompounds;

	Vector3f superimposedCenter;
	Vector3f nonSuperimposedCenter;

	public Clustering()
	{
		listeners = new Vector<PropertyChangeListener>();
		init();
	}

	public void init()
	{
		clusterActive = new SelectionModel();
		clusterWatched = new SelectionModel();
		compoundActive = new SelectionModel(true);
		compoundWatched = new SelectionModel(true);
		clusters = new Vector<Cluster>();
		numMissingValues = new HashMap<CompoundProperty, Integer>();
		numDistinctValues = new HashMap<CompoundProperty, Integer>();
	}

	public void addListener(PropertyChangeListener l)
	{
		listeners.add(l);
	}

	public void fire(String event, Object oldValue, Object newValue)
	{
		if (!suppresAddEvent)
			for (PropertyChangeListener l : listeners)
				l.propertyChange(new PropertyChangeEvent(this, event, oldValue, newValue));
	}

	private Cluster addSingleCluster(ClusterData clusterData, int begin, int endExcl)
	{
		Cluster c = new Cluster(clusterData, begin, endExcl);
		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		clusters.add(c);
		dirty = true;
		fire(CLUSTER_ADDED, old, clusters);
		return c;
	}

	public void update()
	{
		if (!dirty)
			return;

		numCompounds = 0;
		for (Cluster c : clusters)
			numCompounds += c.size();

		bitSetAll = new BitSet();
		for (Cluster c : clusters)
			bitSetAll.or(c.getBitSet());

		compoundIndexToCluster = new HashMap<Integer, Cluster>();
		compoundIndexToCompound = new HashMap<Integer, Compound>();
		for (Cluster c : clusters)
		{
			for (Compound m : c.getCompounds())
			{
				if (compoundIndexToCompound.get(m.getCompoundIndex()) != null)
					throw new Error("WTF");
				compoundIndexToCompound.put(m.getCompoundIndex(), m);
				compoundIndexToCluster.put(m.getCompoundIndex(), c);
			}
		}

		compoundListIncludingMultiClusteredCompounds = new ArrayList<Compound>();
		for (Cluster c : clusters)
			for (Compound mm : c.getCompounds())
				compoundListIncludingMultiClusteredCompounds.add(mm);

		compoundList = new ArrayList<Compound>();
		HashSet<Integer> compoundIndex = new HashSet<Integer>();
		for (Cluster c : clusters)
			for (Compound mm : c.getCompounds())
			{
				if (!compoundIndex.contains(mm.getCompoundOrigIndex()))
				{
					compoundList.add(mm);
					compoundIndex.add(mm.getCompoundOrigIndex());
				}
			}

		numMissingValues.clear();
		numDistinctValues.clear();
		normalizedLogValues.clear();
		normalizedValues.clear();

		dirty = false;
	}

	public boolean isClusterActive()
	{
		return clusterActive.getSelected() != -1;
	}

	public boolean isClusterWatched()
	{
		return clusterWatched.getSelected() != -1;
	}

	public boolean isCompoundActive()
	{
		return compoundActive.getSelected() != -1;
	}

	public boolean isCompoundWatched()
	{
		return compoundWatched.getSelected() != -1;
	}

	public boolean isCompoundActiveFromCluster(int cluster)
	{
		int sel[] = compoundActive.getSelectedIndices();
		if (sel.length == 0)
			return false;
		for (int compound : sel)
			if (getClusterIndexForCompoundIndex(compound) == cluster)
				return true;
		return false;
	}

	public boolean isCompoundWatchedFromCluster(int cluster)
	{
		int sel[] = compoundWatched.getSelectedIndices();
		if (sel.length == 0)
			return false;
		for (int compound : sel)
			if (getClusterIndexForCompoundIndex(compound) == cluster)
				return true;
		return false;
	}

	public SelectionModel getClusterActive()
	{
		return clusterActive;
	}

	public SelectionModel getClusterWatched()
	{
		return clusterWatched;
	}

	public SelectionModel getCompoundActive()
	{
		return compoundActive;
	}

	public SelectionModel getCompoundWatched()
	{
		return compoundWatched;
	}

	public Cluster getClusterForCompound(Compound compound)
	{
		update();
		return compoundIndexToCluster.get(compound.getCompoundIndex());
	}

	public Cluster getClusterForCompoundIndex(int compoundIndex)
	{
		update();
		return compoundIndexToCluster.get(compoundIndex);
	}

	public int indexOf(Cluster cluster)
	{
		return clusters.indexOf(cluster);
	}

	public int getClusterIndexForCompound(Compound compound)
	{
		return indexOf(getClusterForCompound(compound));
	}

	public int getClusterIndexForCompoundIndex(int compoundIndex)
	{
		return indexOf(getClusterForCompoundIndex(compoundIndex));
	}

	public Compound getCompoundWithCompoundIndex(int compoundIndex)
	{
		update();
		return compoundIndexToCompound.get(compoundIndex);
	}

	public int numMissingValues(CompoundProperty p)
	{
		update();
		if (!numMissingValues.containsKey(p))
		{
			int num = 0;
			for (Cluster c : clusters)
				num += c.numMissingValues(p);
			numMissingValues.put(p, num);
		}
		return numMissingValues.get(p);
	}

	public int numDistinctValues(CompoundProperty p)
	{
		update();
		if (!numDistinctValues.containsKey(p))
		{
			int numDistinct = p.getType() == Type.NUMERIC ? CompoundPropertyUtil.computeNumDistinct(getDoubleValues(p))
					: CompoundPropertyUtil.computeNumDistinct(getStringValues(p, null));
			numDistinctValues.put(p, numDistinct);
		}
		return numDistinctValues.get(p);
	}

	public int numCompounds()
	{
		update();
		return numCompounds;
	}

	public int numClusters()
	{
		return clusters.size();
	}

	public BitSet getBitSetAll()
	{
		update();
		return bitSetAll;
	}

	private void clearSelection()
	{
		compoundActive.clearSelection();
		compoundWatched.clearSelection();
		clusterActive.clearSelection();
		clusterWatched.clearSelection();
	}

	public void clear()
	{
		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		clearSelection();
		clusters.removeAllElements();
		clusteringData = null;
		View.instance.zap(true, true, true);
		compoundList.clear();
		compoundListIncludingMultiClusteredCompounds.clear();
		normalizedValues.clear();
		normalizedLogValues.clear();
		dirty = true;

		fire(CLUSTER_REMOVED, old, clusters);
		fire(CLUSTER_CLEAR, old, clusters);
	}

	private void removeCluster(final Cluster... clusters)
	{
		clearSelection();

		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		for (Cluster c : clusters)
		{
			View.instance.hide(c.getBitSet());
			Clustering.this.clusters.remove(c);
		}
		dirty = true;
		updatePositions();
		if (getNumClusters() == 1)
			getClusterActive().setSelected(0);
		fire(CLUSTER_REMOVED, old, clusters);
	}

	public Cluster getCluster(int clusterIndex)
	{
		if (clusterIndex < 0)
			return null;
		return clusters.get(clusterIndex);
	}

	public Iterable<Cluster> getClusters()
	{
		return clusters;
	}

	public void newClustering(ClusteringData d)
	{
		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		suppresAddEvent = true;

		clusteringData = d;

		{
			String filename;
			if (d.getNumClusters() > 1)
			{
				List<File> clusterFiles = new ArrayList<File>();
				for (int i = 0; i < d.getNumClusters(); i++)
					clusterFiles.add(new File(d.getCluster(i).getFilename()));
				filename = Settings.destinationFile("jmol_input.sdf");
				FileUtil.concat(new File(filename), clusterFiles);
			}
			else
				filename = d.getCluster(0).getFilename();
			TaskProvider.update("Loading dataset into Jmol");
			View.instance.loadCompoundFromFile(null, filename, null, null, false, null, null, 0);

			if (d.getNumCompounds(true) != View.instance.getCompoundCount())
				throw new Error("illegal num compounds, loaded by Jmol: " + View.instance.getCompoundCount()
						+ " != from wizard: " + d.getNumCompounds(true));
		}

		int num = 0;
		for (int i = 0; i < d.getNumClusters(); i++)
		{
			//String substructure = null;
			//			if (d.getClusterSubstructureSmarts() != null)
			//				substructure = d.getClusterSubstructureSmarts()[i];

			addSingleCluster(d.getCluster(i), num, num + d.getCluster(i).getSize());
			num += d.getCluster(i).getSize();
			//			TaskProvider.update("Loading cluster dataset " + (i + 1) + "/" + d.getNumClusters());
		}
		TaskProvider.update(90, "Loading graphics");

		updatePositions();

		suppresAddEvent = false;

		fire(CLUSTER_ADDED, old, clusters);
		fire(CLUSTER_NEW, old, clusters);

		View.instance.scriptWait("hover off");
	}

	public void updatePositions()
	{
		if (dirty)
			update();

		ClusteringUtil.updateScaleFactor(this);

		getClusterWatched().clearSelection();
		getCompoundWatched().clearSelection();

		Vector3f[] positions = ClusteringUtil.getClusterPositions(this);

		View.instance.suspendAnimation("updating clustering positions");
		for (int i = 0; i < positions.length; i++)
		{
			Cluster c = clusters.get(i);
			if (!superimposed)
				setClusterOverlap(c, true, null);
			c.updatePositions();
			if (!superimposed)
				setClusterOverlap(c, false, null);
		}
		View.instance.proceedAnimation("updating clustering positions");

		positions = ClusteringUtil.getClusterPositions(this);
		superimposedCenter = Vector3fUtil.centerConvexHull(positions);

		// take only cluster points into account (ignore cluster sizes)
		superimposedDiameter = Vector3fUtil.maxDist(positions);
		// needed for very small distances / only one cluster : diameter should be at least as big as cluster diameter
		for (Cluster c : clusters)
			superimposedDiameter = Math.max(superimposedDiameter, c.getDiameter(true));

		// take only compound positions into account (ignore compound sizes)
		positions = ClusteringUtil.getCompoundPositions(this);
		nonSuperimposedCenter = Vector3fUtil.centerConvexHull(positions);
		nonSuperimposedDiameter = Vector3fUtil.maxDist(positions);
	}

	/**
	 * toggles compound positions between compound position (overlap=false) and cluster position (overlap=true) 
	 * 
	 * @param clusters
	 * @param overlap
	 * @param anim
	 */
	public void setClusterOverlap(List<Cluster> clusters, boolean overlap, View.AnimationSpeed anim)
	{
		List<Vector3f> compoundPositions = new ArrayList<Vector3f>();
		List<BitSet> bitsets = new ArrayList<BitSet>();

		for (Cluster cluster : clusters)
		{
			if (cluster.isSuperimposed() != overlap)
			{
				for (int i = 0; i < cluster.size(); i++)
				{
					bitsets.add(cluster.getCompound(i).getBitSet());

					// destination is compound position
					Vector3f pos = cluster.getCompound(i).getPosition();
					// compound is already at cluster position, sub to get relative vector
					pos.sub(cluster.getCenter(true));
					if (overlap)
						pos.scale(-1);
					compoundPositions.add(pos);
				}
			}
			cluster.setSuperimposed(overlap);
		}
		View.instance.setAtomCoordRelative(compoundPositions, bitsets, anim);
	}

	/**
	 * not animated
	 * 
	 * @param comp
	 * @param enable
	 */
	public void moveForDotMode(Compound comp, boolean enable)
	{
		// move to compound center ...
		Vector3f pos = new Vector3f(comp.origCenter);
		// ... from single atom position
		pos.sub(comp.origDotPosition);
		// reverse if neccessary
		if (!enable)
			pos.scale(-1);
		View.instance.setAtomCoordRelative(pos, comp.getDotModeDisplayBitSet());
	}

	public void setClusterOverlap(Cluster cluster, boolean overlap, View.AnimationSpeed anim)
	{
		List<Cluster> l = new ArrayList<Cluster>();
		l.add(cluster);
		setClusterOverlap(l, overlap, anim);
	}

	private int[] clusterChooser(String title, String description)
	{
		int clusterIndex = getClusterActive().getSelected();
		if (clusterIndex == -1)
			clusterIndex = getClusterWatched().getSelected();

		Cluster c[] = new Cluster[numClusters()];
		for (int i = 0; i < c.length; i++)
			c[i] = getCluster(i);
		boolean b[] = new boolean[numClusters()];
		if (clusterIndex != -1)
			b[clusterIndex] = true;

		return CheckBoxSelectDialog.selectIndices(Settings.TOP_LEVEL_FRAME, title, description, c, b);
	}

	/**
	 * returns jmol compound index (not orig sdf compound index)  
	 */
	private int[] compoundChooser(String title, String description)
	{
		int clusterIndex = getClusterActive().getSelected();
		if (clusterIndex == -1)
			clusterIndex = getClusterWatched().getSelected();

		List<Compound> l = new ArrayList<Compound>();
		List<Boolean> lb = new ArrayList<Boolean>();

		for (int i = 0; i < numClusters(); i++)
		{
			Cluster c = getCluster(i);
			for (int j = 0; j < c.size(); j++)
			{
				l.add(c.getCompound(j));
				lb.add(clusterIndex == -1 || clusterIndex == i);
			}
		}
		Compound m[] = new Compound[l.size()];
		int selectedIndices[] = CheckBoxSelectDialog.selectIndices(Settings.TOP_LEVEL_FRAME, title, description,
				l.toArray(m), ArrayUtil.toPrimitiveBooleanArray(lb));
		return selectedIndices;
	}

	public void chooseClustersToRemove()
	{
		int[] indices = clusterChooser("Remove Cluster/s",
				"Select the clusters you want to remove (the original dataset is not modified).");
		if (indices != null)
		{
			Cluster c2[] = new Cluster[indices.length];
			for (int i = 0; i < indices.length; i++)
				c2[i] = getCluster(indices[i]);
			removeCluster(c2);
		}
	}

	public void chooseClustersToExport()
	{
		int[] indices = clusterChooser("Export Cluster/s",
				"Select the clusters you want to export. The compounds will be stored in a single SDF/CSV file.");
		if (indices != null)
			ExportData.exportClusters(this, indices);
	}

	public void chooseCompoundsToRemove()
	{
		int[] indices = compoundChooser("Remove Compounds/s",
				"Select the compounds you want to remove from the dataset (the original dataset is not modified).");
		if (indices == null)
			return;
		removeCompounds(indices);
	}

	public void chooseCompoundsToExport()
	{
		int indices[] = compoundChooser("Export Compounds/s",
				"Select the compounds you want to export. The compounds will be stored in a single SDF/CSV file.");
		if (indices == null)
			return;
		List<Integer> l = new ArrayList<Integer>();
		for (int i = 0; i < indices.length; i++)
			l.add(getCompoundWithCompoundIndex(indices[i]).getCompoundOrigIndex());
		ExportData.exportCompounds(this, l);
	}

	public String getName()
	{
		if (clusteringData != null)
			return clusteringData.getName();
		else
			return null;
	}

	public String getFullName()
	{
		if (clusteringData != null)
			return clusteringData.getFullName();
		else
			return null;
	}

	public String getOrigSdfFile()
	{
		return clusteringData.getSDFFilename();
	}

	public void removeSelectedCluster()
	{
		if (getClusterActive().getSelected() != -1)
			removeCluster(getClusterActive().getSelected());
		else
			removeCluster(getClusterWatched().getSelected());
	}

	public void removeCluster(int clusterIndex)
	{
		removeCluster(getCluster(clusterIndex));
	}

	public void removeSelectedCompounds()
	{
		if (getClusterWatched().getSelected() != -1)
		{
			removeCompounds(getCompoundWatched().getSelectedIndices());
		}
	}

	public void removeCompounds(int compoundIndices[])
	{
		LinkedHashMap<Cluster, List<Integer>> toDel = new LinkedHashMap<Cluster, List<Integer>>();

		// assign indices to clusters
		for (int i = 0; i < compoundIndices.length; i++)
		{
			Cluster c = getCluster(getClusterIndexForCompoundIndex(compoundIndices[i]));
			List<Integer> l = toDel.get(c);
			if (l == null)
			{
				l = new ArrayList<Integer>();
				toDel.put(c, l);
			}
			l.add(compoundIndices[i]);
		}

		// delete clusterwise
		for (Cluster c : toDel.keySet())
		{
			int indices[] = ArrayUtil.toPrimitiveIntArray(toDel.get(c));
			if (indices.length == c.size())
				removeCluster(c);
			else
			{
				compoundActive.clearSelection();
				compoundWatched.clearSelection();
				c.remove(indices);
				dirty = true;
				updatePositions();
				fire(CLUSTER_MODIFIED, null, null);
			}
		}
	}

	public List<SubstructureSmartsType> getSubstructures()
	{
		return clusteringData.getSubstructureSmartsTypes();
	}

	public List<CompoundProperty> getFeatures()
	{
		return clusteringData.getFeatures();
	}

	public List<CompoundProperty> getProperties()
	{
		return clusteringData.getProperties();
	}

	public List<CompoundData> getCompounds()
	{
		return clusteringData.getCompounds();
	}

	public int getNumClusters()
	{
		return clusters.size();
	}

	public int getNumCompounds(boolean includingMultiClusteredCompounds)
	{
		//		if (compoundList == null)//not yet computed, hence no clusters removed yet, can use complete clustering data
		//			return clusteringData.getNumCompounds(includingMultiClusteredCompounds);
		//		else
		return getCompounds(includingMultiClusteredCompounds).size();
	}

	public List<Compound> getCompounds(boolean includingMultiClusteredCompounds)
	{
		if (includingMultiClusteredCompounds)
			return compoundListIncludingMultiClusteredCompounds;
		else
			return compoundList;
	}

	public String[] getStringValues(CompoundProperty property, Compound excludeCompound)
	{
		List<String> l = new ArrayList<String>();
		for (Compound m : getCompounds(false))
			if (m != excludeCompound && m.getStringValue(property) != null)
				l.add(m.getStringValue(property));
		String v[] = new String[l.size()];
		return l.toArray(v);
	}

	public Double[] getDoubleValues(CompoundProperty property)
	{
		Double v[] = new Double[getNumCompounds(false)];
		int i = 0;
		for (Compound m : getCompounds(false))
			v[i++] = m.getDoubleValue(property);
		return v;
	}

	public String getClusterAlgorithm()
	{
		return clusteringData.getClusterAlgorithm();
	}

	public boolean isClusterAlgorithmDisjoint()
	{
		return clusteringData.isClusterAlgorithmDisjoint();
	}

	public String getEmbedAlgorithm()
	{
		return clusteringData.getEmbedAlgorithm();
	}

	public String getEmbedQuality()
	{
		return clusteringData.getEmbedQuality();
	}

	@Override
	public Vector3f getCenter(boolean superimposed)
	{
		if (superimposed)
			return superimposedCenter;
		else
			return nonSuperimposedCenter;
	}

	@Override
	public float getDiameter(boolean superimposed)
	{
		if (superimposed)
			return superimposedDiameter;
		else
			return nonSuperimposedDiameter;
	}

	@Override
	public boolean isSuperimposed()
	{
		return superimposed;
	}

	public void setSuperimposed(boolean superimposed)
	{
		this.superimposed = superimposed;
	}

	DoubleKeyHashMap<Compound, CompoundProperty, Double> normalizedValues = new DoubleKeyHashMap<Compound, CompoundProperty, Double>();
	DoubleKeyHashMap<Compound, CompoundProperty, Double> normalizedLogValues = new DoubleKeyHashMap<Compound, CompoundProperty, Double>();

	private void updateNormalizedValues(CompoundProperty p)
	{
		Double valNorm[] = null;
		Double valNormLog[] = null;
		if (p.getType() == Type.NUMERIC)
		{
			Double d[] = new Double[compoundList.size()];
			int i = 0;
			for (Compound m : compoundList)
				d[i++] = m.getDoubleValue(p);
			valNorm = ArrayUtil.normalize(d, false);
			valNormLog = ArrayUtil.normalizeLog(d, false);
		}
		else
		{
			String s[] = new String[compoundList.size()];
			int i = 0;
			for (Compound m : compoundList)
				s[i++] = m.getStringValue(p);
			valNorm = ArrayUtil.normalizeObjectArray(s);
		}
		int i = 0;
		for (Compound m : compoundList)
		{
			normalizedValues.put(m, p, valNorm[i]);
			if (valNormLog != null)
				normalizedLogValues.put(m, p, valNormLog[i++]);
		}
	}

	public double getNormalizedDoubleValue(Compound m, CompoundProperty p)
	{
		if (!normalizedValues.containsKeyPair(m, p))
			updateNormalizedValues(p);
		return normalizedValues.get(m, p);
	}

	public double getNormalizedLogDoubleValue(Compound m, CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!normalizedLogValues.containsKeyPair(m, p))
			updateNormalizedValues(p);
		return normalizedLogValues.get(m, p);
	}

	public String getOrigLocalPath()
	{
		return clusteringData.getOrigLocalPath();
	}

	//	public CompoundProperty getEmbeddingQualityProperty()
	//	{
	//		return clusteringData.getEmbeddingQualityProperty();
	//	}
	//
	//	public CompoundPropertyEmbedQuality getEmbeddingQuality(CompoundProperty p)
	//	{
	//		return clusteringData.getEmbeddingQuality(p);
	//	}

}
