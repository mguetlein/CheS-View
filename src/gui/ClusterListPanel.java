package gui;

import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cluster.Cluster;
import cluster.Clustering;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ClusterListPanel extends JPanel
{
	//	JLabel datasetNameLabel;

	JPanel clusterPanel;

	DefaultListModel listModel;
	MouseOverList clusterList;
	JScrollPane scroll;
	boolean selfBlock = false;

	ModelListPanel modelListPanel;

	Clustering clustering;
	ViewControler viewControler;

	JCheckBox superimposeCheckBox;

	public ClusterListPanel(Clustering clustering, ViewControler viewControler)
	{
		this.clustering = clustering;
		this.viewControler = viewControler;

		buildLayout();
		installListeners();
	}

	private void installListeners()
	{
		superimposeCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfBlock)
					return;
				viewControler.setSuperimpose(superimposeCheckBox.isSelected());
			}
		});

		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_SUPERIMPOSE_CHANGED))
				{
					updateSuperimposeCheckBox();
				}
			}
		});

		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateSuperimposeCheckBox();
			}
		});

		clusterList.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;

				Cluster c = (Cluster) clusterList.getSelectedValue();
				if (c == null)
				{
					// clear only if home selected
					if (e.getFirstIndex() == 0)
						clustering.getClusterWatched().clearSelection();
				}
				else
					clustering.getClusterWatched().setSelected(clustering.indexOf(c));

				selfBlock = false;
			}
		});
		clusterList.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;

				final Cluster c = (Cluster) clusterList.getSelectedValue();
				if (c == null)
				{
					View.instance.suspendAnimation("clear cluster selection");
					clustering.getModelWatched().clearSelection();
					clustering.getModelActive().clearSelection();
					View.instance.proceedAnimation("clear cluster selection");
					clustering.getClusterActive().clearSelection();
				}
				else
				{
					int cIndex = clustering.indexOf(c);
					clustering.getModelWatched().clearSelection();
					boolean suspendAnim = clustering.getClusterActive().getSelected() != cIndex;
					if (suspendAnim)
						View.instance.suspendAnimation("change cluster selection");
					clustering.getModelActive().clearSelection();
					if (suspendAnim)
						View.instance.proceedAnimation("change cluster selection");
					clustering.getClusterActive().setSelected(cIndex);
				}
				clustering.getClusterWatched().clearSelection();

				selfBlock = false;
			}
		});

		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(Clustering.CLUSTER_ADDED)
						|| evt.getPropertyName().equals(Clustering.CLUSTER_REMOVED))
				{
					// if (selfBlock)
					// return;
					selfBlock = true;
					updateList();
					selfBlock = false;
				}
			}
		});
	}

	private void updateSuperimposeCheckBox()
	{
		selfBlock = true;
		superimposeCheckBox.setSelected(viewControler.isSuperimpose());
		if (clustering.isClusterActive())
			superimposeCheckBox.setVisible(viewControler.isSingleClusterSpreadable());
		else
			superimposeCheckBox.setVisible(viewControler.isAllClustersSpreadable());
		selfBlock = false;
	}

	private void updateList()
	{
		//		if (listModel.size() == 0)
		//			datasetNameLabel.setVisible(false);
		//		else
		//		{
		//			datasetNameLabel.setVisible(true);
		//			datasetNameLabel.setText("<html><b>Dataset: </b>" + clustering.getName() + " (#"
		//					+ clustering.getNumCompounds() + ")</html>");
		//		}
		//				+ " some endless long name just to make really really really really sure");

		listModel.clear();
		if (clustering.getNumClusters() > 1)
			listModel.addElement(null);
		for (Cluster c : clustering.getClusters())
			listModel.addElement(c);

		clusterList.setVisibleRowCount(16);//Math.min(16, clustering.numClusters() + 1));

		scroll.setVisible(listModel.size() > 1);

		if (listModel.size() == 0)
			superimposeCheckBox.setVisible(false);
		else
		{
			updateSuperimposeCheckBox();
			if (listModel.size() == 1)
				modelListPanel.appendCheckbox(superimposeCheckBox);
			else
				clusterPanel.add(superimposeCheckBox, BorderLayout.SOUTH);
		}

		revalidate();
	}

	private void buildLayout()
	{
		//		datasetNameLabel = ComponentFactory.createLabel();

		listModel = new DefaultListModel();
		clusterList = new MouseOverList(listModel);
		clusterList.setClearOnExit(false);
		clusterList.setFocusable(false);

		//		clusterList.setVisibleRowCount(5);

		//clusterList.setBackground(Settings.TRANSPARENT_BACKGROUND);
		clusterList.setOpaque(false);

		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateCluster(clustering.getClusterActive().getSelected());
			}
		});
		clustering.getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateCluster(clustering.getClusterWatched().getSelected());
			}
		});

		clusterList.setCellRenderer(new DefaultListCellRenderer()
		{
			// Color selectionColor =
			// UIManager.getColor("List.selectionBackground");
			// Color watchColor = new Color(selectionColor.getRed(),
			// selectionColor.getGreen(), selectionColor.getBlue(),
			// 100);

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				Cluster c = (Cluster) value;
				int i = clustering.indexOf(c);
				if (value == null)
					value = "All clusters";
				else
					value = c.toString();
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setOpaque(isSelected || i == clustering.getClusterActive().getSelected());

				setForeground(ComponentFactory.FOREGROUND);
				if (i == clustering.getClusterActive().getSelected())
				{
					setBackground(ComponentFactory.LIST_ACTIVE_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (isSelected)
				{
					setBackground(ComponentFactory.LIST_WATCH_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				return this;
			}
		});

		// add(clusterList, BorderLayout.WEST);

		// clusterList.setBorder(new
		// CompoundBorder(ComponentFactory.createThinBorder(), new
		// EmptyBorder(5, 5, 5, 5)));
		//		clusterList.setBorder(new EmptyBorder(5, 5, 5, 5));

		modelListPanel = new ModelListPanel(clustering, viewControler)
		{
			public Dimension getPreferredSize()
			{
				Dimension dim = super.getPreferredSize();
				if (dim.width > 0)
					dim.width = Math.max(dim.width, clusterPanel.getPreferredSize().width);
				return dim;
			}
		};
		//		SwingUtil.setDebugBorder(infoPanel);

		// setLayout(new GridLayout(2, 1));
		// add(clusterList);
		// add(infoPanel);

		setLayout(new BorderLayout(10, 10));

		FormLayout layout = new FormLayout("pref,10,pref",
		//"pref, 5, "
				"fill:pref");//, 15, fill:pref:grow, 10, pref, 0, pref");
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(layout);
		// PanelBuilder panel = new PanelBuilder(layout, new FormDebugPanel());

		CellConstraints cc = new CellConstraints();
		//		int lineCount = 1;

		scroll = ComponentFactory.createViewScrollpane(clusterList);

		//		datasetNameLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
		//		panel.add(datasetNameLabel, cc.xy(1, lineCount));
		//		lineCount += 2;

		clusterPanel = new TransparentViewPanel(new BorderLayout(5, 5))
		{
			//			public Dimension getPreferredSize()
			//			{
			//				Dimension dim = super.getPreferredSize();
			//				if (dim.width > 0)
			//					dim.width = Math.max(dim.width, modelListPanel.getPreferredSize().width);
			//				return dim;
			//			}
		};
		//		clusterPanel.setOpaque(false);
		//		clusterPanel.setBackground(Settings.TRANSPARENT_BACKGROUND);
		clusterPanel.add(scroll);
		superimposeCheckBox = ComponentFactory.createViewCheckBox("Superimpose");
		superimposeCheckBox.setSelected(viewControler.isSuperimpose());
		superimposeCheckBox.setOpaque(false);
		clusterPanel.add(superimposeCheckBox, BorderLayout.SOUTH);
		panel.add(clusterPanel, cc.xy(1, 1));

		//		lineCount += 2;
		panel.add(modelListPanel, cc.xy(3, 1));
		//		lineCount += 2;
		//		panel.add(new ControlPanel(viewControler), cc.xyw(1, lineCount, 2));

		//		add(datasetNameLabel, BorderLayout.NORTH);
		add(panel, BorderLayout.WEST);
		add(new ControlPanel(viewControler), BorderLayout.SOUTH);

		//		lineCount += 2;
		//		JLabel la = ComponentFactory.createLabel(" " + Settings.VERSION_STRING);
		//		la.setFont(la.getFont().deriveFont(Font.ITALIC));
		//		JPanel p = new JPanel();
		//		p.add(la);
		//		p.setOpaque(false);
		//		panel.add(p, cc.xy(1, lineCount));

		// infoPanel.setBorder(new MatteBorder(1, 1, 1, 1, Color.RED));

		// setLayout(new BorderLayout());
		// add(panel.getPanel(), BorderLayout.WEST);

		setBorder(new EmptyBorder(25, 25, 25, 25));

		setOpaque(false);
		//setBackground(Settings.TRANSPARENT_BACKGROUND);
	}

	private void updateCluster(int index)
	{
		if (selfBlock)
			return;
		selfBlock = true;

		if (index == -1)
			clusterList.clearSelection();
		else
			clusterList.setSelectedValue(clustering.getCluster(index), true);

		selfBlock = false;
	}

}
