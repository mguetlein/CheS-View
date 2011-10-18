package gui.util;

import dataInterface.SubstructureSmartsType;

public class SubstructureHighlighter extends Highlighter
{
	private SubstructureSmartsType type;

	public SubstructureHighlighter(SubstructureSmartsType type)
	{
		super(type.toString());
		this.type = type;
	}

	public SubstructureSmartsType getType()
	{
		return type;
	}
}