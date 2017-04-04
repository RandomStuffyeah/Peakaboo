package peakaboo.filter.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import autodialog.model.Parameter;
import bolt.plugin.BoltPlugin;
import peakaboo.common.Version;
import scidraw.drawing.plot.painters.PlotPainter;
import scitypes.Spectrum;

/**
 * 
 * This abstract class defines a filter for a {@link Spectrum} of data. A large part of this abstract
 * class is for use in UIs, as it focuses on classifying the type and name of the filter, and the
 * types of parameters. This provides a way for UIs to display the filter and let the user change the
 * settings.
 * 
 * 
 * @author Nathaniel Sherry, 2009-2012
 * 
 */

public abstract class AbstractFilter implements Serializable, FilterPlugin
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
		
		public String getFilterTypeDescription()
		{
			return toString() + " Filters";
		}
	}
	
	private Map<Integer, Parameter<?>>		parameters;
	public boolean							enabled;
	
	protected Spectrum	previewCache;
	protected Spectrum	calculatedData;

	private int nextParameterIndex = 0;
	
	
	public AbstractFilter()
	{
		this.parameters = new LinkedHashMap<>();
		this.enabled = true;
	}

	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#getFilterName()
	 */
	@Override
	public abstract String getFilterName();
	
	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#getFilterDescription()
	 */
	@Override
	public abstract String getFilterDescription();
	

	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#getFilterType()
	 */
	@Override
	public abstract FilterType getFilterType();


	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#getParameters()
	 */
	@Override
	public final Map<Integer, Parameter<?>> getParameters()
	{
		return this.parameters;
	}
	
	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#setParameters(java.util.Map)
	 */
	@Override
	public final void setParameters(Map<Integer, Parameter<?>> params)
	{
		parameters = params;
	}
	
	protected void addParameter(Parameter<?> param)
	{
		int key = getNextParameterIndex();
		parameters.put(key, param);
	}
	
	protected void addParameter(Parameter<?>... params)
	{
		for (Parameter<?> param : params) { addParameter(param); }
	}
	

	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#getParameter(java.lang.Integer)
	 */
	@Override
	public final Parameter<?> getParameter(Integer key)
	{
		return parameters.get(key);
	}

	protected final void setPreviewCache(Spectrum data)
	{
		this.previewCache = new Spectrum(data);
	}


	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#initialize()
	 */
	@Override
	public abstract void initialize();
	
	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#getPainter()
	 */
	@Override
	public abstract PlotPainter getPainter();
	
	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#validateParameters()
	 */
	@Override
	public abstract boolean validateParameters();
	protected abstract Spectrum filterApplyTo(Spectrum data, boolean cache);
	
	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#canFilterSubset()
	 */
	@Override
	public abstract boolean canFilterSubset();
	
	
	/* (non-Javadoc)
	 * @see peakaboo.filter.model.Filter#filter(scitypes.Spectrum, boolean)
	 */
	@Override
	public Spectrum filter(Spectrum data, boolean cache)
	{
		
		try{
			Spectrum newdata = filterApplyTo(data, cache);
			if (newdata != null) return newdata;
			return data;
		}
		catch(Throwable e)
		{
			System.out.println(getFilterName() + " Filter Failed");
			if (!Version.release) e.printStackTrace();
			return data;
		}
		
	}
	
		
	

	private int getNextParameterIndex()
	{
		return nextParameterIndex++;
	}
	
	public String toString()
	{
		return this.getFilterName();
	}
	
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	
	
	
	public String save() {
		Map<Integer, Object> valuemap = new HashMap<>();
		for (Integer key : getParameters().keySet()) {
			valuemap.put(key, getParameter(key).getValue());
		}
		Yaml dumper = new Yaml();
		return dumper.dump(valuemap);
	}
	
	public void load(String settings) {
		Map<Integer, Object> valuemap;
		Yaml loader = new Yaml();
		valuemap = (Map<Integer, Object>) loader.load(settings);
		for (Integer key : getParameters().keySet()) {
			setParameter(getParameter(key), valuemap.get(key));
		}
	}
	
	private <T> void setParameter(Parameter<T> param, Object value) {
		param.setValue((T)value);
	}
	

	
	
}