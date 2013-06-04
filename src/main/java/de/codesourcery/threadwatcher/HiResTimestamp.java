/**
 * Copyright 2013 Tobias Gierke <tobias.gierke@code-sourcery.de>
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
    
    public static HiResTimestamp min(HiResTimestamp t1,HiResTimestamp t2) 
    {
        if ( t1.compareTo( t2 ) <= 0 ) {
            return t1;
        }
        return t2;
    }
    
    public static HiResTimestamp max(HiResTimestamp t1,HiResTimestamp t2) 
    {
        if ( t1.compareTo( t2 ) >= 0 ) {
            return t1;
        }
        return t2;
    }    
    
    @Override
    public String toString() {
    	return toDateTime()+" (millis: "+truncatedToMillis+" | "+nanoseconds+")";
    }
    
    public HiResTimestamp plusSeconds(int seconds) 
    {
    	return new HiResTimestamp(this.secondsSinceEpoch+seconds , this.nanoseconds , this.truncatedToMillis );
    }
    
    public HiResTimestamp plusMilliseconds(long millis) 
    {
    	int seconds = (int) (millis/1000.0);
    	long m = (long) (millis - seconds*1000.0);
    	long newNanos = this.nanoseconds + m*1000000;
    	if ( newNanos > 1000000000 ) 
    	{
    		long deltaSeconds = newNanos / 1000000000;
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

        return DF.print( toDateTime());
	}	
}