package gui;

import gui.swing.ComponentFactory;
import gui.swing.TransparentViewPanel;
import gui.util.CompoundPropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
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
import cluster.Compound;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ConstantSize;
import com.jgoodies.forms.layout.FormLayout;
import com.lowagie.text.Font;

import dataInterface.CompoundPropertyUtil;

public class CompoundListPanel extends TransparentViewPanel
{
	JLabel clusterNameVal;
	JLabel clusterNumVal;

	JScrollPane listScrollPane;
	MouseOverList list;
	DefaultListModel listModel;
	DoubleNameListCellRenderer listRenderer;

	boolean selfBlock = false;

	Clustering clustering;
	ClusterController clusterControler;
	ViewControler viewControler;
	GUIControler guiControler;

	public CompoundListPanel(Clustering clustering, ClusterController clusterControler, ViewControler controler,
			GUIControler guiControler)
	{
		this.clustering = clustering;
		this.clusterControler = clusterControler;
		this.viewControler = controler;
		this.guiControler = guiControler;

		buildLayout();

		update(null, false);
		installListeners(controler);
	}

	private void installListeners(final ViewControler controler)
	{
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ClusteringImpl.CLUSTER_MODIFIED))
				{
					update(clustering.getActiveCluster(), false);
				}
			}
		});

		clustering.addSelectionListener(new SelectionListener()
		{

			@Override
			public void compoundWatchedChanged(Compound[] c)
			{
				if (selfBlock)
					return;
				selfBlock = true;
				if (c.length == 0)
					list.clearSelection();
				else
					list.setSelectedValue(c[0], true);
				selfBlock = false;
			}

			@Override
			public void compoundActiveChanged(Compound[] c)
			{
				if (selfBlock)
					return;
				selfBlock = true;

				updateActiveCompoundSelection();
				selfBlock = false;
			}

			@Override
			public void clusterWatchedChanged(Cluster c)
			{
				updateCluster(c, false);
			}

			@Override
			public void clusterActiveChanged(Cluster c)
			{
				updateCluster(c, true);
			}
		});

		list.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(final MouseEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;

				Thread th = new Thread(new Runnable()
				{
					public void run()
					{
						int idx = list.getLastSelectedIndex();
						Compound m = (Compound) listModel.elementAt(idx);
						if (m == null)
							throw new IllegalStateException();
						if (e.isControlDown())
							clusterControler.toggleCompoundActive(m);
						else
						{
							if (clustering.isCompoundActive(m))
								clusterControler.clearCompoundActive(true);
							else
								clusterControler.setCompoundActive(m, true);
						}
						selfBlock = false;
					}
				});
				th.start();
			}
		});

		list.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;
				int index = list.getSelectedIndex();
				if (index == -1)
				{
					//compoundWatched.clearSelection();
				}
				else
					clusterControler.setCompoundWatched((Compound) listModel.elementAt(index));
				selfBlock = false;
			}
		});

		controler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_DESCRIPTOR_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_FEATURE_SORTING_CHANGED)
						|| evt.getPropertyName().equals(ViewControler.PROPERTY_COMPOUND_FILTER_CHANGED))
				{
					update(clustering.getActiveCluster(), false);
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
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

	private void updateCluster(Cluster c, boolean active)
	{
		selfBlock = true;

		if (active)
		{
			update(c, false);
			//			hideUnselectedCheckBox.setSelected(controler.isHideUnselected());
		}
		else
		{
			// only cluster watch updates if no cluster is active
			if (!clustering.isClusterActive())
				update(c, true);
		}
		selfBlock = false;
	}

	private void buildLayout()
	{
		FormLayout layout = new FormLayout("pref");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setLineGapSize(new ConstantSize(2, ConstantSize.PX));

		clusterNameVal = ComponentFactory.createViewLabel();
		clusterNumVal = ComponentFactory.createViewLabel();

		listModel = new DefaultListModel();

		list = new MouseOverList(listModel);
		list.setClearOnExit(false);

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
					list.setFixedCellHeight(getRowHeight());
				}
			}

			public Component getListCellRendererComponent(JList list, Object value, int i, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, i, isSelected, cellHasFocus);

				Compound c = (Compound) value;
				setOpaque(isSelected || clustering.isCompoundActive(c));

				setForeground(ComponentFactory.FOREGROUND);
				if (clustering.isCompoundActive(c))
				{
					setBackground(ComponentFactory.LIST_ACTIVE_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (isSelected)
				{
					setBackground(ComponentFactory.LIST_WATCH_BACKGROUND);
					setForeground(ComponentFactory.LIST_SELECTION_FOREGROUND);
				}
				else if (c.getHighlightColorString() != null
						&& c.getHighlightColor() != CompoundPropertyUtil.getNullValueColor())
				{
					setForegroundLabel2(c.getHighlightColor());
				}
				return this;
			}

		};
		listRenderer.setFontLabel2(listRenderer.getFontLabel2().deriveFont(Font.ITALIC));
		list.setCellRenderer(listRenderer);

		list.setOpaque(false);
		list.setFocusable(false);
		//		list.setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel p = new JPanel(new FormLayout("fill:pref:grow", "fill:p:grow,p"));//,5px,p,p"));
		p.setOpaque(false);
		CellConstraints cc = new CellConstraints();

		listScrollPane = ComponentFactory.createViewScrollpane(list);
		p.add(listScrollPane, cc.xy(1, 1));

		checkBoxContainer = new JPanel(new BorderLayout());
		checkBoxContainer.setOpaque(false);
		checkBoxContainer.setBorder(new EmptyBorder(5, 0, 0, 0));
		checkBoxContainer.setVisible(false);
		checkBoxContainer.addContainerListener(new ContainerListener()
		{
			@Override
			public void componentRemoved(ContainerEvent e)
			{
				checkBoxContainer.setVisible(false);
			}

			@Override
			public void componentAdded(ContainerEvent e)
			{
				checkBoxContainer.setVisible(true);
			}
		});
		p.add(checkBoxContainer, cc.xy(1, 2));//4));
		setLayout(new BorderLayout());

		add(p);
	}

	JPanel checkBoxContainer;

	public void appendCheckbox(JCheckBox superimposeCheckBox)
	{
		checkBoxContainer.add(superimposeCheckBox, BorderLayout.NORTH);
	}

	private void updateActiveCompoundSelection()
	{
		if (clustering.isCompoundActive())
			list.setSelectedValue(clustering.getActiveCompounds()[0], true);
		else
			list.clearSelection();
	}

	private void update(Cluster c, boolean noList)
	{
		if (!SwingUtilities.isEventDispatchThread())
			throw new IllegalStateException("GUI updates only in event dispatch thread plz");

		setVisible(true);

		setIgnoreRepaint(true);
		if (c == null)
		{
			clusterNameVal.setText(" ");
			clusterNumVal.setText(" ");
			listScrollPane.setVisible(false);
		}
		else
		{
			listModel.removeAllElements();

			if (noList)
			{
				listScrollPane.setVisible(false);
			}
			else
			{
				clusterNameVal.setText(c.toString());
				clusterNumVal.setText(" ");
				listScrollPane.setPreferredSize(null);

				Compound m[] = new Compound[c.getCompounds().size()];
				int i = 0;
				for (Compound mod : c.getCompounds())
				{
					m[i++] = mod;
					if (mod.getDisplayName() == null)
						throw new IllegalStateException("display name for compound is nil, check order of listeners");
				}
				for (Compound comp : c.getCompounds())
					comp.setFeatureSortingEnabled(true);//viewControler.isFeatureSortingEnabled());
				Arrays.sort(m);
				for (Compound compound : m)
					listModel.addElement(compound);
				updateActiveCompoundSelection();
				updateListSize();
				listScrollPane.setVisible(true);
			}
		}
		setIgnoreRepaint(false);
		repaint();
	}

	private void updateListSize()
	{
		int rowCount = (guiControler.getComponentMaxHeight(1) / listRenderer.getRowHeight()) / 3;

		double ratioVisible = rowCount / (double) listModel.getSize();
		if (ratioVisible <= 0.5)
		{
			// if less then 50% of elements is visible increase by up to 50%
			double ratioIncrease = 0.5 - ratioVisible;
			rowCount += (int) (ratioIncrease * rowCount);
		}

		list.setVisibleRowCount(rowCount);

		if (viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)
		{
			// features values are shown on the right, restrict long compound names to show feature values without scroll pane
			listRenderer.setMaxl1Width(guiControler.getComponentMaxWidth(0.15));
		}
		else
		{
			// no features values are shown on the right -> long names can be written out, scroll-pane will show up
			listRenderer.setMaxl1Width(Integer.MAX_VALUE);
		}

		listScrollPane.setPreferredSize(null);
		listScrollPane.setPreferredSize(new Dimension(Math.min(guiControler.getComponentMaxWidth(0.2),
				listScrollPane.getPreferredSize().width), listScrollPane.getPreferredSize().height));
		listScrollPane.revalidate();
		listScrollPane.repaint();
	}
}
