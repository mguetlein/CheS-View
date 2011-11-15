package gui;

import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.MoleculePropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import cluster.Cluster;
import cluster.Clustering;
import cluster.Model;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class InfoPanel extends TransparentViewPanel
{
	Clustering clustering;
	ViewControler viewControler;

	JPanel datasetPanel;
	JTextField datasetNameLabel = ComponentFactory.createUneditableViewTextField();
	JLabel datasetSizeLabel = ComponentFactory.createViewLabel();
	JLabel datasetAlgLabel = ComponentFactory.createViewLabel();
	JLabel datasetEmbedLabel = ComponentFactory.createViewLabel();

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

	JTable compoundFeatureTable = ComponentFactory.createTable();
	DefaultTableModel compoundFeatureTableModel;
	JScrollPane compoundFeatureTableScroll;

	public InfoPanel(ViewControler viewControler, Clustering clustering)
	{
		this.viewControler = viewControler;
		this.clustering = clustering;

		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateDataset();
			}
		});

		buildLayout();
		clustering.getModelWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateCompound();
			}
		});
		clustering.getModelActive().addListener(new PropertyChangeListener()
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
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_NEW_HIGHLIGHTERS))
				{
					updateCluster();
					updateCompound();
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

		compoundPanel = new JPanel(new BorderLayout(2, 2))
		{
			public Dimension getPreferredSize()
			{
				Dimension dim = super.getPreferredSize();
				dim.width = Math.max(compoundPanelMinWidth, compoundNameLabel.getPreferredSize().width);
				dim.height = Math.min(dim.width, 180);
				return dim;
			}
		};
		compoundPanel.add(compoundNameLabel, BorderLayout.NORTH);
		compoundFeatureTableScroll = ComponentFactory.createViewScrollpane(compoundFeatureTable);
		compoundPanel.add(compoundFeatureTableScroll);
		compoundFeatureTableModel = (DefaultTableModel) compoundFeatureTable.getModel();
		compoundFeatureTableModel.addColumn("Feature");
		compoundFeatureTableModel.addColumn("Value");

		compoundPanel.setOpaque(false);
		compoundPanel.setVisible(false);

		JPanel p = new JPanel(new BorderLayout(10, 10));
		p.setOpaque(false);
		p.add(datasetPanel, BorderLayout.NORTH);
		p.add(clusterPanel, BorderLayout.SOUTH);

		setLayout(new BorderLayout(10, 10));
		add(p, BorderLayout.NORTH);
		add(compoundPanel, BorderLayout.SOUTH);

		setOpaque(true);
	}

	private void updateCompound()
	{
		int index = InfoPanel.this.clustering.getModelWatched().getSelected();
		if (index == -1)
			index = InfoPanel.this.clustering.getModelActive().getSelected();

		if (index == -1)
			compoundPanel.setVisible(false);
		else
		{
			compoundPanel.setIgnoreRepaint(true);

			Model m = clustering.getModelWithModelIndex(index);
			compoundNameLabel.setText("<html><b>Compound:</b> " + m.toString() + "<html>");
			//			compoundSmilesLabel.setText(m.getSmiles());

			while (compoundFeatureTableModel.getRowCount() > 0)
				compoundFeatureTableModel.removeRow(0);
			compoundFeatureTableModel.addRow(new String[] { "Smiles:", m.getSmiles() });
			for (MoleculeProperty p : clustering.getFeatures())
			{
				Object o[] = new Object[2];
				o[0] = p.getName() + ":";
				o[1] = m.getTemperature(p);
				compoundFeatureTableModel.addRow(o);
			}
			for (MoleculeProperty p : clustering.getProperties())
			{
				Object o[] = new Object[2];
				o[0] = p.getName() + ":";
				o[1] = m.getTemperature(p);
				compoundFeatureTableModel.addRow(o);
			}
			compoundPanelMinWidth = ComponentFactory.packColumn(compoundFeatureTable, 0, 2, 250);
			//			System.err.println(compoundPanelMinWidth);
			compoundPanelMinWidth += ComponentFactory.packColumn(compoundFeatureTable, 1, 2);
			//			System.err.println(compoundPanelMinWidth);

			if (viewControler.getHighlighter() instanceof MoleculePropertyHighlighter)
			{
				MoleculeProperty p = ((MoleculePropertyHighlighter) viewControler.getHighlighter()).getProperty();
				int pIndex = 1 + clustering.getFeatures().indexOf(p);
				if (pIndex == 0)
					pIndex = 1 + clustering.getFeatures().size() + clustering.getProperties().indexOf(p);
				compoundFeatureTable.setRowSelectionInterval(pIndex, pIndex);
				compoundFeatureTable.scrollRectToVisible(new Rectangle(compoundFeatureTable
						.getCellRect(pIndex, 0, true)));
			}

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
			datasetPanel.setVisible(true);
			datasetNameLabel.setText(clustering.getName());
			//			datasetNameLabel.setCaretPosition(datasetNameLabel.getText().length());
			datasetSizeLabel.setText(clustering.getNumCompounds() + "");
			datasetAlgLabel.setText(clustering.getClusterAlgorithm());
			datasetEmbedLabel.setText(clustering.getEmbedAlgorithm());
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
			clusterPanel.setIgnoreRepaint(true);
			Cluster c = clustering.getCluster(index);
			clusterNameLabel.setText(c.getName());
			clusterSizeLabel.setText(c.size() + "");
			clusterAlignLabel.setText(c.getAlignAlgorithm());

			boolean smartsFound = true;
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

			if (viewControler.getHighlighter() instanceof MoleculePropertyHighlighter)
			{
				MoleculeProperty p = ((MoleculePropertyHighlighter) viewControler.getHighlighter()).getProperty();
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
			clusterPanel.setIgnoreRepaint(false);
			clusterPanel.setVisible(true);
		}
	}

	public Dimension getPreferredSize()
	{
		Dimension dim = super.getPreferredSize();
		dim.width = Math.min(400, dim.width);
		return dim;
	}
}
