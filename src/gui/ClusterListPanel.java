package gui;

import gui.DoubleNameListCellRenderer.DoubleNameElement;
import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.CompoundPropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.ScreenSetup;
import cluster.Cluster;
import cluster.ClusterController;
import cluster.Clustering;
import cluster.Clustering.SelectionListener;
import cluster.ClusteringImpl;

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
	ClusterController clusterControler;
	ViewControler viewControler;
	GUIControler guiControler;

	ControlPanel controlPanel;

	JCheckBox superimposeCheckBox;

	JPanel filterPanel;
	JLabel filterLabel;
	LinkButton filterRemoveButton;

	public ClusterListPanel(Clustering clustering, ClusterController clusterControler, ViewControler viewControler,
			GUIControler guiControler)
	{
		this.clustering = clustering;
		this.clusterControler = clusterControler;
		this.viewControler = viewControler;
		this.guiControler = guiControler;

		buildLayout();
		installListeners();
	}

	private void installListeners()
	{
		clustering.addSelectionListener(new SelectionListener()
		{
			@Override
			public void clusterWatchedChanged(Cluster c)
			{
				updateCluster(c);
			}

			@Override
			public void clusterActiveChanged(Cluster c)
			{
				updateCluster(c);
				updateSuperimposeCheckBox();
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
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
				{
					updateFilter();
				}
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
						clusterControler.clearClusterWatched();
				}
				else
					clusterControler.setClusterWatched((Cluster) clusterList.getSelectedValue());

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
				Thread th = new Thread(new Runnable()
				{
					public void run()
					{
						if (clusterList.getSelectedValue() == AllClusters)
						{
							clusterControler.clearClusterActive(true, true);
						}
						else
						{
							Cluster c = (Cluster) clusterList.getSelectedValue();
							if (c == clustering.getActiveCluster())
								clusterControler.clearCompoundActive(true);
							else
								clusterControler.setClusterActive(c, true, true);
						}
						selfBlock = false;
					}
				});
				th.start();
			}
		});

		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_ADDED)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_REMOVED))
				{
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
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_SORTING_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
				{
					selfBlock = true;
					updateList();
					selfBlock = false;
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
					updateListSize();

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
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		listModel.clear();

		if (clustering.getNumClusters() == 0)
		{
			scroll.setVisible(false);
			return;
		}

		if (clustering.getNumClusters() > 1)
			listModel.addElement(AllClusters);

		Cluster clusters[] = new Cluster[clustering.numClusters()];
		clustering.getClusters().toArray(clusters);
		if (viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)//&& viewControler.isFeatureSortingEnabled() 
			Arrays.sort(clusters);
		for (Cluster c : clusters)
			if (c.size() > 0)
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

		if (clustering.isClusterActive())
			updateCluster(clustering.getActiveCluster());
	}

	private void updateListSize()
	{
		//		System.err.println("row height " + listRenderer.getRowHeight());
		int rowCount = (guiControler.getComponentMaxHeight(1) / listRenderer.getRowHeight()) / 3;
		//		System.err.println("row count " + rowCount);

		double ratioVisible = rowCount / (double) listModel.getSize();
		if (ratioVisible <= 0.5)
		{
			// if less then 50% of elements is visible increase by up to 50%
			double ratioIncrease = 0.5 - ratioVisible;
			rowCount += (int) (ratioIncrease * rowCount);
		}

		clusterList.setVisibleRowCount(rowCount);

		scroll.setPreferredSize(null);
		scroll.setPreferredSize(new Dimension(Math.min(guiControler.getComponentMaxWidth(1 / 7.0),
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
			@Override
			public void updateUI()
			{
				super.updateUI();
				if (getFontLabel1() != null)
				{
					setFontLabel1(getFontLabel1().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
					setFontLabel2(getFontLabel2().deriveFont((float) ScreenSetup.INSTANCE.getFontSize()));
					clusterList.setFixedCellHeight(getRowHeight());
					//					clusterList.setPreferredSize(null);
				}
			}

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				Cluster c = null;
				if (value != AllClusters)
					c = (Cluster) value;
				setOpaque(isSelected || c == clustering.getActiveCluster());

				setForeground(ComponentFactory.FOREGROUND);

				if (c == clustering.getActiveCluster())
				{
					setBackground(ComponentFactory.LIST_ACTIVE_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (isSelected)
				{
					setBackground(ComponentFactory.LIST_WATCH_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (c != null && c.getHighlightColor() != null
						&& c.getHighlightColor() != CompoundPropertyUtil.getNullValueColor())
				{
					setForegroundLabel2(c.getHighlightColor());
				}

				return this;
			}

		};
		listRenderer.setFontLabel2(listRenderer.getFontLabel2().deriveFont(Font.ITALIC));
		clusterList.setCellRenderer(listRenderer);

		compoundListPanel = new CompoundListPanel(clustering, clusterControler, viewControler, guiControler);

		setLayout(new BorderLayout(0, 0));
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
		controlPanel = new ControlPanel(viewControler, clusterControler, clustering, guiControler);
		add(controlPanel, BorderLayout.SOUTH);

		filterLabel = ComponentFactory.createViewLabel("Filter applied:");
		filterRemoveButton = ComponentFactory.createViewLinkButton("remove");
		filterRemoveButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Thread th = new Thread(new Runnable()
				{
					public void run()
					{
						clusterControler.setCompoundFilter(null, true);
					}
				});
				th.start();
			}
		});
		filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));//new BorderLayout());
		filterPanel.setOpaque(false);
		filterPanel.add(filterLabel);//, BorderLayout.WEST);
		filterPanel.add(filterRemoveButton);//, BorderLayout.EAST);
		filterPanel.setVisible(false);
		add(filterPanel, BorderLayout.NORTH);

		setBorder(new EmptyBorder(25, 25, 25, 25));
		setOpaque(false);
	}

	private void updateFilter()
	{
		if (clusterControler.getCompoundFilter() == null)
		{
			filterPanel.setVisible(false);
		}
		else
		{
			filterPanel.setIgnoreRepaint(true);
			filterLabel.setText("Compound Filter: " + clusterControler.getCompoundFilter());
			filterPanel.setIgnoreRepaint(false);
			filterPanel.setVisible(true);
		}
	}

	private void updateCluster(Cluster c)
	{
		if (selfBlock)
			return;
		selfBlock = true;

		if (c == null)
			clusterList.clearSelection();
		else
			clusterList.setSelectedValue(c, true);

		selfBlock = false;
	}

}
