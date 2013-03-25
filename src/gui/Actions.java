package gui;

import gui.MainPanel.HighlightMode;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import main.Settings;
import main.TaskProvider;
import task.Task;
import task.TaskDialog;
import util.ArrayUtil;
import util.SwingUtil;
import workflow.MappingWorkflow;
import cluster.Clustering;
import cluster.ExportData;
import data.ClusteringData;
import dataInterface.MoleculeProperty;

public class Actions
{
	private static Actions instance;

	public static Actions getInstance(GUIControler guiControler, ViewControler viewControler, Clustering clustering)
	{
		if (instance == null)
			instance = new Actions(guiControler, viewControler, clustering);
		return instance;
	}

	private GUIControler guiControler;
	private ViewControler viewControler;
	private Clustering clustering;

	private Action[] allActions;

	//file
	private Action fActionNew;
	private Action fActionExit;
	//edit
	///remove
	private Action rActionRemoveCurrent;
	private Action rActionRemoveClusters;
	private Action rActionRemoveModels;
	///export
	private Action eActionExportCurrent;
	private Action eActionExportClusters;
	private Action eActionExportModels;
	private Action eActionExportImage;
	private Action eActionExportWorkflow;
	//view
	private Action vActionFullScreen;
	private Action vActionDrawHydrogens;
	private Action vActionHideUnselectedCompounds;
	private Action vActionSpin;
	private Action vActionBlackWhite;
	private Action vActionMoleculeDescriptor;
	///highlight
	private Action tActionHighlightLog;
	private Action tActionHighlightLastSelectedFeature;
	private Action tActionColorMatch;
	private Action tActionToggleHighlightMode;
	private Action tActionDecreaseSphereSize;
	private Action tActionIncreaseSphereSize;
	private Action tActionDecreaseSphereTranslucency;
	private Action tActionIncreaseSphereTranslucency;
	//help
	private Action hActionDocu;
	private Action hActionAbout;

	//hidden
	private Action xActionUpdateMouseSelectionPressed;
	private Action xActionUpdateMouseSelectionReleased;
	private Action xActionDecreaseCompoundSize;
	private Action xActionIncreaseCompoundSize;

	private Action xActionDisablePopup;

	private Actions(GUIControler guiControler, ViewControler viewControler, Clustering clustering)
	{
		this.guiControler = guiControler;
		this.viewControler = viewControler;
		this.clustering = clustering;

		buildActions();
		allActions = ArrayUtil.concat(Action.class, getFileActions(), getViewActions(), getRemoveActions(),
				getExportActions(), getHelpActions(), getHiddenActions(), getHighlightActions());
		setAccelerators();
		update();
		installListeners();
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
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED))
				{
					tActionIncreaseSphereSize.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
					tActionDecreaseSphereSize.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
					tActionIncreaseSphereTranslucency.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
					tActionDecreaseSphereTranslucency.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
				}
			}
		});
	}

	private void update()
	{
		int m[] = new int[0];
		Integer c = null;

		if (clustering.isClusterActive())
		{
			if (clustering.isModelActive())
				m = ArrayUtil.concat(m, clustering.getModelActive().getSelectedIndices());
			if (clustering.isModelWatched() && ArrayUtil.indexOf(m, clustering.getModelWatched().getSelected()) == -1)
				m = ArrayUtil.concat(m, new int[] { clustering.getModelWatched().getSelected() });
		}
		else if (clustering.isClusterWatched())
			c = clustering.getClusterWatched().getSelected();

		rActionRemoveCurrent.putValue("Cluster", c);
		rActionRemoveCurrent.putValue("Model", m);

		if (m.length > 0 || c != null)
		{
			if (m.length == 1)
			{
				((AbstractAction) rActionRemoveCurrent).putValue(Action.NAME,
						"Remove " + clustering.getModelWithModelIndex(m[0]));
				((AbstractAction) eActionExportCurrent).putValue(Action.NAME,
						"Export " + clustering.getModelWithModelIndex(m[0]));
			}
			else if (m.length > 1)
			{
				((AbstractAction) rActionRemoveCurrent).putValue(Action.NAME, "Remove " + m.length + " Compounds");
				((AbstractAction) eActionExportCurrent).putValue(Action.NAME, "Export " + m.length + " Compounds");
			}
			else if (c != -1)
			{
				((AbstractAction) rActionRemoveCurrent).putValue(Action.NAME, "Remove "
						+ clustering.getCluster(c).getName());
				((AbstractAction) eActionExportCurrent).putValue(Action.NAME, "Export "
						+ clustering.getCluster(c).getName());
			}
			rActionRemoveCurrent.setEnabled(true);
			eActionExportCurrent.setEnabled(true);
		}
		else
		{
			((AbstractAction) rActionRemoveCurrent).putValue(Action.NAME, "Remove Selected Cluster/Compound");
			rActionRemoveCurrent.setEnabled(false);

			((AbstractAction) eActionExportCurrent).putValue(Action.NAME, "Export Selected Cluster/Compound");
			eActionExportCurrent.setEnabled(false);
		}
	}

	private KeyStroke getKeyStroke(Action a)
	{
		if (a == fActionNew)
			return KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK);
		if (a == fActionExit)
			return KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK);
		if (a == rActionRemoveCurrent)
			return KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.ALT_MASK);
		if (a == rActionRemoveClusters)
			return null;
		if (a == rActionRemoveModels)
			return null;
		if (a == eActionExportCurrent)
			return null;
		if (a == eActionExportClusters)
			return null;
		if (a == eActionExportModels)
			return null;
		if (a == eActionExportImage)
			return KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK);
		if (a == eActionExportWorkflow)
			return KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.ALT_MASK);
		if (a == vActionFullScreen)
			return KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.ALT_MASK);
		if (a == vActionDrawHydrogens)
			return KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK);
		if (a == vActionHideUnselectedCompounds)
			return KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK);
		if (a == vActionSpin)
			return KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK);
		if (a == vActionBlackWhite)
			return KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK);
		if (a == tActionColorMatch)
			return KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK);
		if (a == vActionMoleculeDescriptor)
			return KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK);
		if (a == hActionDocu)
			return null;
		if (a == hActionAbout)
			return null;
		if (a == xActionDisablePopup)
			return KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK);
		if (a == xActionUpdateMouseSelectionPressed)
			return KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, ActionEvent.SHIFT_MASK);
		if (a == xActionUpdateMouseSelectionReleased)
			return KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true);
		if (a == tActionHighlightLog)
			return KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK);
		if (a == tActionHighlightLastSelectedFeature)
			return KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK);
		if (a == tActionToggleHighlightMode)
			return KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK);
		if (a == xActionDecreaseCompoundSize)
			return KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK);
		if (a == xActionIncreaseCompoundSize)
			return KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK);
		if (a == tActionDecreaseSphereSize)
			return KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK);
		if (a == tActionIncreaseSphereSize)
			return KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK);
		if (a == tActionIncreaseSphereTranslucency)
			return KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);
		if (a == tActionDecreaseSphereTranslucency)
			return KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK);
		throw new Error("key stroke not yet defined");
	}

	private void setAccelerators()
	{
		for (Action a : allActions)
		{
			KeyStroke k2 = getKeyStroke(a);
			if (k2 != null)
				((AbstractAction) a).putValue(Action.ACCELERATOR_KEY, k2);
		}
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
		fActionExit = new AbstractAction("Exit")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				System.exit(0);
			}
		};
		rActionRemoveCurrent = new AbstractAction("Remove Selected Cluster/Compound")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int[] m = (int[]) ((AbstractAction) rActionRemoveCurrent).getValue("Model");
				Integer c = (Integer) ((AbstractAction) rActionRemoveCurrent).getValue("Cluster");
				View.instance.suspendAnimation("remove selected");
				if (m.length > 0)
					clustering.removeModels(m);
				else if (c != null)
					clustering.removeCluster(c);
				View.instance.proceedAnimation("remove selected");
			}
		};
		rActionRemoveCurrent.setEnabled(false);
		rActionRemoveClusters = new AbstractAction("Remove Cluster/s")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				View.instance.suspendAnimation("remove clusters");
				clustering.chooseClustersToRemove();
				View.instance.proceedAnimation("remove clusters");
			}
		};
		rActionRemoveModels = new AbstractAction("Remove Compound/s")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				View.instance.suspendAnimation("remove compounds");
				clustering.chooseModelsToRemove();
				View.instance.proceedAnimation("remove compounds");
			}
		};

		eActionExportCurrent = new AbstractAction("Export Selected Cluster/Compound")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				int[] m = (int[]) ((AbstractAction) rActionRemoveCurrent).getValue("Model");
				Integer c = (Integer) ((AbstractAction) rActionRemoveCurrent).getValue("Cluster");
				if (m.length > 0)
					ExportData.exportModels(clustering, m);
				else if (c != null)
					ExportData.exportClusters(clustering, new int[] { c });
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
		eActionExportImage = new AbstractAction("Export Image")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				View.instance.exportImage();
			}
		};
		eActionExportWorkflow = new AbstractAction("Export Workflow")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				MappingWorkflow.exportMappingWorkflowToFile(MappingWorkflow.exportSettingsToMappingWorkflow());
			}
		};

		vActionFullScreen = new AbstractAction("Fullscreen mode enabled")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				guiControler.setFullScreen((Boolean) vActionFullScreen.getValue(Action.SELECTED_KEY));
			}
		};
		vActionFullScreen.putValue(Action.SELECTED_KEY, guiControler.isFullScreen());
		guiControler.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(GUIControler.PROPERTY_FULLSCREEN_CHANGED))
				{
					vActionFullScreen.putValue(Action.SELECTED_KEY, guiControler.isFullScreen());
				}
			}
		});
		vActionDrawHydrogens = new AbstractAction("Draw hydrogens (if available)")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setHideHydrogens((Boolean) vActionDrawHydrogens.getValue(Action.SELECTED_KEY));
			}
		};
		vActionDrawHydrogens.putValue(Action.SELECTED_KEY, !viewControler.isHideHydrogens());
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_SHOW_HYDROGENS))
				{
					vActionDrawHydrogens.putValue(Action.SELECTED_KEY, !viewControler.isHideHydrogens());
				}
			}
		});

		vActionHideUnselectedCompounds = new AbstractAction("Hide unselected compounds")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setHideUnselected((Boolean) vActionHideUnselectedCompounds.getValue(Action.SELECTED_KEY));
			}
		};
		vActionHideUnselectedCompounds.putValue(Action.SELECTED_KEY, viewControler.isHideUnselected());
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_HIDE_UNSELECT_CHANGED))
				{
					vActionHideUnselectedCompounds.putValue(Action.SELECTED_KEY, viewControler.isHideUnselected());
				}
			}
		});

		vActionSpin = new AbstractAction("Spin enabled")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setSpinEnabled((Boolean) vActionSpin.getValue(Action.SELECTED_KEY));
			}
		};
		vActionSpin.putValue(Action.SELECTED_KEY, viewControler.isSpinEnabled());
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_SPIN_CHANGED))
				{
					vActionSpin.putValue(Action.SELECTED_KEY, viewControler.isSpinEnabled());
				}
			}
		});

		vActionBlackWhite = new AbstractAction("Background color black")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setBlackgroundBlack((Boolean) vActionBlackWhite.getValue(Action.SELECTED_KEY));
			}
		};
		vActionBlackWhite.putValue(Action.SELECTED_KEY, viewControler.isBlackgroundBlack());
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_BACKGROUND_CHANGED))
				{
					vActionBlackWhite.putValue(Action.SELECTED_KEY, viewControler.isBlackgroundBlack());
				}
			}
		});

		vActionMoleculeDescriptor = new AbstractAction("Compound identifier")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				List<MoleculeProperty> props = new ArrayList<MoleculeProperty>();
				props.add(ViewControler.COMPOUND_INDEX_PROPERTY);
				props.add(ViewControler.COMPOUND_SMILES_PROPERTY);
				for (MoleculeProperty moleculeProperty : clustering.getProperties())
					props.add(moleculeProperty);
				for (MoleculeProperty moleculeProperty : clustering.getFeatures())
					if (!props.contains(moleculeProperty))
						props.add(moleculeProperty);
				MoleculeProperty selected = viewControler.getMoleculeDescriptor();
				MoleculeProperty p = SwingUtil.selectFromListWithDialog(props, selected, "Set compound identifier",
						Settings.TOP_LEVEL_FRAME);
				if (p != null)
					viewControler.setMoleculeDescriptor(p);
			}
		};

		hActionDocu = new AbstractAction("Online Documentation")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					Desktop.getDesktop().browse(new URI(Settings.HOMEPAGE_DOCUMENTATION));
				}
				catch (Exception ex)
				{
					Settings.LOGGER.error(ex);
				}
			}
		};
		hActionAbout = new AbstractAction("About " + Settings.TITLE)
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				showAboutDialog();
			}
		};

		xActionDisablePopup = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				View.instance.scriptWait("set disablePopupMenu off");
			}
		};

		xActionUpdateMouseSelectionPressed = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.updateMouseSelection(true);
			}
		};
		xActionUpdateMouseSelectionReleased = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.updateMouseSelection(false);
			}
		};

		xActionDecreaseCompoundSize = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (viewControler.canChangeCompoundSize(false))
					viewControler.changeCompoundSize(false);
			}
		};
		xActionIncreaseCompoundSize = new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (viewControler.canChangeCompoundSize(true))
					viewControler.changeCompoundSize(true);
			}
		};

		tActionColorMatch = new AbstractAction("Substructure match color")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				Color col = JColorChooser.showDialog(Settings.TOP_LEVEL_FRAME, "Select Color",
						(Color) tActionColorMatch.getValue("matchcolor"));
				if (col != null)
				{
					tActionColorMatch.putValue("matchcolor", col);
					viewControler.setMatchColor((Color) tActionColorMatch.getValue("matchcolor"));
				}
			}
		};
		tActionColorMatch.putValue("matchcolor", viewControler.getMatchColor());
		viewControler.addViewListener(new PropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(ViewControler.PROPERTY_MATCH_COLOR_CHANGED))
				{
					tActionColorMatch.putValue("matchcolor", viewControler.getMatchColor());
				}
			}
		});

		tActionHighlightLog = new AbstractAction("Toggle highlight log on/off")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setHighlightLogEnabled(!viewControler.getHighlightLogEnabled());
			}
		};
		tActionHighlightLastSelectedFeature = new AbstractAction("Highlight last selected feature")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler.setSelectLastSelectedHighlighter();
			}
		};
		tActionToggleHighlightMode = new AbstractAction("Toggle feature highlight mode (Spheres/Colored compounds)")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				viewControler
						.setHighlightMode(viewControler.getHighlightMode() == MainPanel.HighlightMode.ColorCompounds ? MainPanel.HighlightMode.Spheres
								: MainPanel.HighlightMode.ColorCompounds);
			}
		};

		tActionDecreaseSphereSize = new AbstractAction("Decrease Highlight Sphere Size")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (View.instance.sphereSize >= 0.1)
					viewControler.setSphereSize(View.instance.sphereSize - 0.1);
				else
					viewControler.setSphereSize(0);
			}
		};
		tActionIncreaseSphereSize = new AbstractAction("Increase Highlight Sphere Size")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (View.instance.sphereSize <= 0.9)
					viewControler.setSphereSize(View.instance.sphereSize + 0.1);
				else
					viewControler.setSphereSize(1);
			}
		};
		tActionDecreaseSphereTranslucency = new AbstractAction("Decrease Highlight Sphere Translucency")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (View.instance.sphereTranslucency >= 0.1)
					viewControler.setSphereTranslucency(View.instance.sphereTranslucency - 0.1);
				else
					viewControler.setSphereTranslucency(0);
			}
		};
		tActionIncreaseSphereTranslucency = new AbstractAction("Increase Highlight Sphere Translucency")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				if (View.instance.sphereTranslucency <= 0.9)
					viewControler.setSphereTranslucency(View.instance.sphereTranslucency + 0.1);
				else
					viewControler.setSphereTranslucency(1);
			}
		};
		tActionIncreaseSphereSize.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
		tActionDecreaseSphereSize.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
		tActionIncreaseSphereTranslucency.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
		tActionDecreaseSphereTranslucency.setEnabled(viewControler.getHighlightMode() == HighlightMode.Spheres);
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
					JFrame top = Settings.TOP_LEVEL_FRAME;
					final CheSMapperWizard wwd = new CheSMapperWizard(top, startPanel);
					wwd.setCloseButtonText("Cancel");
					Settings.TOP_LEVEL_FRAME = top;
					SwingUtil.waitWhileVisible(wwd);
					if (wwd.isWorkflowSelected())
					{
						View.instance.suspendAnimation("remap");
						clustering.clear();
						Task task = TaskProvider.initTask("Chemical space mapping");
						new TaskDialog(task, Settings.TOP_LEVEL_FRAME);
						ClusteringData d = wwd.doMapping();
						if (d != null)
						{
							clustering.newClustering(d);
							task.finish();
						}
						TaskProvider.removeTask();
						View.instance.proceedAnimation("remap");
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

	public static void showAboutDialog()
	{
		TextPanel p = new TextPanel();
		p.addHeading(Settings.TITLE);
		p.addTable(new String[][] { { "Version:", Settings.VERSION_STRING }, { "Homepage:", Settings.HOMEPAGE },
				{ "Contact:", Settings.CONTACT } });
		p.setPreferredWith(600);
		JOptionPane.showMessageDialog(Settings.TOP_LEVEL_FRAME, p, "About " + Settings.TITLE,
				JOptionPane.INFORMATION_MESSAGE, Settings.CHES_MAPPER_IMAGE);
	}

	public Action[] getFileActions()
	{
		return new Action[] { fActionNew, fActionExit };
	}

	public Action[] getExportActions()
	{
		return new Action[] { eActionExportCurrent, eActionExportClusters, eActionExportModels, eActionExportImage,
				eActionExportWorkflow };
	}

	public Action[] getRemoveActions()
	{
		return new Action[] { rActionRemoveCurrent, rActionRemoveClusters, rActionRemoveModels };
	}

	public Action[] getViewActions()
	{
		return new Action[] { vActionFullScreen, vActionDrawHydrogens, vActionHideUnselectedCompounds, vActionSpin,
				vActionBlackWhite, vActionMoleculeDescriptor };
	}

	public Action[] getHighlightActions()
	{
		return new Action[] { tActionColorMatch, tActionHighlightLog, tActionHighlightLastSelectedFeature,
				tActionToggleHighlightMode, tActionDecreaseSphereSize, tActionIncreaseSphereSize,
				tActionDecreaseSphereTranslucency, tActionIncreaseSphereTranslucency };
	}

	public Action[] getHelpActions()
	{
		return new Action[] { hActionDocu, hActionAbout };
	}

	public Action[] getHiddenActions()
	{
		return new Action[] { xActionUpdateMouseSelectionPressed, xActionUpdateMouseSelectionReleased,
				xActionDisablePopup, xActionDecreaseCompoundSize, xActionIncreaseCompoundSize };
	}

	public void performActions(Object source, KeyStroke k)
	{
		System.out.println("\nlook for actions: " + k);
		//for (Action a : allActions)
		for (Action a : getHiddenActions())
		{
			KeyStroke k2 = getKeyStroke(a);
			if (a.isEnabled() && k2 != null)
			{
				System.out.println("? " + a + " : " + k2);
				if (k2.equals(k))
				{
					System.out.println("! " + a);
					a.actionPerformed(new ActionEvent(source, -1, ""));
				}
			}
		}
	}

	public static void main(String args[])
	{
		showAboutDialog();
	}
}