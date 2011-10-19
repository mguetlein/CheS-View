package gui;

import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.MoleculePropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import cluster.Cluster;
import cluster.Clustering;
import cluster.Model;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class InfoPanel extends TransparentViewPanel
{
	Clustering clustering;
	ViewControler viewControler;

	JPanel datasetPanel;
	JLabel datasetNameLabel = ComponentFactory.createViewLabel();
	JLabel datasetSizeLabel = ComponentFactory.createViewLabel();
	JLabel datasetAlgLabel = ComponentFactory.createViewLabel();
	JLabel datasetEmbedLabel = ComponentFactory.createViewLabel();

	JPanel clusterPanel;
	JLabel clusterNameLabel = ComponentFactory.createViewLabel();
	JLabel clusterSizeLabel = ComponentFactory.createViewLabel();
	JLabel clusterAlignLabel = ComponentFactory.createViewLabel();
	JLabel clusterMCSLabelHeader = ComponentFactory.createViewLabel("MCS:");
	JLabel clusterMCSLabel = ComponentFactory.createViewLabel();
	JLabel clusterFeatureLabelHeader = ComponentFactory.createViewLabel();
	JLabel clusterFeatureLabel = ComponentFactory.createViewLabel();

	JPanel compoundPanel;
	JLabel compoundNameLabel = ComponentFactory.createViewLabel();
	JTextArea compoundSmilesLabel = ComponentFactory.createViewTextArea("", false, false);
	JLabel compoundFeatureLabelHeader = ComponentFactory.createViewLabel();
	JLabel compoundFeatureLabel = ComponentFactory.createViewLabel();

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
					updateCompound();
			}
		});
	}

	private void buildLayout()
	{
		DefaultFormBuilder b1 = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
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

		DefaultFormBuilder b3 = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		b3.append(ComponentFactory.createViewLabel("<html><b>Compound:</b><html>"));
		b3.append(compoundNameLabel);
		b3.nextLine();
		b3.append(ComponentFactory.createViewLabel("<html>Smiles:<html>"));
		b3.append(compoundSmilesLabel);
		b3.nextLine();
		b3.append(compoundFeatureLabelHeader);
		b3.append(compoundFeatureLabel);

		compoundPanel = b3.getPanel();
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
		//		setBackground(Settings.TRANSPARENT_BACKGROUND);
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
			compoundNameLabel.setText(m.toString());
			compoundSmilesLabel.setText(m.getSmiles());

			compoundPanel.setIgnoreRepaint(false);
			compoundPanel.setVisible(true);

			if (viewControler.getHighlighter() instanceof MoleculePropertyHighlighter)
			{
				MoleculeProperty p = ((MoleculePropertyHighlighter) viewControler.getHighlighter()).getProperty();
				compoundFeatureLabelHeader.setText(p.getName() + ":");
				compoundFeatureLabel.setText(m.getTemperature(p));
				compoundFeatureLabel.setVisible(true);
				compoundFeatureLabelHeader.setVisible(true);
			}
			else
			{
				compoundFeatureLabel.setVisible(false);
				compoundFeatureLabelHeader.setVisible(false);
			}
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
			String mcs = c.getSubstructureSmarts(SubstructureSmartsType.MCS);
			clusterMCSLabelHeader.setVisible(mcs != null);
			clusterMCSLabel.setVisible(mcs != null);
			clusterMCSLabel.setText(mcs);

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
