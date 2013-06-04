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

import junit.framework.TestCase;

public class HiResIntervalTest extends TestCase {

	public void testStartNeedsToBeLargerThanEnd() 
	{
		DateTime start = new DateTime();
		DateTime end = start.minusMillis(1);
		try {
			new HiResInterval(start,end);
			fail("Should've failed");
		} catch(IllegalArgumentException e) {
			// ok
		}
	}
	
	public void testContains1() 
	{
		DateTime start = new DateTime();
		DateTime end = start.plusSeconds(1);
		HiResInterval interval = new HiResInterval(start,end);
		DateTime t = start.plusMillis(500);
		assertTrue(interval.contains( new HiResTimestamp(t , false ) ) );
		assertTrue(interval.contains( new HiResTimestamp(start , false ) ) );
		assertFalse(interval.contains( new HiResTimestamp(end , false ) ) );
	}		
	
	public void testOverlaps1() 
	{
		/*
		 *    |---------|  i1
		 *  |-------------| i2
		 */
		DateTime start1 = new DateTime();
		DateTime end1 = start1.plusSeconds(1);
		
		DateTime start2 = start1.minusSeconds(1);
		DateTime end2 = end1.plusSeconds(1);		
		
		HiResInterval interval1 = new HiResInterval(start1,end1);
		HiResInterval interval2 = new HiResInterval(start2,end2);
		assertTrue( interval1.overlaps( interval2 ) );
		assertTrue( interval2.overlaps( interval1 ) );
	}
	
	public void testOverlaps2() 
	{
		/*
		 * |---------|  i1
		 *     |-------------| i2
		 */
		DateTime start1 = new DateTime();
		DateTime end1 = start1.plusSeconds(1);
		
		DateTime start2 = start1.plusMillis(500);
		DateTime end2 = start2.plusSeconds(1);
		
		HiResInterval interval1 = new HiResInterval(start1,end1);
		HiResInterval interval2 = new HiResInterval(start2,end2);
		assertTrue( interval1.overlaps( interval2 ) );
		assertTrue( interval2.overlaps( interval1 ) );
	}	
	
	public void testStartEqualsEnd() 
	{
		DateTime start = new DateTime();
		HiResInterval interval = new HiResInterval(start,start);
		assertEquals(0.0,interval.getDurationInMilliseconds() , 0.001 );
	}	
	
	public void testGetDurationInMillis() 
	{
		DateTime start = new DateTime();
		HiResInterval interval = new HiResInterval(start,start.plusMillis( 111 ));
		assertEquals(111.0,interval.getDurationInMilliseconds() , 0.001 );
	}	
	
	public void testGetDurationInMillis2() 
	{
		DateTime start = new DateTime();
		HiResInterval interval = new HiResInterval(start,start.plusSeconds( 1 ).plusMillis( 123 ));
		assertEquals(1123.0,interval.getDurationInMilliseconds() , 0.001 );
	}		
}
