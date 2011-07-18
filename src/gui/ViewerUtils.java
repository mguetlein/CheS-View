package gui;

import java.awt.Color;

import javax.vecmath.Vector3f;

import org.jmol.viewer.Viewer;

import util.Vector3fUtil;
import cluster.Clustering;

public class ViewerUtils
{
	Viewer viewer;
	Clustering clustering;

	public ViewerUtils(Viewer viewer, Clustering clustering)
	{
		this.viewer = viewer;
		this.clustering = clustering;
	}

	public void setCurrentAnimationFrame(int firstModel, int lastModel)
	{
		viewer.evalString("frame " + viewer.getModelNumberDotted(firstModel) + " "
				+ viewer.getModelNumberDotted(lastModel));
	}

	public void selectModel(int modelIndex, int modelIndex2)
	{
		viewer.setCurrentModelIndex(modelIndex, false);
		viewer.setAnimationOn(false);
		viewer.setAnimationDirection(1);
		viewer.setAnimationRange(modelIndex, modelIndex2);
		viewer.setCurrentModelIndex(-1, false);
	}

	public void setSpinEnabled(boolean spinEnabled)
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

	//	public void zoomOut(boolean wait)
	//	{
	//		zoomOut(wait, new Vector3f(0, 0, 0));
	//	}
	//
	//	public void zoomOut(boolean wait, Vector3f center)
	//	{
	//		zoomOut(wait, center, 1, -1);
	//	}
	//
	//	public void zoomOut(boolean wait, Vector3f center, float time)
	//	{
	//		zoomOut(wait, center, time, -1);
	//	}

	public void zoomOut(boolean wait, Vector3f center, float time, float zoomFactor, float radius)
	{

		//		System.out.println("llllllllllllllllll");
		System.out.println("Radius    " + radius);
		//System.out.println("Rotation  " + viewer.getRotationRadius());
		//		System.out.println("Rotation2 " + viewer.getModelSet().calcRotationRadius(bs));
		//		radius = viewer.getModelSet().calcRotationRadius(bs);

		int zoom;
		if (radius < 10)
			zoom = 50;
		else if (radius < 20)
			zoom = 30;
		else if (radius < 30)
			zoom = 30;
		else if (radius < 40)
			zoom = 30;
		else if (radius < 50)
			zoom = 30;
		else if (radius < 60)
			zoom = 25;
		else if (radius < 80)
			zoom = 20;
		else if (radius < 100)
			zoom = 15;
		else if (radius < 150)
			zoom = 10;
		else if (radius < 200)
			zoom = 5;
		else
			zoom = 5;

		zoom *= zoomFactor;
		zoom = (int) Math.max(5, zoom);

		//		if (clustering.getOrigSdfFile().matches(".*chang.*"))
		//			radius *= 0.4;
		//		else if (clustering.getOrigSdfFile().matches(".*NCTRER_v4b_232_15Feb2008.*"))
		//			radius *= 0.9;
		//		radius = Math.max(5, Math.min(radius, 50));
		//		System.out.println("Zoom    " + radius);

		//radius *= viewer.getRotationRadius() / 8.0;
		//		}

		//		viewer.moveTo(time, new Point3f(center.x, center.y, center.z), JmolConstants.center, Float.NaN, null, 10f, 0f,
		//				0f, Float.NaN, null, Float.NaN, Float.NaN, Float.NaN);

		//		int zoom = 10;
		//		if (radius == 0)
		//			zoom = 50;

		String cmd = "zoomto " + time + " " + Vector3fUtil.toString(center) + " " + zoom;
		if (wait)
			viewer.scriptWait(cmd);
		else
			viewer.evalString(cmd);
	}

	public static String color(Color col)
	{
		return "[" + col.getRed() + ", " + col.getGreen() + ", " + col.getBlue() + "]";
	}

	public void setBackground(Color col)
	{
		viewer.script("background " + color(col));
	}
}
