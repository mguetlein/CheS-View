package gui.util;

import dataInterface.CompoundProperty;

public class CompoundPropertyHighlighter extends Highlighter
{
	private CompoundProperty prop;

	public CompoundPropertyHighlighter(CompoundProperty prop)
	{
		super(prop.toString());
		this.prop = prop;
	}

	public CompoundProperty getProperty()
	{
		return prop;
	}
}