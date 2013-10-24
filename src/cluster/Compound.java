package cluster;

import gui.DoubleNameListCellRenderer.DoubleNameElement;
import gui.MainPanel.Translucency;
import gui.View;
import gui.ViewControler;
import gui.ViewControler.Style;
import gui.Zoomable;

import java.awt.Color;
import java.util.BitSet;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.vecmath.Vector3f;

import main.Settings;

import org.apache.commons.lang.StringEscapeUtils;

import util.ColorUtil;
import util.ObjectUtil;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyOwner;

public class Compound implements Zoomable, Comparable<Compound>, DoubleNameElement, CompoundPropertyOwner
{
	private int compoundIndex;
	private BitSet bitSet;
	private BitSet dotModeHideBitSet;
	private BitSet dotModeDisplayBitSet;

	private CompoundData compoundData;

	private Translucency translucency = Translucency.None;
	private String label = null;
	private boolean showHoverBox = false;
	private boolean showActiveBox = false;
	private String smarts = null;

	private HashMap<String, BitSet> smartsMatches;
	private String compoundColor;
	private String highlightColorString;
	private Color highlightColor;
	private String lastHighlightColorString;
	private Vector3f spherePosition;
	private CompoundProperty highlightCompoundProperty;
	private Style style;
	private CompoundProperty descriptorProperty = null;
	private boolean sphereVisible;
	private boolean lastFeatureSphereVisible;

	private float diameter = -1;

	public final Vector3f origCenter;
	public final Vector3f origDotPosition;

	public Compound(int compoundIndex, CompoundData compoundData)
	{
		this.compoundIndex = compoundIndex;
		this.compoundData = compoundData;
		if (View.instance != null)
		{
			bitSet = View.instance.getCompoundBitSet(compoundIndex);
			dotModeHideBitSet = View.instance.getDotModeHideBitSet(bitSet);
			dotModeDisplayBitSet = View.instance.getDotModeDisplayBitSet(bitSet);
			origCenter = new Vector3f(View.instance.getAtomSetCenter(bitSet));
			origDotPosition = new Vector3f(View.instance.getAtomSetCenter(getDotModeDisplayBitSet()));
		}
		else
		{
			//for export without graphics
			origCenter = null;
			origDotPosition = null;
		}
		smartsMatches = new HashMap<String, BitSet>();
		setDescriptor(ViewControler.COMPOUND_INDEX_PROPERTY);
	}

	public String getFormattedValue(CompoundProperty property)
	{
		return compoundData.getFormattedValue(property);
	}

	public String getStringValue(CompoundProperty property)
	{
		return compoundData.getStringValue(property);
	}

	public Double getDoubleValue(CompoundProperty property)
	{
		return compoundData.getDoubleValue(property);
	}

	/**
	 * index in jmol
	 */
	public int getCompoundIndex()
	{
		return compoundIndex;
	}

	/**
	 * index in original data file
	 */
	public int getCompoundOrigIndex()
	{
		return compoundData.getIndex();
	}

	public static class DisplayName implements Comparable<DisplayName>
	{
		String valDisplay;
		@SuppressWarnings("rawtypes")
		Comparable valCompare[];
		Integer compareIndex;
		String name;

		public String toString(boolean html, Color highlightColor)
		{
			StringBuffer b = new StringBuffer();
			if (html)
				b.append(StringEscapeUtils.escapeHtml(name));
			else
				b.append(name);
			if (valDisplay != null && !valDisplay.equals(name))
			{
				if (html)
				{
					b.append(":&nbsp;");
					if (highlightColor != null)
						b.append("<font color='" + ColorUtil.toHtml(highlightColor) + "'>");
					b.append("<i>");
					b.append(StringEscapeUtils.escapeHtml(valDisplay));
					b.append("</i>");
					if (highlightColor != null)
						b.append("</font>");
				}
				else
				{
					b.append(": ");
					b.append(valDisplay);
				}
			}
			return b.toString();
		}

		@Override
		public int compareTo(DisplayName d)
		{
			if (valCompare != null)
				for (int j = 0; j < valCompare.length; j++)
				{
					int i = ObjectUtil.compare(valCompare[j], d.valCompare[j]);
					if (i != 0)
						return i;
				}
			if (compareIndex != null)
				return compareIndex.compareTo(d.compareIndex);
			// if nothing is selected, compound should be sorted according to identifier
			return name.compareTo(d.name);
		}
	}

	private DisplayName displayName = new DisplayName();

	@Override
	public String toString()
	{
		return getFirstName();
	}

	public String toStringWithValue()
	{
		return displayName.toString(false, null);
	}

	@Override
	public String getFirstName()
	{
		return displayName.name;
	}

	@Override
	public String getSecondName()
	{
		if (ObjectUtil.equals(displayName.valDisplay, displayName.name))
			return null;
		else
			return displayName.valDisplay;
	}

	@Override
	public int compareTo(Compound m)
	{
		return displayName.compareTo(m.displayName);
	}

	public String getSmiles()
	{
		return compoundData.getSmiles();
	}

	public void compoundIndexOffset(int offset)
	{
		compoundIndex += offset;
	}

	public Translucency getTranslucency()
	{
		return translucency;
	}

	public void setTranslucency(Translucency translucency)
	{
		this.translucency = translucency;
	}

	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public boolean isShowHoverBox()
	{
		return showHoverBox;
	}

	public void setShowHoverBox(boolean showBox)
	{
		this.showHoverBox = showBox;
	}

	public boolean isShowActiveBox()
	{
		return showActiveBox;
	}

	public void setShowActiveBox(boolean showBox)
	{
		this.showActiveBox = showBox;
	}

	public String getHighlightedSmarts()
	{
		return smarts;
	}

	public void setHighlightedSmarts(String smarts)
	{
		this.smarts = smarts;
	}

	public void moveTo(Vector3f clusterPos)
	{
		Vector3f center = new Vector3f(View.instance.getAtomSetCenter(getBitSet()));
		Vector3f dest = new Vector3f(clusterPos);
		dest.sub(center);
		View.instance.setAtomCoordRelative(dest, getBitSet());
	}

	public Vector3f getPosition(boolean scaled)
	{
		Vector3f v = new Vector3f(compoundData.getPosition());
		if (scaled)
			v.scale(ClusteringUtil.SCALE);
		return v;
	}

	public Vector3f getPosition()
	{
		return getPosition(true);
	}

	public BitSet getSmartsMatch(String smarts)
	{
		//compute match dynamically
		if (!smartsMatches.containsKey(smarts))
		{
			Settings.LOGGER.info("smarts-matching smarts: " + smarts + " smiles: " + getSmiles());
			smartsMatches.put(smarts, View.instance.getSmartsMatch(smarts, bitSet));
			//			if (smartsMatches.get(smarts).cardinality() == 0)
			//			{
			////				Settings.LOGGER.flush();
			////				Settings.LOGGER.warn("could not match smarts!");
			////				Settings.LOGGER.flush();
			//			}
		}
		return smartsMatches.get(smarts);
	}

	public void setCompoundColor(String colorString)
	{
		this.compoundColor = colorString;
	}

	public String getCompoundColor()
	{
		return compoundColor;
	}

	public void setHighlightColor(String colorString, Color color)
	{
		if (!ObjectUtil.equals(highlightColorString, colorString))
		{
			this.lastHighlightColorString = highlightColorString;
			this.highlightColorString = colorString;
			this.highlightColor = color;
		}
	}

	public Color getHighlightColor()
	{
		return highlightColor;
	}

	public String getHighlightColorString()
	{
		return highlightColorString;
	}

	public String getLastHighlightColorString()
	{
		return lastHighlightColorString;
	}

	public Vector3f getSpherePosition()
	{
		return spherePosition;
	}

	public void setSpherePosition(Vector3f spherePosition)
	{
		this.spherePosition = spherePosition;
	}

	public void setHighlightCompoundProperty(CompoundProperty highlightCompoundProperty)
	{
		if (this.highlightCompoundProperty != highlightCompoundProperty)
		{
			displayName.valDisplay = null;
			displayName.valCompare = null;
			if (highlightCompoundProperty != null)
			{
				if (highlightCompoundProperty.getType() == Type.NUMERIC)
					displayName.valCompare = new Double[] { getDoubleValue(highlightCompoundProperty) };
				else
					displayName.valCompare = new String[] { getStringValue(highlightCompoundProperty) };
				displayName.valDisplay = getFormattedValue(highlightCompoundProperty);
			}
			this.highlightCompoundProperty = highlightCompoundProperty;
		}

		//		if (highlightCompoundProperty != null && this.highlightCompoundProperty.getType() == Type.NUMERIC)
		//		{
		//			// string properties do have a normalized double value as well
		//			// values are normalize between 0-1, fixed temp scheme from jmol expects values between 0-100 => multiply with 100
		//			Double d = compoundData.getNormalizedValue(highlightCompoundProperty);
		//			if (d != null)
		//			{
		//				double v = compoundData.getNormalizedValue(highlightCompoundProperty) * 100.0;
		//				//			Settings.LOGGER.warn(getCompoundOrigIndex() + " " + highlightCompoundProperty + " " + v);
		//				View.instance.setAtomProperty(bitSet, Token.temperature, (int) v, (float) v, v + "", null, null);
		//			}
		//		}
	}

	public Object getHighlightCompoundProperty()
	{
		return highlightCompoundProperty;
	}

	public void setStyle(Style style)
	{
		this.style = style;
	}

	public Style getStyle()
	{
		return style;
	}

	@Override
	public Vector3f getCenter(boolean superimposed)
	{
		return new Vector3f(View.instance.getAtomSetCenter(bitSet));
	}

	@Override
	public float getDiameter(boolean superimposed)
	{
		return getDiameter();
	}

	public float getDiameter()
	{
		if (diameter == -1)
			diameter = View.instance.getDiameter(bitSet);
		return diameter;
	}

	@Override
	public boolean isSuperimposed()
	{
		return false;
	}

	public ImageIcon getIcon(boolean backgroundBlack)
	{
		return compoundData.getIcon(backgroundBlack);
	}

	public void setDescriptor(CompoundProperty descriptorProperty)
	{
		if (this.descriptorProperty != descriptorProperty)
		{
			displayName.compareIndex = null;
			if (descriptorProperty == ViewControler.COMPOUND_INDEX_PROPERTY)
			{
				displayName.compareIndex = getCompoundOrigIndex();
				displayName.name = "Compound " + (getCompoundOrigIndex() + 1);
			}
			else if (descriptorProperty == ViewControler.COMPOUND_SMILES_PROPERTY)
				displayName.name = getSmiles();
			else
				displayName.name = getFormattedValue(descriptorProperty);
			this.descriptorProperty = descriptorProperty;
		}
	}

	public boolean isSphereVisible()
	{
		return sphereVisible;
	}

	public void setSphereVisible(boolean sphereVisible)
	{
		this.sphereVisible = sphereVisible;
	}

	public boolean isLastFeatureSphereVisible()
	{
		return lastFeatureSphereVisible;
	}

	public void setLastFeatureSphereVisible(boolean s)
	{
		this.lastFeatureSphereVisible = s;
	}

	public BitSet getBitSet()
	{
		return bitSet;
	}

	public BitSet getDotModeHideBitSet()
	{
		return dotModeHideBitSet;
	}

	public BitSet getDotModeDisplayBitSet()
	{
		return dotModeDisplayBitSet;
	}

}
