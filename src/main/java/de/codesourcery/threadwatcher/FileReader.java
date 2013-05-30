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
    
    private static final int BUFFER_LENGTH = ThreadEvent.MAX_RECORD_SIZE * 1000;
    
    private final byte[] buffer = new byte[ BUFFER_LENGTH ];
    
    private final File file;    
    private BufferedInputStream in;
    
    private int bytesInBuffer;
    private int bytesConsumed;
    private int readPtr;
    private int writePtr;
    
    private final ThreadEvent event1=new ThreadEvent();
    private final ThreadEvent event2=new ThreadEvent();    
    
    // data discovered during file scan
    private final HiResInterval interval;
    private final Map<Integer,String> threadNamesByID;
    private final Map<HiResTimestamp,Long> fileOffsetsByMilliseconds;
    private final Map<Integer,HiResInterval> threadLifetimes;
    
    public static void main(String[] args) throws IOException
    {
        FileReader reader = new FileReader(new File( "/tmp/threadwatcher.out"));
    }
    
    public FileReader(File file) throws IOException 
    {
        if (file == null) {
            throw new IllegalArgumentException("file must not be NULL.");
        }
        this.file = file;
        final FileScanner visitor = new FileScanner();
        visit( visitor );        
        this.interval = visitor.getInterval();
        this.threadNamesByID = Collections.unmodifiableMap( visitor.threadNamesByID );
        this.fileOffsetsByMilliseconds = Collections.unmodifiableMap( visitor.fileOffsetsByMilliseconds );
        
        final Map<Integer,HiResInterval> tmp = new HashMap<>();
        for ( Entry<Integer, HiResTimestamp> entry : visitor.threadStartTimes.entrySet() ) {
        	final HiResTimestamp end = visitor.threadDeathTimes.get( entry.getKey() );
        	if ( end != null ) {
        		tmp.put( entry.getKey() , new HiResInterval( entry.getValue() , end ) );
        	}
        }
        this.threadLifetimes = Collections.unmodifiableMap( tmp );
        
        System.out.println("Time interval: "+getInterval());
        System.out.println("Threads: \n"+getThreadNamesByID() );
//        System.out.println("Offsets: "+StringUtils.join( fileOffsetsByMilliseconds.entrySet() , "\n"));
        System.out.println("Lifetimes: \n"+StringUtils.join(threadLifetimes.entrySet(),"\n" ) );        
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
        		threadStartTimes.put( event.threadId , event.getTimestamp() );
        	} else if ( event.type == ThreadEvent.THREAD_DEATH ) {
        		threadDeathTimes.put( event.threadId , event.getTimestamp() );
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
        public abstract boolean visit(ThreadEvent event);
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
        return interval;
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
        
        int records = 0;
        do 
        {
            if ( ! readOneEvent( event1 ) ) {
                break;
            }
            records++;
            visitor.visit( event1 );
        } while ( true );
        
       System.out.println("Records: "+records);
    }
    
    public void visit(FileVisitor visitor,HiResTimestamp start,HiResTimestamp end) throws IOException 
    {
        reset();
        
        readFileHeader();
        
        Long startOffset = fileOffsetsByMilliseconds.get( start.truncateToMilliseconds() );
        int delta = startOffset == null ? 0 : (int) (startOffset - FILE_HEADER_LITTLE_ENDIAN.length);
        		
        if ( delta > 0 ) {
        	in.skip( delta );
        }
        
        do 
        {
            if ( ! readOneEvent( event1 ) ) {
                break;
            }
            if ( event1.isAfterMillis( end ) ) {
            	break;
            }
            visitor.visit( event1 );
        } while ( true );
        
    }    
    
    private boolean readOneEvent(ThreadEvent toPopulate) throws IOException 
    {
        if ( spaceInBuffer() >= ThreadEvent.MAX_RECORD_SIZE ) {
            fillBuffer();
        }
        
        if ( bytesInBuffer < ThreadEvent.MIN_RECORD_SIZE ) 
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
        
        int records = 0;
        do 
        {
            if ( ! readOneEvent( next ) ) 
            {
                records++;
                visitor.visit(current,offset,false);
                break;
            }   
            
            records++;            
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
        System.out.println("Records: "+records);        
    }    
    
    private void consumed(int numberOfBytes) 
    {
        bytesInBuffer -= numberOfBytes;
        if ( bytesInBuffer < 0 ) {
            throw new IllegalStateException("Consumed more bytes than are in buffer?");
        }
    	bytesConsumed += numberOfBytes;
        readPtr = (readPtr+numberOfBytes) % BUFFER_LENGTH;
    }
    
    private int spaceInBuffer() 
    {
        if ( writePtr >= readPtr ) {
            return BUFFER_LENGTH-writePtr+readPtr;
        }
        return readPtr - writePtr;
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
        int bytesToRead = BUFFER_LENGTH - writePtr ;
        int read1 = in.read( buffer , writePtr , bytesToRead );
        if ( read1 >= 0 ) 
        {
            writePtr = (writePtr+read1) % BUFFER_LENGTH;
            bytesInBuffer+= read1;
        } 
        
        if ( read1 < bytesToRead ) 
        {
            return read1 == -1 ? false : true;
        }
      
        int read2 = in.read( buffer , 0 , readPtr );
        if ( read2 >= 0 ) {
            writePtr = (writePtr+read2) % BUFFER_LENGTH;            
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
            writePtr = (writePtr+read1) % BUFFER_LENGTH;
            bytesInBuffer+= read1;
            return true;
        } 
        return false;
    }    
}
