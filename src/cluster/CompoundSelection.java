package cluster;

import java.util.HashMap;

import util.ArraySummary;
import util.CountedSet;
import util.DoubleArraySummary;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyUtil;

public class CompoundSelection implements CompoundGroupWithProperties
{
	private Compound[] compounds;
	HashMap<CompoundProperty, ArraySummary> summarys = new HashMap<CompoundProperty, ArraySummary>();

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
		if (p.isSmartsProperty() || CompoundPropertyUtil.isExportedFPProperty(p))
		{
			@SuppressWarnings("unchecked")
			CountedSet<String> set = ((CountedSet<String>) summarys.get(p)).copy();
			if (set.contains("1"))
				set.rename("1", "match");
			if (set.contains("0"))
				set.rename("0", "no-match");
			return set.toString(false);
			//			return "matches " + set.getCount("1") + "/" + set.sum();

		}
		else
			return summarys.get(p).toString(false);
	}

	public int size()
	{
		return compounds.length;
	}

}
