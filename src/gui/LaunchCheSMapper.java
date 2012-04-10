package gui;

import java.util.Locale;

import main.BinHandler;
import main.PropHandler;
import main.ScreenSetup;
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
		Locale.setDefault(Locale.US);
		if (args.length > 0)
		{
			if (args[0].equals("screenshot"))
				ScreenSetup.SETUP = ScreenSetup.SCREENSHOT;
			else if (args[0].equals("video"))
				ScreenSetup.SETUP = ScreenSetup.VIDEO;
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
		startWizard();
	}

	public static void startWizard()
	{
		ClusteringData clusteringData = null;
		Task task = null;
		TaskDialog waitingDialog = null;

		while (clusteringData == null)
		{
			CheSMapperWizard wwd = new CheSMapperWizard(null);
			SwingUtil.waitWhileVisible(wwd);

			if (wwd.isWorkflowSelected())
			{
				task = TaskProvider.initTask("Chemical space mapping");
				waitingDialog = new TaskDialog(task, null);
				clusteringData = wwd.doMapping();
			}
			else
				break;
		}
		if (clusteringData == null)
			System.exit(1);

		// starting Viewer
		try
		{
			CheSViewer viewer = new CheSViewer(clusteringData);
			while (!viewer.frame.isShowing())
				ThreadUtil.sleep(100);
			waitingDialog.setWarningDialogOwner(viewer.frame);
			task.finish();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			TaskProvider.failed("Could not load viewer", e);
			System.gc();
			startWizard();
		}
		finally
		{
			TaskProvider.removeTask();
		}
	}
}
