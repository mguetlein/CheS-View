package gui;

import java.util.Date;
import java.util.Locale;

import javax.swing.JFrame;

import main.BinHandler;
import main.CheSMapping;
import main.PropHandler;
import main.ScreenSetup;
import main.Settings;
import main.TaskProvider;
import task.Task;
import task.TaskDialog;
import util.SwingUtil;
import util.ThreadUtil;
import data.ClusteringData;

public class LaunchCheSMapper
{

	public static void main(String args[])
	{
		Settings.LOGGER.info("Starting CheS-Mapper at " + new Date());
		Settings.LOGGER.info("OS is '" + System.getProperty("os.name") + "'");
		Settings.LOGGER.info("Java runtime version is '" + System.getProperty("java.runtime.version") + "'");

		Locale.setDefault(Locale.US);
		if (args.length > 0)
		{
			if (args[0].equals("screenshot"))
				ScreenSetup.SETUP = ScreenSetup.SCREENSHOT;
			else if (args[0].equals("video"))
				ScreenSetup.SETUP = ScreenSetup.VIDEO;
			else if (args[0].equals("small_screen"))
				ScreenSetup.SETUP = ScreenSetup.SMALL_SCREEN;
			else if (!args[0].equals("default"))
				throw new Error("illegal screen setup arg: " + args[0]);
		}
		boolean loadProperties = true;
		if (args.length > 1)
		{
			if (args[1].equals("no-properties"))
				loadProperties = false;
			else
				throw new Error("illegal properties arg: " + args[1]);
		}
		PropHandler.init(loadProperties);
		BinHandler.init();
		start();
	}

	private static boolean exitOnClose = true;

	public static void setExitOnClose(boolean exit)
	{
		exitOnClose = exit;
	}

	public static void exit(JFrame f)
	{
		if (exitOnClose)
			System.exit(0);
		else
		{
			if (f != null && f.isVisible())
				f.setVisible(false);
			if (Settings.TOP_LEVEL_FRAME != null && Settings.TOP_LEVEL_FRAME != f
					&& Settings.TOP_LEVEL_FRAME.isVisible())
				Settings.TOP_LEVEL_FRAME.setVisible(false);
		}
	}

	public static void start()
	{
		start(null);
	}

	public static void start(CheSMapping mapping)
	{
		if (mapping == null)
		{
			CheSMapperWizard wwd = null;
			while (wwd == null || wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_IMPORT)
			{
				wwd = new CheSMapperWizard(null);
				SwingUtil.waitWhileVisible(wwd);
			}
			if (wwd.getReturnValue() == CheSMapperWizard.RETURN_VALUE_FINISH)
				mapping = wwd.getChesMapping();
		}
		if (mapping == null) //wizard cancelled
		{
			exit(null);
			return;
		}

		Task task = TaskProvider.initTask("Chemical space mapping");
		TaskDialog waitingDialog = new TaskDialog(task, null);
		ClusteringData clusteringData = mapping.doMapping();
		if (clusteringData == null) //mapping failed
		{
			TaskProvider.removeTask();
			start();
			return;
		}

		try
		{ // starting Viewer
			CheSViewer viewer = new CheSViewer(clusteringData);
			while (!viewer.frame.isShowing())
				ThreadUtil.sleep(100);
			waitingDialog.setWarningDialogOwner(viewer.frame);
			task.finish();
		}
		catch (Throwable e)
		{
			Settings.LOGGER.error(e);
			TaskProvider.failed("Could not load viewer", e);
			System.gc();
			start();
		}
		finally
		{
			TaskProvider.removeTask();
		}
	}

}
