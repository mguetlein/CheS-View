package cluster;

import util.CountedSet;
import dataInterface.CompoundPropertyOwner;
import dataInterface.NominalProperty;

public interface CompoundGroupWithProperties extends CompoundPropertyOwner
{
	public int size();

	public CountedSet<String> getNominalSummary(NominalProperty p);
}
