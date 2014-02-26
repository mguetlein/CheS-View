package gui;

import gui.property.ColorGradient;
import gui.util.Highlighter;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.JComponent;

import cluster.Clustering;
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

	public enum DisguiseMode
	{
		solid, translucent, invisible
	}

	public static enum HighlightMode
	{
		ColorCompounds, Spheres;
	}

	public DisguiseMode getDisguiseUnHovered();

	public DisguiseMode getDisguiseUnZoomed();

	public void setDisguiseUnHovered(DisguiseMode hide);

	public void setDisguiseUnZoomed(DisguiseMode hide);

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

	public void setHighlighter(Highlighter highlighter, boolean showMessage);

	public void setHighlighter(CompoundProperty prop);

	public void setHighlighter(SubstructureSmartsType type);

	public Highlighter getHighlighter();

	public Highlighter getHighlighter(SubstructureSmartsType type);

	public Highlighter getHighlighter(CompoundProperty p);

	public CompoundProperty getHighlightedProperty();

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
	public static final String PROPERTY_DISGUISE_CHANGED = "propertyDisguiseChanged";
	public static final String PROPERTY_SPIN_CHANGED = "propertySpinChanged";
	public static final String PROPERTY_BACKGROUND_CHANGED = "propertyBackgroundChanged";
	public static final String PROPERTY_FONT_SIZE_CHANGED = "propertyFontSizeChanged";
	public static final String PROPERTY_MATCH_COLOR_CHANGED = "propertyMatchColorChanged";
	public static final String PROPERTY_COMPOUND_DESCRIPTOR_CHANGED = "propertyCompoundDescriptorChanged";
	public static final String PROPERTY_HIGHLIGHT_MODE_CHANGED = "propertyHighlightModeChanged";
	public static final String PROPERTY_HIGHLIGHT_COLORS_CHANGED = "propertyHighlightColorsChanged";
	public static final String PROPERTY_ANTIALIAS_CHANGED = "propertyAntialiasChanged";
	public static final String PROPERTY_HIGHLIGHT_LAST_FEATURE = "propertyHighlightLastFeature";
	public static final String PROPERTY_STYLE_CHANGED = "propertyStyleChanged";
	public static final String PROPERTY_FEATURE_FILTER_CHANGED = "propertyFeatureFilterChanged";
	public static final String PROPERTY_FEATURE_SORTING_CHANGED = "propertyFeatureSortingChanged";
	public static final String PROPERTY_COMPOUND_FILTER_CHANGED = "propertyCompoundFilterChanged";
	public static final String PROPERTY_SINGLE_COMPOUND_SELECTION_ENABLED = "propertySingleCompoundSelectionEnabled";

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

	public void increaseFontSize(boolean increase);

	public void setFontSize(int fontsize);

	public int getFontSize();

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

	public static enum FeatureFilter
	{
		None, NotUsedByEmbedding, UsedByEmbedding, Filled, Real, Endpoints;

		public static FeatureFilter[] validValues(Clustering clustering)
		{
			if (clustering.isBMBFRealEndpointDataset(true))
				return new FeatureFilter[] { None, NotUsedByEmbedding, UsedByEmbedding, Filled, Real, Endpoints };
			else if (clustering.isBMBFRealEndpointDataset(false))
				return new FeatureFilter[] { None, NotUsedByEmbedding, UsedByEmbedding, Real, Endpoints };
			else
				return new FeatureFilter[] { None, NotUsedByEmbedding, UsedByEmbedding };
		}

		public String niceString()
		{
			switch (this)
			{
				case None:
					return "Show all features (no filter)";
				case NotUsedByEmbedding:
					return "Show only features NOT used by mapping";
				case UsedByEmbedding:
					return "Show only features used by mapping";
				case Filled:
					return "Show only '_filled features'";
				case Real:
					return "Show only '_real features'";
				case Endpoints:
					return "Show only 'endpoint features'";
			}
			throw new IllegalStateException();
		}
	}

	public void setFeatureFilter(FeatureFilter filter);

	public FeatureFilter getFeatureFilter();

	public boolean isFeatureSortingEnabled();

	public void setFeatureSortingEnabled(boolean b);

	public boolean isShowClusteringPropsEnabled();

	public void showSortFilterDialog();

	public void setSingleCompoundSelection(boolean b);

	public boolean isSingleCompoundSelection();

	// to remove

	//	public void setZoomToSingleActiveCompounds(boolean b);

	//	public void setCompoundFilter(CompoundFilter filter, boolean animate);
	//
	//	public void useSelectedCompoundsAsFilter(String filterDescription, boolean animate);
	//
	//	public CompoundFilter getCompoundFilter();
}
