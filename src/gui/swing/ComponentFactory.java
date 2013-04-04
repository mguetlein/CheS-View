package gui.swing;

import gui.DescriptionListCellRenderer;
import gui.LinkButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import main.Settings;
import util.SwingUtil;

public class ComponentFactory
{
	public static Color BACKGROUND;
	public static Color FOREGROUND;
	public static Color BORDER_FOREGROUND;
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
			FOREGROUND = new Color(250, 250, 250);
			BORDER_FOREGROUND = FOREGROUND;
			LIST_SELECTION_FOREGROUND = Color.WHITE;
			LIST_ACTIVE_BACKGROUND = new Color(51, 102, 255);
			LIST_WATCH_BACKGROUND = LIST_ACTIVE_BACKGROUND.darker().darker();
		}
		else
		{
			BACKGROUND = Color.WHITE;
			FOREGROUND = new Color(5, 5, 5);
			BORDER_FOREGROUND = Color.LIGHT_GRAY;
			LIST_SELECTION_FOREGROUND = Color.BLACK;
			LIST_ACTIVE_BACKGROUND = new Color(101, 152, 255);
			LIST_WATCH_BACKGROUND = LIST_ACTIVE_BACKGROUND.brighter().brighter();
		}
		if (Settings.TOP_LEVEL_FRAME != null)
			SwingUtilities.updateComponentTreeUI(Settings.TOP_LEVEL_FRAME);
	}

	//	static class UIUtil
	//	{
	//		static HashMap<String, Color> orig = new HashMap<String, Color>();
	//
	//		public static void set(String v, Color c)
	//		{
	//			if (!orig.containsKey(v))
	//				orig.put(v, UIManager.getColor(v));
	//			UIManager.put(v, new ColorUIResource(c));
	//		}
	//		public static void unset()
	//		{
	//			for (String v : orig.keySet())
	//				UIManager.put(v, new ColorUIResource(orig.get(v)));
	//		}
	//	}
	//	private static void setViewUIColors()
	//	{
	//		UIUtil.set("ScrollBar.background", BACKGROUND);
	//		UIUtil.set("ScrollBar.foreground", BORDER_FOREGROUND);
	//		UIUtil.set("ScrollBar.thumbHighlight", BORDER_FOREGROUND);
	//		UIUtil.set("ScrollBar.thumbShadow", BORDER_FOREGROUND.darker());
	//		UIUtil.set("ScrollBar.thumbDarkShadow", BORDER_FOREGROUND.darker().darker());
	//		UIUtil.set("ScrollBar.thumb", BACKGROUND);
	//		UIUtil.set("ScrollBar.track", BACKGROUND);
	//		UIUtil.set("ScrollBar.trackHighlight", BACKGROUND);
	//		UIUtil.set("ComboBox.buttonBackground", BACKGROUND);
	//		UIUtil.set("ComboBox.buttonShadow", BORDER_FOREGROUND.darker());
	//		UIUtil.set("ComboBox.buttonDarkShadow", BORDER_FOREGROUND.darker().darker());
	//		UIUtil.set("ComboBox.buttonHighlight", BORDER_FOREGROUND);
	//	}
	//	private static void unsetViewUIColors()
	//	{
	//		UIUtil.unset();
	//	}

	public static JTextField createUneditableViewTextField()
	{
		JTextField l = new JTextField()
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
			}
		};
		l.setEditable(false);
		l.setBorder(null);
		l.setOpaque(false);
		return l;
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
				setUI(new BasicComboBoxUI()
				{
					protected JButton createArrowButton()
					{
						JButton button = new BasicArrowButton(BasicArrowButton.SOUTH, BACKGROUND,
								BORDER_FOREGROUND.darker(), BORDER_FOREGROUND.darker().darker(), BORDER_FOREGROUND);
						button.setName("ComboBox.arrowButton");
						return button;
					}

					protected ComboPopup createPopup()
					{
						return new BasicComboPopup(comboBox)
						{
							protected JScrollPane createScroller()
							{
								JScrollPane sp = new JScrollPane(list,
										ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
										ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
								sp.setHorizontalScrollBar(null);
								sp.setVerticalScrollBar(new JScrollBar(JScrollBar.VERTICAL)
								{
									public void updateUI()
									{
										setUI(new MyScrollBarUI());
									}
								});
								return sp;
							}

							protected void configurePopup()
							{
								super.configurePopup();
								setBorder(new EtchedBorder(BORDER_FOREGROUND, BORDER_FOREGROUND.darker()));
							}
						};
					}
				});
				setForeground(FOREGROUND);
				setBackground(BACKGROUND);
				setBorder(new EtchedBorder(BORDER_FOREGROUND, BORDER_FOREGROUND.darker()));
				if (getRenderer() instanceof DescriptionListCellRenderer)
					((DescriptionListCellRenderer) getRenderer())
							.setDescriptionForeground(FOREGROUND.darker().darker());
			}
		};
		for (Object object : items)
			c.addItem(object);
		c.setOpaque(false);
		c.setForeground(FOREGROUND);
		c.setBackground(BACKGROUND);
		c.setFont(f);
		r.setDescriptionForeground(FOREGROUND.darker().darker());
		c.setRenderer(r);
		return c;
	}

	public static JTable createTable()
	{
		return createTable(false);
	}

	public static JTable createTable(final boolean halfTransparent)
	{
		DefaultTableModel m = new DefaultTableModel()
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		JTable t = new JTable(m);
		t.setBorder(null);
		t.getTableHeader().setVisible(false);
		t.getTableHeader().setPreferredSize(new Dimension(-1, 0));
		t.setGridColor(new Color(0, 0, 0, 0));
		t.setOpaque(false);
		t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		t.setRowHeight(t.getRowHeight() + 1);
		t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column)
			{
				super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
				if (isSelected)
				{
					setBackground(LIST_ACTIVE_BACKGROUND);
					setOpaque(true);
					setForeground(LIST_SELECTION_FOREGROUND);
				}
				else
				{
					if (halfTransparent)
					{
						setBackground(new Color(BACKGROUND.getRed(), BACKGROUND.getGreen(), BACKGROUND.getBlue(), 100));
						setOpaque(true);
					}
					else
						setOpaque(false);
					setForeground(FOREGROUND);
				}
				return this;
			}
		});

		return t;
	}

	public static int packColumn(JTable table, int vColIndex, int margin)
	{
		return packColumn(table, vColIndex, margin, Integer.MAX_VALUE);
	}

	public static int packColumn(JTable table, int vColIndex, int margin, int max)
	{
		DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
		TableColumn col = colModel.getColumn(vColIndex);
		int width = 0;

		// Get width of column header
		TableCellRenderer renderer = col.getHeaderRenderer();
		if (renderer == null)
		{
			renderer = table.getTableHeader().getDefaultRenderer();
		}
		Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
		width = comp.getPreferredSize().width;

		// Get maximum width of column data
		for (int r = 0; r < table.getRowCount(); r++)
		{
			renderer = table.getCellRenderer(r, vColIndex);
			comp = renderer.getTableCellRendererComponent(table, table.getValueAt(r, vColIndex), false, false, r,
					vColIndex);
			width = Math.max(width, comp.getPreferredSize().width);
		}

		// Add margin
		width += 2 * margin;
		if (width > max)
			width = max;

		// Set the width
		col.setPreferredWidth(width);
		//		col.setMinWidth(width);
		//		col.setMaxWidth(width);
		return width;
	}

	static class MyScrollBarUI extends BasicScrollBarUI
	{
		protected JButton createDecreaseButton(int orientation)
		{
			return new BasicArrowButton(orientation, BACKGROUND, BORDER_FOREGROUND.darker(), BORDER_FOREGROUND.darker()
					.darker(), BORDER_FOREGROUND);
		}

		protected JButton createIncreaseButton(int orientation)
		{
			return new BasicArrowButton(orientation, BACKGROUND, BORDER_FOREGROUND.darker(), BORDER_FOREGROUND.darker()
					.darker(), BORDER_FOREGROUND);
		}

		protected void configureScrollBarColors()
		{
			scrollbar.setForeground(BORDER_FOREGROUND);
			scrollbar.setBackground(BACKGROUND);
			thumbHighlightColor = BORDER_FOREGROUND;
			thumbLightShadowColor = BORDER_FOREGROUND.darker();
			thumbDarkShadowColor = BORDER_FOREGROUND.darker().darker();
			thumbColor = BACKGROUND;
			trackColor = BACKGROUND;
			trackHighlightColor = BACKGROUND;
		}
	}

	public static JScrollPane createViewScrollpane(JComponent table)
	{
		JScrollPane p = new JScrollPane(table)
		{
			public void updateUI()
			{
				super.updateUI();
				setBorder(new EtchedBorder(BORDER_FOREGROUND, BORDER_FOREGROUND.darker()));
			}
		};
		p.setVerticalScrollBar(new JScrollBar(JScrollBar.VERTICAL)
		{
			public void updateUI()
			{
				setUI(new MyScrollBarUI());
			}
		});
		p.setHorizontalScrollBar(new JScrollBar(JScrollBar.HORIZONTAL)
		{
			public void updateUI()
			{
				setUI(new MyScrollBarUI());
			}
		});
		p.setOpaque(false);
		p.getViewport().setOpaque(false);
		p.setViewportBorder(new EmptyBorder(5, 5, 5, 5));

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
				setForeground(FOREGROUND);
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

	static class MySliderUI extends BasicSliderUI
	{
		public MySliderUI(JSlider b)
		{
			super(b);
			b.setBackground(BACKGROUND);
		}

		protected Color getShadowColor()
		{
			return BORDER_FOREGROUND.darker().darker();
		}

		protected Color getHighlightColor()
		{
			return BORDER_FOREGROUND;
		}
	}

	public static JSlider createViewSlider(int min, int max, int value)
	{
		JSlider s = new JSlider(min, max, value)
		{
			public void updateUI()
			{
				super.updateUI();
				setForeground(FOREGROUND);
				setUI(new MySliderUI(this));
			}
		};
		s.setOpaque(false);
		s.setFocusable(false);
		return s;
	}

	public static void main(String args[])
	{
		ComponentFactory.setBackgroundBlack(false);

		JPanel p = new JPanel();
		p.setBackground(BACKGROUND);
		p.setPreferredSize(new Dimension(500, 100));
		p.add(ComponentFactory.createViewButton("testing"));
		p.add(ComponentFactory.createViewSlider(0, 100, 33));
		SwingUtil.showInDialog(p);
	}
}
