package cluster;

import gui.MainPanel.Translucency;
import gui.View;
import gui.ViewControler;
import gui.Zoomable;

import java.util.BitSet;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.vecmath.Vector3f;

import main.Settings;

import org.jmol.script.Token;

import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class Model implements Zoomable
{
	private int modelIndex;
	private BitSet bitSet;

	private CompoundData compoundData;

	private Translucency translucency = Translucency.None;
	private String label = null;
	private boolean showHoverBox = false;
	private boolean showActiveBox = false;
	private String smarts = null;

	private HashMap<String, BitSet> smartsMatches;
	private String color;
	private MoleculeProperty highlightMoleculeProperty;
	private String style;
	private MoleculeProperty descriptorProperty = ViewControler.COMPOUND_INDEX_PROPERTY;

	private float diameter = -1;

	public final Vector3f origCenter;

	public Model(int modelIndex, CompoundData compoundData)
	{
		this.modelIndex = modelIndex;
		this.compoundData = compoundData;
		bitSet = View.instance.getModelUndeletedAtomsBitSet(modelIndex);
		origCenter = new Vector3f(View.instance.getAtomSetCenter(bitSet));
		smartsMatches = new HashMap<String, BitSet>();
	}

	public String getTemperature(MoleculeProperty property)
	{
		if (property.getType() == Type.NUMERIC)
			return getDoubleValue(property) + "";
		else
			return getStringValue(property) + "";
	}

	public String getStringValue(MoleculeProperty property)
	{
		return compoundData.getStringValue(property);
	}

	public Double getDoubleValue(MoleculeProperty property)
	{
		return compoundData.getDoubleValue(property);
	}

	public BitSet getBitSet()
	{
		return bitSet;
	}

	/**
	 * index in jmol
	 */
	public int getModelIndex()
	{
		return modelIndex;
	}

	/**
	 * index in original data file
	 */
	public int getModelOrigIndex()
	{
		return compoundData.getIndex();
	}

	public String toString()
	{
		if (descriptorProperty == ViewControler.COMPOUND_INDEX_PROPERTY)
			return "Compound " + (getModelOrigIndex() + 1);
		else if (descriptorProperty == ViewControler.COMPOUND_SMILES_PROPERTY)
			return getSmiles();
		else
			return getTemperature(descriptorProperty);
	}

	public String getSmiles()
	{
		return compoundData.getSmiles();
	}

	public void modelIndexOffset(int offset)
	{
		modelIndex += offset;
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

	public void setColor(String color)
	{
		this.color = color;
	}

	public String getColor()
	{
		return color;
	}

	public void setHighlightMoleculeProperty(MoleculeProperty highlightMoleculeProperty)
	{
		this.highlightMoleculeProperty = highlightMoleculeProperty;
		if (highlightMoleculeProperty != null && this.highlightMoleculeProperty.getType() == Type.NUMERIC)
		{
			// string properties do have a normalized double value as well
			// values are normalize between 0-1, fixed temp scheme from jmol expects values between 0-100 => multiply with 100
			Double d = compoundData.getNormalizedValue(highlightMoleculeProperty);
			if (d != null)
			{
				double v = compoundData.getNormalizedValue(highlightMoleculeProperty) * 100.0;
				//			Settings.LOGGER.warn(getModelOrigIndex() + " " + highlightMoleculeProperty + " " + v);
				View.instance.setAtomProperty(bitSet, Token.temperature, (int) v, (float) v, v + "", null, null);
			}
		}
	}

	public Object getHighlightMoleculeProperty()
	{
		return highlightMoleculeProperty;
	}

	public void setStyle(String style)
	{
		this.style = style;
	}

	public String getStyle()
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

	public void setDescriptor(MoleculeProperty descriptorProperty)
	{
		this.descriptorProperty = descriptorProperty;
	}

}
