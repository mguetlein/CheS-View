package cluster;

import gui.View;

import java.util.BitSet;

import javax.vecmath.Vector3f;

import org.jmol.script.Token;

import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.SubstructureSmartsType;

public class Model
{
	int modelIndex;
	BitSet bitSet;
	double values[];

	CompoundData compoundData;

	boolean translucent = false;
	boolean showLabel = false;
	boolean showBox = false;
	boolean hidden = false;
	SubstructureSmartsType substructureHighlighted = null;
	private Vector3f position;

	public Model(int modelIndex, CompoundData compoundData)
	{
		this.modelIndex = modelIndex;
		this.compoundData = compoundData;
		bitSet = View.instance.getModelUndeletedAtomsBitSet(modelIndex);
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

	public void setTemperature(MoleculeProperty property)
	{
		// string properties do have a normalized double value as well
		double v = compoundData.getNormalizedValue(property);
		View.instance.setAtomProperty(bitSet, Token.temperature, (int) v, (float) v, v + "", null, null);
	}

	public BitSet getBitSet()
	{
		return bitSet;
	}

	public int getModelIndex()
	{
		return modelIndex;
	}

	public int getModelOrigIndex()
	{
		return compoundData.getIndex();
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

	public boolean isShowBox()
	{
		return showBox;
	}

	public void setShowBox(boolean showBox)
	{
		this.showBox = showBox;
	}

	public boolean isHidden()
	{
		return hidden;
	}

	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}

	public SubstructureSmartsType getSubstructureHighlighted()
	{
		return substructureHighlighted;
	}

	public void setSubstructureHighlighted(SubstructureSmartsType type)
	{
		substructureHighlighted = type;
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

	public Vector3f getPosition()
	{
		return position;
	}

	public void setPosition(Vector3f pos)
	{
		position = pos;
	}
}
