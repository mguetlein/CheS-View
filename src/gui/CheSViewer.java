package gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import main.CheSMapping;
import main.Settings;
import main.TaskProvider;
import util.SwingUtil;
import cluster.Clustering;
import data.ClusteringData;

public class CheSViewer implements GUIControler
{
	//	static
	//	{
	//		System.err.println(JmolConstants.version);
	//	}

	JFrame frame;
	JPanel glass;
	JPanel coverPanel;

	ClusterPanel clusterPanel;
	Clustering clustering;
	MenuBar menuBar;

	boolean undecorated = false;
	Dimension oldSize;
	Point oldLocation;

	private static boolean DEBUG = false;
	List<String> block = new ArrayList<String>();

	List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

	public CheSViewer(ClusteringData clusteredDataset)
	{
		this(clusteredDataset, false);
	}

	public CheSViewer(ClusteringData clusteredDataset, boolean startNextToScreen)
	{
		glass = new JPanel();
		LayoutManager layout = new OverlayLayout(glass);
		glass.setLayout(layout);

		coverPanel = new JPanel();
		coverPanel.setOpaque(false);
		MouseAdapter listener = new MouseAdapter()
		{
		};
		coverPanel.addMouseListener(listener);
		coverPanel.addMouseMotionListener(listener);
		coverPanel.setVisible(false);
		coverPanel.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		glass.add(coverPanel);

		clusterPanel = new ClusterPanel(this);

		Dimension full = Toolkit.getDefaultToolkit().getScreenSize();
		oldSize = new Dimension(full.width - 100, full.height - 100);
		oldLocation = null;
		//		oldSize = new Dimension(1024, 768);
		//oldLocation = new Point(0, 0);
		if (startNextToScreen)
		{
			oldSize = new Dimension(1280, 1024);
			oldLocation = new Point(Toolkit.getDefaultToolkit().getScreenSize().width, 0);
		}

		clusterPanel.init(clusteredDataset);
		clustering = clusterPanel.getClustering();
		menuBar = new MenuBar(this, clusterPanel.getViewControler(), clustering);
		setFullScreen(false, true);
	}

	@Override
	public void setFullScreen(boolean b)
	{
		setFullScreen(b, false);
	}

	private void setFullScreen(boolean b, boolean force)
	{
		if (force || frame.isUndecorated() != b)
		{
			if (frame != null)
			{
				frame.dispose();
				// f.removeAll();
				frame.setVisible(false);
			}
			if (b)
			{
				oldSize = frame.getSize();
				oldLocation = frame.getLocation();
				show(b, Toolkit.getDefaultToolkit().getScreenSize(), new Point(0, 0));
			}
			else
			{
				show(b, oldSize, oldLocation);
			}
			fireEvent(PROPERTY_FULLSCREEN_CHANGED);
		}
	}

	@Override
	public boolean isFullScreen()
	{
		if (frame == null)
			return false;
		else
			return frame.isUndecorated();
	}

	public void show(boolean undecorated, Dimension size, Point location)
	{
		if (clustering == null)
			throw new Error("clustering is null");

		frame = new JFrame(Settings.TITLE + " (" + Settings.VERSION_STRING + ")");
		Settings.TOP_LEVEL_COMPONENT = frame;

		frame.setUndecorated(undecorated);

		if (!undecorated)
			frame.setJMenuBar(menuBar);

		frame.setSize(size);
		if (location == null)
			frame.setLocationRelativeTo(null);
		else
			frame.setLocation(location);

		frame.getContentPane().add(clusterPanel);

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(Settings.CHES_MAPPER_IMAGE.getImage());
		frame.setGlassPane(glass);
		glass.setVisible(true);
		glass.setOpaque(false);
		frame.setVisible(true);
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				clusterPanel.requestFocus();
			}
		});

		String msg;
		if (frame.isUndecorated())
			msg = "Press 'ALT+ENTER' to leave fullscreen mode";
		else
			msg = "Press 'ALT+ENTER' for fullscreen mode";
		clusterPanel.showMessage(msg);
	}

	public void block(String blocker)
	{
		if (clusterPanel == null)
			throw new Error("cluster panel not yet set");
		if (block.contains(blocker))
			throw new Error("already blocking for: " + blocker);
		block.add(blocker);
		if (DEBUG)
			System.out.println("BLOCK (" + block.size() + ") '" + blocker + "' ------------------");
		coverPanel.setVisible(true);
		coverPanel.requestFocus();
	}

	@Override
	public boolean isBlocked()
	{
		return coverPanel.isVisible();
	}

	public void unblock(String blocker)
	{
		if (!block.contains(blocker))
			throw new Error("use block first for " + blocker);

		block.remove(blocker);

		if (block.size() == 0)
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					coverPanel.setVisible(false);
					clusterPanel.requestFocus();
					if (DEBUG)
						System.out.println("---------------- UNBLOCK");
				}
			});
	}

	public static void main(String args[])
	{
		Locale.setDefault(Locale.US);
		boolean startNextToScreen = args.length > 1 && args[1].equals("true");

		if (args.length > 0 && args[0].equals("true"))
			start(CheSMapping.testWorkflow().doMapping(), startNextToScreen);
		else
		{
			ClusteringData clusteringData = null;
			while (clusteringData == null)
			{
				CheSMapperWizard wwd = new CheSMapperWizard(null);
				SwingUtil.waitWhileVisible(wwd);

				if (wwd.isWorkflowSelected())
					clusteringData = doMapping(wwd);
				else
					break;
			}
			if (clusteringData != null)
				start(clusteringData, startNextToScreen);
			else
				System.exit(1);
		}
	}

	public static ClusteringData doMapping(CheSMapperWizard wwd)
	{
		if (TaskProvider.exists())
			TaskProvider.clear();
		TaskProvider.registerThread("Ches-Mapper-Task");
		TaskProvider.task().showDialog(null, "Chemical space mapping");
		ClusteringData d = wwd.loadDataset();
		return d;
	}

	public static void finalizeTask()
	{
		TaskProvider.task().getDialog().setVisible(false);
		if (TaskProvider.task().containsWarnings())
			TaskProvider.task().showWarningDialog(Settings.TOP_LEVEL_COMPONENT, "Warning",
					"The following non-critical errors occured during the mapping:");
		TaskProvider.clear();
	}

	public static void start(ClusteringData clusteredDataset, boolean startNextToScreen)
	{
		new CheSViewer(clusteredDataset, startNextToScreen);
		finalizeTask();
	}

	@Override
	public JPopupMenu getPopup()
	{
		return menuBar.getPopup();
	}

	@Override
	public void handleKeyEvent(KeyEvent e)
	{
		menuBar.handleKeyEvent(e);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener l)
	{
		listeners.add(l);
	}

	private void fireEvent(String prop)
	{
		for (PropertyChangeListener ll : listeners)
			ll.propertyChange(new PropertyChangeEvent(this, prop, false, true));
	}

}