package gui;

import gui.MouseOverCheckBoxList.CheckboxCellRenderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import main.Settings;

import org.jmol.viewer.Viewer;

import util.ArrayUtil;
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

	// MouseOverList modelList;
	// DefaultListModel listModel;
	// JScrollPane scrollPane;

	MouseOverCheckBoxList list;
	DefaultListModel listModel;
	MouseOverCheckBoxListComponent listPanel;

	boolean selfBlock = false;

	Viewer viewer;
	Clustering clustering;

	public ModelListPanel(Clustering clustering, Viewer viewer, ViewControler controler)
	{
		this.viewer = viewer;
		this.clustering = clustering;

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
				updateCheckboxSelection();
				releaseBlock();

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
				releaseBlock();
			}
		});

		superimposeCheckBox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				controler.setSuperimpose(superimposeCheckBox.isSelected());
			}
		});

		list.getCheckBoxSelection().addListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				System.out.println("checkbox selection: \n" + ArrayUtil.toString((int[]) evt.getNewValue()));

				if (selfBlock)
				{
					System.out.println("BLOCKED");
					return;
				}
				selfBlock = true;

				boolean clearWatched = false;

				HashMap<Integer, Boolean> selection = new HashMap<Integer, Boolean>();
				int oldSelection[] = (int[]) evt.getOldValue();
				for (int i : oldSelection)
				{
					if (list.isSelectedIndex(i))
						clearWatched = true;
					selection.put(i, false); // unless ovewritten via new values
				}
				int newSelection[] = (int[]) evt.getNewValue();
				for (int i : newSelection)
				{
					if (list.isSelectedIndex(i))
						clearWatched = false;
					selection.put(i, true);
				}

				int indices[] = new int[selection.keySet().size()];
				boolean selected[] = new boolean[indices.length];
				int count = 0;
				for (Integer index : selection.keySet())
				{
					indices[count] = getModelFromList(index);
					selected[count] = selection.get(index);
					count++;
				}

				// int[] indices = list.getSelectedIndices();
				// for (int i = 0; i < indices.length; i++)
				// indices[i] = getModelFromList(indices[i]);

				System.out.println("checkbox - add/remove model selection for inidices: \n"
						+ ArrayUtil.toString(indices) + " \n" + ArrayUtil.toString(selected));

				if (clearWatched)
				{
					//// do manually because blocked
					//list.clearSelection();
					//modelWatched.clearSelection();
				}
				modelActive.addRemoveSelectedIndices(indices, selected);

				releaseBlock();

				// ModelListPanel.this.mainPanel.getCluster().getModelSelection().setWatched(getListSelectedModel());
				// releaseBlock();
				// }
				// });
				// modelList.addMouseListener(new MouseAdapter()
				// {
				// public void mouseClicked(MouseEvent e)
				// {
				// if (selfBlock)
				// return;
				// selfBlock = true;
				// // ModelListPanel.this.mainPanel.getCluster().getModelSelection().setWatched(-1);
				// ModelListPanel.this.mainPanel.getCluster().getModelSelection().setSelected(getListSelectedModel());
				// releaseBlock();
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
					modelWatched.setSelected(getModelFromList(index));
				releaseBlock();

				// ModelListPanel.this.mainPanel.getCluster().getModelSelection().setWatched(getListSelectedModel());
				// releaseBlock();
				// }
				// });
				// modelList.addMouseListener(new MouseAdapter()
				// {
				// public void mouseClicked(MouseEvent e)
				// {
				// if (selfBlock)
				// return;
				// selfBlock = true;
				// // ModelListPanel.this.mainPanel.getCluster().getModelSelection().setWatched(-1);
				// ModelListPanel.this.mainPanel.getCluster().getModelSelection().setSelected(getListSelectedModel());
				// releaseBlock();
			}
		});

	}

	private void updateCluster(int index, boolean active)
	{
		selfBlock = true;

		if (active)
			update(index, false);
		else
		{
			// only cluster watch updates if no cluster is active
			if (clusterActive.getSelected() == -1)
				update(index, true);
		}

		releaseBlock();
	}

	private void releaseBlock()
	{
		clustering.invokeAfterViewer(new Runnable()
		{
			@Override
			public void run()
			{
				selfBlock = false;
			}
		});
	}

	private int getModelFromList(int listIndex)
	{
		if (listIndex == -1)
			return -1;
		Integer val = (Integer) listModel.getElementAt(listIndex);
		if (val == null)
			return -1;
		else
			return val;
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
		list = new MouseOverCheckBoxList(listModel);
		list.setClearOnExit(false);
		listPanel = new MouseOverCheckBoxListComponent(list);
		listPanel.getSelectAllCheckBox().setForeground(Settings.FOREGROUND);

		// listModel = new DefaultListModel();
		// modelList = new MouseOverList(listModel);
		// modelList.setFocusable(false);
		// // modelList.setBackground(new Color(0, 0, 0, 200));

		// ((CheckboxCellRenderer) list.getCellRenderer()).setOpaque(false);

		list.setCellRenderer(new CheckboxCellRenderer()
		{
			// Color selectionColor = UIManager.getColor("List.selectionBackground");
			// Color watchColor = new Color(selectionColor.getRed(), selectionColor.getGreen(), selectionColor.getBlue(),
			// 100);

			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, viewer.getModelName((Integer) value), index, isSelected,
						cellHasFocus);
				setForeground(Settings.FOREGROUND);
				if (isSelected) // selected == watched //modelWatched.isSelected(getModelFromList(index)))
				{
					setForeground(Settings.LIST_SELECTION_FOREGROUND);
					setBackground(Settings.LIST_WATCH_BACKGROUND);
					setOpaque(true);
				}
				else
					setOpaque(false);
				return this;
			}
		});

		// public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		// {
		// int val = (Integer) value;
		// if (val == -1)
		// value = "None";
		// // else
		// // value = mainPanel.getCluster().getClusterName((Integer) value);
		//
		// JComponent res = (JComponent) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		// res.setOpaque(isSelected || val == mainPanel.getCluster().getModelSelection().getSelected());
		//
		// if (val == mainPanel.getCluster().getModelSelection().getSelected())
		// {
		// res.setBackground(selectionColor);
		// res.setForeground(UIManager.getColor("List.selectionForeground"));
		// }
		// else if (isSelected)
		// res.setBackground(watchColor);
		//
		// return res;
		// }
		// });

		list.setOpaque(false);
		list.setFocusable(false);
		listPanel.getSelectAllCheckBox().setOpaque(false);
		listPanel.getSelectAllCheckBox().setFocusable(false);
		listPanel.getScrollPane().setOpaque(false);
		listPanel.getScrollPane().getViewport().setOpaque(false);
		listPanel.setOpaque(false);

		builder.appendRow("fill:pref:grow");
		builder.append(listPanel, 3);
		builder.nextLine();

		// modelList.setBorder(null);
		// scrollPane.setBorder(new CompoundBorder(new LineBorder(UIManager.getColor("Label.foreground")), new EmptyBorder(5, 5, 5, 5)));

		// builder.getPanel().setBackground(new Color(0, 0, 0, 200));
		// builder.getPanel().setBorder(
		// new CompoundBorder(new MatteBorder(1, 0, 0, 0, UIManager.getColor("Label.foreground")), new EmptyBorder(10, 10, 10, 10)));

		builder.getPanel().setOpaque(false);

		setLayout(new BorderLayout());
		add(builder.getPanel());

		// setOpaque(false);
		setBackground(Settings.TRANSPARENT_BACKGROUND);

		// setFocusable(false);
	}

	private void updateCheckboxSelection()
	{
		int[] sel = modelActive.getSelectedIndices();

		Vector<Integer> checkBoxIndices = new Vector<Integer>();
		for (int modelIndex : sel)
			if (listModel.contains(modelIndex))
				checkBoxIndices.add(listModel.indexOf(modelIndex));

		list.getCheckBoxSelection().setSelectedIndices(ArrayUtil.toPrimitiveIntArray(checkBoxIndices));
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
			listPanel.setVisible(false);
		}
		else
		{
			//			Cluster c = clustering.getCluster(index);
			//			clusterNameVal.setText(c.getName());
			//			clusterNumVal.setText("Num molecules: " + c.size());

			listModel.removeAllElements();

			if (noList)
			{
				listPanel.setVisible(false);
			}
			else
			{
				Cluster c = clustering.getCluster(index);
				clusterNameVal.setText(c.getName());
				clusterNumVal.setText("Num molecules: " + c.size());

				superimposeCheckBox.setVisible(true);
				//superimposeCheckBox.setSelected(false);

				// List<Integer> models = mainPanel.getCluster().getModelsForCluster(index);
				// listModel.addElement(-1);
				// System.out.print("new models ");

				for (Model m : c.getModels())
				{
					listModel.addElement(m.getModelIndex());
				}
				// list.setVisibleRowCount(Math.min(15, listModel.getSize()));

				updateCheckboxSelection();
				// list.getCheckBoxSelection().setSelectedIndices(modelActive.getSelectedIndices(), true);
				// list.setCheckboxSelectionAll(true);
				listPanel.setVisible(true);
			}
			// System.out.println();
		}
		setIgnoreRepaint(false);
		repaint();
	}
}
