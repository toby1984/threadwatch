package de.codesourcery.threadwatcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class FileReader
{
    private static final byte[] FILE_HEADER_LITTLE_ENDIAN = { (byte) 0xef , (byte) 0xbe, (byte) 0xad , (byte) 0xde };    
    
    private static final boolean DEBUG = false;
    
    private final byte[] buffer = new byte[ ThreadEvent.BUFFER_LENGTH ];
    
    private final File file;    
    private BufferedInputStream in;
    
    private int bytesInBuffer;
    private int bytesConsumed;
    private int readPtr;
    private int writePtr;
    
    private final ThreadEvent event1=new ThreadEvent();
    private final ThreadEvent event2=new ThreadEvent();    
    
    // data discovered during file scan
    private final HiResInterval dataInterval;
    private final Map<Integer,String> threadNamesByID;
    private final Map<HiResTimestamp,Long> fileOffsetsByMilliseconds;
    private final Map<Integer,HiResInterval> threadLifetimes;
    
    public static void main(String[] args) throws IOException
    {
        FileReader reader = new FileReader(new File( "/tmp/threadwatcher.out"));
        System.out.println(reader.threadNamesByID);
        HiResInterval start = new HiResInterval( reader.getInterval().start , reader.getInterval().start.plusSeconds( 1 ) ); 
        for (int i = 0; i < 5 ; i++ ) 
        {
        	System.out.println("Threads in interval "+start);
        	System.out.println(reader.getAliveThreadsInInterval( start) );
        	start = start.rollBySeconds(1);
        }
    }
    
    public FileReader(File file) throws IOException 
    {
        if (file == null) {
            throw new IllegalArgumentException("file must not be NULL.");
        }
        this.file = file;
        final FileScanner visitor = new FileScanner();
        visit( visitor );        
        this.dataInterval = visitor.getInterval();
        this.threadNamesByID = Collections.unmodifiableMap( visitor.threadNamesByID );
        this.fileOffsetsByMilliseconds = Collections.unmodifiableMap( visitor.fileOffsetsByMilliseconds );
        
        final Map<Integer,HiResInterval> tmp = new HashMap<>();
        for ( Entry<Integer, HiResTimestamp> entry : visitor.threadStartTimes.entrySet() ) {
        	HiResTimestamp end = visitor.threadDeathTimes.get( entry.getKey() );
        	if ( end == null ) {
        	    end = visitor.lastEvent;
        	}
       		tmp.put( entry.getKey() , new HiResInterval( entry.getValue() , end ) );
        }
        this.threadLifetimes = Collections.unmodifiableMap( tmp );
        
        if ( DEBUG ) {
            System.out.println("Time interval: "+getInterval());
            System.out.println("Threads: \n"+getThreadNamesByID() );
            System.out.println("Offsets: "+StringUtils.join( fileOffsetsByMilliseconds.entrySet() , "\n"));
            System.out.println("Lifetimes: \n"+StringUtils.join(threadLifetimes.entrySet(),"\n" ) );
        }
    }
    
    public Map<Integer, HiResInterval> getThreadLifetimes() {
		return threadLifetimes;
	}
    
    public HiResInterval getThreadLifetime(int threadId) 
    {
    	HiResInterval result = threadLifetimes.get( threadId );
    	if ( result == null ) {
    		throw new IllegalArgumentException("No thread with ID "+threadId);
    	}
    	return result;
    }
    
    public boolean isAliveIn(int threadId , HiResInterval interval) {
    	return getThreadLifetime( threadId ).overlaps( interval );
    }
    
    public Map<Integer, String> getThreadNamesByID() {
		return threadNamesByID;
	}
    
    protected final class FileScanner extends LookAheadFileVisitor
    {
        public HiResTimestamp firstEvent;
        public HiResTimestamp lastEvent;
        
        public final Map<Integer,String> threadNamesByID = new HashMap<>();
        public final Map<HiResTimestamp,Long> fileOffsetsByMilliseconds = new HashMap<>();
        public final Map<Integer,HiResTimestamp> threadStartTimes=new HashMap<>();
        public final Map<Integer,HiResTimestamp> threadDeathTimes=new HashMap<>();
        
        private long previousTimestampSeconds;
        private long previousTimestampNanos;
        
        @Override
        public boolean visit(ThreadEvent event,long fileOffset,boolean hasNext)
        {
        	if ( event.type == ThreadEvent.THREAD_START ) 
        	{
        		threadNamesByID.put( event.threadId , event.threadName );
        		HiResTimestamp existing = threadStartTimes.put( event.threadId , event.getTimestamp() );
        		if ( existing != null ) {
        			throw new RuntimeException("Thread #"+event.threadId+" started more than once?");
        		}
        	} else if ( event.type == ThreadEvent.THREAD_DEATH ) {
        		HiResTimestamp existing = threadDeathTimes.put( event.threadId , event.getTimestamp() );
        		if ( existing != null ) {
        			throw new RuntimeException("Thread #"+event.threadId+" died more than once?");
        		}
        	}
        	
            if ( firstEvent == null ) 
            {
                firstEvent = event.getTimestamp();
                previousTimestampSeconds = event.timestampSeconds;
                previousTimestampNanos = event.timestampNanos;
                fileOffsetsByMilliseconds.put( event.getMillisecondTimestamp() , fileOffset );
            } 
            else if ( ! event.isSameMillisecond( previousTimestampSeconds , previousTimestampNanos ) ) 
            {
                fileOffsetsByMilliseconds.put( event.getMillisecondTimestamp() , fileOffset );            	
            }
            
            if ( ! hasNext ) {
                lastEvent = event.getTimestamp();
            }
            return true;
        }
        
        public HiResInterval getInterval() 
        {
        	if ( firstEvent != null && lastEvent != null ) 
        	{
        		return new HiResInterval( firstEvent , lastEvent );
        	}
        	return null;
        }
    }    
    
    public static abstract class FileVisitor 
    {
        public abstract void visit(ThreadEvent event);
    }  
    
    public static abstract class LookAheadFileVisitor 
    {
        public abstract boolean visit(ThreadEvent event,long fileoffset,boolean hasMore);
    }    
    
    private void reset() throws IOException 
    {
        if ( in != null ) {
            in.close();
            in = null;
        }
        
        in = new BufferedInputStream(new FileInputStream( file ) );
        
        bytesInBuffer = 0;
        readPtr = 0;
        writePtr = 0;     
    }
    
    public HiResInterval getInterval() 
    {
        return dataInterval;
    }
    
    public Set<Integer> getAliveThreadsInInterval(HiResInterval interval) 
    {
    	Set<Integer> result = new HashSet<>();
    	for ( Entry<Integer, HiResInterval> entry : threadLifetimes.entrySet() ) {
    		if ( interval.overlaps( entry.getValue() ) ) {
    			result.add( entry.getKey() );
    		} 
    	}
    	return result;
    }
    
    public void visit(FileVisitor visitor) throws IOException 
    {
        reset();
        
        readFileHeader();
        
        do 
        {
            if ( ! readOneEvent( event1 ) ) {
                break;
            }
            visitor.visit( event1 );
        } while ( true );
        
    }
    
    public void visit(FileVisitor visitor,HiResInterval interval,final Set<Integer> threadIds) throws IOException 
    {
        
        final HiResTimestamp start=interval.start;
        final HiResTimestamp end=interval.end;
        
        reset();
        
        readFileHeader();
        
        // for each requested thread determine its state at the start of the requested interval        
        final Map<Integer,Integer> initialThreadStatesByThread=new HashMap<>();

        do 
        {
            if ( ! readOneEvent( event1 ) ) {
                return;
            }
            
            if ( event1.isAfterOrAt( start ) ) {
                break;
            }
            
            switch( event1.type ) 
            {
                case ThreadEvent.THREAD_START:
                    if ( threadIds.contains( event1.threadId ) ) 
                    {
                        initialThreadStatesByThread.put( event1.threadId , JVMTIThreadState.RUNNABLE.getBitMask() );
                    }
                    break;
                case ThreadEvent.THREAD_DEATH:
                    initialThreadStatesByThread.remove( event1.threadId );
                    break;                   
                case ThreadEvent.THREAD_STATE_CHANGE:
                    if ( threadIds.contains( event1.threadId ) ) 
                    {
                        initialThreadStatesByThread.put( event1.threadId , event1.threadStateMask );
                    }                    
                    break;                    
                default:
                    throw new RuntimeException("Unhandled event type: "+event1.type);
            }
        } while ( true );            
        
        // create fake state-change events to simulate initial thread states
        event2.type = ThreadEvent.THREAD_STATE_CHANGE;
        event2.timestampSeconds = start.secondsSinceEpoch;
        event2.timestampNanos = start.nanoseconds;
        for ( Entry<Integer, Integer> entry: initialThreadStatesByThread.entrySet() ) 
        {
            event2.threadId = entry.getKey();
            event2.threadStateMask = entry.getValue();
            visitor.visit( event2 );
        }
        
        Set<Integer> died = new HashSet<>();
        do 
        {
            if ( event1.type == ThreadEvent.THREAD_DEATH ) {
                died.add( event1.threadId );
            }
            
            if ( event1.isAfter( end ) ) {
                break;
            }
            if ( threadIds.contains( event1.threadId ) ) 
            {
                visitor.visit( event1 );
            }
            if ( ! readOneEvent( event1 ) ) {
                break;
            }            
        } while ( true );
        
        // fake any missing "thread death" events if search interval extends
        // after the actual data interval
        if ( end.isAfter( dataInterval.end ) ) 
        {
            event2.type = ThreadEvent.THREAD_DEATH;
            event2.timestampSeconds = dataInterval.end.secondsSinceEpoch;
            event2.timestampNanos = dataInterval.end.nanoseconds;
            for ( Entry<Integer, Integer> entry: initialThreadStatesByThread.entrySet() ) 
            {
                if ( ! died.contains( entry.getKey() ) ) 
                {
                    event2.threadId = entry.getKey();
                    visitor.visit( event2 );
                }
            }            
        }
        
    }    
    
    private boolean readOneEvent(ThreadEvent toPopulate) throws IOException 
    {
        if ( bytesInBuffer <= ThreadEvent.MAX_RECORD_SIZE ) {
            fillBuffer();
        }
        
        if ( bytesInBuffer < ThreadEvent.MAX_RECORD_SIZE ) 
        {
            return false;
        }
        
        final int bytesConsumed = toPopulate.parseBuffer( buffer , readPtr );
        consumed(bytesConsumed);
        return true;
    }
    
    public void visit(LookAheadFileVisitor visitor) throws IOException 
    {
        reset();
        
        readFileHeader();
        
        ThreadEvent current = event1;
        ThreadEvent next = event2;    
        
        long offset = bytesConsumed;
        if ( ! readOneEvent( current ) ) {
            return;
        }
        
        do 
        {
            if ( ! readOneEvent( next ) ) 
            {
                visitor.visit(current,offset,false);
                break;
            }   
            
            visitor.visit( current , offset , true );
            offset += bytesConsumed;
            if ( current == event1 ) {
                current = event2;
                next = event1;
            } else {
                current = event1;
                next = event2;                
            }
            
        } while ( true );
    }    
    
    private void consumed(int numberOfBytes) 
    {
        bytesInBuffer -= numberOfBytes;
        if ( bytesInBuffer < 0 ) {
            throw new IllegalStateException("Consumed more bytes than are in buffer?");
        }
    	bytesConsumed += numberOfBytes;
        readPtr = (readPtr+numberOfBytes) % ThreadEvent.BUFFER_LENGTH;
    }
    
    private void readFileHeader() throws IOException
    {
        if ( bytesInBuffer < FILE_HEADER_LITTLE_ENDIAN.length ) {
            if ( ! fillBuffer() ) {
                throw new IOException("Premature EOF");
            }
            if ( bytesInBuffer < FILE_HEADER_LITTLE_ENDIAN.length ) 
            {
                throw new IOException("Truncated file header");
            }
        }
        
        for ( int i = 0 ; i < FILE_HEADER_LITTLE_ENDIAN.length ; i++ ) 
        {
            if ( buffer[readPtr+i] != FILE_HEADER_LITTLE_ENDIAN[i] ) 
            {
                throw new IOException("Invalid file header, file is truncated");
            }
        }
        consumed(FILE_HEADER_LITTLE_ENDIAN.length);
    }

    private boolean fillBuffer() throws IOException
    {
        if ( writePtr >= readPtr ) {
            return fillBuffer1();
        } 
        return fillBuffer2();
    }
    
    private boolean fillBuffer1() throws IOException 
    {
        int bytesToRead = ThreadEvent.BUFFER_LENGTH - writePtr ;
        int read1 = in.read( buffer , writePtr , bytesToRead );
        if ( read1 >= 0 ) 
        {
            writePtr = (writePtr+read1) % ThreadEvent.BUFFER_LENGTH;
            bytesInBuffer+= read1;
        } 
        
        if ( read1 < bytesToRead ) 
        {
            return read1 == -1 ? false : true;
        }
      
        int read2 = in.read( buffer , 0 , readPtr );
        if ( read2 >= 0 ) {
            writePtr = (writePtr+read2) % ThreadEvent.BUFFER_LENGTH;            
            bytesInBuffer+= read2;
        }
        return true;
    }
    
    private boolean fillBuffer2() throws IOException 
    {
        int bytesToRead = readPtr-writePtr ;
        int read1 = in.read( buffer , writePtr , bytesToRead );
        if ( read1 > 0 ) 
        {
            writePtr = (writePtr+read1) % ThreadEvent.BUFFER_LENGTH;
            bytesInBuffer+= read1;
            return true;
        } 
        return false;
    }    
}
