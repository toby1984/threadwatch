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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import de.codesourcery.threadwatcher.ui.HorizontalSelectionHelper;
import de.codesourcery.threadwatcher.ui.HorizontalSelectionHelper.DraggedMarker;
import de.codesourcery.threadwatcher.ui.IntervalPanel;
import de.codesourcery.threadwatcher.ui.StatisticsPanel;
import de.codesourcery.threadwatcher.ui.ThreadPanel;

public class Main 
{
    private FileReader fileReader;
    private IntervalPanel viewIntervalPanel;
    private IntervalPanel selectionIntervalPanel;
    
    private ThreadPanel chartPanel;
    private StatisticsPanel statisticsPanel;
    
    protected static enum SelectionType {
        VIEW_INTERVAL,
        INFO_INTERVAL;
    }
    
    private SelectionType activeSelectionType = null;    
    
    private final HorizontalSelectionHelper<HiResInterval> viewIntervalChooser = new HorizontalSelectionHelper<HiResInterval>(Color.BLUE) {

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
                chartPanel.setInterval( xmin , windowDurationMillis );
                viewIntervalPanel.updateTextFields( chartPanel.getInterval() );
            }
        }
        
        @Override
        protected int getYOffset() 
        {
        	return chartPanel.getYOffset();
        }

        @Override
        public int getMinX() { return chartPanel.getXOffset(); }

        @Override
        public int getMaxX() { return chartPanel.getCanvasMaxX(); }

        @Override
        protected HiResInterval getLastSelectionModelObject()
        {
            return selection;
        }

        @Override
        protected void selectionCleared()
        {
        }
    };
    
    private final HorizontalSelectionHelper<HiResInterval> infoIntervalChooser = new HorizontalSelectionHelper<HiResInterval>(Color.RED) {

        private HiResInterval selection;
        
        @Override
        protected void selectionFinished(int start, int end)
        {
            HiResTimestamp xmin = chartPanel.viewToModel( start );
            HiResTimestamp xmax = chartPanel.viewToModel( end );
            
            HiResInterval tmp = new HiResInterval( xmin , xmax );
            if ( tmp.getDurationInMilliseconds() >= 1.0 ) 
            {
                selection = tmp;
                chartPanel.repaint();
                try {
                    statisticsPanel.setInterval( tmp );
                    selectionIntervalPanel.updateTextFields( tmp );
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        @Override
        protected int getYOffset() 
        {
        	return chartPanel.getYOffset();
        }        
        
        public HorizontalSelectionHelper.SelectedInterval setLastSelection(HorizontalSelectionHelper.SelectedInterval interval) 
        {
            super.setLastSelection( interval );
            
            if ( interval == null ) {
                selection = null;
            } 
            else 
            {
                int min = Math.min( interval.xMin , interval.xMax );
                int max = Math.max( interval.xMin , interval.xMax );
                
                HiResTimestamp tmin = chartPanel.viewToModel( min );
                HiResTimestamp tmax = chartPanel.viewToModel( max );                
                HiResInterval tmp = new HiResInterval( tmin , tmax );
                if ( tmp.getDurationInMilliseconds() >= 1.0 ) 
                {
                    selection = tmp;
                } else {
                    selection = null;
                }
            }
            
            try 
            {
                statisticsPanel.setInterval( this.selection );
                selectionIntervalPanel.updateTextFields( this.selection );
            } catch (IOException e) {
                e.printStackTrace();
            }             
            return interval;
        }
        
        @Override
        public int getMinX() { return chartPanel.getXOffset(); }

        @Override
        public int getMaxX() { return chartPanel.getCanvasMaxX(); }

        @Override
        protected  HiResInterval getLastSelectionModelObject()
        {
            return selection;
        }

        @Override
        protected void selectionCleared()
        {
            setLastSelection( null );
            chartPanel.repaint();
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
                if ( infoIntervalChooser.isCloseToLastSelectionStart( e.getPoint() ) ) 
                {
                    setSelectionCursor();
                    infoIntervalChooser.setDraggedMarker(DraggedMarker.START );
                } 
                else if ( infoIntervalChooser.isCloseToLastSelectionEnd( e.getPoint() ) ) 
                {
                    setSelectionCursor();   
                    infoIntervalChooser.setDraggedMarker(DraggedMarker.END );
                } 
                else 
                {
                    infoIntervalChooser.setDraggedMarker( DraggedMarker.NONE );                    
                    activeSelectionType = SelectionType.VIEW_INTERVAL;
                }
            } 
            else if ( e.getButton() == MouseEvent.BUTTON3 ) 
            {
                activeSelectionType = SelectionType.INFO_INTERVAL;
            } 
        }
        
        public void mouseReleased(java.awt.event.MouseEvent e) 
        {
            if ( e.getButton() == MouseEvent.BUTTON1 || e.getButton() == MouseEvent.BUTTON3 ) 
            {
                if ( infoIntervalChooser.getDraggedMarker() != DraggedMarker.NONE) 
                {
                    setDefaultCursor();
                    infoIntervalChooser.setDraggedMarker( DraggedMarker.NONE );
                    return;
                }
                
                if ( activeSelectionType == SelectionType.INFO_INTERVAL) 
                {
                    infoIntervalChooser.stopSelecting( e.getPoint() , chartPanel.getGraphics() , chartPanel.getCanvasHeight() );
                    activeSelectionType = null;
                } else if ( activeSelectionType == SelectionType.VIEW_INTERVAL) {
                    viewIntervalChooser.stopSelecting( e.getPoint() , chartPanel.getGraphics() , chartPanel.getCanvasHeight() );
                    activeSelectionType = null;
                }
            } 
        }       
        
        protected void setDefaultCursor() {
            chartPanel.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );            
        }
        
        protected void setSelectionCursor() 
        {
            chartPanel.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
        }        
        
        public void mouseMoved(java.awt.event.MouseEvent e) 
        {
            if ( infoIntervalChooser.getDraggedMarker() == DraggedMarker.NONE && 
                 infoIntervalChooser.getDragMarkerForPoint( e.getPoint() ) != DraggedMarker.NONE )
            {
                setSelectionCursor();
            } else {
                setDefaultCursor();
            }
        }
        
        public void mouseDragged(java.awt.event.MouseEvent e) 
        {
        	
            switch ( infoIntervalChooser.getDraggedMarker() )
            {
                case START:
                	if ( infoIntervalChooser.isValid( e.getPoint() ) ) {
                		infoIntervalChooser.setLastSelection(infoIntervalChooser.getLastSelection().withMinX( e.getPoint().x ) );
                		chartPanel.repaint();
                	}
                    return;
                case END:
                	if ( infoIntervalChooser.isValid( e.getPoint() ) ) {
                		infoIntervalChooser.setLastSelection(infoIntervalChooser.getLastSelection().withMaxX( e.getPoint().x ) );
                		chartPanel.repaint();
                	}
                    return;
                default:
                   // $$FALL-THROUGH $$
            }
            
            if ( activeSelectionType != null ) 
            {
                switch (activeSelectionType)
                {
                    case INFO_INTERVAL:
                        infoIntervalChooser.updateSelection( e.getPoint() , chartPanel.getGraphics(), chartPanel.getCanvasHeight() );                        
                        break;
                    case VIEW_INTERVAL:
                        viewIntervalChooser.updateSelection( e.getPoint() , chartPanel.getGraphics(), chartPanel.getCanvasHeight() );
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
                            infoIntervalChooser.clearSelection( chartPanel.getGraphics() , chartPanel.getCanvasHeight() );
                            activeSelectionType = null;
                            break;
                        case VIEW_INTERVAL:
                            viewIntervalChooser.clearSelection( chartPanel.getGraphics() , chartPanel.getCanvasHeight() );
                            activeSelectionType = null;
                            break;
                        default:
                            break;
                    }                    
                } 
                else 
                {
                    infoIntervalChooser.clearSelection( chartPanel.getGraphics() , chartPanel.getCanvasHeight() );                    
                }
            }
        }
        
        @Override
        public void keyTyped(KeyEvent e) 
        {
            if ( e.getKeyChar() == 'd' ) 
            {
                chartPanel.stepForward();
                viewIntervalPanel.updateTextFields( chartPanel.getInterval());
                viewIntervalPanel.repaint();
            } else if ( e.getKeyChar() == 'a' ) {
                chartPanel.stepBackward();
                viewIntervalPanel.updateTextFields( chartPanel.getInterval());
            } else if ( e.getKeyChar() == 's' ) 
            {
                long newIntervalLengthMillis = (long) (chartPanel.getIntervalLengthMillis()*2.0);
                if ( newIntervalLengthMillis > 0 ) {
                    chartPanel.setIntervalLength( newIntervalLengthMillis );
                    viewIntervalPanel.updateTextFields( chartPanel.getInterval() );
                }
            } 
            else if ( e.getKeyChar() == 'w' ) 
            {
                long newIntervalLengthMillis = (long) (chartPanel.getIntervalLengthMillis()/2.0);
                if ( newIntervalLengthMillis > 0 ) {
                    chartPanel.setIntervalLength( newIntervalLengthMillis);
                    viewIntervalPanel.updateTextFields( chartPanel.getInterval() );
                }                   
            }
        }
    };    
    
    public static void main(String[] args) throws IOException
    {
    	final File file;
    	if ( args.length != 1 ) {
    		System.err.println("Usage: <event log file>");
    		file = new File( "/tmp/threadwatcher.out");    		
    		System.err.println("Trying to use default file "+file.getAbsolutePath()); 
    	} else {
    		file = new File( args[0] );
    	}
    		
    	if ( ! file.exists() || ! file.isFile() || ! file.canRead() ) {
    		throw new IOException("File "+file.getAbsolutePath()+" is not accessible / not a reglar file");
    	}
    	new Main().run( file );
    }
    
    private static void setBackgroundColor(Component c) {
    	c.setBackground( Color.WHITE );
    }
    public void run(File file) throws IOException
    {
        fileReader = new FileReader(file);
        final HiResInterval interval = fileReader.getInterval();
        
        // setup top-level interval panel
        viewIntervalPanel = new IntervalPanel(interval);        
        setBackgroundColor( viewIntervalPanel );
        viewIntervalPanel.addKeyListener( keyListener );
        viewIntervalPanel.setFocusable( true );
        
        // setup bottom-level interval panel
        selectionIntervalPanel = new IntervalPanel(null); 
        setBackgroundColor( selectionIntervalPanel );
        selectionIntervalPanel.addKeyListener( keyListener );
        selectionIntervalPanel.setFocusable( true );
        
        // setup chart panel
		chartPanel = new ThreadPanel(fileReader , infoIntervalChooser , interval.start , (long) Math.ceil( interval.getDurationInMilliseconds() )+3000 );
        chartPanel.addMouseListener( mouseListener );
        chartPanel.setFocusable( true );
        chartPanel.setPreferredSize( new Dimension(640,480 ) );
        chartPanel.addMouseMotionListener( mouseListener );
        chartPanel.addKeyListener( keyListener );
        
        // setup main frame
        final JFrame frame = new JFrame("Thread-Watcher V1.0");
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new GridBagLayout());
        
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.HORIZONTAL;
        cnstrs.gridwidth=GridBagConstraints.REMAINDER;
        cnstrs.gridheight=1;
        cnstrs.gridx=0;
        cnstrs.gridy=0;        
        cnstrs.weightx=1.0;
        cnstrs.weighty=0;
        frame.getContentPane().add( viewIntervalPanel , cnstrs );
        
		// setup bottom panel
        final JPanel bottom = new JPanel();
        bottom.setLayout(new GridBagLayout());
        
        cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.HORIZONTAL;
        cnstrs.gridwidth=GridBagConstraints.REMAINDER;
        cnstrs.gridheight=1;
        cnstrs.gridx=0;
        cnstrs.gridy=0;        
        cnstrs.weightx=1.0;
        cnstrs.weighty=0;        
        
        bottom.add( selectionIntervalPanel , cnstrs );
        
        statisticsPanel = new StatisticsPanel(fileReader);
        statisticsPanel.setFocusable(true);
        statisticsPanel.setBackground(Color.WHITE);
        statisticsPanel.addKeyListener( keyListener );            
        
        cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.BOTH;
        cnstrs.gridwidth=GridBagConstraints.REMAINDER;
        cnstrs.gridheight=GridBagConstraints.REMAINDER;
        cnstrs.gridx=0;
        cnstrs.gridy=1;        
        cnstrs.weightx=1.0;
        cnstrs.weighty=1.0;        
        
        bottom.add( statisticsPanel , cnstrs );

        // add split pane
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT , new JScrollPane( chartPanel ) , bottom );
        
        cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.BOTH;
        cnstrs.gridwidth=GridBagConstraints.REMAINDER;
        cnstrs.gridheight=GridBagConstraints.REMAINDER;
        cnstrs.gridx=0;
        cnstrs.gridy=1;        
        cnstrs.weightx=1.0;
        cnstrs.weighty=1;          
        frame.getContentPane().add( splitPane , cnstrs );

        frame.pack();
        frame.setVisible( true );
    }   
}