package peakaboo.ui.swing.mapping;

import static java.util.stream.Collectors.toList;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import peakaboo.controller.mapper.MappingController;
import peakaboo.controller.mapper.mapdisplay.AreaSelection;
import peakaboo.controller.mapper.mapdisplay.PointsSelection;
import peakaboo.controller.settings.SavedSettings;
import peakaboo.curvefit.model.transitionseries.TransitionSeries;
import peakaboo.datasource.model.DataSource;
import peakaboo.datasource.model.internal.CroppedDataSource;
import peakaboo.datasource.model.internal.SelectionDataSource;
import peakaboo.datasource.model.internal.SubsetDataSource;
import peakaboo.mapping.correction.Corrections;
import peakaboo.mapping.correction.CorrectionsManager;
import peakaboo.mapping.results.MapResult;
import scitypes.GridPerspective;
import scitypes.Pair;
import scitypes.Range;
import scitypes.SigDigits;
import swidget.icons.StockIcon;
import swidget.widgets.ButtonBox;
import swidget.widgets.ImageButton;
import swidget.widgets.Spacing;
import swidget.widgets.ToolbarImageButton;
import swidget.widgets.properties.PropertyViewPanel;

public class MapperToolbar extends JToolBar {

	ToolbarImageButton	readIntensities, examineSubset;
	
	JCheckBoxMenuItem	monochrome;
	JMenuItem			title, spectrum, coords, dstitle;
	
	public MapperToolbar(MapperPanel panel, MappingController controller) {


		this.setFloatable(false);
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.insets = new Insets(4, 4, 4, 4);
		
		ToolbarImageButton savePicture = new ToolbarImageButton(StockIcon.DEVICE_CAMERA, "Save Image", "Save the current map as an image");
		savePicture.addActionListener(e -> panel.actionSavePicture());
		this.add(savePicture, c);
		c.gridx++;
		
		
		ToolbarImageButton saveText = new ToolbarImageButton(StockIcon.DOCUMENT_EXPORT, "Export as Text", "Export the current map as a comma separated value file");
		saveText.addActionListener(e -> panel.actionSaveCSV());
		this.add(saveText, c);
		c.gridx++;
		
		
		this.addSeparator();
		
		
		readIntensities = new ToolbarImageButton(StockIcon.BADGE_INFO, "Get Intensities", "Get fitting intensities for the selection", true);
		readIntensities.addActionListener(e -> {
			Map<String, String> fittings = new HashMap<String, String>();
			
			final Corrections corr = CorrectionsManager.getCorrections("WL");
			
			AreaSelection selection = controller.getDisplay().getAreaSelection(); 
			
			final int xstart = selection.getStart().x;
			final int ystart = selection.getStart().y;
			
			final int xend = selection.getEnd().x;
			final int yend = selection.getEnd().y;
			
			final int size = (Math.abs(xstart - xend) + 1) * (Math.abs(ystart - yend) + 1);
			
			final GridPerspective<Float> grid = new GridPerspective<Float>(controller.settings.getDataWidth(), controller.settings.getDataHeight(), 0f);
			
			
			//generate a list of pairings of TransitionSeries and their intensity values
			List<Pair<TransitionSeries, Float>> averages = controller.mapsController.getMapResultSet().stream().map((MapResult r) -> {
				float sum = 0;
				for (int x : new Range(xstart, xend)) {
					for (int y : new Range(ystart, yend)){
						sum += r.data.get(grid.getIndexFromXY(x, y));
					}
				}
				return new Pair<TransitionSeries, Float>(r.transitionSeries, sum / size);
			}).collect(toList());
			
			
			//get the total of all of the corrected values
			float total = averages.stream().map((Pair<TransitionSeries, Float> p) -> {
				Float corrFactor = corr.getCorrection(p.first);
				return (corrFactor == null) ? 0f : p.second * corrFactor;
			}).reduce(0f, (a, b) -> a + b);
			
			for (Pair<TransitionSeries, Float> p : averages)
			{
				float average = p.second;
				Float corrFactor = corr.getCorrection(p.first);
				String corrected = "(-)";
				if (corrFactor != null) corrected = "(~" + SigDigits.toIntSigDigit((average*corrFactor/total*100), 1) + "%)";
				
				fittings.put(p.first.getDescription(), SigDigits.roundFloatTo(average, 2) + " " + corrected);
			}
			
			PropertyViewPanel correctionsPanel = new PropertyViewPanel(fittings);
			
			
			JPanel corrections = new JPanel(new BorderLayout());
			JPanel contentPanel = new JPanel(new BorderLayout());
			corrections.add(contentPanel, BorderLayout.CENTER);
			
			contentPanel.add(new JLabel("Concentrations accurate to a factor of 5", JLabel.CENTER), BorderLayout.SOUTH);
			contentPanel.add(correctionsPanel, BorderLayout.CENTER);
			contentPanel.setBorder(Spacing.bHuge());
			
			ButtonBox bbox = new ButtonBox(Spacing.bHuge());
			ImageButton close = new ImageButton(StockIcon.WINDOW_CLOSE, "Close", "Close this window");
			close.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent e)
				{
					panel.clearModal();
				}
			});
			bbox.addRight(close);
			corrections.add(bbox, BorderLayout.SOUTH);
			
			
			panel.showModal(corrections);
				
				
		});
		this.add(readIntensities, c);
		c.gridx++;
		
		
		examineSubset =  new ToolbarImageButton("view-subset", "Plot Selection", "Plot the selection as a new data set", true);
		examineSubset.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e)
			{
				AreaSelection areaSelection = controller.getDisplay().getAreaSelection();
				PointsSelection pointSelection = controller.getDisplay().getPointsSelection();
				
				SubsetDataSource sds;
				if (areaSelection.hasSelection()) {
					sds = controller.getDataSourceForSubset(areaSelection.getStart(), areaSelection.getEnd());
				} else {
					sds = controller.getDataSourceForSubset(pointSelection.getPoints());
				}
				
				SavedSettings settings = controller.getSavedSettingsObject();
				
				//update the bad scan indexes to match the new data source's indexing scheme
				settings.badScans = settings.badScans.stream()
						.map(index -> sds.getUpdatedIndex(index))
						.filter(index -> index > 0)
						.collect(Collectors.toList()
					);
				
				panel.parentPlotter.newTab(sds, settings.serialize());
				panel.parentPlotter.getWindow().toFront();
				
			}
		});
		this.add(examineSubset, c);
		c.gridx++;
		
		
		readIntensities.setEnabled(false);
		examineSubset.setEnabled(false);
		
		c.weightx = 1.0;
		this.add(Box.createHorizontalGlue(), c);
		c.weightx = 0.0;
		c.gridx++;
		
		this.add(createOptionsButton(controller), c);
		c.gridx++;
		
	}
	

	private ToolbarImageButton createOptionsButton(MappingController controller) {
		
		ToolbarImageButton opts = new ToolbarImageButton(StockIcon.ACTION_MENU, "Map Preferences");
		
		JPopupMenu menu = new JPopupMenu();
		
		title = new JCheckBoxMenuItem("Show Elements List");
		dstitle = new JCheckBoxMenuItem("Show Dataset Title");
		spectrum = new JCheckBoxMenuItem("Show Spectrum");
		coords = new JCheckBoxMenuItem("Show Coordinates");
		monochrome = new JCheckBoxMenuItem("Monochrome");

		title.setSelected(controller.settings.getShowTitle());
		spectrum.setSelected(controller.settings.getShowSpectrum());
		coords.setSelected(controller.settings.getShowCoords());
		dstitle.setSelected(controller.settings.getShowDatasetTitle());

		spectrum.addActionListener(e -> controller.settings.setShowSpectrum(spectrum.isSelected()));
		coords.addActionListener(e -> controller.settings.setShowCoords(coords.isSelected()));
		title.addActionListener(e -> controller.settings.setShowTitle(title.isSelected()));
		dstitle.addActionListener(e -> controller.settings.setShowDatasetTitle(dstitle.isSelected()));
		monochrome.addActionListener(e -> controller.settings.setMonochrome(monochrome.isSelected()));
		
		menu.add(title);
		menu.add(dstitle);
		menu.add(spectrum);
		menu.add(coords);
		menu.addSeparator();
		menu.add(monochrome);
		
		opts.addActionListener(e -> menu.show(opts, (int)(opts.getWidth() - menu.getPreferredSize().getWidth()), opts.getHeight()));
		
		return opts;
	}
	
}
