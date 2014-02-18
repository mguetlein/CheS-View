package cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.TaskProvider;
import util.ArraySummary;
import util.ArrayUtil;
import util.CountedSet;
import util.DoubleArraySummary;
import util.DoubleKeyHashMap;
import util.ListUtil;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyOwner;
import dataInterface.CompoundPropertySpecificity;

public class ClusteringValues
{
	HashMap<CompoundProperty, ArraySummary> summarys = new HashMap<CompoundProperty, ArraySummary>();
	HashMap<CompoundProperty, ArraySummary> formattedSummarys = new HashMap<CompoundProperty, ArraySummary>();
	DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double> normalizedValues = new DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double>();
	DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double> specificity = new DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double>();
	DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double> normalizedLogValues = new DoubleKeyHashMap<CompoundPropertyOwner, CompoundProperty, Double>();

	HashMap<CompoundProperty, double[]> specNumVals = new HashMap<CompoundProperty, double[]>();
	DoubleKeyHashMap<CompoundGroupWithProperties, CompoundProperty, double[]> specNumClusterVals = new DoubleKeyHashMap<CompoundGroupWithProperties, CompoundProperty, double[]>();

	Clustering clustering;

	public ClusteringValues(Clustering clustering)
	{
		this.clustering = clustering;
	}

	void clear()
	{
		normalizedLogValues.clear();
		normalizedValues.clear();
		specificity.clear();
		summarys.clear();
		formattedSummarys.clear();
	}

	private synchronized void updateNormalizedNumericValues(final CompoundProperty p)
	{
		Double d[] = new Double[clustering.getCompounds(true).size()];
		int i = 0;
		for (Compound m : clustering.getCompounds(true))
			d[i++] = m.getDoubleValue(p);
		summarys.put(p, DoubleArraySummary.create(d));
		Double valNorm[] = ArrayUtil.normalize(d, false);
		Double valNormLog[] = ArrayUtil.normalizeLog(d, false);
		specNumVals.put(p, ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(valNorm)));

		normalizedValues.put(clustering, p, DoubleArraySummary.create(valNorm).getMean());
		normalizedLogValues.put(clustering, p, DoubleArraySummary.create(valNormLog).getMean());
		HashMap<Cluster, List<Double>> clusterVals = new HashMap<Cluster, List<Double>>();
		HashMap<Cluster, List<Double>> clusterValsLog = new HashMap<Cluster, List<Double>>();
		for (Cluster c : clustering.getClusters())
		{
			clusterVals.put(c, new ArrayList<Double>());
			clusterValsLog.put(c, new ArrayList<Double>());
		}
		i = 0;
		for (Compound m : clustering.getCompounds(true))
		{
			normalizedValues.put(m, p, valNorm[i]);
			normalizedLogValues.put(m, p, valNormLog[i]);
			if (valNorm[i] != null)
				for (Cluster c : clustering.getClusters())
					if (c.contains(m))
					{
						clusterVals.get(c).add(valNorm[i]);
						clusterValsLog.get(c).add(valNormLog[i]);
					}
			i++;
		}
		for (Cluster c : clustering.getClusters())
		{
			normalizedValues.put(c, p, DoubleArraySummary.create(clusterVals.get(c)).getMean());
			normalizedLogValues.put(c, p, DoubleArraySummary.create(clusterValsLog.get(c)).getMean());
			specNumClusterVals.put(c, p, ArrayUtil.toPrimitiveDoubleArray(clusterVals.get(c)));
		}
	}

	private synchronized void updateNormalizedNumericSelectionValues(CompoundProperty p, CompoundSelection s)
	{
		if (!normalizedValues.containsKeyPair(s.getCompounds()[0], p))
			updateNormalizedValues(p);

		List<Double> clusterVals = new ArrayList<Double>();
		List<Double> clusterValsLog = new ArrayList<Double>();
		for (Compound m : s.getCompounds())
		{
			if (getNormalizedDoubleValue(m, p) != null)
			{
				clusterVals.add(getNormalizedDoubleValue(m, p));
				clusterValsLog.add(getNormalizedLogDoubleValue(m, p));
			}
		}
		normalizedValues.put(s, p, DoubleArraySummary.create(clusterVals).getMean());
		normalizedLogValues.put(s, p, DoubleArraySummary.create(clusterValsLog).getMean());
		specNumClusterVals.put(s, p, ArrayUtil.toPrimitiveDoubleArray(clusterVals));
	}

	private synchronized double numericClusterSpec(CompoundGroupWithProperties c, CompoundProperty p)
	{
		if (!specificity.containsKeyPair(c, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNumericValues(p);
			if (c.size() == 0)
				specificity.put(c, p, CompoundPropertySpecificity.NO_SPEC_AVAILABLE);
			else
				specificity.put(
						c,
						p,
						CompoundPropertySpecificity.numericMultiSpecificty(specNumClusterVals.get(c, p),
								specNumVals.get(p)));
		}
		return specificity.get(c, p);
	}

	private synchronized double numericCompoundSpec(Compound m, CompoundProperty p)
	{
		if (!specificity.containsKeyPair(m, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNumericValues(p);
			if (normalizedValues.get(m, p) == null)
				specificity.put(m, p, CompoundPropertySpecificity.NO_SPEC_AVAILABLE);
			else
				specificity.put(
						m,
						p,
						CompoundPropertySpecificity.numericSingleSpecificty(normalizedValues.get(m, p),
								specNumVals.get(p)));
		}
		return specificity.get(m, p);
	}

	HashMap<CompoundProperty, List<String>> specNomVals = new HashMap<CompoundProperty, List<String>>();
	HashMap<CompoundProperty, long[]> specNomCounts = new HashMap<CompoundProperty, long[]>();

	private synchronized void updateNormalizedNominalValues(final CompoundProperty p)
	{
		String s[] = new String[clustering.getCompounds(true).size()];
		int i = 0;
		for (Compound m : clustering.getCompounds(true))
			s[i++] = m.getStringValue(p);
		CountedSet<String> set = CountedSet.create(s);
		summarys.put(p, set);
		CountedSet<String> fSet = set.copy();
		for (String key : fSet.values())
			fSet.rename(key, p.getFormattedValue(key));
		formattedSummarys.put(p, fSet);

		specNomVals.put(p, set.values());
		specNomCounts.put(p, CompoundPropertySpecificity.nominalCounts(specNomVals.get(p), set));
	}

	private synchronized double nominalClusterSpec(CompoundGroupWithProperties c, CompoundProperty p)
	{
		if (!specificity.containsKeyPair(c, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNominalValues(p);
			if (c.size() == 0)
				specificity.put(c, p, CompoundPropertySpecificity.NO_SPEC_AVAILABLE);
			else
				specificity.put(c, p, CompoundPropertySpecificity.nominalSpecificty(
						CompoundPropertySpecificity.nominalCounts(specNomVals.get(p), c.getNominalSummary(p)),
						specNomCounts.get(p)));
		}
		return specificity.get(c, p);
	}

	private synchronized double nominalCompoundSpec(Compound m, CompoundProperty p)
	{
		if (!specificity.containsKeyPair(m, p))
		{
			if (!summarys.containsKey(p))
				updateNormalizedNominalValues(p);
			specificity.put(m, p, CompoundPropertySpecificity.nominalSpecificty(
					CompoundPropertySpecificity.nominalCount(specNomVals.get(p), m.getStringValue(p)),
					specNomCounts.get(p)));
		}
		return specificity.get(m, p);
	}

	private void updateNormalizedValues(CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			updateNormalizedNumericValues(p);
		else
			updateNormalizedNominalValues(p);
	}

	private void updateNormalizedSelectionValues(CompoundProperty p, CompoundSelection s)
	{
		if (p.getType() == Type.NUMERIC)
			updateNormalizedNumericSelectionValues(p, s);
		//		else
		//			updateNormalizedNominalSelectionValues(p, s);
	}

	public synchronized double getSpecificity(CompoundGroupWithProperties c, CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			return numericClusterSpec(c, p);
		else
			return nominalClusterSpec(c, p);
	}

	public synchronized double getSpecificity(Compound m, CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			return numericCompoundSpec(m, p);
		else
			return nominalCompoundSpec(m, p);
	}

	public synchronized String getSummaryStringValue(CompoundProperty p, boolean html)
	{
		if (!summarys.containsKey(p))
			updateNormalizedValues(p);
		if (p.getType() == Type.NOMINAL)
			return formattedSummarys.get(p).toString(html);
		else
			return summarys.get(p).toString(html);
	}

	public synchronized void initFeatureNormalization()
	{
		@SuppressWarnings("unchecked")
		List<CompoundProperty> props = ListUtil.concat(clustering.getProperties(), clustering.getFeatures());
		TaskProvider.debug("Compute feature value statistics");
		for (CompoundProperty p : props)
			updateNormalizedValues(p);
	}

	public synchronized void initSelectionNormalization(CompoundSelection s)
	{
		@SuppressWarnings("unchecked")
		List<CompoundProperty> props = ListUtil.concat(clustering.getProperties(), clustering.getFeatures());
		for (CompoundProperty p : props)
			updateNormalizedSelectionValues(p, s);
	}

	public synchronized Double getNormalizedDoubleValue(CompoundPropertyOwner m, CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!normalizedValues.containsKeyPair(m, p))
			updateNormalizedValues(p);
		return normalizedValues.get(m, p);
	}

	public synchronized Double getNormalizedLogDoubleValue(CompoundPropertyOwner m, CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!normalizedLogValues.containsKeyPair(m, p))
			updateNormalizedValues(p);
		return normalizedLogValues.get(m, p);
	}

	public Double getDoubleValue(CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!summarys.containsKey(p))
			updateNormalizedValues(p);
		return ((DoubleArraySummary) summarys.get(p)).getMean();
	}

	@SuppressWarnings("unchecked")
	public String getStringValue(CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			throw new IllegalStateException();
		if (!summarys.containsKey(p))
			updateNormalizedValues(p);
		CountedSet<String> set = (CountedSet<String>) summarys.get(p);
		String mode = set.getMode(false);
		if (set.getCount(mode) > set.getSum(false) * 2 / 3.0)
			return mode;
		else
			return null;
	}
}