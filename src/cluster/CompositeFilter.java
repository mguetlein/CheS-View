package cluster;

public class CompositeFilter implements CompoundFilter
{
	CompoundFilter filter1;
	CompoundFilter filter2;

	public CompositeFilter(CompoundFilter filter1, CompoundFilter filter2)
	{
		this.filter1 = filter1;
		this.filter2 = filter2;
	}

	@Override
	public String toString()
	{
		return "Composite filter";
	}

	@Override
	public boolean accept(Compound c)
	{
		return filter1.accept(c) && filter2.accept(c);
	}
}
