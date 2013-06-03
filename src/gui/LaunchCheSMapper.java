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
	private static boolean initialized = false;

	public static void init()
	{
		init(Locale.US, ScreenSetup.DEFAULT, true);
	}

	public static void init(Locale locale, ScreenSetup screenSetup, boolean loadProps)
	{
		if (initialized)
			throw new IllegalStateException("init only once!");

		Settings.LOGGER.info("Starting CheS-Mapper at " + new Date());
		Settings.LOGGER.info("OS is '" + System.getProperty("os.name") + "'");
		Settings.LOGGER.info("Java runtime version is '" + System.getProperty("java.runtime.version") + "'");

		Locale.setDefault(Locale.US);
		ScreenSetup.SETUP = screenSetup;

		PropHandler.init(loadProps);
		BinHandler.init();

		initialized = true;
	}

	public static void main(String args[])
	{
		ScreenSetup screenSetup = ScreenSetup.DEFAULT;
		if (args != null && args.length > 0)
		{
			if (args[0].equals("screenshot"))
				screenSetup = ScreenSetup.SCREENSHOT;
			else if (args[0].equals("video"))
				screenSetup = ScreenSetup.VIDEO;
			else if (args[0].equals("small_screen"))
				screenSetup = ScreenSetup.SMALL_SCREEN;
			else if (!args[0].equals("default"))
				throw new Error("illegal screen setup arg: " + args[0]);
		}
		boolean loadProperties = true;
		if (args != null && args.length > 1)
		{
			if (args[1].equals("no-properties"))
				loadProperties = false;
			else
				throw new Error("illegal properties arg: " + args[1]);
		}
		init(Locale.US, screenSetup, loadProperties);
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
		if (!initialized)
			throw new IllegalStateException("not initialized!");

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
			CheSViewer.show(clusteringData);
			while (!CheSViewer.getFrame().isShowing())
				ThreadUtil.sleep(100);
			waitingDialog.setWarningDialogOwner(CheSViewer.getFrame());
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
