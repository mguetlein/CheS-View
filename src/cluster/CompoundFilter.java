package cluster;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompoundFilter
{
	private String desc;
	private Set<Compound> compounds;

	public CompoundFilter(String desc, List<Compound> compounds)
	{
		this.desc = desc;
		this.compounds = new HashSet<Compound>(compounds);
	}

	public String toString()
	{
		return desc + " (#" + compounds.size() + ")";
	}

	public boolean accept(Compound c)
	{
		return compounds.contains(c);
	}

}
