package gui;

import freechart.AbstractFreeChartPanel;
import freechart.FreeChartPanel.ChartMouseSelectionListener;
import freechart.HistogramPanel;
import freechart.StackedBarPlot;
import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.Highlighter;
import gui.util.CompoundPropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import main.ScreenSetup;
import util.ArrayUtil;
import util.ColorUtil;
import util.CountedSet;
import util.DefaultComparator;
import util.ObjectUtil;
import util.SequentialWorkerThread;
import util.ToStringComparator;
import cluster.Cluster;
import cluster.Clustering;
import cluster.Compound;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyUtil;

public class ChartPanel extends TransparentViewPanel
{
	Clustering clustering;
	ViewControler viewControler;
	GUIControler guiControler;

	Cluster cluster;
	List<Compound> compounds;
	CompoundProperty property;

	private JPanel featurePanel;
	private JLabel featureNameLabel = ComponentFactory.createViewLabel("");
	private JLabel featureSetLabel = ComponentFactory.createViewLabel("");
	private JLabel featureDescriptionLabel = ComponentFactory.createViewLabel("");
	private JLabel featureDescriptionLabelHeader = ComponentFactory.createViewLabel("Description:");
	private JLabel featureSmartsLabelHeader = ComponentFactory.createViewLabel("Smarts:");
	private JLabel featureSmartsLabel = ComponentFactory.createViewLabel("");
	private JLabel featureMappingLabel = ComponentFactory.createViewLabel("");
	private JLabel featureMissingLabel = ComponentFactory.createViewLabel("");

	Set<String> cardContents = new HashSet<String>();
	JPanel cardPanel;

	SequentialWorkerThread workerThread = new SequentialWorkerThread();

	public ChartPanel(Clustering clustering, ViewControler viewControler, GUIControler guiControler)
	{
		this.clustering = clustering;
		this.viewControler = viewControler;
		this.guiControler = guiControler;

		buildLayout();
		addListeners();
		update(true);
	}

	private void buildLayout()
	{
		DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		b.setLineGapSize(Sizes.pixel(2));
		b.append(ComponentFactory.createViewLabel("<html><b>Feature:</b><html>"));
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
		b.append(ComponentFactory.createViewLabel("<html>Usage:<html>"));
		b.append(featureMappingLabel);
		b.nextLine();
		b.append(ComponentFactory.createViewLabel("<html>Missing values:<html>"));
		b.append(featureMissingLabel);

		featurePanel = b.getPanel();
		featurePanel.setOpaque(false);

		setLayout(new BorderLayout(3, 3));

		add(featurePanel, BorderLayout.NORTH);

		cardPanel = new JPanel(new CardLayout());
		cardPanel.setOpaque(false);
		add(cardPanel, BorderLayout.CENTER);

		setOpaque(true);
		//		setBackground(Settings.TRANSPARENT_BACKGROUND);
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
					cardPanel.removeAll();
					cardContents.clear();
					update(true);
				}
				else if (evt.getPropertyName().equals(Clustering.CLUSTER_ADDED))
				{
					cardPanel.removeAll();
					cardContents.clear();
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
		clustering.getCompoundActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(false);
			}
		});
		clustering.getCompoundWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(false);
			}
		});

	}

	private static String getKey(Cluster c, CompoundProperty p, List<Compound> m)
	{
		String mString = "";
		for (Compound compound : m)
			mString = mString.concat(compound.toString());
		return (c == null ? "null" : c.getName()) + "_" + p.toString() + "_" + mString;
	}

	boolean selfUpdate = false;
	List<Integer> selfUpdateCompounds = null;

	private abstract class CompoundSelector implements ChartMouseSelectionListener
	{
		protected abstract boolean hasSelectionCriterionChanged();

		protected abstract void updateSelectionCriterion();

		protected abstract boolean isSelected(Compound m, CompoundProperty p);

		@Override
		public void hoverEvent()
		{
			handleEvent(true, false);
		}

		@Override
		public void clickEvent(boolean ctrlDown)
		{
			handleEvent(false, ctrlDown);
		}

		private void handleEvent(boolean hover, boolean ctrlDown)
		{
			System.err.println();
			if (selfUpdate)
			{
				System.err.println("self update");
				return;

			}
			selfUpdate = true;
			try
			{
				if (!hasSelectionCriterionChanged() && hover)
				{
					System.err.println("selection criterion has not changed");
					return;
				}
				updateSelectionCriterion();
				if (this instanceof NumericCompoundSelector)
					System.err.println("interval : " + ((NumericCompoundSelector) this).hist.getSelectedMin() + " "
							+ ((NumericCompoundSelector) this).hist.getSelectedMax());

				//				if (clustering.isClusterActive())
				//				{
				Highlighter h = viewControler.getHighlighter();
				CompoundProperty prop = null;
				if (h instanceof CompoundPropertyHighlighter)
					prop = ((CompoundPropertyHighlighter) h).getProperty();

				final List<Integer> m = new ArrayList<Integer>();
				Iterable<Compound> compounds;
				if (clustering.isClusterActive())
					compounds = clustering.getCluster(clustering.getClusterActive().getSelected()).getCompounds();
				else
					compounds = clustering.getCompounds(false);
				for (Compound compound : compounds)
					if (isSelected(compound, prop))
						m.add(compound.getCompoundIndex());

				if (hover)
				{
					if (ObjectUtil.equals(selfUpdateCompounds, m))
						return;
					selfUpdateCompounds = m;
					System.err.println("updating via chart panel " + m);
					clustering.getCompoundWatched().setSelectedIndices(ArrayUtil.toPrimitiveIntArray(m));
				}
				else
				{
					System.err.println("before: "
							+ ArrayUtil.toString(clustering.getCompoundActive().getSelectedIndices()));
					System.err.println("select " + (!ctrlDown) + " " + m);
					clustering.getCompoundActive().setSelectedIndices(ArrayUtil.toPrimitiveIntArray(m), !ctrlDown);
					System.err
							.println("after: " + ArrayUtil.toString(clustering.getCompoundActive().getSelectedIndices()));
					System.err.println();
				}
			}
			finally
			{
				selfUpdate = false;
			}
		}
	}

	double selectedMin = 1.0;
	double selectedMax = 0.0;

	private class NumericCompoundSelector extends CompoundSelector
	{
		HistogramPanel hist;

		public NumericCompoundSelector(HistogramPanel hist)
		{
			this.hist = hist;
		}

		@Override
		protected boolean hasSelectionCriterionChanged()
		{
			return selectedMin != hist.getSelectedMin() || selectedMax != hist.getSelectedMax();
		}

		@Override
		protected void updateSelectionCriterion()
		{
			selectedMin = hist.getSelectedMin();
			selectedMax = hist.getSelectedMax();
		}

		@Override
		protected boolean isSelected(Compound m, CompoundProperty p)
		{
			Double d = m.getDoubleValue(p);
			return d != null && d >= selectedMin && d <= selectedMax;
		}
	}

	private abstract class PlotData
	{
		AbstractFreeChartPanel plot;

		public AbstractFreeChartPanel getPlot()
		{
			return plot;
		}
	}

	private class NumericPlotData extends PlotData
	{
		List<String> captions;
		List<double[]> vals;

		public NumericPlotData(Cluster c, CompoundProperty p, List<Compound> m)
		{
			Double v[] = clustering.getDoubleValues(p);
			captions = new ArrayList<String>();
			vals = new ArrayList<double[]>();
			captions.add("Dataset");
			vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
			if (c != null)
			{
				captions.add(c.getName());
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(c.getDoubleValues(p))));
			}

			Double mVals[] = new Double[m.size()];
			boolean notNull = false;
			for (int i = 0; i < mVals.length; i++)
			{
				mVals[i] = m.get(i).getDoubleValue(p);
				notNull |= mVals[i] != null;
			}
			if (m.size() > 0 && notNull)
			{
				if (m.size() == 1)
					captions.add(m.get(0).toString());
				else
					captions.add("Selected compounds");
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(mVals)));
			}

			plot = new HistogramPanel(null, null, null, "#compounds", captions, vals, 20);
			plot.addSelectionListener(new NumericCompoundSelector((HistogramPanel) plot));
			configurePlotColors(plot, c, m, p);
		}
	}

	String selectedCategory;

	private class NominalCompoundSelector extends CompoundSelector
	{
		StackedBarPlot bar;

		public NominalCompoundSelector(StackedBarPlot bar)
		{
			this.bar = bar;
		}

		@Override
		protected boolean hasSelectionCriterionChanged()
		{
			return !ObjectUtil.equals(selectedCategory, bar.getSelectedCategory());
		}

		@Override
		protected void updateSelectionCriterion()
		{
			selectedCategory = bar.getSelectedCategory();
		}

		@Override
		protected boolean isSelected(Compound m, CompoundProperty p)
		{
			return ObjectUtil.equals(m.getStringValue(p), selectedCategory);
		}
	}

	private class NominalPlotData extends PlotData
	{
		LinkedHashMap<String, List<Double>> data;
		String vals[];

		public NominalPlotData(Cluster c, CompoundProperty p, List<Compound> ms)
		{
			Compound m = ms.size() > 0 ? ms.get(0) : null;

			String v[] = clustering.getStringValues(p, m);
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
			if (m != null && m.getStringValue(p) != null)
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

			plot = new StackedBarPlot(null, null, "#compounds", StackedBarPlot.convertTotalToAdditive(data), vals);
			plot.addSelectionListener(new NominalCompoundSelector((StackedBarPlot) plot));
			configurePlotColors(plot, c, ms, p);
		}
	}

	private void configurePlotColors(AbstractFreeChartPanel chartPanel, Cluster cluster, List<Compound> compounds,
			CompoundProperty property)
	{
		int dIndex = -1;
		int cIndex = -1;
		int mIndex = -1;

		if (cluster == null)
		{
			if (compounds.size() == 0)
				dIndex = 0;
			else
			{
				mIndex = 0;
				dIndex = 1;
			}
		}
		else
		{
			if (compounds.size() == 0)
			{
				cIndex = 0;
				dIndex = 1;
			}
			else
			{
				mIndex = 0;
				cIndex = 1;
				dIndex = 2;
			}
		}

		if (chartPanel instanceof StackedBarPlot)
		{
			if (mIndex != -1)
				throw new IllegalArgumentException(
						"does NOT help much in terms of visualisation (color code should be enough), difficult to realize in terms of color brightness");

			Color cols[] = CompoundPropertyUtil.getNominalColors(property);
			if (cIndex == -1)
			{
				chartPanel.setSeriesColor(dIndex, ColorUtil.grayscale(CompoundPropertyUtil.getColor(0)));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(dIndex, cols);
			}
			else
			{
				chartPanel.setSeriesColor(dIndex,
						ColorUtil.grayscale(CompoundPropertyUtil.getColor(0).darker().darker().darker()));
				chartPanel.setSeriesColor(cIndex, ColorUtil.grayscale(CompoundPropertyUtil.getColor(0)).brighter());

				((StackedBarPlot) chartPanel).setSeriesCategoryColors(dIndex,
						ColorUtil.darker(ColorUtil.darker(ColorUtil.darker(cols))));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(cIndex, ColorUtil.brighter(cols));
			}
		}
		else
		{
			if (cIndex == -1)
				chartPanel.setSeriesColor(dIndex, CompoundPropertyUtil.getColor(0));
			else
			{
				chartPanel.setSeriesColor(dIndex, CompoundPropertyUtil.getColor(0).darker().darker().darker());
				chartPanel.setSeriesColor(cIndex, CompoundPropertyUtil.getColor(0).brighter());
			}

			if (mIndex != -1)
				chartPanel.setSeriesColor(mIndex, CompoundPropertyUtil.getColor(1));
		}

		chartPanel.setOpaqueFalse();
		chartPanel.setForegroundColor(ComponentFactory.FOREGROUND);
		final AbstractFreeChartPanel finalP = chartPanel;
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_BACKGROUND_CHANGED))
					finalP.setForegroundColor(ComponentFactory.FOREGROUND);
			}
		});
		chartPanel.setShadowVisible(false);
		chartPanel.setIntegerTickUnits();
		chartPanel.setBarWidthLimited();
		chartPanel.setFontSize(ScreenSetup.SETUP.getFontSize());
	}

	private void update(final boolean force)
	{
		if (clustering.getNumClusters() == 0)
		{
			setVisible(false);
			return;
		}

		Highlighter h = viewControler.getHighlighter();
		CompoundProperty prop = null;
		if (h instanceof CompoundPropertyHighlighter)
			prop = ((CompoundPropertyHighlighter) h).getProperty();

		int cIndex = clustering.getClusterActive().getSelected();
		if (cIndex == -1)
			cIndex = clustering.getClusterWatched().getSelected();
		Cluster c = clustering.getCluster(cIndex);
		if (clustering.getNumClusters() == 1)
			c = null;

		int mIndex[] = clustering.getCompoundActive().getSelectedIndices();
		if (mIndex.length == 0)
			mIndex = clustering.getCompoundWatched().getSelectedIndices();

		List<Compound> ms = new ArrayList<Compound>();
		for (int i : mIndex)
			ms.add(clustering.getCompoundWithCompoundIndex(i));
		if (prop != null && prop.getType() == Type.NOMINAL)
		{
			//does NOT help much in terms of visualisation (color code should be enough), difficult to realize in terms of color brightness
			ms.clear();
		}

		System.err.println("update " + ms);

		if (force || cluster != c || property != prop || !compounds.equals(ms))
		{
			cluster = c;
			property = prop;
			compounds = ms;

			if (property == null)
				setVisible(false);
			else
			{
				final Cluster fCluster = this.cluster;
				final CompoundProperty fProperty = this.property;
				final List<Compound> fCompounds = this.compounds;

				workerThread.addJob(new Runnable()
				{
					public void run()
					{
						if (fCluster != cluster || fProperty != property || fCompounds != compounds)
							return;

						System.out.println("updating chart");

						String plotKey = getKey(fCluster, fProperty, fCompounds);
						if (force && cardContents.contains(plotKey))
							cardContents.remove(plotKey);
						if (!cardContents.contains(plotKey))
						{
							System.out.println("create new plot");
							CompoundProperty.Type type = fProperty.getType();
							PlotData d = null;
							if (type == Type.NOMINAL)
								d = new NominalPlotData(fCluster, fProperty, fCompounds);
							else if (type == Type.NUMERIC)
								d = new NumericPlotData(fCluster, fProperty, fCompounds);
							if (d != null)
							{
								cardContents.add(plotKey);
								cardPanel.add(d.getPlot(), plotKey);
							}
						}
						else
							System.out.println("plot was cached");

						if (fCluster != cluster || fProperty != property || fCompounds != compounds)
							return;
						setIgnoreRepaint(true);

						featureNameLabel.setText(fProperty.toString());
						featureSetLabel.setText(fProperty.getCompoundPropertySet().toString());
						//hack, ommits this for cdk features
						featureSetLabel.setVisible(fProperty.getCompoundPropertySet().isSizeDynamic());
						featureDescriptionLabel.setText(fProperty.getDescription() + "");
						featureDescriptionLabel.setVisible(fProperty.getDescription() != null);
						featureDescriptionLabelHeader.setVisible(fProperty.getDescription() != null);
						featureSmartsLabel.setText(fProperty.getSmarts() + "");
						featureSmartsLabel.setVisible(fProperty.getSmarts() != null);
						featureSmartsLabelHeader.setVisible(fProperty.getSmarts() != null);
						featureMappingLabel
								.setText((fProperty.getCompoundPropertySet().isUsedForMapping() ? "Used for clustering and/or embedding."
										: "NOT used for clustering and/or embedding."));
						featureMissingLabel.setText(clustering.numMissingValues(fProperty) + "");

						if (cardContents.contains(plotKey))
						{
							cardPanel.setVisible(true);
							((CardLayout) cardPanel.getLayout()).show(cardPanel, plotKey);
						}
						else
							cardPanel.setVisible(false);
						revalidate();
						setVisible(true);
						setIgnoreRepaint(false);
						repaint();
					}
				}, "update chart");
			}
		}
	}

	public Dimension getPreferredSize()
	{
		Dimension dim = super.getPreferredSize();
		dim.width = Math.min(guiControler.getViewerWidth() / 3, dim.width);
		dim.height = (int) (dim.width * 0.55);
		return dim;
	}
}
