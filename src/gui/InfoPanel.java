package gui;

import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.CompoundPropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import util.ListUtil;
import cluster.Cluster;
import cluster.Clustering;
import cluster.Compound;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import dataInterface.CompoundProperty;
import dataInterface.SubstructureSmartsType;

public class InfoPanel extends JPanel
{
	Clustering clustering;
	ViewControler viewControler;
	GUIControler guiControler;

	JPanel datasetAndClusterPanelContainer;

	JPanel datasetPanel;
	JTextField datasetNameLabel = ComponentFactory.createUneditableViewTextField();
	JLabel datasetSizeLabel = ComponentFactory.createViewLabel();
	JLabel datasetAlgLabel = ComponentFactory.createViewLabel();
	JLabel datasetEmbedLabel = ComponentFactory.createViewLabel();
	JLabel datasetEmbedQualityLabel = ComponentFactory.createViewLabel();

	JPanel clusterPanel;
	JLabel clusterNameLabel = ComponentFactory.createViewLabel();
	JLabel clusterSizeLabel = ComponentFactory.createViewLabel();
	JLabel clusterAlignLabel = ComponentFactory.createViewLabel();
	JLabel clusterMCSLabelHeader = ComponentFactory.createViewLabel();
	JLabel clusterMCSLabel = ComponentFactory.createViewLabel();
	JLabel clusterFeatureLabelHeader = ComponentFactory.createViewLabel();
	JLabel clusterFeatureLabel = ComponentFactory.createViewLabel();

	JPanel compoundPanel;
	int compoundPanelMinWidth = -1;
	JLabel compoundNameLabel = ComponentFactory.createViewLabel();

	JTable compoundFeatureTable_fixed = ComponentFactory.createTable(true);
	DefaultTableModel compoundFeatureTableModel_fixed;

	JTable compoundFeatureTable_interactive = ComponentFactory.createTable(true);
	DefaultTableModel compoundFeatureTableModel_interactive;
	JScrollPane compoundFeatureTableScroll_interactive;

	JPanel tableCardPanel;
	CardLayout tableCardLayout;
	final static String CARD_FIXED = "fixed-card";
	final static String CARD_INTERACTIVE = "interactive-card";

	JLabel compoundIconLabel = new JLabel("");

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
			}
		});

		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_VIEWER_SIZE_CHANGED))
					updateDatasetAndClusterPanelSize();
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

		DefaultFormBuilder b2 = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		b2.setLineGapSize(Sizes.pixel(2));
		b2.append(ComponentFactory.createViewLabel("<html><b>Cluster:</b><html>"));
		b2.append(clusterNameLabel);
		b2.nextLine();
		b2.append(ComponentFactory.createViewLabel("<html>Num compounds:<html>"));
		b2.append(clusterSizeLabel);
		b2.nextLine();
		b2.append(ComponentFactory.createViewLabel("<html>3D Alignement:<html>"));
		b2.append(clusterAlignLabel);
		b2.nextLine();
		b2.append(clusterMCSLabelHeader);
		b2.append(clusterMCSLabel);
		b2.nextLine();
		b2.append(clusterFeatureLabelHeader);
		b2.append(clusterFeatureLabel);

		clusterPanel = b2.getPanel();
		clusterPanel.setOpaque(false);
		clusterPanel.setVisible(false);

		JPanel tablePanel = new JPanel(new BorderLayout(0, 2))
		{
			public Dimension getPreferredSize()
			{
				Dimension dim = super.getPreferredSize();
				dim.width = Math.min(guiControler.getViewerWidth() / (interactive ? 4 : 6),
						Math.max(compoundPanelMinWidth, compoundNameLabel.getPreferredSize().width));
				//				dim.height = Math.min(dim.width, 180);
				return dim;
			}
		};
		tablePanel.setOpaque(false);

		tableCardLayout = new CardLayout();
		tableCardPanel = new JPanel(tableCardLayout);
		tableCardPanel.setOpaque(false);

		compoundFeatureTableScroll_interactive = ComponentFactory
				.createViewScrollpane(compoundFeatureTable_interactive);
		compoundFeatureTableModel_interactive = (DefaultTableModel) compoundFeatureTable_interactive.getModel();
		compoundFeatureTableModel_interactive.addColumn("Feature");
		compoundFeatureTableModel_interactive.addColumn("Value");
		tableCardPanel.add(compoundFeatureTableScroll_interactive, CARD_INTERACTIVE);

		compoundFeatureTableModel_fixed = (DefaultTableModel) compoundFeatureTable_fixed.getModel();
		compoundFeatureTableModel_fixed.addColumn("Feature");
		compoundFeatureTableModel_fixed.addColumn("Value");
		tableCardPanel.add(compoundFeatureTable_fixed, CARD_FIXED);

		tablePanel.add(compoundNameLabel, BorderLayout.NORTH);
		tablePanel.add(tableCardPanel, BorderLayout.CENTER);
		JPanel iconPanel = new JPanel(new BorderLayout());
		iconPanel.setOpaque(false);
		iconPanel.add(compoundIconLabel, BorderLayout.NORTH);

		compoundPanel = new JPanel(new BorderLayout(5, 0));
		compoundPanel.add(iconPanel, BorderLayout.WEST);
		compoundPanel.add(tablePanel, BorderLayout.EAST);
		compoundPanel.setOpaque(false);
		compoundPanel.setVisible(false);

		datasetAndClusterPanelContainer = new TransparentViewPanel(new BorderLayout(20, 0));
		datasetAndClusterPanelContainer.setOpaque(true);
		datasetAndClusterPanelContainer.add(datasetPanel, BorderLayout.EAST);
		datasetAndClusterPanelContainer.add(clusterPanel, BorderLayout.WEST);

		setLayout(new BorderLayout(0, 20));
		add(datasetAndClusterPanelContainer, BorderLayout.NORTH);
		add(compoundPanel, BorderLayout.EAST);

		setOpaque(false);
	}

	boolean interactive;

	@SuppressWarnings("unchecked")
	private void updateCompound()
	{
		int index = InfoPanel.this.clustering.getCompoundActive().getSelected();
		interactive = true;
		if (index == -1)
		{
			index = InfoPanel.this.clustering.getCompoundWatched().getSelected();
			interactive = false;
		}

		if (index == -1)
		{
			compoundPanel.setVisible(false);
			compoundPanelMinWidth = 0;
		}
		else
		{
			compoundPanel.setIgnoreRepaint(true);

			final Compound m = clustering.getCompoundWithCompoundIndex(index);
			compoundNameLabel.setText("<html><b>Compound:</b>&nbsp;" + m.toString().replace(" ", "&nbsp;") + "<html>");
			//			compoundSmilesLabel.setText(m.getSmiles());

			tableCardLayout.show(tableCardPanel, interactive ? CARD_INTERACTIVE : CARD_FIXED);
			DefaultTableModel model = (interactive ? compoundFeatureTableModel_interactive
					: compoundFeatureTableModel_fixed);
			JTable table = (interactive ? compoundFeatureTable_interactive : compoundFeatureTable_fixed);

			while (model.getRowCount() > 0)
				model.removeRow(0);
			model.addRow(new String[] { "Smiles:", m.getSmiles() });
			List<CompoundProperty> props = ListUtil.concat(clustering.getProperties(), clustering.getFeatures());
			for (CompoundProperty p : props)
			{
				Object o[] = new Object[2];
				o[0] = p.getName() + ":";
				o[1] = m.getFormattedValue(p);
				model.addRow(o);
			}

			int width = ComponentFactory.packColumn(table, 0, 2, interactive ? guiControler.getViewerWidth() / 6
					: guiControler.getViewerWidth() / 12);
			width += ComponentFactory.packColumn(table, 1, 2,
					interactive ? Integer.MAX_VALUE : guiControler.getViewerWidth() / 12);
			compoundPanelMinWidth = Math.max(compoundPanelMinWidth, width);

			if (viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)
			{
				CompoundProperty p = ((CompoundPropertyHighlighter) viewControler.getHighlighter()).getProperty();
				int pIndex = 1 + props.indexOf(p);
				table.setRowSelectionInterval(pIndex, pIndex);
				table.scrollRectToVisible(new Rectangle(table.getCellRect(pIndex, 0, true)));
			}
			else
			{
				table.scrollRectToVisible(new Rectangle(table.getCellRect(0, 0, true)));
			}

			Thread th = new Thread(new Runnable()
			{
				public void run()
				{

					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							compoundPanel.setIgnoreRepaint(false);
							compoundIconLabel.setIcon(m.getIcon(viewControler.isBlackgroundBlack()));
							compoundPanel.setIgnoreRepaint(true);
						}
					});
				}
			});
			th.start();
			compoundPanel.setIgnoreRepaint(false);
			compoundPanel.setVisible(true);
		}
	}

	private void updateDataset()
	{
		if (clustering.getNumClusters() == 0)
			datasetPanel.setVisible(false);
		else
		{
			datasetAndClusterPanelContainer.setIgnoreRepaint(true);

			datasetNameLabel.setText(clustering.getName());
			//			datasetNameLabel.setCaretPosition(datasetNameLabel.getText().length());
			datasetSizeLabel.setText(clustering.getNumCompounds(false) + "");
			datasetAlgLabel.setText(clustering.getClusterAlgorithm());
			datasetEmbedLabel.setText(clustering.getEmbedAlgorithm());
			datasetEmbedQualityLabel.setText(clustering.getEmbedQuality());

			datasetPanel.setVisible(true);
			updateDatasetAndClusterPanelSize();
		}
	}

	private void updateCluster()
	{
		int index = InfoPanel.this.clustering.getClusterActive().getSelected();
		if (index == -1)
			index = InfoPanel.this.clustering.getClusterWatched().getSelected();

		if (index == -1)
		{
			clusterPanel.setVisible(false);
			compoundPanel.setVisible(false);
		}
		else
		{
			datasetAndClusterPanelContainer.setIgnoreRepaint(true);
			Cluster c = clustering.getCluster(index);
			clusterNameLabel.setText(c.getName());
			clusterSizeLabel.setText(c.size() + "");
			clusterAlignLabel.setText(c.getAlignAlgorithm());

			boolean smartsFound = false;
			for (SubstructureSmartsType t : SubstructureSmartsType.values())
			{
				if (c.getSubstructureSmarts(t) != null)
				{
					smartsFound = true;
					clusterMCSLabelHeader.setText(t.getName() + ":");
					clusterMCSLabel.setText(c.getSubstructureSmarts(t));
					break;
				}
			}
			clusterMCSLabelHeader.setVisible(smartsFound);
			clusterMCSLabel.setVisible(smartsFound);

			if (viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)
			{
				CompoundProperty p = ((CompoundPropertyHighlighter) viewControler.getHighlighter()).getProperty();
				clusterFeatureLabelHeader.setText(p.getName() + ":");
				clusterFeatureLabel.setText(c.getSummaryStringValue(p));
				clusterFeatureLabel.setVisible(true);
				clusterFeatureLabelHeader.setVisible(true);
			}
			else
			{
				clusterFeatureLabel.setVisible(false);
				clusterFeatureLabelHeader.setVisible(false);
			}

			clusterPanel.setVisible(true);
			updateDatasetAndClusterPanelSize();
		}
	}

	private void updateDatasetAndClusterPanelSize()
	{
		if (datasetPanel.isVisible())
		{
			datasetPanel.setPreferredSize(null);
			Dimension d = datasetPanel.getPreferredSize();
			datasetPanel
					.setPreferredSize(new Dimension(Math.min(d.width, guiControler.getViewerWidth() / 5), d.height));
		}
		if (clusterPanel.isVisible())
		{
			clusterFeatureLabelHeader.setPreferredSize(null);
			Dimension d = clusterFeatureLabelHeader.getPreferredSize();
			clusterFeatureLabelHeader.setPreferredSize(new Dimension(Math.min(d.width,
					guiControler.getViewerWidth() / 12), d.height));

			clusterPanel.setPreferredSize(null);
			d = clusterPanel.getPreferredSize();
			clusterPanel
					.setPreferredSize(new Dimension(Math.min(d.width, guiControler.getViewerWidth() / 6), d.height));
		}

		datasetAndClusterPanelContainer.setIgnoreRepaint(false);
		datasetAndClusterPanelContainer.revalidate();
	}
}
