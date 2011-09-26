package cluster;

import gui.View;

import java.util.BitSet;
import java.util.HashMap;

import javax.vecmath.Vector3f;

import org.jmol.script.Token;

import data.CompoundDataImpl;
import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class Model
{
	private int modelIndex;
	private BitSet bitSet;

	private CompoundData compoundData;

	private boolean translucent = false;
	private boolean showLabel = false;
	private boolean showHoverBox = false;
	private boolean showActiveBox = false;
	private String smarts = null;
	private Vector3f position;
	private HashMap<String, BitSet> smartsMatches;
	private String color;
	private MoleculeProperty highlightMoleculeProperty;
	private String style;

	public Model(int modelIndex, CompoundData compoundData)
	{
		this.modelIndex = modelIndex;
		this.compoundData = compoundData;
		bitSet = View.instance.getModelUndeletedAtomsBitSet(modelIndex);
		smartsMatches = new HashMap<String, BitSet>();
	}

	public String getTemperature(MoleculeProperty property)
	{
		if (property.getType() == Type.NUMERIC)
			return getDoubleValue(property) + "";
		else
			return getStringValue(property);
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
		return "Compound " + getModelIndex();
	}

	public String getSmiles()
	{
		return compoundData.getSmiles();
	}

	public void modelIndexOffset(int offset)
	{
		modelIndex += offset;
	}

	public boolean isTranslucent()
	{
		return translucent;
	}

	public void setTranslucent(boolean translucent)
	{
		this.translucent = translucent;
	}

	public boolean isShowLabel()
	{
		return showLabel;
	}

	public void setShowLabel(boolean showLabel)
	{
		this.showLabel = showLabel;
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

	public Vector3f getOrigPosition()
	{
		return compoundData.getPosition();
	}

	public void setOrigPosition(Vector3f vector3f)
	{
		((CompoundDataImpl) compoundData).setPosition(vector3f);
	}

	public Vector3f getPosition()
	{
		return position;
	}

	public void setPosition(Vector3f pos)
	{
		position = pos;
	}

	public BitSet getSmartsMatch(String smarts)
	{
		//compute match dynamically
		if (!smartsMatches.containsKey(smarts))
		{
			System.out.println("smarts-matching smarts: " + smarts + " smiles: " + getSmiles());
			smartsMatches.put(smarts, View.instance.getSmartsMatch(smarts, bitSet));
			//			if (smartsMatches.get(smarts).cardinality() == 0)
			//			{
			////				System.out.flush();
			////				System.err.println("could not match smarts!");
			////				System.err.flush();
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
		if (highlightMoleculeProperty != null)
		{
			// string properties do have a normalized double value as well
			// values are normalize between 0-1, fixed temp scheme from jmol expects values between 0-100 => multiply with 100
			double v = compoundData.getNormalizedValue(highlightMoleculeProperty) * 100.0;
			//			System.err.println(getModelOrigIndex() + " " + highlightMoleculeProperty + " " + v);
			View.instance.setAtomProperty(bitSet, Token.temperature, (int) v, (float) v, v + "", null, null);
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

}
