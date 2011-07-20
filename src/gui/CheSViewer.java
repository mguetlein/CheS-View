package gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingUtilities;

import main.CheSMapping;
import main.Settings;
import cluster.Clustering;
import data.ClusteringData;

public class CheSViewer implements GUIControler
{
	public static Progressable initProgress;

	JFrame frame;
	JPanel glass;
	JPanel coverPanel;

	ClusterPanel clusterPanel;
	Clustering clustering;

	boolean undecorated = false;
	Dimension oldSize;
	Point oldLocation;

	public CheSViewer(ClusteringData clusteredDataset)
	{
		this(clusteredDataset, false);
	}

	public CheSViewer(ClusteringData clusteredDataset, boolean startNextToScreen)
	{

		glass = new JPanel();// (JPanel) getGlassPane();

		// CBListener listener = new CBListener();// aButton, menuBar, this,
		// contentPane);

		// glass.setVisible(true);
		LayoutManager layout = new OverlayLayout(glass);
		glass.setLayout(layout);

		// FormLayout l = new FormLayout("center:pref:grow",
		// "center:pref:grow");
		// messagePanel = new JPanel(l);// new Bor.setBackground(Color.BLACK);derLayout());
		// messagePanel.setOpaque(false);
		// messageLabel = ComponentFactory.createLabel();
		// messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		// messageLabel.setOpaque(true);
		// messageLabel.setBackground(new Color(0, 0, 0, 200));
		// messageLabel.setFont(messageLabel.getFont().deriveFont(24f));
		// CellConstraints cc = new CellConstraints();
		// messagePanel.add(messageLabel, cc.xy(1, 1));
		// messagePanel.setVisible(false);
		// glass.add(messagePanel);

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

		// JPanel p = new JPanel(new BorderLayout());
		// p.setOpaque(false);
		// p.add(new SideBar(mainPanel), BorderLayout.WEST);
		// JPanel pp = new JPanel(new BorderLayout());
		// pp.setOpaque(false);
		// pp.add(new ModelInfoPanel(mainPanel), BorderLayout.SOUTH);
		// pp.setBorder(new EmptyBorder(0, 0, 50, 0));
		// p.add(pp);
		// glass.add(p);

		clusterPanel = new ClusterPanel(this, clusteredDataset);
		clustering = clusterPanel.getClustering();

		// glass.add(new SideBar(mainPanel), BorderLayout.WEST);
		// JPanel pp = new JPanel(new BorderLayout());
		// pp.setOpaque(false);
		// pp.add(new ModelInfoPanel(mainPanel), BorderLayout.SOUTH);
		// pp.setBorder(new EmptyBorder(0, 0, 50, 0));
		// glass.add(pp);
		// glass.setBorder(new EmptyBorder(10, 0, 0, 0));

		Dimension full = Toolkit.getDefaultToolkit().getScreenSize();
		oldSize = new Dimension(full.width - 100, full.height - 100);
		oldLocation = null;
		//oldSize = new Dimension(1024, 768);
		//oldLocation = new Point(0, 0);
		if (startNextToScreen)
		{
			oldSize = new Dimension(1280, 1024);
			oldLocation = new Point(Toolkit.getDefaultToolkit().getScreenSize().width, 0);
		}
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
		}
	}

	public void show(boolean undecorated, Dimension size, Point location)
	{
		frame = new JFrame(Settings.TITLE + " (" + Settings.VERSION_STRING + ")");
		Settings.TOP_LEVEL_COMPONENT = frame;

		frame.setUndecorated(undecorated);

		if (!undecorated)
			frame.setJMenuBar(new MenuBar(this, clustering));

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
			msg = "Press 'ESCAPE' to leave fullscreen mode";
		else
			msg = "Press 'ALT+ENTER' for fullscreen mode";
		clusterPanel.showMessage(msg);
	}

	public void block()
	{
		// SwingUtilities.invokeLater(new Runnable()
		// {
		// @Override
		// public void run()
		// {

		System.out.println("BLOCK------------------");
		coverPanel.setVisible(true);
		coverPanel.requestFocus();
		// }
		// });
	}

	@Override
	public boolean isBlocked()
	{
		return coverPanel.isVisible();
	}

	public void unblock()
	{
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				coverPanel.setVisible(false);
				clusterPanel.requestFocus();
				System.out.println("----------------UNBLOCK");
			}
		});
	}

	public static void main(String args[])
	{
		Locale.setDefault(Locale.US);
		boolean startNextToScreen = args.length > 1 && args[1].equals("true");

		if (args.length > 0 && args[0].equals("true"))
			start(CheSMapping.testWorkflow().doMapping(null), startNextToScreen);
		else
		{
			ClusteringData clusteringData = null;
			while (clusteringData == null)
			{
				CheSMapperWizard wwd = new CheSMapperWizard(null);
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
		ProgressDialog progress = ProgressDialog.showProgress(null, "Chemical space mapping", 100);
		Progressable clusterProgress = SubProgress.create(progress, 0, 66);
		CheSViewer.initProgress = SubProgress.create(progress, 66, 100);
		return wwd.loadDataset(clusterProgress);
	}

	public static void start(ClusteringData clusteredDataset, boolean startNextToScreen)
	{
		new CheSViewer(clusteredDataset, startNextToScreen);
	}

}
