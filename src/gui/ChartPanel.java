package gui;

import freechart.AbstractFreeChartPanel;
import freechart.FreeChartPanel.ChartMouseSelectionListener;
import freechart.HistogramPanel;
import freechart.StackedBarPlot;
import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.CompoundPropertyHighlighter;
import gui.util.Highlighter;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import main.ScreenSetup;
import main.Settings;
import util.ArrayUtil;
import util.ColorUtil;
import util.CountedSet;
import util.DefaultComparator;
import util.ObjectUtil;
import util.SequentialWorkerThread;
import util.StringUtil;
import util.ToStringComparator;
import cluster.Cluster;
import cluster.ClusterController;
import cluster.Clustering;
import cluster.Clustering.SelectionListener;
import cluster.ClusteringImpl;
import cluster.Compound;
import cluster.CompoundFilter;
import cluster.CompoundFilterImpl;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyUtil;

public class ChartPanel extends TransparentViewPanel
{
	Clustering clustering;
	ClusterController clusterControler;
	ViewControler viewControler;
	GUIControler guiControler;

	Cluster cluster;
	Compound compounds[];
	CompoundProperty property;
	CompoundFilter filter;

	private JPanel featurePanel;
	private JLabel featureNameLabel = ComponentFactory.createViewLabel("");
	private JLabel featureSetLabel = ComponentFactory.createViewLabel("");
	private JLabel featureValuesLabel = ComponentFactory.createViewLabel("");
	private JLabel featureValuesLabelHeader = ComponentFactory.createViewLabel("Values:");
	private JLabel featureDescriptionLabel = ComponentFactory.createViewLabel("");
	private JLabel featureDescriptionLabelHeader = ComponentFactory.createViewLabel("Description:");
	private JLabel featureSmartsLabelHeader = ComponentFactory.createViewLabel("Smarts:");
	private JLabel featureSmartsLabel = ComponentFactory.createViewLabel("");
	private JLabel featureMappingLabel = ComponentFactory.createViewLabel("");
	private JLabel featureMissingLabel = ComponentFactory.createViewLabel("");

	private JButton clearSelectedFeatureButton;

	HashMap<String, AbstractFreeChartPanel> cardContents = new HashMap<String, AbstractFreeChartPanel>();
	JPanel cardPanel;
	AbstractFreeChartPanel currentChartPanel;

	SequentialWorkerThread workerThread = new SequentialWorkerThread();

	public ChartPanel(Clustering clustering, ViewControler viewControler, ClusterController clusterControler,
			GUIControler guiControler)
	{
		this.clusterControler = clusterControler;
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
		b.append(featureValuesLabelHeader);
		b.append(featureValuesLabel);
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

		clearSelectedFeatureButton = ComponentFactory.createCrossViewButton();
		JPanel featureRemovePanel = new JPanel(new BorderLayout());
		featureRemovePanel.setOpaque(false);
		featureRemovePanel.add(featurePanel);
		JPanel removeButtonPanel = new JPanel(new BorderLayout());
		removeButtonPanel.setOpaque(false);
		removeButtonPanel.add(clearSelectedFeatureButton, BorderLayout.NORTH);
		featureRemovePanel.add(removeButtonPanel, BorderLayout.EAST);

		add(featureRemovePanel, BorderLayout.NORTH);

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
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
				{
					update(false);
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_BACKGROUND_CHANGED))
				{
					if (currentChartPanel != null)
						currentChartPanel.setForegroundColor(ComponentFactory.FOREGROUND);
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
				{
					if (currentChartPanel != null)
						currentChartPanel.setFontSize(ScreenSetup.INSTANCE.getFontSize());
				}
			}
		});
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_MODIFIED))
				{
					update(true);
				}
				else if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_REMOVED))
				{
					cardPanel.removeAll();
					cardContents.clear();
					update(true);
				}
				else if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_ADDED))
				{
					cardPanel.removeAll();
					cardContents.clear();
					update(true);
				}
			}
		});
		clustering.addSelectionListener(new SelectionListener()
		{
			@Override
			public void compoundWatchedChanged(Compound[] c)
			{
				update(false);
			}

			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				update(false);
			}

			@Override
			public void clusterWatchedChanged(Cluster c)
			{
				update(false);
			}

			@Override
			public void clusterActiveChanged(Cluster c)
			{
				update(false);
			}
		});

		featureSmartsLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (property != null && property.getSmarts() != null)
				{
					SmartsViewDialog.show(Settings.TOP_LEVEL_FRAME, property.getSmarts(),
							Settings.TOP_LEVEL_FRAME.getWidth(), Settings.TOP_LEVEL_FRAME.getHeight());
				}
			}
		});

		clearSelectedFeatureButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
			}
		});
	}

	private static String getKey(Cluster c, CompoundProperty p, Compound m[], CompoundFilter f)
	{
		String mString = "";
		for (Compound compound : m)
			mString = mString.concat(compound.toString());
		return (c == null ? "null" : c.getName()) + "_" + p.toString() + "_" + mString.hashCode() + "_"
				+ (f == null ? "" : f.hashCode());
	}

	static boolean selfUpdate = false;
	HashSet<Compound> selfUpdateCompounds = null;

	private abstract class CompoundSelector implements ChartMouseSelectionListener
	{
		protected abstract boolean hasSelectionCriterionChanged();

		protected abstract void updateSelectionCriterion();

		protected abstract String selectionCriterionToString(CompoundProperty p);

		protected abstract boolean isSelected(Compound m, CompoundProperty p);

		@Override
		public void hoverEvent()
		{
			handleEvent(true, false, false);
		}

		@Override
		public void clickEvent(boolean ctrlDown, boolean doubleClick)
		{
			handleEvent(false, ctrlDown, doubleClick);
		}

		private void handleEvent(final boolean hover, final boolean ctrlDown, final boolean doubleClick)
		{
			if (selfUpdate)
			{
				return;
			}
			selfUpdate = true;
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					clusterControler.clearClusterWatched();
					try
					{
						if (!hasSelectionCriterionChanged() && hover)
							return;
						updateSelectionCriterion();

						Highlighter h = viewControler.getHighlighter();
						CompoundProperty prop = null;
						if (h instanceof CompoundPropertyHighlighter)
							prop = ((CompoundPropertyHighlighter) h).getProperty();

						final HashSet<Compound> comps = new HashSet<Compound>();
						Iterable<Compound> compounds;
						if (clustering.isClusterActive())
							compounds = clustering.getActiveCluster().getCompounds();
						else
							compounds = clustering.getCompounds(false);
						for (Compound compound : compounds)
							if (isSelected(compound, prop))
								comps.add(compound);

						if (hover)
						{
							if (ObjectUtil.equals(selfUpdateCompounds, comps))
								return;
							selfUpdateCompounds = comps;

							if (comps.size() == 0)
								clusterControler.clearCompoundWatched();
							else
								clusterControler.setCompoundWatched(ArrayUtil.toArray(comps));
						}
						else
						{
							if (comps.size() == 0)
								clusterControler.clearCompoundActive(true);
							else
							{
								if (ctrlDown)
								{
									for (Compound compound : clustering.getActiveCompounds())
										if (comps.contains(compound))
											comps.remove(compound);
										else
											comps.add(compound);
								}

								if (doubleClick)
								{
									CompoundFilter compoundFilter = new CompoundFilterImpl(
											selectionCriterionToString(prop), new ArrayList<Compound>(comps), true);
									clusterControler.setCompoundFilter(compoundFilter, true);
								}
								else
								{
									if (comps.size() == 0)
										clusterControler.clearCompoundActive(true);
									else
										clusterControler.setCompoundActive(ArrayUtil.toArray(comps), true);
								}
							}
						}
					}
					finally
					{
						workerThread.addJob(new Runnable()
						{
							@Override
							public void run()
							{
								selfUpdate = false;
							}
						}, "selfUpdate false after chart was created!");
					}
				}
			});
			th.start();
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
		public String toString()
		{
			return "selector " + hist.hashCode();
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
		protected String selectionCriterionToString(CompoundProperty p)
		{
			return StringUtil.formatDouble(selectedMin) + " <= " + p + " <= " + StringUtil.formatDouble(selectedMax);
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

		public NumericPlotData(Cluster c, CompoundProperty p, Compound m[])
		{
			Double v[] = clustering.getDoubleValues(p);
			captions = new ArrayList<String>();
			vals = new ArrayList<double[]>();
			captions.add("Dataset");
			vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(v)));
			if (c != null)
			{
				captions.add(c.toString());
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(c.getDoubleValues(p))));
			}

			Double mVals[] = new Double[m.length];
			boolean notNull = false;
			for (int i = 0; i < mVals.length; i++)
			{
				mVals[i] = m[i].getDoubleValue(p);
				notNull |= mVals[i] != null;
			}
			if (m.length > 0 && notNull)
			{
				if (m.length == 1)
					captions.add(m[0].toString());
				else
					captions.add("Selected compounds");
				vals.add(ArrayUtil.toPrimitiveDoubleArray(ArrayUtil.removeNullValues(mVals)));
			}

			plot = new HistogramPanel(null, null, null, "#compounds", captions, vals, 20);
			configurePlotColors(plot, c, m, p);
			SwingUtilities.invokeLater(new Runnable()// wait to add listerner, so that chart has finished painting 
					{
						@Override
						public void run()
						{
							plot.addSelectionListener(new NumericCompoundSelector((HistogramPanel) plot));
						}
					});
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
		public String toString()
		{
			return "selector " + bar.hashCode();
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
		protected String selectionCriterionToString(CompoundProperty p)
		{
			return p + " = " + selectedCategory;
		}

		@Override
		protected boolean isSelected(Compound m, CompoundProperty p)
		{
			return m.getStringValue(p) == null && selectedCategory == null
					|| m.getFormattedValue(p).equals(selectedCategory);
		}
	}

	private class NominalPlotData extends PlotData
	{
		LinkedHashMap<String, List<Double>> data;
		String vals[];

		public NominalPlotData(Cluster c, CompoundProperty p, Compound ms[])
		{
			Compound m = ms.length > 0 ? ms[0] : null;

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
				data.put(c.toString(), clusterCounts);
			}
			List<Double> datasetCounts = new ArrayList<Double>();
			for (String o : datasetValues)
				datasetCounts.add((double) datasetSet.getCount(o));
			data.put("Dataset", datasetCounts);

			vals = new String[datasetValues.size()];
			//			datasetValues.toArray(vals);
			for (int i = 0; i < vals.length; i++)
				vals[i] = p.getFormattedValue(datasetValues.get(i));

			plot = new StackedBarPlot(null, null, "#compounds", StackedBarPlot.convertTotalToAdditive(data), vals);
			configurePlotColors(plot, c, ms, p);
			SwingUtilities.invokeLater(new Runnable() // wait to add listerner, so that chart has finished painting 
					{
						@Override
						public void run()
						{
							plot.addSelectionListener(new NominalCompoundSelector((StackedBarPlot) plot));
						}
					});

		}
	}

	private void configurePlotColors(AbstractFreeChartPanel chartPanel, Cluster cluster, Compound[] compounds,
			CompoundProperty property)
	{
		int dIndex = -1;
		int cIndex = -1;
		int mIndex = -1;

		if (cluster == null)
		{
			if (compounds.length == 0)
				dIndex = 0;
			else
			{
				mIndex = 0;
				dIndex = 1;
			}
		}
		else
		{
			if (compounds.length == 0)
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
				chartPanel.setSeriesColor(dIndex, ColorUtil.grayscale(cols[0]));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(dIndex, cols);
			}
			else
			{
				chartPanel.setSeriesColor(dIndex, ColorUtil.grayscale(cols[0].darker().darker().darker()));
				chartPanel.setSeriesColor(cIndex, ColorUtil.grayscale(cols[0]).brighter());

				((StackedBarPlot) chartPanel).setSeriesCategoryColors(dIndex,
						ColorUtil.darker(ColorUtil.darker(ColorUtil.darker(cols))));
				((StackedBarPlot) chartPanel).setSeriesCategoryColors(cIndex, ColorUtil.brighter(cols));
			}
		}
		else
		{
			if (cIndex == -1)
				chartPanel.setSeriesColor(dIndex, CompoundPropertyUtil.getNumericChartColor());
			else
			{
				chartPanel.setSeriesColor(dIndex, CompoundPropertyUtil.getNumericChartColor().darker().darker()
						.darker());
				chartPanel.setSeriesColor(cIndex, CompoundPropertyUtil.getNumericChartColor().brighter());
			}

			if (mIndex != -1)
				chartPanel.setSeriesColor(mIndex, CompoundPropertyUtil.getNumericChartHighlightColor());
		}

		chartPanel.setOpaqueFalse();
		chartPanel.setForegroundColor(ComponentFactory.FOREGROUND);
		chartPanel.setShadowVisible(false);
		chartPanel.setIntegerTickUnits();
		chartPanel.setBarWidthLimited();
		chartPanel.setFontSize(ScreenSetup.INSTANCE.getFontSize());
	}

	private void update(final boolean force)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		if (clustering.getNumClusters() == 0)
		{
			setVisible(false);
			return;
		}

		Highlighter h = viewControler.getHighlighter();
		CompoundProperty prop = null;
		if (h instanceof CompoundPropertyHighlighter)
			prop = ((CompoundPropertyHighlighter) h).getProperty();

		Cluster c = clustering.getActiveCluster();
		if (c == null)
			c = clustering.getWatchedCluster();
		if (clustering.getNumClusters() == 1)
			c = null;

		Compound comps[] = clustering.getActiveCompounds();
		if (comps.length == 0)
			comps = clustering.getWatchedCompounds();

		if (prop != null && prop.getType() == Type.NOMINAL)
		{
			//does NOT help much in terms of visualisation (color code should be enough), difficult to realize in terms of color brightness
			comps = new Compound[0];
		}

		if (force || cluster != c || property != prop || !Arrays.equals(compounds, comps)
				|| filter != clusterControler.getCompoundFilter())
		{
			cluster = c;
			property = prop;
			compounds = comps;
			filter = clusterControler.getCompoundFilter();

			if (property == null)
				setVisible(false);
			else
			{
				final Cluster fCluster = this.cluster;
				final CompoundProperty fProperty = this.property;
				final Compound fCompounds[] = this.compounds;
				final CompoundFilter fFilter = filter;

				workerThread.addJob(new Runnable()
				{
					public void run()
					{
						if (fCluster != cluster || fProperty != property || fCompounds != compounds)
							return;

						String plotKey = getKey(fCluster, fProperty, fCompounds, fFilter);
						if (force && cardContents.containsKey(plotKey))
							cardContents.remove(plotKey);
						if (!cardContents.containsKey(plotKey))
						{
							CompoundProperty.Type type = fProperty.getType();
							PlotData d = null;
							if (type == Type.NOMINAL)
								d = new NominalPlotData(fCluster, fProperty, fCompounds);
							else if (type == Type.NUMERIC)
								d = new NumericPlotData(fCluster, fProperty, fCompounds);
							if (d != null)
							{
								cardContents.put(plotKey, d.getPlot());
								cardPanel.add(d.getPlot(), plotKey);
							}
						}

						if (fCluster != cluster || fProperty != property || fCompounds != compounds)
							return;
						setIgnoreRepaint(true);

						featureNameLabel.setText(fProperty.toString());
						featureSetLabel.setText(fProperty.getCompoundPropertySet().toString());
						//hack, ommits this for cdk features
						featureSetLabel.setVisible(fProperty.getCompoundPropertySet().isSizeDynamic());
						featureValuesLabel.setText("<html>" + clustering.getSummaryStringValue(fProperty, true)
								+ "</html>");
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

						if (cardContents.containsKey(plotKey))
						{
							cardPanel.setVisible(true);
							currentChartPanel = cardContents.get(plotKey);
							currentChartPanel.setFontSize(ScreenSetup.INSTANCE.getFontSize());
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
		dim.width = Math.min(dim.width, guiControler.getComponentMaxWidth(0.33));
		dim.height = Math.min(dim.height, Math.min(guiControler.getComponentMaxHeight(0.33), dim.width));
		return dim;
	}
}
