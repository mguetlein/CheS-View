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
import cluster.Model;

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
	Model model;
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
		clustering.getModelActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(false);
			}
		});
		clustering.getModelWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(false);
			}
		});
	}

	private static String getKey(Cluster c, MoleculeProperty p, Model m)
	{
		return (c == null ? "null" : c.getName()) + "_" + p.toString() + "_" + (m == null ? "null" : m.toString());
	}

	private abstract class PlotData
	{
		public abstract AbstractFreeChartPanel getPlot();
	}

	private class NumericPlotData extends PlotData
	{
		List<String> captions;
		List<double[]> vals;

		public NumericPlotData(Cluster c, MoleculeProperty p, Model m)
		{
			Double v[] = new Double[0];
			for (Cluster cc : clustering.getClusters())
				v = ArrayUtil.concat(Double.class, v, cc.getDoubleValues(p));
			captions = new ArrayList<String>();
			vals = new ArrayList<double[]>();
			captions.add("Dataset");
			vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
			if (c != null)
			{
				captions.add(c.getName());
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(c.getDoubleValues(p))));
			}
			if (m != null)
			{
				captions.add(m.toString());
				vals.add(new double[] { m.getDoubleValue(p) });
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

		public NominalPlotData(Cluster c, MoleculeProperty p, Model m)
		{
			String v[] = new String[0];
			for (Cluster cc : clustering.getClusters())
				if (cc != c)
					v = ArrayUtil.concat(String.class, v, cc.getStringValues(p, m));
			CountedSet<String> datasetSet = CountedSet.fromArray(v);
			List<String> datasetValues = datasetSet.values(new DefaultComparator<String>());

			CountedSet<String> clusterSet = null;
			if (c != null)
			{
				v = c.getStringValues(p, m);
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
			String compoundVal = null;
			if (m != null)
			{
				compoundVal = m.getStringValue(p);

				if (!datasetValues.contains(compoundVal))
				{
					datasetValues.add(compoundVal);
					Collections.sort(datasetValues, new ToStringComparator());
				}
			}
			data = new LinkedHashMap<String, List<Double>>();
			if (m != null)
			{
				List<Double> compoundCounts = new ArrayList<Double>();
				for (String o : datasetValues)
					compoundCounts.add(compoundVal.equals(o) ? 1.0 : 0.0);
				data.put(m.toString(), compoundCounts);
			}
			if (c != null)
			{
				List<Double> clusterCounts = new ArrayList<Double>();
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

	private static Color COMPOUND = Color.YELLOW;

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

		int mIndex = clustering.getModelWatched().getSelected();
		if (mIndex == -1)
			mIndex = clustering.getModelActive().getSelected();
		Model m = null;
		if (mIndex != -1)
			m = clustering.getModelWithModelIndex(mIndex);

		if (force || cluster != c || property != prop || model != m)
		{
			cluster = c;
			property = prop;
			model = m;

			if (property == null)
				setVisible(false);
			else
			{
				final Cluster fCluster = this.cluster;
				final MoleculeProperty fProperty = this.property;
				final Model fModel = this.model;

				Thread th = new Thread(new Runnable()
				{
					public void run()
					{
						String key = getKey(fCluster, fProperty, fModel);
						if (force && cache.containsKey(key))
							cache.remove(key);
						PlotData d = null;
						if (!cache.containsKey(key))
						{
							MoleculeProperty.Type type = fProperty.getType();
							if (type == Type.NOMINAL)
								d = new NominalPlotData(fCluster, fProperty, fModel);
							else if (type == Type.NUMERIC)
								d = new NumericPlotData(fCluster, fProperty, fModel);
							cache.put(key, d);
						}
						d = cache.get(key);
						AbstractFreeChartPanel p = null;
						if (d != null)
						{
							p = d.getPlot();
							if (fCluster == null)
							{
								if (fModel == null)
									p.setSeriesColor(0, Color.BLUE);
								else
								{
									p.setSeriesColor(0, COMPOUND);
									p.setSeriesColor(1, Color.BLUE);
								}
							}
							else
							{
								if (fModel == null)
								{
									p.setSeriesColor(0, Color.RED);
									p.setSeriesColor(1, Color.BLUE);
								}
								else
								{
									p.setSeriesColor(0, COMPOUND);
									p.setSeriesColor(1, Color.RED);
									p.setSeriesColor(2, Color.BLUE);
								}
							}
							p.setOpaqueFalse();
							p.setForegroundColor(Settings.FOREGROUND);
							p.setShadowVisible(false);
							p.setIntegerTickUnits();
							p.setPreferredSize(new Dimension(400, 220));

						}

						setIgnoreRepaint(true);
						removeAll();

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
						if (p != null)
							add(p, BorderLayout.CENTER);
						revalidate();
						setVisible(true);
						setIgnoreRepaint(false);
					}
				});
				th.start();
			}
		}
	}
}
