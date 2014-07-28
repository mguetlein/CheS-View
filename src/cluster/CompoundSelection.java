package cluster;

import java.util.HashMap;

import util.ArraySummary;
import util.CountedSet;
import util.DoubleArraySummary;
import dataInterface.CompoundGroupWithProperties;
import dataInterface.CompoundProperty;
import dataInterface.NominalProperty;
import dataInterface.NumericProperty;

public class CompoundSelection implements CompoundGroupWithProperties
{
	private Compound[] compounds;
	HashMap<CompoundProperty, ArraySummary> summarys = new HashMap<CompoundProperty, ArraySummary>();
	HashMap<CompoundProperty, ArraySummary> formattedSummarys = new HashMap<CompoundProperty, ArraySummary>();

	public CompoundSelection(Compound[] c)
	{
		this.compounds = c;
	}

	@Override
	public String toString()
	{
		return "Selection of " + getNumCompounds() + " compounds";
	}

	public Compound[] getCompounds()
	{
		return compounds;
	}

	@Override
	public Double getDoubleValue(NumericProperty p)
	{
		if (!summarys.containsKey(p))
			updateNumeric(p);
		return ((DoubleArraySummary) summarys.get(p)).getMean();
	}

	private void updateNumeric(NumericProperty p)
	{
		Double d[] = new Double[getNumCompounds()];
		int i = 0;
		for (Compound m : compounds)
			d[i++] = m.getDoubleValue(p);
		summarys.put(p, DoubleArraySummary.create(d));
	}

	private void updateNominal(NominalProperty p)
	{
		String s[] = new String[getNumCompounds()];
		int i = 0;
		for (Compound m : compounds)
			s[i++] = m.getStringValue(p);
		CountedSet<String> set = CountedSet.create(s);
		summarys.put(p, set);
		CountedSet<String> fSet = set.copy();
		for (String key : fSet.values())
			fSet.rename(key, p.getFormattedValue(key));
		fSet.setToBack(p.getFormattedNullValue());
		formattedSummarys.put(p, fSet);
	}

	@SuppressWarnings("unchecked")
	@Override
	public CountedSet<String> getNominalSummary(NominalProperty p)
	{
		if (!summarys.containsKey(p))
			updateNominal(p);
		return (CountedSet<String>) summarys.get(p);
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		if (!summarys.containsKey(p))
			if (p instanceof NumericProperty)
				updateNumeric((NumericProperty) p);
			else
				updateNominal((NominalProperty) p);
		if (p instanceof NumericProperty)
			return summarys.get(p).toString(false);
		else
			return formattedSummarys.get(p).toString(false);
	}

	public int getNumCompounds()
	{
		return compounds.length;
	}

}
