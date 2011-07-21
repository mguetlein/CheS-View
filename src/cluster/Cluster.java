package cluster;

import gui.ViewControler.HighlightSorting;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.vecmath.Vector3f;

import org.jmol.viewer.Viewer;

import util.Vector3fUtil;
import dataInterface.ClusterData;
import dataInterface.ClusteringDataUtil;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class Cluster
{
	Viewer viewer;
	Vector<Model> models;
	ClusterData clusterData;

	boolean dirty = true;
	BitSet bitSet;
	Vector3f center;

	boolean overlap = true;
	double maxDist;
	int radius;

	public Cluster(Viewer viewer, dataInterface.ClusterData clusterData, boolean firstCluster, boolean clusterAligend)
	{
		this.viewer = viewer;
		this.clusterData = clusterData;

		int before = firstCluster ? 0 : viewer.getModelCount();
		viewer.loadModelFromFile(null, clusterData.getFilename(), null, null, !firstCluster, null, null, 0);
		int after = viewer.getModelCount();

		Vector3f positions[] = ClusteringDataUtil.getCompoundPositions(clusterData);

		if ((after - before) != positions.length)
			throw new IllegalStateException("models in file: " + (after - before) + " != model props passed: "
					+ positions.length);

		float scale = ClusterUtil.getScaleFactor(positions);
		//		System.out.println("SSSSSSSSSSSSSSSSSSSSSSSSSSS model scaling: " + scale);
		for (Vector3f v3f : positions)
			v3f.scale(scale);
		radius = (int) (Vector3fUtil.maxDist(positions));

		//		HashMap<String, Object> props = new LinkedHashMap<String, Object>();
		//		int pCount = 0;
		//		for (Object o : clusterProperties)
		//			props.put(properties[pCount++], o);

		models = new Vector<Model>();
		int mCount = 0;
		for (int i = before; i < after; i++)
		{
			//			LinkedHashMap<String, Double> nProps = new LinkedHashMap<String, Double>();
			//			props = new LinkedHashMap<String, Object>();
			//			pCount = 0;
			//			for (Object o : modelValues.get(mCount))
			//				props.put(properties[pCount++], o);
			//			pCount = 0;
			//			for (Object d : normalizedValues.get(mCount))
			//				nProps.put(properties[pCount++], (Double) d);

			addModel(new Model(viewer, i, clusterData.getCompounds().get(mCount++))); //  modelOrigIndices[mCount], props, nProps));
			//			mCount++;
		}

		//		CDKService.computeSmiles(filePath, c.getModelsSmilesOwner());
		//		c.
		resetCenter();

		translate(clusterData.getPosition());
		if (!clusterAligend)
			for (Model m : models)
				m.moveTo(clusterData.getPosition());

		//		initModelPositions();

		//		this.name = name;
		//		this.viewer = viewer;
		//		this.propertyNames = ArrayUtil.reverse(propertyNames);
		//		this.properties = properties;
		//		this.radius = radius;
		//		this.substructureSmarts = substructureSmarts;
	}

	//	public int numProperties()
	//	{
	//		return properties.size();
	//	}
	//
	//	public Iterable<String> getProperties()
	//	{
	//		return properties.keySet();
	//	}
	//
	public Object getProperty(MoleculeProperty property)
	{
		return clusterData.getObjectValue(property, false);
	}

	public String toString()
	{
		return getName();
	}

	public void update()
	{
		if (!dirty)
			return;

		bitSet = new BitSet();
		for (Model m : models)
			bitSet.or(m.getBitSet());
		center = new Vector3f(viewer.getAtomSetCenter(bitSet));

		dirty = false;
	}

	public void addModel(Model model)
	{
		models.add(model);
		dirty = true;
	}

	public Model getModelWithModelIndex(int modelIndex)
	{
		for (Model m : models)
			if (m.getModelIndex() == modelIndex)
				return m;
		return null;
	}

	public int getIndex(Model model)
	{
		return models.indexOf(model);
	}

	public Model getModel(int index)
	{
		return models.get(index);
	}

	public int size()
	{
		return models.size();
	}

	public boolean contains(Model model)
	{
		return models.contains(model);
	}

	public BitSet getBitSet()
	{
		update();
		return bitSet;
	}

	public Vector3f getCenter()
	{
		update();
		return center;
	}

	Vector3f nonOverlapCenter;

	public Vector3f getNonOverlapCenter()
	{
		//		if (nonOverlapCenter == null)
		//		{
		if (!overlap)
		{
			nonOverlapCenter = getCenter();
			System.out.println("exact " + nonOverlapCenter);
		}
		else
		{
			Vector3f c = new Vector3f(getCenter());
			c.add(Vector3fUtil.center(ClusteringDataUtil.getCompoundPositions(clusterData)));
			nonOverlapCenter = c;
			System.out.println("computed " + nonOverlapCenter);
		}
		//		}
		return nonOverlapCenter;
	}

	public boolean containsModelIndex(int modelIndex)
	{
		for (Model m : models)
			if (m.getModelIndex() == modelIndex)
				return true;
		return false;
	}

	public List<Model> getModels()
	{
		return models;
	}

	HashMap<String, List<Model>> order = new HashMap<String, List<Model>>();

	public List<Model> getModelsInOrder(final MoleculeProperty property, HighlightSorting sorting)
	{
		String key = property + "_" + sorting;
		if (!order.containsKey(key))
		{
			List<Model> m = new ArrayList<Model>();
			for (Model model : models)
				m.add(model);
			final HighlightSorting finalSorting;
			if (sorting == HighlightSorting.Med)
				finalSorting = HighlightSorting.Max;
			else
				finalSorting = sorting;
			Collections.sort(m, new Comparator<Model>()
			{
				@Override
				public int compare(Model o1, Model o2)
				{
					int res;
					if (o1 == null)
					{
						if (o2 == null)
							res = 0;
						else
							res = 1;
					}
					else if (o2 == null)
						res = -1;
					else if (property.isNumeric())
					{
						Double d1 = (Double) o1.getTemperature(property);
						Double d2 = (Double) o2.getTemperature(property);
						if (d1 == null)
						{
							if (d2 == null)
								res = 0;
							else
								res = 1;
						}
						else if (d2 == null)
							res = -1;
						else
							res = ((Double) o1.getTemperature(property)).compareTo((Double) o2.getTemperature(property));
					}
					else
						res = (o1.getTemperature(property) + "").compareTo(o2.getTemperature(property) + "");
					return (finalSorting == HighlightSorting.Max ? -1 : 1) * res;
				}
			});
			if (sorting == HighlightSorting.Med)
			{
				Model medianModel = m.get(m.size() / 2);
				m.clear();
				for (Model model : models)
					m.add(model);
				m.remove(medianModel);
				m.add(0, medianModel);
			}
			order.put(key, m);
		}
		//		System.err.println("in order: ");
		//		for (Model m : order.get(key))
		//			System.err.print(m.getModelOrigIndex() + " ");
		//		System.err.println("");
		return order.get(key);
	}

	public String getName()
	{
		return clusterData.getName();
	}

	public void resetCenter()
	{
		update();
		Vector3f c = new Vector3f(center);
		c.negate();
		// viewer.setAtomCoord(bitSet, Token.xyz, c);
		viewer.setAtomCoordRelative(c, bitSet);

		dirty = true;
	}

	public void translate(Vector3f newCenter)
	{
		Vector3f v = new Vector3f(newCenter);
		viewer.setAtomCoordRelative(v, getBitSet());
		dirty = true;
	}

	public boolean isOverlap()
	{
		return overlap;
	}

	public static enum OverlapAnimation
	{
		SLOW, FAST, NONE
	}

	public void setOverlap(boolean overlap, OverlapAnimation anim)
	{
		if (this.overlap != overlap)
		{
			this.overlap = overlap;
			Vector3f modelPositions[] = ClusteringDataUtil.getCompoundPositions(clusterData);

			if (anim == OverlapAnimation.NONE)
			{
				for (int j = 0; j < modelPositions.length; j++)
					viewer.setAtomCoordRelative(modelPositions[j], models.get(j).getBitSet());
			}
			else
			{
				int n = (anim == OverlapAnimation.SLOW) ? 12 : 25;
				for (int i = 0; i < n; i++)
				{
					for (int j = 0; j < modelPositions.length; j++)
					{
						Vector3f v = new Vector3f(modelPositions[j]);
						v.scale(1 / (float) n);
						viewer.setAtomCoordRelative(v, models.get(j).getBitSet());
					}
					viewer.scriptWait("delay 0.01");
				}
			}

			for (Vector3f vector3f : modelPositions)
				vector3f.scale(-1);

			dirty = true;
		}
	}

	public void modelIndexOffset(int offset)
	{
		for (Model m : models)
			m.modelIndexOffset(offset);
	}

	public void setTemperature(MoleculeProperty highlightProperty)
	{
		for (Model m : models)
			m.setTemperature(highlightProperty);
	}

	public int getRadius()
	{
		return radius;
	}

	public String getSubstructureSmarts(SubstructureSmartsType type)
	{
		return clusterData.getSubstructureSmarts(type);
	}

}
