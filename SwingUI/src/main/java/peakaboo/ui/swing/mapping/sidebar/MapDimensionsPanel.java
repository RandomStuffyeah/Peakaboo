package peakaboo.ui.swing.mapping.sidebar;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import eventful.EventfulTypeListener;
import peakaboo.controller.mapper.MappingController;
import peakaboo.ui.swing.mapping.MapperPanel;
import peakaboo.ui.swing.mapping.MapperSidebar;
import plural.streams.StreamExecutor;
import plural.streams.swing.StreamExecutorPanel;
import plural.streams.swing.StreamExecutorView;
import scitypes.Coord;
import swidget.icons.IconSize;
import swidget.icons.StockIcon;
import swidget.widgets.ImageButton;
import swidget.widgets.Spacing;
import swidget.widgets.ImageButton.Layout;

public class MapDimensionsPanel extends JPanel {

	private JSpinner width;
	private JSpinner height;
	
	public MapDimensionsPanel(MapperPanel tabPanel, MappingController controller) {
				
		if (MapperSidebar.SHOW_UI_FRAME_BORDERS) this.setBorder(new TitledBorder("Dimensions"));
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = Spacing.iTiny();
		c.ipadx = 0;
		c.ipady = 0;

		c.weightx = 0.0;
		c.weighty = 0.0;
		c.gridy = 0;
		width = new JSpinner();
		width.setValue(controller.settings.getDataWidth());
		width.addChangeListener(e -> {
			controller.settings.setDataWidth((Integer) ((JSpinner) e.getSource()).getValue());
		});

		c.gridx = 0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_START;
		this.add(new JLabel("Width:"), c);
		c.gridx = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.LINE_END;
		this.add(width, c);

		c.gridy += 1;
		height = new JSpinner();
		height.setValue(controller.settings.getDataHeight());
		height.addChangeListener(e -> {
			controller.settings.setDataHeight((Integer) ((JSpinner) e.getSource()).getValue());
		});

		c.gridx = 0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.LINE_START;
		this.add(new JLabel("Height:"), c);
		c.gridx = 1;
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.LINE_END;
		this.add(height, c);
		
		
		if (!controller.mapsController.isDimensionsProvided()) {
			ImageButton magic = new ImageButton("auto", "", "Try to detect the map's dimensions.", Layout.IMAGE, IconSize.TOOLBAR_SMALL);
			c.gridx = 0;
			c.gridwidth = 2;
			c.gridy += 1;
			c.weightx = 0.0;
			c.anchor = GridBagConstraints.LINE_END;
			magic.addActionListener(e -> {
				StreamExecutor<Coord<Integer>> guessTask = controller.mapsController.guessDataDimensions();
				StreamExecutorView view = new StreamExecutorView(guessTask, "Evaluating Sizes");
				StreamExecutorPanel panel = new StreamExecutorPanel("Detecting Dimensions", view);
				guessTask.addListener(() -> {
					SwingUtilities.invokeLater(() -> {
						if (guessTask.getState() == StreamExecutor.State.ABORTED) {
							tabPanel.clearModal();
						}
						if (guessTask.getState() == StreamExecutor.State.COMPLETED) {
						
							tabPanel.clearModal();
							
							Coord<Integer> guess = guessTask.getResult().orElse(null);
							if (guess != null) {
								height.setValue(1);
								width.setValue(1);
								height.setValue(guess.y);
								width.setValue(guess.x);
							}							
						}
					});
				});
				tabPanel.showModal(panel);
				guessTask.start();				

			});
			this.add(magic, c);
		} else {
			ImageButton reset = new ImageButton(StockIcon.ACTION_REFRESH, "", "Reset the dimensions to those given in the data set.", Layout.IMAGE, IconSize.TOOLBAR_SMALL);
			c.gridx = 0;
			c.gridwidth = 2;
			c.gridy += 1;
			c.weightx = 0.0;
			c.anchor = GridBagConstraints.LINE_END;
			reset.addActionListener(e -> {
				height.setValue(controller.mapsController.getOriginalDataHeight());
				width.setValue(controller.mapsController.getOriginalDataWidth());
			});
			this.add(reset, c);
		}
		c.gridwidth = 1;
		
		
		
		controller.addListener(e -> {
			width.setValue(controller.settings.getDataWidth());
			height.setValue(controller.settings.getDataHeight());
		});
		
	}
	
}