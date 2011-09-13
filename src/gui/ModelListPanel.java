package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Settings;
import util.SelectionModel;
import cluster.Cluster;
import cluster.Clustering;
import cluster.Model;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.ConstantSize;
import com.jgoodies.forms.layout.FormLayout;

public class ModelListPanel extends JPanel
{
	SelectionModel clusterActive;
	SelectionModel clusterWatched;
	SelectionModel modelActive;
	SelectionModel modelWatched;
	JLabel clusterNameVal;
	JLabel clusterNumVal;
	JCheckBox superimposeCheckBox;
	JCheckBox hideUnselectedCheckBox;

	JScrollPane listScrollPane;
	MouseOverList list;
	DefaultListModel listModel;

	boolean selfBlock = false;

	Clustering clustering;
	ViewControler controler;

	public ModelListPanel(Clustering clustering, ViewControler controler)
	{
		this.clustering = clustering;
		this.controler = controler;

		this.clusterActive = clustering.getClusterActive();
		this.clusterWatched = clustering.getClusterWatched();
		this.modelActive = clustering.getModelActive();
		this.modelWatched = clustering.getModelWatched();

		buildLayout();

		update(-1, false);
		installListeners(controler);
	}

	private void installListeners(final ViewControler controler)
	{
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(Clustering.CLUSTER_MODIFIED))
				{
					update(clusterActive.getSelected(), false);
				}
			}
		});

		clusterActive.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateCluster(clusterActive.getSelected(), true);
			}
		});

		clusterWatched.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateCluster(clusterWatched.getSelected(), false);
			}
		});

		modelActive.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;

				//updateCheckboxSelection();
				updateSelectedModel();
				selfBlock = false;

			}
		});
		modelWatched.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;
				if (modelWatched.getSelected() == -1)
					list.clearSelection();
				else
					list.setSelectedValue(modelWatched.getSelected(), true);
				selfBlock = false;
			}
		});

		superimposeCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfBlock)
					return;
				controler.setSuperimpose(superimposeCheckBox.isSelected());

				// hack listen to controler instead
				selfBlock = true;
				hideUnselectedCheckBox.setSelected(controler.isHideUnselected());
				selfBlock = false;
			}
		});

		hideUnselectedCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfBlock)
					return;
				controler.setHideUnselected(hideUnselectedCheckBox.isSelected());
			}
		});

		list.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;

				int m = getModelIndexFromList(list.getLastSelectedIndex());
				if (m < 0)
					throw new IllegalStateException();
				if (e.isControlDown())
					modelActive.setSelectedInverted(m);
				else
					modelActive.setSelected(m);
				modelWatched.clearSelection();
				selfBlock = false;
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
					//modelWatched.clearSelection();
				}
				else
					modelWatched.setSelected(getModelIndexFromList(index));
				selfBlock = false;
			}
		});

	}

	private void updateCluster(int index, boolean active)
	{
		selfBlock = true;

		if (active)
		{
			update(index, false);
			// moveing out of cluster -> deselct box
			superimposeCheckBox.setSelected(false);
			hideUnselectedCheckBox.setSelected(controler.isHideUnselected());
		}
		else
		{
			// only cluster watch updates if no cluster is active
			if (clusterActive.getSelected() == -1)
				update(index, true);
		}

		selfBlock = false;
	}

	private int getModelIndexFromList(int listIndex)
	{
		if (listIndex == -1)
			return -1;
		Integer val = (Integer) listModel.getElementAt(listIndex);
		if (val == null)
			return -1;
		else
			return val;
	}

	private int getListIndexFromModel(int modelIndex)
	{
		return listModel.indexOf(modelIndex);
	}

	private void buildLayout()
	{
		FormLayout layout = new FormLayout("pref:grow, 4dlu, pref");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setLineGapSize(new ConstantSize(2, ConstantSize.PX));

		builder.append(clusterNameVal = ComponentFactory.createLabel(), 3);
		builder.nextLine();
		builder.append(clusterNumVal = ComponentFactory.createLabel());
		builder.nextLine();

		superimposeCheckBox = ComponentFactory.createCheckBox("Superimpose compounds");
		superimposeCheckBox.setOpaque(false);
		builder.append(superimposeCheckBox, 3);
		builder.nextLine();

		listModel = new DefaultListModel();

		list = new MouseOverList(listModel);
		list.setClearOnExit(false);

		list.setOpaque(false);
		list.setCellRenderer(new DefaultListCellRenderer()
		{
			public Component getListCellRendererComponent(JList list, Object value, int i, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, i, isSelected, cellHasFocus);
				int model = getModelIndexFromList(i);
				setOpaque(isSelected || modelActive.isSelected(model));

				setForeground(Settings.FOREGROUND);
				if (modelActive.isSelected(model))
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

		list.setOpaque(false);
		list.setFocusable(false);

		builder.appendRow("fill:pref:grow");
		//builder.append(listPanel, 3);
		listScrollPane = ComponentFactory.createScrollpane(list);
		builder.append(listScrollPane, 3);
		builder.nextLine();

		hideUnselectedCheckBox = ComponentFactory.createCheckBox("Hide not-selected compounds");
		hideUnselectedCheckBox.setOpaque(false);
		builder.append(hideUnselectedCheckBox, 3);
		builder.nextLine();

		builder.getPanel().setOpaque(false);

		setLayout(new BorderLayout());
		add(builder.getPanel());

		// setOpaque(false);
		setBackground(Settings.TRANSPARENT_BACKGROUND);
		// setFocusable(false);
	}

	private void updateSelectedModel()
	{
		list.setSelectedIndex(getListIndexFromModel(modelActive.getSelected()));
	}

	private void update(int index, boolean noList)
	{
		// setVisible(index != -1);
		setVisible(true);

		setIgnoreRepaint(true);
		if (index == -1)
		{
			clusterNameVal.setText(" ");
			clusterNumVal.setText(" ");
			superimposeCheckBox.setVisible(false);
			hideUnselectedCheckBox.setVisible(false);

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
				Cluster c = clustering.getCluster(index);
				clusterNameVal.setText(c.getName());
				clusterNumVal.setText("Num compounds: " + c.size());

				superimposeCheckBox.setVisible(true);
				hideUnselectedCheckBox.setVisible(true);

				for (Model m : c.getModels())
				{
					listModel.addElement(m.getModelIndex());
				}
				updateSelectedModel();
				listScrollPane.setVisible(true);
			}
		}
		setIgnoreRepaint(false);
		repaint();
	}
}
