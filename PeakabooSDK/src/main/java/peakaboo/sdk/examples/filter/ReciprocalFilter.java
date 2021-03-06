package peakaboo.sdk.examples.filter;



import net.sciencestudio.autodialog.model.Parameter;
import net.sciencestudio.autodialog.model.style.editors.BooleanStyle;
import peakaboo.filter.model.AbstractSimpleFilter;
import peakaboo.filter.model.FilterType;
import scitypes.ISpectrum;
import scitypes.ReadOnlySpectrum;
import scitypes.Spectrum;


public class ReciprocalFilter extends AbstractSimpleFilter
{

	Parameter<Boolean> pEnabled;
	
	@Override
	public void initialize()
	{
		pEnabled = new Parameter<>("Enabled", new BooleanStyle(), Boolean.TRUE);
		addParameter(pEnabled);
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
	public String pluginVersion() {
		return "1.0";
	}
	
	@Override
	public String pluginUUID() {
		return "d0b9c415-4d41-4a45-bb9a-0f35bc77cd14";
	}
	
	@Override
	protected ReadOnlySpectrum filterApplyTo(ReadOnlySpectrum data)
	{
		//check enabled property
		if (!pEnabled.getValue()) return data;
		
		
		Spectrum result = new ISpectrum(data.size());
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
