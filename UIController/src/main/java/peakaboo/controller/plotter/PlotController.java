package peakaboo.controller.plotter;

import java.util.List;

import eventful.EventfulType;
import peakaboo.controller.plotter.data.DataController;
import peakaboo.controller.plotter.filtering.FilteringController;
import peakaboo.controller.plotter.fitting.FittingController;
import peakaboo.controller.plotter.undo.UndoController;
import peakaboo.controller.plotter.view.ViewController;
import peakaboo.controller.settings.SavedSession;
import peakaboo.display.plot.ChannelCompositeMode;
import peakaboo.mapping.results.MapResultSet;
import plural.streams.StreamExecutor;
import scidraw.drawing.painters.axis.AxisPainter;
import scitypes.Pair;
import scitypes.ReadOnlySpectrum;




/**
 * This class is the controller for plot displays.
 * 
 * @author Nathaniel Sherry, 2009
 */

public class PlotController extends EventfulType<String> 
{
	
	public List<AxisPainter>				axisPainters;

	
	private UndoController					undoController;
	private DataController					dataController;
	private FilteringController				filteringController;
	private FittingController				fittingController;
	private ViewController					viewController;


	public static enum UpdateType
	{
		DATA, FITTING, FILTER, UNDO, UI
	}
	
	
	public PlotController()
	{
		super();
		initPlotController();
	}

	private void initPlotController()
	{
				
		undoController = new UndoController(this);
		dataController = new DataController(this);
		filteringController = new FilteringController(this);
		fittingController = new FittingController(this);
		viewController = new ViewController(this);
		viewController.loadPersistentSettings();
		
		undoController.addListener(() -> updateListeners(UpdateType.UNDO.toString()));
		dataController.addListener(() -> updateListeners(UpdateType.DATA.toString()));
		filteringController.addListener(() -> updateListeners(UpdateType.FILTER.toString()));		
		fittingController.addListener(b -> updateListeners(UpdateType.FITTING.toString()));
		viewController.addListener(() -> updateListeners(UpdateType.UI.toString()));
		
		undoController.setUndoPoint("");
	}

	
	public SavedSession getSavedSettings() {
		return SavedSession.storeFrom(this);
	}
	


	public void loadSettings(String data, boolean isUndoAction)
	{
		SavedSession saved = SavedSession.deserialize(data);
		saved.loadInto(this);
		
		if (!isUndoAction) undoController.setUndoPoint("Load Session");
		
		filteringController.filteredDataInvalidated();
		fittingController.fittingDataInvalidated();
		fittingController.fittingProposalsInvalidated();
		viewController.updateListeners();
		
		//fire an update message from the fittingcontroller with a boolean flag
		//indicating that the change is not comming from inside the fitting controller
		fittingController.updateListeners(true);
		
	}
	
	public SavedSession readSavedSettings(String yaml) {
		return SavedSession.deserialize(yaml);
	}
	
	public void loadSessionSettings(SavedSession saved) {
		saved.loadInto(this);
		
		filteringController.filteredDataInvalidated();
		fittingController.fittingDataInvalidated();
		fittingController.fittingProposalsInvalidated();
		viewController.updateListeners();
		
		//fire an update message from the fittingcontroller with a boolean flag
		//indicating that the change is not comming from inside the fitting controller
		fittingController.updateListeners(true);

	}

	/**
	 * Get the scan that should currently be shown. Looks up appropriate information
	 * in the settings controller, and uses it to calculate the current scan from the
	 * raw data supplied by the data controller.
	 * @return a Spectrum which contains a scan
	 */
	private ReadOnlySpectrum currentScan()
	{
		ReadOnlySpectrum originalData = null;
		
		if (viewController.getChannelCompositeMode() == ChannelCompositeMode.AVERAGE) {
			originalData = dataController.getDataSet().getAnalysis().averagePlot();
		} else if (viewController.getChannelCompositeMode()  == ChannelCompositeMode.MAXIMUM) {
			originalData = dataController.getDataSet().getAnalysis().maximumPlot();
		} else {
			originalData = dataController.getDataSet().getScanData().get(viewController.getScanNumber());
		}
		
		return originalData;
		
	}
	

	/**
	 * Returns a pair of spectra. The first one is the filtered data, the second is the original
	 */
	public Pair<ReadOnlySpectrum, ReadOnlySpectrum> getDataForPlot()
	{

		ReadOnlySpectrum originalData = null;
	
		if (!dataController.hasDataSet() || currentScan() == null) return null;

		
		// get the original data
		originalData = currentScan();

		regenerateCahcedData();
		
		return new Pair<ReadOnlySpectrum, ReadOnlySpectrum>(filteringController.getFilteredPlot(), originalData);
	}


	/**
	 * In order to prevent high cpu use every time calculated data such as averaged, filtered, or 
	 * fitted plots are requested, the calculated data is cached. When a setting is changed such 
	 * that the cached data would no longer match freshly calculated data, the cache must be cleared.
	 */
	public void regenerateCahcedData()
	{

		// Regenerate Filtered Data
		if (dataController.hasDataSet() && currentScan() != null)
		{

			
			if (filteringController.getFilteredPlot() == null)
			{
				filteringController.calculateFilteredData(currentScan());
			}

			// Fitting Selections
			if (!fittingController.hasSelectionFitting())
			{
				fittingController.calculateSelectionFittings(filteringController.getFilteredPlot());
			}

			// Fitting Proposals
			if (!fittingController.hasProposalFitting())
			{
				fittingController.calculateProposalFittings();
			}

		}

	}
	
	
	/**
	 * Returns an {@link StreamExecutor} which will generate a map based on the user's current 
	 * selections.
	 * @param type The type of {@link FittingTransform} to use in the calculation
	 * @return
	 */
	public StreamExecutor<MapResultSet> getMapTask() {
		return dataController.getMapTask(
				filteringController.getActiveFilters(), 
				fittingController.getFittingSelections(), 
				fittingController.getCurveFitter(), 
				fittingController.getFittingSolver()
			);
	}
	
	
	public DataController data()
	{
		return dataController;
	}

	public FilteringController filtering()
	{
		return filteringController;
	}

	public FittingController fitting()
	{
		return fittingController;
	}

	public UndoController history()
	{
		return undoController;
	}

	public ViewController view()
	{
		return viewController;
	}

	public List<AxisPainter> getAxisPainters()
	{
		return axisPainters;
	}

	public void setAxisPainters(List<AxisPainter> axisPainters)
	{
		this.axisPainters = axisPainters;
	}
	
	
}
