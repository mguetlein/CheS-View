package gui.util;

import gui.TextPanel;
import gui.ViewControler;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import main.Settings;
import util.ArrayUtil;
import cluster.Clustering;
import cluster.SALIProperty;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.FormLayout;

import dataInterface.CompoundProperty;

public class SALIDialog extends JDialog
{
	private SALIDialog(final ViewControler viewControler, final Clustering clustering, List<CompoundProperty> list)
	{
		super(Settings.TOP_LEVEL_FRAME, Settings.text("action.edit-show-sali"), true);

		DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:p:grow,10px,fill:p:grow"));

		TextPanel tp1 = new TextPanel(Settings.text("props.sali.detail", SALIProperty.MIN_ENDPOINT_DEV_STR));
		builder.append(tp1, 3);

		final JComboBox<CompoundProperty> propCombo = new JComboBox<CompoundProperty>(ArrayUtil.toArray(list));
		if (viewControler.getHighlightedProperty() != null)
		{
			CompoundProperty sel = viewControler.getHighlightedProperty();
			if (list.contains(sel))
				propCombo.setSelectedItem(sel);
		}
		JLabel label = new JLabel("Endpoint:");
		builder.append(label);
		builder.append(propCombo);

		final JComboBox<Boolean> maxCombo = new JComboBox<Boolean>(new Boolean[] { false, true });
		maxCombo.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus)
			{
				return super.getListCellRendererComponent(list, ((Boolean) value) ? "Max" : "Mean", index, isSelected,
						cellHasFocus);
			}
		});
		JLabel label2 = new JLabel("Convert pairwise values with:");
		builder.append(label2);
		builder.append(maxCombo);

		JButton ok = new JButton("OK");
		JButton close = new JButton("Cancel");
		ok.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SALIDialog.this.setVisible(false);
				CompoundProperty p = clustering.addSALIFeature((CompoundProperty) propCombo.getSelectedItem(),
						(Boolean) maxCombo.getSelectedItem());
				if (p != null)
					viewControler.setHighlighter(p);
			}
		});
		close.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				SALIDialog.this.setVisible(false);
			}
		});
		builder.append(" ");//add gap
		builder.append(ButtonBarFactory.buildOKCancelBar(ok, close));
		builder.setBorder(new EmptyBorder(10, 10, 10, 10));

		setLayout(new BorderLayout());
		add(builder.getPanel());

		pack();
		tp1.setPreferredWith(Math.max(300, label2.getPreferredSize().width + 10 + propCombo.getPreferredSize().width));
		pack();
		setLocationRelativeTo(getOwner());
	}

	public static void showDialog(ViewControler viewControler, Clustering clustering)
	{
		List<CompoundProperty> list = new ArrayList<CompoundProperty>();
		for (CompoundProperty p : clustering.getProperties())
		{
			if (p.getType() == dataInterface.CompoundProperty.Type.NUMERIC
					|| p.getNominalDomainInMappedDataset().length == 2)
				list.add(p);
		}
		if (list.size() == 0)
			JOptionPane
					.showMessageDialog(
							Settings.TOP_LEVEL_FRAME,
							"Currently, only numeric or binary endpoint properties are supported.\nNo such property is available in the dataset.",
							"Message", JOptionPane.OK_OPTION);
		else
		{
			SALIDialog d = new SALIDialog(viewControler, clustering, list);
			d.setVisible(true);
		}
	}
}
