package cluster;

import java.util.HashMap;

import util.ArraySummary;
import util.CountedSet;
import util.DoubleArraySummary;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;

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
		return "Selection of " + size() + " compounds";
	}

	public Compound[] getCompounds()
	{
		return compounds;
	}

	@Override
	public Double getDoubleValue(CompoundProperty p)
	{
		if (p.getType() != Type.NUMERIC)
			throw new IllegalStateException();
		if (!summarys.containsKey(p))
			updateNumeric(p);
		return ((DoubleArraySummary) summarys.get(p)).getMean();
	}

	private void updateNumeric(CompoundProperty p)
	{
		Double d[] = new Double[size()];
		int i = 0;
		for (Compound m : compounds)
			d[i++] = m.getDoubleValue(p);
		summarys.put(p, DoubleArraySummary.create(d));
	}

	private void updateNominal(CompoundProperty p)
	{
		String s[] = new String[size()];
		int i = 0;
		for (Compound m : compounds)
			s[i++] = m.getStringValue(p);
		CountedSet<String> set = CountedSet.create(s);
		summarys.put(p, set);
		CountedSet<String> fSet = set.copy();
		for (String key : fSet.values())
			fSet.rename(key, p.getFormattedValueInMappedDataset(key));
		fSet.setToBack(p.getFormattedNullValue());
		formattedSummarys.put(p, fSet);
	}

	@Override
	public String getStringValue(CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			throw new IllegalStateException();
		if (!summarys.containsKey(p))
			updateNominal(p);
		@SuppressWarnings("unchecked")
		CountedSet<String> set = (CountedSet<String>) summarys.get(p);
		String mode = set.getMode(false);
		if (set.getCount(mode) > set.getSum(false) * 2 / 3.0)
			return mode;
		else
			return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CountedSet<String> getNominalSummary(CompoundProperty p)
	{
		if (p.getType() == Type.NUMERIC)
			throw new IllegalStateException();
		if (!summarys.containsKey(p))
			updateNominal(p);
		return (CountedSet<String>) summarys.get(p);
	}

	@Override
	public String getFormattedValue(CompoundProperty p)
	{
		if (!summarys.containsKey(p))
			if (p.getType() == Type.NUMERIC)
				updateNumeric(p);
			else
				updateNominal(p);
		if (p.getType() == Type.NUMERIC)
			return summarys.get(p).toString(false);
		else
			return formattedSummarys.get(p).toString(false);
	}

	public int size()
	{
		return compounds.length;
	}

}
