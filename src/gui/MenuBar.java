package gui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;

import main.Settings;
import util.ArrayUtil;
import util.SwingUtil;
import cluster.Clustering;
import data.ClusteringData;

public class MenuBar extends JMenuBar
{
	static interface MyMenuItem
	{

	}

	static class DefaultMyMenuItem implements MyMenuItem
	{
		Action action;

		public DefaultMyMenuItem(Action action)
		{
			this.action = action;
		}
	}

	static class MyMenu implements MyMenuItem
	{
		String name;
		Vector<MyMenuItem> items = new Vector<MyMenuItem>();

		public MyMenu(String name, Action... actions)
		{
			this.name = name;
			for (Action action : actions)
				items.add(new DefaultMyMenuItem(action));

		}

		public MyMenu(String name, MyMenuItem... items)
		{
			this.name = name;
			for (MyMenuItem item : items)
				this.items.add(item);
		}
	}

	static class MyMenuBar
	{
		Vector<MyMenu> menus = new Vector<MyMenu>();
		List<Action> actions = new ArrayList<Action>();

		public MyMenuBar(MyMenu... menus)
		{
			for (MyMenu m : menus)
				this.menus.add(m);

			for (MyMenu m : menus)
			{
				for (MyMenuItem i : m.items)
				{
					if (i instanceof DefaultMyMenuItem)
						actions.add(((DefaultMyMenuItem) i).action);
					else if (i instanceof MyMenu)
						for (MyMenuItem ii : ((MyMenu) i).items)
							actions.add(((DefaultMyMenuItem) ii).action);
				}
			}
		}
	}

	GUIControler guiControler;
	Clustering clustering;

	MyMenuBar menuBar;

	//file
	Action fActionNew;
	//edit
	Action eActionRemoveCurrent;
	Action eActionRemoveClusters;
	Action eActionRemoveModels;
	Action eActionExportCurrent;
	Action eActionExportClusters;
	Action eActionExportModels;
	//view
	Action vActionFullScreen;

	//help

	public MenuBar(GUIControler guiControler, Clustering clustering)
	{
		this.guiControler = guiControler;
		this.clustering = clustering;
		buildActions();
		buildMenu();
		installListeners();
		update();
	}

	private void buildMenu()
	{
		for (MyMenu m : menuBar.menus)
		{
			JMenu menu = new JMenu(m.name);
			for (MyMenuItem i : m.items)
			{
				if (i instanceof DefaultMyMenuItem)
					menu.add(((DefaultMyMenuItem) i).action);
				else if (i instanceof MyMenu)
				{
					JMenu mm = new JMenu(((MyMenu) i).name);
					for (MyMenuItem ii : ((MyMenu) i).items)
						mm.add(((DefaultMyMenuItem) ii).action);
					menu.add(mm);
				}
			}
			add(menu);
		}
	}

	public JPopupMenu getPopup()
	{
		JPopupMenu p = new JPopupMenu();
		boolean first = true;
		for (MyMenu m : menuBar.menus)
		{
			if (!first)
				p.addSeparator();
			else
				first = false;
			for (MyMenuItem i : m.items)
			{
				if (i instanceof DefaultMyMenuItem)
					p.add(((DefaultMyMenuItem) i).action);
				else if (i instanceof MyMenu)
				{
					JMenu mm = new JMenu(((MyMenu) i).name);
					for (MyMenuItem ii : ((MyMenu) i).items)
						mm.add(((DefaultMyMenuItem) ii).action);
					p.add(mm);
				}
			}
		}
		return p;
	}

	private void buildActions()
	{
		fActionNew = new AbstractAction("New dataset / mapping")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				newClustering(0);
			}
		};
		((AbstractAction) fActionNew).putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		Action fActionExit = new AbstractAction("Exit")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		};
		((AbstractAction) fActionExit).putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
		MyMenu fileMenu = new MyMenu("File", fActionNew, fActionExit);

		eActionRemoveCurrent = new AbstractAction("Remove Selected Cluster/Compound")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int[] m = (int[]) ((AbstractAction) eActionRemoveCurrent).getValue("Model");
				Integer c = (Integer) ((AbstractAction) eActionRemoveCurrent).getValue("Cluster");
				View.instance.setAnimated(false);
				if (m.length > 0)
					clustering.removeModels(m);
				else if (c != null)
					clustering.removeCluster(c);
				View.instance.setAnimated(true);
			}
		};
		eActionRemoveCurrent.setEnabled(false);
		((AbstractAction) eActionRemoveCurrent).putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.ALT_MASK));
		eActionRemoveClusters = new AbstractAction("Remove Cluster/s")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				View.instance.setAnimated(false);
				clustering.chooseClustersToRemove();
				View.instance.setAnimated(true);
			}
		};
		eActionRemoveModels = new AbstractAction("Remove Compound/s")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				View.instance.setAnimated(false);
				clustering.chooseModelsToRemove();
				View.instance.setAnimated(true);
			}
		};
		MyMenu removeMenu = new MyMenu("Remove", eActionRemoveCurrent, eActionRemoveClusters, eActionRemoveModels);

		eActionExportCurrent = new AbstractAction("Export Selected Cluster/Compound")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int[] m = (int[]) ((AbstractAction) eActionRemoveCurrent).getValue("Model");
				Integer c = (Integer) ((AbstractAction) eActionRemoveCurrent).getValue("Cluster");
				if (m.length > 0)
					clustering.exportModels(m);
				else if (c != null)
					clustering.exportClusters(new int[] { c });
			}
		};
		eActionExportClusters = new AbstractAction("Export Cluster/s")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clustering.chooseClustersToExport();
			}
		};
		eActionExportModels = new AbstractAction("Export Compound/s")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clustering.chooseModelsToExport();
			}
		};
		MyMenu exportMenu = new MyMenu("Export", eActionExportCurrent, eActionExportClusters, eActionExportModels);
		MyMenu editMenu = new MyMenu("Edit", removeMenu, exportMenu);

		vActionFullScreen = new AbstractAction("Fullscreen ON/OFF")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				guiControler.setFullScreen(!guiControler.isFullScreen());
			}
		};
		((AbstractAction) vActionFullScreen).putValue(Action.ACCELERATOR_KEY,
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.ALT_MASK));
		MyMenu viewMenu = new MyMenu("View", vActionFullScreen);

		Action hActionAbout = new AbstractAction("About " + Settings.TITLE)
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showAboutDialog();
			}
		};
		MyMenu helpMenu = new MyMenu("Help", hActionAbout);

		menuBar = new MyMenuBar(fileMenu, editMenu, viewMenu, helpMenu);
	}

	public static void showAboutDialog()
	{
		JTextPane t = new JTextPane();
		t.setContentType("text/html");
		t.setText("<html><h5>"
				+ Settings.TITLE
				+ "</h5><table><tr><td>Version:</td><td>"
				+ Settings.VERSION_STRING
				+ "</td></tr><tr><td>Homepage:</td><td>"
				+ Settings.HOMEPAGE
				+ "</td></tr><tr><td>Contact:</td><td>Martin Gütlein (martin.guetlein@gmail.com)</td></tr></table></html>");
		MutableAttributeSet attrs = t.getInputAttributes();
		Font font = new JLabel().getFont();
		StyleConstants.setFontFamily(attrs, font.getFamily());
		StyleConstants.setFontSize(attrs, font.getSize());
		t.getStyledDocument().setCharacterAttributes(0, t.getText().length() + 1, attrs, true);
		t.setOpaque(false);
		JOptionPane.showMessageDialog(Settings.TOP_LEVEL_COMPONENT, t, "About " + Settings.TITLE,
				JOptionPane.INFORMATION_MESSAGE, Settings.CHES_MAPPER_IMAGE);
	}

	private void update()
	{

		int m[] = new int[0];
		Integer c = null;

		if (clustering.getClusterActive().getSelected() != -1)
		{
			if (clustering.getModelActive().getSelected() != -1)
				m = ArrayUtil.concat(m, clustering.getModelActive().getSelectedIndices());
			if (clustering.getModelWatched().getSelected() != -1
					&& ArrayUtil.indexOf(m, clustering.getModelWatched().getSelected()) == -1)
				m = ArrayUtil.concat(m, new int[] { clustering.getModelWatched().getSelected() });
		}
		else if (clustering.getClusterWatched().getSelected() != -1)
			c = clustering.getClusterWatched().getSelected();

		eActionRemoveCurrent.putValue("Cluster", c);
		eActionRemoveCurrent.putValue("Model", m);

		if (m.length > 0 || c != null)
		{
			if (m.length == 1)
			{
				((AbstractAction) eActionRemoveCurrent).putValue(Action.NAME, "Remove Compound " + m[0]);
				((AbstractAction) eActionExportCurrent).putValue(Action.NAME, "Export Compound " + m[0]);
			}
			else if (m.length > 1)
			{
				((AbstractAction) eActionRemoveCurrent).putValue(Action.NAME, "Remove " + m.length + " Compounds");
				((AbstractAction) eActionExportCurrent).putValue(Action.NAME, "Export " + m.length + " Compounds");
			}
			else if (c != -1)
			{
				((AbstractAction) eActionRemoveCurrent).putValue(Action.NAME, "Remove Cluster '"
						+ clustering.getCluster(c).getName() + "'");
				((AbstractAction) eActionExportCurrent).putValue(Action.NAME, "Export Cluster '"
						+ clustering.getCluster(c).getName() + "'");
			}
			eActionRemoveCurrent.setEnabled(true);
			eActionExportCurrent.setEnabled(true);
		}
		else
		{
			((AbstractAction) eActionRemoveCurrent).putValue(Action.NAME, "Remove Selected Cluster/Compound");
			eActionRemoveCurrent.setEnabled(false);

			((AbstractAction) eActionExportCurrent).putValue(Action.NAME, "Export Selected Cluster/Compound");
			eActionExportCurrent.setEnabled(false);

		}
	}

	private void installListeners()
	{
		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});
		clustering.getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});
		clustering.getModelWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});
		clustering.getModelActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});

	}

	private void newClustering(final int startPanel)
	{
		guiControler.block("new clustering");
		Thread noAWTThread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{
					final CheSMapperWizard wwd = new CheSMapperWizard((JFrame) SwingUtilities.getRoot(MenuBar.this),
							startPanel);
					Settings.TOP_LEVEL_COMPONENT = MenuBar.this.getTopLevelAncestor();
					SwingUtil.waitWhileVisible(wwd);
					if (wwd.isWorkflowSelected())
					{
						View.instance.setAnimated(false);

						clustering.clear();
						ClusteringData d = CheSViewer.doMapping(wwd);
						if (d != null)
						{
							clustering.newClustering(d);
							CheSViewer.finalizeTask();
						}

						View.instance.setAnimated(true);
					}
				}
				finally
				{
					guiControler.unblock("new clustering");
				}
			}
		});
		noAWTThread.start();
	}

	/**
	 * somehow the accelerate key registration does not work reliably, do that manually
	 * 
	 * @param e
	 */
	public void handleKeyEvent(KeyEvent e)
	{
		//		System.err.println("handle key event " + KeyEvent.getKeyText(e.getKeyCode()) + " "
		//				+ KeyEvent.getKeyModifiersText(e.getModifiers()) + " " + e.getKeyCode() + " " + e.getModifiers());
		for (Action action : menuBar.actions)
		{
			if (((AbstractAction) action).isEnabled())
			{
				KeyStroke k = (KeyStroke) action.getValue(Action.ACCELERATOR_KEY);
				if (k != null)
				{
					if (e.getKeyCode() == k.getKeyCode() && ((k.getModifiers() & e.getModifiers()) != 0))
					{
						//							System.err.println("perform " + action.toString());
						action.actionPerformed(new ActionEvent(this, -1, ""));
					}
					else
					{
						//							System.err.println("no match: " + KeyEvent.getKeyText(k.getKeyCode()) + " "
						//									+ KeyEvent.getKeyModifiersText(k.getModifiers()) + " " + k.getKeyCode() + " "
						//									+ k.getModifiers());
					}
				}
			}
		}
	}

	public static void main(String args[])
	{
		showAboutDialog();
	}

}
