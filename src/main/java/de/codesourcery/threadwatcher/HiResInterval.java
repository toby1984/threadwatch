package de.codesourcery.threadwatcher;

public final class HiResInterval {

	public final HiResTimestamp start;
	public final HiResTimestamp end;
	
	public HiResInterval(HiResTimestamp start, HiResTimestamp end) 
	{
		if ( start.isAfter(end ) ) {
			throw new IllegalArgumentException("start "+start+" s after end "+end+" ?");
		}
		if ( start.truncatedToMillis != end.truncatedToMillis ) {
			throw new IllegalArgumentException("start "+start+" and end "+end+" have different precisions");
		}
		this.start = start;
		this.end = end;
	}
	
	public boolean contains(HiResTimestamp timestamp) 
	{
		return start.compareTo( timestamp ) <= 0 &&
			   end.compareTo( timestamp ) >0;
	}
	
	public HiResInterval rollBySeconds(int seconds) {
		return new HiResInterval( start.plusSeconds( seconds ) , end.plusSeconds( seconds ) );
	}
	
	public HiResInterval rollByMilliseconds(int millis) 
	{
		return new HiResInterval( start.plusMilliseconds( millis ) , end.plusSeconds( millis ) );
	}	
	
	public double getDurationInMilliseconds() 
	{
		return getDurationInMilliseconds(this.start,this.end);
	}
	
	public static double getDurationInMilliseconds(HiResTimestamp start,HiResTimestamp end) {
		double deltaSeconds = end.secondsSinceEpoch - start.secondsSinceEpoch;
		double deltaNano;
		if ( start.nanoseconds < end.nanoseconds ) {
			deltaNano = end.nanoseconds - start.nanoseconds;
		} else { // start > end
			deltaNano = (1000000-start.nanoseconds)+end.nanoseconds;
		}
		return  deltaSeconds*1000.0 + deltaNano/1000000.0;
	}
	
	@Override
	public String toString() {
		return start.toString()+" - "+end.toString();
	}
	
	public boolean overlaps(HiResInterval interval) 
	{	
		return overlaps(this,interval) || overlaps( interval, this );
	}

	private static boolean overlaps(HiResInterval i1,HiResInterval i2) 
	{
		if ( i1.start.compareTo( i2.start ) <= 0 ) 
		{
			if ( i2.contains( i1.end ) ) {
				return true;
			}
			return i1.end.compareTo( i2.end ) > 0;
		}
		return false;
	}
	
}
