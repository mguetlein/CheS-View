package cluster;

import alg.DistanceMeasure;
import dataInterface.DefaultCompoundProperty;

public class DistanceToProperty extends DefaultCompoundProperty// NumericDynamicCompoundProperty
{

	Compound comp;
	DistanceMeasure measure;

	public DistanceToProperty(Compound comp, DistanceMeasure measure, Double[] vals)
	{
		super();
		this.comp = comp;
		this.measure = measure;
		setType(Type.NUMERIC);
		setDoubleValues(vals);
	}

	@Override
	public String getName()
	{
		if (measure != DistanceMeasure.UNKNOWN_DISTANCE)
			return measure + " distance to " + comp;
		else
			return "Distance to " + comp;
	}

	public Compound getCompound()
	{
		return comp;
	}

	@Override
	public String getDescription()
	{
		return "Distance based on features and distance mesure that have been used for embedding";
	}

}
