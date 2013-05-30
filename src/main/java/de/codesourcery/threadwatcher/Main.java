package de.codesourcery.threadwatcher;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.threadwatcher.FileReader.FileVisitor;

public class Main 
{
	private static final int BAR_HEIGHT = 20;
	
	private static final int Y_OFFSET = 15;
	private static final int X_OFFSET = 10;
	
	public static enum Resolution 
	{
		ONE_MILLISECOND(1) {

			@Override
			public Resolution nextHigherResolution() {
				return ONE_MILLISECOND;
			}

			@Override
			public Resolution nextLowerResolution() {
				return ONE_SECOND;
			}
		},
		ONE_SECOND(1000) {

			@Override
			public Resolution nextHigherResolution() {
				return ONE_MILLISECOND;
			}

			@Override
			public Resolution nextLowerResolution() {
				return ONE_MINUTE;
			}},
		ONE_MINUTE(60*1000) {

			@Override
			public Resolution nextHigherResolution() {
				return ONE_SECOND;
			}

			@Override
			public Resolution nextLowerResolution() {
				return ONE_MINUTE;
			}};
		
		private final int milliseconds;

		private Resolution(int milliseconds) {
			this.milliseconds = milliseconds;
		}
		
		public int getMilliseconds() {
			return milliseconds;
		}
		
		public abstract Resolution nextHigherResolution();
		public abstract Resolution nextLowerResolution();
	}
	
    public static void main(String[] args) throws IOException
    {
        new Main().run(new File("/tmp/threadwatcher.out" ) );
    }

    public void run(File file) throws IOException
    {
        final JFrame frame = new JFrame("Thread-Watcher V0.0");
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
        final RenderPanel panel = new RenderPanel(new FileReader(file) , Resolution.ONE_SECOND);
        panel.setPreferredSize(new Dimension(640,480 ) );
        frame.add( panel );
        frame.pack();
        frame.setVisible( true );
        
        final KeyAdapter keyListener = new KeyAdapter() 
        {
        	@Override
        	public void keyTyped(KeyEvent e) 
        	{
        		System.out.println("Typed: "+e.getKeyChar());
        		if ( e.getKeyChar() == 'd' ) {
        			panel.advanceByResolution();
        		} else if ( e.getKeyChar() == 'a' ) {
        			panel.goBackByResolution();
        		} else if ( e.getKeyChar() == 'w' ) {
        			panel.nextHigherResolution();
        		} else if ( e.getKeyChar() == 'a' ) {
        			panel.nextLowerResolution();
        		}
        	}
		};
		frame.addKeyListener( keyListener );
    }
    
    protected static final class RenderPanel extends JPanel 
    {
        private FileReader reader;
        private HiResInterval interval;
        private Resolution resolution;
        
        public RenderPanel(FileReader reader,Resolution resolution) 
        {
            this.reader = reader;
            this.resolution = resolution;
            recalcInterval(reader.getInterval().start);
        }
        
        private void recalcInterval(HiResTimestamp start) {
            interval = new HiResInterval( start , start.plusMilliseconds( resolution.getMilliseconds() ) );
        }
        
        public void nextLowerResolution() {
        	resolution=resolution.nextLowerResolution();
        	recalcInterval(this.interval.start);
        	repaint();
		}

		public void nextHigherResolution() {
			resolution=resolution.nextHigherResolution();
			recalcInterval(this.interval.start);
			repaint();
		}

		public void advanceByResolution() 
        {
        	interval = interval.rollByMilliseconds( resolution.getMilliseconds() );
        	repaint();
        }
        
        public void goBackByResolution() 
        {
        	interval = interval.rollByMilliseconds( -resolution.getMilliseconds() );
        	repaint();
        }        
        
        public Resolution getResolution() {
			return resolution;
		}
        
        public void setResolution(Resolution resolution) {
			this.resolution = resolution;
		}
        
        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            
            g.setColor(Color.BLACK);
            g.drawString( interval.start+" - "+interval.end+" / "+resolution , 5,10 );
            
            try 
            {
				reader.visit(new RenderingVisitor((Graphics2D) g),interval.start , interval.end );
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        
        protected final class RenderingVisitor extends FileVisitor 
        {
        	private final Graphics2D graphics;
        	private final double scaleX;
        	private final Map<Integer,Integer> threadYOffsetMap=new HashMap<>();
        	private final int x1;
        	
        	public RenderingVisitor(Graphics2D graphics) 
        	{
        		this.graphics  = graphics;
        		this.scaleX = (getWidth()-X_OFFSET) / interval.getDurationInMilliseconds();
        		
        		System.out.println("Displaying interval: "+interval);
        		final Map<Integer, String> threadNamesByID = reader.getThreadNamesByID();
            	final List<Integer> threadIds = new ArrayList<>( reader.getAliveThreadsInInterval( interval ) );
            	System.out.println("Threads in interval: "+interval);
            	
            	Collections.sort( threadIds , new Comparator<Integer>() {

					@Override
					public int compare(Integer o1, Integer o2) 
					{
						return threadNamesByID.get(o1).compareTo(threadNamesByID.get(o2));
					}
				});
            	// assign Y coordinates
            	int y = Y_OFFSET;
            	for ( int threadId : threadIds ) {
            		threadYOffsetMap.put( threadId , y );
            		y += BAR_HEIGHT;
            	}
            	x1 = (int) Math.round( interval.getDurationInMilliseconds() * scaleX );
        	}
        	
			@Override
			public boolean visit(ThreadEvent event) 
			{
				Integer y0 = threadYOffsetMap.get( event.threadId );
				if ( y0 == null ) {
					System.out.println("Unknown thread ID: "+event.threadId);
					return true;
				}
				final double durationMillis = HiResInterval.getDurationInMilliseconds( interval.start , event.getTimestamp() );
				int x0 = X_OFFSET + (int) Math.round( durationMillis*scaleX);
				
				final int state = event.threadStateMask;
				Color color=Color.BLACK;
				if ( JVMTIThreadState.ALIVE.isSet( state ) ) 
				{
					if ( JVMTIThreadState.SUSPENDED.isSet( state ) ) 
					{
						color = Color.GRAY;
					} else if ( JVMTIThreadState.WAITING.isSet( state ) ) 
					{
						color = Color.YELLOW;
					} 
					else if ( JVMTIThreadState.BLOCKED_ON_MONITOR_ENTER.isSet( state ) ) 
					{
						color = Color.RED;
					} else if ( JVMTIThreadState.RUNNABLE.isSet( state ) ) {
						color = Color.GREEN;
					}
				} 

				graphics.setColor( color );
				graphics.fillRect( x0 ,y0 , x1 - x0 , BAR_HEIGHT );
				return true;
			}
        }
    }
}
