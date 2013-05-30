package de.codesourcery.threadwatcher;

import junit.framework.TestCase;

import org.joda.time.DateTime;

public class HiResTimestampTest extends TestCase {

	public void testTruncateToMillis1() 
	{
		long seconds = 123123123;
		long nanos = 534531111;
		HiResTimestamp ts1 = new HiResTimestamp(seconds,nanos,false);
		HiResTimestamp truncated = ts1.truncateToMilliseconds();
		assertEquals(123123123,truncated.secondsSinceEpoch);
		assertEquals(534*1000000,truncated.nanoseconds);
		assertTrue(truncated.truncatedToMillis);
	}	
	
	public void testCompareToNoTruncate() 
	{
		HiResTimestamp ts1 = new HiResTimestamp(new DateTime(),false);
		HiResTimestamp ts2 = new HiResTimestamp(ts1);
		
		assertFalse( ts1.isAfter( ts2 ) );
		assertFalse( ts2.isAfter( ts1 ) );
		
		assertFalse( ts1.truncatedToMillis );
		assertEquals( 0 , ts1.compareTo( ts2 ) );
		
		ts2 = ts2.plusMilliseconds( 1 );
		assertEquals( -1 , ts1.compareTo( ts2 ) );
		assertTrue( ts2.isAfter( ts1 ) );
		assertFalse( ts1.isAfter( ts2 ) );
		
		ts2 = ts1.plusMilliseconds( -1 );
		assertEquals( 1 , ts1.compareTo( ts2 ) );		
	}	
	
	public void testPlusSeconds() {
		
		long seconds = 123123123;
		long nanos = 5345311;
		HiResTimestamp ts1 = new HiResTimestamp(seconds,nanos,false);
		HiResTimestamp ts2 = ts1.plusSeconds( 1 );
		assertEquals(seconds+1,ts2.secondsSinceEpoch);
		assertEquals(nanos,ts2.nanoseconds);
	}
	
	public void testMinusSeconds() {
		
		long seconds = 123123123;
		long nanos = 5345311;
		HiResTimestamp ts1 = new HiResTimestamp(seconds,nanos,false);
		HiResTimestamp ts2 = ts1.plusSeconds( -1 );
		assertEquals(seconds-1,ts2.secondsSinceEpoch);
		assertEquals(nanos,ts2.nanoseconds);
	}	
	
	public void testPlusMillisNoOverflow() {
		
		long seconds = 123123123;
		long millis = 534;
		long nanos = millis*1000000;
		HiResTimestamp ts1 = new HiResTimestamp(seconds,nanos,false);
		HiResTimestamp ts2 = ts1.plusMilliseconds( 100 );
		assertEquals(seconds,ts2.secondsSinceEpoch);
		assertEquals(millis+100,ts2.nanoseconds/1000000);
	}
	
	public void testMinusMillisNoUnderflow() {

		long seconds = 123123123;
		long millis = 534;
		long nanos = millis*1000000;
		HiResTimestamp ts1 = new HiResTimestamp(seconds,nanos,false);
		HiResTimestamp ts2 = ts1.plusMilliseconds( -100 );
		assertEquals(seconds,ts2.secondsSinceEpoch);
		assertEquals(millis-100,ts2.nanoseconds/1000000);		
	}		
	
	public void testPlusMillisOverflow() {
		
		long seconds = 123123123;
		long millis = 600;
		long nanos = millis*1000000;
		HiResTimestamp ts1 = new HiResTimestamp(seconds,nanos,false);
		HiResTimestamp ts2 = ts1.plusMilliseconds( 500 );
		assertEquals(seconds+1,ts2.secondsSinceEpoch);
		assertEquals(100,ts2.nanoseconds/1000000);
	}
	
	public void testMinusMillisUnderflow() {

		long seconds = 123123123;
		long millis = 500;
		long nanos = millis*1000000;
		HiResTimestamp ts1 = new HiResTimestamp(seconds,nanos,false);
		HiResTimestamp ts2 = ts1.plusMilliseconds( -600 );
		assertEquals(seconds-1,ts2.secondsSinceEpoch);
		assertEquals(900,ts2.nanoseconds/1000000);		
	}	
}
