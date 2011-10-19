package gui.swing;

import java.awt.Color;

import javax.swing.JLabel;

public class TransparentViewLabel extends JLabel
{
	private Color background;

	public TransparentViewLabel()
	{
		super();
		setOpaque(true);
	}

	public TransparentViewLabel(String t)
	{
		super(t);
		setOpaque(true);
	}

	public void updateUI()
	{
		super.updateUI();
		setBackground(ComponentFactory.BACKGROUND);
		setForeground(ComponentFactory.FOREGROUND);
	}

	public void setBackground(Color col)
	{
		background = new Color(col.getRed(), col.getGreen(), col.getBlue(), 100);
	}

	public Color getBackground()
	{
		return background;
	}
}
