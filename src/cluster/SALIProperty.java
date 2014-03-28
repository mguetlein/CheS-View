package cluster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import main.Settings;
import util.DoubleArraySummary;
import dataInterface.NumericDynamicCompoundProperty;

public class SALIProperty extends NumericDynamicCompoundProperty
{
	String target;
	boolean max;

	public SALIProperty(Double[] endpointVals, double[][] featureDistanceMatrix, String target, boolean max)
	{
		super(computeMaxSali(endpointVals, featureDistanceMatrix, max));
		this.target = target;
		this.max = max;
	}

	public static final double MIN_ENDPOINT_DEV = 0.1;
	public static final double IDENTICAL_FEATURES_SALI = 1000.0;

	public static final String MIN_ENDPOINT_DEV_STR = ((int) (MIN_ENDPOINT_DEV * 100)) + "%";

	private static class EqualFeatureTuple
	{
		private Double id;
		private Boolean isCliff;
		private Set<Integer> indices = new HashSet<Integer>();

		public boolean isCliff(double[][] featureDistanceMatrix, Double[] endpointVals, double minEndpointDiff)
		{
			if (isCliff == null)
			{
				isCliff = false;
				for (Integer idx1 : indices)
				{
					if (endpointVals[idx1] == null)
						continue;
					for (Integer idx2 : indices)
					{
						if (endpointVals[idx2] == null)
							continue;
						if (idx1 == idx2)
							continue;
						if (featureDistanceMatrix[idx1][idx2] != 0)
							throw new IllegalStateException("distance measure not transitiv");
						double endpointDist = Math.abs(endpointVals[idx1] - endpointVals[idx2]);
						if (endpointDist >= minEndpointDiff)
						{
							isCliff = true;
							break;
						}
					}
					if (isCliff)
						break;
				}
			}
			return isCliff;
		}
	}

	private static Double[] computeMaxSali(Double[] endpointVals, double[][] featureDistanceMatrix, boolean max)
	{
		if (endpointVals.length != featureDistanceMatrix.length
				|| endpointVals.length != featureDistanceMatrix[0].length)
			throw new IllegalArgumentException();

		List<EqualFeatureTuple> eqTuplesList = new ArrayList<EqualFeatureTuple>();
		EqualFeatureTuple eqTuplesArray[] = new EqualFeatureTuple[endpointVals.length];
		for (int i = 0; i < eqTuplesArray.length - 1; i++)
		{
			for (int j = i + 1; j < eqTuplesArray.length; j++)
			{
				if (featureDistanceMatrix[i][j] != featureDistanceMatrix[j][i])
					throw new IllegalStateException("distance measure not symmetric");
				if (featureDistanceMatrix[i][j] == 0)
				{
					if (eqTuplesArray[i] != null && eqTuplesArray[j] != null && eqTuplesArray[i] != eqTuplesArray[j])
						throw new IllegalStateException();
					EqualFeatureTuple n = eqTuplesArray[i];
					if (n == null)
						n = eqTuplesArray[j];
					if (n == null)
					{
						n = new EqualFeatureTuple();
						eqTuplesList.add(n);
					}
					n.indices.add(i);
					n.indices.add(j);
					eqTuplesArray[i] = n;
					eqTuplesArray[j] = n;
				}
			}
		}
		double minEndpointDiff = MIN_ENDPOINT_DEV;
		int identicalFeatsCompounds = 0;
		int identicalFeatsCliffs = 0;
		for (EqualFeatureTuple n : eqTuplesList)
			if (n.isCliff(featureDistanceMatrix, endpointVals, minEndpointDiff))
			{
				n.id = IDENTICAL_FEATURES_SALI + identicalFeatsCliffs;
				identicalFeatsCliffs++;
				identicalFeatsCompounds += n.indices.size();
			}

		Double salis[] = new Double[endpointVals.length];
		for (int i = 0; i < salis.length; i++)
		{
			if (eqTuplesArray[i] != null
					&& eqTuplesArray[i].isCliff(featureDistanceMatrix, endpointVals, minEndpointDiff))
			{
				salis[i] = eqTuplesArray[i].id;
				continue;
			}
			if (endpointVals[i] == null)
				continue;
			List<Double> allSalis = new ArrayList<Double>();
			for (int j = 0; j < salis.length; j++)
			{
				if (i == j)
					continue;
				if (endpointVals[j] == null)
					continue;
				if (endpointVals[i] < 0 || endpointVals[i] > 1)
					throw new IllegalStateException("please normalize!");
				double endpointDist = Math.abs(endpointVals[i] - endpointVals[j]);
				if (endpointDist < minEndpointDiff)
					continue;
				if (featureDistanceMatrix[i][j] == 0)
					throw new IllegalStateException();
				double tmpSali = endpointDist / featureDistanceMatrix[i][j];
				allSalis.add(tmpSali);
			}
			if (max)
				salis[i] = DoubleArraySummary.create(allSalis).getMax();
			else
				salis[i] = DoubleArraySummary.create(allSalis).getMean();
		}

		if (identicalFeatsCompounds > 0)
		{
			String warning = Settings.text("props.sali.identical-warning", identicalFeatsCompounds + "",
					identicalFeatsCliffs + "", MIN_ENDPOINT_DEV_STR, IDENTICAL_FEATURES_SALI + "") + "\n\n";
			warning += "Details:\n";
			warning += Settings.text("props.sali.detail", MIN_ENDPOINT_DEV_STR);
			JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, warning, "Warning", JOptionPane.WARNING_MESSAGE);
		}
		return salis;
	}

	@Override
	public String getName()
	{
		return Settings.text("props.sali");
	}

	@Override
	public String getDescription()
	{
		return Settings.text("props.sali.desc", max ? "Max" : "Mean", target);
	}

}
