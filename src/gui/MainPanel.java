package gui;

import gui.View.AnimationSpeed;
import gui.swing.ComponentFactory;
import gui.util.CompoundPropertyHighlighter;
import gui.util.HighlightAutomatic;
import gui.util.Highlighter;
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
import util.ObjectUtil;
import util.ThreadUtil;
import cluster.Cluster;
import cluster.Clustering;
import cluster.ClusteringUtil;
import cluster.Compound;
import data.ClusteringData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundProperty.Type;
import dataInterface.CompoundPropertyUtil;
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
	CompoundProperty compoundDescriptorProperty = null;
	List<JComponent> ignoreMouseMovementPanels = new ArrayList<JComponent>();

	private Color getCompoundHighlightColor(Compound m, Highlighter h, CompoundProperty p)
	{
		if (h == Highlighter.CLUSTER_HIGHLIGHTER)
			return CompoundPropertyUtil.getColor(clustering.getClusterIndexForCompound(m));
		else if (p == null)
			return null;
		else if (m.getFormattedValue(p).equals("null"))
			return Color.DARK_GRAY;
		else if (p.getType() == Type.NOMINAL)
			return CompoundPropertyUtil.getNominalColor(p, m.getStringValue(p));
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
	CompoundProperty selectedHighlightCompoundProperty = null;
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
						boolean selectCompound = clustering.isClusterActive() || e.isShiftDown();

						if (selectCluster)
						{
							// set a cluster to active (zooming will be done in listener)
							clustering.getClusterActive().setSelected(
									clustering.getClusterIndexForCompoundIndex(view.getAtomCompoundIndex(atomIndex)));
							clustering.getClusterWatched().clearSelection();
						}
						if (selectCompound)
						{
							// already a cluster active, zooming into compound
							// final Compound m = clustering.getCompoundWithCompoundIndex(view.getAtomCompoundIndex(atomIndex));
							clustering.getCompoundWatched().clearSelection();
							if (e.isControlDown())
								clustering.getCompoundActive()
										.setSelectedInverted(view.getAtomCompoundIndex(atomIndex));
							else
							{
								if (clustering.getCompoundActive().isSelected(view.getAtomCompoundIndex(atomIndex)))
									clustering.getCompoundActive().clearSelection();
								else
									clustering.getCompoundActive().setSelected(view.getAtomCompoundIndex(atomIndex));
							}
						}
					}
					else
					{
						clustering.getCompoundWatched().clearSelection();
						clustering.getClusterWatched().clearSelection();
						clustering.getCompoundActive().clearSelection();
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
				clustering.getCompoundWatched().clearSelection();
				//clustering.getCompoundActive().clearSelection();
				clustering.getClusterWatched().setSelected(
						(clustering.getClusterIndexForCompoundIndex(view.getAtomCompoundIndex(atomIndex))));
			}
		}
		else
		{
			if (atomIndex == -1)
			{
				// // do not clear compound selection
				clustering.getCompoundWatched().clearSelection();
			}
			else
			{
				clustering.getClusterWatched().clearSelection();
				clustering.getCompoundWatched().setSelected(view.getAtomCompoundIndex(atomIndex));
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
			updateAllClustersAndCompounds(false);
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
			if (selectedHighlighter instanceof CompoundPropertyHighlighter)
				selectedHighlightCompoundProperty = ((CompoundPropertyHighlighter) highlighter).getProperty();
			else
				selectedHighlightCompoundProperty = null;

			updateAllClustersAndCompounds(false);
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
		}
	}

	@Override
	public void setHighlighter(CompoundProperty prop)
	{
		Highlighter high = null;
		for (Highlighter hs[] : highlighters.values())
		{
			for (Highlighter h : hs)
			{
				if (h instanceof CompoundPropertyHighlighter)
				{
					CompoundPropertyHighlighter m = (CompoundPropertyHighlighter) h;
					if (m.getProperty() == prop)
					{
						high = h;
						break;
					}
				}
			}
		}
		if (high != null)
			setHighlighter(high);
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
			updateAllClustersAndCompounds(false);
			fireViewChange(PROPERTY_HIGHLIGHT_CHANGED);
		}
	}

	@Override
	public void setHighlightSorting(HighlightSorting sorting)
	{
		if (this.highlightSorting != sorting)
		{
			highlightSorting = sorting;
			updateAllClustersAndCompounds(false);
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
	 * hides compounds
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
		boolean someCompoundsHidden = visible && active == -1 && clustering.isSuperimposed();

		if (forceUpdate
				|| visible != c.isVisible()
				|| (visible && (someCompoundsHidden != c.someCompoundsHidden()
						|| selectedHighlightCompoundProperty != c.getHighlightProperty() || highlightSorting != c
						.getHighlightSorting())))
		{
			c.setVisible(visible);
			c.setSomeCompoundsHidden(someCompoundsHidden);
			c.setHighlighProperty(selectedHighlightCompoundProperty);
			c.setHighlightSorting(highlightSorting);
			hideOrDisplayCluster(c, visible, someCompoundsHidden);
		}
	}

	private void hideOrDisplayCluster(Cluster c, boolean clusterVisible, boolean hideSomeCompounds)
	{
		BitSet toDisplay = new BitSet();
		BitSet toHide = new BitSet();
		List<Compound> toDisplayCompound = new ArrayList<Compound>();
		List<Compound> toHideCompound = new ArrayList<Compound>();

		if (!clusterVisible)
		{
			toHide.or(c.getBitSet());
			for (Compound compound : c.getCompounds())
				toHideCompound.add(compound);
		}
		else
		{
			if (!hideSomeCompounds)
			{
				toDisplay.or(c.getBitSet());
				for (Compound compound : c.getCompounds())
					toDisplayCompound.add(compound);
			}
			else
			{
				int numCompounds = c.getCompounds().size();
				int max;
				if (numCompounds <= 10)
					max = 10;
				else
					max = (numCompounds - 10) / 3 + 10;
				max = Math.min(25, max);
				//			Settings.LOGGER.warn("hiding: ''" + hide + "'', num '" + numCompounds + "', max '" + max + "'");

				if (numCompounds < max)
					toDisplay.or(c.getBitSet());
				else
				{
					List<Compound> compounds;
					if (selectedHighlighter instanceof CompoundPropertyHighlighter)
						compounds = c.getCompoundsInOrder(
								((CompoundPropertyHighlighter) selectedHighlighter).getProperty(), highlightSorting);
					else
						compounds = c.getCompounds();
					int count = 0;
					for (Compound m : compounds)
					{
						if (count++ >= max)
							toHide.or(m.getBitSet());
						else
							toDisplay.or(m.getBitSet());
					}
				}
			}
		}

		for (Compound compound : toHideCompound)
		{
			if (compound.isShowActiveBox())
				view.scriptWait("draw bb" + compound.getCompoundIndex() + "a OFF");
			if (compound.isSphereVisible())
				view.hideSphere(compound);
		}
		view.hide(toHide);

		for (Compound compound : toDisplayCompound)
		{
			if (compound.isShowActiveBox())
				view.scriptWait("draw bb" + compound.getCompoundIndex() + "a ON");
			if (compound.isSphereVisible())
				view.showSphere(compound, compound.isLastFeatureSphereVisible(), false);
		}
		view.display(toDisplay);
	}

	/**
	 * udpates all clusters
	 */
	private void updateAllClustersAndCompounds(boolean forceUpdate)
	{
		int a = getClustering().getClusterActive().getSelected();
		for (int j = 0; j < clustering.numClusters(); j++)
		{
			updateCluster(j, forceUpdate);
			if (a == -1 || a == j)
				for (Compound m : clustering.getCluster(j).getCompounds())
					updateCompound(m.getCompoundIndex(), forceUpdate);
		}
	}

	/**
	 * udpates single compound
	 * forceUpdate = true -> everything is reset (independent of compound is part of active cluster or if single props have changed)
	 * 
	 * shows/hides box around compound
	 * show/hides compound label
	 * set compound translucent/opaque
	 * highlight substructure in compound 
	 */
	private void updateCompound(int compoundIndex, boolean forceUpdate)
	{
		int clus = clustering.getClusterIndexForCompoundIndex(compoundIndex);
		Cluster c = clustering.getCluster(clus);
		Compound m = clustering.getCompoundWithCompoundIndex(compoundIndex);
		if (m == null)
		{
			Settings.LOGGER.warn("compound is null!");
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
			if (!clustering.isCompoundWatchedFromCluster(clus) && !clustering.isCompoundActiveFromCluster(clus))
				translucent = false;
			else
			{
				if (clustering.getCompoundWatched().isSelected(compoundIndex)
						|| clustering.getCompoundActive().isSelected(compoundIndex))
					translucent = false;
				else
					translucent = true;
			}

			if (selectedHighlighter instanceof CompoundPropertyHighlighter)
				showLabel = true;

			if (clustering.getCompoundWatched().isSelected(compoundIndex) && !c.isSuperimposed())
				showHoverBox = true;
			if (clustering.getCompoundActive().isSelected(compoundIndex) && !c.isSuperimposed())
				showActiveBox = true;
		}
		else
		{
			List<Compound> compounds;
			if (selectedHighlighter instanceof CompoundPropertyHighlighter)
				compounds = c.getCompoundsInOrder(((CompoundPropertyHighlighter) selectedHighlighter).getProperty(),
						highlightSorting);
			else
				compounds = c.getCompounds();

			if (selectedHighlightCompoundProperty != null
					&& (compounds.indexOf(m) == 0 || !clustering.isSuperimposed()))
				showLabel = true;

			translucent = false;
			if (clustering.isSuperimposed())
				translucent = (compounds.indexOf(m) > 0);
			else
			{
				if (clustering.isCompoundWatched() || clustering.isCompoundActive())
				{
					translucent = !(clustering.getCompoundWatched().isSelected(compoundIndex) || clustering
							.getCompoundActive().isSelected(compoundIndex));
				}
				else if (watchedCluster == -1 || selectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER)
					translucent = false;
				else
					translucent = (clus != watchedCluster);
			}

			if (clustering.getCompoundWatched().isSelected(compoundIndex) && !c.isSuperimposed())
				showHoverBox = true;
			if (clustering.getCompoundActive().isSelected(compoundIndex) && !c.isSuperimposed())
				showActiveBox = true;
		}

		String smarts = null;
		if (selectedHighlighter instanceof SubstructureHighlighter)
			smarts = c.getSubstructureSmarts(((SubstructureHighlighter) selectedHighlighter).getType());
		else if (selectedHighlighter instanceof CompoundPropertyHighlighter
				&& ((CompoundPropertyHighlighter) selectedHighlighter).getProperty().isSmartsProperty())
			smarts = ((CompoundPropertyHighlighter) selectedHighlighter).getProperty().getSmarts();

		if (!highlighterLabelsVisible)
			showLabel = false;

		BitSet bs = view.getCompoundBitSet(compoundIndex);
		view.select(bs);
		Color col = getCompoundHighlightColor(m, selectedHighlighter, selectedHighlightCompoundProperty);
		String highlightColor = col == null ? null : "color " + ColorUtil.toJMolString(col);
		String compoundColor;
		if (highlightMode == HighlightMode.Spheres)
			compoundColor = "color cpk";
		else
			compoundColor = col == null ? "color cpk" : "color " + ColorUtil.toJMolString(col);

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
			else if (hideUnselected || clustering.getCompoundActive().getSelected() != -1)
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
		boolean compoundUpdate = styleUpdate || translucency != m.getTranslucency()
				|| !ObjectUtil.equals(compoundColor, m.getCompoundColor());
		//showLabel is enough because superimpose<->not-superimpose is not tracked in compound
		boolean checkLabelUpdate = showLabel || (!showLabel && m.getLabel() != null)
				|| !ObjectUtil.equals(compoundColor, m.getCompoundColor())
				|| selectedHighlightCompoundProperty != m.getHighlightCompoundProperty();
		boolean sphereUpdate = sphereVisible != m.isSphereVisible()
				|| (lastFeatureSphereVisible != m.isLastFeatureSphereVisible())
				|| (sphereVisible && (translucency != m.getTranslucency() || !ObjectUtil.equals(highlightColor,
						m.getHighlightColor())));
		boolean spherePositionUpdate = sphereVisible && !ObjectUtil.equals(m.getPosition(), m.getSpherePosition());
		boolean hoverBoxUpdate = showHoverBox != m.isShowHoverBox();
		boolean activeBoxUpdate = showActiveBox != m.isShowActiveBox();
		boolean smartsUpdate = compoundUpdate || smarts != m.getHighlightedSmarts();

		m.setCompoundColor(compoundColor);
		m.setStyle(style);
		m.setTranslucency(translucency);
		m.setHighlightCompoundProperty(selectedHighlightCompoundProperty);
		m.setHighlightColor(highlightColor);
		m.setSpherePosition(m.getPosition());
		m.setSphereVisible(sphereVisible);
		m.setLastFeatureSphereVisible(lastFeatureSphereVisible);
		m.setShowHoverBox(showHoverBox);
		m.setShowActiveBox(showActiveBox);

		if (forceUpdate || styleUpdate)
			view.scriptWait(style);
		if (forceUpdate || compoundUpdate)
			view.scriptWait(compoundColor + getColorSuffixTranslucent(translucency));

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
				view.scriptWait("draw ID bb" + m.getCompoundIndex() + "h BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_WATCH_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL \"" + m.toString() + "\"");
				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
				view.scriptWait("draw bb" + m.getCompoundIndex() + "h OFF");
		}

		if (forceUpdate || activeBoxUpdate)
		{
			if (showActiveBox)
			{
				view.scriptWait("boundbox { selected }");
				view.scriptWait("boundbox off");
				view.scriptWait("draw ID bb" + m.getCompoundIndex() + "a BOUNDBOX color "
						+ ColorUtil.toJMolString(ComponentFactory.LIST_ACTIVE_BACKGROUND) + " translucent "
						+ boxTranslucency + " MESH NOFILL");
				//				jmolPanel.repaint(); // HACK to avoid label display errors
			}
			else
			{
				view.scriptWait("draw bb" + m.getCompoundIndex() + "a OFF");
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
					view.scriptWait(compoundColor + getColorSuffixTranslucent(translucency));
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
							+ ((CompoundPropertyHighlighter) selectedHighlighter).getProperty() + ": "
							+ c.getSummaryStringValue(selectedHighlightCompoundProperty);
				}
				else
				{
					CompoundProperty p = ((CompoundPropertyHighlighter) selectedHighlighter).getProperty();
					Object val = clustering.getCompoundWithCompoundIndex(compoundIndex).getFormattedValue(p);
					//				Settings.LOGGER.warn("label : " + i + " : " + c + " : " + val);
					labelString = ((CompoundPropertyHighlighter) selectedHighlighter).getProperty() + ": " + val;
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
		clustering.getCompoundActive().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				updateCompoundActiveSelection(((int[]) e.getNewValue()), ((int[]) e.getOldValue()), clustering
						.getCompoundActive().isExclusiveSelection());
			}
		});
		clustering.getCompoundWatched().addListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent e)
			{
				//				int mIndexOldArray[] = ((int[]) e.getOldValue());
				//				int mIndexOld = mIndexOldArray.length == 0 ? -1 : mIndexOldArray[0];
				updateCompoundWatchedSelection();//clustering.getCompoundWatched().getSelected(), mIndexOld);
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
		//, new CompoundPropertyHighlighter(clustering.getEmbeddingQualityProperty()) };
		if (clustering.getSubstructures().size() > 0)
			for (SubstructureSmartsType type : clustering.getSubstructures())
				h = ArrayUtil.concat(Highlighter.class, h, new Highlighter[] { new SubstructureHighlighter(type) });
		highlighters = new LinkedHashMap<String, Highlighter[]>();
		highlighters.put("", h);

		List<CompoundProperty> props = clustering.getProperties();
		CompoundPropertyHighlighter[] featureHighlighters = new CompoundPropertyHighlighter[props.size()];
		int fCount = 0;
		for (CompoundProperty p : props)
			featureHighlighters[fCount++] = new CompoundPropertyHighlighter(p);
		highlighters.put("Features NOT used for mapping", featureHighlighters);

		props = clustering.getFeatures();
		featureHighlighters = new CompoundPropertyHighlighter[props.size()];
		fCount = 0;
		for (CompoundProperty p : props)
			featureHighlighters[fCount++] = new CompoundPropertyHighlighter(p);
		highlighters.put("Features used for mapping", featureHighlighters);

		fireViewChange(PROPERTY_NEW_HIGHLIGHTERS);

		updateAllClustersAndCompounds(true);

		view.evalString("frame " + view.getCompoundNumberDotted(0) + " "
				+ view.getCompoundNumberDotted(clustering.numCompounds() - 1));

		setSpinEnabled(isSpinEnabled(), true);

		initHighlighter();
		initCompoundDescriptor();

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
		updateAllClustersAndCompounds(true);
		if (clustering.getNumClusters() == 0 && selectedHighlightCompoundProperty != null)
			initHighlighter();
		if (clustering.getNumClusters() == 1)
			clustering.getClusterActive().setSelected(0);
	}

	private void updateClusterModified()
	{
		updateAllClustersAndCompounds(true);
	}

	private void updateCompoundWatchedSelection()//int mIndex, int mIndexOld)
	{
		//		Settings.LOGGER.println("update compound active: " + active + " " + ArrayUtil.toString(mIndex));
		int activeCluster = clustering.getClusterActive().getSelected();
		Iterable<Compound> compounds;
		if (activeCluster != -1)
			compounds = clustering.getCluster(activeCluster).getCompounds();
		else
			compounds = clustering.getCompounds(true);
		for (Compound m : compounds)
			updateCompound(m.getCompoundIndex(), false);
	}

	private void updateCompoundActiveSelection(int mIndex[], int mIndexOld[], boolean zoomIntoCompound)
	{
		//		Settings.LOGGER.println("update compound active: " + active + " " + ArrayUtil.toString(mIndex));
		int activeCluster = clustering.getClusterActive().getSelected();
		Iterable<Compound> compounds;
		if (activeCluster != -1)
			compounds = clustering.getCluster(activeCluster).getCompounds();
		else
			compounds = clustering.getCompounds(true);
		for (Compound m : compounds)
			updateCompound(m.getCompoundIndex(), false);

		if (/*activeCluster != -1 &&*/!clustering.isSuperimposed())
		{
			// zoom because compound selection has changed
			if (mIndex.length != 0 && zoomIntoCompound)
			{
				// a compound selected
				if (mIndex.length == 1)
				{
					final Compound m = clustering.getCompoundWithCompoundIndex(mIndex[0]);
					view.zoomTo(m, AnimationSpeed.SLOW);
				}
			}
			else if (mIndex.length == 0 && mIndexOld.length > 0 && activeCluster != -1)
			{
				final Cluster c = getClustering().getClusterForCompoundIndex(mIndexOld[0]);
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

		//		Settings.LOGGER.info("updating cluster selection: " + cIndex + " " + cIndexOld + " " + activeClusterChanged);

		highlightAutomatic.resetClusterHighlighter(activeClusterChanged);

		updateAllClustersAndCompounds(false);
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

		clustering.getCompoundWatched().clearSelection();

		if (!superimpose && animateSuperimpose) // zoom out before spreading (explicitly fetch non-superimpose diameter)
		{
			if (activeCluster == null)
			{
				for (Cluster cluster : c) // paint solid before spreading out
				{
					updateCluster(clustering.indexOf(cluster), false);
					for (Compound m : cluster.getCompounds())
						updateCompound(m.getCompoundIndex(), false);
				}
				view.zoomTo(clustering, AnimationSpeed.SLOW, false);
			}
			else
				view.zoomTo(activeCluster, AnimationSpeed.SLOW, false);
		}

		for (Compound compound : clustering.getCompounds(true))
			if (compound.isSphereVisible())
			{
				compound.setSphereVisible(false);
				view.hideSphere(compound);
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

					for (Compound m : cluster.getCompounds())
						updateCompound(m.getCompoundIndex(), false);
				}
				if (setAntialiasBackOn && !view.isAntialiasOn())
				{
					ThreadUtil.sleep(200);
					view.setAntialiasOn(true);
				}
			}
		}, "set compounds translucent in clusters");

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
				updateAllClustersAndCompounds(true);
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
				updateAllClustersAndCompounds(true);
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
				updateAllClustersAndCompounds(true);
		}
	}

	@Override
	public void setSphereTranslucency(double translucency)
	{
		if (view.sphereTranslucency != translucency)
		{
			view.sphereTranslucency = translucency;
			if (highlightMode == HighlightMode.Spheres && selectedHighlighter != Highlighter.DEFAULT_HIGHLIGHTER)
				updateAllClustersAndCompounds(true);
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
			if (clustering.isCompoundActive())
				clustering.getCompoundActive().clearSelection();
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
		for (Compound compound : clustering.getCompounds(true))
			if (compound.isSphereVisible())
				view.showSphere(compound, compound.isLastFeatureSphereVisible(), true);
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
			updateAllClustersAndCompounds(false);
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
			updateAllClustersAndCompounds(true);
			fireViewChange(PROPERTY_MATCH_COLOR_CHANGED);
		}
	}

	@Override
	public Color getMatchColor()
	{
		return matchColor;
	}

	@SuppressWarnings("unchecked")
	private void initCompoundDescriptor()
	{
		if (compoundDescriptorProperty != COMPOUND_INDEX_PROPERTY
				&& compoundDescriptorProperty != COMPOUND_SMILES_PROPERTY
				&& !clustering.getFeatures().contains(compoundDescriptorProperty)
				&& !clustering.getProperties().contains(compoundDescriptorProperty))
			compoundDescriptorProperty = null;
		if (compoundDescriptorProperty == null)
		{
			for (String names : new String[] { "(?i)^name$", "(?i).*name.*", "(?i)^id$", "(?i).*id.*", "(?i)^cas$",
					"(?i).*cas.*" })
			{
				for (List<CompoundProperty> props : new List[] { clustering.getFeatures(), clustering.getProperties() })
				{
					for (CompoundProperty p : props)
					{
						// cond 1 : prop-name has to match
						// cond 2 : distinct values are > 75% of the dataset size
						// cond 3 : if its the id column, it has to either non-numeric or numeric & integer
						if (p.toString().matches(names)
								&& clustering.numDistinctValues(p) > (clustering.numCompounds() * 3 / 4.0)
								&& (!names.equals("(?i)^id$") || !names.equals("(?i).*id.*")
										|| p.getType() != Type.NUMERIC || p.isIntegerInMappedDataset()))
						{
							compoundDescriptorProperty = p;
							break;
						}
					}
					if (compoundDescriptorProperty != null)
						break;
				}
				if (compoundDescriptorProperty != null)
					break;
			}
		}
		if (compoundDescriptorProperty == null)
			compoundDescriptorProperty = COMPOUND_INDEX_PROPERTY;
		for (Compound m : clustering.getCompounds(true))
			m.setDescriptor(compoundDescriptorProperty);
	}

	@Override
	public void setCompoundDescriptor(CompoundProperty prop)
	{
		if (compoundDescriptorProperty != prop)
		{
			compoundDescriptorProperty = prop;
			for (Compound m : clustering.getCompounds(true))
				m.setDescriptor(compoundDescriptorProperty);
			fireViewChange(PROPERTY_COMPOUND_DESCRIPTOR_CHANGED);
		}
	}

	@Override
	public CompoundProperty getCompoundDescriptor()
	{
		return compoundDescriptorProperty;
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
			updateAllClustersAndCompounds(true);
			fireViewChange(PROPERTY_HIGHLIGHT_LOG_CHANGED);
		}
	}
}
