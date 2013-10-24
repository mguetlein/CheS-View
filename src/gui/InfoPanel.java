package gui;

import gui.ViewControler.FeatureFilter;
import gui.swing.ComponentFactory;
import gui.util.CompoundPropertyHighlighter;
import gui.util.Highlighter;
import gui.util.SubstructureHighlighter;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import util.ArrayUtil;
import util.ListUtil;
import cluster.Cluster;
import cluster.Clustering;
import cluster.Compound;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertyUtil;
import dataInterface.SubstructureSmartsType;

public class InfoPanel extends JPanel
{
	Clustering clustering;
	ViewControler viewControler;
	GUIControler guiControler;

	JPanel datasetPanel;
	JTextField datasetNameLabel = ComponentFactory.createUneditableViewTextField();
	JLabel datasetSizeLabel = ComponentFactory.createViewLabel();
	JLabel datasetAlgLabel = ComponentFactory.createViewLabel();
	JLabel datasetEmbedLabel = ComponentFactory.createViewLabel();
	JLabel datasetEmbedQualityLabel = ComponentFactory.createViewLabel();

	JPanel clusterCompoundPanel;
	CardLayout clusterCompoundLayout;
	final static String CARD_CLUSTER = "cluster";
	final static String CARD_COMPOUND = "compound";

	int clusterPanelMinWidth = -1;

	JTable clusterFeatureTable_fix = ComponentFactory.createTable(true);
	DefaultTableModel clusterFeatureTableModel_fix;

	JTable clusterFeatureTable_interact = ComponentFactory.createTable(true);
	DefaultTableModel clusterFeatureTableModel_interact;
	JScrollPane clusterFeatureTableScroll_interact;

	JPanel clusterTableCardPanel;
	CardLayout clusterTableCardLayout;

	int compoundPanelMinWidth = -1;

	JTable compoundFeatureTable_fix = ComponentFactory.createTable(true);
	DefaultTableModel compoundFeatureTableModel_fix;

	JTable compoundFeatureTable_interact = ComponentFactory.createTable(true);
	DefaultTableModel compoundFeatureTableModel_interact;
	JScrollPane compoundFeatureTableScroll_interact;

	JPanel compoundTableCardPanel;
	CardLayout compoundTableCardLayout;

	final static String CARD_FIX = "fix-card";
	final static String CARD_INTERACT = "interact-card";

	JLabel compoundIconLabel = new JLabel("");

	boolean interactive = false;
	Compound selectedCompound = null;
	Cluster selectedCluster = null;

	int clusterSubstructureRow = -1;
	SubstructureSmartsType clusterSubstructureType;

	boolean selfUpdate = true;

	public InfoPanel(ViewControler viewControler, Clustering clustering, GUIControler guiControler)
	{
		this.viewControler = viewControler;
		this.clustering = clustering;
		this.guiControler = guiControler;

		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateDataset();
				updateCluster();
			}
		});

		buildLayout();
		clustering.getCompoundWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateCompound();
			}
		});
		clustering.getCompoundActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateCompound();
			}
		});
		clustering.getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateCluster();
			}
		});
		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateCluster();
			}
		});
		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_NEW_HIGHLIGHTERS)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_BACKGROUND_CHANGED))
				{
					updateCluster();
					updateCompound();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
				{
					updateDatasetPanelSize();
					if (selectedCompound != null)
					{
						compoundPanelMinWidth = 0;
						updateCompound();
					}
					else if (selectedCluster != null)
					{
						clusterPanelMinWidth = 0;
						updateCluster();
					}
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_FILTER_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_SORTING_CHANGED))
				{
					if (selectedCompound != null)
						updateCompound();
					else if (selectedCluster != null)
						updateCluster();
				}
			}
		});

		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_VIEWER_SIZE_CHANGED))
				{
					//updateDatasetAndClusterPanelSize();
					updateDatasetPanelSize();
					//					updateClusterPanelSize();
				}
			}
		});
	}

	private void buildLayout()
	{
		DefaultFormBuilder b1 = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		b1.setLineGapSize(Sizes.pixel(2));
		b1.append(ComponentFactory.createViewLabel("<html><b>Dataset:</b><html>"));
		b1.append(datasetNameLabel);
		b1.nextLine();
		b1.append(ComponentFactory.createViewLabel("<html>Num compounds:<html>"));
		b1.append(datasetSizeLabel);
		b1.nextLine();
		b1.append(ComponentFactory.createViewLabel("<html>Cluster algorithm:<html>"));
		b1.append(datasetAlgLabel);
		b1.nextLine();
		b1.append(ComponentFactory.createViewLabel("<html>3D Embedding:<html>"));
		b1.append(datasetEmbedLabel);
		b1.append(ComponentFactory.createViewLabel("<html>3D Embedding Quality:<html>"));
		b1.append(datasetEmbedQualityLabel);

		datasetPanel = b1.getPanel();
		datasetPanel.setOpaque(false);
		datasetPanel.setVisible(true);

		clusterTableCardLayout = new CardLayout();
		clusterTableCardPanel = new JPanel(clusterTableCardLayout);
		clusterTableCardPanel.setOpaque(false);

		clusterFeatureTableScroll_interact = ComponentFactory.createViewScrollpane(clusterFeatureTable_interact);
		clusterFeatureTableModel_interact = (DefaultTableModel) clusterFeatureTable_interact.getModel();
		clusterFeatureTableModel_interact.addColumn("Feature");
		clusterFeatureTableModel_interact.addColumn("Value");
		clusterTableCardPanel.add(clusterFeatureTableScroll_interact, CARD_INTERACT);
		clusterFeatureTable_interact.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (selfUpdate)
					return;
				int r = clusterFeatureTable_interact.getSelectedRow();
				if (r != -1 && clusterFeatureTable_interact.getValueAt(r, 0) instanceof CompoundProperty)
					viewControler.setHighlighter((CompoundProperty) clusterFeatureTable_interact.getValueAt(r, 0));
				else if (r != -1 && r == clusterSubstructureRow)
					viewControler.setHighlighter(clusterSubstructureType);
				else
					viewControler.setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
			}
		});

		clusterFeatureTableModel_fix = (DefaultTableModel) clusterFeatureTable_fix.getModel();
		clusterFeatureTableModel_fix.addColumn("Feature");
		clusterFeatureTableModel_fix.addColumn("Value");
		clusterTableCardPanel.add(clusterFeatureTable_fix, CARD_FIX);

		compoundTableCardLayout = new CardLayout();
		compoundTableCardPanel = new JPanel(compoundTableCardLayout);
		compoundTableCardPanel.setOpaque(false);

		compoundFeatureTableScroll_interact = ComponentFactory.createViewScrollpane(compoundFeatureTable_interact);
		compoundFeatureTableModel_interact = (DefaultTableModel) compoundFeatureTable_interact.getModel();
		compoundFeatureTableModel_interact.addColumn("Feature");
		compoundFeatureTableModel_interact.addColumn("Value");
		compoundTableCardPanel.add(compoundFeatureTableScroll_interact, CARD_INTERACT);
		compoundFeatureTable_interact.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{

			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (selfUpdate)
					return;
				int r = compoundFeatureTable_interact.getSelectedRow();
				if (r != -1 && compoundFeatureTable_interact.getValueAt(r, 0) instanceof CompoundProperty)
					viewControler.setHighlighter((CompoundProperty) compoundFeatureTable_interact.getValueAt(r, 0));
				else
					viewControler.setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
			}
		});

		compoundFeatureTableModel_fix = (DefaultTableModel) compoundFeatureTable_fix.getModel();
		compoundFeatureTableModel_fix.addColumn("Feature");
		compoundFeatureTableModel_fix.addColumn("Value");
		compoundTableCardPanel.add(compoundFeatureTable_fix, CARD_FIX);

		JPanel compoundIconPanel = new JPanel(new BorderLayout());
		compoundIconPanel.setOpaque(false);
		compoundIconPanel.add(compoundIconLabel, BorderLayout.NORTH);

		clusterCompoundLayout = new CardLayout();
		clusterCompoundPanel = new JPanel(clusterCompoundLayout)
		{
			@Override
			public Dimension getPreferredSize()
			{
				if (selectedCompound == null && selectedCluster == null)
					return new Dimension(0, 0);
				Dimension dim = super.getPreferredSize();
				int panelWidth;
				if (selectedCompound != null)
					panelWidth = compoundPanelMinWidth; //Math.max(compoundPanelMinWidth, compoundNameLabel.getPreferredSize().width);
				else
					panelWidth = clusterPanelMinWidth; //Math.max(clusterPanelMinWidth, clusterNameLabel.getPreferredSize().width);
				dim.width = Math.min(dim.width,
						Math.min(guiControler.getComponentMaxWidth(interactive ? 1 / 4.0 : 1 / 6.0), panelWidth));
				//				dim.height = Math.min(dim.width, 180);
				return dim;
			}
		};
		clusterCompoundPanel.setOpaque(false);
		clusterCompoundPanel.setVisible(false);

		clusterCompoundPanel.add(clusterTableCardPanel, CARD_CLUSTER);
		clusterCompoundPanel.add(compoundTableCardPanel, CARD_COMPOUND);

		JPanel iconAndClusterCompoundPanel = new JPanel(new BorderLayout(5, 0));
		iconAndClusterCompoundPanel.add(compoundIconPanel, BorderLayout.WEST);
		iconAndClusterCompoundPanel.add(clusterCompoundPanel, BorderLayout.EAST);
		iconAndClusterCompoundPanel.setOpaque(false);

		setLayout(new BorderLayout(0, 20));
		JPanel datasetPanelContainer = new JPanel(new BorderLayout());
		datasetPanelContainer.setOpaque(false);
		datasetPanelContainer.add(datasetPanel, BorderLayout.EAST);
		add(datasetPanelContainer, BorderLayout.NORTH);
		add(iconAndClusterCompoundPanel, BorderLayout.EAST);

		DefaultTableCellRenderer renderer = new ComponentFactory.FactoryTableCellRenderer(true)
		{
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (row == 0 && column == 0)
					c.setFont(c.getFont().deriveFont(Font.BOLD));
				if (column == 1)
				{
					if (table.getValueAt(row, 0) instanceof CompoundProperty)
					{
						c.setFont(c.getFont().deriveFont(Font.ITALIC));

						if (isSelected == false)
						{
							Color col = MainPanel.getHighlightColor(clustering,
									selectedCompound != null ? selectedCompound : selectedCluster,
									(CompoundProperty) table.getValueAt(row, 0));
							if (col != null && col != CompoundPropertyUtil.getNullValueColor())
								setForeground(col);
						}
					}
				}
				return c;
			};
		};
		compoundFeatureTable_fix.setDefaultRenderer(Object.class, renderer);
		compoundFeatureTable_interact.setDefaultRenderer(Object.class, renderer);
		clusterFeatureTable_fix.setDefaultRenderer(Object.class, renderer);
		clusterFeatureTable_interact.setDefaultRenderer(Object.class, renderer);

		setOpaque(false);
	}

	@SuppressWarnings("unchecked")
	private List<CompoundProperty> getPropList()
	{
		List<CompoundProperty> props = null;
		if (viewControler.getFeatureFilter() == FeatureFilter.UsedByEmbedding)
			props = new ArrayList<CompoundProperty>(clustering.getFeatures());
		else
		{
			props = new ArrayList<CompoundProperty>();
			for (CompoundProperty p : clustering.getProperties())
				if (!p.isSmiles())
					props.add(p);
			// if (viewControler.getFeatureFilter() == FeatureFilter.NotUsedByEmbedding) do nothing
			if (viewControler.getFeatureFilter() == FeatureFilter.None)
				props = ListUtil.concat(props, clustering.getFeatures());
		}
		return props;
	}

	@SuppressWarnings("unchecked")
	private synchronized void updateCompound()
	{
		selfUpdate = true;

		int index = -1;
		if (clustering.isCompoundActive() && clustering.isCompoundWatched())
		{
			interactive = true;
			int active[] = InfoPanel.this.clustering.getCompoundActive().getSelectedIndices();
			int watched[] = InfoPanel.this.clustering.getCompoundWatched().getSelectedIndices();
			Integer activeAndWatched[] = ArrayUtil.cut(Integer.class, ArrayUtil.toIntegerArray(active),
					ArrayUtil.toIntegerArray(watched));
			if (activeAndWatched.length > 0)
				index = activeAndWatched[0];
			else
				index = active[0];
		}
		else if (clustering.isCompoundActive())
		{
			interactive = true;
			index = clustering.getCompoundActive().getSelected();

		}
		else if (clustering.isCompoundWatched())
		{
			interactive = false;
			index = clustering.getCompoundWatched().getSelected();
		}

		if (index == -1)
		{
			selectedCompound = null;
			compoundIconLabel.setVisible(false);
			compoundPanelMinWidth = 0;

			if (selectedCluster != null)
				updateCluster();
			else
				clusterCompoundPanel.setVisible(false);

		}
		else
		{
			selectedCompound = clustering.getCompoundWithCompoundIndex(index);
			clusterCompoundPanel.setIgnoreRepaint(true);
			clusterCompoundLayout.show(clusterCompoundPanel, CARD_COMPOUND);

			CompoundProperty selectedP = null;
			if (viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)
				selectedP = ((CompoundPropertyHighlighter) viewControler.getHighlighter()).getProperty();

			compoundTableCardLayout.show(compoundTableCardPanel, interactive ? CARD_INTERACT : CARD_FIX);
			DefaultTableModel model = (interactive ? compoundFeatureTableModel_interact : compoundFeatureTableModel_fix);
			JTable table = (interactive ? compoundFeatureTable_interact : compoundFeatureTable_fix);

			while (model.getRowCount() > 0)
				model.removeRow(0);
			model.addRow(new String[] { "Compound", selectedCompound.toString() });
			model.addRow(new String[] { "Smiles", selectedCompound.getSmiles() });
			//model.addRow(new String[] { "Accented by:", clustering.getAccent(m) });

			List<CompoundProperty> props = getPropList();
			if (viewControler.isFeatureSortingEnabled())
				Collections.sort(props, new Comparator<CompoundProperty>()
				{
					@Override
					public int compare(CompoundProperty o1, CompoundProperty o2)
					{
						return Double.compare(clustering.getSpecificity(selectedCompound, o1),
								clustering.getSpecificity(selectedCompound, o2));
					}
				});
			int rowOffset = model.getRowCount();
			for (CompoundProperty p : props)
			{
				Object o[] = new Object[2];
				o[0] = p;
				o[1] = selectedCompound.getFormattedValue(p);
				model.addRow(o);
			}

			int width = ComponentFactory.packColumn(table, 0, 2,
					guiControler.getComponentMaxWidth(interactive ? 1 / 6.0 : 1 / 12.0));
			width += ComponentFactory.packColumn(table, 1, 2,
					guiControler.getComponentMaxWidth(interactive ? 1 : 1 / 12.0));
			compoundPanelMinWidth = Math.max(compoundPanelMinWidth, width);

			if (selectedP != null && props.indexOf(selectedP) != -1)
			{
				int pIndex = rowOffset + props.indexOf(selectedP);
				table.setRowSelectionInterval(pIndex, pIndex);
				table.scrollRectToVisible(new Rectangle(table.getCellRect(pIndex, 0, true)));
			}
			else
				table.scrollRectToVisible(new Rectangle(table.getCellRect(0, 0, true)));

			final Compound fCompound = selectedCompound;
			Thread th = new Thread(new Runnable()
			{
				public void run()
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							ImageIcon icon = null;
							if (selectedCompound == fCompound)
								icon = fCompound.getIcon(viewControler.isBlackgroundBlack());
							if (selectedCompound == fCompound) // after icon is loaded
							{
								compoundIconLabel.setIcon(icon);
								compoundIconLabel.setVisible(true);
							}
						}
					});
				}
			});
			th.start();
			clusterCompoundPanel.setPreferredSize(null);
			clusterCompoundPanel.setIgnoreRepaint(false);
			clusterCompoundPanel.setVisible(true);
		}

		selfUpdate = false;
	}

	private void updateDataset()
	{
		if (clustering.getNumClusters() == 0)
			datasetPanel.setVisible(false);
		else
		{
			//datasetAndClusterPanelContainer.setIgnoreRepaint(true);
			datasetPanel.setIgnoreRepaint(true);

			datasetNameLabel.setText(clustering.getName());
			datasetSizeLabel.setText(clustering.getNumCompounds(false) + "");
			datasetAlgLabel.setText(clustering.getClusterAlgorithm());
			datasetEmbedLabel.setText(clustering.getEmbedAlgorithm());
			datasetEmbedQualityLabel.setText(clustering.getEmbedQuality());

			datasetPanel.setVisible(true);
			updateDatasetPanelSize();
		}
	}

	@SuppressWarnings("unchecked")
	private synchronized void updateCluster()
	{
		if (selectedCompound != null)
			return;

		selfUpdate = true;

		int index = InfoPanel.this.clustering.getClusterActive().getSelected();
		interactive = true;
		if (index == -1)
		{
			index = InfoPanel.this.clustering.getClusterWatched().getSelected();
			interactive = false;
		}

		if (index == -1 || clustering.getNumClusters() == 1)
		{
			selectedCluster = null;
			clusterCompoundPanel.setVisible(false);
			clusterPanelMinWidth = 0;
		}
		else
		{
			selectedCluster = clustering.getCluster(index);

			clusterCompoundPanel.setIgnoreRepaint(true);
			clusterCompoundLayout.show(clusterCompoundPanel, CARD_CLUSTER);

			clusterTableCardLayout.show(clusterTableCardPanel, interactive ? CARD_INTERACT : CARD_FIX);
			DefaultTableModel model = (interactive ? clusterFeatureTableModel_interact : clusterFeatureTableModel_fix);
			JTable table = (interactive ? clusterFeatureTable_interact : clusterFeatureTable_fix);

			while (model.getRowCount() > 0)
				model.removeRow(0);
			model.addRow(new String[] { "Cluster", selectedCluster.toString() });
			model.addRow(new String[] { "Size", selectedCluster.size() + "" });
			clusterSubstructureRow = -1;
			clusterSubstructureType = null;
			for (SubstructureSmartsType t : SubstructureSmartsType.values())
			{
				if (selectedCluster.getSubstructureSmarts(t) != null)
				{
					clusterSubstructureRow = model.getRowCount();
					clusterSubstructureType = t;
					model.addRow(new String[] { t.getName(), selectedCluster.getSubstructureSmarts(t) });
					break;
				}
			}
			List<CompoundProperty> props = getPropList();
			if (viewControler.isFeatureSortingEnabled())
				Collections.sort(props, new Comparator<CompoundProperty>()
				{
					@Override
					public int compare(CompoundProperty o1, CompoundProperty o2)
					{
						return Double.compare(clustering.getSpecificity(selectedCluster, o1),
								clustering.getSpecificity(selectedCluster, o2));
					}
				});
			int pOffset = model.getRowCount();
			for (CompoundProperty p : props)
			{
				Object o[] = new Object[2];
				o[0] = p;
				o[1] = selectedCluster.getSummaryStringValue(p, false);
				model.addRow(o);
			}
			int width = ComponentFactory.packColumn(table, 0, 2,
					guiControler.getComponentMaxWidth(interactive ? 1 / 6.0 : 1 / 12.0));
			width += ComponentFactory.packColumn(table, 1, 2,
					guiControler.getComponentMaxWidth(interactive ? 1 : 1 / 12.0));
			clusterPanelMinWidth = Math.max(clusterPanelMinWidth, width);

			CompoundProperty selectedP = null;
			if ((viewControler.getHighlighter() instanceof CompoundPropertyHighlighter))
				selectedP = ((CompoundPropertyHighlighter) viewControler.getHighlighter()).getProperty();

			if (selectedP != null && props.indexOf(selectedP) != -1)
			{
				int pIndex = pOffset + props.indexOf(selectedP);
				table.setRowSelectionInterval(pIndex, pIndex);
				table.scrollRectToVisible(new Rectangle(table.getCellRect(pIndex, 0, true)));
			}
			else if (viewControler.getHighlighter() instanceof SubstructureHighlighter
					&& ((SubstructureHighlighter) viewControler.getHighlighter()).getType() == clusterSubstructureType
					&& clusterSubstructureRow != -1)
			{
				table.setRowSelectionInterval(clusterSubstructureRow, clusterSubstructureRow);
				table.scrollRectToVisible(new Rectangle(table.getCellRect(clusterSubstructureRow, 0, true)));
			}
			else
			{
				table.scrollRectToVisible(new Rectangle(table.getCellRect(0, 0, true)));
			}
			clusterCompoundPanel.setPreferredSize(null);
			clusterCompoundPanel.setIgnoreRepaint(true);
			clusterCompoundPanel.setVisible(true);
		}
		selfUpdate = false;
	}

	private void updateDatasetPanelSize()
	{
		if (datasetPanel.isVisible())
		{
			datasetPanel.setPreferredSize(null);
			Dimension d = datasetPanel.getPreferredSize();
			datasetPanel.setPreferredSize(new Dimension(Math.min(d.width, guiControler.getComponentMaxWidth(0.33)),
					d.height));
		}
		datasetPanel.setIgnoreRepaint(false);
		datasetPanel.revalidate();
	}
}
