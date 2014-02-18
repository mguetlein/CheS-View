package gui;

import gui.swing.ComponentFactory;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.LayoutManager;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import main.ScreenSetup;
import util.StringUtil;
import util.ThreadUtil;
import cluster.ClusterController;
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
		messageLabel = ComponentFactory.createTransparentViewLabel();
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
		messageLabel.setOpaque(true);
		//		messageLabel.setBackground(Settings.TRANSPARENT_BACKGROUND);
		CellConstraints cc = new CellConstraints();
		messagePanel.add(messageLabel, cc.xy(1, 1));
		messagePanel.setVisible(false);
		add(messagePanel);

		JPanel allPanelsContainer = new JPanel(new BorderLayout());
		allPanelsContainer.setOpaque(false);
		ClusterListPanel clusterListPanel = new ClusterListPanel(mainPanel.getClustering(), mainPanel, mainPanel,
				guiControler);
		allPanelsContainer.add(clusterListPanel, BorderLayout.WEST);
		add(allPanelsContainer, BorderLayout.WEST);

		final int gap = 20;
		JPanel infoAndChartContainer = new JPanel(new BorderLayout(0, gap));
		infoAndChartContainer.setOpaque(false);

		final int top = 25;
		final int bottom = 25;

		final InfoPanel infoPanel = new InfoPanel(mainPanel, mainPanel, mainPanel.getClustering(), guiControler);
		final JPanel chartContainer = new JPanel(new BorderLayout())
		{
			@Override
			public Dimension getPreferredSize()
			{
				// to "push back" the table
				int increasedHeight = (ClusterPanel.this.getHeight() - (gap + top + bottom + 40))
						- infoPanel.getPreferredTableHeight();
				if (increasedHeight < 0)
					return super.getPreferredSize();
				else
					return new Dimension(10, Math.max(super.getPreferredSize().height, increasedHeight));
			}
		};
		ChartPanel chartPanel = new ChartPanel(mainPanel.getClustering(), mainPanel, mainPanel, guiControler);
		chartPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
		chartContainer.setOpaque(false);
		JPanel chartWrapperPanel = new JPanel(new BorderLayout());
		chartWrapperPanel.setOpaque(false);
		chartWrapperPanel.add(chartPanel, BorderLayout.EAST);
		chartContainer.add(chartWrapperPanel, BorderLayout.SOUTH);

		infoAndChartContainer.add(infoPanel, BorderLayout.EAST);
		infoAndChartContainer.add(chartContainer, BorderLayout.SOUTH);
		infoAndChartContainer.setBorder(new EmptyBorder(top, 25, bottom, 25));

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
	}

	public void init(ClusteringData clusteredDataset)
	{
		mainPanel.init(clusteredDataset);
	}

	public Clustering getClustering()
	{
		return mainPanel.getClustering();
	}

	public void showMessage(final String msg)
	{
		Thread th = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				messageLabel.setFont(messageLabel.getFont().deriveFont(ScreenSetup.INSTANCE.getFontSize() + 4f));
				messageLabel.setText(msg);
				messageLabel.setPreferredSize(null);
				messagePanel.setVisible(true);
				// show message between 2 and 6 seconds depending on the number of words (<=3 words 2s, 7 words 4.5s, >=9 words 6000)
				long sleep = Math.max(2000, Math.min(6000, StringUtil.numOccurences(msg, " ") * 750));
				ThreadUtil.sleep(sleep);
				if (msg.equals(messageLabel.getText()))
					messagePanel.setVisible(false);
			}
		});
		th.start();
	}

	public ViewControler getViewControler()
	{
		return mainPanel;
	}

	public ClusterController getClusterControler()
	{
		return mainPanel;
	}

}
