package cluster;

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
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class ExportData
{
	public static void exportClusters(Clustering clustering, int clusterIndices[])
	{
		List<Integer> l = new ArrayList<Integer>();
		for (int i = 0; i < clusterIndices.length; i++)
			for (Model m : clustering.getCluster(clusterIndices[i]).getModels())
				l.add(m.getModelOrigIndex());
		exportModels(clustering, l);
	}

	public static void exportModels(Clustering clustering, List<Integer> modelIndices)
	{
		exportModels(clustering, ArrayUtil.toPrimitiveIntArray(modelIndices));
	}

	public static void exportModels(Clustering clustering, int modelOrigIndices[])
	{
		JFileChooser f = new JFileChooser(clustering.getOrigSdfFile());//origSDFFile);
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

		DoubleKeyHashMap<Integer, Object, Object> featureValues = new DoubleKeyHashMap<Integer, Object, Object>();
		for (Integer j : modelOrigIndices)
		{
			if (clustering.numClusters() > 1)
			{
				if (clustering.isClusterAlgorithmDisjoint())
				{
					Model m = null;
					for (Cluster c : clustering.getClusters())
						for (Model mm : c.getModels())
							if (mm.getModelOrigIndex() == j)
							{
								m = mm;
								break;
							}
					featureValues.put(j, (clustering.getClusterAlgorithm() + " cluster assignement").replace(' ', '_'),
							clustering.getClusterIndexForModel(m));
				}
				else
				{
					for (Cluster c : clustering.getClusters())
					{
						if (!c.containsNotClusteredCompounds())
						{
							Model m = null;
							for (Model mm : c.getModels())
								if (mm.getModelOrigIndex() == j)
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
			//			if (csvExport)
			//			{
			for (MoleculeProperty p : clustering.getProperties())
				if (!p.getName().matches("(?i)smiles"))
				{
					if (p.getType() == Type.NUMERIC)
						featureValues.put(j, p, clustering.getCompounds().get(j).getDoubleValue(p) == null ? ""
								: clustering.getCompounds().get(j).getDoubleValue(p));
					else
						featureValues.put(j, p, clustering.getCompounds().get(j).getStringValue(p) == null ? ""
								: clustering.getCompounds().get(j).getStringValue(p));
				}
			//			}
			for (MoleculeProperty p : clustering.getFeatures())
				if (!(p instanceof IntegratedProperty))
					if (p.getType() == Type.NUMERIC)
						featureValues.put(j, p, clustering.getCompounds().get(j).getDoubleValue(p));
					else
						featureValues.put(j, p, clustering.getCompounds().get(j).getStringValue(p));
		}
		List<Object> skipUniform = new ArrayList<Object>();
		List<Object> skipNull = new ArrayList<Object>();
		List<Object> skip = new ArrayList<Object>();
		if (featureValues.keySet1().size() > 0 && featureValues.keySet2(modelOrigIndices[0]).size() > 0)
			for (Object prop : featureValues.keySet2(modelOrigIndices[0]))
			{
				boolean uniform = true;
				boolean nullValues = false;
				boolean first = true;
				Object val = null;
				for (Integer j : modelOrigIndices)
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
				if (uniform && modelOrigIndices.length > 1)
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
			for (Integer j : modelOrigIndices)
				featureValues.remove(j, p);
		}
		if (csvExport)
		{
			List<Object> feats = new ArrayList<Object>();
			for (Integer j : modelOrigIndices)
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
				for (Integer modelIndex : modelOrigIndices)
				{
					CompoundData c = clustering.getCompounds().get(modelIndex);
					b.write("\"");
					b.write(c.getSmiles());
					b.write("\"");
					for (Object feat : feats)
					{
						b.write(",\"");
						Object val = featureValues.get(modelIndex, feat);
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
			SDFUtil.filter(clustering.getOrigSdfFile(), dest, modelOrigIndices, featureValues);
	}
}
