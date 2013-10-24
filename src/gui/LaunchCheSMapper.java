package gui;

import gui.CheSViewer.PostStartModifier;
import gui.ViewControler.HideCompounds;
import gui.ViewControler.HighlightMode;
import gui.ViewControler.Style;
import gui.property.ColorGradient;
import gui.util.CompoundPropertyHighlighter;
import gui.util.Highlighter;

import java.awt.Color;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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
import util.IntegerUtil;
import util.StringLineAdder;
import util.StringUtil;
import util.SwingUtil;
import util.ThreadUtil;
import workflow.MappingWorkflow;
import workflow.MappingWorkflow.DescriptorSelection;
import alg.build3d.AbstractReal3DBuilder;
import alg.build3d.OpenBabel3DBuilder;
import cluster.ExportData;
import data.CDKCompoundIcon;
import data.ClusteringData;
import dataInterface.CompoundProperty;
import dataInterface.CompoundPropertyUtil;

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

		ScreenSetup.INSTANCE = screenSetup;

		Settings.LOGGER.info("Starting CheS-Mapper at " + new Date());
		Settings.LOGGER.info("OS is '" + System.getProperty("os.name") + "'");
		Settings.LOGGER.info("Java runtime version is '" + System.getProperty("java.runtime.version") + "'");

		Locale.setDefault(Locale.US);

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

	@SuppressWarnings("static-access")
	private static Option longParamOption(String longOpt, String description, String paramName)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).hasArgs(1).withArgName(paramName)
				.create();
	}

	@SuppressWarnings("static-access")
	private static Option longOption(String longOpt, String description)
	{
		return OptionBuilder.withLongOpt(longOpt).withDescription(description).create();
	}

	public static void main(String args[])
	{
		if (args != null && args.length == 1 && args[0].equals("debug"))
		{
			//Settings.CACHING_ENABLED = false;
			//args = ("-s -d /home/martin/data/caco2.sdf -f integrated -i caco2").split(" ");
			//args = ("-x -d /home/martin/data/caco2.sdf -f integrated -i caco2 -o /home/martin/data/caco-workflow.ches").split(" ");
			//args = ("-w /tmp/delme.ches").split(" ");
			//			args = ("-r -e -d /home/martin/data/caco2.sdf -f cdk -o /tmp/caco-ob-features.csv --rem-missing-above-ratio 0.05")
			//					.split(" ");
			//			args = ("-e -d /home/martin/data/caco2.sdf -n 1 -f obFP3,obFP4,obMACCS -o /tmp/caco-fp-features.csv")
			//					.split(" ");
			//			args = ("-e -d /home/martin/data/caco2.sdf -n 1 -f cdk,obFP3,obFP4,obMACCS -o /tmp/caco-pcfp-features.csv")
			//					.split(" ");

			//			args = ("-r -e -d /home/martin/data/mixed.smi -f cdk -o /tmp/mixed-features.csv --rem-missing-above-ratio 0.05")
			//					.split(" ");

			//		args = ("-e -d /home/martin/workspace/BMBF-MLC/data/dataZ.sdf -f cdk -o /dev/null").split(" ");
			//			args = "-z -d /home/martin/workspace/BMBF-MLC/data/dataR.smi -o /home/martin/workspace/BMBF-MLC/data/dataR.sdf"
			//					.split(" ");
			//			args = "-n -d /home/martin/workspace/BMBF-MLC/data/dataR.smi -o /home/martin/workspace/BMBF-MLC/data/dataR.inchi"
			//					.split(" ");
			//			args = " -z -k -d /home/martin/workspace/BMBF-MLC/predictions/00e884588b8a6ba666fbdf29e9a75eda.smi -o /home/martin/workspace/BMBF-MLC/predictions/00e884588b8a6ba666fbdf29e9a75eda.sdf"
			//					.split(" ");
			//			args = "-e -n 10 -u -d /home/martin/data/caco2.sdf -f fminer -o /home/martin/tmp/delme.csv".split(" ");
			//			args = "-e -m -u -d /home/martin/workspace/BMBF-MLC/predictions/9712985d2d3cd4b067bcd77590ab10f0.sdf -f obFP3 -o /home/martin/tmp/delme.csv"
			//					.split(" ");
			//			args = "-z -k -d /home/martin/workspace/BMBF-MLC/predictions/aa53ca0b650dfd85c4f59fa156f7a2cc.smi -o /home/martin/workspace/BMBF-MLC/predictions/aa53ca0b650dfd85c4f59fa156f7a2cc.sdf"
			//					.split(" ");
			//args = "-z -d /tmp/test.smi -o /tmp/res.sdf".split(" ");

			//			args = "-e -d /home/martin/workspace/BMBF-MLC/data/dataR.sdf -f cdk,ob -o /home/martin/workspace/BMBF-MLC/features/dataR_PC.csv"
			//					.split(" ");
			//			args = ArrayUtil
			//					.toArray(StringUtil
			//							.split("-x -d /home/martin/data/test.csv -f integrated -i cas,cluster -a cluster -c \"Manual Cluster Assignment\" -q \"property-Cluster feature=cluster\" -o /tmp/delme.ches",
			//									' ')); // cannot use .split(" ") to respect quotes
			//args = "-e -d data/dataY.sdf -f cdk,ob -o features/dataY_PC2.sdf".split(" ");
			//args = "-w /home/martin/data/presentation/demo-ob-descriptors.ches".split(" ");
			//args = "-e  -d data/dataR.sdf -f obFP3 -o features/dataR_FP3.csv".split(" ");
			//			args = "-e  -d data/dataR.sdf -f fminer -n 20 -o features/dataR_fminer.csv".split(" ");
			//			args = "-y sxga+ -w /home/martin/data/presentation/demo-ob-descriptors.ches --font-size 20 --compound-style ballsAndSticks --compound-size 35 --highlight-mode Spheres --hide-compounds none"
			//					.split(" ");
			//args = "-y sxga+ -w /home/martin/data/presentation/demo-ob-descriptors.ches".split(" ");
			//			args = "-e -m -u -d predictions/068df623e8c42c1f01d9d04b93aebb4a.sdf -f cdk,ob,obFP3,obFP4,obMACCS -o predictions/068df623e8c42c1f01d9d04b93aebb4a_PCFP1.csv"
			//					.split(" ");

			//			args = "-y sxga+ -w /home/martin/data/presentation/cox2-clustered-aligned.ches --font-size 20 --compound-style ballsAndSticks --compound-size 15 --endpoint-highlight IC50_uM"
			//					.split(" ");
			//		args = "-h".split(" ");
			//args = "-z -d /home/martin/data/cor/test.smi -o /home/martin/data/cor/test.ches3d.sdf".split(" ");

			args = ArrayUtil
					.toArray(StringUtil
							.split("-x -d /home/martin/workspace/BMBF-MLC/pct/clusters_VarianceReduction/dataC_noV_Ca15-20c20_FP1.data.csv -o /home/martin/workspace/BMBF-MLC/pct/clusters_VarianceReduction/dataC_noV_Ca15-20c20_FP1.data.ches -f integrated -b \"OB-MACCS:N,OB-MACCS:OCO,OB-MACCS:O=A>1,OB-MACCS:CH3 > 2  (&...),OB-FP3:alkylaryl ether,OB-FP3:carboxylic acid,OB-FP4:Heteroaromatic,OB-MACCS:ACH2AACH2A,OB-FP4:1,3-Tautomerizable,OB-MACCS:Onot%A%A,OB-FP3:aldehyde or ketone,OB-MACCS:A$A!N,OB-FP4:Rotatable_bond,OB-MACCS:ACH2AAACH2A,OB-FP3:aryl,OB-FP4:Amine,OB-MACCS:C=O,OB-FP4:Hetero_N_basic_no_H,OB-FP3:HBD,OB-MACCS:AA(A)(A)A,OB-MACCS:NN,OB-MACCS:X!A$A,OB-MACCS:QA(Q)Q,OB-MACCS:S,OB-MACCS:NA(A)A,OB-MACCS:ACH2N,OB-MACCS:NAAO,OB-MACCS:O > 3 (&...),OB-MACCS:QAAAAA@1,OB-MACCS:N > 1,OB-FP4:Vinylogous_carbonyl_or_carboxyl_derivative,OB-MACCS:XA(A)A,OB-FP4:Primary_carbon,OB-FP4:Imidoylhalide_cyclic,OB-MACCS:NH2,OB-MACCS:Anot%A%Anot%A,OB-MACCS:NC(N)N,OB-FP4:Heterocyclic,OB-MACCS:QH > 1,OB-FP4:1,5-Tautomerizable,OB-MACCS:O > 2,OB-MACCS:CH3,OB-FP3:aniline,OB-FP3:nitro,OB-FP3:Ring,OB-MACCS:ACH2CH2A > 1,OB-MACCS:QO,OB-FP4:Alkylchloride,OB-MACCS:C=C,OB-FP4:Quaternary_carbon,OB-MACCS:C=C(C)C,OB-FP4:Vinylogous_ester,OB-MACCS:CH3AAACH2A,OB-MACCS:CH3AACH2A,OB-MACCS:S=A,OB-FP3:cation,OB-MACCS:8M Ring or larger. This only handles up to ring sizes of 14,OB-MACCS:BR,OB-MACCS:F,OB-MACCS:A!CH2!A,OB-MACCS:CH3CH2A,OB-MACCS:A$A!O > 1 (&...),OB-MACCS:OC(C)C,OB-FP4:Conjugated_double_bond,OB-FP4:Hetero_O,OB-MACCS:A!A$A!A,OB-FP4:Alkene,OB-MACCS:3M Ring,OB-FP4:Aldehyde,OB-MACCS:QCH2A>1 (&...),OB-MACCS:CH3ACH2A,OB-MACCS:ACH2O,OB-MACCS:CL\" -a leaf,level1,level2,level3,level4,level5,level6,level7,level8,level9,level10,level11,level12,level13,level14,level15,level16,level17,level18 -c \"Manual Cluster Assignment\" -q \"property-Cluster feature=leaf\"",
									' '));
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
		options.addOption(longOption("add-obsolete-pc-features",
				"exports obsolete pc features for backward compatibility"));
		options.addOption(option('m', "match-fingerprints",
				"sets min-freq to 1 and mines omnipresent fingerprint features (eclusive with min frequency)"));
		options.addOption(option('r', "enable-mixture-handling",
				"enableds mixture handling for physico-chemical descriptors"));
		options.addOption(paramOption('n', "fp-min-frequency",
				"sets min-frequency for fingerprints (eclusive with match-fingerprints)", "fp-min-frequency"));
		options.addOption(option('u', "keep-uniform-values",
				"exports features including features with uniform feature values"));
		options.addOption(longParamOption("rem-missing-above-ratio",
				"remove features from export with too much missing values (0 <= missing-ratio <=1)", "missing-ratio"));
		options.addOption(option('x', "export-workflow",
				"creates a workflow-file (from dataset -d and features -f to outfile -o)"));
		options.addOption(paramOption(
				'q',
				"export-properties",
				"for experts only: additional property that are directly written into the worflow file (e.g.: k1=v1,k2=v2)",
				"export-properties"));
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
		options.addOption(paramOption('b', "integrated-features",
				"comma seperated list of feature-names that should be used (from features -f)", "integrated-features"));
		options.addOption(paramOption('i', "ignore-features",
				"comma seperated list of feature-names that should be ignored (from features -f)", "ignored-features"));
		options.addOption(paramOption('a', "nominal-features",
				"comma seperated list of integrated feature-names that should be interpreted as nominal",
				"nominal-features"));
		//		List<String> clusterNames = new ArrayList<String>();
		//		for (Algorithm a : DatasetClusterer.CLUSTERERS)
		//			clusterNames.add(a.getName());
		//		options.addOption(paramOption('c', "cluster-algorithm",
		//				"specify cluster algorithm: " + ListUtil.toString(clusterNames, ", "), "cluster-algorithm"));
		options.addOption(paramOption('c', "cluster-algorithm", "specify cluster algorithm", "cluster-algorithm"));

		options.addOption(paramOption('t', "fix-3d-sdf-file",
				"replaces corrupt structures in input-file -t with structures from input-file -d, saves to outfile -o",
				"corrupt-3d-sdf-file"));
		options.addOption(paramOption(
				'v',
				"fix-3d-sdf-file-external",
				"replaces corrupt structures in input-file -v with structures derieved with external-script from smi-file -d, saves to outfile -o",
				"corrupt-3d-sdf-file"));

		options.addOption(option(
				'z',
				"compute-3d",
				"uses openbabel to compute a SDF file (-o) for the input-file -d (no auto-correction of openbabel errors like in gui, use -t or -v)"));
		options.addOption(option('k', "depict-2d", "depicts 2d images for each compound in dataset file -d"));
		//		options.addOption(option('n', "compute-inchi", "computes inchi for dataset file -d, saves to outfile -o"));

		options.addOption(longParamOption("font-size", "change initial font size", "font-size"));
		options.addOption(longParamOption("compound-style", "change initial style", "compound-style"));
		options.addOption(longParamOption("compound-size", "change initial compound size", "compound-size"));
		options.addOption(longParamOption("highlight-mode", "change initial highlight mode", "highlight-mode"));
		options.addOption(longParamOption("hide-compounds", "change initial hide-compounds mode", "hide compounds"));
		options.addOption(longParamOption("endpoint-highlight",
				"enable endpoint-highlighting (log + reverse) for a feature", "endpoint-highlight feature"));

		options.addOption(longParamOption("cluster-tree", "show cluster tree", "cluster-tree csv file"));

		CommandLineParser parser = new BasicParser();
		try
		{
			final CommandLine cmd = parser.parse(options, args);

			PostStartModifier mod = new PostStartModifier()
			{
				@Override
				public void modify(GUIControler gui, final ViewControler view)
				{
					if (cmd.hasOption("font-size"))
					{
						final Integer font = IntegerUtil.parseInteger(cmd.getOptionValue("font-size"));
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								view.setFontSize(font);
							}
						});
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("compound-style"))
					{
						Style style = Style.valueOf(cmd.getOptionValue("compound-style"));
						view.setStyle(style);
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("compound-size"))
					{
						Integer size = IntegerUtil.parseInteger(cmd.getOptionValue("compound-size"));
						view.setCompoundSize(size);
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("highlight-mode"))
					{
						HighlightMode mode = HighlightMode.valueOf(cmd.getOptionValue("highlight-mode"));
						view.setHighlightMode(mode);
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("hide-compounds"))
					{
						HideCompounds mode = HideCompounds.valueOf(cmd.getOptionValue("hide-compounds"));
						view.setHideCompounds(mode);
						ThreadUtil.sleep(2000);
					}
					if (cmd.hasOption("endpoint-highlight"))
					{
						CompoundProperty p = null;
						for (Highlighter h[] : view.getHighlighters().values())
							for (Highlighter hi : h)
								if (hi instanceof CompoundPropertyHighlighter)
									if (((CompoundPropertyHighlighter) hi).getProperty().toString()
											.equals(cmd.getOptionValue("endpoint-highlight")))
										p = ((CompoundPropertyHighlighter) hi).getProperty();
						if (p == null)
							throw new Error("feature not found: " + cmd.getOptionValue("endpoint-highlight"));
						view.setHighlightColors(new ColorGradient(new Color(100, 255, 100), Color.WHITE,
								CompoundPropertyUtil.getHighValueColor()), true, new CompoundProperty[] { p });
						ThreadUtil.sleep(2000);
					}
				}
			};

			if (cmd.hasOption('h'))
			{
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java (-Xmx1g) -jar ches-mapper(-complete).jar", options);
				System.out.println("\n" + examples);
				System.exit(0);
			}

			ScreenSetup screenSetup;
			if (cmd.hasOption('y'))
			{
				if (cmd.getOptionValue('y').equals("screenshot"))
					screenSetup = ScreenSetup.SCREENSHOT;
				else if (cmd.getOptionValue('y').equals("video"))
					screenSetup = ScreenSetup.VIDEO;
				else if (cmd.getOptionValue('y').equals("small_screen"))
					screenSetup = ScreenSetup.SMALL_SCREEN;
				else if (cmd.getOptionValue('y').equals("sxga+"))
					screenSetup = ScreenSetup.SXGA_PLUS;
				else if (cmd.getOptionValue('y').equals("default"))
					screenSetup = ScreenSetup.DEFAULT;
				else
					throw new Error("illegal screen setup-arg: " + cmd.getOptionValue('y'));
			}
			else
				screenSetup = ScreenSetup.DEFAULT;

			boolean loadProperties = true;
			if (cmd.hasOption('p'))
				loadProperties = false;

			init(Locale.US, screenSetup, loadProperties);

			if (cmd.hasOption('r'))
				Settings.DESC_MIXTURE_HANDLING = true;

			if (cmd.hasOption('e'))
			{
				if (cmd.hasOption("add-obsolete-pc-features"))
					Settings.CDK_SKIP_SOME_DESCRIPTORS = false;

				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || outfile == null || featureNames == null)
					throw new ParseException(
							"please give dataset-file (-d) and features (-f) and outfile (-o) for feature export");
				DescriptorSelection features = new DescriptorSelection(cmd.getOptionValue('f'),
						cmd.getOptionValue('b'), cmd.getOptionValue('i'), cmd.getOptionValue('a'));
				if (cmd.hasOption('m'))
				{
					if (cmd.hasOption('n'))
						throw new IllegalArgumentException("exclusive settings n + m");
					features.setFingerprintSettings(1, false);
				}
				else if (cmd.hasOption('n'))
					features.setFingerprintSettings(Integer.parseInt(cmd.getOptionValue('n')), true);
				double missingRatio = 0;
				if (cmd.hasOption("rem-missing-above-ratio"))
					missingRatio = Double.parseDouble(cmd.getOptionValue("rem-missing-above-ratio"));
				ExportData.scriptExport(infile, features, outfile, cmd.hasOption('u'), missingRatio);
			}
			else if (cmd.hasOption('x')) // export workflow
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || outfile == null || featureNames == null)
					throw new ParseException(
							"please give dataset-file (-d) and features (-f) and outfile (-o) for workflow export");
				DescriptorSelection features = new DescriptorSelection(cmd.getOptionValue('f'),
						cmd.getOptionValue('b'), cmd.getOptionValue('i'), cmd.getOptionValue('a'));
				MappingWorkflow.createAndStoreMappingWorkflow(infile, outfile, features,
						MappingWorkflow.clustererFromName(cmd.getOptionValue('c')), cmd.getOptionValue('q'));
			}
			else if (cmd.hasOption('w'))
			{
				CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(cmd.getOptionValue('w'));
				start(mapping, mod);
			}
			else if (cmd.hasOption('s')) // direct start
			{
				String infile = cmd.getOptionValue('d');
				String featureNames = cmd.getOptionValue('f');
				if (infile == null || featureNames == null)
					throw new ParseException("please give dataset-file (-d) and features (-f) to start viewer");
				DescriptorSelection features = new DescriptorSelection(cmd.getOptionValue('f'),
						cmd.getOptionValue('b'), cmd.getOptionValue('i'), cmd.getOptionValue('a'));
				Properties workflow = MappingWorkflow.createMappingWorkflow(infile, features);
				CheSMapping mapping = MappingWorkflow.createMappingFromMappingWorkflow(workflow);
				start(mapping, mod);
			}
			else if (cmd.hasOption('t'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				if (infile == null || outfile == null)
					throw new ParseException("please give correct-2d-sdf-file (-d) and outfile (-o) for sdf-3d-fix");
				AbstractReal3DBuilder.check3DSDFile(cmd.getOptionValue('t'), infile, outfile);
			}
			else if (cmd.hasOption('v'))
			{
				String infile = cmd.getOptionValue('d');
				String outfile = cmd.getOptionValue('o');
				if (infile == null || outfile == null || !infile.endsWith("smi"))
					throw new ParseException(
							"please give correct-smi-file (-d) and outfile (-o) for sdf-3d-fix with external script");
				AbstractReal3DBuilder.check3DSDFileExternal(cmd.getOptionValue('v'), infile, outfile);
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
				builder.disableAutocorrect();
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
			else if (cmd.hasOption("cluster-tree"))
			{
				CSVtoTree.show(cmd.getOptionValue("cluster-tree"));
				System.exit(0);
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
				start(null, mod);
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
		start(mapping, null);
	}

	public static void start(CheSMapping mapping, PostStartModifier mod)
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
			CheSViewer.show(clusteringData, mod);
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
