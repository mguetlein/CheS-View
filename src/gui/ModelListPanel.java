package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
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
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Settings;
import util.SelectionModel;
import cluster.Cluster;
import cluster.Clustering;
import cluster.Model;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
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
	//	JCheckBox hideUnselectedCheckBox;

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
		//		controler.addViewListener(new PropertyChangeListener()
		//		{
		//			@Override
		//			public void propertyChange(PropertyChangeEvent evt)
		//			{
		//				if (evt.equals(ViewControler.PROPERTY_SUPERIMPOSE_CHANGED))
		//				{
		//					selfBlock = true;
		//					hideUnselectedCheckBox.setSelected(controler.isHideUnselected());
		//					selfBlock = false;
		//				}
		//			}
		//		});

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
				updateActiveModelSelection();
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
					list.setSelectedValue(clustering.getModelWithModelIndex(modelWatched.getSelected()), true);
				selfBlock = false;
			}
		});

		//		hideUnselectedCheckBox.addActionListener(new ActionListener()
		//		{
		//			@Override
		//			public void actionPerformed(ActionEvent e)
		//			{
		//				if (selfBlock)
		//					return;
		//				controler.setHideUnselected(hideUnselectedCheckBox.isSelected());
		//			}
		//		});

		list.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (selfBlock)
					return;
				selfBlock = true;

				Model m = (Model) listModel.elementAt(list.getLastSelectedIndex());
				if (m == null)
					throw new IllegalStateException();
				if (e.isControlDown())
					modelActive.setSelectedInverted(m.getModelIndex());
				else
				{
					if (modelActive.isSelected(m.getModelIndex()))
						modelActive.clearSelection();
					else
						modelActive.setSelected(m.getModelIndex());
				}
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
					modelWatched.setSelected(((Model) listModel.elementAt(index)).getModelIndex());
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
			//			hideUnselectedCheckBox.setSelected(controler.isHideUnselected());
		}
		else
		{
			// only cluster watch updates if no cluster is active
			if (clusterActive.getSelected() == -1)
				update(index, true);
		}
		selfBlock = false;
	}

	private void buildLayout()
	{
		FormLayout layout = new FormLayout("pref");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setLineGapSize(new ConstantSize(2, ConstantSize.PX));

		clusterNameVal = ComponentFactory.createLabel();
		clusterNumVal = ComponentFactory.createLabel();

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

				int modelIndex = ((Model) value).getModelIndex();
				setOpaque(isSelected || modelActive.isSelected(modelIndex));

				setForeground(Settings.FOREGROUND);
				if (modelActive.isSelected(modelIndex))
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
		list.setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel p = new JPanel(new FormLayout("fill:pref:grow", "fill:p:grow,p"));//,5px,p,p"));
		p.setOpaque(false);
		CellConstraints cc = new CellConstraints();

		//builder.append(listPanel, 3);
		listScrollPane = ComponentFactory.createScrollpane(list);
		p.add(listScrollPane, cc.xy(1, 1));

		//		hideUnselectedCheckBox = ComponentFactory.createCheckBox("Hide not-selected");
		//		hideUnselectedCheckBox.setOpaque(false);
		//		p.add(hideUnselectedCheckBox, cc.xy(1, 3));

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
		// setOpaque(false);
		setBackground(Settings.TRANSPARENT_BACKGROUND);
		// setFocusable(false);
	}

	JPanel checkBoxContainer;

	public void appendCheckbox(JCheckBox superimposeCheckBox)
	{
		checkBoxContainer.add(superimposeCheckBox, BorderLayout.NORTH);
	}

	private void updateActiveModelSelection()
	{
		list.setSelectedValue(clustering.getModelWithModelIndex(modelActive.getSelected()), true);
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
			//			hideUnselectedCheckBox.setVisible(false);

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
				clusterNameVal.setText(c.toString());
				clusterNumVal.setText(" ");

				//				hideUnselectedCheckBox.setVisible(true);

				for (Model m : c.getModels())
				{
					listModel.addElement(m);
				}
				updateActiveModelSelection();
				listScrollPane.setVisible(true);
			}
		}
		setIgnoreRepaint(false);
		repaint();
	}

}
