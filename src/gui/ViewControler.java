package gui;

import gui.property.ColorGradient;
import gui.util.Highlighter;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.JComponent;

import dataInterface.AbstractCompoundProperty;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertySet;
import dataInterface.SubstructureSmartsType;

public interface ViewControler
{
	public enum Style
	{
		wireframe, ballsAndSticks, dots
	}

	public enum HideCompounds
	{
		none, nonWatched, nonActive
	}

	public static enum HighlightMode
	{
		ColorCompounds, Spheres;
	}

	public HideCompounds getHideCompounds();

	public void setHideCompounds(HideCompounds hide);

	public boolean isSpinEnabled();

	public void setSpinEnabled(boolean spinEnabled);

	public boolean canChangeCompoundSize(boolean larger);

	public void changeCompoundSize(boolean larger);

	public int getCompoundSize();

	public int getCompoundSizeMax();

	public void setCompoundSize(int compoundSize);

	public HighlightMode getHighlightMode();

	public void setHighlightMode(HighlightMode mode);

	public void setSphereSize(double size);

	public void setSphereTranslucency(double translucency);

	public Style getStyle();

	public void setStyle(Style style);

	public HashMap<String, Highlighter[]> getHighlighters();

	public void setHighlighter(Highlighter highlighter);

	public void setHighlighter(CompoundProperty prop);

	public void setHighlighter(SubstructureSmartsType type);

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
	public static final String PROPERTY_COMPOUND_DESCRIPTOR_CHANGED = "propertyCompoundDescriptorChanged";
	public static final String PROPERTY_HIGHLIGHT_MODE_CHANGED = "propertyHighlightModeChanged";
	public static final String PROPERTY_HIGHLIGHT_COLORS_CHANGED = "propertyHighlightColorsChanged";
	public static final String PROPERTY_ANTIALIAS_CHANGED = "propertyAntialiasChanged";
	public static final String PROPERTY_HIGHLIGHT_LAST_FEATURE = "propertyHighlightLastFeature";
	public static final String PROPERTY_STYLE_CHANGED = "propertyStyleChanged";

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

	public void setBackgroundBlack(boolean backgroudBlack);

	public void setMatchColor(Color color);

	public Color getMatchColor();

	static final CompoundProperty COMPOUND_INDEX_PROPERTY = new AbstractCompoundProperty("Compound Index", "no-desc")
	{
		@Override
		public CompoundPropertySet getCompoundPropertySet()
		{
			return null;
		}
	};
	static final CompoundProperty COMPOUND_SMILES_PROPERTY = new AbstractCompoundProperty("Compound SMILES", "no-desc")
	{
		@Override
		public CompoundPropertySet getCompoundPropertySet()
		{
			return null;
		}
	};

	public void setCompoundDescriptor(CompoundProperty prop);

	public CompoundProperty getCompoundDescriptor();

	public void addIgnoreMouseMovementComponents(JComponent ignore);

	public void updateMouseSelection(boolean buttonDown);

	public Boolean isHighlightLogEnabled();

	public ColorGradient getHighlightGradient();

	public void setHighlightColors(ColorGradient g, boolean log, CompoundProperty props[]);

	public void setSelectLastSelectedHighlighter();

	public boolean isAntialiasEnabled();

	public void setAntialiasEnabled(boolean b);

	public void setHighlightLastFeatureEnabled(boolean b);

	public boolean isHighlightLastFeatureEnabled();

	public void increaseSpinSpeed(boolean increase);

}
