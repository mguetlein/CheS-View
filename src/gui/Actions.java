package gui;

import gui.MainPanel.HighlightMode;
import gui.ViewControler.HideCompounds;
import gui.util.CompoundPropertyHighlighter;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
import dataInterface.CompoundProperty;

public class Actions
{
	public static final String TOOLTIP = "tooltip";

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

	private final static String DATA_COMPOUND_TABLE = "file-compound-table";
	private final static String DATA_FEATURE_TABLE = "file-feature-table";
	private final static String[] DATA_ACTIONS = { DATA_COMPOUND_TABLE, DATA_FEATURE_TABLE };

	private final static String FILE_NEW = "file-new";
	private final static String FILE_EXIT = "file-exit";
	private final static String[] FILE_ACTIONS = { FILE_NEW, FILE_EXIT };

	private final static String REMOVE_CURRENT = "remove-current";
	private final static String REMOVE_CLUSTERS = "remove-clusters";
	private final static String REMOVE_COMPOUNDS = "remove-compounds";
	private final static String[] REMOVE_ACTIONS = { REMOVE_CURRENT, REMOVE_CLUSTERS, REMOVE_COMPOUNDS };

	private final static String EXPORT_CURRENT = "export-current";
	private final static String EXPORT_CLUSTERS = "export-clusters";
	private final static String EXPORT_COMPOUNDS = "export-compounds";
	private final static String EXPORT_IMAGE = "export-image";
	private final static String EXPORT_WORKFLOW = "export-workflow";
	private final static String[] EXPORT_ACTIONS = { EXPORT_CURRENT, EXPORT_CLUSTERS, EXPORT_COMPOUNDS, EXPORT_IMAGE,
			EXPORT_WORKFLOW };

	private final static String VIEW_HIDE_NONE = "view-hide-none";
	private final static String VIEW_HIDE_NON_WATCHED = "view-hide-non-watched";
	private final static String VIEW_HIDE_NON_ACTIVE = "view-hide-non-active";
	private final static String[] VIEW_HIDE_ACTIONS = { VIEW_HIDE_NONE, VIEW_HIDE_NON_ACTIVE, VIEW_HIDE_NON_WATCHED };

	private final static String VIEW_FULL_SCREEN = "view-full-screen";
	private final static String VIEW_DRAW_HYDROGENS = "view-draw-hydrogens";
	private final static String VIEW_SPIN = "view-spin";
	private final static String VIEW_BLACK_WHITE = "view-black-white";
	private final static String VIEW_ANTIALIAS = "view-antialias";
	private final static String VIEW_COMPOUND_DESCRIPTOR = "view-compound-descriptor";
	private final static String VIEW_SELECT_LAST_FEATURE = "view-select-last-feature";
	private final static String[] VIEW_ACTIONS = { VIEW_FULL_SCREEN, VIEW_DRAW_HYDROGENS, VIEW_SPIN, VIEW_BLACK_WHITE,
			VIEW_ANTIALIAS, VIEW_COMPOUND_DESCRIPTOR, VIEW_SELECT_LAST_FEATURE };

	private final static String HIGHLIGHT_LOG = "highlight-log";
	private final static String HIGHLIGHT_COLOR_MATCH = "highlight-color-match";
	private final static String HIGHLIGHT_MODE = "highlight-mode";
	private final static String HIGHLIGHT_LAST_FEATURE = "highlight-last-feature";
	private final static String HIGHLIGHT_DECR_SPHERE_SIZE = "highlight-decr-sphere-size";
	private final static String HIGHLIGHT_INCR_SPHERE_SIZE = "highlight-incr-sphere-size";
	private final static String HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY = "highlight-decr-sphere-translucency";
	private final static String HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY = "highlight-incr-sphere-translucency";
	private final static String[] HIGHLIGHT_ACTIONS = { HIGHLIGHT_LOG, HIGHLIGHT_COLOR_MATCH, HIGHLIGHT_MODE,
			HIGHLIGHT_LAST_FEATURE };
	private final static String[] HIGHLIGHT_SPHERE_ACTIONS = { HIGHLIGHT_DECR_SPHERE_SIZE, HIGHLIGHT_INCR_SPHERE_SIZE,
			HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY, HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY };

	private final static String HELP_DOCU = "help-docu";
	private final static String HELP_ABOUT = "help-about";
	private final static String[] HELP_ACTIONS = { HELP_DOCU, HELP_ABOUT };

	private final static String HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED = "hidden-update-mouse-selection-pressed";
	private final static String HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED = "hidden-update-mouse-selection-released";
	private final static String HIDDEN_DECR_COMPOUND_SIZE = "hidden-decr-compound-size";
	private final static String HIDDEN_INCR_COMPOUND_SIZE = "hidden-incr-compound-size";
	private final static String HIDDEN_ENABLE_JMOL_POPUP = "enable-jmol-popup";
	private final static String[] HIDDEN_ACTIONS = { HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED,
			HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED, HIDDEN_DECR_COMPOUND_SIZE, HIDDEN_INCR_COMPOUND_SIZE,
			HIDDEN_ENABLE_JMOL_POPUP };

	private HashMap<String, Action> actions = new LinkedHashMap<String, Action>();

	private static HashMap<String, KeyStroke> keys = new HashMap<String, KeyStroke>();

	static
	{
		keys.put(DATA_COMPOUND_TABLE, KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
		keys.put(DATA_FEATURE_TABLE, KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));
		keys.put(FILE_NEW, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
		keys.put(FILE_EXIT, KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
		keys.put(REMOVE_CURRENT, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, ActionEvent.ALT_MASK));
		keys.put(EXPORT_IMAGE, KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
		keys.put(EXPORT_WORKFLOW, KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.ALT_MASK));
		keys.put(VIEW_FULL_SCREEN, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ActionEvent.ALT_MASK));
		keys.put(VIEW_DRAW_HYDROGENS, KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
		//		keys.put(VIEW_HIDE_UNSELECTED, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
		keys.put(VIEW_SPIN, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
		keys.put(VIEW_BLACK_WHITE, KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_COLOR_MATCH, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
		keys.put(VIEW_COMPOUND_DESCRIPTOR, KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_ENABLE_JMOL_POPUP, KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED,
				KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, ActionEvent.SHIFT_MASK));
		keys.put(HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED, KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT, 0, true));
		keys.put(VIEW_ANTIALIAS, KeyStroke.getKeyStroke(KeyEvent.VK_G, ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_LOG, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_LAST_FEATURE, KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK));
		keys.put(VIEW_SELECT_LAST_FEATURE, KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_MODE, KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.ALT_MASK));
		keys.put(HIDDEN_DECR_COMPOUND_SIZE, KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK));
		keys.put(HIDDEN_INCR_COMPOUND_SIZE, KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK));
		keys.put(HIGHLIGHT_DECR_SPHERE_SIZE,
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		keys.put(HIGHLIGHT_INCR_SPHERE_SIZE,
				KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
		keys.put(HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY,
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK));
		keys.put(HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY,
				KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, ActionEvent.CTRL_MASK | ActionEvent.ALT_MASK));
	}

	private Actions(GUIControler guiControler, ViewControler viewControler, Clustering clustering)
	{
		this.guiControler = guiControler;
		this.viewControler = viewControler;
		this.clustering = clustering;

		buildActions();
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
		clustering.getCompoundWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				update();
			}
		});
		clustering.getCompoundActive().addListener(new PropertyChangeListener()
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
					actions.get(HIGHLIGHT_LAST_FEATURE).setEnabled(
							viewControler.getHighlightMode() == HighlightMode.Spheres);
					actions.get(HIGHLIGHT_INCR_SPHERE_SIZE).setEnabled(
							viewControler.getHighlightMode() == HighlightMode.Spheres);
					actions.get(HIGHLIGHT_DECR_SPHERE_SIZE).setEnabled(
							viewControler.getHighlightMode() == HighlightMode.Spheres);
					actions.get(HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY).setEnabled(
							viewControler.getHighlightMode() == HighlightMode.Spheres);
					actions.get(HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY).setEnabled(
							viewControler.getHighlightMode() == HighlightMode.Spheres);
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
			if (clustering.isCompoundActive())
				m = ArrayUtil.concat(m, clustering.getCompoundActive().getSelectedIndices());
			if (clustering.isCompoundWatched()
					&& ArrayUtil.indexOf(m, clustering.getCompoundWatched().getSelected()) == -1)
				m = ArrayUtil.concat(m, new int[] { clustering.getCompoundWatched().getSelected() });
		}
		else if (clustering.isClusterWatched())
			c = clustering.getClusterWatched().getSelected();

		actions.get(REMOVE_CURRENT).putValue("Cluster", c);
		actions.get(REMOVE_CURRENT).putValue("Compound", m);

		if (m.length > 0 || c != null)
		{
			if (m.length == 1)
			{
				((AbstractAction) actions.get(REMOVE_CURRENT)).putValue(Action.NAME,
						"Remove " + clustering.getCompoundWithCompoundIndex(m[0]));
				((AbstractAction) actions.get(EXPORT_CURRENT)).putValue(Action.NAME,
						"Export " + clustering.getCompoundWithCompoundIndex(m[0]));
			}
			else if (m.length > 1)
			{
				((AbstractAction) actions.get(REMOVE_CURRENT)).putValue(Action.NAME, "Remove " + m.length
						+ " Compounds");
				((AbstractAction) actions.get(EXPORT_CURRENT)).putValue(Action.NAME, "Export " + m.length
						+ " Compounds");
			}
			else if (c != -1)
			{
				((AbstractAction) actions.get(REMOVE_CURRENT)).putValue(Action.NAME,
						"Remove " + clustering.getCluster(c).getName());
				((AbstractAction) actions.get(EXPORT_CURRENT)).putValue(Action.NAME,
						"Export " + clustering.getCluster(c).getName());
			}
			actions.get(REMOVE_CURRENT).setEnabled(true);
			actions.get(EXPORT_CURRENT).setEnabled(true);
		}
		else
		{
			((AbstractAction) actions.get(REMOVE_CURRENT)).putValue(Action.NAME, "Remove Selected Cluster/Compound");
			actions.get(REMOVE_CURRENT).setEnabled(false);

			((AbstractAction) actions.get(EXPORT_CURRENT)).putValue(Action.NAME, "Export Selected Cluster/Compound");
			actions.get(EXPORT_CURRENT).setEnabled(false);
		}
	}

	private void setAccelerators()
	{
		for (String action : actions.keySet())
		{
			KeyStroke keyStroke = keys.get(action);
			if (keyStroke != null)
				((AbstractAction) actions.get(action)).putValue(Action.ACCELERATOR_KEY, keyStroke);
		}
	}

	private abstract class ActionCreator
	{
		AbstractAction action;

		public ActionCreator(String s)
		{
			this(s, true, null);
		}

		public ActionCreator(String s, boolean enabled)
		{
			this(s, enabled, null);
		}

		public ActionCreator(String s, String guiProperty)
		{
			this(s, true, guiProperty);
		}

		public ActionCreator(String s, boolean enabled, final String changeProperty)
		{
			this(s, enabled, changeProperty, false);
		}

		public ActionCreator(String s, boolean enabled, final String changeProperty, boolean isRadio)
		{
			String name = null;
			String tooltip = null;
			if (ArrayUtil.indexOf(HIDDEN_ACTIONS, s) == -1)
			{
				name = Settings.text("action." + s);
				tooltip = "<html>" + Settings.text("action." + s + ".tooltip").replace("\n", "<br>") + "</html>";
			}

			action = new AbstractAction(name)
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					action();
				}
			};
			action.putValue(TOOLTIP, tooltip);
			action.putValue("is-radio-buttion", (Boolean) isRadio);
			action.setEnabled(enabled);

			if (changeProperty != null)
			{
				action.putValue(Action.SELECTED_KEY, getValueFromGUI());
				PropertyChangeListener l = new PropertyChangeListener()
				{
					@Override
					public void propertyChange(PropertyChangeEvent evt)
					{
						if (evt.getPropertyName().equals(changeProperty))
						{
							action.putValue(Action.SELECTED_KEY, getValueFromGUI());
						}
					}
				};
				guiControler.addPropertyChangeListener(l);
				viewControler.addViewListener(l);
			}

			actions.put(s, action);
		}

		public abstract void action();

		public Object getValueFromGUI()
		{
			return null;
		}

		public Object getActionValue()
		{
			return action.getValue(Action.SELECTED_KEY);
		}
	}

	private CompoundProperty getLogProp()
	{
		CompoundProperty logProp = null;
		if (viewControler.isHighlightLogEnabled()
				&& viewControler.getHighlighter() instanceof CompoundPropertyHighlighter)
			logProp = ((CompoundPropertyHighlighter) viewControler.getHighlighter()).getProperty();
		return logProp;
	}

	private void buildActions()
	{
		new ActionCreator(FILE_NEW)
		{
			@Override
			public void action()
			{
				newClustering(0);
			}
		};
		new ActionCreator(DATA_COMPOUND_TABLE)
		{
			@Override
			public void action()
			{
				new CompoundTable(viewControler, clustering);
			}
		};
		new ActionCreator(DATA_FEATURE_TABLE)
		{
			@Override
			public void action()
			{
				new FeatureTable(viewControler, clustering);
			}
		};
		new ActionCreator(FILE_EXIT)
		{
			@Override
			public void action()
			{
				JFrame f = null;
				if (viewControler instanceof JPanel)
				{
					java.awt.Container top = ((JPanel) viewControler).getTopLevelAncestor();
					if (top instanceof JFrame)
						f = (JFrame) top;
				}
				LaunchCheSMapper.exit(f);
			}
		};
		new ActionCreator(REMOVE_CURRENT, false)
		{
			@Override
			public void action()
			{
				int[] m = (int[]) ((AbstractAction) actions.get(REMOVE_CURRENT)).getValue("Compound");
				Integer c = (Integer) ((AbstractAction) actions.get(REMOVE_CURRENT)).getValue("Cluster");
				View.instance.suspendAnimation("remove selected");
				if (m.length > 0)
					clustering.removeCompounds(m);
				else if (c != null)
					clustering.removeCluster(c);
				View.instance.proceedAnimation("remove selected");
			}
		};
		new ActionCreator(REMOVE_CLUSTERS)
		{
			@Override
			public void action()
			{
				View.instance.suspendAnimation("remove clusters");
				clustering.chooseClustersToRemove();
				View.instance.proceedAnimation("remove clusters");
			}
		};
		new ActionCreator(REMOVE_COMPOUNDS)
		{
			@Override
			public void action()
			{
				View.instance.suspendAnimation("remove compounds");
				clustering.chooseCompoundsToRemove();
				View.instance.proceedAnimation("remove compounds");
			}
		};
		new ActionCreator(EXPORT_CURRENT)
		{
			@Override
			public void action()
			{
				int[] m = (int[]) ((AbstractAction) actions.get(REMOVE_CURRENT)).getValue("Compound");
				Integer c = (Integer) ((AbstractAction) actions.get(REMOVE_CURRENT)).getValue("Cluster");
				if (m.length > 0)
					ExportData.exportCompounds(clustering, m, getLogProp());
				else if (c != null)
					ExportData.exportClusters(clustering, new int[] { c }, getLogProp());
			}
		};
		new ActionCreator(EXPORT_CLUSTERS)
		{
			@Override
			public void action()
			{
				clustering.chooseClustersToExport(getLogProp());
			}
		};
		new ActionCreator(EXPORT_COMPOUNDS)
		{
			@Override
			public void action()
			{
				clustering.chooseCompoundsToExport(getLogProp());
			}
		};
		new ActionCreator(EXPORT_IMAGE)
		{
			@Override
			public void action()
			{
				View.instance.exportImage();
			}
		};
		new ActionCreator(EXPORT_WORKFLOW)
		{
			@Override
			public void action()
			{
				MappingWorkflow.exportMappingWorkflowToFile(MappingWorkflow.exportSettingsToMappingWorkflow());
			}
		};
		new ActionCreator(VIEW_FULL_SCREEN, GUIControler.PROPERTY_FULLSCREEN_CHANGED)
		{
			@Override
			public void action()
			{
				guiControler.setFullScreen((Boolean) getActionValue());
			}

			@Override
			public Object getValueFromGUI()
			{
				return guiControler.isFullScreen();
			}
		};
		new ActionCreator(VIEW_DRAW_HYDROGENS, ViewControler.PROPERTY_SHOW_HYDROGENS)
		{
			@Override
			public void action()
			{
				viewControler.setHideHydrogens(!((Boolean) getActionValue()));
			}

			@Override
			public Object getValueFromGUI()
			{
				return !viewControler.isHideHydrogens();
			}
		};

		new ActionCreator(VIEW_HIDE_NONE, true, ViewControler.PROPERTY_HIDE_UNSELECT_CHANGED, true)
		{
			@Override
			public void action()
			{
				viewControler.setHideCompounds(HideCompounds.none);
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.getHideCompounds() == HideCompounds.none;
			}
		};
		new ActionCreator(VIEW_HIDE_NON_WATCHED, true, ViewControler.PROPERTY_HIDE_UNSELECT_CHANGED, true)
		{
			@Override
			public void action()
			{
				viewControler.setHideCompounds(HideCompounds.nonWatched);
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.getHideCompounds() == HideCompounds.nonWatched;
			}
		};
		new ActionCreator(VIEW_HIDE_NON_ACTIVE, true, ViewControler.PROPERTY_HIDE_UNSELECT_CHANGED, true)
		{
			@Override
			public void action()
			{
				viewControler.setHideCompounds(HideCompounds.nonActive);
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.getHideCompounds() == HideCompounds.nonActive;
			}
		};

		new ActionCreator(VIEW_SPIN, ViewControler.PROPERTY_SPIN_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setSpinEnabled((Boolean) getActionValue());
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.isSpinEnabled();
			}
		};
		new ActionCreator(VIEW_BLACK_WHITE, ViewControler.PROPERTY_BACKGROUND_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setBackgroundBlack((Boolean) getActionValue());
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.isBlackgroundBlack();
			}
		};
		new ActionCreator(VIEW_ANTIALIAS, ViewControler.PROPERTY_ANTIALIAS_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setAntialiasEnabled((Boolean) getActionValue());
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.isAntialiasEnabled();
			}
		};
		new ActionCreator(VIEW_COMPOUND_DESCRIPTOR)
		{
			@Override
			public void action()
			{
				List<CompoundProperty> props = new ArrayList<CompoundProperty>();
				props.add(ViewControler.COMPOUND_INDEX_PROPERTY);
				props.add(ViewControler.COMPOUND_SMILES_PROPERTY);
				for (CompoundProperty compoundProperty : clustering.getProperties())
					props.add(compoundProperty);
				for (CompoundProperty compoundProperty : clustering.getFeatures())
					if (!props.contains(compoundProperty))
						props.add(compoundProperty);
				CompoundProperty selected = viewControler.getCompoundDescriptor();
				CompoundProperty p = SwingUtil.selectFromListWithDialog(props, selected, "Set compound identifier",
						Settings.TOP_LEVEL_FRAME);
				if (p != null)
					viewControler.setCompoundDescriptor(p);
			}
		};
		new ActionCreator(HELP_DOCU)
		{
			@Override
			public void action()
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
		new ActionCreator(HELP_ABOUT)
		{
			@Override
			public void action()
			{
				showAboutDialog();
			}
		};
		new ActionCreator(HIDDEN_ENABLE_JMOL_POPUP)
		{
			@Override
			public void action()
			{
				View.instance.scriptWait("set disablePopupMenu off");
			}
		};
		new ActionCreator(HIDDEN_UPDATE_MOUSE_SELECTION_PRESSED)
		{
			@Override
			public void action()
			{
				viewControler.updateMouseSelection(true);
			}
		};
		new ActionCreator(HIDDEN_UPDATE_MOUSE_SELECTION_RELEASED)
		{
			@Override
			public void action()
			{
				viewControler.updateMouseSelection(false);
			}
		};
		new ActionCreator(HIDDEN_DECR_COMPOUND_SIZE)
		{
			@Override
			public void action()
			{
				if (viewControler.canChangeCompoundSize(false))
					viewControler.changeCompoundSize(false);
			}
		};
		new ActionCreator(HIDDEN_INCR_COMPOUND_SIZE)
		{
			@Override
			public void action()
			{
				if (viewControler.canChangeCompoundSize(true))
					viewControler.changeCompoundSize(true);
			}
		};
		new ActionCreator(HIGHLIGHT_COLOR_MATCH, ViewControler.PROPERTY_MATCH_COLOR_CHANGED)
		{
			@Override
			public void action()
			{
				Color col = JColorChooser
						.showDialog(Settings.TOP_LEVEL_FRAME, "Select Color", (Color) getActionValue());
				if (col != null)
				{
					//					tActionColorMatch.putValue("matchcolor", col);
					viewControler.setMatchColor(col);
				}
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.getMatchColor();
			}
		};
		new ActionCreator(HIGHLIGHT_LOG, ViewControler.PROPERTY_HIGHLIGHT_LOG_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setHighlightLogEnabled((Boolean) getActionValue());
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.isHighlightLogEnabled();
			}
		};
		new ActionCreator(HIGHLIGHT_LAST_FEATURE, viewControler.getHighlightMode() == HighlightMode.Spheres,
				ViewControler.PROPERTY_HIGHLIGHT_LAST_FEATURE)
		{
			@Override
			public void action()
			{
				viewControler.setHighlightLastFeatureEnabled((Boolean) getActionValue());
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.isHighlightLastFeatureEnabled();
			}
		};
		new ActionCreator(VIEW_SELECT_LAST_FEATURE)
		{
			@Override
			public void action()
			{
				viewControler.setSelectLastSelectedHighlighter();
			}
		};
		new ActionCreator(HIGHLIGHT_MODE, ViewControler.PROPERTY_HIGHLIGHT_MODE_CHANGED)
		{
			@Override
			public void action()
			{
				viewControler.setHighlightMode(((Boolean) getActionValue()) ? MainPanel.HighlightMode.Spheres
						: MainPanel.HighlightMode.ColorCompounds);
			}

			@Override
			public Object getValueFromGUI()
			{
				return viewControler.getHighlightMode() == MainPanel.HighlightMode.Spheres;
			}
		};
		new ActionCreator(HIGHLIGHT_DECR_SPHERE_SIZE, viewControler.getHighlightMode() == HighlightMode.Spheres)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereSize >= 0.1)
					viewControler.setSphereSize(View.instance.sphereSize - 0.1);
				else
					viewControler.setSphereSize(0);
			}
		};
		new ActionCreator(HIGHLIGHT_INCR_SPHERE_SIZE, viewControler.getHighlightMode() == HighlightMode.Spheres)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereSize <= 0.9)
					viewControler.setSphereSize(View.instance.sphereSize + 0.1);
				else
					viewControler.setSphereSize(1);
			}
		};
		new ActionCreator(HIGHLIGHT_DECR_SPHERE_TRANSLUCENCY, viewControler.getHighlightMode() == HighlightMode.Spheres)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereTranslucency >= 0.1)
					viewControler.setSphereTranslucency(View.instance.sphereTranslucency - 0.1);
				else
					viewControler.setSphereTranslucency(0);
			}
		};
		new ActionCreator(HIGHLIGHT_INCR_SPHERE_TRANSLUCENCY, viewControler.getHighlightMode() == HighlightMode.Spheres)
		{
			@Override
			public void action()
			{
				if (View.instance.sphereTranslucency <= 0.9)
					viewControler.setSphereTranslucency(View.instance.sphereTranslucency + 0.1);
				else
					viewControler.setSphereTranslucency(1);
			}
		};
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
					CheSMapperWizard wwd = null;
					while (wwd == null || wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_IMPORT)
					{
						wwd = new CheSMapperWizard(top, startPanel);
						wwd.setCloseButtonText("Cancel");
						Settings.TOP_LEVEL_FRAME = top;
						SwingUtil.waitWhileVisible(wwd);
					}
					if (wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_FINISH)
					{
						View.instance.suspendAnimation("remap");
						clustering.clear();
						Task task = TaskProvider.initTask("Chemical space mapping");
						new TaskDialog(task, Settings.TOP_LEVEL_FRAME);
						ClusteringData d = wwd.getChesMapping().doMapping();
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

	private Action[] getActions(String actionNames[])
	{
		Action a[] = new Action[actionNames.length];
		for (int i = 0; i < a.length; i++)
			a[i] = actions.get(actionNames[i]);
		return a;
	}

	public Action[] getFileActions()
	{
		return getActions(FILE_ACTIONS);
	}

	public Action[] getDataActions()
	{
		return getActions(DATA_ACTIONS);
	}

	public Action[] getExportActions()
	{
		return getActions(EXPORT_ACTIONS);
	}

	public Action[] getRemoveActions()
	{
		return getActions(REMOVE_ACTIONS);
	}

	public Action[] getViewHideActions()
	{
		return getActions(VIEW_HIDE_ACTIONS);
	}

	public Action[] getViewActions()
	{
		return getActions(VIEW_ACTIONS);
	}

	public Action[] getHighlightActions()
	{
		return getActions(HIGHLIGHT_ACTIONS);
	}

	public Action[] getHighlightSphereActions()
	{
		return getActions(HIGHLIGHT_SPHERE_ACTIONS);
	}

	public Action[] getHelpActions()
	{
		return getActions(HELP_ACTIONS);
	}

	public Action[] getHiddenActions()
	{
		return getActions(HIDDEN_ACTIONS);
	}

	public void performActions(Object source, KeyStroke k)
	{
		//		System.out.println("\nlook for actions: " + k);
		for (String a : HIDDEN_ACTIONS)
		{
			KeyStroke keyStroke = keys.get(a);
			if (actions.get(a).isEnabled() && keyStroke != null)
			{
				//				System.out.println("? " + a + " : " + keyStroke);
				if (keyStroke.equals(k))
				{
					//					System.out.println("! " + a);
					actions.get(a).actionPerformed(new ActionEvent(source, -1, ""));
				}
			}
		}
	}

	public static void main(String args[])
	{
		showAboutDialog();
	}
}