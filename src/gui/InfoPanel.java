package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import main.Settings;
import cluster.Cluster;
import cluster.Clustering;
import cluster.Model;
import dataInterface.MoleculeProperty;
import dataInterface.SubstructureSmartsType;

public class InfoPanel extends JPanel
{
	JTable table;
	DefaultTableModel model;
	JScrollPane scroll;
	Clustering clustering;
	LinkButton resizeButton;

	public InfoPanel(Clustering clustering)
	{
		this.clustering = clustering;

		buildLayout();
		clustering.getModelWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(false, InfoPanel.this.clustering.getModelWatched().getSelected());
			}
		});
		clustering.getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (InfoPanel.this.clustering.getClusterActive().getSelected() == -1)
					update(true, InfoPanel.this.clustering.getClusterWatched().getSelected());
			}
		});
		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update(true, -1);
			}
		});
	}

	private void buildLayout()
	{
		JPanel p = new JPanel(new BorderLayout(2, 2));
		p.setOpaque(false);

		resizeButton = ComponentFactory.createLinkButton("");
		resizeButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				setSmall(!small);
				if (InfoPanel.this.clustering.getClusterActive().getSelected() == -1)
					update(true, InfoPanel.this.clustering.getClusterWatched().getSelected());
				else
					update(false, InfoPanel.this.clustering.getModelWatched().getSelected());
			}
		});

		LinkButton clearButton = ComponentFactory.createLinkButton("close");
		clearButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (InfoPanel.this.clustering.getClusterActive().getSelected() == -1)
					InfoPanel.this.clustering.getClusterWatched().clearSelection();
				else
					InfoPanel.this.clustering.getModelWatched().clearSelection();
			}
		});

		JPanel buttons = new JPanel();
		buttons.setOpaque(false);
		buttons.add(resizeButton);
		buttons.add(new JLabel("|"));
		buttons.add(clearButton);

		JPanel pp = new JPanel(new BorderLayout());
		pp.setOpaque(false);
		pp.add(buttons, BorderLayout.EAST);
		p.add(pp, BorderLayout.NORTH);

		table = ComponentFactory.createTable();

		table.addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				if (e.getKeyCode() == KeyEvent.VK_DELETE)
				{
					if (table.getSelectedRow() != -1)
					{
						model.removeRow(table.getSelectedRow());
						//										maxNumRows--;
						//										resize();
						//										repaint();
					}
				}
			}
		});

		model = (DefaultTableModel) table.getModel();
		model.addColumn("Property");
		model.addColumn("Value");
		scroll = ComponentFactory.createScrollpane(table);

		//JPanel p = new JPanel();
		//p.add(scroll);

		scroll.setOpaque(true);
		scroll.setBackground(Settings.TRANSPARENT_BACKGROUND);

		//p.setBorder(new CompoundBorder(ComponentFactory.createLineBorder(1), new EmptyBorder(5, 5, 5, 5)));
		p.add(scroll, BorderLayout.CENTER);

		add(p);
		setOpaque(false);
		//setBackground(Settings.TRANSPARENT_BACKGROUND);
		setVisible(false);
	}

	int maxNumRows;
	int maxWidth;

	boolean small = true;

	private void setSmall(boolean small)
	{
		this.small = small;
		if (small)
		{
			resizeButton.setText("bigger");
			table.getColumnModel().getColumn(0).setPreferredWidth(150);
			table.getColumnModel().getColumn(0).setMaxWidth(150);
			maxNumRows = 2;
			maxWidth = 400;
		}
		else
		{
			resizeButton.setText("smaller");
			table.getColumnModel().getColumn(0).setPreferredWidth(200);
			table.getColumnModel().getColumn(0).setMaxWidth(200);
			maxNumRows = 8;
			maxWidth = 550;
		}
	}

	private void resize()
	{
		int num = Math.min(maxNumRows, model.getRowCount());
		scroll.setPreferredSize(new Dimension(maxWidth, num * (table.getRowHeight() + 1)));
	}

	private void update(boolean updateCluster, int index)
	{
		if (index == -1)
			setVisible(false);
		else
		{
			setIgnoreRepaint(true);

			while (model.getRowCount() > 0)
				model.removeRow(0);

			if (updateCluster)
			{
				Cluster c = clustering.getCluster(index);
				model.addRow(new Object[] { "Cluster", c.getName() });
				model.addRow(new Object[] { "Num molecules", c.getModels().size() });
				for (SubstructureSmartsType type : clustering.getSubstructures())
					model.addRow(new Object[] { type.toString(), c.getSubstructureSmarts(type) });
				for (MoleculeProperty p : clustering.getFeatures())
					model.addRow(new Object[] { p, c.getSummaryStringValue(p) });
				for (MoleculeProperty p : clustering.getProperties())
					model.addRow(new Object[] { p, c.getSummaryStringValue(p) });
			}
			else
			{
				model.addRow(new Object[] { "Model", View.instance.getModelName(index) });
				model.addRow(new Object[] { "Num atoms", View.instance.getAtomCountInModel(index) });
				model.addRow(new Object[] { "Smiles", clustering.getModelWithModelIndex(index).getSmiles() });
				for (SubstructureSmartsType type : clustering.getSubstructures())
					model.addRow(new Object[] { type.toString(),
							clustering.getClusterForModelIndex(index).getSubstructureSmarts(type) });
				Model m = clustering.getModelWithModelIndex(index);
				for (MoleculeProperty p : clustering.getFeatures())
					model.addRow(new Object[] { p, m.getTemperature(p) });
				for (MoleculeProperty p : clustering.getProperties())
					model.addRow(new Object[] { p, m.getTemperature(p) });
			}
			setSmall(small);
			resize();
			setIgnoreRepaint(false);
			setVisible(true);
			repaint();
		}
	}
}
