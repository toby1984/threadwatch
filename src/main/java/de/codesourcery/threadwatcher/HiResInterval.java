package de.codesourcery.threadwatcher;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

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
	
    public boolean containsEndInclusive(HiResTimestamp timestamp) 
    {
        return start.compareTo( timestamp ) <= 0 &&
               end.compareTo( timestamp ) >= 0;
    }	
	
	public HiResInterval rollBySeconds(int seconds) {
		return new HiResInterval( start.plusSeconds( seconds ) , end.plusSeconds( seconds ) );
	}
	
	public HiResInterval rollByMilliseconds(long millis) 
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
        return getDurationInMilliseconds(start.secondsSinceEpoch , start.nanoseconds , end.secondsSinceEpoch , end.nanoseconds );
    }
    
	public static double getDurationInMilliseconds(long startSeconds,long startNanos,long endSeconds,long endNanos) 
	{
		long nanos1 = startSeconds*1000000000+startNanos;
		long nanos2 = endSeconds*1000000000+endNanos;
		long deltaNano = nanos2-nanos1;
		double result = deltaNano/1000000;
		if ( result < 0 ) {
			throw new RuntimeException("getDurationMillis( "+startSeconds+"."+startNanos+" - "+endSeconds+"."+endNanos+" ) = "+deltaNano);
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
	
	public String toUIString()
	{
        DateTimeFormatter DF = new DateTimeFormatterBuilder()
        .appendYear(4, 4)
        .appendLiteral('-')                
        .appendMonthOfYear(1)
        .appendLiteral('-')                
        .appendDayOfMonth(1)
        .appendLiteral(" ")
        .appendHourOfDay(2)
        .appendLiteral(':')                
        .appendMinuteOfHour(2)
        .appendLiteral(':')
        .appendSecondOfMinute(2)
        .appendLiteral('.')
        .appendMillisOfSecond(1)
        .toFormatter();

        final String start = DF.print( this.start.toDateTime());
        final String end = DF.print( this.end.toDateTime());
        return start+" - "+end+" [ "+getDurationInMilliseconds()+" ms ]";
	}
}