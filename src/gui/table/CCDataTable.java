package gui.table;

import gui.ClickMouseOverTable;
import gui.ViewControler;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;

import cluster.ClusterController;
import cluster.Clustering;
import cluster.Compound;
import dataInterface.CompoundProperty;
import dataInterface.NumericProperty;

public abstract class CCDataTable extends DataTable
{
	protected int nonPropColumns;
	protected List<CompoundProperty> props;
	protected ClickMouseOverTable table;
	protected int sortColumn = 1;

	public CCDataTable(ViewControler viewControler, ClusterController clusterControler, Clustering clustering)
	{
		super(viewControler, clusterControler, clustering);
	}

	public abstract String getExtraColumn();

	public abstract boolean addAdditionalProperties();

	protected void updateFeatureSelection()
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		if (viewControler.getHighlightedProperty() != null)
		{
			CompoundProperty prop = viewControler.getHighlightedProperty();
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
				if (columnIndex >= nonPropColumns && props.get(columnIndex - nonPropColumns) instanceof NumericProperty)
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
		if (addAdditionalProperties() && clustering.getAdditionalProperties() != null)
			for (CompoundProperty p : clustering.getAdditionalProperties())
				props.add(p);
		for (CompoundProperty p : clustering.getProperties())
			if (p.getCompoundPropertySet() == null || !p.getCompoundPropertySet().isSmiles())
				props.add(p);
		for (CompoundProperty p : clustering.getFeatures())
			props.add(p);
		for (CompoundProperty p : props)
			model.addColumn(p);

		return model;
	}

	protected void updateTableFromSelection(boolean active, int... selected)
	{
		if (selfUpdate)
			return;
		selfUpdate = true;

		if (active)
			table.getClickSelectionModel().clearSelection();
		else
			table.getSelectionModel().clearSelection();

		if (selected.length > 1 || (selected.length == 1 && selected[0] != -1))
		{
			int idx = -1;
			for (int i : selected)
			{
				idx = sorter.convertRowIndexToView(i);
				if (active)
					table.getClickSelectionModel().setSelected(idx, false);
				else
					table.getSelectionModel().addSelectionInterval(idx, idx);
			}
			table.scrollRectToVisible(new Rectangle(table.getCellRect(idx, 0, true)));
		}
		selfUpdate = false;
	}
}
