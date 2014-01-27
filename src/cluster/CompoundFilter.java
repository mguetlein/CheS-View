package cluster;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompoundFilter
{
	private String desc;
	private Set<Compound> compounds;
	private boolean accept;

	public CompoundFilter(String desc, List<Compound> compounds, boolean accept)
	{
		this.desc = desc;
		this.compounds = new HashSet<Compound>(compounds);
		this.accept = accept;
	}

	public String toString()
	{
		return desc + " (#" + compounds.size() + ")";
	}

	public boolean accept(Compound c)
	{
		if (accept)
			return compounds.contains(c);
		else
			return !compounds.contains(c);
	}

}
