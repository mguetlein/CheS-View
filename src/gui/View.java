package gui;

import gui.MainPanel.JmolPanel;

import java.awt.Color;
import java.util.BitSet;
import java.util.Hashtable;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import org.jmol.viewer.Viewer;

import util.SequentialWorkerThread;
import util.Vector3fUtil;

public class View
{
	private Viewer viewer;
	boolean animated = true;
	GUIControler guiControler;
	public static View instance;

	private View(Viewer viewer, GUIControler guiControler)
	{
		this.viewer = viewer;
		this.guiControler = guiControler;

		viewer.script("set disablePopupMenu on");
		//		viewer.script("hover OFF");
		//		viewer.script("label OFF");
		//		viewer.script("set drawHover OFF");
		//		viewer.script("set labelAtom OFF");
		//		viewer.script("set hoverLabel ''");
	}

	public static void init(JmolPanel jmolPanel, GUIControler guiControler)
	{
		instance = new View((Viewer) jmolPanel.getViewer(), guiControler);
	}

	public synchronized void setCurrentAnimationFrame(int firstModel, int lastModel)
	{
		viewer.evalString("frame " + viewer.getModelNumberDotted(firstModel) + " "
				+ viewer.getModelNumberDotted(lastModel));
	}

	public synchronized void selectModel(int modelIndex, int modelIndex2)
	{
		viewer.setCurrentModelIndex(modelIndex, false);
		viewer.setAnimationOn(false);
		viewer.setAnimationDirection(1);
		viewer.setAnimationRange(modelIndex, modelIndex2);
		viewer.setCurrentModelIndex(-1, false);
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

	public synchronized void zoomOut(final Vector3f center, final float time, float radius)
	{
		//		System.err.println("Radius    " + radius);
		int zoom = (int) (1200 / radius);
		//		System.err.println("zoom " + zoom);
		zoom = (int) Math.max(5, zoom);

		if (animated)
		{
			final int finalZoom = zoom;
			sequentially(new Runnable()
			{
				@Override
				public void run()
				{
					guiControler.block();
					String cmd = "zoomto " + time + " " + Vector3fUtil.toString(center) + " " + finalZoom;
					viewer.scriptWait(cmd);
					guiControler.unblock();
				}
			}, "zoom out");
		}
		else
		{
			String cmd = "zoomto 0 " + Vector3fUtil.toString(center) + " " + zoom;
			viewer.scriptWait(cmd);
		}

	}

	public synchronized void zoomIn(final Vector3f center)
	{
		if (animated)
		{
			sequentially(new Runnable()
			{
				@Override
				public void run()
				{
					guiControler.block();
					viewer.scriptWait("zoomto 1 " + Vector3fUtil.toString(center) + " 50");
					guiControler.unblock();
				}
			}, "zoom in");
		}
		else
		{
			viewer.scriptWait("zoomto 0 " + Vector3fUtil.toString(center) + " 50");
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

	public synchronized int sloppyFindNearestAtomIndex(int x, int y)
	{
		// 6px is the hard coded "epsilon" for clicking atoms
		// -> making it 36 ( (12+6)*2 )
		int xx[] = new int[] { x - 12, x, x + 12 };
		int yy[] = new int[] { y - 12, y, y + 12 };
		for (int i = 0; i < yy.length; i++)
		{
			for (int j = 0; j < yy.length; j++)
			{
				int index = viewer.findNearestAtomIndex(xx[i], yy[i]);
				if (index != -1)
					return index;
			}
		}
		return -1;
	}

	public synchronized int getAtomModelIndex(int atomIndex)
	{
		return viewer.getAtomModelIndex(atomIndex);
	}

	public synchronized void selectAll()
	{
		viewer.selectAll();
	}

	public synchronized void select(BitSet bitSet, boolean b)
	{
		//		System.err.println("XX> selecting bitset: " + bitSet);
		viewer.select(bitSet, b);
	}

	public synchronized void scriptWait(String script)
	{
		//		System.err.println("XX> " + script);
		viewer.scriptWait(script);
	}

	public synchronized void evalString(String script)
	{
		viewer.evalString(script);
	}

	public synchronized void clearBfactorRange()
	{
		viewer.clearBfactorRange();
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

	public synchronized int deleteAtoms(BitSet bitSet, boolean b)
	{
		return viewer.deleteAtoms(bitSet, b);
	}

	public synchronized void loadModelFromFile(String s, String filename, String s2[], Object o, boolean b,
			Hashtable<?, ?> t, StringBuffer sb, int i)
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

	public static enum MoveAnimation
	{
		SLOW, FAST, NONE
	}

	public synchronized void setAtomCoordRelative(final Vector3f[] c, final BitSet[] bitSet,
			final MoveAnimation overlapAnim)
	{
		if (animated && overlapAnim != MoveAnimation.NONE && c.length > 1)
		{
			sequentially(new Runnable()
			{
				@Override
				public void run()
				{
					guiControler.block();
					int n = (overlapAnim == MoveAnimation.SLOW) ? 12 : 25;
					for (int i = 0; i < n; i++)
					{
						for (int j = 0; j < bitSet.length; j++)
						{
							Vector3f v = new Vector3f(c[j]);
							v.scale(1 / (float) n);
							viewer.setAtomCoordRelative(v, bitSet[j]);
						}
						viewer.scriptWait("delay 0.01");
					}
					guiControler.unblock();
				}
			}, "move bitsets");
		}
		else
		{
			for (int i = 0; i < bitSet.length; i++)
				viewer.setAtomCoordRelative(c[i], bitSet[i]);
		}
	}

	public synchronized void setAtomProperty(BitSet bitSet, int temperature, int v, float v2, String string, float f[],
			String s[])
	{
		viewer.setAtomProperty(bitSet, temperature, v, v2, string, f, s);
	}

	public synchronized String getModelName(int index)
	{
		return viewer.getModelName(index);
	}

	public synchronized int getAtomCountInModel(int index)
	{
		return viewer.getAtomCountInModel(index);
	}

	public synchronized BitSet getSmartsMatch(String smarts, BitSet bitSet)
	{
		return viewer.getSmartsMatch(smarts, bitSet);
	}

	public void setAnimated(boolean b)
	{
		this.animated = b;
	}

	public boolean isAnimated()
	{
		return animated;
	}

	SequentialWorkerThread swt = new SequentialWorkerThread();

	private void sequentially(final Runnable r, final String name)
	{
		swt.addJob(r, name);
	}

	public void afterAnimation(final Runnable r, final String name)
	{
		if (animated)
			swt.addJob(r, name);
		else
			r.run();
	}
}
