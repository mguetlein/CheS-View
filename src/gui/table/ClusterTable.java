package gui.table;

import gui.ClickMouseOverTable;
import gui.ClickMouseOverTable.ClickMouseOverRenderer;
import gui.MainPanel;
import gui.ViewControler;
import gui.swing.ComponentFactory;

import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import util.SelectionModel;
import util.StringUtil;
import cluster.Cluster;
import cluster.Clustering;
import dataInterface.CompoundProperty;

public class ClusterTable extends CCDataTable
{
	public ClusterTable(final ViewControler viewControler, final Clustering clustering)
	{
		super(viewControler, clustering);
	}

	@Override
	protected JTable createTable()
	{
		table = new ClickMouseOverTable(tableModel)
		{
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
			{
				Component c = super.prepareRenderer(renderer, row, column);
				if (c instanceof JComponent)
				{
					JComponent jc = (JComponent) c;
					jc.setToolTipText(getValueAt(row, 1).toString());
				}
				return c;
			}
		};
		sorter = new TableRowSorter<DefaultTableModel>();
		table.setRowSorter(sorter);
		sorter.setModel(tableModel);

		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		int count = 0;
		for (Cluster c : clustering.getClusters())
		{
			Object o[] = new Object[tableModel.getColumnCount()];
			int i = 0;
			o[i++] = ++count;
			o[i++] = c;
			o[i++] = c.size();
			if (i != nonPropColumns)
				throw new Error();
			for (CompoundProperty p : props)
				if (p.getType() == CompoundProperty.Type.NUMERIC)
					o[i++] = c.getDoubleValue(p);
				else
					o[i++] = c.getStringValue(p);
			tableModel.addRow(o);
		}

		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (!isVisible())
					return;
				updateTableFromSelection(clustering.getClusterActive(), null, table.getClickSelectionModel());
			}
		});
		clustering.getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (!isVisible())
					return;
				updateTableFromSelection(clustering.getClusterWatched(), table.getSelectionModel(), null);
			}
		});

		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (!isVisible())
					return;
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
					table.repaint();
			}
		});

		table.getClickSelectionModel().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateClusterFromTable(table.getClickSelectionModel().getSelected(), clustering.getClusterActive());
			}
		});

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting())
					return;
				updateClusterFromTable(table.getSelectedRow(), clustering.getClusterWatched());
			}
		});
		ClickMouseOverRenderer renderer = new ClickMouseOverTable.ClickMouseOverRenderer(table)
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				Object val;
				//Compound m = ((Compound) table.getValueAt(sorter.convertRowIndexToView(row), 1));
				Cluster c = ((Cluster) table.getValueAt(row, 1));
				//				System.out.println(row + " " + m);
				CompoundProperty p = null;

				if (column >= nonPropColumns)
				{
					p = props.get(column - nonPropColumns);
					val = c.getFormattedValue(p) + " (" + StringUtil.formatDouble(clustering.getSpecificity(c, p))
							+ ")";
				}
				else
					val = value;
				Component comp = super.getTableCellRendererComponent(table, val, isSelected, hasFocus, row, column);

				if (column >= nonPropColumns)
				{
					Color col = MainPanel.getHighlightColor(clustering, c, p, true);
					setForeground(col);
				}
				else
				{
					setForeground(Color.BLACK);
				}
				return comp;
			}
		};
		renderer.clickSelectedBackground = ComponentFactory.LIST_ACTIVE_BACKGROUND_WHITE;
		renderer.mouseOverSelectedBackground = ComponentFactory.LIST_WATCH_BACKGROUND_WHITE;

		//		renderer.clickSelectedBackground = ComponentFactory.LIST_ACTIVE_BACKGROUND;
		//		renderer.mouseOverSelectedBackground = ComponentFactory.LIST_WATCH_BACKGROUND;
		//		renderer.background = ComponentFactory.BACKGROUND;
		//		renderer.clickSelectedForeground = ComponentFactory.LIST_SELECTION_FOREGROUND;
		//		renderer.mouseOverSelectedForeground = ComponentFactory.LIST_SELECTION_FOREGROUND;
		//		renderer.foreground = ComponentFactory.FOREGROUND;
		table.setDefaultRenderer(Object.class, renderer);
		table.setDefaultRenderer(Integer.class, renderer);
		table.setDefaultRenderer(Double.class, renderer);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setGridColor(ComponentFactory.BACKGROUND);

		updateTableFromSelection(clustering.getClusterWatched(), table.getSelectionModel(), null);
		updateTableFromSelection(clustering.getClusterActive(), null, table.getClickSelectionModel());
		updateFeatureSelection();

		return table;
	}

	@Override
	protected boolean addSpecificityInfo()
	{
		return true;
	}

	@Override
	protected String getItemName()
	{
		return "cluster";
	}

	@Override
	public String getExtraColumn()
	{
		return "Size";
	}

	@Override
	public boolean addEmbeddingQuality()
	{
		return false;
	}

	@Override
	protected String getShortName()
	{
		return "cluster-table";
	}

	private void updateClusterFromTable(int tableSelection, SelectionModel modelSelection)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;
		final int row = tableSelection;
		if (row == -1)
			modelSelection.clearSelection();
		else
			//if (row.length == 1)
			modelSelection.setSelected(sorter.convertRowIndexToModel(row));
		//		else
		//		{
		//			View.instance.suspendAnimation("manual zooming out");
		//			block("waiting for anim");
		//			for (int i = 0; i < row.length; i++)
		//				row[i] = sorter.convertRowIndexToModel(row[i]);
		//			modelSelection.setSelectedIndices(row);
		//			Thread th = new Thread(new Runnable()
		//			{
		//				public void run()
		//				{
		//					ThreadUtil.sleep(300);
		//					View.instance.afterAnimation(new Runnable()
		//					{
		//						@Override
		//						public void run()
		//						{
		//							View.instance.proceedAnimation("manual zooming out");
		//							if (View.instance.getZoomTarget() != clustering)
		//								View.instance.zoomTo(clustering, AnimationSpeed.SLOW, false);
		//							View.instance.afterAnimation(new Runnable()
		//							{
		//
		//								@Override
		//								public void run()
		//								{
		//									unblock("waiting for anim");
		//								}
		//							}, "wait for anim");
		//						}
		//					}, "manual zooming out");
		//				}
		//			});
		//			th.start();
		//		}
		selfUpdate = false;
	}

}
