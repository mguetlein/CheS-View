package gui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Settings;
import cluster.Cluster;
import cluster.Clustering;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class SideBar extends JPanel
{
	JLabel datasetNameLabel;

	DefaultListModel listModel;
	MouseOverList clusterList;
	JScrollPane scroll;
	boolean selfBlock = false;

	ModelListPanel infoPanel;

	Clustering clustering;
	ViewControler viewControler;

	public SideBar(Clustering clustering, ViewControler viewControler)
	{
		this.clustering = clustering;
		this.viewControler = viewControler;

		buildLayout();
		installListeners();
	}

	private void installListeners()
	{
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
					clustering.getClusterActive().clearSelection();
				else
				{
					int cIndex = clustering.indexOf(c);
					if (clustering.getClusterActive().getSelected() == cIndex)
					{
						clustering.getModelActive().clearSelection();
						clustering.getModelWatched().clearSelection();
					}
					clustering.getClusterActive().setSelected(cIndex);
				}
				clustering.getClusterWatched().clearSelection();

				selfBlock = false;
			}
		});

		clustering.addRemoveAddClusterListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				// if (selfBlock)
				// return;
				selfBlock = true;
				updateList();
				selfBlock = false;
			}
		});
	}

	private void updateList()
	{
		datasetNameLabel.setText(clustering.getName());

		// clusterList.setIgnoreRepaint(true);
		listModel.clear();
		if (clustering.getNumClusters() > 1)
			listModel.addElement(null);
		for (Cluster c : clustering.getClusters())
			listModel.addElement(c);
		// clusterList.setIgnoreRepaint(false);
		// clusterList.setVisibleRowCount(Math.min(16, clustering.numClusters()
		// + 1));

		// clusterList.revalidate();
		// scroll.getViewport().revalidate();
		// scroll.revalidate();
		// scroll.repaint();

		revalidate();

		// System.out.println(">> " + Math.min(16, clustering.numClusters() +
		// 1));

		// clusterList.repaint();
	}

	private void buildLayout()
	{
		datasetNameLabel = ComponentFactory.createLabel();

		listModel = new DefaultListModel();
		clusterList = new MouseOverList(listModel);
		clusterList.setClearOnExit(false);
		clusterList.setFocusable(false);

		clusterList.setBackground(Settings.TRANSPARENT_BACKGROUND);
		// clusterList.setOpaque(false);

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
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setOpaque(isSelected || i == clustering.getClusterActive().getSelected());

				setForeground(Settings.FOREGROUND);
				if (i == clustering.getClusterActive().getSelected())
				{
					setBackground(Settings.LIST_ACTIVE_BACKGROUND);
					setForeground(Settings.LIST_SELECTION_FOREGROUND);
				}
				else if (isSelected)
				{
					setBackground(Settings.LIST_WATCH_BACKGROUND);
					setForeground(Settings.LIST_SELECTION_FOREGROUND);
				}
				return this;
			}
		});

		// add(clusterList, BorderLayout.WEST);

		// clusterList.setBorder(new
		// CompoundBorder(ComponentFactory.createThinBorder(), new
		// EmptyBorder(5, 5, 5, 5)));
		clusterList.setBorder(new EmptyBorder(5, 5, 5, 5));

		infoPanel = new ModelListPanel(clustering, viewControler);
		//		SwingUtil.setDebugBorder(infoPanel);

		// setLayout(new GridLayout(2, 1));
		// add(clusterList);
		// add(infoPanel);

		FormLayout layout = new FormLayout("left:pref:grow",
				"pref, 5, fill:pref:grow, 15, fill:pref:grow, 10, pref, 0, pref");

		JPanel panel = this;
		setLayout(layout);
		// PanelBuilder panel = new PanelBuilder(layout, new FormDebugPanel());

		CellConstraints cc = new CellConstraints();
		int lineCount = 1;

		scroll = new JScrollPane(clusterList);
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);

		datasetNameLabel.setBorder(new EmptyBorder(0, 5, 0, 0));
		panel.add(datasetNameLabel, cc.xy(1, lineCount));
		lineCount += 2;
		panel.add(scroll, cc.xy(1, lineCount));
		lineCount += 2;
		panel.add(infoPanel, cc.xy(1, lineCount));
		lineCount += 2;
		panel.add(new ControlPanel(viewControler), cc.xy(1, lineCount));
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
