package peakaboo.filter.model;

public enum FilterType
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
					
	},
	OTHER {

		@Override
		public String toString()
		{
			return "Other";
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