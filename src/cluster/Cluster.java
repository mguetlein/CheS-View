package cluster;

import gui.View;
import gui.View.MoveAnimation;
import gui.ViewControler.HighlightSorting;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.vecmath.Vector3f;

import util.ArrayUtil;
import util.Vector3fUtil;
import dataInterface.ClusterData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.SubstructureSmartsType;

public class Cluster
{
	private Vector<Model> models;
	private ClusterData clusterData;

	private boolean dirty = true;
	private BitSet bitSet;
	private Vector3f center;

	private boolean overlap = true;
	private boolean superimposed = false;
	private int radius;

	private Vector3f position;

	HashMap<String, List<Model>> modelsOrderedByPropterty = new HashMap<String, List<Model>>();

	private boolean watched;
	private boolean visible;
	private MoleculeProperty highlightProp;
	private HighlightSorting highlightSorting;
	private boolean someModelsHidden;

	public Cluster(dataInterface.ClusterData clusterData, boolean firstCluster)
	{
		this.clusterData = clusterData;

		int before = firstCluster ? 0 : View.instance.getModelCount();
		View.instance.loadModelFromFile(null, clusterData.getFilename(), null, null, !firstCluster, null, null, 0);
		int after = View.instance.getModelCount();

		if ((after - before) != clusterData.getSize())
			throw new IllegalStateException("models in file: " + (after - before) + " != model props passed: "
					+ clusterData.getSize());

		models = new Vector<Model>();
		int mCount = 0;
		for (int i = before; i < after; i++)
			addModel(new Model(i, clusterData.getCompounds().get(mCount++))); //  modelOrigIndices[mCount], props, nProps));
	}

	public void updatePositions()
	{
		boolean overlap = this.overlap;
		boolean superimposed = this.superimposed;
		if (!overlap)
			setOverlap(true, MoveAnimation.NONE, false);

		Vector3f positions[] = ClusterUtil.getModelPositions(this, true);
		float scale = ClusterUtil.getScaleFactor(positions);
		//		System.out.println("SSSSSSSSSSSSSSSSSSSSSSSSSSS model scaling: " + scale);

		//		for (Vector3f v3f : positions)
		//			v3f.scale(scale);
		for (int i = 0; i < positions.length; i++)
		{
			Vector3f pos = new Vector3f(positions[i]);
			pos.scale(scale);
			models.get(i).setPosition(pos);
		}

		positions = ClusterUtil.getModelPositions(this, false);
		radius = (int) (Vector3fUtil.maxDist(positions));

		resetCenter();

		translate(getPosition());
		if (!clusterData.isAligned())
			for (Model m : models)
				m.moveTo(getPosition());

		if (!overlap)
			setOverlap(false, MoveAnimation.NONE, superimposed);
	}

	public String getSummaryStringValue(MoleculeProperty property)
	{
		return clusterData.getSummaryStringValue(property);
	}

	public String toString()
	{
		return getName();
	}

	public void updateValues()
	{
		if (!dirty)
			return;

		nonOverlapCenter = null;
		bitSet = new BitSet();
		for (Model m : models)
			bitSet.or(m.getBitSet());
		center = new Vector3f(View.instance.getAtomSetCenter(bitSet));
		Vector3f positions[] = ClusterUtil.getModelPositions(this, false);
		radius = (int) (Vector3fUtil.maxDist(positions));
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
		updateValues();
		return bitSet;
	}

	public Vector3f getCenter()
	{
		updateValues();
		return center;
	}

	Vector3f nonOverlapCenter;

	public Vector3f getNonOverlapCenter()
	{
		updateValues();
		//		if (nonOverlapCenter == null)
		//		{
		if (!overlap)
		{
			nonOverlapCenter = getCenter();
			System.out.println("exact " + nonOverlapCenter);
		}
		else if (nonOverlapCenter == null)
		{
			Vector3f c = new Vector3f(getCenter());
			c.add(Vector3fUtil.center(ClusterUtil.getModelPositions(this, false)));
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

	public List<Model> getModelsInOrder(final MoleculeProperty property, HighlightSorting sorting)
	{
		String key = property + "_" + sorting;
		if (!modelsOrderedByPropterty.containsKey(key))
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
					else if (property.getType() == Type.NUMERIC)
					{
						Double d1 = o1.getDoubleValue(property);
						Double d2 = o2.getDoubleValue(property);
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
							res = d1.compareTo(d2);
					}
					else
						res = (o1.getStringValue(property) + "").compareTo(o2.getStringValue(property) + "");
					return (finalSorting == HighlightSorting.Max ? -1 : 1) * res;
				}
			});
			if (sorting == HighlightSorting.Med)
			{
				//				System.err.println("max order: ");
				//				for (Model mm : m)
				//					System.err.print(mm.getStringValue(property) + " ");
				//				System.err.println();

				/**
				 * median sorting:
				 * - first order by max to compute median
				 * - create a dist-to-median array, sort models according to that array
				 */
				Model medianModel = m.get(m.size() / 2);
				//				System.err.println(medianModel.getStringValue(property));
				double distToMedian[] = new double[m.size()];
				if (property.getType() == Type.NUMERIC)
				{
					Double med = medianModel.getDoubleValue(property);
					for (int i = 0; i < distToMedian.length; i++)
					{
						Double d = m.get(i).getDoubleValue(property);
						if (med == null)
						{
							if (d == null)
								distToMedian[i] = 0;
							else
								distToMedian[i] = Double.MAX_VALUE;
						}
						else if (d == null)
							distToMedian[i] = Double.MAX_VALUE;
						else
							distToMedian[i] = Math.abs(med - d);
					}
				}
				else
				{
					String medStr = medianModel.getStringValue(property);
					for (int i = 0; i < distToMedian.length; i++)
						distToMedian[i] = Math.abs((m.get(i).getStringValue(property) + "").compareTo(medStr + ""));
				}
				int order[] = ArrayUtil.getOrdering(distToMedian, true);
				Model a[] = new Model[m.size()];
				Model s[] = ArrayUtil.sortAccordingToOrdering(order, m.toArray(a));
				m = ArrayUtil.toList(s);

				//				System.err.println("med order: ");
				//				for (Model mm : m)
				//					System.err.print(mm.getStringValue(property) + " ");
				//				System.err.println();
			}
			modelsOrderedByPropterty.put(key, m);
		}
		//		System.err.println("in order: ");
		//		for (Model m : order.get(key))
		//			System.err.print(m.getModelOrigIndex() + " ");
		//		System.err.println("");
		return modelsOrderedByPropterty.get(key);
	}

	public String getName()
	{
		return clusterData.getName();
	}

	public void resetCenter()
	{
		updateValues();
		Vector3f c = new Vector3f(center);
		c.negate();
		// viewer.setAtomCoord(bitSet, Token.xyz, c);
		View.instance.setAtomCoordRelative(c, bitSet);
		dirty = true;
	}

	public void translate(Vector3f newCenter)
	{
		Vector3f v = new Vector3f(newCenter);
		View.instance.setAtomCoordRelative(v, getBitSet());
		dirty = true;
	}

	/**
	 * the compounds are drawn on the same location (because cluster view or manually superimposed)
	 * 
	 * @return
	 */
	public boolean isOverlap()
	{
		return overlap;
	}

	/**
	 * the compounds are drawn on the same location manually superimposed 
	 * 
	 * @return
	 */
	public boolean isSuperimposed()
	{
		return superimposed;
	}

	public void setOverlap(boolean overlap, View.MoveAnimation anim, boolean superimposed)
	{
		if (this.overlap != overlap)
		{
			this.overlap = overlap;
			final Vector3f modelPositions[] = ClusterUtil.getModelPositions(this, false);

			BitSet bitsets[] = new BitSet[modelPositions.length];
			for (int j = 0; j < modelPositions.length; j++)
				bitsets[j] = models.get(j).getBitSet();
			View.instance.setAtomCoordRelative(modelPositions, bitsets, anim);

			View.instance.afterAnimation(new Runnable()
			{
				@Override
				public void run()
				{
					for (Vector3f vector3f : modelPositions)
						vector3f.scale(-1);
					dirty = true;
				}
			}, "after superimpose: invert vectors");
		}
		if (superimposed && !overlap)
			throw new IllegalArgumentException();
		this.superimposed = superimposed;
	}

	public int getRadius()
	{
		updateValues();
		return radius;
	}

	public String getSubstructureSmarts(SubstructureSmartsType type)
	{
		return clusterData.getSubstructureSmarts(type);
	}

	public Vector3f getPosition()
	{
		return position;
	}

	public void setPosition(Vector3f pos)
	{
		position = pos;
	}

	public Vector3f getOrigPosition()
	{
		return clusterData.getPosition();
	}

	public void remove(int[] modelIndices)
	{
		List<Model> toDel = new ArrayList<Model>();
		for (int i : modelIndices)
			toDel.add(getModelWithModelIndex(i));
		BitSet bs = new BitSet();
		for (Model m : toDel)
		{
			bs.or(m.getBitSet());
			models.remove(m);
		}
		View.instance.hide(bs);

		modelsOrderedByPropterty.clear();
		dirty = true;

		if (models.size() == 1)
		{
			models.get(0).setOrigPosition(new Vector3f(0, 0, 0));
			updatePositions();
		}
	}

	public boolean isWatched()
	{
		return watched;
	}

	public void setWatched(boolean watched)
	{
		this.watched = watched;
	}

	public boolean isVisible()
	{
		return visible;
	}

	public void setVisible(boolean visible)
	{
		this.visible = visible;
	}

	public void setHighlighProperty(MoleculeProperty highlightProp)
	{
		this.highlightProp = highlightProp;
	}

	public MoleculeProperty getHighlightProperty()
	{
		return highlightProp;
	}

	public void setHighlightSorting(HighlightSorting highlightSorting)
	{
		this.highlightSorting = highlightSorting;
	}

	public HighlightSorting getHighlightSorting()
	{
		return highlightSorting;
	}

	public void setSomeModelsHidden(boolean someModelsHidden)
	{
		this.someModelsHidden = someModelsHidden;
	}

	public boolean someModelsHidden()
	{
		return someModelsHidden;
	}

	public String[] getStringValues(MoleculeProperty property)
	{
		String v[] = new String[models.size()];
		for (int i = 0; i < v.length; i++)
			v[i] = models.get(i).getStringValue(property);
		return v;
	}

	public Double[] getDoubleValues(MoleculeProperty property)
	{
		Double v[] = new Double[models.size()];
		for (int i = 0; i < v.length; i++)
			v[i] = models.get(i).getDoubleValue(property);
		return v;
	}

}
