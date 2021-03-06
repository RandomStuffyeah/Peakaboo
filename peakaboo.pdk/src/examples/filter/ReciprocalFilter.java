package examples.filter;


import peakaboo.filter.AbstractSimpleFilter;
import peakaboo.filter.Parameter;
import peakaboo.filter.Parameter.ValueType;
import scitypes.Spectrum;


public class ReciprocalFilter extends AbstractSimpleFilter
{

	Parameter enabled;
	
	@Override
	public void initialize()
	{
		enabled = new Parameter("Enabled", ValueType.BOOLEAN, new Boolean(true));
		addParameter(enabled);
	}
	
	@Override
	public String getFilterDescription()
	{
		return "This filter calculates the reciprocal value of each " +
				"channel, and normalizes the data so that the strongest" +
				"channel in the resultant spectrum is the same intensity" +
				"as the strongest channel in the input spectrum";
	}

	@Override
	public String getFilterName()
	{
		return "Reciprocal";
	}

	@Override
	public boolean pluginEnabled()
	{
		return true;
	}

	@Override
	public boolean canFilterSubset()
	{
		return true;
	}

	@Override
	public FilterType getFilterType()
	{
		return FilterType.MATHEMATICAL;
	}

	@Override
	public boolean validateParameters()
	{
		//no parameters requiring validate
		return true;
	}

	@Override
	protected Spectrum filterApplyTo(Spectrum data)
	{
		//check enabled property
		if (!enabled.boolValue()) return data;
		
		
		Spectrum result = new Spectrum(data.size());
		float maxIn = data.get(0);
		float maxOut = 0;
		
		float value;
		for (int i = 0; i < data.size(); i++)
		{
			
			value = data.get(i);
			if (value == 0)
			{
				result.set(i, 0f);
			}
			else
			{
				result.set(i, 1f / value);
			}
			
			maxIn = Math.max(data.get(i), maxIn);
			maxOut = Math.max(result.get(i), maxOut);
		}
		if (maxOut == 0) maxOut = 1; 
		
		float multiplier = maxIn / maxOut;
		for (int i = 0; i < data.size(); i++)
		{
			result.set(i, result.get(i) * multiplier);
		}
		
		return result;
	}


}
