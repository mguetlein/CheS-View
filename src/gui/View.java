package gui;

import gui.MainPanel.JmolPanel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import main.Settings;

import org.jmol.viewer.Viewer;

import util.SequentialWorkerThread;
import util.Vector3fUtil;

public class View
{
	private Viewer viewer;
	GUIControler guiControler;
	public static View instance;

	public static int FONT_SIZE = 10;

	public static enum AnimationSpeed
	{
		SLOW, FAST
	}

	private View(Viewer viewer, GUIControler guiControler, boolean hideHydrogens)
	{
		this.viewer = viewer;
		this.guiControler = guiControler;

		viewer.script("set disablePopupMenu on");
		viewer.script("set minPixelSelRadius 40");

		if (Settings.SCREENSHOT_SETUP)
			viewer.script("set antialiasDisplay ON");

		hideHydrogens(hideHydrogens);
	}

	public static void init(JmolPanel jmolPanel, GUIControler guiControler, boolean hideHydrogens)
	{
		instance = new View((Viewer) jmolPanel.getViewer(), guiControler, hideHydrogens);
	}

	public synchronized void setSpinEnabled(boolean spinEnabled)
	{
		if (spinEnabled)
		{
			viewer.evalString("set spinx 0");
			viewer.evalString("set spiny 3");
			viewer.evalString("set spinz 0");
			viewer.evalString("spin on");
		}
		else
		{
			viewer.evalString("spin off");
		}
	}

	public synchronized void zoomTo(Zoomable zoomable, AnimationSpeed speed)
	{
		zoomTo(zoomable, speed, null);
	}

	public synchronized void zoomTo(Zoomable zoomable, final AnimationSpeed speed, Boolean superimposed)
	{
		if (superimposed == null)
			superimposed = zoomable.isSuperimposed();
		final float diameter = zoomable.getDiameter(superimposed);
		final Vector3f center = zoomable.getCenter(superimposed);

		System.err.println("Superimposed " + superimposed);
		System.err.println("Center       " + center);
		System.err.println("Diameter     " + diameter);
		//		System.err.println("Rot radius   " + viewer.getRotationRadius());

		int zoom = (int) ((1200 / (15 / viewer.getRotationRadius())) / diameter);
		//		System.err.println("zoom " + zoom);
		zoom = (int) Math.max(5, zoom);

		if (isAnimated())
		{
			guiControler.block("zoom to " + Vector3fUtil.toString(center));
			final int finalZoom = zoom;
			sequentially(new Runnable()
			{
				@Override
				public void run()
				{
					String cmd = "zoomto " + (speed == AnimationSpeed.SLOW ? 0.66 : 0.33) + " "
							+ Vector3fUtil.toString(center) + " " + finalZoom;
					viewer.scriptWait(cmd);
					guiControler.unblock("zoom to " + Vector3fUtil.toString(center));
				}
			}, "zoom to " + zoomable);
		}
		else
		{
			String cmd = "zoomto 0 " + Vector3fUtil.toString(center) + " " + zoom;
			viewer.scriptWait(cmd);
		}

	}

	public synchronized static String color(Color col)
	{
		return "[" + col.getRed() + ", " + col.getGreen() + ", " + col.getBlue() + "]";
	}

	public synchronized void setBackground(Color col)
	{
		viewer.script("background " + color(col));
	}

	public synchronized int findNearestAtomIndex(int x, int y)
	{
		return viewer.findNearestAtomIndex(x, y);
	}

	public synchronized int getAtomModelIndex(int atomIndex)
	{
		return viewer.getAtomModelIndex(atomIndex);
	}

	public synchronized void clearSelection()
	{
		viewer.clearSelection();
	}

	public synchronized void select(BitSet bitSet)
	{
		//		System.err.println("XX> selecting bitset: " + bitSet);
		viewer.select(bitSet, false, null, false);
	}

	private void evalScript(String script)
	{
		if (script.matches("(?i).*hide.*") || script.matches("(?i).*subset.*") || script.matches("(?i).*display.*"))
			throw new Error("use wrap methods");
	}

	public synchronized void scriptWait(String script)
	{
		evalScript(script);
		//		System.err.println("XX> " + script);
		viewer.scriptWait(script);
	}

	public synchronized void evalString(String script)
	{
		//		System.err.println("XX> " + script);
		evalScript(script);
		viewer.evalString(script);
	}

	public synchronized void selectAll()
	{
		viewer.scriptWait("select not hidden");
	}

	public synchronized void hide(BitSet bs)
	{
		//		System.err.println("XX> hide " + bs);
		viewer.select(bs, false, null, false);
		hideSelected();
	}

	public synchronized void hideSelected()
	{
		//		System.err.println("XX> select selected OR hidden; hide selected");
		viewer.scriptWait("select selected OR hidden; hide selected");
	}

	public synchronized void display(BitSet bs)
	{
		//		System.err.println("XX> display " + bs);
		viewer.select(bs, false, null, false);
		viewer.scriptWait("select (not hidden) OR selected; select not selected; hide selected");
	}

	public synchronized BitSet getModelUndeletedAtomsBitSet(int modelIndex)
	{
		return viewer.getModelUndeletedAtomsBitSet(modelIndex);
	}

	public synchronized String getModelNumberDotted(int i)
	{
		return viewer.getModelNumberDotted(i);
	}

	public synchronized Point3f getAtomSetCenter(BitSet bitSet)
	{
		return viewer.getAtomSetCenter(bitSet);
	}

	public synchronized void zap(boolean b, boolean c, boolean d)
	{
		viewer.zap(b, c, d);
	}

	public synchronized void loadModelFromFile(String s, String filename, String s2[], Object o, boolean b,
			Hashtable<String, Object> t, StringBuffer sb, int i)
	{
		viewer.loadModelFromFile(s, filename, s2, o, b, t, sb, i);
	}

	public synchronized int getModelCount()
	{
		return viewer.getModelCount();
	}

	public synchronized void setAtomCoordRelative(Vector3f c, BitSet bitSet)
	{
		viewer.setAtomCoordRelative(c, bitSet);
	}

	public synchronized void setAtomCoordRelative(final List<Vector3f> c, final List<BitSet> bitSet,
			final AnimationSpeed overlapAnim)
	{
		if (isAnimated() && c.size() > 1)
		{
			guiControler.block("spread cluster " + c);
			sequentially(new Runnable()
			{
				@Override
				public void run()
				{
					int n = (overlapAnim == AnimationSpeed.SLOW) ? 24 : 10;
					for (int i = 0; i < n; i++)
					{
						for (int j = 0; j < bitSet.size(); j++)
						{
							Vector3f v = new Vector3f(c.get(j));
							v.scale(1 / (float) n);
							viewer.setAtomCoordRelative(v, bitSet.get(j));
						}
						viewer.scriptWait("delay 0.01");
					}
					guiControler.unblock("spread cluster " + c);
				}
			}, "move bitsets");
		}
		else
		{
			for (int i = 0; i < bitSet.size(); i++)
				viewer.setAtomCoordRelative(c.get(i), bitSet.get(i));
		}
	}

	public synchronized void setAtomProperty(BitSet bitSet, int temperature, int v, float v2, String string, float f[],
			String s[])
	{
		viewer.setAtomProperty(bitSet, temperature, v, v2, string, f, s);
	}

	public synchronized int getAtomCountInModel(int index)
	{
		return viewer.getAtomCountInModel(index);
	}

	public synchronized BitSet getSmartsMatch(String smarts, BitSet bitSet)
	{
		BitSet b = viewer.getSmartsMatch(smarts, bitSet);
		if (b == null)
		{
			System.err.println("jmol did not like: " + smarts + " " + bitSet);
			return new BitSet();
		}
		else
			return b;
	}

	HashSet<String> animSuspend = new HashSet<String>();

	public synchronized void suspendAnimation(String key)
	{
		if (animSuspend.contains(key))
			throw new Error("already suspended animation for: " + key);
		animSuspend.add(key);
	}

	public synchronized void proceedAnimation(String key)
	{
		if (!animSuspend.contains(key))
			throw new Error("use suspend first for " + key);
		animSuspend.remove(key);
	}

	public synchronized boolean isAnimated()
	{
		return animSuspend.size() == 0;
	}

	SequentialWorkerThread swt = new SequentialWorkerThread();

	private void sequentially(final Runnable r, final String name)
	{
		if (swt.runningInThread())
			r.run();
		else
			swt.addJob(r, name);
	}

	public void afterAnimation(final Runnable r, final String name)
	{
		if (isAnimated())
			swt.addJob(r, name);
		else
			r.run();
	}

	public synchronized void hideHydrogens(boolean b)
	{
		scriptWait("set showHydrogens " + (b ? "FALSE" : "TRUE"));
	}

	public float getDiameter(BitSet bitSet)
	{
		List<Vector3f> points = new ArrayList<Vector3f>();
		for (int i = 0; i < bitSet.size(); i++)
			if (bitSet.get(i))
				points.add(new Vector3f(viewer.getAtomPoint3f(i)));
		Vector3f[] a = new Vector3f[points.size()];
		return Vector3fUtil.maxDist(points.toArray(a));
	}
}
