package cluster;

import util.CountedSet;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertyOwner;

public interface CompoundGroupWithProperties extends CompoundPropertyOwner
{
	public int size();

	public CountedSet<String> getNominalSummary(CompoundProperty p);
}
