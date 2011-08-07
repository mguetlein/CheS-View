package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import main.Settings;
import util.SwingUtil;
import cluster.Clustering;
import data.ClusteringData;

public class MenuBar extends JMenuBar
{
	GUIControler guiControler;
	Clustering clustering;
	JMenuItem removeSelectedClusterItem;
	JMenuItem exportSelectedClusterItem;
	JMenuItem removeSelectedModelItem;
	JMenuItem exportSelectedModelItem;

	public MenuBar(GUIControler guiControler, Clustering clustering)
	{
		this.guiControler = guiControler;
		this.clustering = clustering;
		buildMenu();
		installListeners();
		update();
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
		clustering.addRemoveAddClusterListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});

	}

	private void newClustering(int startPanel)
	{
		final CheSMapperWizard wwd = new CheSMapperWizard((JFrame) SwingUtilities.getRoot(this), startPanel);
		Settings.TOP_LEVEL_COMPONENT = MenuBar.this.getTopLevelAncestor();
		if (wwd.isWorkflowSelected())
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					clustering.clear();
				}
			});
			Thread th = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					SwingUtil.waitForAWTEventThread();
					clustering.invokeAfterViewer(null);
					ClusteringData d = CheSViewer.doMapping(wwd);
					if (d != null)
					{
						clustering.newClustering(d);
					}
					guiControler.unblock();
				}
			});
			th.start();
		}
	}

	private void buildMenu()
	{
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(createItem("New dataset / mapping", new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				newClustering(0);
			}
		}));

		fileMenu.add(createItem("Remove clusters", new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clustering.chooseClustersToRemove();
			}
		}));

		fileMenu.add(createItem("Export dataset", new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				clustering.chooseClustersToExport();
			}
		}));

		//		JMenu addClusterWorkflowMenu = new JMenu("Change current mapping settings");
		//		String dialogPanels[] = { "Dataset", "3D-Structure", "Features", "Clustering", "3D-Embedding", "3D-Alignement" };
		//		int pCount = 0;
		//		for (String panel : dialogPanels)
		//		{
		//			final int finalPCount = pCount++;
		//			addClusterWorkflowMenu.add(createItem(panel, new ActionListener()
		//			{
		//				@Override
		//				public void actionPerformed(ActionEvent e)
		//				{
		//					newClustering(finalPCount);
		//				}
		//			}));
		//		}
		//		fileMenu.add(addClusterWorkflowMenu);
		fileMenu.addSeparator();
		fileMenu.add(createItem("Exit", new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		}));
		add(fileMenu);

		//		for (final String data : ClusteredDatasetWorflow.availableClusteredDatasets()) // SdfProvider.CLUSTER_SETS)
		//		{
		//			addClusterSetMenu.add(createItem(data, new ActionListener()
		//			{
		//				@Override
		//				public void actionPerformed(ActionEvent e)
		//				{
		//					guiControler.block();
		//					clustering.clear();
		//					// to clear screen first, than do loading in awt thread to
		//					// not show intermediate results
		//					SwingUtilities.invokeLater(new Runnable()
		//					{
		//						@Override
		//						public void run()
		//						{
		//							clustering.addCluster(ClusteredDatasetWorflow.getDataset(data));// SdfProvider.getClusterSet(data));
		//
		//							guiControler.unblock();
		//						}
		//					});
		//				}
		//			}));
		//		}

		//		JMenu editMenu = new JMenu("Edit");
		//		exportSelectedClusterItem = createItem("Export selected cluster", new ActionListener()
		//		{
		//			@Override
		//			public void actionPerformed(ActionEvent e)
		//			{
		//			}
		//		});
		//		exportSelectedModelItem = createItem("Export selected model", new ActionListener()
		//		{
		//			@Override
		//			public void actionPerformed(ActionEvent e)
		//			{
		//			}
		//		});
		//		removeSelectedClusterItem = createItem("Remove selected cluster", new ActionListener()
		//		{
		//			@Override
		//			public void actionPerformed(ActionEvent e)
		//			{
		//				clustering.removeSelectedCluster();
		//			}
		//		});
		//		removeSelectedModelItem = createItem("Remove selected model", new ActionListener()
		//		{
		//			@Override
		//			public void actionPerformed(ActionEvent e)
		//			{
		//				clustering.removeSelectedModel();
		//			}
		//		});
		//		editMenu.add(exportSelectedClusterItem);
		//		editMenu.add(exportSelectedModelItem);
		//		editMenu.addSeparator();
		//		editMenu.add(removeSelectedClusterItem);
		//		editMenu.add(removeSelectedModelItem);
		//		add(editMenu);

		JMenu viewMenu = new JMenu("View");
		viewMenu.add(createItem("Fullscreen    [ALT+ENTER]", new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				guiControler.setFullScreen(true);
			}
		}));
		add(viewMenu);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.setEnabled(false);
		add(helpMenu);
	}

	private void update()
	{
		//		boolean clusterSelected = clustering.getClusterActive().getSelected() != -1
		//				|| clustering.getClusterWatched().getSelected() != -1;
		//		removeSelectedClusterItem.setEnabled(clusterSelected);
		//		exportSelectedClusterItem.setEnabled(clusterSelected);
		//
		//		boolean modelSelected = clustering.getClusterActive().getSelected() != -1
		//				&& clustering.getModelWatched().getSelected() != -1;
		//		removeSelectedModelItem.setEnabled(modelSelected);
		//		exportSelectedModelItem.setEnabled(modelSelected);
	}

	public static JMenuItem createItem(String text, ActionListener l)
	{
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(l);
		return item;
	}
}
