package gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JFrame;

import main.BinHandler;
import main.CheSMapping;
import main.PropHandler;
import main.ScreenSetup;
import main.Settings;
import main.TaskProvider;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import task.Task;
import task.TaskDialog;
import util.ArrayUtil;
import util.FileUtil;
import util.ListUtil;
import util.StringLineAdder;
import util.SwingUtil;
import util.ThreadUtil;
import workflow.MappingWorkflow;
import workflow.MappingWorkflow.DescriptorSelection;
import alg.Algorithm;
import alg.build3d.AbstractReal3DBuilder;
import alg.build3d.OpenBabel3DBuilder;
import alg.cluster.DatasetClusterer;
import cluster.ExportData;
import data.CDKCompoundIcon;
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

	@SuppressWarnings("static-access")
	private static Option option(char charOpt, String longOpt, String description)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).create(charOpt);
	}

	@SuppressWarnings("static-access")
	private static Option paramOption(char charOpt, String longOpt, String description, String paramName)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).hasArgs(1).withArgName(paramName)
				.create(charOpt);
	}

	public static void main(String args[])
	{
		if (args != null && args.length == 1 && args[0].equals("debug"))
		{
			//Settings.CACHING_ENABLED = false;
			//args = ("-s -d /home/martin/data/caco2.sdf -f integrated -i caco2").split(" ");
			//args = ("-x -d /home/martin/data/caco2.sdf -f integrated -i caco2 -o /home/martin/data/caco-workflow.ches").split(" ");
			//args = ("-w /tmp/delme.ches").split(" ");
			//args = ("-e -d /home/martin/data/caco2.sdf -f cdk -o /tmp/caco-ob-features.csv").split(" ");
			//		args = ("-e -d /home/martin/workspace/BMBF-MLC/data/dataZ.sdf -f cdk -o /dev/null").split(" ");
			//			args = "-z -d /home/martin/workspace/BMBF-MLC/data/dataR.smi -o /home/martin/workspace/BMBF-MLC/data/dataR.sdf"
			//					.split(" ");
			//			args = "-n -d /home/martin/workspace/BMBF-MLC/data/dataR.smi -o /home/martin/workspace/BMBF-MLC/data/dataR.inchi"
			//					.split(" ");
			//			args = " -z -k -d /home/martin/workspace/BMBF-MLC/predictions/00e884588b8a6ba666fbdf29e9a75eda.smi -o /home/martin/workspace/BMBF-MLC/predictions/00e884588b8a6ba666fbdf29e9a75eda.sdf"
			//					.split(" ");
			args = "-e -m -u -d /home/martin/workspace/BMBF-MLC/predictions/9712985d2d3cd4b067bcd77590ab10f0.sdf -f obFP3 -o /home/martin/tmp/delme.csv"
					.split(" ");
			//			args = "-z -k -d /home/martin/workspace/BMBF-MLC/predictions/aa53ca0b650dfd85c4f59fa156f7a2cc.smi -o /home/martin/workspace/BMBF-MLC/predictions/aa53ca0b650dfd85c4f59fa156f7a2cc.sdf"
			//					.split(" ");
			//args = "-z -d /tmp/test.smi -o /tmp/res.sdf".split(" ");

			//			args = "-e -d /home/martin/workspace/BMBF-MLC/data/dataR.sdf -f cdk,ob -o /home/martin/workspace/BMBF-MLC/features/dataR_PC.csv"
			//					.split(" ");

			//		args = ArrayUtil
			//				.toArray(StringUtil
			//						.split("-x -d /home/martin/workspace/BMBF-MLC/pct/dataAAgg_noV_Cl68_FP.data.csv -f integrated -i cas,cluster -c \"Manual Cluster Assignment\" -o /tmp/delme.ches",
			//								' ')); // cannot use .split(" ") to respect quotes

			//		args = "-h".split(" ");
		}

		StringLineAdder examples = new StringLineAdder();
		examples.add("Examples");
		examples.add("* directly start viewer caco2.sdf dataset with all integrated features apart from the endpoint feature caco2");
		examples.add("  -s -d data/caco2.sdf -f integrated -i caco2");
		examples.add("* export workflow-file for caco2.sdf dataset with all integrated features apart from the endpoint feature caco2");
		examples.add("  -x -d data/caco2.sdf -f integrated -i caco2 -o data/caco-workflow.ches");
		examples.add("* directly start ches-mapper with workflow-file");
		examples.add("  -w data/caco-workflow.ches");
		examples.add("* export open-babel descriptors for caco2.sdf dataset");
		examples.add("  -e -d data/caco2.sdf -f ob -o data/caco2-ob-features.csv");

		Options options = new Options();
		options.addOption(paramOption('y', "screen-setup",
				"for expert users, should be one of debug|screenshot|video|small_screen", "setup-mode"));
		options.addOption(option('p', "no-properties",
				"for expert users, prevent ches-mapper from reading property file with saved settings"));
		options.addOption(option('h', "help", "show this help output"));

		options.addOption(option('e', "export-features",
				"exports features (from dataset -d and features -f to outfile -o)"));
		options.addOption(option('m', "match-fingerprints", "usefull when exporting fingerprint features"));
		options.addOption(option('u', "uniform-values",
				"exports features including features with uniform feature values"));
		options.addOption(option('x', "export-workflow",
				"creates a workflow-file (from dataset -d and features -f to outfile -o)"));
		options.addOption(option('s', "start-viewer", "directly starts the viewer (from dataset -d and features -f)"));
		options.addOption(paramOption('w', "start-workflow", "directly starts the viewer", "workflow-file"));

		options.addOption(paramOption('d', "dataset-file",
				"input file for export-features, export-workflow, start-viewer", "dataset-file"));
		options.addOption(paramOption('o', "outfile", "output file for export-features, export-workflow", "outfile"));
		options.addOption(paramOption(
				'f',
				"features",
				"specify features (comma seperated) : "
						+ ArrayUtil.toString(MappingWorkflow.DescriptorCategory.values(), ",", "", "", ""), "features"));
		options.addOption(paramOption('i', "ignore-features",
				"comma seperated list of feature-names that should be ignored (from features -f)", "ignored-features"));
		List<String> clusterNames = new ArrayList<String>();
		for (Algorithm a : DatasetClusterer.CLUSTERERS)
			clusterNames.add(a.getName());
		options.addOption(paramOption('c', "cluster-algorithm",
				"specify cluster algorithm: " + ListUtil.toString(clusterNames, ", "), "cluster-algorithm"));

		options.addOption(paramOption('t', "fix-3d-sdf-file",
				"replaces corrupt structures with structures from input-file -d, saves to outfile -o",
				"corrupt-3d-sdf-file"));

		options.addOption(option('z', "compute-3d", "uses openbabel to compute a SDF file (-o) for the input-file -d"));
		options.addOption(option('k', "depict-2d", "depicts 2d images for each compound in dataset file -d"));
		//		options.addOption(option('n', "compute-inchi", "computes inchi for dataset file -d, saves to outfile -o"));

		CommandLineParser parser = new BasicParser();
		try
		{
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption('h'))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java (-Xmx1g) -jar ches-mapper(-complete).jar", options);
				System.out.println("\n" + examples);
				System.exit(0);
			}

			ScreenSetup screenSetup = ScreenSetup.DEFAULT;
			if (cmd.hasOption('y'))
			{
				if (cmd.getOptionValue('y').equals("screenshot"))
					screenSetup = ScreenSetup.SCREENSHOT;
				else if (cmd.getOptionValue('y').equals("video"))
					screenSetup = ScreenSetup.VIDEO;
				else if (cmd.getOptionValue('y').equals("small_screen"))
					screenSetup = ScreenSetup.SMALL_SCREEN;
				else if (!cmd.getOptionValue('y').equals("default"))
					throw new Error("illegal screen setup-arg: " + cmd.getOptionValue('y'));
			}

			boolean loadProperties = true;
			if (cmd.hasOption('p'))
				loadProperties = false;

			init(Locale.US, screenSetup, loadProperties);

			if (cmd.hasOption('e'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || outfile == null || featureNames == null)
					throw new ParseException(
							"please give dataset-file (-d) and features (-f) and outfile (-o) for feature export");
				DescriptorSelection features = new DescriptorSelection(cmd.getOptionValue('f'), cmd.getOptionValue('i'));
				features.setMatchFingerprints(cmd.hasOption('m'));
				ExportData.scriptExport(infile, features, outfile, cmd.hasOption('u'));
			}
			else if (cmd.hasOption('x')) // export workflow
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || outfile == null || featureNames == null)
					throw new ParseException(
							"please give dataset-file (-d) and features (-f) and outfile (-o) for workflow export");
				DescriptorSelection features = new DescriptorSelection(cmd.getOptionValue('f'), cmd.getOptionValue('i'));
				MappingWorkflow.createAndStoreMappingWorkflow(infile, outfile, features,
						MappingWorkflow.clustererFromName(cmd.getOptionValue('c')));
			}
			else if (cmd.hasOption('w'))
			{
				CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(cmd.getOptionValue('w'));
				start(mapping);
			}
			else if (cmd.hasOption('s')) // direct start
			{
				String infile = cmd.getOptionValue('d');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || featureNames == null)
					throw new ParseException("please give dataset-file (-d) and features (-f) to start viewer");
				DescriptorSelection features = new DescriptorSelection(cmd.getOptionValue('f'), cmd.getOptionValue('i'));
				Properties workflow = MappingWorkflow.createMappingWorkflow(infile, features);
				CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(workflow);
				start(mapping);
			}
			else if (cmd.hasOption('t'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				if (infile == null || outfile == null)
					throw new ParseException("please give correct-2d-sdf-file (-d) and outfile (-o) for sdf-3d-fix");
				AbstractReal3DBuilder.check3DSDFile(cmd.getOptionValue('t'), infile, outfile);
			}
			else if (cmd.hasOption('z'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				if (infile == null || outfile == null)
					throw new ParseException("please give dataset-file (-d) and outfile (-o) for compute-3d");

				DatasetWizardPanel p = new DatasetWizardPanel(false);
				p.load(infile, true);
				if (p.getDatasetFile() == null)
					throw new Error("Could not load dataset file " + infile);
				if (cmd.hasOption('k'))
					CDKCompoundIcon.createIcons(p.getDatasetFile(), FileUtil.getParent(infile));

				OpenBabel3DBuilder builder = OpenBabel3DBuilder.INSTANCE;
				builder.build3D(p.getDatasetFile());
				if (!FileUtil.copy(builder.get3DSDFFile(), outfile))
					throw new Error("Could not copy 3D-File to outfile " + outfile);
			}
			else if (cmd.hasOption('k'))
			{
				String infile = cmd.getOptionValue('d');
				if (infile == null)
					throw new ParseException("please give dataset-file (-d) for depict-2d");
				DatasetWizardPanel p = new DatasetWizardPanel(false);
				p.load(infile, true);
				if (p.getDatasetFile() == null)
					throw new Error("Could not load dataset file " + infile);
				CDKCompoundIcon.createIcons(p.getDatasetFile(), FileUtil.getParent(infile));
			}
			//			else if (cmd.hasOption('n'))
			//			{
			//				String infile = cmd.getOptionValue('d');
			//				String outfile = cmd.getOptionValue('o');
			//				if (infile == null || outfile == null)
			//					throw new ParseException("please give dataset-file (-d) and outfile (-o) for compute-3d");
			//				DatasetWizardPanel p = new DatasetWizardPanel(false);
			//				p.load(infile, true);
			//				if (p.getDatasetFile() == null)
			//					throw new Error("Could not load dataset file " + infile);
			//				String inichi[] = OBWrapper.computeInchiFromSmiles(
			//						BinHandler.BABEL_BINARY.getSisterCommandLocation("obabel"), p.getDatasetFile().getSmiles());
			//				FileUtil.writeStringToFile(outfile, ArrayUtil.toString(inichi, "\n", "", "", "") + "\n");
			//			}
			else
				start();
		}
		catch (ParseException e)
		{
			System.out.println();
			System.out.flush();
			e.printStackTrace();
			System.err.flush();
			System.out.println("\nCould not parse command line options\n" + e.getMessage() + "\n");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java (-Xmx1g) -jar ches-mapper(-complete).jar", options);
			System.out.println("\n" + examples);
			System.exit(1);
		}
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
