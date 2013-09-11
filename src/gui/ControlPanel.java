package gui;

import gui.ViewControler.HighlightSorting;
import gui.ViewControler.Style;
import gui.swing.ComponentFactory;
import gui.swing.ComponentFactory.StyleButton;
import gui.swing.TransparentViewPanel;
import gui.util.CompoundPropertyHighlighter;
import gui.util.Highlighter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cluster.Clustering;

public class ControlPanel extends TransparentViewPanel
{
	boolean selfUpdate = false;

	//	JCheckBox spinCheckbox;

	StyleButton buttonWire;
	StyleButton buttonBalls;
	StyleButton buttonDots;

	JButton buttonPlus;
	JButton buttonMinus;

	JSlider slider;

	JComboBox highlightCombobox;
	JCheckBox labelCheckbox;
	JComboBox highlightMinMaxCombobox;

	ViewControler viewControler;
	Clustering clustering;
	GUIControler guiControler;

	public ControlPanel(ViewControler viewControler, Clustering clustering, GUIControler guiControler)
	{
		this.viewControler = viewControler;
		this.clustering = clustering;
		this.guiControler = guiControler;

		buildLayout();
		addListeners();
	}

	private void buildLayout()
	{
		//		spinCheckbox = ComponentFactory.createCheckBox("Spin on/off");
		//		spinCheckbox.setSelected(viewControler.isSpinEnabled());
		//		spinCheckbox.setOpaque(false);

		//		setBackground(Settings.TRANSPARENT_BACKGROUND);

		buttonWire = new StyleButton("Wireframe", true, Style.wireframe);
		buttonBalls = new StyleButton("Balls & Sticks", false, Style.ballsAndSticks);
		buttonDots = new StyleButton("Dots", false, Style.dots);

		ButtonGroup g = new ButtonGroup();
		for (StyleButton b : new StyleButton[] { buttonWire, buttonBalls, buttonDots })
		{
			g.add(b);
			b.setSelected(viewControler.getStyle() == b.style);
			b.setOpaque(false);
			b.setFocusable(false);
		}

		buttonPlus = ComponentFactory.createViewButton("+", new Insets(1, 3, 1, 3));
		buttonMinus = ComponentFactory.createViewButton("-", new Insets(1, 3, 1, 3),
				new ComponentFactory.PreferredSizeProvider()
				{

					@Override
					public Dimension getPreferredSize()
					{
						return buttonPlus.getPreferredSize();
					}
				});
		//		buttonMinus.setPreferredSize(buttonPlus.getPreferredSize());

		slider = ComponentFactory.createViewSlider(0, viewControler.getCompoundSizeMax(),
				viewControler.getCompoundSize());
		slider.setPreferredSize(new Dimension(100, slider.getPreferredSize().height));

		highlightCombobox = ComponentFactory.createViewComboBox();
		loadHighlighters();

		highlightCombobox.setSelectedItem(viewControler.getHighlighter());

		labelCheckbox = ComponentFactory.createViewCheckBox("Label");
		labelCheckbox.setSelected(viewControler.isHighlighterLabelsVisible());
		labelCheckbox.setOpaque(false);
		labelCheckbox.setVisible(false);

		highlightMinMaxCombobox = ComponentFactory.createViewComboBox(HighlightSorting.values());

		highlightMinMaxCombobox.setSelectedItem(viewControler.getHighlightSorting());
		highlightMinMaxCombobox.setVisible(false);

		// setBorder(new MatteBorder(1, 1, 1, 1, Color.red));
		// JPanel p = new JPanel();

		// add(new JLabel("Graphic Settings:"));

		JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		p.setOpaque(false);
		p.add(buttonMinus);
		p.add(slider);
		p.add(buttonPlus);
		p.add(new JLabel("   "));
		//		p.add(spinCheckbox);
		p.add(buttonWire);
		p.add(new JLabel(""));
		p.add(buttonBalls);
		p.add(new JLabel(""));
		p.add(buttonDots);

		JPanel p2 = new JPanel();
		p2.setOpaque(false);
		p2.add(ComponentFactory.createViewLabel("<html><b>Feature:</b></html>"));
		p2.add(highlightCombobox);
		p2.add(labelCheckbox);
		p2.add(highlightMinMaxCombobox);

		setLayout(new BorderLayout());
		JPanel pp = new JPanel(new BorderLayout());
		pp.setOpaque(false);
		pp.add(p, BorderLayout.WEST);
		add(pp, BorderLayout.NORTH);
		JPanel pp2 = new JPanel(new BorderLayout());
		pp2.setOpaque(false);
		pp2.add(p2, BorderLayout.WEST);
		add(pp2, BorderLayout.SOUTH);
	}

	private void addListeners()
	{
		//		spinCheckbox.addActionListener(new ActionListener()
		//		{
		//
		//			@Override
		//			public void actionPerformed(ActionEvent e)
		//			{
		//				if (updateByViewControler)
		//					return;
		//				viewControler.setSpinEnabled(spinCheckbox.isSelected());
		//			}
		//		});

		ActionListener l = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				viewControler.setStyle(((StyleButton) e.getSource()).style);
			}
		};
		for (StyleButton b : new StyleButton[] { buttonWire, buttonBalls, buttonDots })
			b.addActionListener(l);

		ActionListener l2 = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				viewControler.changeCompoundSize(e.getSource() == buttonPlus);
			}
		};
		buttonPlus.addActionListener(l2);
		buttonMinus.addActionListener(l2);

		slider.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(ChangeEvent e)
			{
				if (selfUpdate)
					return;
				JSlider source = (JSlider) e.getSource();
				if (!source.getValueIsAdjusting())
				{
					viewControler.setCompoundSize((int) source.getValue());
				}
			}
		});

		highlightCombobox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						viewControler.setHighlighter((Highlighter) highlightCombobox.getSelectedItem());
					}
				});
			}
		});

		labelCheckbox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						viewControler.setHighlighterLabelsVisible(labelCheckbox.isSelected());
					}
				});
			}
		});
		highlightMinMaxCombobox.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (selfUpdate)
					return;
				SwingUtilities.invokeLater(new Runnable()
				{
					@Override
					public void run()
					{
						viewControler.setHighlightSorting((HighlightSorting) highlightMinMaxCombobox.getSelectedItem());
					}
				});
			}
		});

		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				updateComboStuff();
			}
		});

		viewControler.addViewListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				//					Settings.LOGGER.println("fire updated " + evt.getPropertyName());
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_CHANGED))
				{
					updateComboStuff();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_SUPERIMPOSE_CHANGED))
				{
					updateComboStuff();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_NEW_HIGHLIGHTERS))
				{
					loadHighlighters();
					updateComboStuff();
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_DENSITY_CHANGED))
				{
					selfUpdate = true;
					buttonPlus.setEnabled(viewControler.canChangeCompoundSize(true));
					buttonMinus.setEnabled(viewControler.canChangeCompoundSize(false));
					slider.setValue(viewControler.getCompoundSize());
					selfUpdate = false;
				}
				else if (evt.getPropertyName().equals(ViewControler.PROPERTY_FONT_SIZE_CHANGED))
					updateComboSize();
			}
		});

		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_VIEWER_SIZE_CHANGED))
				{
					updateComboSize();
				}
			}
		});
	}

	private void updateComboStuff()
	{
		selfUpdate = true;
		highlightCombobox.setSelectedItem(viewControler.getHighlighter());
		labelCheckbox.setSelected(viewControler.isHighlighterLabelsVisible());
		highlightMinMaxCombobox.setSelectedItem(viewControler.getHighlightSorting());
		boolean featHighSel = ((Highlighter) highlightCombobox.getSelectedItem()) instanceof CompoundPropertyHighlighter;
		labelCheckbox.setVisible(featHighSel);
		highlightMinMaxCombobox.setVisible(featHighSel && viewControler.isSuperimpose()
				&& !clustering.isClusterActive());
		selfUpdate = false;
	}

	private void loadHighlighters()
	{
		selfUpdate = true;
		((DefaultComboBoxModel) highlightCombobox.getModel()).removeAllElements();
		(((DescriptionListCellRenderer) highlightCombobox.getRenderer())).clearDescriptions();

		HashMap<String, Highlighter[]> h = viewControler.getHighlighters();
		if (h != null)
		{
			int index = 0;
			for (String desc : h.keySet())
			{
				if (desc != null && desc.length() > 0)
					(((DescriptionListCellRenderer) highlightCombobox.getRenderer())).addDescription(index, desc);
				index += h.get(desc).length;
				for (Highlighter hh : h.get(desc))
					highlightCombobox.addItem(hh);
			}
		}
		updateComboSize();
		selfUpdate = false;
	}

	private void updateComboSize()
	{
		highlightCombobox.setPreferredSize(null);
		Dimension dim = highlightCombobox.getPreferredSize();
		int width = Math.min(dim.width, guiControler.getComponentMaxWidth(0.33));
		//		System.out.println(width);
		highlightCombobox.setPreferredSize(new Dimension(width, dim.height));
		highlightCombobox.revalidate();
	}

}
