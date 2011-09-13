package gui;

import java.beans.PropertyChangeListener;
import java.util.HashMap;

import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public interface ViewControler
{
	public static final String STYLE_WIREFRAME = "spacefill 0; wireframe";
	public static final String STYLE_BALLS_AND_STICKS = "wireframe 25; spacefill 15%";

	public boolean isHideUnselected();

	public void setHideUnselected(boolean hide);

	public boolean isSpinEnabled();

	public void setSpinEnabled(boolean spinEnabled);

	public boolean canChangeDensitiy(boolean higher);

	public void setDensitiyHigher(boolean higher);

	public String getStyle();

	public void setStyle(String style);

	public HashMap<String, Highlighter[]> getHighlighters();

	public void setHighlighter(Highlighter highlighter);

	public Highlighter getHighlighter();

	public void setSuperimpose(boolean superimpose);

	public static final String PROPERTY_HIGHLIGHT_CHANGED = "propertyHighlightChanged";
	public static final String PROPERTY_NEW_HIGHLIGHTERS = "propertyNewHighlighters";
	public static final String PROPERTY_DENSITY_CHANGED = "propertyDensityChanged";

	public boolean isHighlighterLabelsVisible();

	public void setHighlighterLabelsVisible(boolean selected);

	public static enum HighlightSorting
	{
		Max, Med, Min;
	}

	public void setHighlightSorting(HighlightSorting sorting);

	public HighlightSorting getHighlightSorting();

	public void addViewListener(PropertyChangeListener l);

	///////////////

	public interface Highlighter
	{
	}

	public class SimpleHighlighter implements Highlighter
	{
		private String name;

		public SimpleHighlighter(String name)
		{
			this.name = name;
		}

		public String toString()
		{
			return name;
		}
	}

	public class MoleculePropertyHighlighter extends SimpleHighlighter
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

	public class SubstructureHighlighter extends SimpleHighlighter
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

}
