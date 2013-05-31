package de.codesourcery.threadwatcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import de.codesourcery.threadwatcher.ui.HorizontalSelectionHelper;
import de.codesourcery.threadwatcher.ui.StatisticsPanel;
import de.codesourcery.threadwatcher.ui.ThreadPanel;

public class Main 
{
    private FileReader fileReader;
    private ThreadPanel chartPanel;
    private StatisticsPanel statisticsPanel;
    
    protected static enum SelectionType {
        VIEW_INTERVAL,
        INFO_INTERVAL;
    }
    
    private SelectionType activeSelectionType = null;    
    
    private final HorizontalSelectionHelper<HiResInterval> viewIntervalChooser = new HorizontalSelectionHelper<HiResInterval>(Color.RED) {

        private HiResInterval selection; 
        
        @Override
        protected void selectionFinished(int start, int end)
        {
            HiResTimestamp xmin = chartPanel.viewToModel( start );
            HiResTimestamp xmax = chartPanel.viewToModel( end );

            final long windowDurationMillis = (long) new HiResInterval( xmin,xmax ).getDurationInMilliseconds();
            if ( windowDurationMillis > 0 ) 
            {
                selection = new HiResInterval( xmin , xmax );
                System.out.println("*************************");
                System.out.println("* NEW INTERVAL: "+selection+" ("+selection.getDurationInMilliseconds()+")");
                System.out.println("*************************");
                chartPanel.setInterval( xmin , windowDurationMillis );
            }
        }

        @Override
        protected int getMinX() { return chartPanel.getXOffset(); }

        @Override
        protected int getMaxX() { return chartPanel.getWidth(); }

        @Override
        protected HiResInterval getLastSelectionModelObject()
        {
            return selection;
        }
    };
    
    private final HorizontalSelectionHelper<HiResInterval> infoIntervalChooser = new HorizontalSelectionHelper<HiResInterval>(Color.YELLOW) {

        private HiResInterval selection;
        
        @Override
        protected void selectionFinished(int start, int end)
        {
            HiResTimestamp xmin = chartPanel.viewToModel( start );
            HiResTimestamp xmax = chartPanel.viewToModel( end );
            
            HiResInterval tmp = new HiResInterval( xmin , xmax );
            if ( tmp.getDurationInMilliseconds() >= 1.0 ) {
                selection = tmp;
                System.out.println("*************************");
                System.out.println("* NEW INFO INTERVAL: "+selection+" ("+selection.getDurationInMilliseconds()+")");
                System.out.println("*************************");
                chartPanel.repaint();
                try {
                    statisticsPanel.setInterval( tmp );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        protected int getMinX() { return chartPanel.getXOffset(); }

        @Override
        protected int getMaxX() { return chartPanel.getWidth(); }

        @Override
        protected  HiResInterval getLastSelectionModelObject()
        {
            return selection;
        }
    };    
    
    private final MouseAdapter mouseListener = new MouseAdapter() 
    {
        public void mousePressed(MouseEvent e) 
        {
            if ( activeSelectionType != null ) {
                return;
            }
            
            if ( e.getButton() == MouseEvent.BUTTON1 ) 
            {
                activeSelectionType = SelectionType.VIEW_INTERVAL;
            } else if ( e.getButton() == MouseEvent.BUTTON3 ) {
                activeSelectionType = SelectionType.INFO_INTERVAL;
            } 
        }
        public void mouseReleased(java.awt.event.MouseEvent e) 
        {
            if ( e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3 ) 
            {
                if ( activeSelectionType == SelectionType.INFO_INTERVAL) 
                {
                    infoIntervalChooser.stopSelecting( e.getPoint() , chartPanel.getGraphics() , chartPanel.getHeight() );
                    activeSelectionType = null;
                } else if ( activeSelectionType == SelectionType.VIEW_INTERVAL) {
                    viewIntervalChooser.stopSelecting( e.getPoint() , chartPanel.getGraphics() , chartPanel.getHeight() );
                    activeSelectionType = null;
                }
            } 
        }           

        public void mouseDragged(java.awt.event.MouseEvent e) 
        {
            if ( activeSelectionType != null ) 
            {
                switch (activeSelectionType)
                {
                    case INFO_INTERVAL:
                        infoIntervalChooser.updateSelection( e.getPoint() , chartPanel.getGraphics(), chartPanel.getHeight() );                        
                        break;
                    case VIEW_INTERVAL:
                        viewIntervalChooser.updateSelection( e.getPoint() , chartPanel.getGraphics(), chartPanel.getHeight() );
                        break;
                    default:
                        break;
                }
            }
        }
    };    
    
    final KeyAdapter keyListener = new KeyAdapter() 
    {
        @Override
        public void keyPressed(KeyEvent e)
        {
            if ( e.getKeyCode() == KeyEvent.VK_ESCAPE ) 
            {
                if ( activeSelectionType != null ) 
                {
                    switch (activeSelectionType)
                    {
                        case INFO_INTERVAL:
                            infoIntervalChooser.clearSelection( chartPanel.getGraphics() , chartPanel.getHeight() );
                            activeSelectionType = null;
                            break;
                        case VIEW_INTERVAL:
                            viewIntervalChooser.clearSelection( chartPanel.getGraphics() , chartPanel.getHeight() );
                            activeSelectionType = null;
                            break;
                        default:
                            break;
                    }                    
                }
            }
        }
        
        @Override
        public void keyTyped(KeyEvent e) 
        {
            System.out.println("Typed: "+e.getKeyChar());
            if ( e.getKeyChar() == 'd' ) 
            {
                chartPanel.stepForward();
            } else if ( e.getKeyChar() == 'a' ) {
                chartPanel.stepBackward();
            } else if ( e.getKeyChar() == 's' ) 
            {
                long newIntervalLengthMillis = (long) (chartPanel.getIntervalLengthMillis()*2.0);
                if ( newIntervalLengthMillis > 0 ) {
                    chartPanel.setIntervalLength( newIntervalLengthMillis);
                }
            } else if ( e.getKeyChar() == 'w' ) 
            {
                long newIntervalLengthMillis = (long) (chartPanel.getIntervalLengthMillis()/2.0);
                if ( newIntervalLengthMillis > 0 ) {
                    chartPanel.setIntervalLength( newIntervalLengthMillis);
                }                   
            }
        }
    };    
    
    public static void main(String[] args) throws IOException
    {
        new Main().run(new File("/tmp/threadwatcher.out" ) );
    }
    
    public void run(File file) throws IOException
    {
        fileReader = new FileReader(file);
        
        final JFrame frame = new JFrame("Thread-Watcher V0.0");
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
        
        chartPanel = new ThreadPanel(fileReader , infoIntervalChooser , fileReader.getInterval().start , 1000 );
        chartPanel.setPreferredSize(new Dimension(640,480 ) );
        frame.getContentPane().add( chartPanel );
        frame.pack();
        frame.setVisible( true );
        
        chartPanel.addMouseListener( mouseListener );
        chartPanel.addMouseMotionListener( mouseListener );
        
        chartPanel.setFocusable(true);
        chartPanel.addKeyListener( keyListener );

		// setup statistics frame
		JFrame statisticsFrame = new JFrame("Statistics");
		statisticsFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
        statisticsPanel = new StatisticsPanel(fileReader);
        statisticsPanel.setPreferredSize( new Dimension(400,100 ) );
        frame.getContentPane().add( statisticsPanel , BorderLayout.NORTH );
        
        statisticsPanel.setFocusable(true);
        statisticsPanel.addKeyListener( keyListener );     
        
		statisticsFrame.getContentPane().add( statisticsPanel );
		
		statisticsFrame.setLocation( frame.getLocation().x + frame.getWidth()+2 , frame.getLocation().y );
		statisticsFrame.pack();
        statisticsFrame.setVisible( true );		
    }
    
}