package gui;

import java.awt.event.KeyEvent;

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

}
