package gui;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPopupMenu;

import cluster.Clustering;

public interface GUIControler extends Blockable
{
	public void updateTitle(Clustering c);

	public void setFullScreen(boolean b);

	public boolean isFullScreen();

	public JPopupMenu getPopup();

	public void handleKeyEvent(KeyEvent e);

	public static final String PROPERTY_FULLSCREEN_CHANGED = "PROPERTY_FULLSCREEN_CHANGED";

	public void addPropertyChangeListener(PropertyChangeListener l);

	public void setVisible(boolean b);

}
