package peakaboo.filter;


import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import commonenvironment.Env;
import fava.functionable.FList;

import bolt.plugin.BoltPlugin;
import bolt.plugin.BoltPluginLoader;
import bolt.plugin.ClassInheritanceException;

import peakaboo.calculations.Background;
import peakaboo.calculations.Calculations;
import peakaboo.calculations.Noise;
import peakaboo.common.Version;
import scidraw.drawing.plot.painters.PlotPainter;
import scitypes.Spectrum;

/**
 * 
 * This abstract class defines a filter for a {@link Spectrum} of data. Also contains messy logic for enumerating
 * all classes in this package which inherit from this class, and are thus considered filters. This is
 * primarily intended for use in UIs, as it focuses on classifying the type and name of the filter, and the
 * types of parameters. This provides a way for UIs to display the filter and let the user change the
 * settings. When applying filters programmatically, it would probably be more desirable to simple use the
 * functions in the {@link Calculations}, {@link SpectrumCalculations}, {@link Background}, and {@link Noise}
 * classes which are the backend for these filters.
 * 
 * 
 * @author Nathaniel Sherry, 2009
 * 
 */

public abstract class AbstractFilter implements BoltPlugin, Serializable
{
	
	public static enum FilterType
	{
		
		BACKGROUND {

			@Override
			public String toString()
			{
				return "Background Removal";
			}			
		},
		NOISE {

			@Override
			public String toString()
			{
				return "Noise Removal";
			}
		},
		MATHEMATICAL {

			@Override
			public String toString()
			{
				return "Mathematical";
			}
		},
		ADVANCED {

			@Override
			public String toString()
			{
				return "Advanced";
			}	
		},
		PROGRAMMING {
		
			@Override
			public String toString()
			{
				return "Programming";
			}
			
		};
		
		public String getSubPackage()
		{
			return "filters." + name().toLowerCase();
		}
	}
	
	private Map<Object, Parameter>			parameters;
	public boolean							enabled;
	
	protected Spectrum	previewCache;
	protected Spectrum	calculatedData;

	
	
	
	public AbstractFilter()
	{
		this.parameters = new LinkedHashMap<Object, Parameter>();
		this.enabled = true;
	}

	public static FList<AbstractFilter> getAvailableFilters()
	{

		try {
			
			FList<AbstractFilter> filters = new FList<AbstractFilter>(); 
			
			BoltPluginLoader<AbstractFilter> pluginLoader;
			pluginLoader = new BoltPluginLoader<AbstractFilter>(AbstractFilter.class);
			
			Package p;
			for (FilterType ft : FilterType.values()) {
				
				p = AbstractFilter.class.getPackage();				
				pluginLoader.loadLocalPlugins(p.getName() + "." + ft.getSubPackage());
				
			}
			
			File appDataDir = Env.appDataDirectory(Version.program_name);
			appDataDir.mkdirs();
			
			pluginLoader.loadPluginsFromJarsInDirectory(appDataDir);
			if (Env.isClassInJar(AbstractFilter.class)) 
			{
				pluginLoader.loadPluginsFromJarsInDirectory(Env.getJarForClass(AbstractFilter.class).getParentFile());
			}
			
			filters.addAll(pluginLoader.getNewInstancesForAllPlugins());
			return filters;
			
		} catch (ClassInheritanceException e) {
			e.printStackTrace();
		}
				
		return null;
		
	}


	
	
	
	
	
	public abstract FilterType getFilterType();


	public final Map<Object, Parameter> getParameters()
	{
		return this.parameters;
	}
	public final void setParameters(Map<Object, Parameter> params)
	{
		parameters = params;
	}
	
	protected void addParameter(Object key, Parameter value)
	{
		parameters.put(key, value);
	}

	public final Parameter getParameter(Object key)
	{
		return parameters.get(key);
	}

	protected final void setPreviewCache(Spectrum data)
	{
		this.previewCache = new Spectrum(data);
	}


	public abstract void initialize();
	public abstract PlotPainter getPainter();
	public abstract boolean validateParameters();
	protected abstract Spectrum filterApplyTo(Spectrum data, boolean cache);
	public abstract boolean canFilterSubset();
	
	
	public Spectrum filter(Spectrum data, boolean cache)
	{
		
		try{
			return filterApplyTo(data, cache);
		}
		catch(Throwable e)
		{
			System.out.println(getPluginName() + " Filter Failed");
			if (!Version.release) e.printStackTrace();
			return data;
		}
		
	}
	
		
	

	
	
	public String toString()
	{
		return this.getPluginName();
	}
}
