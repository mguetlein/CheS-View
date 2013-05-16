package gui;

import gui.swing.ComponentFactory;
import gui.util.MoleculePropertyHighlighter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import main.Settings;
import util.CountedSet;
import util.DoubleArraySummary;
import util.ListUtil;
import util.StringUtil;
import cluster.Clustering;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;

public class FeatureDialog extends JDialog
{
	@SuppressWarnings("unchecked")
	public FeatureDialog(final ViewControler viewControler, Clustering clustering)
	{
		super(Settings.TOP_LEVEL_FRAME);

		final DefaultTableModel model = new DefaultTableModel()
		{
			@SuppressWarnings("rawtypes")
			@Override
			public Class getColumnClass(int columnIndex)
			{
				if (columnIndex == 0)
					return MoleculeProperty.class;
				if (columnIndex == 3 || columnIndex == 4)
					return Integer.class;
				if (columnIndex >= 5 || columnIndex <= 7)
					return Double.class;
				//				if (columnIndex >= 5 || columnIndex <= 8)
				//					return MoleculePropertyEmbedQuality.class;
				return String.class;
			}

			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		final JTable table = new JTable(model);
		final TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>();
		table.setRowSorter(sorter);
		sorter.setModel(model);
		model.addColumn("Feature");
		model.addColumn("Used for Embedding");
		model.addColumn("Type");
		model.addColumn("#Missing");
		model.addColumn("#Distinct");
		model.addColumn("Min");
		model.addColumn("Median");
		model.addColumn("Max");
		model.addColumn("Values");
		table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				int row = table.getSelectedRow();
				if (row != -1)
				{
					row = sorter.convertRowIndexToModel(row);
					MoleculeProperty p = (MoleculeProperty) model.getValueAt(row, 0);
					viewControler.setHighlighter(p);
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
		//		for (Dimensions d : Dimensions.values())
		//			model.addColumn(d + " Emb-Qual");
		final List<MoleculeProperty> props = ListUtil.concat(clustering.getProperties(), clustering.getFeatures());
		//		final DoubleKeyHashMap<Dimensions, MoleculeProperty, MoleculePropertyEmbedQuality> embMap = new DoubleKeyHashMap<EmbedUtil.Dimensions, MoleculeProperty, EmbedUtil.MoleculePropertyEmbedQuality>();
		for (MoleculeProperty p : props)
		{
			Object o[] = new Object[9];// + Dimensions.values().length];
			int i = 0;
			o[i++] = p;
			o[i++] = clustering.getFeatures().contains(p) ? "Yes" : "no";
			o[i++] = p.getType() == Type.NUMERIC ? "Numeric" : (p.getType() == Type.NOMINAL ? "Nominal" : "undef.");
			o[i++] = clustering.numMissingValues(p);
			o[i++] = clustering.numDistinctValues(p);
			if (p.getType() == Type.NUMERIC)
			{
				DoubleArraySummary s = DoubleArraySummary.create(clustering.getDoubleValues(p));
				o[i++] = s.getMin();
				o[i++] = s.getMedian();
				o[i++] = s.getMax();
				o[i++] = null;
			}
			else
			{
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
			model.addRow(o);
		}
		if (viewControler.getHighlighter() instanceof MoleculePropertyHighlighter)
		{
			MoleculeProperty p = ((MoleculePropertyHighlighter) viewControler.getHighlighter()).getProperty();
			table.setRowSelectionInterval(props.indexOf(p), props.indexOf(p));
		}
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int width = 0;
		for (int i = 0; i < table.getColumnCount(); i++)
			width += Math.min(300, ComponentFactory.packColumn(table, i, 5));
		JScrollPane scroll = new JScrollPane(table);
		//		Thread th = new Thread(new Runnable()
		//		{
		//			@Override
		//			public void run()
		//			{
		//				while (!table.isShowing())
		//					ThreadUtil.sleep(100);
		//				for (MoleculeProperty p : props)
		//				{
		//					if (!table.isShowing())
		//						break;
		//					for (Dimensions d : Dimensions.values())
		//						embMap.get(d, p).compute(d);
		//					SwingUtilities.invokeLater(new Runnable()
		//					{
		//						@Override
		//						public void run()
		//						{
		//							table.repaint();
		//						}
		//					});
		//				}
		//			}
		//		});
		//		th.start();
		setTitle("Feature Table");
		JPanel p = new JPanel(new BorderLayout());
		p.add(scroll);
		p.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(p);
		pack();
		pack();
		int scrollBarSize = ((Integer) UIManager.get("ScrollBar.width")).intValue();
		setSize(new Dimension(Math.min(Settings.TOP_LEVEL_FRAME.getWidth(), width + 20 + scrollBarSize + 2),
				getHeight()));
		setLocationRelativeTo(Settings.TOP_LEVEL_FRAME);
		setVisible(true);
	}
}
