package cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import main.Settings;
import util.DoubleArraySummary;
import dataInterface.NumericDynamicCompoundProperty;

public class SALIProperty extends NumericDynamicCompoundProperty
{
	String target;

	public SALIProperty(Double[] endpointVals, double[][] featureDistanceMatrix, String target)
	{
		super(computeMaxSali(endpointVals, featureDistanceMatrix));
		this.target = target;
	}

	public static final double MIN_ENDPOINT_DEV = 0.1;
	public static final double NUM_TOP_PERCENT = 0.1;
	public static final double IDENTICAL_FEATURES_SALI = 1000.0;

	public static final String MIN_ENDPOINT_DEV_STR = ((int) (MIN_ENDPOINT_DEV * 100)) + "%";
	public static final String NUM_TOP_PERCENT_STR = ((int) (NUM_TOP_PERCENT * 100)) + "%";

	private static Double[] computeMaxSali(Double[] endpointVals, double[][] featureDistanceMatrix)
	{
		int identicalFeats = 0;
		int numTopPercent = (int) (endpointVals.length * NUM_TOP_PERCENT);

		if (endpointVals.length != featureDistanceMatrix.length
				|| endpointVals.length != featureDistanceMatrix[0].length)
			throw new IllegalArgumentException();

		Double salis[] = new Double[endpointVals.length];
		for (int i = 0; i < salis.length; i++)
		{
			if (endpointVals[i] == null)
				continue;
			if (endpointVals[i] < 0 || endpointVals[i] > 1)
				throw new IllegalStateException("please normalize!");

			//			int a = 0;
			//			int b = 0;
			//			int c = 0;
			//			int d = 0;

			List<Double> allSalis = new ArrayList<Double>();

			for (int j = 0; j < salis.length; j++)
			{
				if (i == j)
					continue;
				if (endpointVals[j] == null)
					continue;
				double endpointDist = Math.abs(endpointVals[i] - endpointVals[j]);

				//				if (endpointDist < 0.5)
				//				{// small endpoint diff
				//					if (featureDistanceMatrix[i][j] < 0.2)
				//						c++;//high similarity
				//					else
				//						d++;//low similarity
				//				}
				//				else
				//				{//large endpoint diff
				//					if (featureDistanceMatrix[i][j] < 0.2)
				//						a++;//high similarity
				//					else
				//						b++;//low similarity
				//				}
				//				salis[i] = a * Math.log((a * (c + d)) / (double) (c * (a + b))) + b
				//						* Math.log((b * (c + d)) / (double) (d * (a + b)));	

				if (endpointDist == 0)
					continue;
				if (endpointDist < MIN_ENDPOINT_DEV)
					continue;
				if (featureDistanceMatrix[i][j] == 0)
				{
					salis[i] = IDENTICAL_FEATURES_SALI;
					identicalFeats++;
					break;
				}
				double tmpSali = endpointDist / featureDistanceMatrix[i][j];
				//				if (salis[i] == null || tmpSali > salis[i])
				//					salis[i] = tmpSali;
				allSalis.add(tmpSali);
			}
			if (salis[i] == null)
			{
				Collections.sort(allSalis);
				while (allSalis.size() > numTopPercent)
					allSalis.remove(0);
				salis[i] = DoubleArraySummary.create(allSalis).getMean();

				//salis[i] = DoubleArraySummary.create(allSalis).getMax();
			}
		}

		if (identicalFeats > 0)
		{
			String warning = Settings.text("props.sali.identical-warning", identicalFeats + "", MIN_ENDPOINT_DEV_STR,
					IDENTICAL_FEATURES_SALI + "") + "\n\n";
			warning += "Details:\n";
			warning += Settings.text("props.sali.detail", NUM_TOP_PERCENT_STR, MIN_ENDPOINT_DEV_STR);
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
		return Settings.text("props.sali.desc", NUM_TOP_PERCENT_STR, target);
	}

}
