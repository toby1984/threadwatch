/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
		double nanos1 = startSeconds*1000000000+startNanos;
		double nanos2 = endSeconds*1000000000+endNanos;
		double deltaNano = nanos2-nanos1;
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