package gui;

import gui.ViewControler.MoleculePropertyHighlighter;

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

public class InfoPanel extends JPanel
{
	Clustering clustering;
	ViewControler viewControler;

	JPanel clusterPanel;
	JLabel clusterNameLabel = ComponentFactory.createLabel();
	JLabel clusterSizeLabel = ComponentFactory.createLabel();
	JLabel clusterAlgLabel = ComponentFactory.createLabel();
	JLabel clusterEmbedLabel = ComponentFactory.createLabel();
	JLabel clusterAlignLabel = ComponentFactory.createLabel();
	JLabel clusterMCSLabelHeader = ComponentFactory.createLabel("MCS:");
	JLabel clusterMCSLabel = ComponentFactory.createLabel();

	JPanel compoundPanel;
	JLabel compoundNameLabel = ComponentFactory.createLabel();
	JTextArea compoundSmilesLabel = ComponentFactory.createTextArea("", false, false);
	JLabel compoundFeatureLabelHeader = ComponentFactory.createLabel();
	JLabel compoundFeatureLabel = ComponentFactory.createLabel();

	public InfoPanel(ViewControler viewControler, Clustering clustering)
	{
		this.viewControler = viewControler;
		this.clustering = clustering;

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
		b1.append(ComponentFactory.createLabel("<html><b>Cluster:</b><html>"));
		b1.append(clusterNameLabel);
		b1.nextLine();
		b1.append(ComponentFactory.createLabel("<html>Num compounds:<html>"));
		b1.append(clusterSizeLabel);
		b1.nextLine();
		b1.append(ComponentFactory.createLabel("<html>Cluster algorithm:<html>"));
		b1.append(clusterAlgLabel);
		b1.nextLine();
		b1.append(ComponentFactory.createLabel("<html>3D Embedding:<html>"));
		b1.append(clusterEmbedLabel);
		b1.nextLine();
		b1.append(ComponentFactory.createLabel("<html>3D Alignement:<html>"));
		b1.append(clusterAlignLabel);
		b1.nextLine();
		b1.append(clusterMCSLabelHeader);
		b1.append(clusterMCSLabel);

		clusterPanel = b1.getPanel();
		clusterPanel.setOpaque(false);
		clusterPanel.setVisible(false);

		DefaultFormBuilder b2 = new DefaultFormBuilder(new FormLayout("p,3dlu,p"));
		b2.append(ComponentFactory.createLabel("<html><b>Compound:</b><html>"));
		b2.append(compoundNameLabel);
		b2.nextLine();
		b2.append(ComponentFactory.createLabel("<html>Smiles:<html>"));
		b2.append(compoundSmilesLabel);
		b2.nextLine();
		b2.append(compoundFeatureLabelHeader);
		b2.append(compoundFeatureLabel);

		compoundPanel = b2.getPanel();
		compoundPanel.setOpaque(false);
		compoundPanel.setVisible(false);

		setLayout(new BorderLayout(10, 10));
		add(clusterPanel, BorderLayout.NORTH);
		add(compoundPanel, BorderLayout.SOUTH);
		setOpaque(false);
	}

	private void updateCompound()
	{
		int index = InfoPanel.this.clustering.getModelActive().getSelected();
		if (index == -1)
			index = InfoPanel.this.clustering.getModelWatched().getSelected();

		if (index == -1)
			compoundPanel.setVisible(false);
		else
		{
			compoundPanel.setIgnoreRepaint(true);

			Model m = clustering.getModelWithModelIndex(index);
			compoundNameLabel.setText("Compound " + index);
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
			clusterAlgLabel.setText(c.getClusterAlgorithm());
			clusterEmbedLabel.setText(c.getEmbedAlgorithm());
			clusterAlignLabel.setText(c.getAlignAlgorithm());
			String mcs = c.getSubstructureSmarts(SubstructureSmartsType.MCS);
			clusterMCSLabelHeader.setVisible(mcs != null);
			clusterMCSLabel.setVisible(mcs != null);
			clusterMCSLabel.setText(mcs);

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
