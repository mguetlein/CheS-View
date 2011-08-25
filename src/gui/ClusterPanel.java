package gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.LayoutManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import main.Settings;
import cluster.Clustering;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import data.ClusteringData;

public class ClusterPanel extends JPanel
{
	GUIControler guiControler;
	JPanel messagePanel;
	JLabel messageLabel;
	private MainPanel mainPanel;

	public ClusterPanel(GUIControler guiControler)
	{
		this.guiControler = guiControler;

		mainPanel = new MainPanel(guiControler);

		LayoutManager layout = new OverlayLayout(this);
		setLayout(layout);

		FormLayout l = new FormLayout("center:pref:grow", "center:pref:grow");
		messagePanel = new JPanel(l);// new BorderLayout());
		messagePanel.setOpaque(false);
		messageLabel = ComponentFactory.createLabel();
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		messageLabel.setOpaque(true);
		messageLabel.setBackground(Settings.TRANSPARENT_BACKGROUND);
		messageLabel.setFont(messageLabel.getFont().deriveFont(24f));
		CellConstraints cc = new CellConstraints();
		messagePanel.add(messageLabel, cc.xy(1, 1));
		messagePanel.setVisible(false);
		add(messagePanel);

		JPanel sideBarContainer = new JPanel(new BorderLayout());
		sideBarContainer.setOpaque(false);
		SideBar sideBar = new SideBar(mainPanel.getClustering(), mainPanel);
		sideBarContainer.add(sideBar, BorderLayout.WEST);
		add(sideBarContainer, BorderLayout.WEST);

		JPanel pp = new JPanel(new BorderLayout());
		pp.setOpaque(false);
		InfoPanel info = new InfoPanel(mainPanel.getClustering());
		JPanel infoContainer = new JPanel(new BorderLayout());
		infoContainer.setOpaque(false);
		infoContainer.add(info, BorderLayout.WEST);
		pp.add(infoContainer, BorderLayout.SOUTH);
		pp.setBorder(new EmptyBorder(0, 0, 25, 0));

		sideBarContainer.add(pp);
		//add(pp);
		//		SwingUtil.setDebugBorder(sideBar);
		//		SwingUtil.setDebugBorder(info);
		add(mainPanel);
		setOpaque(false);
		installListeners();
	}

	public void init(ClusteringData clusteredDataset)
	{
		mainPanel.init(clusteredDataset);
	}

	public Clustering getClustering()
	{
		return mainPanel.getClustering();
	}

	private void installListeners()
	{
		KeyListener listener = new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyCode() == KeyEvent.VK_MINUS))
				{
					if (e.getKeyCode() == KeyEvent.VK_PLUS)
						mainPanel.zoomFactor += 0.1f;
					if (e.getKeyCode() == KeyEvent.VK_MINUS)
						mainPanel.zoomFactor -= 0.1f;
				}

				if (e.getKeyCode() == KeyEvent.VK_F4 && e.isAltDown())
				{
					Container anc = getTopLevelAncestor();
					if (anc instanceof JFrame)
						((JFrame) anc).setVisible(false);
					System.exit(0);
				}
				if (e.getKeyCode() == KeyEvent.VK_ENTER && e.isAltDown())
				{
					guiControler.setFullScreen(true);
				}
				if (e.getKeyCode() == KeyEvent.VK_O && e.isControlDown())
				{
					// if (e.isShiftDown()) // DEBUG HACK
					// mainPanel
					// .getClustering()
					// .addCluster(
					// "C:\\Users\\martin\\workspace\\ClusterViewer\\cox2_3d_WithReals\\thresh_0.6\\cluster1.0.sdf");
					// else

					//					mainPanel.getClustering().chooseClustersToAdd();
				}
				if (e.getKeyCode() == KeyEvent.VK_DELETE && e.isControlDown())
				{
					if (e.isShiftDown())
					{
						// mainPanel.getClustering().chooseClustersToRemove();
					}
					else
						mainPanel.getClustering().removeSelectedCluster();
				}
				if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				{
					guiControler.setFullScreen(false);
				}
				if (e.getKeyCode() == KeyEvent.VK_C && e.isControlDown())
				{
					mainPanel.getClustering().clear();
				}
				//				if (e.getKeyCode() == KeyEvent.VK_SPACE)
				//				{
				//					mainPanel.toggleSelectedCluster();
				//				}

			}
		};
		addKeyListener(listener);
		mainPanel.addKeyListener(listener);
		mainPanel.jmolPanel.addKeyListener(listener);
	}

	public void showMessage(final String msg)
	{
		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				messageLabel.setText(msg);
				messagePanel.setVisible(true);
				try
				{
					Thread.sleep(2000);
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				messagePanel.setVisible(false);
			}
		});
		th.start();
	}

}
