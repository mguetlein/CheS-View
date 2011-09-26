package gui;

import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JPopupMenu;

public interface GUIControler
{
	public void block(String blocker);

	public boolean isBlocked();

	public void unblock(String blocker);

	public void setFullScreen(boolean b);

	public boolean isFullScreen();

	public JPopupMenu getPopup();

	public void handleKeyEvent(KeyEvent e);

	public static final String PROPERTY_FULLSCREEN_CHANGED = "PROPERTY_FULLSCREEN_CHANGED";

	public void addPropertyChangeListener(PropertyChangeListener l);

}
