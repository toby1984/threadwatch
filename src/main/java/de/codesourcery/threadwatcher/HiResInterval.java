package de.codesourcery.threadwatcher;

import org.joda.time.DateTime;

public final class HiResInterval {

	public final HiResTimestamp start;
	public final HiResTimestamp end;
	
	public HiResInterval(DateTime start, DateTime end) {
		this( new HiResTimestamp( start , false ) ,new HiResTimestamp( end , false ) ); 
	}
	
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
		return new HiResInterval( start.plusMilliseconds( millis ) , end.plusMilliseconds( millis ) );
	}	
	
	public double getDurationInMilliseconds() 
	{
		return getDurationInMilliseconds(this.start,this.end);
	}
	
	public static double getDurationInMilliseconds(HiResTimestamp start,HiResTimestamp end) 
	{
		if ( start.compareTo( end ) > 0 ) {
			throw new IllegalArgumentException( "start "+start+" > end "+end);
		}
		long deltaSeconds = end.secondsSinceEpoch - start.secondsSinceEpoch;
		long deltaNano = end.nanoseconds >= start.nanoseconds ? end.nanoseconds-start.nanoseconds : start.nanoseconds - end.nanoseconds;
		if ( deltaNano < 0 ) {
			throw new RuntimeException("getDurationMillis( "+start+" - "+end+" ) = "+deltaNano);
		}
		double result = deltaSeconds*1000.0 + deltaNano/1000000.0;
		if ( result < 0 ) {
			throw new RuntimeException("getDurationMillis( "+start+" - "+end+" ) = "+deltaNano);
		}
		return result;
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
			return i1.end.compareTo( i2.end ) >= 0;
		} else if ( i2.contains( i1.start ) && i2.contains( i1.end ) ) {
			return true;
		}
		return false;
	}
	
}
