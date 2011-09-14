package gui;

import freechart.AbstractFreeChartPanel;
import freechart.HistogramPanel;
import freechart.StackedBarPlot;
import gui.ViewControler.Highlighter;
import gui.ViewControler.MoleculePropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JPanel;

import main.Settings;
import util.ArrayUtil;
import util.CountedSet;
import util.DefaultComparator;
import cluster.Cluster;
import cluster.Clustering;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class ChartPanel extends JPanel
{
	Clustering clustering;
	ViewControler viewControler;

	Cluster cluster;
	MoleculeProperty property;

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
		setLayout(new BorderLayout());
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

	private void update(boolean force)
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
						//				System.err.println(cluster);
						//				System.err.println(property);

						AbstractFreeChartPanel p = null;
						MoleculeProperty.Type type = fProperty.getType();
						if (type == Type.NOMINAL)
						{
							String v[] = new String[0];
							for (Cluster cc : clustering.getClusters())
								if (cc != fCluster)
									v = ArrayUtil.concat(String.class, v, cc.getStringValues(fProperty));
							CountedSet<String> datasetSet = CountedSet.fromArray(v);
							List<String> datasetValues = datasetSet.values(new DefaultComparator<String>());

							CountedSet<String> clusterSet = null;
							List<Double> clusterCounts = new ArrayList<Double>();
							if (fCluster != null)
							{
								v = fCluster.getStringValues(fProperty);
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
									Collections.sort(datasetValues);
							}

							LinkedHashMap<String, List<Double>> data = new LinkedHashMap<String, List<Double>>();
							if (fCluster != null)
							{
								for (String o : datasetValues)
									clusterCounts.add((double) clusterSet.getCount(o));
								data.put(fCluster.toString(), clusterCounts);
							}
							List<Double> datasetCounts = new ArrayList<Double>();
							for (String o : datasetValues)
								datasetCounts.add((double) datasetSet.getCount(o));
							data.put("Dataset", datasetCounts);

							String[] vals = new String[datasetValues.size()];
							datasetValues.toArray(vals);
							p = new StackedBarPlot(null, fProperty.toString(), "#compounds", data, vals);
						}
						else if (type == Type.NUMERIC)
						{
							Double v[] = new Double[0];
							for (Cluster cc : clustering.getClusters())
								v = ArrayUtil.concat(Double.class, v, cc.getDoubleValues(fProperty));
							List<String> captions = new ArrayList<String>();
							List<double[]> vals = new ArrayList<double[]>();
							if (fCluster == null)
							{
								captions.add("Dataset");
								vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
							}
							else
							{
								captions.add("Dataset");
								vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
								captions.add(fCluster.toString());
								vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(fCluster
										.getDoubleValues(fProperty))));

							}
							p = new HistogramPanel(null, null, fProperty.toString(), "#compounds", captions, vals, 20);

						}
						if (p != null)
						{
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
							p.setPreferredSize(new Dimension(350, 220));

							removeAll();
							add(p);
							setVisible(true);
							setIgnoreRepaint(false);
							invalidate();
							repaint();
						}
					}
				});
				th.start();
			}
		}
	}
}
