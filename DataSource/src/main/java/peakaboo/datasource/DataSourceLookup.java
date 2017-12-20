package peakaboo.datasource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import peakaboo.datasource.components.fileformat.FileFormatCompatibility;
import peakaboo.datasource.framework.PluginDataSource;

public class DataSourceLookup
{

	public static List<DataSource> findDataSourcesForFiles(List<File> filenames, List<PluginDataSource> dsps)
	{	
		
		List<DataSource> maybe_by_filename = new ArrayList<DataSource>();
		List<DataSource> maybe_by_contents = new ArrayList<DataSource>();
		List<DataSource> yes_by_contents = new ArrayList<DataSource>();
		
		if (filenames.size() == 1)
		{
			File file = filenames.get(0);
			for (DataSource datasource : dsps)
			{
				try {
					FileFormatCompatibility compat = datasource.getFileFormat().compatibility(file);
					if ( compat == FileFormatCompatibility.NO ) continue;
					if ( compat == FileFormatCompatibility.MAYBE_BY_FILENAME) { maybe_by_filename.add(datasource); }
					if ( compat == FileFormatCompatibility.MAYBE_BY_CONTENTS) { maybe_by_contents.add(datasource); }
					if ( compat == FileFormatCompatibility.YES_BY_CONTENTS) { yes_by_contents.add(datasource); }
				} 
				catch (Throwable e) {
					e.printStackTrace();
				} 
			}
		}
		else
		{
			for (DataSource datasource : dsps)
			{
				try {
					FileFormatCompatibility compat = datasource.getFileFormat().compatibility(new ArrayList<File>(filenames));
					if ( compat == FileFormatCompatibility.NO ) continue;
					if ( compat == FileFormatCompatibility.MAYBE_BY_FILENAME) { maybe_by_filename.add(datasource); }
					if ( compat == FileFormatCompatibility.MAYBE_BY_CONTENTS) { maybe_by_contents.add(datasource); }
					if ( compat == FileFormatCompatibility.YES_BY_CONTENTS) { yes_by_contents.add(datasource); }
				} 
				catch (Throwable e) {
					e.printStackTrace();
				} 
			}
			
		}
		if (yes_by_contents.size() > 0) { return yes_by_contents; }
		if (maybe_by_contents.size() > 0) { return maybe_by_contents; }
		return maybe_by_filename;
		
	}

}