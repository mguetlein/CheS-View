package gui.swing;

import gui.DescriptionListCellRenderer;
import gui.LinkButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import main.Settings;

public class ComponentFactory
{
	public static Color BACKGROUND;
	public static Color FOREGROUND;
	public static Color LIST_SELECTION_FOREGROUND;
	public static Color LIST_ACTIVE_BACKGROUND;
	public static Color LIST_WATCH_BACKGROUND;

	static
	{
		setBackgroundBlack(true);
	}

	public static void setBackgroundBlack(Boolean b)
	{
		if (b)
		{
			BACKGROUND = Color.BLACK;
			FOREGROUND = Color.WHITE;
			//				FOREGROUND = new Color(170, 170, 170);
			LIST_SELECTION_FOREGROUND = FOREGROUND.brighter().brighter();
			LIST_ACTIVE_BACKGROUND = new Color(51, 102, 255);
			LIST_WATCH_BACKGROUND = LIST_ACTIVE_BACKGROUND.darker().darker();
		}
		else
		{
			BACKGROUND = Color.WHITE;
			FOREGROUND = Color.BLACK;
			//				FOREGROUND = new Color(50, 50, 50);
			LIST_SELECTION_FOREGROUND = FOREGROUND.darker().darker();
			LIST_ACTIVE_BACKGROUND = new Color(101, 152, 255);
			LIST_WATCH_BACKGROUND = LIST_ACTIVE_BACKGROUND.brighter().brighter();
		}
		if (Settings.TOP_LEVEL_COMPONENT != null)
			SwingUtilities.updateComponentTreeUI(Settings.TOP_LEVEL_COMPONENT);
	}

	public static JLabel createTransparentViewLabel()
	{
		return new TransparentViewLabel();
	}

	public static JLabel createViewLabel()
	{
		return createViewLabel("");
	}

	public static JLabel createViewLabel(String text)
	{
		JLabel l = new JLabel(text)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
			}
		};
		return l;
	}

	//	public static Border createThinBorder()
	//	{
	//		return new MatteBorder(1, 1, 1, 1, FOREGROUND);
	//	}
	//
	//	public static Border createLineBorder(int thickness)
	//	{
	//		return new LineBorder(FOREGROUND, thickness);
	//	}

	public static LinkButton createLinkButton(String text)
	{
		LinkButton l = new LinkButton(text);
		l.setForegroundColor(FOREGROUND);
		l.setSelectedForegroundColor(LIST_SELECTION_FOREGROUND);
		l.setSelectedForegroundFont(l.getFont());
		return l;
	}

	public static JCheckBox createViewCheckBox(String text)
	{
		JCheckBox c = new JCheckBox(text)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
			}
		};
		return c;
	}

	//	public static JRadioButton createViewRadioButton(String text)
	//	{
	//		JRadioButton r = new JRadioButton(text)
	//		{
	//			public void updateUI()
	//			{
	//				super.updateUI();
	//				setForeground(FOREGROUND);
	//			}
	//		};
	//		return r;
	//	}

	public static class StyleButton extends JRadioButton
	{
		public String style;

		public StyleButton(String text, boolean selected, String style)
		{
			super(text, selected);
			this.style = style;
		}

		public void updateUI()
		{
			super.updateUI();
			setForeground(FOREGROUND);
		}
	}

	public static JComboBox createViewComboBox()
	{
		return createViewComboBox(new Object[0]);
	}

	public static JComboBox createViewComboBox(Object[] items)
	{
		final Font f = new JLabel().getFont();
		DescriptionListCellRenderer r = new DescriptionListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				list.setSelectionBackground(LIST_ACTIVE_BACKGROUND);
				list.setSelectionForeground(LIST_SELECTION_FOREGROUND);
				list.setForeground(FOREGROUND);
				list.setBackground(BACKGROUND);
				list.setFont(f);
				return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			}
		};
		JComboBox c = new JComboBox()
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setBackground(BACKGROUND);

				if (getRenderer() instanceof DescriptionListCellRenderer)
					((DescriptionListCellRenderer) getRenderer())
							.setDescriptionForeground(FOREGROUND.darker().darker());
			}
		};
		for (Object object : items)
			c.addItem(object);
		c.setOpaque(false);
		c.setFont(f);
		r.setDescriptionForeground(FOREGROUND.darker().darker());
		c.setRenderer(r);
		return c;
	}

	//	public static JTable createTable()
	//	{
	//		DefaultTableModel m = new DefaultTableModel()
	//		{
	//			public boolean isCellEditable(int row, int column)
	//			{
	//				return false;
	//			}
	//		};
	//		JTable t = new JTable(m);
	//		t.getTableHeader().setVisible(false);
	//		t.getTableHeader().setPreferredSize(new Dimension(-1, 0));
	//		t.setGridColor(new Color(0, 0, 0, 0));
	//		t.setOpaque(false);
	//		t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
	//		{
	//
	//			@Override
	//			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
	//					boolean hasFocus, int row, int column)
	//			{
	//				setBackground(LIST_ACTIVE_BACKGROUND);
	//				if (row == 0 || isSelected)
	//				{
	//					setOpaque(true);
	//					setForeground(LIST_SELECTION_FOREGROUND);
	//				}
	//				else
	//				{
	//					setOpaque(false);
	//					setForeground(FOREGROUND);
	//				}
	//				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	//			}
	//		});
	//
	//		return t;
	//	}

	public static JScrollPane createViewScrollpane(JComponent table)
	{
		JScrollPane p = new JScrollPane(table);
		p.setOpaque(false);
		p.getViewport().setOpaque(false);
		return p;
	}

	public static JButton createViewButton(String string)
	{
		return createViewButton(string, new Insets(5, 5, 5, 5));
	}

	public static JButton createViewButton(String string, final Insets insets)
	{
		JButton c = new JButton(string)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setBackground(BACKGROUND);
				setBorder(new CompoundBorder(new LineBorder(FOREGROUND, 1), new EmptyBorder(insets)));
			}
		};
		c.setOpaque(false);
		c.setFocusable(false);
		c.setFont(new JLabel().getFont());
		return c;
	}

	public static JTextArea createViewTextArea(String text, boolean editable, boolean wrap)
	{
		JTextArea infoTextArea = new JTextArea(text)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground((Color) UIManager.get("View.foreground"));
			}
		};
		//infoTextArea.setFont(infoTextArea.getFont().deriveFont(Font.BOLD));
		if (!editable)
		{
			infoTextArea.setBorder(null);
			infoTextArea.setEditable(false);
			infoTextArea.setOpaque(false);
		}
		infoTextArea.setWrapStyleWord(wrap);
		infoTextArea.setLineWrap(wrap);
		return infoTextArea;
	}

}
