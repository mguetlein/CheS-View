package gui;

import gui.swing.ComponentFactory;

import java.awt.BorderLayout;
import java.awt.LayoutManager;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
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
	private ClusterListPanel clusterListPanel;

	public ClusterPanel(GUIControler guiControler)
	{
		this.guiControler = guiControler;

		mainPanel = new MainPanel(guiControler);

		LayoutManager layout = new OverlayLayout(this);
		setLayout(layout);

		FormLayout l = new FormLayout("center:pref:grow", "center:pref:grow");
		messagePanel = new JPanel(l);// new BorderLayout());
		messagePanel.setOpaque(false);
		messageLabel = ComponentFactory.createTransparentViewLabel();
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		messageLabel.setOpaque(true);
		//		messageLabel.setBackground(Settings.TRANSPARENT_BACKGROUND);
		messageLabel.setFont(messageLabel.getFont().deriveFont(24f));
		CellConstraints cc = new CellConstraints();
		messagePanel.add(messageLabel, cc.xy(1, 1));
		messagePanel.setVisible(false);
		add(messagePanel);

		JPanel sideBarContainer = new JPanel(new BorderLayout());
		sideBarContainer.setOpaque(false);
		clusterListPanel = new ClusterListPanel(mainPanel.getClustering(), mainPanel);
		sideBarContainer.add(clusterListPanel, BorderLayout.WEST);
		add(sideBarContainer, BorderLayout.WEST);

		JPanel pp = new JPanel(new BorderLayout());
		pp.setOpaque(false);

		InfoPanel infoPanel = new InfoPanel(mainPanel, mainPanel.getClustering());
		JPanel ppp = new JPanel(new BorderLayout());
		ppp.setOpaque(false);
		ppp.add(infoPanel, BorderLayout.NORTH);
		ppp.setBorder(new EmptyBorder(0, 0, 10, 0));

		JPanel infoContainer = new JPanel(new BorderLayout());
		infoContainer.setOpaque(false);
		infoContainer.add(ppp, BorderLayout.CENTER);
		pp.add(infoContainer, BorderLayout.EAST);
		pp.setBorder(new EmptyBorder(25, 25, 25, 25));

		ChartPanel cp = new ChartPanel(mainPanel.getClustering(), mainPanel);
		cp.setBorder(new EmptyBorder(0, 0, 0, 0));
		infoContainer.add(cp, BorderLayout.SOUTH);

		mainPanel.addIgnoreMouseMovementComponents(cp);

		sideBarContainer.add(pp);

		//		SwingUtil.setDebugBorder(cp, Color.LIGHT_GRAY);
		//		SwingUtil.setDebugBorder(sideBarContainer, Color.RED);
		//		SwingUtil.setDebugBorder(pp, Color.GREEN);
		//		SwingUtil.setDebugBorder(sideBar, Color.ORANGE);
		//		SwingUtil.setDebugBorder(info, Color.BLUE);
		//		SwingUtil.setDebugBorder(ppp, Color.MAGENTA);
		//		SwingUtil.setDebugBorder(infoContainer, Color.CYAN);

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
			@Override
			public void keyPressed(KeyEvent e)
			{
				Actions.getInstance(guiControler, mainPanel, mainPanel.getClustering()).performActions(e.getSource(),
						KeyStroke.getKeyStrokeForEvent(e));
				//				guiControler.handleKeyEvent(e);
			}

			@Override
			public void keyReleased(KeyEvent e)
			{
				Actions.getInstance(guiControler, mainPanel, mainPanel.getClustering()).performActions(e.getSource(),
						KeyStroke.getKeyStrokeForEvent(e));
			}
		};
		addKeyListener(listener);
		mainPanel.addKeyListener(listener);
		mainPanel.jmolPanel.addKeyListener(listener);
		clusterListPanel.controlPanel.highlightCombobox.addKeyListener(listener);
		clusterListPanel.controlPanel.labelCheckbox.addKeyListener(listener);
		clusterListPanel.controlPanel.highlightMinMaxCombobox.addKeyListener(listener);
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
					Settings.LOGGER.error(e);
				}
				messagePanel.setVisible(false);
			}
		});
		th.start();
	}

	public ViewControler getViewControler()
	{
		return mainPanel;
	}

}
