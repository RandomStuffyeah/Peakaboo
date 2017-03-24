package peakaboo.datasource.internal;

import java.util.Collections;
import java.util.List;

import peakaboo.datasource.DataSource;
import peakaboo.datasource.components.dimensions.DataSourceDimensions;
import peakaboo.datasource.components.metadata.DataSourceMetadata;
import scitypes.Bounds;
import scitypes.Coord;
import scitypes.Spectrum;

/**
 * @author maxweld
 * 
 */
public class EmptyDataSource implements DataSource {

	// Data Source //
	
	@Override
	public DataSourceMetadata getMetadata() {
		return null;
	}

	@Override
	public boolean canRead(String filename) {
		return false;
	}

	@Override
	public boolean canRead(List<String> filenames) {
		return false;
	}

	@Override
	public List<String> getFileExtensions() {
		return Collections.emptyList();
	}

	@Override
	public void read(String filename) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public void read(List<String> filenames) throws Exception {
		throw new UnsupportedOperationException();
	}

	@Override
	public Spectrum get(int index) {
		throw new IndexOutOfBoundsException(); 
	}
	
	// DSScanData //
	

	@Override
	public int scanCount() {
		return 0;
	}

	@Override
	public List<String> scanNames() {
		return Collections.emptyList();
	}

	@Override
	public float maxEnergy() {
		return 0;
	}

	@Override
	public String datasetName() {
		return "";
	}

	@Override
	public DataSourceDimensions getDimensions() {
		return null;
	}


	
}