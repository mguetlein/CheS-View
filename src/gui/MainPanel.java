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

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolAdapter;
import org.jmol.api.JmolSimpleViewer;

import util.ArrayUtil;
import util.ColorUtil;
import util.DoubleUtil;
import util.ObjectUtil;
import util.StringUtil;
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
	boolean hideUnselected = true;

	private String getColor(Model m)
	{
		if (selectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER)
			return "color "
					+ ColorUtil.toJMolString(MoleculePropertyUtil.getColor(clustering.getClusterIndexForModel(m)));
		else if (selectedHighlightMoleculeProperty == null)
			return "color cpk";
		else if (m.getTemperature(selectedHighlightMoleculeProperty).equals("null"))
			return "color " + ColorUtil.toJMolString(Color.DARK_GRAY);
		else if (selectedHighlightMoleculeProperty.getType() == Type.NOMINAL)
			return "color "
					+ ColorUtil.toJMolString(MoleculePropertyUtil.getNominalColor(selectedHighlightMoleculeProperty,
							m.getStringValue(selectedHighlightMoleculeProperty)));
		else
			return "color FIXEDTEMPERATURE";
	}

	public static enum Translucency
	{
		None, ModerateWeak, ModerateStrong, Strong;
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
		//		System.err.println(trans);
		return "; color translucent " + trans;
	}

	HashMap<String, Highlighter[]> highlighters;
	Highlighter selectedHighlighter = Highlighter.CLUSTER_HIGHLIGHTER;
	MoleculeProperty selectedHighlightMoleculeProperty = null;
	boolean highlighterLabelsVisible = false;
	HighlightSorting highlightSorting = HighlightSorting.Median;
	HighlightAutomatic highlightAutomatic;
	boolean backgroundBlack = true;

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
		View.init(jmolPanel, guiControler, hideHydrogens);
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
						if (!clustering.isClusterActive())
						{
							// set a cluster to active (zooming will be done in listener)
							clustering.getClusterActive().setSelected(
									clustering.getClusterIndexForModelIndex(view.getAtomModelIndex(atomIndex)));
							clustering.getClusterWatched().clearSelection();
						}
						else
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
				int atomIndex = view.findNearestAtomIndex(e.getX(), e.getY());

				if (!clustering.isClusterActive())
				{
					if (atomIndex == -1)
					{
						// // do not clear cluster selection
						clustering.getClusterWatched().clearSelection();
					}
					else
						clustering.getClusterWatched().setSelected(
								(clustering.getClusterIndexForModelIndex(view.getAtomModelIndex(atomIndex))));
				}
				else
				{
					if (atomIndex == -1)
					{
						// // do not clear model selection
						clustering.getModelWatched().clearSelection();
					}
					else
						clustering.getModelWatched().setSelected(view.getAtomModelIndex(atomIndex));
				}
			}
		});
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

		if (!clusterVisible)
			toHide.or(c.getBitSet());
		else
		{
			if (!hideSomeModels)
				toDisplay.or(c.getBitSet());
			else
			{
				int numModels = c.getModels().size();
				int max;
				if (numModels <= 10)
					max = 10;
				else
					max = (numModels - 10) / 3 + 10;
				max = Math.min(25, max);
				//			System.err.println("hiding: ''" + hide + "'', num '" + numModels + "', max '" + max + "'");

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
		view.hide(toHide);
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
	 * forceUpdate = true -> everything is reset (independent of model is part of active cluster or if single props habve changed)
	 * 
	 * shows/hides box around model
	 * show/hides model label
	 * set model translucent/opaque
	 * highlight substructure in model 
	 */
	private void updateModel(int i, boolean forceUpdate)
	{
		int clus = clustering.getClusterIndexForModelIndex(i);
		Cluster c = clustering.getCluster(clus);
		Model m = clustering.getModelWithModelIndex(i);
		if (m == null)
		{
			System.err.println("model is null!");
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
			if (!clustering.isModelWatched() && !clustering.isModelActive())
				translucent = false;
			else
			{
				if (clustering.getModelWatched().isSelected(i) || clustering.getModelActive().isSelected(i))
					translucent = false;
				else
					translucent = true;
			}

			if (selectedHighlighter instanceof MoleculePropertyHighlighter)
				showLabel = true;

			if (clustering.getModelWatched().isSelected(i) && !c.isSuperimposed())
				showHoverBox = true;
			if (clustering.getModelActive().isSelected(i) && !c.isSuperimposed())
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
				if (watchedCluster == -1 || selectedHighlighter == Highlighter.CLUSTER_HIGHLIGHTER)
					translucent = false;
				else
					translucent = (clus != watchedCluster);
			}
		}

		String smarts = null;
		if (selectedHighlighter instanceof SubstructureHighlighter)
			smarts = c.getSubstructureSmarts(((SubstructureHighlighter) selectedHighlighter).getType());
		else if (selectedHighlighter instanceof MoleculePropertyHighlighter
				&& ((MoleculePropertyHighlighter) selectedHighlighter).getProperty().isSmartsProperty())
			smarts = ((MoleculePropertyHighlighter) selectedHighlighter).getProperty().getSmarts();

		if (!highlighterLabelsVisible)
			showLabel = false;

		BitSet bs = view.getModelUndeletedAtomsBitSet(i);
		view.select(bs);
		String color = getColor(m);

		boolean labelUpdate = showLabel
				&& (!color.equals(m.getColor()) || selectedHighlightMoleculeProperty != m
						.getHighlightMoleculeProperty());

		Translucency translucency;
		if (translucent)
		{
			if (clustering.isSuperimposed())
				translucency = Translucency.Strong;
			else if (hideUnselected)
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

		boolean colorUpdated = false;
		if (forceUpdate || translucency != m.getTranslucency() || !color.equals(m.getColor())
				|| selectedHighlightMoleculeProperty != m.getHighlightMoleculeProperty() || !style.equals(m.getStyle()))
		{
			colorUpdated = true;
			if (forceUpdate || !style.equals(m.getStyle()))
			{
				m.setStyle(style);
				view.scriptWait(style);
			}
			m.setTranslucency(translucency);
			m.setColor(color);
			m.setHighlightMoleculeProperty(selectedHighlightMoleculeProperty);

			view.scriptWait(color + getColorSuffixTranslucent(translucency));
		}

		if (forceUpdate || showHoverBox != m.isShowHoverBox())
		{
			m.setShowHoverBox(showHoverBox);
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
			{
				view.scriptWait("draw bb" + m.getModelIndex() + "h OFF");
			}
		}

		if (forceUpdate || showActiveBox != m.isShowActiveBox())
		{
			m.setShowActiveBox(showActiveBox);
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
		if (forceUpdate || colorUpdated || smarts != m.getHighlightedSmarts())
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
					view.scriptWait("color orange" + getColorSuffixTranslucent(translucency));
				}
			}
			if (!match)// || forceUpdate || translucentUpdate)
			{
				m.setHighlightedSmarts(null);
				view.scriptWait(color + getColorSuffixTranslucent(translucency));
			}
		}

		String label = null;
		if (showLabel || labelUpdate)
		{
			if (activeCluster == -1 && clustering.isSuperimposed())
			{
				label = c.getName() + " - " + ((MoleculePropertyHighlighter) selectedHighlighter).getProperty() + ": "
						+ c.getSummaryStringValue(selectedHighlightMoleculeProperty);
			}
			else
			{
				Object val = clustering.getModelWithModelIndex(i).getTemperature(
						((MoleculePropertyHighlighter) selectedHighlighter).getProperty());
				Double d = DoubleUtil.parseDouble(val + "");
				if (d != null)
					val = StringUtil.formatDouble(d);
				//				System.err.println("label : " + i + " : " + c + " : " + val);
				label = ((MoleculePropertyHighlighter) selectedHighlighter).getProperty() + ": " + val;
			}
		}

		// CHANGES JMOL SELECTION !!!
		if (forceUpdate || labelUpdate || !ObjectUtil.equals(label, m.getLabel()))
		{
			m.setLabel(label);
			BitSet empty = new BitSet(bs.length());
			empty.set(bs.nextSetBit(0));

			view.select(empty);
			if (showLabel)
			{

				view.scriptWait("set fontSize " + View.FONT_SIZE);
				view.scriptWait("label \"" + label + "\"");
			}
			else
			{
				//				System.err.println("label : " + i + " : " + c + " : off");
				view.scriptWait("label OFF");
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
				int mIndexOldArray[] = ((int[]) e.getOldValue());
				int mIndexOld = mIndexOldArray.length == 0 ? -1 : mIndexOldArray[0];
				updateModelWatchedSelection(clustering.getModelWatched().getSelected(), mIndexOld);
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

		List<MoleculeProperty> props = clustering.getFeatures();
		MoleculePropertyHighlighter featureHighlighters[] = new MoleculePropertyHighlighter[props.size()];
		int fCount = 0;
		for (MoleculeProperty p : props)
			featureHighlighters[fCount++] = new MoleculePropertyHighlighter(p);
		highlighters.put("Features used for mapping", featureHighlighters);

		props = clustering.getProperties();
		featureHighlighters = new MoleculePropertyHighlighter[props.size()];
		fCount = 0;
		for (MoleculeProperty p : props)
			featureHighlighters[fCount++] = new MoleculePropertyHighlighter(p);
		highlighters.put("Features NOT used for mapping", featureHighlighters);
		fireViewChange(PROPERTY_NEW_HIGHLIGHTERS);

		updateAllClustersAndModels(true);

		view.evalString("frame " + view.getModelNumberDotted(0) + " "
				+ view.getModelNumberDotted(clustering.numModels() - 1));

		setSpinEnabled(isSpinEnabled(), true);

		initHighlighter();

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

	private void updateModelWatchedSelection(int mIndex, int mIndexOld)
	{
		//		System.out.println("update model active: " + active + " " + ArrayUtil.toString(mIndex));
		int activeCluster = clustering.getClusterActive().getSelected();
		for (Model m : clustering.getCluster(activeCluster).getModels())
			updateModel(m.getModelIndex(), false);
	}

	private void updateModelActiveSelection(int mIndex[], int mIndexOld[], boolean zoomIntoModel)
	{
		//		System.out.println("update model active: " + active + " " + ArrayUtil.toString(mIndex));
		int activeCluster = clustering.getClusterActive().getSelected();
		for (Model m : clustering.getCluster(activeCluster).getModels())
			updateModel(m.getModelIndex(), false);

		if (activeCluster != -1 && !clustering.isSuperimposed())
		{
			if (mIndex.length != 0 && zoomIntoModel)
			{
				if (mIndex.length != 1)
					throw new IllegalStateException();
				final Model m = clustering.getModelWithModelIndex(mIndex[0]);
				view.zoomTo(m, AnimationSpeed.SLOW);
			}
			else if (mIndex.length == 0 && mIndexOld.length > 0)
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

		System.out.println("updating cluster selection: " + cIndex + " " + cIndexOld + " " + activeClusterChanged);

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

		public void paint(Graphics g)
		{
			getSize(currentSize);
			g.getClipBounds(rectClip);
			if (g != null && currentSize != null && rectClip != null)
				viewer.renderScreenImage(g, currentSize, rectClip);
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

	private void updateSuperimpose(boolean animateSuperimpose)
	{
		guiControler.block("superimposing and zooming");

		final List<Cluster> c = new ArrayList<Cluster>();
		final Cluster activeCluster;
		boolean superimpose = clustering.isSuperimposed();

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
	public boolean canChangeDensitiy(boolean higher)
	{
		if (higher && ClusteringUtil.DENSITY <= 0.1f)
			return false;
		Cluster activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
		if (activeCluster == null)
			return true;
		else
			return !clustering.isSuperimposed();
	}

	@Override
	public void setDensitiyHigher(boolean higher)
	{
		Cluster activeCluster = clustering.getCluster(clustering.getClusterActive().getSelected());
		if (activeCluster != null && clustering.isSuperimposed())
			throw new IllegalStateException("does not make sense, because superimposed!");

		if (activeCluster != null)
		{
			if (clustering.isModelActive())
				clustering.getModelActive().clearSelection();
		}

		if (higher)
			ClusteringUtil.DENSITY = ClusteringUtil.DENSITY - 0.1f;
		else
			ClusteringUtil.DENSITY = ClusteringUtil.DENSITY + 0.1f;

		view.suspendAnimation("change density");
		clustering.updatePositions();
		if (activeCluster != null)
		{
			System.out.println("zooming out - cluster");
			view.zoomTo(activeCluster, null);
		}
		else
		{
			System.out.println("zooming out - home");
			view.zoomTo(clustering, null);
		}
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

}
