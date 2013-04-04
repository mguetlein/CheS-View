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

		JPanel allPanelsContainer = new JPanel(new BorderLayout());
		allPanelsContainer.setOpaque(false);
		clusterListPanel = new ClusterListPanel(mainPanel.getClustering(), mainPanel, guiControler);
		allPanelsContainer.add(clusterListPanel, BorderLayout.WEST);
		add(allPanelsContainer, BorderLayout.WEST);

		JPanel infoAndChartContainer = new JPanel(new BorderLayout(0, 20));
		infoAndChartContainer.setOpaque(false);

		InfoPanel infoPanel = new InfoPanel(mainPanel, mainPanel.getClustering(), guiControler);
		ChartPanel chartPanel = new ChartPanel(mainPanel.getClustering(), mainPanel, guiControler);
		chartPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		infoAndChartContainer.add(infoPanel, BorderLayout.EAST);

		JPanel chartContainer = new JPanel(new BorderLayout());
		chartContainer.setOpaque(false);
		chartContainer.add(chartPanel, BorderLayout.EAST);

		infoAndChartContainer.add(chartContainer, BorderLayout.SOUTH);
		infoAndChartContainer.setBorder(new EmptyBorder(25, 25, 25, 25));

		mainPanel.addIgnoreMouseMovementComponents(chartPanel);

		allPanelsContainer.add(infoAndChartContainer);

		//		SwingUtil.setDebugBorder(chartPanel, Color.LIGHT_GRAY);
		//		SwingUtil.setDebugBorder(allPanelsContainer, Color.RED);
		//		SwingUtil.setDebugBorder(infoAndChartContainer, Color.GREEN);
		//		SwingUtil.setDebugBorder(chartContainer, Color.ORANGE);
		//		SwingUtil.setDebugBorder(infoPanel, Color.BLUE);
		//		SwingUtil.setDebugBorder(clusterListPanel, Color.MAGENTA);

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
