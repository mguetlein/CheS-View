package cluster;

import gui.CheckBoxSelectDialog;
import gui.LaunchCheSMapper;
import io.SDFUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import main.CheSMapping;
import main.Settings;
import util.ArrayUtil;
import util.DoubleKeyHashMap;
import util.FileUtil;
import util.ListUtil;
import util.ObjectUtil;
import util.StringUtil;
import workflow.MappingWorkflow;
import workflow.MappingWorkflow.DescriptorSelection;
import data.ClusteringData;
import data.DatasetFile;
import data.cdk.CDKProperty;
import data.obdesc.OBDescriptorProperty;
import data.obfingerprints.OBFingerprintProperty;
import dataInterface.CompoundData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;

public class ExportData
{
	public static void exportAll(Clustering clustering, CompoundProperty compoundDescriptorFeature, Script script)
	{
		List<Integer> l = new ArrayList<Integer>();
		for (Compound m : clustering.getCompounds(false))
			l.add(m.getCompoundOrigIndex());
		exportCompounds(clustering, l, compoundDescriptorFeature, script);
	}

	public static void exportClusters(Clustering clustering, int clusterIndices[],
			CompoundProperty compoundDescriptorFeature)
	{
		List<Integer> l = new ArrayList<Integer>();
		for (int i = 0; i < clusterIndices.length; i++)
			for (Compound m : clustering.getCluster(clusterIndices[i]).getCompounds())
				l.add(m.getCompoundOrigIndex());
		exportCompounds(clustering, l, compoundDescriptorFeature, null);
	}

	public static void exportCompounds(Clustering clustering, List<Integer> compoundIndices,
			CompoundProperty compoundDescriptorFeature)
	{
		exportCompounds(clustering, ArrayUtil.toPrimitiveIntArray(compoundIndices), compoundDescriptorFeature, null);
	}

	public static void exportCompounds(Clustering clustering, List<Integer> compoundIndices,
			CompoundProperty compoundDescriptorFeature, Script script)
	{
		exportCompounds(clustering, ArrayUtil.toPrimitiveIntArray(compoundIndices), compoundDescriptorFeature, script);
	}

	public static void exportCompounds(Clustering clustering, int compoundOrigIndices[],
			CompoundProperty compoundDescriptorFeature)
	{
		exportCompounds(clustering, compoundOrigIndices, compoundDescriptorFeature, null);
	}

	public static class Script
	{
		String dest;
		boolean allFeatures;
		public boolean skipEqualValues;
		public double skipNullValueRatio;

		public Script(String dest, boolean allFeatures, boolean skipEqualValues, double skipNullValueRatio)
		{
			this.dest = dest;
			this.allFeatures = allFeatures;
			this.skipEqualValues = skipEqualValues;
			this.skipNullValueRatio = skipNullValueRatio;
		}
	}

	public static String propToExportString(CompoundProperty p)
	{
		if (p instanceof CDKProperty)
			return "CDK:" + p.toString();
		if (p instanceof OBDescriptorProperty)
			return "OB:" + p.toString();
		if (p instanceof OBFingerprintProperty)
			return "OB-" + ((OBFingerprintProperty) p).getOBType() + ":" + p.toString();
		return p.toString();
	}

	public static void exportCompounds(Clustering clustering, int compoundOrigIndices[],
			CompoundProperty compoundDescriptorFeature, Script script)
	{
		String dest;
		if (script != null)
			dest = script.dest;
		else
		{
			String dir = clustering.getOrigLocalPath();
			if (dir == null)
				dir = System.getProperty("user.home");
			JFileChooser f = new JFileChooser(dir);//origSDFFile);
			f.setDialogTitle("Save to SDF/CSV file (according to filename extension)");
			int i = f.showSaveDialog(Settings.TOP_LEVEL_FRAME);
			if (i != JFileChooser.APPROVE_OPTION)
				return;
			dest = f.getSelectedFile().getAbsolutePath();
			if (!f.getSelectedFile().exists() && !FileUtil.getFilenamExtension(dest).matches("(?i)sdf")
					&& !FileUtil.getFilenamExtension(dest).matches("(?i)csv"))
				dest += ".sdf";
			if (new File(dest).exists())
			{
				if (JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "File '" + dest
						+ "' already exists, overwrite?", "Warning", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION)
					return;
			}
		}
		boolean csvExport = FileUtil.getFilenamExtension(dest).matches("(?i)csv");

		// file may be overwritten, and then reloaded -> clear
		DatasetFile.clearFilesWith3DSDF(dest);

		CompoundProperty selectedProps[];
		List<CompoundProperty> availableProps = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : clustering.getProperties())
			availableProps.add(p);
		for (CompoundProperty p : clustering.getFeatures())
			availableProps.add(p);

		if (availableProps.size() == 0) // no features to select
			selectedProps = new CompoundProperty[0];
		if (script != null && script.allFeatures)
			selectedProps = ArrayUtil.toArray(CompoundProperty.class, availableProps);
		else
		{
			String title;
			if (csvExport)
			{
				title = "Select features for CSV export";
			}
			else
			{
				title = "Select features for SDF export";
			}
			selectedProps = ArrayUtil.cast(
					CompoundProperty.class,
					CheckBoxSelectDialog.select(Settings.TOP_LEVEL_FRAME, title, null,
							ArrayUtil.toArray(CompoundProperty.class, availableProps), true));
			if (selectedProps == null)//pressed cancel
				return;
		}
		List<CompoundProperty> logFeatures = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : selectedProps)
			if (p.getType() == Type.NUMERIC && p.isLogHighlightingEnabled())
				logFeatures.add(p);
		if (logFeatures.size() > 0)
		{
			int ret = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, "Add log-transformation for feature/s: "
					+ ListUtil.toString(logFeatures));
			if (ret != JOptionPane.OK_OPTION)
				logFeatures.clear();
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
					Object val;
					if (p.getType() == Type.NUMERIC)
					{
						val = clustering.getCompounds().get(j).getDoubleValue(p);
						if (val != null && p.isIntegerInMappedDataset())
							val = StringUtil.formatDouble((Double) val, 0);
					}
					else
						val = clustering.getCompounds().get(j).getStringValue(p);
					if (val == null)
						val = "";
					featureValues.put(j, propToExportString(p), val);
				}

			for (CompoundProperty c : logFeatures)
				featureValues.put(
						j,
						propToExportString(c) + "_log",
						clustering.getCompounds().get(j).getDoubleValue(c) == null ? "" : Math.log10(clustering
								.getCompounds().get(j).getDoubleValue(c)));
		}
		List<Object> skipUniform = new ArrayList<Object>();
		List<Object> skipNull = new ArrayList<Object>();
		List<Object> skip = new ArrayList<Object>();
		if (featureValues.keySet1().size() > 0 && featureValues.keySet2(compoundOrigIndices[0]).size() > 0)
			for (Object prop : featureValues.keySet2(compoundOrigIndices[0]))
			{
				boolean uniform = true;
				int nullValueCount = 0;
				boolean first = true;
				Object val = null;
				for (Integer j : compoundOrigIndices)
				{
					Object newVal = featureValues.get(j, prop);
					if ((newVal == null || newVal.equals("") || new Double(Double.NaN).equals(newVal)))
						nullValueCount++;
					if (first)
					{
						first = false;
						val = newVal;
					}
					else
					{
						if (!ObjectUtil.equals(val, newVal))
							uniform = false;
					}
				}
				if (uniform && compoundOrigIndices.length > 1)
					skipUniform.add(prop);
				if (nullValueCount > 0)
				{
					double ratio = 0;
					if (compoundOrigIndices.length > 1)
						ratio = nullValueCount / (double) compoundOrigIndices.length;
					if (script == null || ratio > script.skipNullValueRatio)
					{
						if (script != null)
							Settings.LOGGER.info("null value ratio " + ratio + " > " + script.skipNullValueRatio
									+ ", skipping from export: " + prop + " ");
						skipNull.add(prop);
					}
				}
			}
		if (skipUniform.size() > 0)
		{
			boolean doSkip;
			if (script != null)
				doSkip = script.skipEqualValues;
			else
			{
				String msg = skipUniform.size() + " feature/s have equal values for each compound.\nSkip from export?";
				int sel = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, msg, "Skip feature",
						JOptionPane.YES_NO_OPTION);
				doSkip = sel == JOptionPane.YES_OPTION;
			}
			if (doSkip)
				for (Object p : skipUniform)
				{
					if (skipNull.contains(p))
						skipNull.remove(p);
					Settings.LOGGER.info("uniform values, skipping from export: " + p + " ");
					skip.add(p);
				}
		}
		if (skipNull.size() > 0)
		{
			boolean doSkip;
			if (script != null)
				doSkip = true;
			else
			{
				String msg = skipNull.size() + " feature/s have null values.\nSkip from export?";
				int sel = JOptionPane.showConfirmDialog(Settings.TOP_LEVEL_FRAME, msg, "Skip feature",
						JOptionPane.YES_NO_OPTION);
				doSkip = sel == JOptionPane.YES_OPTION;
			}
			if (doSkip)
				for (Object p : skipNull)
				{
					if (script == null)
						Settings.LOGGER.info("null values, skipping from export: " + p + " ");
					skip.add(p);
				}
		}
		for (Object p : skip)
		{
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
		{
			HashMap<Integer, Object> newTitle = null;
			if (compoundDescriptorFeature != null)
			{
				newTitle = new HashMap<Integer, Object>();
				for (Integer j : compoundOrigIndices)
				{
					Object val;
					if (compoundDescriptorFeature.getType() == Type.NUMERIC)
					{
						val = clustering.getCompounds().get(j).getDoubleValue(compoundDescriptorFeature);
						if (val != null && compoundDescriptorFeature.isIntegerInMappedDataset())
							val = StringUtil.formatDouble((Double) val, 0);
					}
					else
						val = clustering.getCompounds().get(j).getStringValue(compoundDescriptorFeature);
					if (val == null)
						val = "";
					featureValues.put(j, propToExportString(compoundDescriptorFeature), val);
					newTitle.put(j, val);
				}
			}
			SDFUtil.filter(clustering.getOrigSdfFile(), dest, compoundOrigIndices, featureValues, true, newTitle);
		}

		String msg = "Successfully exported " + compoundOrigIndices.length + " compounds to\n" + dest;
		if (script != null)
			System.out.println("\n" + msg);
		else
			JOptionPane
					.showMessageDialog(Settings.TOP_LEVEL_FRAME, msg, "Export done", JOptionPane.INFORMATION_MESSAGE);
	}

	public static void scriptExport(String datasetFile, DescriptorSelection features, String outfile,
			boolean keepUniform, double missingRatio)
	{
		Properties props = MappingWorkflow.createMappingWorkflow(datasetFile, features, null, null);
		CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(props, "");

		ClusteringData clusteringData = mapping.doMapping();
		Clustering clustering = new Clustering();
		clustering.newClustering(clusteringData);

		//		LaunchCheSMapper.start(mapping);
		//		while (CheSViewer.getFrame() == null || CheSViewer.getClustering() == null)
		//			ThreadUtil.sleep(100);
		//		ExportData.exportAll(CheSViewer.getClustering(), null, new Script(outfile, true, true, true));

		ExportData.exportAll(clustering, null, new Script(outfile, true, !keepUniform, missingRatio));
		//LaunchCheSMapper.exit(CheSViewer.getFrame());
		LaunchCheSMapper.exit(null);
	}

	public static void main(String[] args)
	{
		LaunchCheSMapper.init();

		//String input = "/home/martin/data/valium.csv";
		String input = "/home/martin/data/caco2.sdf";
		scriptExport(input, new DescriptorSelection("integrated"), "/tmp/data.csv", false, 0.1);

	}

}
