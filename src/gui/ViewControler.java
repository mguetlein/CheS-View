package gui;

import java.beans.PropertyChangeListener;
import java.util.HashMap;

import dataInterface.MoleculeProperty;

public interface ViewControler
{
	public static final String STYLE_WIREFRAME = "spacefill 0; wireframe";
	public static final String STYLE_BALLS_AND_STICKS = "wireframe 25; spacefill 15%";

	public boolean isSpinEnabled();

	public void setSpinEnabled(boolean spinEnabled);

	public String getStyle();

	public void setStyle(String style);

	public HashMap<String, Highlighter[]> getHighlighters();

	public void setHighlighter(Highlighter highlighter);

	public Highlighter getHighlighter();

	public void setSuperimpose(boolean superimpose);

	public static final String PROPERTY_HIGHLIGHT_CHANGED = "propertyHighlightChanged";
	public static final String PROPERTY_NEW_HIGHLIGHTERS = "propertyNewHighlighters";

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
		public MoleculeProperty getProperty();
	}

	public class DefaultHighlighter implements Highlighter
	{
		private MoleculeProperty prop;

		public DefaultHighlighter(MoleculeProperty prop)
		{
			this.prop = prop;
		}

		@Override
		public MoleculeProperty getProperty()
		{
			return prop;
		}

		@Override
		public String toString()
		{
			return prop.toString();
		}
	}

	class FakeProperty implements MoleculeProperty
	{
		public String s;

		public FakeProperty(String s)
		{
			this.s = s;
		}

		public String toString()
		{
			return s;
		}

		@Override
		public boolean isNumeric()
		{
			return false;
		}
	}

	public class FakeHighlighter extends DefaultHighlighter
	{
		public FakeHighlighter(String prop)
		{
			super(new FakeProperty(prop));
		}
	}

}
