package gui;

import gui.DoubleNameListCellRenderer.DoubleNameElement;
import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;

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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.ScreenSetup;
import util.ImageLoader;
import cluster.Cluster;
import cluster.ClusterController;
import cluster.Clustering;
import cluster.Clustering.SelectionListener;
import cluster.ClusteringImpl;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.lowagie.text.Font;

public class ClusterListPanel extends JPanel
{
	JPanel clusterPanel;

	DefaultListModel<DoubleNameElement> listModel;
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

	JPanel filterPanel;
	JLabel filterLabel;
	JButton filterRemoveButton;

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
			}
		});

		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
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
				if (clusterList.getSelectedValue() == AllCompounds || clusterList.getSelectedIndex() == -1)
					clusterControler.clearClusterWatched();
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
						//clear only if there is a single zoomed in compound
						boolean clearCompound = clustering.getActiveCompounds().length == 1
								&& View.instance.getZoomTarget() == clustering.getActiveCompound();
						if (clusterList.getSelectedValue() == AllCompounds)
						{
							if (clustering.isClusterActive())
								clusterControler.clearClusterActive(true, clearCompound);
							SwingUtilities.invokeLater(new Runnable()
							{
								@Override
								public void run()
								{
									viewControler.setSingleCompoundSelection(true);
								}
							});
						}
						else
						{
							Cluster c = (Cluster) clusterList.getSelectedValue();
							if (c != clustering.getActiveCluster())
								clusterControler.setClusterActive(c, true, clearCompound);
							else if (clearCompound)
								clusterControler.clearCompoundActive(true);
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
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_REMOVED)
						|| evt.getPropertyName().equals(ClusteringImpl.CLUSTER_CLEAR))
				{
					selfBlock = true;
					updateList();
					updateFilter();
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

	public DoubleNameElement AllCompounds = new DoubleNameElement()
	{
		@Override
		public String getFirstName()
		{
			return "All Compounds";
		}

		@Override
		public String getSecondName()
		{
			if (viewControler.getHighlightedProperty() != null)
				return clustering.getFormattedValue(viewControler.getHighlightedProperty());
			else
				return null;
		}
	};

	private void updateList()
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		listModel.clear();

		if (clustering.getNumClusters() < 2)
		{
			scroll.setVisible(false);
			return;
		}

		listModel.addElement(AllCompounds);
		Cluster clusters[] = new Cluster[clustering.numClusters()];
		clustering.getClusters().toArray(clusters);
		if (viewControler.getHighlightedProperty() != null)
			Arrays.sort(clusters);
		for (Cluster c : clusters)
			if (c.size() > 0)
				listModel.addElement(c);

		updateListSize();
		scroll.setVisible(listModel.size() > 1);

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

	@SuppressWarnings("unchecked")
	private void buildLayout()
	{
		listModel = new DefaultListModel<DoubleNameElement>();
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

			@SuppressWarnings("rawtypes")
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

				Cluster c = value == AllCompounds ? null : (Cluster) value;
				setOpaque(isSelected || c == clustering.getActiveCluster());
				setForeground(ComponentFactory.FOREGROUND);
				setBackground(ComponentFactory.BACKGROUND);
				if ((c != null && clustering.isClusterActive() && c == clustering.getActiveCluster())
						|| (c == null && viewControler.isSingleCompoundSelection()))
				{
					setBackground(ComponentFactory.LIST_ACTIVE_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (isSelected)
				{
					setBackground(ComponentFactory.LIST_WATCH_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (c != null)
				{
					setForegroundLabel2(c.getHighlightColorText());
				}
				else if (c == null)
				{
					setForegroundLabel2(clustering.getHighlightColorText());
				}
				return this;
			}

		};
		listRenderer.setFontLabel2(listRenderer.getFontLabel2().deriveFont(Font.ITALIC));
		clusterList.setCellRenderer(listRenderer);

		compoundListPanel = new CompoundListPanel(clustering, clusterControler, viewControler, guiControler);

		setLayout(new BorderLayout(0, 0));
		FormLayout layout = new FormLayout("pref,6,pref", "fill:pref");
		JPanel panel = new JPanel();
		panel.setOpaque(false);
		panel.setLayout(layout);
		CellConstraints cc = new CellConstraints();
		scroll = ComponentFactory.createViewScrollpane(clusterList);
		clusterPanel = new TransparentViewPanel(new BorderLayout(5, 5));
		clusterPanel.add(scroll);
		panel.add(clusterPanel, cc.xy(1, 1));
		panel.add(compoundListPanel, cc.xy(3, 1));
		add(panel, BorderLayout.WEST);

		controlPanel = new ControlPanel(viewControler, clusterControler, clustering, guiControler);
		add(controlPanel, BorderLayout.SOUTH);

		filterLabel = ComponentFactory.createViewLabel("Filter applied:",
				ImageLoader.getImage(ImageLoader.Image.filter14_black),
				ImageLoader.getImage(ImageLoader.Image.filter14));
		filterRemoveButton = ComponentFactory.createCrossViewButton();
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
		if (clusterControler.getCompoundFilter() == null || clustering.getNumClusters() == 0)
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
