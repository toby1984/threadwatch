package de.codesourcery.threadwatcher;

import org.joda.time.DateTime;

public final class HiResTimestamp implements Comparable<HiResTimestamp>
{
    public final long secondsSinceEpoch;
    public final long nanoseconds;
    public final boolean truncatedToMillis;
    
    public HiResTimestamp(HiResTimestamp other)
    {
    	this.secondsSinceEpoch = other.secondsSinceEpoch;
    	this.nanoseconds = other.nanoseconds;
    	this.truncatedToMillis = other.truncatedToMillis;
    }
    
    public HiResTimestamp(DateTime dt,boolean truncatedToMillis)
    {
    	this( dt.getMillis()/1000 , (dt.getMillis() - (dt.getMillis()/1000)*1000)*1000000, truncatedToMillis );
    }
    
    public HiResTimestamp(long secondsSinceEpoch, long nanoseconds,boolean truncatedToMillis)
    {
        this.secondsSinceEpoch = secondsSinceEpoch;
        this.nanoseconds = nanoseconds;
        this.truncatedToMillis = truncatedToMillis;
    }
    
    @Override
    public String toString() {
    	return toDateTime()+" (millis: "+truncatedToMillis+" | "+nanoseconds+")";
    }
    
    public HiResTimestamp plusSeconds(int seconds) 
    {
    	return new HiResTimestamp(this.secondsSinceEpoch+seconds , this.nanoseconds , this.truncatedToMillis );
    }
    
    public HiResTimestamp plusMilliseconds(int millis) 
    {
    	int seconds = (int) (millis/1000.0);
    	long m = (long) (millis - seconds*1000.0);
    	long newNanos = this.nanoseconds + m*1000000;
    	if ( newNanos > 1000000000 ) 
    	{
    		long deltaSeconds = (long) (newNanos / 1000000000);
    		seconds += deltaSeconds;
    		newNanos = newNanos - deltaSeconds*1000000000;
    	} else if ( newNanos < 0 ) {
    		seconds--;
    		newNanos += 1000000000;
    	}
    	return new HiResTimestamp(this.secondsSinceEpoch+seconds , newNanos , this.truncatedToMillis );
    }    
    
    @Override
	public int hashCode() 
    {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (nanoseconds ^ (nanoseconds >>> 32));
		result = prime * result + (int) (secondsSinceEpoch ^ (secondsSinceEpoch >>> 32));
		return result;
	}
    
	@Override
	public boolean equals(Object obj) 
	{
		if (obj == null|| obj.getClass() != HiResTimestamp.class) 
		{
			return false;
		}
		final HiResTimestamp other = (HiResTimestamp) obj;
		return nanoseconds == other.nanoseconds && secondsSinceEpoch != other.secondsSinceEpoch;
	}
	
	public HiResTimestamp truncateToMilliseconds() {
		long millis = (long) (nanoseconds / 1000000.0);
		return new HiResTimestamp( secondsSinceEpoch , millis*1000000 , true );
	}

	public DateTime toDateTime() 
    {
        final int millis = (int) (nanoseconds / 1000000.0);
        return new DateTime( secondsSinceEpoch*1000  ).plusMillis( millis );
    }

	public boolean isAfter(HiResTimestamp timestamp) 
	{
    	if ( this.secondsSinceEpoch > timestamp.secondsSinceEpoch ) {
    		return true;
    	}
    	if ( this.secondsSinceEpoch < timestamp.secondsSinceEpoch ) {
    		return false;
    	}
		return this.nanoseconds > timestamp.nanoseconds;
    }
	
	@Override
	public int compareTo(HiResTimestamp other) 
	{
		if ( this.truncatedToMillis != other.truncatedToMillis ) 
		{
			if ( this.truncatedToMillis ) 
			{
				return this.compareTo( other.truncateToMilliseconds() );
			}
			return this.truncateToMilliseconds().compareTo( other );
		}
		
		if ( this.secondsSinceEpoch > other.secondsSinceEpoch ) {
			return 1;
		}
		if ( this.secondsSinceEpoch < other.secondsSinceEpoch ) {
			return -1;
		}		
		if ( this.nanoseconds < other.nanoseconds ) {
			return -1;
		}
		if ( this.nanoseconds > other.nanoseconds ) {
			return 1;
		}		
		return 0;
	}	
}