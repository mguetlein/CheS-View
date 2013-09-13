package gui.table;

import gui.ViewControler;
import gui.util.CompoundPropertyHighlighter;
import gui.util.Highlighter;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import util.CountedSet;
import util.DoubleArraySummary;
import util.StringUtil;
import cluster.Clustering;
import dataInterface.CompoundProperty;

public class FeatureTable extends DataTable
{
	List<Highlighter> highlighters;

	public FeatureTable(final ViewControler viewControler, Clustering clustering)
	{
		super(viewControler, clustering);
	}

	protected JTable createTable()
	{
		table = new JTable(tableModel);
		sorter = new TableRowSorter<DefaultTableModel>();
		table.setRowSorter(sorter);
		sorter.setModel(tableModel);

		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				if (selfUpdate)
					return;

				int row = table.getSelectedRow();
				if (row != -1)
				{
					row = sorter.convertRowIndexToModel(row);
					Highlighter p = (Highlighter) tableModel.getValueAt(row, 1);
					viewControler.setHighlighter(p);
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							table.requestFocus();
						}
					});
				}
			}
		});
		table.setDefaultRenderer(Double.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				return super.getTableCellRendererComponent(table,
						value instanceof Double ? StringUtil.formatDouble((Double) value) : value, isSelected,
						hasFocus, row, column);
			}
		});
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (selfUpdate)
					return;
				if (!FeatureTable.this.isVisible())
					return;
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
					updateFeatureSelection();
			}
		});
		//		for (Dimensions d : Dimensions.values())
		//			model.addColumn(d + " Emb-Qual");
		highlighters = new ArrayList<Highlighter>();
		for (Highlighter[] hh : viewControler.getHighlighters().values())
			for (Highlighter h : hh)
				highlighters.add(h);

		//		final DoubleKeyHashMap<Dimensions, CompoundProperty, CompoundPropertyEmbedQuality> embMap = new DoubleKeyHashMap<EmbedUtil.Dimensions, CompoundProperty, EmbedUtil.CompoundPropertyEmbedQuality>();
		int count = 0;
		for (Highlighter h : highlighters)
		{
			Object o[] = new Object[11];// + Dimensions.values().length];
			int i = 0;
			o[i++] = ++count;
			o[i++] = h;
			if (h instanceof CompoundPropertyHighlighter)
			{
				CompoundProperty p = ((CompoundPropertyHighlighter) h).getProperty();
				o[i++] = (clustering.getFeatures().contains(p) ? "Yes" : "no");
				o[i++] = p.getType() == CompoundProperty.Type.NUMERIC ? "Numeric"
						: (p.getType() == CompoundProperty.Type.NOMINAL ? "Nominal" : "undef.");
				o[i++] = clustering.numMissingValues(p);
				o[i++] = clustering.numDistinctValues(p);
				if (p.getType() == CompoundProperty.Type.NUMERIC)
				{
					DoubleArraySummary s = DoubleArraySummary.create(clustering.getDoubleValues(p));
					o[i++] = Double.isNaN(s.getMin()) ? null : s.getMin();
					o[i++] = Double.isNaN(s.getMedian()) ? null : s.getMedian();
					o[i++] = Double.isNaN(s.getStdev()) ? null : s.getStdev();
					o[i++] = Double.isNaN(s.getMax()) ? null : s.getMax();
					o[i++] = null;
				}
				else
				{
					o[i++] = null;
					o[i++] = null;
					o[i++] = null;
					o[i++] = null;
					CountedSet<String> set = CountedSet.fromArray(clustering.getStringValues(p, null));
					o[i++] = set.toString();
				}
				//			for (Dimensions d : Dimensions.values())
				//			{
				//				embMap.put(d, p, embedQuality.get(p).clone());
				//				o[i++] = embMap.get(d, p);
				//			}
			}
			else
			{
				for (; i < o.length; i++)
					o[i] = null;
			}
			tableModel.addRow(o);
		}

		updateFeatureSelection();

		return table;
	}

	@Override
	protected String getShortName()
	{
		return "feature-table";
	}

	@Override
	protected DefaultTableModel createTableModel()
	{
		DefaultTableModel model = new DefaultTableModel()
		{
			@Override
			public Class<?> getColumnClass(int columnIndex)
			{
				if (columnIndex == 0)
					return Integer.class;
				if (columnIndex == 1)
					return Highlighter.class;
				if (columnIndex == 4 || columnIndex == 5)
					return Integer.class;
				if (columnIndex >= 6 || columnIndex <= 8)
					return Double.class;
				//				if (columnIndex >= 5 || columnIndex <= 8)
				//					return CompoundPropertyEmbedQuality.class;
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		model.addColumn("");
		model.addColumn("Feature");
		model.addColumn("Used for Embedding");
		model.addColumn("Type");
		model.addColumn("#Missing");
		model.addColumn("#Distinct");
		model.addColumn("Min");
		model.addColumn("Median");
		model.addColumn("Stdev");
		model.addColumn("Max");
		model.addColumn("Values");
		return model;
	}

	@Override
	protected boolean addSpecificityInfo()
	{
		return false;
	}

	@Override
	protected String getItemName()
	{
		return "feature";
	}

	private void updateFeatureSelection()
	{
		selfUpdate = true;
		int index = sorter.convertRowIndexToView(highlighters.indexOf(viewControler.getHighlighter()));
		table.setRowSelectionInterval(index, index);
		selfUpdate = false;
	}
}