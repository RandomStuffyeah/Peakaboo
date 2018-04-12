package peakaboo.datasource.model.internal;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import net.sciencestudio.autodialog.model.Group;
import peakaboo.datasource.model.DataSource;
import peakaboo.datasource.model.components.datasize.DataSize;
import peakaboo.datasource.model.components.fileformat.FileFormat;
import peakaboo.datasource.model.components.interaction.Interaction;
import peakaboo.datasource.model.components.metadata.Metadata;
import peakaboo.datasource.model.components.physicalsize.PhysicalSize;
import peakaboo.datasource.model.components.scandata.ScanData;

public class DelegatingDataSource implements DataSource {

	private DataSource backer;
	
	public DelegatingDataSource() {
		this(new EmptyDataSource());
	}
	
	public DelegatingDataSource(DataSource backer) {
		this.backer = backer;
	}
	
	public void setDataSource(DataSource backer) {
		this.backer = backer;
	}
	
	public DataSource getDataSource() {
		return backer;
	}
	
	@Override
	public void setInteraction(Interaction interaction) {
		this.backer.setInteraction(interaction);
	}
	@Override
	public Interaction getInteraction() {
		return this.backer.getInteraction();
	}

	
	public Optional<Metadata> getMetadata() {
		return backer.getMetadata();
	}

	public Optional<DataSize> getDataSize() {
		return backer.getDataSize();
	}

	public FileFormat getFileFormat() {
		return backer.getFileFormat();
	}

	public void read(Path file) throws Exception {
		backer.read(file);
	}

	public void read(List<Path> files) throws Exception {
		backer.read(files);
	}

	@Override
	public ScanData getScanData() {
		return backer.getScanData();
	}

	@Override
	public Optional<PhysicalSize> getPhysicalSize() {
		return backer.getPhysicalSize();
	}
	
	@Override
	public Optional<Group> getParameters() {
		return Optional.empty();
	}
	
}
