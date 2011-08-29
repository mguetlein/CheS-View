package cluster;

import gui.CheckBoxSelectDialog;
import gui.View;
import io.SDFUtil;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.vecmath.Vector3f;

import main.Settings;
import main.TaskProvider;
import util.ArrayUtil;
import util.SelectionModel;
import util.Vector3fUtil;
import util.VectorUtil;
import data.ClusteringData;
import data.DatasetFile;
import dataInterface.ClusterData;
import dataInterface.ClusteringDataUtil;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class Clustering
{
	private Vector<Cluster> clusters;
	private ClusteringData clusteringData;

	SelectionModel clusterActive;
	SelectionModel clusterWatched;
	SelectionModel modelActive;
	SelectionModel modelWatched;

	boolean suppresAddEvent = false;
	Vector<PropertyChangeListener> listeners;
	public static String CLUSTER_ADDED = "cluster_added";

	boolean dirty = true;
	int numModels = -1;
	float radius;

	BitSet bitSetAll;
	HashMap<Integer, Model> modelIndexToModel;
	HashMap<Integer, Cluster> modelIndexToCluster;

	Vector3f center;

	public Clustering()
	{
		listeners = new Vector<PropertyChangeListener>();
		init();
	}

	public void init()
	{
		clusterActive = new SelectionModel();
		clusterWatched = new SelectionModel();
		modelActive = new SelectionModel();
		modelWatched = new SelectionModel();
		clusters = new Vector<Cluster>();
	}

	public void addRemoveAddClusterListener(PropertyChangeListener l)
	{
		listeners.add(l);
	}

	private Cluster addSingleCluster(ClusterData clusterData)
	{

		Cluster c = new Cluster(clusterData, clusters.size() == 0);

		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		clusters.add(c);
		dirty = true;

		if (!suppresAddEvent)
			for (PropertyChangeListener l : listeners)
				l.propertyChange(new PropertyChangeEvent(this, CLUSTER_ADDED, old, clusters));

		//		for (Model m : c.getModels())
		//			modelActive.setSelected(m.getModelIndex());

		return c;
	}

	public void update()
	{
		if (!dirty)
			return;

		numModels = 0;
		for (Cluster c : clusters)
			numModels += c.size();

		bitSetAll = new BitSet();
		for (Cluster c : clusters)
			bitSetAll.or(c.getBitSet());

		modelIndexToCluster = new HashMap<Integer, Cluster>();
		modelIndexToModel = new HashMap<Integer, Model>();
		for (Cluster c : clusters)
		{
			for (Model m : c.getModels())
			{
				modelIndexToModel.put(m.modelIndex, m);
				modelIndexToCluster.put(m.modelIndex, c);
			}
		}

		dirty = false;
	}

	public SelectionModel getClusterActive()
	{
		return clusterActive;
	}

	public SelectionModel getClusterWatched()
	{
		return clusterWatched;
	}

	public SelectionModel getModelActive()
	{
		return modelActive;
	}

	public SelectionModel getModelWatched()
	{
		return modelWatched;
	}

	public Cluster getClusterForModel(Model model)
	{
		update();
		return modelIndexToCluster.get(model.getModelIndex());
	}

	public Cluster getClusterForModelIndex(int modelIndex)
	{
		update();
		return modelIndexToCluster.get(modelIndex);
	}

	public int indexOf(Cluster cluster)
	{
		return clusters.indexOf(cluster);
	}

	public int getClusterIndexForModel(Model model)
	{
		return indexOf(getClusterForModel(model));
	}

	public int getClusterIndexForModelIndex(int modelIndex)
	{
		return indexOf(getClusterForModelIndex(modelIndex));
	}

	public Model getModelWithModelIndex(int modelIndex)
	{
		update();
		return modelIndexToModel.get(modelIndex);
	}

	public int numModels()
	{
		update();
		return numModels;
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
		modelActive.clearSelection();
		modelWatched.clearSelection();
		clusterActive.clearSelection();
		clusterWatched.clearSelection();
	}

	public void clear()
	{
		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		clearSelection();
		clusters.removeAllElements();
		View.instance.zap(true, true, true);
		dirty = true;
		for (PropertyChangeListener l : listeners)
			l.propertyChange(new PropertyChangeEvent(this, "removed", old, clusters));
	}

	private void removeCluster(final Cluster... clusters)
	{
		clearSelection();

		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		for (Cluster c : clusters)
		{
			View.instance.deleteAtoms(c.getBitSet(), false);
			Clustering.this.clusters.remove(c);
		}
		dirty = true;
		for (PropertyChangeListener l : listeners)
			l.propertyChange(new PropertyChangeEvent(this, "removed", old, Clustering.this.clusters));
	}

	private void removeModel(Model m)
	{
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

		Vector3f[] positions = ClusteringDataUtil.getClusterPositions(d);
		float scale = ClusterUtil.getScaleFactor(positions);

		for (int i = 0; i < d.getSize(); i++)
		{
			Vector3f pos = d.getCluster(i).getPosition();
			pos.scale(scale);

			//String substructure = null;
			//			if (d.getClusterSubstructureSmarts() != null)
			//				substructure = d.getClusterSubstructureSmarts()[i];

			addSingleCluster(d.getCluster(i));
			TaskProvider.task().update("Loading cluster dataset " + (i + 1));
		}
		TaskProvider.task().update(90, "Loading graphics");

		radius = Vector3fUtil.maxDist(positions);
		center = Vector3fUtil.center(positions);

		suppresAddEvent = false;
		for (PropertyChangeListener l : listeners)
			l.propertyChange(new PropertyChangeEvent(this, CLUSTER_ADDED, old, Clustering.this.clusters));
	}

	private int[] clusterChooser(String title, String description, boolean allSelected)
	{
		Cluster c[] = new Cluster[numClusters()];
		for (int i = 0; i < c.length; i++)
			c[i] = getCluster(i);
		return CheckBoxSelectDialog.select((JFrame) Settings.TOP_LEVEL_COMPONENT, title, description, c, allSelected);
	}

	public void chooseClustersToRemove()
	{
		int[] indices = clusterChooser("Remove Cluster/s", "Select the clusters you want to remove from the dataset.",
				false);
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
				"Select the clusters you want to export. The compounds will be stored in a single SDF file.", true);
		if (indices != null)
		{
			List<Integer> l = new ArrayList<Integer>();
			for (int i = 0; i < indices.length; i++)
				for (Model m : getCluster(indices[i]).getModels())
					l.add(m.getModelOrigIndex());
			JFileChooser f = new JFileChooser(clusteringData.getSDFFilename());//origSDFFile);
			int i = f.showSaveDialog(Settings.TOP_LEVEL_COMPONENT);
			if (i == JFileChooser.APPROVE_OPTION)
			{
				String dest = f.getSelectedFile().getAbsolutePath();
				// file may be overwritten, and then reloaded -> clear
				DatasetFile.clearFilesWith3DSDF(dest);
				SDFUtil.filter(clusteringData.getSDFFilename(), dest, ArrayUtil.toPrimitiveIntArray(l));
			}
		}
	}

	public String getName()
	{
		return clusteringData.getName();
	}

	public String getOrigSdfFile()
	{
		return clusteringData.getSDFFilename();
	}

	public void removeSelectedCluster()
	{
		if (getClusterActive().getSelected() != -1)
			removeCluster(getCluster(getClusterActive().getSelected()));
		else
			removeCluster(getCluster(getClusterWatched().getSelected()));
	}

	public void removeSelectedModel()
	{
		if (getClusterWatched().getSelected() != -1)
		{
			removeModel(getModelWithModelIndex(getModelWatched().getSelected()));
		}
	}

	public void setTemperature(MoleculeProperty highlightProperty)
	{
		for (Cluster c : clusters)
			c.setTemperature(highlightProperty);
	}

	public List<SubstructureSmartsType> getSubstructures()
	{
		return clusteringData.getSubstructureSmartsTypes();
	}

	public float getRadius()
	{
		return radius;
	}

	public List<MoleculeProperty> getFeatures()
	{
		return clusteringData.getFeatures();
	}

	public List<MoleculeProperty> getProperties()
	{
		return clusteringData.getProperties();
	}

	public Vector3f getCenter()
	{
		return center;
	}

	public int getNumClusters()
	{
		return clusters.size();
	}

}
