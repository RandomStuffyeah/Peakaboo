package peakaboo.datasource.internal;

import java.util.List;

import com.sun.xml.internal.ws.server.UnsupportedMediaException;

import fava.functionable.FList;
import fava.functionable.Range;
import peakaboo.datasource.DataSource;
import peakaboo.datasource.components.datasize.DataSize;
import peakaboo.datasource.components.fileformat.FileFormat;
import peakaboo.datasource.components.interaction.Interaction;
import peakaboo.datasource.components.metadata.Metadata;
import peakaboo.datasource.components.physicalsize.PhysicalSize;
import peakaboo.datasource.components.scandata.ScanData;
import scitypes.Bounds;
import scitypes.Coord;
import scitypes.GridPerspective;
import scitypes.SISize;
import scitypes.Spectrum;


public class CroppedDataSource implements DataSource, DataSize, PhysicalSize, ScanData
{

	private FList<String>				scannames = new FList<String>();
	private DataSource					originalDataSource;
	
	private int							sizeX, sizeY;
	private Range						rangeX, rangeY;
	
	public CroppedDataSource(DataSource ds, int sizeX, int sizeY, Coord<Integer> cstart, Coord<Integer> cend)
	{
		
		originalDataSource = ds;
		
		this.sizeX = sizeX;
		this.sizeY = sizeY;
		
		this.rangeX = new Range(cstart.x, cend.x);
		this.rangeY = new Range(cstart.y, cend.y);
		
		scannames = new FList<String>();
		for (Integer c : new Range(0, scanCount()))
		{
			scannames.add("Scan " + c);			
		}
		
	}
	

	public String datasetName()
	{
		return originalDataSource.getScanData().datasetName() + " Subset";
	}


	public int getExpectedScanCount()
	{
		return rangeX.size() * rangeY.size();
	}


	public float maxEnergy()
	{
		return originalDataSource.getScanData().maxEnergy();
	}


	public Spectrum get(int index)
	{
		
		
		GridPerspective<Spectrum> grid = new GridPerspective<Spectrum>(rangeX.size(), rangeY.size(), null);
		GridPerspective<Spectrum> origgrid = new GridPerspective<Spectrum>(sizeX, sizeY, null);
		
		int x = grid.getXYFromIndex(index).first;
		int y = grid.getXYFromIndex(index).second;
		
		x += rangeX.getStart();
		y += rangeY.getStart();
				
		int realIndex = origgrid.getIndexFromXY(x, y);
				
		return originalDataSource.getScanData().get(realIndex);
	}


	@Override
	public Coord<Integer> getDataCoordinatesAtIndex(int index)
	{
		
		if (!originalDataSource.hasDataSize()) { throw new UnsupportedOperationException(); }
		
		GridPerspective<Spectrum> grid = new GridPerspective<Spectrum>(rangeX.size(), rangeY.size(), null);
		GridPerspective<Spectrum> origgrid = new GridPerspective<Spectrum>(sizeX, sizeY, null);
		
		int x = grid.getXYFromIndex(index).first;
		int y = grid.getXYFromIndex(index).second;
		
		x += rangeX.getStart();
		y += rangeY.getStart();
		
		int realIndex = origgrid.getIndexFromXY(x, y);
				
		return originalDataSource.getDataSize().getDataCoordinatesAtIndex(realIndex);
	}
	
	
	public int scanCount()
	{
		return rangeX.size() * rangeY.size();
	}


	public List<String> scanNames()
	{
		return scannames.toSink();
	}


	public Coord<Integer> getDataDimensions()
	{
		return new Coord<Integer>(rangeX.size(), rangeY.size());
	}


	public Coord<Number> getPhysicalCoordinatesAtIndex(int index)
	{
		
		if (!originalDataSource.hasDataSize()) { throw new UnsupportedOperationException(); }
		
		GridPerspective<Spectrum> grid = new GridPerspective<Spectrum>(rangeX.size(), rangeY.size(), null);
		GridPerspective<Spectrum> origgrid = new GridPerspective<Spectrum>(sizeX, sizeY, null);
		
		int x = grid.getXYFromIndex(index).first;
		int y = grid.getXYFromIndex(index).second;
		
		x += rangeX.getStart();
		y += rangeY.getStart();
		
		int realIndex = origgrid.getIndexFromXY(x, y);
				
		return originalDataSource.getPhysicalSize().getPhysicalCoordinatesAtIndex(realIndex);
	
	}


	public Coord<Bounds<Number>> getPhysicalDimensions()
	{		
		
		GridPerspective<Spectrum> grid = new GridPerspective<Spectrum>(rangeX.size(), rangeY.size(), null);
		Coord<Number> bottomLeft, bottomRight, topLeft;
		
		bottomLeft 	= getPhysicalCoordinatesAtIndex(0);
		topLeft 	= getPhysicalCoordinatesAtIndex(grid.getIndexFromXY( 0, 				rangeY.size()-1	));
		bottomRight = getPhysicalCoordinatesAtIndex(grid.getIndexFromXY( rangeX.size()-1, 	0				));
		//topRight	= getRealCoordinatesAtIndex(grid.getIndexFromXY( rangeX.size()-1, 	rangeY.size()-1	));
				
		Bounds<Number> bx = new Bounds<Number>(bottomLeft.x, bottomRight.x);
		Bounds<Number> by = new Bounds<Number>(bottomLeft.y, topLeft.y);
		
	
		return new Coord<Bounds<Number>>(bx, by);
		
	}


	public SISize getPhysicalUnit()
	{
		if (!originalDataSource.hasDataSize()) { throw new UnsupportedOperationException(); }
		return originalDataSource.getPhysicalSize().getPhysicalUnit();
	}



	@Override
	public void read(String filename) throws Exception
	{
		//This should never be called, since the data source this one copies from
		//should already have been initialized
		throw new UnsupportedOperationException();
	}


	@Override
	public void read(List<String> filenames) throws Exception
	{
		//This should never be called, since the data source this one copies from
		//should already have been initialized
		throw new UnsupportedOperationException();
	}


	@Override
	public Metadata getMetadata() {
		return originalDataSource.getMetadata();
	}


	@Override
	public DataSize getDataSize() {
		if (!originalDataSource.hasDataSize()) { return null; }
		return this;
	}


	@Override
	public FileFormat getFileFormat() {
		return originalDataSource.getFileFormat();
	}


	@Override
	public void setInteraction(Interaction interaction) {
		originalDataSource.setInteraction(interaction);
	}

	@Override
	public Interaction getInteraction() {
		return originalDataSource.getInteraction();
	}


	@Override
	public ScanData getScanData() {
		return this;
	}


	@Override
	public PhysicalSize getPhysicalSize() {
		return this;
	}


	
	
}