package gui;

import gui.MainPanel.HighlightMode;
import gui.util.Highlighter;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.JComponent;

import dataInterface.AbstractMoleculeProperty;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculePropertySet;

public interface ViewControler
{
	public static final String STYLE_WIREFRAME = "spacefill 0; wireframe 0.02";
	public static final String STYLE_BALLS_AND_STICKS = "wireframe 25; spacefill 15%";

	public boolean isHideUnselected();

	public void setHideUnselected(boolean hide);

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

	public String getStyle();

	public void setStyle(String style);

	public HashMap<String, Highlighter[]> getHighlighters();

	public void setHighlighter(Highlighter highlighter);

	public void setHighlighter(MoleculeProperty prop);

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
	public static final String PROPERTY_MOLECULE_DESCRIPTOR_CHANGED = "propertyMoleculeDescriptorChanged";
	public static final String PROPERTY_HIGHLIGHT_MODE_CHANGED = "propertyHighlightModeChanged";
	public static final String PROPERTY_HIGHLIGHT_LOG_CHANGED = "propertyHighlightLogChanged";
	public static final String PROPERTY_ANTIALIAS_CHANGED = "propertyAntialiasChanged";
	public static final String PROPERTY_HIGHLIGHT_LAST_FEATURE = "propertyHighlightLastFeature";

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

	static final MoleculeProperty COMPOUND_INDEX_PROPERTY = new AbstractMoleculeProperty("Compound Index", "no-desc")
	{
		@Override
		public MoleculePropertySet getMoleculePropertySet()
		{
			return null;
		}
	};
	static final MoleculeProperty COMPOUND_SMILES_PROPERTY = new AbstractMoleculeProperty("Compound SMILES", "no-desc")
	{
		@Override
		public MoleculePropertySet getMoleculePropertySet()
		{
			return null;
		}
	};

	public void setMoleculeDescriptor(MoleculeProperty prop);

	public MoleculeProperty getMoleculeDescriptor();

	public void addIgnoreMouseMovementComponents(JComponent ignore);

	public void updateMouseSelection(boolean buttonDown);

	public boolean isHighlightLogEnabled();

	public void setHighlightLogEnabled(boolean b);

	public void setSelectLastSelectedHighlighter();

	public boolean isAntialiasEnabled();

	public void setAntialiasEnabled(boolean b);

	public void setHighlightLastFeatureEnabled(boolean b);

	public boolean isHighlightLastFeatureEnabled();

}
