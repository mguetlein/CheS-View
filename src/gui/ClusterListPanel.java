package gui;

import gui.DoubleNameListCellRenderer.DoubleNameElement;
import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.CompoundPropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

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
import cluster.Compound;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.lowagie.text.Font;

import dataInterface.CompoundPropertyUtil;

public class ClusterListPanel extends JPanel
{
	//	JLabel datasetNameLabel;

	JPanel clusterPanel;

	DefaultListModel listModel;
	DoubleNameListCellRenderer listRenderer;

	MouseOverList clusterList;
	JScrollPane scroll;
	boolean selfBlock = false;

	CompoundListPanel compoundListPanel;

	Clustering clustering;
	ViewControler viewControler;
	GUIControler guiControler;

	ControlPanel controlPanel;

	JCheckBox superimposeCheckBox;

	//	private DefaultListCellRenderer clusterListRenderer;

	public ClusterListPanel(Clustering clustering, ViewControler viewControler, GUIControler guiControler)
	{
		this.clustering = clustering;
		this.viewControler = viewControler;
		this.guiControler = guiControler;

		buildLayout();
		installListeners();
	}

	private void installListeners()
	{
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

				if (clusterList.getSelectedValue() == AllClusters)
				{
					// clear only if home selected
					if (e.getFirstIndex() == 0)
						clustering.getClusterWatched().clearSelection();
				}
				else
					clustering.getClusterWatched().setSelected(
							clustering.indexOf((Cluster) clusterList.getSelectedValue()));

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

				if (clusterList.getSelectedValue() == AllClusters)
				{
					View.instance.suspendAnimation("clear cluster selection");
					clustering.getCompoundWatched().clearSelection();
					if (View.instance.getZoomTarget() instanceof Compound)
						clustering.getCompoundActive().clearSelection();
					View.instance.proceedAnimation("clear cluster selection");
					clustering.getClusterActive().clearSelection();
				}
				else
				{
					int cIndex = clustering.indexOf((Cluster) clusterList.getSelectedValue());
					clustering.getCompoundWatched().clearSelection();
					boolean suspendAnim = clustering.getClusterActive().getSelected() != cIndex;
					if (suspendAnim)
						View.instance.suspendAnimation("change cluster selection");
					if (View.instance.getZoomTarget() instanceof Compound)
						clustering.getCompoundActive().clearSelection();
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

		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_DESCRIPTOR_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
				{
					selfBlock = true;
					updateList();
					selfBlock = false;
				}
			}
		});

		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_VIEWER_SIZE_CHANGED))
					updateListSize();
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
			listModel.addElement(AllClusters);

		Cluster clusters[] = new Cluster[clustering.numClusters()];
		clustering.getClusters().toArray(clusters);
		if (viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)
			Arrays.sort(clusters);
		for (Cluster c : clusters)
			listModel.addElement(c);

		updateListSize();
		scroll.setVisible(listModel.size() > 1);

		if (listModel.size() == 0)
			superimposeCheckBox.setVisible(false);
		else
		{
			updateSuperimposeCheckBox();
			if (listModel.size() == 1)
				compoundListPanel.appendCheckbox(superimposeCheckBox);
			else
				clusterPanel.add(superimposeCheckBox, BorderLayout.SOUTH);
		}

		updateListSize();
		revalidate();
	}

	//	private void updateListSize()
	//	{
	//		int rowCount = (guiControler.getViewerHeight() / listRenderer.getPreferredSize().height) / 3;
	//		clusterList.setVisibleRowCount(rowCount);
	//		clusterList.revalidate();
	//	}

	private void updateListSize()
	{
		//		System.err.println("row height " + listRenderer.getRowHeight());
		int rowCount = (guiControler.getViewerHeight() / listRenderer.getRowHeight()) / 3;
		//		System.err.println("row count " + rowCount);
		clusterList.setVisibleRowCount(rowCount);

		scroll.setPreferredSize(null);
		scroll.setPreferredSize(new Dimension(Math.min(guiControler.getViewerWidth() / 7,
				scroll.getPreferredSize().width), scroll.getPreferredSize().height));
		scroll.revalidate();
		scroll.repaint();
	}

	public static DoubleNameElement AllClusters = new DoubleNameElement()
	{
		@Override
		public String getFirstName()
		{
			return "All clusters";
		}

		@Override
		public String getSecondName()
		{
			return null;
		}
	};

	private void buildLayout()
	{
		listModel = new DefaultListModel();
		clusterList = new MouseOverList(listModel);
		clusterList.setClearOnExit(false);
		clusterList.setFocusable(false);
		clusterList.setOpaque(false);

		listRenderer = new DoubleNameListCellRenderer(listModel)
		{
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				int i = -1;
				if (value != AllClusters)
					i = clustering.indexOf((Cluster) value);
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
				else if (i != -1 && ((Cluster) value).getHighlightColor() != null
						&& ((Cluster) value).getHighlightColor() != CompoundPropertyUtil.getNullValueColor())
				{
					setForegroundLabel2(((Cluster) value).getHighlightColor());
				}

				return this;
			}

		};
		listRenderer.setFontLabel2(listRenderer.getFontLabel2().deriveFont(Font.ITALIC));
		clusterList.setCellRenderer(listRenderer);

		compoundListPanel = new CompoundListPanel(clustering, viewControler, guiControler)
		{
			public Dimension getPreferredSize()
			{
				Dimension dim = super.getPreferredSize();
				if (dim.width > 0)
					dim.width = Math.max(dim.width, clusterPanel.getPreferredSize().width);
				return dim;
			}
		};

		setLayout(new BorderLayout(10, 10));
		FormLayout layout = new FormLayout("pref,10,pref", "fill:pref");
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(layout);
		CellConstraints cc = new CellConstraints();
		scroll = ComponentFactory.createViewScrollpane(clusterList);
		clusterPanel = new TransparentViewPanel(new BorderLayout(5, 5));
		clusterPanel.add(scroll);
		superimposeCheckBox = ComponentFactory.createViewCheckBox("Superimpose");
		superimposeCheckBox.setSelected(viewControler.isSuperimpose());
		superimposeCheckBox.setOpaque(false);
		clusterPanel.add(superimposeCheckBox, BorderLayout.SOUTH);
		panel.add(clusterPanel, cc.xy(1, 1));
		panel.add(compoundListPanel, cc.xy(3, 1));
		add(panel, BorderLayout.WEST);
		controlPanel = new ControlPanel(viewControler, clustering, guiControler);
		add(controlPanel, BorderLayout.SOUTH);

		setBorder(new EmptyBorder(25, 25, 25, 25));
		setOpaque(false);
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
