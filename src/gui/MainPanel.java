package gui;

import gui.View.AnimationSpeed;
import gui.swing.ComponentFactory;
import gui.util.HighlightAutomatic;
import gui.util.Highlighter;
import gui.util.MoleculePropertyHighlighter;
import gui.util.SubstructureHighlighter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import main.ScreenSetup;
import main.Settings;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolSimpleViewer;

import util.ArrayUtil;
import util.ColorUtil;
import util.DoubleUtil;
import util.ObjectUtil;
import util.StringUtil;
import util.ThreadUtil;
import cluster.Cluster;
import cluster.Clustering;
import cluster.ClusteringUtil;
import cluster.Model;
import data.ClusteringData;
import dataInterface.MoleculeProperty;
import dataInterface.MoleculeProperty.Type;
import dataInterface.MoleculePropertyUtil;
import dataInterface.SubstructureSmartsType;

public class MainPanel extends JPanel implements ViewControler
{
	GUIControler guiControler;
	JmolPanel jmolPanel;
	View view;
	private Clustering clustering;
	private boolean spinEnabled = false;
	private boolean hideHydrogens = true;
	private String style = STYLE_WIREFRAME;
	boolean hideUnselected = false;
	Color matchColor = Color.ORANGE;
	MoleculeProperty modelDescriptorProperty = null;
	List<JComponent> ignoreMouseMovementPanels = new ArrayList<JComponent>();

	private Color getModelHighlightColor(Model m, Highlighter h, MoleculeProperty p)
	{
		if (h == Highlighter.CLUSTER_HIGHLIGHTER)
			return MoleculePropertyUtil.getColor(clustering.getClusterIndexForModel(m));
		else if (p == null)
			return null;
		else if (m.getTemperature(p).equals("null"))
			return Color.DARK_GRAY;
		else if (p.getType() == Type.NOMINAL)
			return MoleculePropertyUtil.getNominalColor(p, m.getStringValue(p));
		else if (highlightLogEnabled && p.getType() == Type.NUMERIC)
			return ColorUtil.getThreeColorGradient(clustering.getNormalizedLogDoubleValue(m, p), Color.RED,
					Color.WHITE, Color.BLUE);
		else
			return ColorUtil.getThreeColorGradient(clustering.getNormalizedDoubleValue(m, p), Color.RED, Color.WHITE,
					Color.BLUE);
	}

	public static enum Translucency
	{
		None, ModerateWeak, ModerateStrong, Strong;
	}

	public static enum HighlightMode
	{
		ColorCompounds, Spheres;
	}

	private String getColorSuffixTranslucent(Translucency t)
	{
		if (t == Translucency.None)
			return "";
		double translucency[] = null;
		if (style.equals(STYLE_WIREFRAME))
			translucency = new double[] { -1, 0.4, 0.6, 0.8 };
		else if (style.equals(STYLE_BALLS_AND_STICKS))
			translucency = new double[] { -1, 0.5, 0.7, 0.9 };
		double trans = translucency[ArrayUtil.indexOf(Translucency.values(), t)];
		//		if (Settings.SCREENSHOT_SETUP)
		//			trans = 0.99;// Math.min(0.95, trans + 0.15);
		//		Settings.LOGGER.warn(trans);
		return "; color translucent " + trans;
	}

	HashMap<String, Highlighter[]> highlighters;
	Highlighter selectedHighlighter = Highlighter.CLUSTER_HIGHLIGHTER;
	Highlighter lastSelectedHighlighter = Highlighter.DEFAULT_HIGHLIGHTER;
	MoleculeProperty selectedHighlightMoleculeProperty = null;
	boolean highlighterLabelsVisible = false;
	HighlightSorting highlightSorting = HighlightSorting.Median;
	HighlightAutomatic highlightAutomatic;
	boolean backgroundBlack = true;
	HighlightMode highlightMode = HighlightMode.ColorCompounds;
	boolean highlightLogEnabled = false;
	boolean antialiasEnabled = ScreenSetup.SETUP.isAntialiasOn();
	boolean highlightLastFeatureEnabled = false;

	public Clustering getClustering()
	{
		return clustering;
	}

	public boolean isSpinEnabled()
	{
		return spinEnabled;
	}

	public void setSpinEnabled(boolean spin)
	{
		setSpinEnabled(spin, false);
	}

	private void setSpinEnabled(boolean spinEnabled, boolean force)
	{
		if (this.spinEnabled != spinEnabled || force)
		{
			this.spinEnabled = spinEnabled;
			view.setSpinEnabled(spinEnabled);
			fireViewChange(PROPERTY_SPIN_CHANGED);
		}
	}

	public MainPanel(GUIControler guiControler)
	{
		this.guiControler = guiControler;
		jmolPanel = new JmolPanel();

		setLayout(new BorderLayout());
		add(jmolPanel);

		clustering = new Clustering();
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(Clustering.CLUSTER_NEW)
						|| evt.getPropertyName().equals(Clustering.CLUSTER_CLEAR))
					MainPanel.this.guiControler.updateTitle(clustering);
			}
		});
		View.init(jmolPanel, guiControler, this, clustering);
		view = View.instance;
		highlightAutomatic = new HighlightAutomatic(this, clustering);

		// mouse listener to click atoms or clusters (zoom in)
		jmolPanel.addMouseListener(new MouseAdapter()
		{
			JPopupMenu popup;

			public void mouseClicked(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e))
				{
					int atomIndex = view.findNearestAtomIndex(e.getX(), e.getY());
					if (atomIndex != -1)
					{
						boolean selectCluster = !clustering.isClusterActive() && !e.isControlDown();
						boolean selectModel = clustering.isClusterActive() || e.isShiftDown();

						if (selectCluster)
						{
							// set a cluster to active (zooming will be done in listener)
							clustering.getClusterActive().setSelected(
									clustering.getClusterIndexForModelIndex(view.getAtomModelIndex(atomIndex)));
							clustering.getClusterWatched().clearSelection();
						}
						if (selectModel)
						{
							// already a cluster active, zooming into model
							// final Model m = clustering.getModelWithModelIndex(view.getAtomModelIndex(atomIndex));
							clustering.getModelWatched().clearSelection();
							if (e.isControlDown())
								clustering.getModelActive().setSelectedInverted(view.getAtomModelIndex(atomIndex));
							else
							{
								if (clustering.getModelActive().isSelected(view.getAtomModelIndex(atomIndex)))
									clustering.getModelActive().clearSelection();
								else
									clustering.getModelActive().setSelected(view.getAtomModelIndex(atomIndex));
							}
						}
					}
					else
					{
						clustering.getModelWatched().clearSelection();
						clustering.getClusterWatched().clearSelection();
						clustering.getModelActive().clearSelection();
					}
				}
				else if (SwingUtilities.isRightMouseButton(e))
				{
					if (popup == null)
						popup = MainPanel.this.guiControler.getPopup();
					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		jmolPanel.addMouseMotionListener(new MouseAdapter()
		{

			public void mouseMoved(MouseEvent e)
			{
				updateMouse(e.getPoint(), e.isShiftDown());
			}
		});
	}

	Point mousePos;
	boolean shiftDown;

	@Override
	public void updateMouseSelection(boolean shiftDown)
	{
		updateMouse(mousePos, shiftDown);
	}

	private void updateMouse(Point mousePos, boolean shiftDown)
	{
		this.mousePos = mousePos;
		this.shiftDown = shiftDown;

		//		System.err.println("mouse udpate " + mousePos + " " + shiftDown);

		for (JComponent c : ignoreMouseMovementPanels)
		{
			if (c.isVisible())
			{
				Point p = SwingUtilities.convertPoint(MainPanel.this, mousePos, c);
				if (p.x >= 0 && p.y >= 0 && p.x <= c.getWidth() && p.y <= c.getHeight())
					return;
			}
		}

		int atomIndex = view.findNearestAtomIndex(mousePos.x, mousePos.y);

		if (!clustering.isClusterActive() && !shiftDown)
		{
			if (atomIndex == -1)
			{
				// // do not clear cluster selection
				clustering.getClusterWatched().clearSelection();
			}
			else
			{
				clustering.getModelWatched().clearSelection();
				//clustering.getModelActive().clearSelection();
				clustering.getClusterWatched().setSelected(
						(clustering.getClusterIndexForModelIndex(view.getAtomModelIndex(atomIndex))));
			}
		}
		else
		{
			if (atomIndex == -1)
			{
				// // do not clear model selection
				clustering.getModelWatched().clearSelection();
			}
			else
			{
				clustering.getClusterWatched().clearSelection();
				clustering.getModelWatched().setSelected(view.getAtomModelIndex(atomIndex));
			}
		}
	}

	@Override
	public void addIgnoreMouseMovementComponents(JComponent c)
	{
		this.ignoreMouseMovementPanels.add(c);
	}

	public String getStyle()
	{
		return style;
	}

	public void setStyle(String style)
	{
		if (!this.style.equals(style))
		{
			this.style = style;
			updateAllClustersAndModels(false);
		}
	}

	@Override
	public HashMap<String, Highlighter[]> getHighlighters()
	{
		return highlighters;
	}

	@Override
	public Highlighter getHighlighter()
	{
		return selectedHighlighter;
	}

	@Override
	public void setHighlighter(final Highlighter highlighter)
	{
		if (this.selectedHighlighter != highlighter)
		{
			lastSelectedHighlighter = selectedHighlighter;
			selectedHighlighter = highlighter;
			if (selectedHighlighter instanceof MoleculePropertyHighlighter)
				selectedHighlightMoleculeProperty = ((MoleculePropertyHighlighter) highlighter).getProperty();
			else
				selectedHighlightMoleculeProperty = null;

			updateAllClustersAndModels(false);
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
		}
	}

	@Override
	public void setSelectLastSelectedHighlighter()
	{
		setHighlighter(lastSelectedHighlighter);
	}

	@Override
	public boolean isHighlighterLabelsVisible()
	{
		return highlighterLabelsVisible;
	}

	@Override
	public void setHighlighterLabelsVisible(boolean selected)
	{
		if (this.highlighterLabelsVisible != selected)
		{
			highlighterLabelsVisible = selected;
			updateAllClustersAndModels(false);
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
		}
	}

	@Override
	public void setHighlightSorting(HighlightSorting sorting)
	{
		if (this.highlightSorting != sorting)
		{
			highlightSorting = sorting;
			updateAllClustersAndModels(false);
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
		}
	}

	@Override
	public HighlightSorting getHighlightSorting()
	{
		return highlightSorting;
	}

	private static double boxTranslucency = 0.05;

	/**
	 * updates cluster with index i
	 * update is done if i != old index or forceupdate is true
	 * 
	 * paints/removes box around cluster
	 * hides models
	 * 
	 * @param clusterIndex
	 * @param forceUpdate
	 */
	public void updateCluster(int clusterIndex, boolean forceUpdate)
	{
		Cluster c = clustering.getCluster(clusterIndex);
		int active = clustering.getClusterActive().getSelected();
		int watched = clustering.getClusterWatched().getSelected();

		boolean watch = active == -1 && clusterIndex == watched;

		if (forceUpdate || watch != c.isWatched())
		{
			c.setWatched(watch);
			if (watch)
			{
				view.select(c.getBitSet());
				view.scriptWait("boundbox { selected }");
				view.scriptWait("boundbox off");
				view.scriptWait("draw ID bb" + clusterIndex + " BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_WATCH_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL \"" + c + "\"");

				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
				view.scriptWait("draw bb" + clusterIndex + " off");
		}

		boolean visible = active == -1 || clusterIndex == active;
		boolean someModelsHidden = visible && active == -1 && clustering.isSuperimposed();

		if (forceUpdate
				|| visible != c.isVisible()
				|| (visible && (someModelsHidden != c.someModelsHidden()
						|| selectedHighlightMoleculeProperty != c.getHighlightProperty() || highlightSorting != c
						.getHighlightSorting())))
		{
			c.setVisible(visible);
			c.setSomeModelsHidden(someModelsHidden);
			c.setHighlighProperty(selectedHighlightMoleculeProperty);
			c.setHighlightSorting(highlightSorting);
			hideOrDisplayCluster(c, visible, someModelsHidden);
		}
	}

	private void hideOrDisplayCluster(Cluster c, boolean clusterVisible, boolean hideSomeModels)
	{
		BitSet toDisplay = new BitSet();
		BitSet toHide = new BitSet();
		List<Model> toDisplayM = new ArrayList<Model>();
		List<Model> toHideM = new ArrayList<Model>();

		if (!clusterVisible)
		{
			toHide.or(c.getBitSet());
			for (Model model : c.getModels())
				toHideM.add(model);
		}
		else
		{
			if (!hideSomeModels)
			{
				toDisplay.or(c.getBitSet());
				for (Model model : c.getModels())
					toDisplayM.add(model);
			}
			else
			{
				int numModels = c.getModels().size();
				int max;
				if (numModels <= 10)
					max = 10;
				else
					max = (numModels - 10) / 3 + 10;
				max = Math.min(25, max);
				//			Settings.LOGGER.warn("hiding: ''" + hide + "'', num '" + numModels + "', max '" + max + "'");

				if (numModels < max)
					toDisplay.or(c.getBitSet());
				else
				{
					List<Model> models;
					if (selectedHighlighter instanceof MoleculePropertyHighlighter)
						models = c.getModelsInOrder(((MoleculePropertyHighlighter) selectedHighlighter).getProperty(),
								highlightSorting);
					else
						models = c.getModels();
					int count = 0;
					for (Model m : models)
					{
						if (count++ >= max)
							toHide.or(m.getBitSet());
						else
							toDisplay.or(m.getBitSet());
					}
				}
			}
		}

		for (Model model : toHideM)
		{
			if (model.isShowActiveBox())
				view.scriptWait("draw bb" + model.getModelIndex() + "a OFF");
			if (model.isSphereVisible())
				view.hideSphere(model);
		}
		view.hide(toHide);

		for (Model model : toDisplayM)
		{
			if (model.isShowActiveBox())
				view.scriptWait("draw bb" + model.getModelIndex() + "a ON");
			if (model.isSphereVisible())
				view.showSphere(model, model.isLastFeatureSphereVisible(), false);
		}
		view.display(toDisplay);
	}

	/**
	 * udpates all clusters
	 */
	private void updateAllClustersAndModels(boolean forceUpdate)
	{
		int a = getClustering().getClusterActive().getSelected();
		for (int j = 0; j < clustering.numClusters(); j++)
		{
			updateCluster(j, forceUpdate);
			if (a == -1 || a == j)
				for (Model m : clustering.getCluster(j).getModels())
					updateModel(m.getModelIndex(), forceUpdate);
		}
	}

	/**
	 * udpates single model
	 * forceUpdate = true -> everything is reset (independent of model is part of active cluster or if single props have changed)
	 * 
	 * shows/hides box around model
	 * show/hides model label
	 * set model translucent/opaque
	 * highlight substructure in model 
	 */
	private void updateModel(int modelIndex, boolean forceUpdate)
	{
		int clus = clustering.getClusterIndexForModelIndex(modelIndex);
		Cluster c = clustering.getCluster(clus);
		Model m = clustering.getModelWithModelIndex(modelIndex);
		if (m == null)
		{
			Settings.LOGGER.warn("model is null!");
			return;
		}
		int activeCluster = clustering.getClusterActive().getSelected();
		int watchedCluster = clustering.getClusterWatched().getSelected();

		boolean showHoverBox = false;
		boolean showActiveBox = false;
		boolean showLabel = false;
		boolean translucent;

		// inside the active cluster
		if (clus == activeCluster)
		{
			if (!clustering.isModelWatchedFromCluster(clus) && !clustering.isModelActiveFromCluster(clus))
				translucent = false;
			else
			{
				if (clustering.getModelWatched().isSelected(modelIndex)
						|| clustering.getModelActive().isSelected(modelIndex))
					translucent = false;
				else
					translucent = true;
			}

			if (selectedHighlighter instanceof MoleculePropertyHighlighter)
				showLabel = true;

			if (clustering.getModelWatched().isSelected(modelIndex) && !c.isSuperimposed())
				showHoverBox = true;
			if (clustering.getModelActive().isSelected(modelIndex) && !c.isSuperimposed())
				showActiveBox = true;
		}
		else
		{
			List<Model> models;
			if (selectedHighlighter instanceof MoleculePropertyHighlighter)
				models = c.getModelsInOrder(((MoleculePropertyHighlighter) selectedHighlighter).getProperty(),
						highlightSorting);
			else
				models = c.getModels();

			if (selectedHighlightMoleculeProperty != null && (models.indexOf(m) == 0 || !clustering.isSuperimposed()))
				showLabel = true;

			translucent = false;
			if (clustering.isSuperimposed())
				translucent = (models.indexOf(m) > 0);
			else
			{
				if (clustering.isModelWatched() || clustering.isModelActive())
				{
					translucent = !(clustering.getModelWatched().isSelected(modelIndex) || clustering.getModelActive()
							.isSelected(modelIndex));
				}
				else if (watchedCluster == -1 || selectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER)
					translucent = false;
				else
					translucent = (clus != watchedCluster);
			}

			if (clustering.getModelWatched().isSelected(modelIndex) && !c.isSuperimposed())
				showHoverBox = true;
			if (clustering.getModelActive().isSelected(modelIndex) && !c.isSuperimposed())
				showActiveBox = true;
		}

		String smarts = null;
		if (selectedHighlighter instanceof SubstructureHighlighter)
			smarts = c.getSubstructureSmarts(((SubstructureHighlighter) selectedHighlighter).getType());
		else if (selectedHighlighter instanceof MoleculePropertyHighlighter
				&& ((MoleculePropertyHighlighter) selectedHighlighter).getProperty().isSmartsProperty())
			smarts = ((MoleculePropertyHighlighter) selectedHighlighter).getProperty().getSmarts();

		if (!highlighterLabelsVisible)
			showLabel = false;

		BitSet bs = view.getModelUndeletedAtomsBitSet(modelIndex);
		view.select(bs);
		Color col = getModelHighlightColor(m, selectedHighlighter, selectedHighlightMoleculeProperty);
		String highlightColor = col == null ? null : "color " + ColorUtil.toJMolString(col);
		String modelColor;
		if (highlightMode == HighlightMode.Spheres)
			modelColor = "color cpk";
		else
			modelColor = col == null ? "color cpk" : "color " + ColorUtil.toJMolString(col);

		boolean sphereVisible = (highlightMode == HighlightMode.Spheres && highlightColor != null);
		boolean lastFeatureSphereVisible = sphereVisible && highlightLastFeatureEnabled;

		Translucency translucency;
		if (translucent)
		{
			if (clustering.isSuperimposed())
			{
				translucency = Translucency.Strong;
				if (highlightMode == HighlightMode.Spheres)
					sphereVisible = false;
			}
			else if (hideUnselected || clustering.getModelActive().getSelected() != -1)
			{
				if (clus == activeCluster)
				{
					if (c.size() <= 5)
						translucency = Translucency.ModerateWeak;
					else if (c.size() <= 15)
						translucency = Translucency.ModerateStrong;
					else
						translucency = Translucency.Strong;
				}
				else
					translucency = Translucency.ModerateStrong;
			}
			else
				translucency = Translucency.None;
		}
		else
			translucency = Translucency.None;

		boolean styleUpdate = !style.equals(m.getStyle());
		boolean modelUpdate = styleUpdate || translucency != m.getTranslucency()
				|| !ObjectUtil.equals(modelColor, m.getModelColor());
		//showLabel is enough because superimpose<->not-superimpose is not tracked in model
		boolean checkLabelUpdate = showLabel || (!showLabel && m.getLabel() != null)
				|| !ObjectUtil.equals(modelColor, m.getModelColor())
				|| selectedHighlightMoleculeProperty != m.getHighlightMoleculeProperty();
		boolean sphereUpdate = sphereVisible != m.isSphereVisible()
				|| (lastFeatureSphereVisible != m.isLastFeatureSphereVisible())
				|| (sphereVisible && (translucency != m.getTranslucency() || !ObjectUtil.equals(highlightColor,
						m.getHighlightColor())));
		boolean spherePositionUpdate = sphereVisible && !ObjectUtil.equals(m.getPosition(), m.getSpherePosition());
		boolean hoverBoxUpdate = showHoverBox != m.isShowHoverBox();
		boolean activeBoxUpdate = showActiveBox != m.isShowActiveBox();
		boolean smartsUpdate = modelUpdate || smarts != m.getHighlightedSmarts();

		m.setModelColor(modelColor);
		m.setStyle(style);
		m.setTranslucency(translucency);
		m.setHighlightMoleculeProperty(selectedHighlightMoleculeProperty);
		m.setHighlightColor(highlightColor);
		m.setSpherePosition(m.getPosition());
		m.setSphereVisible(sphereVisible);
		m.setLastFeatureSphereVisible(lastFeatureSphereVisible);
		m.setShowHoverBox(showHoverBox);
		m.setShowActiveBox(showActiveBox);

		if (forceUpdate || styleUpdate)
			view.scriptWait(style);
		if (forceUpdate || modelUpdate)
			view.scriptWait(modelColor + getColorSuffixTranslucent(translucency));

		if (forceUpdate || sphereUpdate || spherePositionUpdate)
		{
			if (sphereVisible)
				view.showSphere(m, lastFeatureSphereVisible, forceUpdate || spherePositionUpdate);
			else
				view.hideSphere(m);
		}

		if (forceUpdate || hoverBoxUpdate)
		{
			if (showHoverBox)
			{
				view.scriptWait("boundbox { selected }");
				view.scriptWait("boundbox off");
				view.scriptWait("draw ID bb" + m.getModelIndex() + "h BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_WATCH_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL \"" + m.toString() + "\"");
				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
				view.scriptWait("draw bb" + m.getModelIndex() + "h OFF");
		}

		if (forceUpdate || activeBoxUpdate)
		{
			if (showActiveBox)
			{
				view.scriptWait("boundbox { selected }");
				view.scriptWait("boundbox off");
				view.scriptWait("draw ID bb" + m.getModelIndex() + "a BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_ACTIVE_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL");
				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
			{
				view.scriptWait("draw bb" + m.getModelIndex() + "a OFF");
			}
		}

		// CHANGES JMOL SELECTION !!!
		if (forceUpdate || smartsUpdate)
		{
			boolean match = false;
			if (smarts != null && smarts.length() > 0)
			{
				BitSet matchBitSet = m.getSmartsMatch(smarts);
				if (matchBitSet.cardinality() > 0)
				{
					match = true;
					m.setHighlightedSmarts(smarts);
					view.select(matchBitSet);
					view.scriptWait("color " + View.convertColor(matchColor) + getColorSuffixTranslucent(translucency));
				}
			}
			if (!match)// || forceUpdate || translucentUpdate)
			{
				m.setHighlightedSmarts(null);
				if (highlightMode == HighlightMode.Spheres)
					view.scriptWait("color cpk" + getColorSuffixTranslucent(translucency));
				else
					view.scriptWait(modelColor + getColorSuffixTranslucent(translucency));
			}
		}

		// MAY CHANGE JMOL SELECTION !!!
		if (forceUpdate || checkLabelUpdate)
		{
			String labelString = null;
			if (showLabel)
			{
				if (activeCluster == -1 && clustering.isSuperimposed())
				{
					labelString = c.getName() + " - "
							+ ((MoleculePropertyHighlighter) selectedHighlighter).getProperty() + ": "
							+ c.getSummaryStringValue(selectedHighlightMoleculeProperty);
				}
				else
				{
					Object val = clustering.getModelWithModelIndex(modelIndex).getTemperature(
							((MoleculePropertyHighlighter) selectedHighlighter).getProperty());
					Double d = DoubleUtil.parseDouble(val + "");
					if (d != null)
						val = StringUtil.formatDouble(d);
					//				Settings.LOGGER.warn("label : " + i + " : " + c + " : " + val);
					labelString = ((MoleculePropertyHighlighter) selectedHighlighter).getProperty() + ": " + val;
				}
			}

			// CHANGES JMOL SELECTION!!
			if (forceUpdate || !ObjectUtil.equals(labelString, m.getLabel()))
			{
				m.setLabel(labelString);

				view.selectFirstCarbonAtom(m.getBitSet());
				//				BitSet empty = new BitSet(bs.length());
				//				empty.set(bs.nextSetBit(0));
				//				view.select(empty);

				if (showLabel)
				{
					view.scriptWait("set fontSize " + View.FONT_SIZE);
					view.scriptWait("label \"" + labelString + "\"");
				}
				else
				{
					//				Settings.LOGGER.warn("label : " + i + " : " + c + " : off");
					view.scriptWait("label OFF");
				}
			}
		}
	}

	public void init(ClusteringData clusteredDataset)
	{
		clustering.newClustering(clusteredDataset);

		clustering.getClusterActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				int cIndexOldArray[] = ((int[]) e.getOldValue());
				int cIndexOld = cIndexOldArray.length == 0 ? -1 : cIndexOldArray[0];
				updateClusterSelection(clustering.getClusterActive().getSelected(), cIndexOld, true);
			}
		});
		clustering.getClusterWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				int cIndexOldArray[] = ((int[]) e.getOldValue());
				int cIndexOld = cIndexOldArray.length == 0 ? -1 : cIndexOldArray[0];
				updateClusterSelection(clustering.getClusterWatched().getSelected(), cIndexOld, false);
			}
		});
		clustering.getModelActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateModelActiveSelection(((int[]) e.getNewValue()), ((int[]) e.getOldValue()), clustering
						.getModelActive().isExclusiveSelection());
			}
		});
		clustering.getModelWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				//				int mIndexOldArray[] = ((int[]) e.getOldValue());
				//				int mIndexOld = mIndexOldArray.length == 0 ? -1 : mIndexOldArray[0];
				updateModelWatchedSelection();//clustering.getModelWatched().getSelected(), mIndexOld);
			}
		});
		clustering.addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(Clustering.CLUSTER_ADDED))
					updateClusteringNew();
				else if (evt.getPropertyName().equals(Clustering.CLUSTER_REMOVED))
					updateClusterRemoved();
				else if (evt.getPropertyName().equals(Clustering.CLUSTER_MODIFIED))
					updateClusterModified();
			}
		});
		updateClusteringNew();
	}

	private void updateClusteringNew()
	{
		Highlighter[] h = new Highlighter[] { Highlighter.DEFAULT_HIGHLIGHTER, Highlighter.CLUSTER_HIGHLIGHTER };
		if (clustering.getSubstructures().size() > 0)
			for (SubstructureSmartsType type : clustering.getSubstructures())
				h = ArrayUtil.concat(Highlighter.class, h, new Highlighter[] { new SubstructureHighlighter(type) });
		highlighters = new LinkedHashMap<String, Highlighter[]>();
		highlighters.put("", h);

		List<MoleculeProperty> props = clustering.getProperties();
		MoleculePropertyHighlighter[] featureHighlighters = new MoleculePropertyHighlighter[props.size()];
		int fCount = 0;
		for (MoleculeProperty p : props)
			featureHighlighters[fCount++] = new MoleculePropertyHighlighter(p);
		highlighters.put("Features NOT used for mapping", featureHighlighters);

		props = clustering.getFeatures();
		featureHighlighters = new MoleculePropertyHighlighter[props.size()];
		fCount = 0;
		for (MoleculeProperty p : props)
			featureHighlighters[fCount++] = new MoleculePropertyHighlighter(p);
		highlighters.put("Features used for mapping", featureHighlighters);

		fireViewChange(PROPERTY_NEW_HIGHLIGHTERS);

		updateAllClustersAndModels(true);

		view.evalString("frame " + view.getModelNumberDotted(0) + " "
				+ view.getModelNumberDotted(clustering.numModels() - 1));

		setSpinEnabled(isSpinEnabled(), true);

		initHighlighter();
		initMoleculeDescriptor();

		view.suspendAnimation("new clustering");
		if (!isAllClustersSpreadable())
			setSuperimpose(true);
		if (clustering.getNumClusters() == 1)
			clustering.getClusterActive().setSelected(0);
		else
			view.zoomTo(clustering, null);
		view.proceedAnimation("new clustering");
	}

	private void initHighlighter()
	{
		if (clustering.getNumClusters() == 1)
			setHighlighter(Highlighter.DEFAULT_HIGHLIGHTER);
		else
			setHighlighter(Highlighter.CLUSTER_HIGHLIGHTER);
		highlightAutomatic.init();
	}

	private void updateClusterRemoved()
	{
		updateAllClustersAndModels(true);
		if (clustering.getNumClusters() == 0 && selectedHighlightMoleculeProperty != null)
			initHighlighter();
		if (clustering.getNumClusters() == 1)
			clustering.getClusterActive().setSelected(0);
	}

	private void updateClusterModified()
	{
		updateAllClustersAndModels(true);
	}

	private void updateModelWatchedSelection()//int mIndex, int mIndexOld)
	{
		//		Settings.LOGGER.println("update model active: " + active + " " + ArrayUtil.toString(mIndex));
		int activeCluster = clustering.getClusterActive().getSelected();
		Iterable<Model> models;
		if (activeCluster != -1)
			models = clustering.getCluster(activeCluster).getModels();
		else
			models = clustering.getModels(true);
		for (Model m : models)
			updateModel(m.getModelIndex(), false);
	}

	private void updateModelActiveSelection(int mIndex[], int mIndexOld[], boolean zoomIntoModel)
	{
		//		Settings.LOGGER.println("update model active: " + active + " " + ArrayUtil.toString(mIndex));
		int activeCluster = clustering.getClusterActive().getSelected();
		Iterable<Model> models;
		if (activeCluster != -1)
			models = clustering.getCluster(activeCluster).getModels();
		else
			models = clustering.getModels(true);
		for (Model m : models)
			updateModel(m.getModelIndex(), false);

		if (/*activeCluster != -1 &&*/!clustering.isSuperimposed())
		{
			// zoom because model selection has changed
			if (mIndex.length != 0 && zoomIntoModel)
			{
				// a model selected
				if (mIndex.length == 1)
				{
					final Model m = clustering.getModelWithModelIndex(mIndex[0]);
					view.zoomTo(m, AnimationSpeed.SLOW);
				}
			}
			else if (mIndex.length == 0 && mIndexOld.length > 0 && activeCluster != -1)
			{
				final Cluster c = getClustering().getClusterForModelIndex(mIndexOld[0]);
				view.zoomTo(c, AnimationSpeed.SLOW);
			}
		}
	}

	/**
	 * the cluster selection has changed
	 * active=true -> the selection change is about active/inactive
	 * active=false -> the selection change is about watched/not-watched
	 * 
	 * @param cIndex
	 * @param cIndexOld
	 * @param activeClusterChanged
	 */
	private void updateClusterSelection(int cIndex, int cIndexOld, final boolean activeClusterChanged)
	{
		// ignore watch updates when a cluster is active
		if (!activeClusterChanged && clustering.isClusterActive())
			return;

		Settings.LOGGER.info("updating cluster selection: " + cIndex + " " + cIndexOld + " " + activeClusterChanged);

		highlightAutomatic.resetClusterHighlighter(activeClusterChanged);

		updateAllClustersAndModels(false);
		if (activeClusterChanged)
			updateSuperimpose(false);

		view.afterAnimation(new Runnable()
		{
			@Override
			public void run()
			{
				highlightAutomatic.resetDefaultHighlighter(activeClusterChanged);
			}
		}, "highlight automatic after zooming and superimposing");
	}

	static class JmolPanel extends JPanel
	{
		JmolSimpleViewer viewer;
		JmolAdapter adapter;

		JmolPanel()
		{
			adapter = new SmarterJmolAdapter();
			viewer = JmolSimpleViewer.allocateSimpleViewer(this, adapter);
		}

		public JmolSimpleViewer getViewer()
		{
			return viewer;
		}

		final Dimension currentSize = new Dimension();
		final Rectangle rectClip = new Rectangle();

		final Dimension dimSize = new Dimension();

		public void paint(Graphics g)
		{
			////code for old version
			//			getSize(currentSize);
			//			g.getClipBounds(rectClip);
			//			if (g != null && currentSize != null && rectClip != null)
			//				viewer.renderScreenImage(g, currentSize, rectClip);

			getSize(dimSize);
			if (dimSize.width == 0)
				return;
			viewer.renderScreenImage(g, dimSize.width, dimSize.height);
		}
	}

	@Override
	public boolean isSuperimpose()
	{
		return clustering.isSuperimposed();
	}

	@Override
	public void setSuperimpose(boolean superimpose)
	{
		if (clustering.isSuperimposed() != superimpose)
		{
			clustering.setSuperimposed(superimpose);
			updateSuperimpose(true);
		}
	}

	@Override
	public boolean isAllClustersSpreadable()
	{
		for (Cluster c : clustering.getClusters())
			if (c.isSpreadable())
				return true;
		return false;
	}

	@Override
	public boolean isSingleClusterSpreadable()
	{
		if (!clustering.isClusterActive())
			throw new IllegalStateException();
		return clustering.getCluster(clustering.getClusterActive().getSelected()).isSpreadable();
	}

	@Override
	public boolean isAntialiasEnabled()
	{
		return antialiasEnabled;
	}

	@Override
	public void setAntialiasEnabled(boolean b)
	{
		if (this.antialiasEnabled != b)
		{
			this.antialiasEnabled = b;
			View.instance.setAntialiasOn(antialiasEnabled);
			fireViewChange(PROPERTY_ANTIALIAS_CHANGED);
		}
	}

	private void updateSuperimpose(boolean animateSuperimpose)
	{
		guiControler.block("superimposing and zooming");

		final List<Cluster> c = new ArrayList<Cluster>();
		final Cluster activeCluster;
		boolean superimpose = clustering.isSuperimposed();

		final boolean setAntialiasBackOn;
		if (antialiasEnabled && view.isAntialiasOn())
		{
			setAntialiasBackOn = true;
			view.setAntialiasOn(false);
		}
		else
			setAntialiasBackOn = false;

		if (clustering.isClusterActive())
		{
			activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
			if (activeCluster.isSuperimposed() != superimpose)
				c.add(activeCluster);
		}
		else
		{
			for (Cluster cluster : clustering.getClusters())
				if (cluster.isSuperimposed() != superimpose)
					c.add(cluster);
			activeCluster = null;
		}

		clustering.getModelWatched().clearSelection();

		if (!superimpose && animateSuperimpose) // zoom out before spreading (explicitly fetch non-superimpose diameter)
		{
			if (activeCluster == null)
			{
				for (Cluster cluster : c) // paint solid before spreading out
				{
					updateCluster(clustering.indexOf(cluster), false);
					for (Model m : cluster.getModels())
						updateModel(m.getModelIndex(), false);
				}
				view.zoomTo(clustering, AnimationSpeed.SLOW, false);
			}
			else
				view.zoomTo(activeCluster, AnimationSpeed.SLOW, false);
		}

		for (Model model : clustering.getModels(true))
			if (model.isSphereVisible())
			{
				model.setSphereVisible(false);
				view.hideSphere(model);
			}

		if (c.size() > 0)
		{
			if (!animateSuperimpose)
				view.suspendAnimation("superimpose");
			clustering.setClusterOverlap(c, superimpose, View.AnimationSpeed.SLOW);
			if (!animateSuperimpose)
				view.proceedAnimation("superimpose");
		}

		if (superimpose || !animateSuperimpose) // zoom in after superimposing
		{
			final Zoomable zoom = activeCluster == null ? clustering : activeCluster;
			view.afterAnimation(new Runnable()
			{
				@Override
				public void run()
				{
					view.zoomTo(zoom, AnimationSpeed.SLOW);
				}
			}, "zoom to " + zoom);
		}

		view.afterAnimation(new Runnable()
		{
			@Override
			public void run()
			{
				for (final Cluster cluster : c)
				{
					updateCluster(clustering.indexOf(cluster), false);

					for (Model m : cluster.getModels())
						updateModel(m.getModelIndex(), false);
				}
				if (setAntialiasBackOn && !view.isAntialiasOn())
				{
					ThreadUtil.sleep(200);
					view.setAntialiasOn(true);
				}
			}
		}, "set models translucent in clusters");

		view.afterAnimation(new Runnable()
		{
			@Override
			public void run()
			{
				guiControler.unblock("superimposing and zooming");
				fireViewChange(PROPERTY_SUPERIMPOSE_CHANGED);
				fireViewChange(PROPERTY_DENSITY_CHANGED);
			}
		}, "do unblock wenn all done");
	}

	List<PropertyChangeListener> viewListeners = new ArrayList<PropertyChangeListener>();

	@Override
	public void addViewListener(PropertyChangeListener l)
	{
		viewListeners.add(l);
	}

	private void fireViewChange(String prop)
	{
		for (PropertyChangeListener l : viewListeners)
			l.propertyChange(new PropertyChangeEvent(this, prop, "old", "new"));
	}

	@Override
	public boolean canChangeCompoundSize(boolean larger)
	{
		if (larger && ClusteringUtil.COMPOUND_SIZE == ClusteringUtil.COMPOUND_SIZE_MAX)
			return false;
		if (!larger && ClusteringUtil.COMPOUND_SIZE == 0)
			return false;
		Cluster activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
		if (activeCluster == null)
			return true;
		else
			return !clustering.isSuperimposed();
	}

	@Override
	public void changeCompoundSize(boolean larger)
	{
		if (larger && ClusteringUtil.COMPOUND_SIZE < ClusteringUtil.COMPOUND_SIZE_MAX)
		{
			ClusteringUtil.COMPOUND_SIZE++;
			updateDensitiy();
		}
		else if (!larger && ClusteringUtil.COMPOUND_SIZE > 0)
		{
			ClusteringUtil.COMPOUND_SIZE--;
			updateDensitiy();
		}
	}

	@Override
	public void setCompoundSize(int compoundSize)
	{
		if (ClusteringUtil.COMPOUND_SIZE != compoundSize)
		{
			ClusteringUtil.COMPOUND_SIZE = compoundSize;
			updateDensitiy();
		}
	}

	@Override
	public HighlightMode getHighlightMode()
	{
		return highlightMode;
	}

	@Override
	public void setHighlightMode(HighlightMode mode)
	{
		if (highlightMode != mode)
		{
			highlightMode = mode;
			if (selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndModels(true);
			fireViewChange(PROPERTY_HIGHLIGHT_MODE_CHANGED);
		}
	}

	@Override
	public boolean isHighlightLastFeatureEnabled()
	{
		return highlightLastFeatureEnabled;
	}

	@Override
	public void setHighlightLastFeatureEnabled(boolean b)
	{
		if (highlightLastFeatureEnabled != b)
		{
			highlightLastFeatureEnabled = b;
			if (highlightMode == HighlightMode.Spheres && selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndModels(true);
			fireViewChange(PROPERTY_HIGHLIGHT_LAST_FEATURE);
		}
	}

	@Override
	public void setSphereSize(double size)
	{
		if (view.sphereSize != size)
		{
			view.sphereSize = size;
			if (highlightMode == HighlightMode.Spheres && selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndModels(true);
		}
	}

	@Override
	public void setSphereTranslucency(double translucency)
	{
		if (view.sphereTranslucency != translucency)
		{
			view.sphereTranslucency = translucency;
			if (highlightMode == HighlightMode.Spheres && selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndModels(true);
		}
	}

	@Override
	public int getCompoundSizeMax()
	{
		return ClusteringUtil.COMPOUND_SIZE_MAX;
	}

	@Override
	public int getCompoundSize()
	{
		return ClusteringUtil.COMPOUND_SIZE;
	}

	private void updateDensitiy()
	{
		Cluster activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
		if (activeCluster != null && clustering.isSuperimposed())
			throw new IllegalStateException("does not make sense, because superimposed!");

		if (activeCluster != null)
		{
			if (clustering.isModelActive())
				clustering.getModelActive().clearSelection();
		}

		//		if (higher)
		//			ClusteringUtil.DENSITY = ClusteringUtil.DENSITY - 0.1f;
		//		else
		//			ClusteringUtil.DENSITY = ClusteringUtil.DENSITY + 0.1f;

		view.suspendAnimation("change density");
		clustering.updatePositions();
		if (activeCluster != null)
		{
			Settings.LOGGER.info("zooming out - cluster");
			view.zoomTo(activeCluster, null);
		}
		else
		{
			Settings.LOGGER.info("zooming out - home");
			view.zoomTo(clustering, null);
		}
		for (Model model : clustering.getModels(true))
			if (model.isSphereVisible())
				view.showSphere(model, model.isLastFeatureSphereVisible(), true);
		view.proceedAnimation("change density");

		fireViewChange(PROPERTY_DENSITY_CHANGED);
	}

	@Override
	public boolean isHideUnselected()
	{
		return hideUnselected;
	}

	@Override
	public void setHideUnselected(boolean hide)
	{
		if (this.hideUnselected != hide)
		{
			hideUnselected = hide;
			updateAllClustersAndModels(false);
			fireViewChange(PROPERTY_HIDE_UNSELECT_CHANGED);
		}
	}

	@Override
	public boolean isHideHydrogens()
	{
		return hideHydrogens;
	}

	@Override
	public void setHideHydrogens(boolean b)
	{
		this.hideHydrogens = b;
		view.hideHydrogens(b);
	}

	@Override
	public boolean isBlackgroundBlack()
	{
		return backgroundBlack;
	}

	@Override
	public void setBlackgroundBlack(boolean backgroundBlack)
	{
		setBlackgroundBlack(backgroundBlack, false);
	}

	public void setBlackgroundBlack(boolean backgroundBlack, boolean forceUpdate)
	{
		if (backgroundBlack != this.backgroundBlack || forceUpdate)
		{
			this.backgroundBlack = backgroundBlack;
			ComponentFactory.setBackgroundBlack(backgroundBlack);
			view.setBackground(ComponentFactory.BACKGROUND);
			fireViewChange(PROPERTY_BACKGROUND_CHANGED);
		}
	}

	@Override
	public void setMatchColor(Color color)
	{
		if (!matchColor.equals(color))
		{
			this.matchColor = color;
			updateAllClustersAndModels(true);
			fireViewChange(PROPERTY_MATCH_COLOR_CHANGED);
		}
	}

	@Override
	public Color getMatchColor()
	{
		return matchColor;
	}

	@SuppressWarnings("unchecked")
	private void initMoleculeDescriptor()
	{
		if (modelDescriptorProperty != COMPOUND_INDEX_PROPERTY && modelDescriptorProperty != COMPOUND_SMILES_PROPERTY
				&& !clustering.getFeatures().contains(modelDescriptorProperty)
				&& !clustering.getProperties().contains(modelDescriptorProperty))
			modelDescriptorProperty = null;
		if (modelDescriptorProperty == null)
		{
			for (String names : new String[] { "(?i)^name$", "(?i).*name.*", "(?i)^id$", "(?i).*id.*", "(?i)^cas$",
					"(?i).*cas.*" })
			{
				for (List<MoleculeProperty> props : new List[] { clustering.getFeatures(), clustering.getProperties() })
				{
					for (MoleculeProperty p : props)
					{
						if (p.toString().matches(names)
								&& clustering.numDistinctValues(p) > (clustering.numModels() * 3 / 4.0))
						{
							modelDescriptorProperty = p;
							break;
						}
					}
					if (modelDescriptorProperty != null)
						break;
				}
				if (modelDescriptorProperty != null)
					break;
			}
		}
		if (modelDescriptorProperty == null)
			modelDescriptorProperty = COMPOUND_INDEX_PROPERTY;
		for (Model m : clustering.getModels(true))
			m.setDescriptor(modelDescriptorProperty);
	}

	@Override
	public void setMoleculeDescriptor(MoleculeProperty prop)
	{
		if (modelDescriptorProperty != prop)
		{
			modelDescriptorProperty = prop;
			for (Model m : clustering.getModels(true))
				m.setDescriptor(modelDescriptorProperty);
			fireViewChange(PROPERTY_MOLECULE_DESCRIPTOR_CHANGED);
		}
	}

	@Override
	public MoleculeProperty getMoleculeDescriptor()
	{
		return modelDescriptorProperty;
	}

	@Override
	public boolean isHighlightLogEnabled()
	{
		return highlightLogEnabled;
	}

	@Override
	public void setHighlightLogEnabled(boolean b)
	{
		if (b != highlightLogEnabled)
		{
			highlightLogEnabled = b;
			updateAllClustersAndModels(true);
			fireViewChange(PROPERTY_HIGHLIGHT_LOG_CHANGED);
		}
	}
}
