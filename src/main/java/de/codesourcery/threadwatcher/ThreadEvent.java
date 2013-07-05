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

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.MalformedInputException;

public final class ThreadEvent
{
    public static final int THREAD_START = 0;
    public static final int THREAD_DEATH= 1;
    public static final int THREAD_STATE_CHANGE = 2;

    // max. thread name length (including zero termination byte)
    public static final int MAX_THREADNAME_LENGTH = 51;
    
    // size of generic part
    public static final int SIZEOF_GENERIC_EVENT = 24;
    
    // total record sizes for specific event types
    public static final int SIZEOF_THREAD_START_EVENT = SIZEOF_GENERIC_EVENT+MAX_THREADNAME_LENGTH;
    public static final int SIZEOF_THREAD_DEATH_EVENT = SIZEOF_GENERIC_EVENT+0;
    public static final int SIZEOF_THREAD_STATE_CHANGE_EVENT = SIZEOF_GENERIC_EVENT+4;    
    
    public static final int MAX_RECORD_SIZE= Math.max( Math.max( SIZEOF_THREAD_START_EVENT, SIZEOF_THREAD_DEATH_EVENT ) , SIZEOF_THREAD_STATE_CHANGE_EVENT);
    public static final int MIN_RECORD_SIZE= Math.min( Math.min( SIZEOF_THREAD_START_EVENT, SIZEOF_THREAD_DEATH_EVENT ) , SIZEOF_THREAD_STATE_CHANGE_EVENT);    

    public static final int BUFFER_LENGTH = MAX_RECORD_SIZE * 1000;
    
    public byte type;
    public int threadId;
    public long timestampSeconds;
    public long timestampNanos;
    public int threadStateMask;
    public String threadName;

    protected void parseCommonFields(byte[] buffer,int offset) 
    {
        this.type = buffer[offset]; // offset 0
        this.threadId = read16Bit( buffer          , (offset + 4) % BUFFER_LENGTH );
        this.timestampSeconds = read64Bit( buffer , (offset + 8) % BUFFER_LENGTH);
        this.timestampNanos = read64Bit( buffer  , (offset + 16) % BUFFER_LENGTH);        
    }
    
    protected int parseThreadStartEvent(byte[] buffer,int offset) {
        parseCommonFields(buffer,offset);
        threadName = readString(buffer, (offset+24)%BUFFER_LENGTH, MAX_THREADNAME_LENGTH);
        return ThreadEvent.SIZEOF_THREAD_START_EVENT;
    }
    
    protected int parseThreadDeathEvent(byte[] buffer,int offset) {
        parseCommonFields(buffer,offset);
        return ThreadEvent.SIZEOF_THREAD_DEATH_EVENT;
    }    
    
    protected int parseThreadStateChangeEvent(byte[] buffer,int offset) {
        parseCommonFields(buffer,offset);
        threadStateMask = read32Bit(buffer, (offset+24)%BUFFER_LENGTH);
        return ThreadEvent.SIZEOF_THREAD_STATE_CHANGE_EVENT;
    }    
    
    public ThreadEvent() {
    }
    
    public ThreadEvent(ThreadEvent other)
    {
        this.type = other.type;
        this.threadId = other.threadId;
        this.timestampSeconds = other.timestampSeconds;
        this.timestampNanos = other.timestampNanos;
        this.threadStateMask = other.threadStateMask;
        this.threadName = other.threadName;
    }
    
    /**
     * 
     * @param target
     * @param buffer
     * @param offset
     * @return the number of bytes consumed from the buffer
     */
    public int parseBuffer(byte[] buffer,int offset) 
    {
        switch(buffer[offset]) 
        {
            case ThreadEvent.THREAD_START:
                return parseThreadStartEvent(buffer, offset);
            case ThreadEvent.THREAD_DEATH:
                return parseThreadDeathEvent(buffer, offset);
            case ThreadEvent.THREAD_STATE_CHANGE:      
                return parseThreadStateChangeEvent(buffer, offset);                
            default:
        }      
        throw new RuntimeException("Unhandled event type "+buffer[offset]);        
    }

    @Override
    public String toString()
    {
        String result = ",threadId="+threadId+",ts_seconds="+timestampSeconds+",ts_micros="+timestampNanos;         
        final String sType;
        switch(type) {
            case THREAD_START:
                sType="THREAD_START";
                result += ",thread_name="+threadName;
                break;
            case THREAD_DEATH:
                sType="THREAD_DEATH";
                break;
            case THREAD_STATE_CHANGE:
                sType="THREAD_STATE_CHANGE";
                result += ",state="+JVMTIThreadState.fromBitMask( threadStateMask )+" ("+threadStateMask+")";
                break;
            default:
                sType="<< UNKNOWN >>";
        }
        return "event_type="+sType+" ("+type+")"+result;
    }
    
    protected final int read16Bit(byte[] buffer,int offset) 
    {
        final int low = buffer[offset] & 0xff;
        final int hi = buffer[(offset+1)%BUFFER_LENGTH] & 0xff;
        return (hi<<8)|low;
    }

    protected final String readString(byte[] buffer,int startOffset,int maxLength) 
    {
        int zeroByteIndex=-1;
        for ( int idx = 0 ; idx < maxLength ;idx++ ) 
        {
            final int offset = (startOffset+idx) % BUFFER_LENGTH;
            if ( buffer[ offset ] == 0 ) {
                zeroByteIndex = offset;
                break;
            }
        }
        if ( zeroByteIndex == -1 ) {
            return null;
        }
        try {
        	return convert(buffer,startOffset,zeroByteIndex);
        } catch(CharacterCodingException e) {
        	throw new RuntimeException("Failed to parse string with max. length "+maxLength+" in file at offset "+Integer.toHexString(startOffset ) );
        }
    }
    
    private String convert(byte[] data,int offsetStart,int zeroByteIndex) throws CharacterCodingException
    {
        final int len;
        if ( zeroByteIndex >= offsetStart ) {
            len = zeroByteIndex - offsetStart;
        } else {
            len = BUFFER_LENGTH-offsetStart+zeroByteIndex;
        }
        byte[] tmp = new byte[len];
        for ( int i = 0 , ptr = offsetStart ; ptr != zeroByteIndex ; i++) 
        {
            tmp[i] = data[ ptr ];
            ptr = (ptr+1) % BUFFER_LENGTH;
        }
        final ByteBuffer uniBuf = ByteBuffer.wrap(tmp,0,len);  
        final CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();  
        return utf8Decoder.decode(uniBuf).toString();
    }

    protected final int read32Bit(byte[] buffer,int offset) 
    {
        final int loWord = read16Bit(buffer,offset) & 0xffff;
        final int hiWord = read16Bit(buffer,(offset+2)%BUFFER_LENGTH) & 0xffff;         
        return ((hiWord<<16)|loWord);
    }
    
    protected final long read64Bit(byte[] buffer,int offset) 
    {
        final long  loWord = read32Bit(buffer,offset);
        final long  hiWord = read32Bit(buffer,(offset+4)%BUFFER_LENGTH);         
        return ((hiWord<<32)|loWord) & 0xffffffff;
    }    

    public long getTimestampMicros()
    {
        return timestampNanos;
    }

    public long getTimestampSeconds()
    {
        return timestampSeconds;
    }

    public int getThreadId()
    {
        return threadId;
    }
    
    public HiResTimestamp getTimestamp() {
        return new HiResTimestamp(this.timestampSeconds,this.timestampNanos,false);
    }
    
    public boolean isSameMillisecond(long secondsSinceEpoch,long nanoseconds) {
    	if ( this.timestampSeconds != secondsSinceEpoch ) {
    		return false;
    	}
    	return (this.timestampNanos / 1000000) == (nanoseconds / 1000000);
    }
    
    public boolean isAfter(HiResTimestamp timestamp) 
    {
    	if ( this.timestampSeconds > timestamp.secondsSinceEpoch ) {
    		return true;
    	}
    	if ( this.timestampSeconds < timestamp.secondsSinceEpoch ) {
    		return false;
    	}    	
		return this.timestampNanos > timestamp.nanoseconds;
    }
    
    public boolean isBefore(HiResTimestamp timestamp) 
    {
        if ( this.timestampSeconds > timestamp.secondsSinceEpoch ) {
            return false;
        }
        if ( this.timestampSeconds < timestamp.secondsSinceEpoch ) {
            return true;
        }       
        return this.timestampNanos < timestamp.nanoseconds;
    }    
    
    public boolean isAfterOrAt(HiResTimestamp timestamp) 
    {
    	if ( this.timestampSeconds < timestamp.secondsSinceEpoch ) {
    		return false;
    	}    	    	
    	if ( this.timestampSeconds > timestamp.secondsSinceEpoch ) {
    		return true;
    	}
		return this.timestampNanos >= timestamp.nanoseconds;
    }    
    
    public HiResTimestamp getMillisecondTimestamp() {
		int millis = (int) (timestampNanos / 1000000.0);
		return new HiResTimestamp( timestampSeconds , millis*1000000 , true );
    }    
}