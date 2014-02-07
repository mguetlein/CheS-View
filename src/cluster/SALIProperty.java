package cluster;

import javax.swing.JOptionPane;

import main.Settings;
import util.StringUtil;
import dataInterface.NumericDynamicCompoundProperty;

public class SALIProperty extends NumericDynamicCompoundProperty
{
	String target;

	public SALIProperty(Double[] endpointVals, double[][] featureDistanceMatrix, String target)
	{
		super(computeMaxSali(endpointVals, featureDistanceMatrix));
		this.target = target;
	}

	public static final double MIN_ENDPOINT_DEV = 0.25;
	public static final double IDENTICAL_FEATURES_SALI = 1000.0;

	private static Double[] computeMaxSali(Double[] endpointVals, double[][] featureDistanceMatrix)
	{
		int identicalFeats = 0;

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
				if (salis[i] == null || tmpSali > salis[i])
					salis[i] = tmpSali;
			}
		}

		if (identicalFeats > 0)
		{
			JOptionPane
					.showConfirmDialog(
							Settings.TOP_LEVEL_FRAME,
							identicalFeats
									+ " compounds have identical feature values but differing endpoint values (by >"
									+ StringUtil.formatDouble(MIN_ENDPOINT_DEV * 100)
									+ "%).\n"
									+ "A fixed value of "
									+ IDENTICAL_FEATURES_SALI
									+ " is assigned to these compounds.\n\n"
									+ "Details:\n"
									+ "The maximum pairwise SALI index is computed to identify if a compound is part of an activity cliff.\n"
									+ "For numeric endpoints, a change in activity of at least "
									+ StringUtil.formatDouble(MIN_ENDPOINT_DEV * 100)
									+ "% must be given to compute SALI values\n"
									+ "(otherwise, very similar compound pairs could get exterme high SALI values with only small differences in activity).\n"
									+ "By definition, compounds have an infinite high SALI index if they are 100% similar, but have no identical endpoint value.\n"
									+ "Here, this compounds have a fixed value of " + IDENTICAL_FEATURES_SALI
									+ " assigned.", "Warning", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_OPTION);
		}

		return salis;
	}

	@Override
	public String getName()
	{
		return "Activity cliffs";
	}

	@Override
	public String getDescription()
	{
		return "Maximum pairwise SALI for : " + target;
	}

}
