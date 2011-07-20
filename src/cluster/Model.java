package cluster;

import java.util.BitSet;

import javax.vecmath.Vector3f;

import org.jmol.script.Token;
import org.jmol.viewer.Viewer;

import dataInterface.CompoundData;
import dataInterface.MoleculeProperty;

public class Model
{
	Viewer viewer;
	int modelIndex;
	BitSet bitSet;
	double values[];

	CompoundData compoundData;

	boolean translucent = true;
	boolean showLabel = false;
	boolean showBox = false;
	boolean hidden = false;
	boolean substructureHighlighted = false;

	public Model(Viewer viewer, int modelIndex, CompoundData compoundData)
	{
		this.modelIndex = modelIndex;
		this.compoundData = compoundData;
		bitSet = viewer.getModelUndeletedAtomsBitSet(modelIndex);

		this.viewer = viewer;
	}

	public Object getTemperature(MoleculeProperty property)
	{
		return compoundData.getObjectValue(property, false);
	}

	public void setTemperature(MoleculeProperty property)
	{
		// string properties do have a normalized double value as well
		double v = compoundData.getValue(property, true);
		viewer.setAtomProperty(bitSet, Token.temperature, (int) v, (float) v, v + "", null, null);
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

	public boolean isSubstructureHighlighted()
	{
		return substructureHighlighted;
	}

	public void setSubstructureHighlighted(boolean substructureHighlighted)
	{
		this.substructureHighlighted = substructureHighlighted;
	}

	public void moveTo(Vector3f clusterPos)
	{
		Vector3f center = new Vector3f(viewer.getAtomSetCenter(getBitSet()));
		Vector3f dest = new Vector3f(clusterPos);
		dest.sub(center);
		viewer.setAtomCoordRelative(dest, getBitSet());
	}
}
