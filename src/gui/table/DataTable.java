package gui.table;

import gui.BlockableFrame;
import gui.LinkButton;
import gui.ViewControler;
import gui.swing.ComponentFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import main.Settings;
import cluster.Clustering;

public abstract class DataTable extends BlockableFrame
{
	protected ViewControler viewControler;
	protected Clustering clustering;
	protected JTable table;
	protected DefaultTableModel tableModel;
	protected TableRowSorter<DefaultTableModel> sorter;
	protected boolean selfUpdate;

	public DataTable(ViewControler viewControler, Clustering clustering)
	{
		this.viewControler = viewControler;
		this.clustering = clustering;

		addListeners();

		tableModel = createTableModel();
		table = createTable();

		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		int width = 0;
		for (int i = 0; i < table.getColumnCount(); i++)
			width += Math.min(300, ComponentFactory.packColumn(table, i, 5, 150));
		JScrollPane scroll = new JScrollPane(table);
		setTitle(Settings.text(getShortName() + ".title"));
		JPanel p = new JPanel(new BorderLayout(10, 10));
		JPanel pp = new JPanel(new BorderLayout());
		pp.add(new JLabel("<html>" + Settings.text(getShortName() + ".info") + "</html>"), BorderLayout.NORTH);
		if (addSpecificityInfo())
		{
			LinkButton l = new LinkButton("Next to each feature value, the specificity of the " + getItemName()
					+ " feature values is given in brackets.");
			l.addActionListener(new ActionListener()
			{

				@Override
				public void actionPerformed(ActionEvent e)
				{
					try
					{
						Desktop.getDesktop().browse(new URI(Settings.HOMEPAGE_SPECIFICITY));
					}
					catch (Exception ex)
					{
						Settings.LOGGER.error(ex);
					}
				}
			});
			l.setForegroundFont(l.getFont().deriveFont(Font.PLAIN));
			l.setSelectedForegroundFont(l.getFont().deriveFont(Font.PLAIN));
			l.setSelectedForegroundColor(Color.BLUE);
			pp.add(l, BorderLayout.SOUTH);
		}
		//		String additional = "";
		//		if (addSpecificityInfo())
		//			additional = "<br>In brackets: the specificity of the " + getItemName() + " feature value <a href=\""
		//					+ Settings.HOMEPAGE_SPECIFICITY + "\">more</a>";
		p.add(pp, BorderLayout.NORTH);
		p.add(scroll);
		p.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(p);
		pack();
		pack();
		int scrollBarSize = ((Integer) UIManager.get("ScrollBar.width")).intValue();
		setSize(new Dimension(Math.min(Settings.TOP_LEVEL_FRAME.getWidth() * 3 / 4, width + 20 + scrollBarSize + 2),
				getHeight()));
		setLocationRelativeTo(Settings.TOP_LEVEL_FRAME);

		setVisible(true);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				table.requestFocus();
			}
		});
	}

	protected abstract String getItemName();

	protected abstract boolean addSpecificityInfo();

	protected abstract DefaultTableModel createTableModel();

	protected abstract JTable createTable();

	protected abstract String getShortName();

	private void addListeners()
	{
		((BlockableFrame) Settings.TOP_LEVEL_FRAME).addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (!isVisible())
					return;
				if (BlockableFrame.BLOCKED.equals(evt.getPropertyName()))
				{
					block(evt.getNewValue().toString());
				}
				else if (BlockableFrame.UN_BLOCKED.equals(evt.getPropertyName()))
				{
					unblock(evt.getNewValue().toString());
				}
			}
		});
		Settings.TOP_LEVEL_FRAME.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent e)
			{
				if (!isVisible())
					return;
				setVisible(false);
			}
		});
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (!isVisible())
					return;
				setVisible(false);
			}
		});
	}

}
