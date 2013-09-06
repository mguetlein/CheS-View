package gui.table;

import gui.ClickMouseOverTable;
import gui.ViewControler;
import gui.util.CompoundPropertyHighlighter;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;

import util.SelectionModel;
import cluster.Clustering;
import cluster.Compound;
import dataInterface.CompoundProperty;

public abstract class CCDataTable extends DataTable
{
	protected int nonPropColumns;
	protected List<CompoundProperty> props;
	protected ClickMouseOverTable table;
	protected int sortColumn = 1;

	public CCDataTable(ViewControler viewControler, Clustering clustering)
	{
		super(viewControler, clustering);
	}

	public abstract String getExtraColumn();

	public abstract boolean addEmbeddingQuality();

	protected void updateFeatureSelection()
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		if (viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)
		{
			CompoundProperty prop = ((CompoundPropertyHighlighter) viewControler.getHighlighter()).getProperty();
			int idx = props.indexOf(prop);
			if (idx != -1)
				sortColumn = idx + nonPropColumns;
		}
		List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
		sortKeys.add(new RowSorter.SortKey(sortColumn, SortOrder.ASCENDING));
		sorter.setSortKeys(sortKeys);

		selfUpdate = false;
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
					return Compound.class;
				if (columnIndex >= nonPropColumns
						&& props.get(columnIndex - nonPropColumns).getType() == CompoundProperty.Type.NUMERIC)
					return Double.class;
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		model.addColumn("");
		model.addColumn("Compound");
		model.addColumn(getExtraColumn());
		nonPropColumns = model.getColumnCount();

		props = new ArrayList<CompoundProperty>();
		if (addEmbeddingQuality() && clustering.getEmbeddingQualityProperty() != null)
			props.add(clustering.getEmbeddingQualityProperty());
		for (CompoundProperty p : clustering.getProperties())
			if (!p.isSmiles())
				props.add(p);
		for (CompoundProperty p : clustering.getFeatures())
			props.add(p);
		for (CompoundProperty p : props)
			model.addColumn(p);

		return model;
	}

	protected void updateTableFromSelection(SelectionModel compoundSelection, ListSelectionModel tableSelectionA,
			SelectionModel tableSelectionB)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		if (tableSelectionA != null)
			tableSelectionA.clearSelection();
		else
			tableSelectionB.clearSelection();
		int sel[] = compoundSelection.getSelectedIndices();

		if (sel != null && sel.length > 0)
			for (int i = 0; i < sel.length; i++)
			{
				int idx = sorter.convertRowIndexToView(sel[i]);
				if (tableSelectionA != null)
					tableSelectionA.addSelectionInterval(idx, idx);
				else
					tableSelectionB.setSelected(idx, false);
				if (i == sel.length - 1)
					table.scrollRectToVisible(new Rectangle(table.getCellRect(idx, 0, true)));
			}
		selfUpdate = false;
	}
}
