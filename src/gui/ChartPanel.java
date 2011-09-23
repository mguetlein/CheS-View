package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import main.Settings;
import util.ArrayUtil;
import util.CountedSet;
import util.DefaultComparator;
import util.ToStringComparator;
import cluster.Cluster;
import cluster.Clustering;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import freechart.AbstractFreeChartPanel;
import freechart.HistogramPanel;
import freechart.StackedBarPlot;
import gui.ViewControler.Highlighter;
import gui.ViewControler.MoleculePropertyHighlighter;

public class ChartPanel extends JPanel
{
	Clustering clustering;
	ViewControler viewControler;

	Cluster cluster;
	MoleculeProperty property;

	HashMap<String, PlotData> cache = new HashMap<String, ChartPanel.PlotData>();

	private JPanel featurePanel;
	private JLabel featureNameLabel = ComponentFactory.createLabel("");
	private JLabel featureSetLabel = ComponentFactory.createLabel("");
	private JLabel featureDescriptionLabel = ComponentFactory.createLabel("");
	private JLabel featureDescriptionLabelHeader = ComponentFactory.createLabel("Description:");
	private JLabel featureSmartsLabelHeader = ComponentFactory.createLabel("Smarts:");
	private JLabel featureSmartsLabel = ComponentFactory.createLabel("");
	private JLabel featureMappingLabel = ComponentFactory.createLabel("");

	public ChartPanel(Clustering clustering, ViewControler viewControler)
	{
		this.clustering = clustering;
		this.viewControler = viewControler;

		buildLayout();
		addListeners();
		update(true);
	}

	private void buildLayout()
	{
		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		b.append(ComponentFactory.createLabel("<html><b>Feature:</b><html>"));
		b.append(featureNameLabel);
		b.nextLine();
		b.append("");
		b.append(featureSetLabel);
		b.nextLine();
		b.append(featureDescriptionLabelHeader);
		b.append(featureDescriptionLabel);
		b.nextLine();
		b.append(featureSmartsLabelHeader);
		b.append(featureSmartsLabel);
		b.nextLine();
		b.append(ComponentFactory.createLabel("<html>Usage:<html>"));
		b.append(featureMappingLabel);

		featurePanel = b.getPanel();
		featurePanel.setOpaque(false);

		setLayout(new BorderLayout(3, 3));
		setOpaque(false);
	}

	private void addListeners()
	{
		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
				{
					update(false);
				}
			}
		});
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(Clustering.CLUSTER_MODIFIED))
				{
					update(true);
				}
				else if (evt.getPropertyName().equals(Clustering.CLUSTER_REMOVED))
				{
					cache.clear();
					update(true);
				}
				else if (evt.getPropertyName().equals(Clustering.CLUSTER_ADDED))
				{
					cache.clear();
					update(true);
				}
			}
		});
		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(false);
			}
		});
		clustering.getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(false);
			}
		});
	}

	private static String getKey(Cluster c, MoleculeProperty p)
	{
		return (c == null ? "null" : c.getName()) + "_" + p.toString();
	}

	private abstract class PlotData
	{
		public abstract AbstractFreeChartPanel getPlot();
	}

	private class NumericPlotData extends PlotData
	{
		List<String> captions;
		List<double[]> vals;

		public NumericPlotData(Cluster c, MoleculeProperty p)
		{
			Double v[] = new Double[0];
			for (Cluster cc : clustering.getClusters())
				v = ArrayUtil.concat(Double.class, v, cc.getDoubleValues(p));
			captions = new ArrayList<String>();
			vals = new ArrayList<double[]>();
			if (c == null)
			{
				captions.add("Dataset");
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
			}
			else
			{
				captions.add("Dataset");
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
				captions.add(c.getName());
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(c.getDoubleValues(p))));

			}
		}

		@Override
		public AbstractFreeChartPanel getPlot()
		{
			return new HistogramPanel(null, null, null, "#compounds", captions, vals, 20);
		}
	}

	private class NominalPlotData extends PlotData
	{
		LinkedHashMap<String, List<Double>> data;
		String vals[];

		public NominalPlotData(Cluster c, MoleculeProperty p)
		{
			String v[] = new String[0];
			for (Cluster cc : clustering.getClusters())
				if (cc != c)
					v = ArrayUtil.concat(String.class, v, cc.getStringValues(p));
			CountedSet<String> datasetSet = CountedSet.fromArray(v);
			List<String> datasetValues = datasetSet.values(new DefaultComparator<String>());

			CountedSet<String> clusterSet = null;
			List<Double> clusterCounts = new ArrayList<Double>();
			if (c != null)
			{
				v = c.getStringValues(p);
				clusterSet = CountedSet.fromArray(v);
				List<String> clusterValues = clusterSet.values(new DefaultComparator<String>());

				boolean newVal = false;
				for (String vv : clusterValues)
					if (!datasetValues.contains(vv))
					{
						newVal = true;
						datasetValues.add(vv);
					}
				if (newVal)
					Collections.sort(datasetValues, new ToStringComparator());
			}

			data = new LinkedHashMap<String, List<Double>>();
			if (c != null)
			{
				for (String o : datasetValues)
					clusterCounts.add((double) clusterSet.getCount(o));
				data.put(c.getName(), clusterCounts);
			}
			List<Double> datasetCounts = new ArrayList<Double>();
			for (String o : datasetValues)
				datasetCounts.add((double) datasetSet.getCount(o));
			data.put("Dataset", datasetCounts);

			vals = new String[datasetValues.size()];
			datasetValues.toArray(vals);

			for (int i = 0; i < vals.length; i++)
				if (vals[i] == null)
					vals[i] = "null";
		}

		public AbstractFreeChartPanel getPlot()
		{
			return new StackedBarPlot(null, null, "#compounds", data, vals);
		}
	}

	private void update(final boolean force)
	{
		Highlighter h = viewControler.getHighlighter();
		MoleculeProperty prop = null;
		if (h instanceof MoleculePropertyHighlighter)
			prop = ((MoleculePropertyHighlighter) h).getProperty();

		int cIndex = clustering.getClusterActive().getSelected();
		if (cIndex == -1)
			cIndex = clustering.getClusterWatched().getSelected();
		Cluster c = clustering.getCluster(cIndex);
		if (clustering.getNumClusters() == 1)
			c = null;

		if (force || cluster != c || property != prop)
		{
			cluster = c;
			property = prop;

			setVisible(false);

			if (property != null)
			{
				final Cluster fCluster = this.cluster;
				final MoleculeProperty fProperty = this.property;

				Thread th = new Thread(new Runnable()
				{
					public void run()
					{
						String key = getKey(fCluster, fProperty);
						if (force && cache.containsKey(key))
							cache.remove(key);
						PlotData d = null;
						if (!cache.containsKey(key))
						{
							MoleculeProperty.Type type = fProperty.getType();
							if (type == Type.NOMINAL)
								d = new NominalPlotData(fCluster, fProperty);
							else if (type == Type.NUMERIC)
								d = new NumericPlotData(fCluster, fProperty);
							cache.put(key, d);
						}
						d = cache.get(key);
						AbstractFreeChartPanel p = null;
						if (d != null)
						{
							p = d.getPlot();
							setIgnoreRepaint(true);

							if (fCluster == null)
								p.setSeriesColor(0, Color.BLUE);
							else
							{
								p.setSeriesColor(1, Color.BLUE);
								p.setSeriesColor(0, Color.RED);
							}
							p.setOpaqueFalse();
							p.setForegroundColor(Settings.FOREGROUND);
							p.setShadowVisible(false);
							p.setIntegerTickUnits();
							p.setPreferredSize(new Dimension(400, 220));
						}
						removeAll();
						if (p != null)
							add(p);

						featureNameLabel.setText(fProperty.toString());
						featureSetLabel.setText(fProperty.getMoleculePropertySet().toString());
						//hack, ommits this for cdk features
						featureSetLabel.setVisible(fProperty.getMoleculePropertySet().isSizeDynamic());
						featureDescriptionLabel.setText(fProperty.getDescription() + "");
						featureDescriptionLabel.setVisible(fProperty.getDescription() != null);
						featureDescriptionLabelHeader.setVisible(fProperty.getDescription() != null);
						featureSmartsLabel.setText(fProperty.getSmarts() + "");
						featureSmartsLabel.setVisible(fProperty.getSmarts() != null);
						featureSmartsLabelHeader.setVisible(fProperty.getSmarts() != null);
						featureMappingLabel
								.setText((fProperty.getMoleculePropertySet().isUsedForMapping() ? "Used for clustering and/or embedding."
										: "NOT used for clustering and/or embedding."));
						add(featurePanel, BorderLayout.NORTH);

						//						setPreferredSize(new Dimension(400, getPreferredSize().height));
						setIgnoreRepaint(false);
						invalidate();
						repaint();
						setVisible(true);
					}
				});
				th.start();
			}
		}
	}
}
