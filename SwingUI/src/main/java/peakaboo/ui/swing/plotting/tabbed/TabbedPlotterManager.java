package peakaboo.ui.swing.plotting.tabbed;


import peakaboo.datasource.model.DataSource;
import peakaboo.ui.swing.plotting.PlotPanel;


//simple controls for TabbedPlotterFrame
public class TabbedPlotterManager
{
	
	private TabbedPlotterFrame plotterFrame;
	
	TabbedPlotterManager(TabbedPlotterFrame plotterFrame)
	{
		this.plotterFrame = plotterFrame;
	}

	public TabbedPlotterFrame getWindow()
	{
		return plotterFrame;
	}

	public void setTitle(PlotPanel plotPanel, String title)
	{
		if (title.trim().length() == 0) title = "No Data";
		plotterFrame.getTabControl().setTabTitle(plotPanel, title);
	}
	
	public PlotPanel newTab(DataSource ds, String savedSettings)
	{

		PlotPanel plotPanel = newTab();
		
		//create a new datasource which is a subset of the passed one
		plotPanel.loadExistingDataSource(ds, savedSettings);
		return plotPanel;
		
	}
	
	private PlotPanel newTab()
	{
		return plotterFrame.getTabControl().newTab();
	}
	
	
	

}
