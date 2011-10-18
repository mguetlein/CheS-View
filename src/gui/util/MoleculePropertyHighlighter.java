package gui.util;

import dataInterface.MoleculeProperty;

public class MoleculePropertyHighlighter extends Highlighter
{
	private MoleculeProperty prop;

	public MoleculePropertyHighlighter(MoleculeProperty prop)
	{
		super(prop.toString());
		this.prop = prop;
	}

	public MoleculeProperty getProperty()
	{
		return prop;
	}
}