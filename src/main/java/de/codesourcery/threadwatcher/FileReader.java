package de.codesourcery.threadwatcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileReader
{
    private static final byte[] FILE_HEADER_BIG_ENDIAN = { (byte) 0xde , (byte) 0xad, (byte) 0xbe , (byte) 0xef };
    private static final byte[] FILE_HEADER_LITTLE_ENDIAN = { (byte) 0xef , (byte) 0xbe, (byte) 0xad , (byte) 0xde };    
    
    private static final int BUFFER_LENGTH = ThreadEvent.MAX_RECORD_SIZE * 1000;
    
    private final byte[] buffer = new byte[ BUFFER_LENGTH ];
    
    private final File file;    
    private BufferedInputStream in;
    
    private int bytesInBuffer;
    private int readPtr;
    private int writePtr;
    
    private final ThreadEvent event=new ThreadEvent();
    
    public static void main(String[] args) throws IOException
    {
        final FileVisitor visitor = new FileVisitor() {
            
            @Override
            public boolean visit(ThreadEvent event)
            {
                System.out.println( event );
                return true;
            }
        };
        new FileReader(new File( "/tmp/threadwatcher.out")).visit( visitor );
    }
    

    
    public FileReader(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file must not be NULL.");
        }
        this.file = file;
    }
    
    public static abstract class FileVisitor 
    {
        public abstract boolean visit(ThreadEvent event);
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

    public void visit(FileVisitor visitor) throws IOException 
    {
        reset();
        
        readFileHeader();
        
        int records = 0;
        do 
        {
            if ( spaceInBuffer() >= ThreadEvent.MAX_RECORD_SIZE ) {
                fillBuffer();
            }
            
            if ( bytesInBuffer < ThreadEvent.MIN_RECORD_SIZE ) 
            {
                break;
            }
            
            final int bytesConsumed = event.parseBuffer( buffer , readPtr );
            
            records++;
            consumed(bytesConsumed);
            visitor.visit( event );
        } while ( true );
        
       System.out.println("Records: "+records);
    }
    
    private void consumed(int numberOfBytes) 
    {
        bytesInBuffer -= numberOfBytes;
        if ( bytesInBuffer < 0 ) {
            throw new IllegalStateException("Consumed more bytes than are in buffer?");
        }
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
