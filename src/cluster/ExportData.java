package cluster;

import gui.CheckBoxSelectDialog;
import io.SDFUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import main.Settings;
import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.FileUtil;
import data.DatasetFile;
import data.IntegratedProperty;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;

public class ExportData
{
	public static void exportClusters(Clustering clustering, int clusterIndices[], CompoundProperty logSelectedFeature)
	{
		List<Integer> l = new ArrayList<Integer>();
		for (int i = 0; i < clusterIndices.length; i++)
			for (Compound m : clustering.getCluster(clusterIndices[i]).getCompounds())
				l.add(m.getCompoundOrigIndex());
		exportCompounds(clustering, l, logSelectedFeature);
	}

	public static void exportCompounds(Clustering clustering, List<Integer> compoundIndices,
			CompoundProperty logSelectedFeature)
	{
		exportCompounds(clustering, ArrayUtil.toPrimitiveIntArray(compoundIndices), logSelectedFeature);
	}

	public static void exportCompounds(Clustering clustering, int compoundOrigIndices[],
			CompoundProperty logSelectedFeature)
	{
		String dir = clustering.getOrigLocalPath();
		if (dir == null)
			dir = System.getProperty("user.home");
		JFileChooser f = new JFileChooser(dir);//origSDFFile);
		f.setDialogTitle("Save to SDF/CSV file (according to filename extension)");
		int i = f.showSaveDialog(Settings.TOP_LEVEL_FRAME);
		if (i != JFileChooser.APPROVE_OPTION)
			return;
		String dest = f.getSelectedFile().getAbsolutePath();
		if (!f.getSelectedFile().exists() && !FileUtil.getFilenamExtension(dest).matches("(?i)sdf")
				&& !FileUtil.getFilenamExtension(dest).matches("(?i)csv"))
			dest += ".sdf";
		boolean csvExport = FileUtil.getFilenamExtension(dest).matches("(?i)csv");

		if (new File(dest).exists())
		{
			if (JOptionPane
					.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "File '" + dest + "' already exists, overwrite?",
							"Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
				return;
		}
		// file may be overwritten, and then reloaded -> clear
		DatasetFile.clearFilesWith3DSDF(dest);

		boolean integratedPropsAlreadyIncluded = !csvExport
				&& (clustering.getFullName().endsWith("sdf") || clustering.getFullName().endsWith("SDF"));
		CompoundProperty selectedProps[];
		List<CompoundProperty> availableProps = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : clustering.getProperties())
			if (!integratedPropsAlreadyIncluded || !(p instanceof IntegratedProperty))
				availableProps.add(p);
		for (CompoundProperty p : clustering.getFeatures())
			if (!integratedPropsAlreadyIncluded || !(p instanceof IntegratedProperty))
				availableProps.add(p);

		if (availableProps.size() == 0) // no features to select
			selectedProps = new CompoundProperty[0];
		else
		{
			if (csvExport)
			{
				selectedProps = ArrayUtil.cast(CompoundProperty.class, CheckBoxSelectDialog.select(
						Settings.TOP_LEVEL_FRAME, "Select features for CSV export", null,
						ArrayUtil.toArray(CompoundProperty.class, availableProps), true));
			}
			else
			{
				selectedProps = ArrayUtil
						.cast(CompoundProperty.class,
								CheckBoxSelectDialog
										.select(Settings.TOP_LEVEL_FRAME,
												"Select features for SDF export",
												integratedPropsAlreadyIncluded ? "(Features that are integrated in the original SDF file, will be included in the exported SDF as well.)"
														: "",
												ArrayUtil.toArray(CompoundProperty.class, availableProps), true));
			}
		}
		if (selectedProps == null)//pressed cancel
			return;

		if (logSelectedFeature != null)
		{
			int ret = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "Add log-transformation of '"
					+ logSelectedFeature + "'");
			if (ret != JOptionPane.OK_OPTION)
				logSelectedFeature = null;
		}

		DoubleKeyHashMap<Integer, Object, Object> featureValues = new DoubleKeyHashMap<Integer, Object, Object>();
		for (Integer j : compoundOrigIndices)
		{
			if (clustering.numClusters() > 1)
			{
				if (clustering.isClusterAlgorithmDisjoint())
				{
					Compound m = null;
					for (Cluster c : clustering.getClusters())
						for (Compound mm : c.getCompounds())
							if (mm.getCompoundOrigIndex() == j)
							{
								m = mm;
								break;
							}
					featureValues.put(j, (clustering.getClusterAlgorithm() + " cluster assignement").replace(' ', '_'),
							clustering.getClusterIndexForCompound(m));
				}
				else
				{
					for (Cluster c : clustering.getClusters())
					{
						if (!c.containsNotClusteredCompounds())
						{
							Compound m = null;
							for (Compound mm : c.getCompounds())
								if (mm.getCompoundOrigIndex() == j)
								{
									m = mm;
									break;
								}
							featureValues.put(j, (clustering.getClusterAlgorithm() + " " + c.getName()).replace(' ',
									'_'), m == null ? 0 : 1);

						}
					}
				}
			}

			for (CompoundProperty p : selectedProps)
				if (!p.getName().matches("(?i)smiles"))
				{
					if (p.getType() == Type.NUMERIC)
						featureValues.put(j, p, clustering.getCompounds().get(j).getDoubleValue(p) == null ? ""
								: clustering.getCompounds().get(j).getDoubleValue(p));
					else
						featureValues.put(j, p, clustering.getCompounds().get(j).getStringValue(p) == null ? ""
								: clustering.getCompounds().get(j).getStringValue(p));
				}

			if (logSelectedFeature != null)
				featureValues.put(
						j,
						logSelectedFeature + "_log",
						clustering.getCompounds().get(j).getDoubleValue(logSelectedFeature) == null ? "" : Math
								.log10(clustering.getCompounds().get(j).getDoubleValue(logSelectedFeature)));
		}
		List<Object> skipUniform = new ArrayList<Object>();
		List<Object> skipNull = new ArrayList<Object>();
		List<Object> skip = new ArrayList<Object>();
		if (featureValues.keySet1().size() > 0 && featureValues.keySet2(compoundOrigIndices[0]).size() > 0)
			for (Object prop : featureValues.keySet2(compoundOrigIndices[0]))
			{
				boolean uniform = true;
				boolean nullValues = false;
				boolean first = true;
				Object val = null;
				for (Integer j : compoundOrigIndices)
				{
					Object newVal = featureValues.get(j, prop);
					nullValues |= (newVal == null || new Double(Double.NaN).equals(newVal));
					if (first)
					{
						first = false;
						val = newVal;
					}
					else
					{
						if (val != newVal)
							uniform = false;
					}
				}
				if (uniform && compoundOrigIndices.length > 1)
					skipUniform.add(prop);
				if (nullValues)
					skipNull.add(prop);
			}
		if (skipUniform.size() > 0)
		{
			String msg = skipUniform.size() + " feature/s have equal values for each compound.\nSkip from export?";
			int sel = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, msg, "Skip feature",
					JOptionPane.YES_NO_OPTION);
			if (sel == JOptionPane.YES_OPTION)
				for (Object p : skipUniform)
				{
					if (skipNull.contains(p))
						skipNull.remove(p);
					skip.add(p);
				}
		}
		if (skipNull.size() > 0)
		{
			String msg = skipNull.size() + " feature/s have null values.\nSkip from export?";
			int sel = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, msg, "Skip feature",
					JOptionPane.YES_NO_OPTION);
			if (sel == JOptionPane.YES_OPTION)
				for (Object p : skipNull)
					skip.add(p);
		}
		for (Object p : skip)
		{
			Settings.LOGGER.info("Skipping from export: " + p);
			for (Integer j : compoundOrigIndices)
				featureValues.remove(j, p);
		}
		if (csvExport)
		{
			List<Object> feats = new ArrayList<Object>();
			for (Integer j : compoundOrigIndices)
				if (featureValues.keySet1().size() > 0 && featureValues.keySet2(j) != null)
					for (Object feat : featureValues.keySet2(j))
						if (!feats.contains(feat))
							feats.add(feat);
			File file = new File(dest);
			try
			{
				BufferedWriter b = new BufferedWriter(new FileWriter(file));
				Set<String> featNames = new HashSet<String>();
				b.write("\"SMILES\"");
				for (Object feat : feats)
				{
					b.write(",\"");
					String featName = feat.toString();
					int mult = 2;
					while (featNames.contains(featName))
						featName = feat.toString() + "_" + (mult++);
					featNames.add(featName);
					b.write(featName);
					b.write("\"");
				}
				b.write("\n");
				for (Integer compoundIndex : compoundOrigIndices)
				{
					CompoundData c = clustering.getCompounds().get(compoundIndex);
					b.write("\"");
					b.write(c.getSmiles());
					b.write("\"");
					for (Object feat : feats)
					{
						b.write(",\"");
						Object val = featureValues.get(compoundIndex, feat);
						String s = val == null ? "" : val.toString();
						if (s.contains("\""))
						{
							System.err.println("csv export: replacing \" with ' for feature " + feat + " and value "
									+ s);
							s = s.replace('"', '\'');
						}
						b.write(s);
						b.write("\"");
					}
					b.write("\n");
				}
				b.close();
			}
			catch (IOException e)
			{
				throw new Error(e);
			}
		}
		else
			SDFUtil.filter(clustering.getOrigSdfFile(), dest, compoundOrigIndices, featureValues);
		JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, "Successfully exported " + compoundOrigIndices.length
				+ " compounds to\n" + dest, "Export done", JOptionPane.INFORMATION_MESSAGE);
	}
}
