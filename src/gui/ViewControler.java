package gui;

import gui.util.Highlighter;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

public interface ViewControler
{
	public static final String STYLE_WIREFRAME = "spacefill 0; wireframe 0.02";
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

	public boolean isSuperimpose();

	public boolean isAllClustersSpreadable();

	public boolean isSingleClusterSpreadable();

	public boolean isHideHydrogens();

	public void setHideHydrogens(boolean b);

	public static final String PROPERTY_HIGHLIGHT_CHANGED = "propertyHighlightChanged";
	public static final String PROPERTY_SHOW_HYDROGENS = "propertyShowHydrogens";
	public static final String PROPERTY_NEW_HIGHLIGHTERS = "propertyNewHighlighters";
	public static final String PROPERTY_DENSITY_CHANGED = "propertyDensityChanged";
	public static final String PROPERTY_SUPERIMPOSE_CHANGED = "propertySuperimposeChanged";
	public static final String PROPERTY_HIDE_UNSELECT_CHANGED = "propertyHideUnselectChanged";
	public static final String PROPERTY_SPIN_CHANGED = "propertySpinChanged";
	public static final String PROPERTY_BACKGROUND_CHANGED = "propertyBackgroundChanged";
	public static final String PROPERTY_MATCH_COLOR_CHANGED = "propertyMatchColorChanged";

	public boolean isHighlighterLabelsVisible();

	public void setHighlighterLabelsVisible(boolean selected);

	public static enum HighlightSorting
	{
		Max, Median, Min;
	}

	public void setHighlightSorting(HighlightSorting sorting);

	public HighlightSorting getHighlightSorting();

	public void addViewListener(PropertyChangeListener l);

	public boolean isBlackgroundBlack();

	public void setBlackgroundBlack(boolean backgroudBlack);

	public void setMatchColor(Color color);

	public Color getMatchColor();
}
