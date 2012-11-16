package gui;

import gui.ViewControler.HighlightSorting;
import gui.swing.ComponentFactory;
import gui.swing.ComponentFactory.StyleButton;
import gui.swing.TransparentViewPanel;
import gui.util.Highlighter;
import gui.util.MoleculePropertyHighlighter;

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

	JButton buttonPlus;
	JButton buttonMinus;

	JSlider slider;

	JComboBox highlightCombobox;
	JCheckBox labelCheckbox;
	JComboBox highlightMinMaxCombobox;

	ViewControler viewControler;
	Clustering clustering;

	public ControlPanel(ViewControler viewControler, Clustering clustering)
	{
		this.viewControler = viewControler;
		this.clustering = clustering;

		buildLayout();
		addListeners();
	}

	private void buildLayout()
	{
		//		spinCheckbox = ComponentFactory.createCheckBox("Spin on/off");
		//		spinCheckbox.setSelected(viewControler.isSpinEnabled());
		//		spinCheckbox.setOpaque(false);
		//		spinCheckbox.setFocusable(false);

		//		setBackground(Settings.TRANSPARENT_BACKGROUND);

		buttonWire = new StyleButton("Wireframe", true, ViewControler.STYLE_WIREFRAME);
		buttonBalls = new StyleButton("Balls & Sticks", false, ViewControler.STYLE_BALLS_AND_STICKS);
		ButtonGroup g = new ButtonGroup();
		g.add(buttonWire);
		g.add(buttonBalls);
		buttonWire.setSelected(viewControler.getStyle().equals(buttonWire.style));
		buttonBalls.setSelected(viewControler.getStyle().equals(buttonBalls.style));

		buttonWire.setOpaque(false);
		buttonBalls.setOpaque(false);
		buttonWire.setFocusable(false);
		buttonBalls.setFocusable(false);

		buttonPlus = ComponentFactory.createViewButton("+", new Insets(1, 3, 1, 3));
		buttonMinus = ComponentFactory.createViewButton("-", new Insets(1, 3, 1, 3));
		buttonMinus.setPreferredSize(buttonPlus.getPreferredSize());

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
		p.add(new JLabel(" "));
		p.add(buttonBalls);

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
		buttonWire.addActionListener(l);
		buttonBalls.addActionListener(l);

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
			}
		});
	}

	private void updateComboStuff()
	{
		selfUpdate = true;
		highlightCombobox.setSelectedItem(viewControler.getHighlighter());
		labelCheckbox.setSelected(viewControler.isHighlighterLabelsVisible());
		highlightMinMaxCombobox.setSelectedItem(viewControler.getHighlightSorting());
		boolean featHighSel = ((Highlighter) highlightCombobox.getSelectedItem()) instanceof MoleculePropertyHighlighter;
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
			Dimension dim = highlightCombobox.getPreferredSize();
			highlightCombobox.setPreferredSize(new Dimension(250, dim.height));
		}
		selfUpdate = false;
	}

	//	public static void main(String args[])
	//	{
	//		SwingUtil.showInDialog(new ControlPanel(new ViewControler()
	//		{
	//
	//			@Override
	//			public boolean isSpinEnabled()
	//			{
	//				return false;
	//			}
	//
	//			@Override
	//			public void setSpinEnabled(boolean spinEnabled)
	//			{
	//			}
	//
	//			@Override
	//			public void setDensitiyHigher(boolean higher)
	//			{
	//			}
	//
	//			@Override
	//			public String getStyle()
	//			{
	//				return ViewControler.STYLE_WIREFRAME;
	//			}
	//
	//			@Override
	//			public void setStyle(String style)
	//			{
	//			}
	//
	//			@Override
	//			public HashMap<String, Highlighter[]> getHighlighters()
	//			{
	//				return null;
	//			}
	//
	//			@Override
	//			public void setHighlighter(Highlighter highlighter)
	//			{
	//			}
	//
	//			@Override
	//			public Highlighter getHighlighter()
	//			{
	//				return null;
	//			}
	//
	//			@Override
	//			public boolean isHighlighterLabelsVisible()
	//			{
	//				return false;
	//			}
	//
	//			@Override
	//			public void setHighlighterLabelsVisible(boolean selected)
	//			{
	//			}
	//
	//			@Override
	//			public void setHighlightSorting(HighlightSorting sorting)
	//			{
	//
	//			}
	//
	//			@Override
	//			public HighlightSorting getHighlightSorting()
	//			{
	//				return null;
	//			}
	//
	//			@Override
	//			public void addViewListener(PropertyChangeListener l)
	//			{
	//			}
	//
	//			@Override
	//			public boolean canChangeDensitiy(boolean higher)
	//			{
	//				return true;
	//			}
	//
	//			//			@Override
	//			//			public boolean isHideUnselected()
	//			//			{
	//			//				return false;
	//			//			}
	//			//
	//			//			@Override
	//			//			public void setHideUnselected(boolean hide)
	//			//			{
	//			//			}
	//
	//			@Override
	//			public boolean isHideHydrogens()
	//			{
	//				return false;
	//			}
	//
	//			@Override
	//			public void setHideHydrogens(boolean b)
	//			{
	//			}
	//
	//			@Override
	//			public void setSuperimpose(boolean superimpose)
	//			{
	//			}
	//
	//			@Override
	//			public boolean isSuperimpose()
	//			{
	//				return false;
	//			}
	//
	//			@Override
	//			public boolean isAllClustersSpreadable()
	//			{
	//				return false;
	//			}
	//
	//			@Override
	//			public boolean isSingleClusterSpreadable()
	//			{
	//				return false;
	//			}
	//
	//		}));
	//	}
}
