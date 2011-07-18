package cluster;

import gui.CheckBoxSelectDialog;
import gui.CheSViewer;
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

import org.jmol.viewer.Viewer;
import org.omegahat.Environment.System.NotImplementedException;

import util.ArrayUtil;
import util.SelectionModel;
import util.Vector3fUtil;
import util.VectorUtil;
import data.CDKService;
import data.ClusteringData;
import dataInterface.ClusterData;
import dataInterface.ClusteringDataUtil;
import dataInterface.MoleculeProperty;

public class Clustering
{
	Viewer viewer;

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

	public Clustering(Viewer viewer)
	{
		this.viewer = viewer;
		listeners = new Vector<PropertyChangeListener>();
		init();
	}

	public void init()
	{
		clusterActive = new SelectionModel();
		clusterWatched = new SelectionModel();
		modelActive = new SelectionModel(true);
		modelWatched = new SelectionModel();
		clusters = new Vector<Cluster>();
	}

	public void clear()
	{
		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		clusterActive.clearSelection();
		clusterWatched.clearSelection();
		modelActive.clearSelection();
		modelWatched.clearSelection();
		clusters.removeAllElements();
		viewer.zap(true, true, true);
		dirty = true;
		for (PropertyChangeListener l : listeners)
			l.propertyChange(new PropertyChangeEvent(this, "removed", old, clusters));
	}

	public void addRemoveAddClusterListener(PropertyChangeListener l)
	{
		listeners.add(l);
	}

	private Cluster addSingleCluster(ClusterData clusterData)
	{

		Cluster c = new Cluster(viewer, clusterData, clusters.size() == 0, clusteringData.isClusterFilesAligned());

		@SuppressWarnings("unchecked")
		Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
		clusters.add(c);
		dirty = true;

		if (!suppresAddEvent)
			for (PropertyChangeListener l : listeners)
				l.propertyChange(new PropertyChangeEvent(this, CLUSTER_ADDED, old, clusters));

		for (Model m : c.getModels())
			modelActive.setSelected(m.getModelIndex());

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

	private void removeCluster(final Cluster... clusters)
	{
		for (Cluster c : clusters)
		{
			int i = indexOf(c);
			if (getModelWatched().getSelected() != -1
					&& getClusterIndexForModelIndex(getModelWatched().getSelected()) == i)
				getModelWatched().clearSelection();
			int[] activeModels = getModelActive().getSelectedIndices();
			for (int m : activeModels)
				if (getClusterIndexForModelIndex(m) == i)
					getModelActive().setSelected(m, false);
			if (getClusterWatched().getSelected() == i)
				getClusterWatched().clearSelection();
			if (getClusterActive().getSelected() == i)
				getClusterActive().clearSelection();
		}

		invokeAfterViewer(new Runnable()
		{
			@Override
			public void run()
			{
				@SuppressWarnings("unchecked")
				Vector<Cluster> old = (Vector<Cluster>) VectorUtil.clone(Clustering.this.clusters);
				for (Cluster c : clusters)
				{
					viewer.deleteAtoms(c.getBitSet(), false);
					Clustering.this.clusters.remove(c);
				}
				dirty = true;
				for (PropertyChangeListener l : listeners)
					l.propertyChange(new PropertyChangeEvent(this, "removed", old, Clustering.this.clusters));
			}
		});
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

	public void chooseClustersToAdd()
	{
		throw new NotImplementedException();

		//		// String filenames[] = SdfProvider.chooseSdfFiles();
		//		// if (filenames != null)
		//		// addCluster(filenames);
		//		
		//		ClusteredDataset clusteredDataset = ClusteredDatasetWorflow.chooseDataset();
		//		if (clusteredDataset != null)
		//			addCluster(clusteredDataset);
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
			if (CheSViewer.initProgress != null)
				CheSViewer.initProgress.update(66 * ((i + 1) / (double) d.getSize()),
						"Loading cluster dataset " + (i + 1));
		}
		if (CheSViewer.initProgress != null)
			CheSViewer.initProgress.update(66, "Loading graphics");

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
			JFileChooser f = new JFileChooser(clusteringData.getFilename());//origSDFFile);
			int i = f.showSaveDialog(Settings.TOP_LEVEL_COMPONENT);
			if (i == JFileChooser.APPROVE_OPTION)
			{
				String dest = f.getSelectedFile().getAbsolutePath();
				CDKService.clear(dest);
				SDFUtil.filter(clusteringData.getFilename(), dest, ArrayUtil.toPrimitiveIntArray(l));
			}
		}
	}

	public String getName()
	{
		return clusteringData.getName();
	}

	public String getOrigSdfFile()
	{
		return clusteringData.getFilename();
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

	Thread th;
	Thread oldThread;

	public void invokeAfterViewer(Runnable r)
	{
		invokeAfterViewer(false, r);
	}

	public void invokeAfterViewer(final boolean sleep, final Runnable r)
	{
		if (th == null)
			oldThread = th;

		th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				if (oldThread != null && oldThread.isAlive())
				{
					System.err.println("waiting for old Thread to die");
					try
					{
						oldThread.join();
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
					System.err.println("old Thread died");
				}
				viewer.scriptWait("");
				if (sleep)
				{
					try
					{
						System.out.print("intentional sleep ...");
						Thread.sleep(5000);
						System.out.println("over");
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				r.run();
			}
		});
		th.start();
	}

	public void setTemperature(MoleculeProperty highlightProperty)
	{
		for (Cluster c : clusters)
			c.setTemperature(highlightProperty);
	}

	public boolean hasSubstructures()
	{
		return clusters.get(0).getSubstructureSmarts() != null;
	}

	//	public static interface SuperImposeListener
	//	{
	//		public void superimpose(boolean superimpose);
	//	}

	//	List<SuperImposeListener> superImposeListeners = new ArrayList<SuperImposeListener>();
	//
	//	public void addSuperimposeListener(SuperImposeListener l)
	//	{
	//		superImposeListeners.add(l);
	//	}

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
		return clusteringData.getSize();
	}

}
