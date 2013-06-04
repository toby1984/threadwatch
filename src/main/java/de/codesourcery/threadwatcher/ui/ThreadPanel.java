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
package de.codesourcery.threadwatcher.ui;

import static de.codesourcery.threadwatcher.ui.UIConstants.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.Scrollable;

import de.codesourcery.threadwatcher.FileReader;
import de.codesourcery.threadwatcher.FileReader.FileVisitor;
import de.codesourcery.threadwatcher.HiResInterval;
import de.codesourcery.threadwatcher.HiResTimestamp;
import de.codesourcery.threadwatcher.ThreadEvent;
import de.codesourcery.threadwatcher.ui.HorizontalSelectionHelper.SelectedInterval;

public final class ThreadPanel extends JPanel implements Scrollable 
{
    private static final int LEGEND_HEIGHT = 20;
    private static final int RIGHT_BORDER = 20;
    
    private FileReader reader;
    private final HorizontalSelectionHelper<HiResInterval> intervalHelper;

    private HiResTimestamp intervalStart;
    private long intervalLengthInMillis;

    private int yOffset;
    private int xOffset;
	private int canvasHeight;
	private int canvasMaxX;
    
    private double scaleX;    
    
    private Dimension myPreferredSize = null;

    private final ComponentAdapter compAdaptor = new ComponentAdapter() 
    {
        @Override
        public void componentResized(ComponentEvent e)
        {
            super.componentResized(e);
            updateScaleX();                
        }
    };

    public ThreadPanel(FileReader reader,HorizontalSelectionHelper<HiResInterval> intervalHelper,HiResTimestamp intervalStart,long intervalLengthInMillis) 
    {
        this.reader = reader;
        this.intervalHelper = intervalHelper;

        this.intervalStart = intervalStart;        
        this.intervalLengthInMillis = intervalLengthInMillis;

        setBackground(Color.WHITE);
        addComponentListener(compAdaptor);
    }
    
    public void setFileReader(FileReader reader,HiResInterval interval) 
    {
    	this.reader = reader;
    	setInterval( interval.start , (long) Math.ceil( interval.getDurationInMilliseconds() ) );
    }

    @Override
    public void setSize(Dimension d)
    {
        super.setSize(d);
        updateScaleX();
    }

    @Override
    public void setSize(int width, int height)
    {
        super.setSize(width, height);
        updateScaleX();
    }

    public int getXOffset() {
        return xOffset;
    }

    public HiResTimestamp viewToModel(int x) 
    {
        int millis = (int) Math.round( ( x - xOffset ) / scaleX );
        return new HiResTimestamp(intervalStart.plusMilliseconds( millis ));
    }        

    public void setInterval(HiResTimestamp start,long durationInMillis) {
        this.intervalStart = start;
        this.intervalLengthInMillis = durationInMillis;
        updateScaleX();
        repaint();
    }

    public void setIntervalLength(long lengthInMillis) {
        this.intervalLengthInMillis = lengthInMillis;
        updateScaleX();
        repaint();            
    }

    public void shiftIntervalByMillis(long deltaMillis) 
    {
        intervalStart = intervalStart.plusMilliseconds( deltaMillis );
        repaint();
    }

    public long getIntervalLengthMillis() {
        return this.intervalLengthInMillis;
    }

    public void stepForward()
    {
        shiftIntervalByMillis( this.intervalLengthInMillis/4 );
    }       

    public void stepBackward()
    {
        shiftIntervalByMillis( -(this.intervalLengthInMillis/4) );
    }           

    public HiResInterval getInterval() 
    {
    	return  new HiResInterval( intervalStart , intervalStart.plusMilliseconds( intervalLengthInMillis ));
    }
    
    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        updateScaleX();

        final HiResInterval viewInterval = getInterval();

        Dimension newSize = null;
        try 
        {
            RenderingVisitor visitor = new RenderingVisitor( (Graphics2D) g );
            visitor.render( reader );
            newSize = visitor.getEstimatedSize();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        final SelectedInterval lastSelection = intervalHelper.getLastSelection();
        if ( lastSelection != null ) 
        {
            HiResInterval selectedInterval = intervalHelper.getLastSelectionModelObject();
            if ( viewInterval.contains( selectedInterval.start ) && viewInterval.containsEndInclusive( selectedInterval.end ) )
            {
                int xmin = modelToView( selectedInterval.start);
                int xmax = modelToView( selectedInterval.end);
                intervalHelper.paintSelection( g , xmin,xmax,getCanvasHeight() );
            }
        }
        if ( newSize != null && ! newSize.equals( getPreferredSize() ) ) 
        {
            setPreferredSize( newSize );
            myPreferredSize = newSize;
            revalidate();
        }
    }
    
    private int modelToView(HiResTimestamp ts) 
    {
        double millis = new HiResInterval(intervalStart,ts).getDurationInMilliseconds();
        return xOffset + (int) Math.round( millis * scaleX );
    }

    protected void updateScaleX() 
    {
        scaleX = (getWidth() - X_OFFSET - RIGHT_BORDER) / (double) intervalLengthInMillis;
    }
    
    public int getYOffset() {
    	return yOffset;
    }
    
    public int getCanvasHeight() {
    	return canvasHeight;
    }
    
    public int getCanvasMaxX() {
		return canvasMaxX;
	}

    protected final class RenderingVisitor extends FileVisitor 
    {
        private final Graphics2D graphics;
        private final Map<Integer,Integer> threadYOffsetMap=new HashMap<>();
        private final int x1;
        private final int BAR_HEIGHT;
        private final Set<Integer> aliveThreadIds;

        public RenderingVisitor(Graphics2D graphics) 
        {
            this.graphics  = graphics;
            HiResInterval interval =getInterval();
            updateScaleX();

            final Map<Integer, String> threadNamesByID = reader.getThreadNamesByID();
            aliveThreadIds = reader.getAliveThreadsInInterval( interval ) ;
            final List<Integer> threadIds = new ArrayList<>( aliveThreadIds);

            // sort ascending by thread ID first, then ascending by name
            Collections.sort( threadIds , new Comparator<Integer>() {

                @Override
                public int compare(Integer o1, Integer o2) 
                {
                    int result = o1.compareTo( o2 );
                    if ( result == 0 ) {
                        return threadNamesByID.get(o1).toLowerCase().compareTo(threadNamesByID.get(o2).toLowerCase());
                    }
                    return result;
                }
            });

            double longestNameWidth = 0;
            for ( int threadId : threadIds ) {
                final String threadName = threadNamesByID.get(threadId)+" ("+threadId+")";
                Rectangle2D stringBounds = graphics.getFontMetrics().getStringBounds( threadName , graphics);
                if ( stringBounds.getWidth() > longestNameWidth ) {
                    longestNameWidth = stringBounds.getWidth();
                }
            }

            xOffset = (int) Math.round( X_OFFSET+longestNameWidth*1.1 );
            yOffset = Y_OFFSET;

            Rectangle2D stringBounds = graphics.getFontMetrics().getStringBounds("XYZ", graphics);
            BAR_HEIGHT = (int) Math.ceil( stringBounds.getHeight()*1.5 );

            // assign Y coordinates
            graphics.setColor(LEGENDITEM_DEAD.color);
            int y = Y_OFFSET;
            canvasMaxX = getWidth() - RIGHT_BORDER;
            final int barWidth = getWidth() - xOffset - RIGHT_BORDER;
            for ( int threadId : threadIds ) 
            {
                final String threadName = threadNamesByID.get(threadId)+" ("+threadId+")";

                final Point p = LegendItem.centerTextVertically(threadName,0,y+(BAR_HEIGHT/4),BAR_HEIGHT,graphics);
                graphics.drawString( threadName, p.x , p.y );

                threadYOffsetMap.put( threadId , y );
                graphics.fillRect( xOffset , y , barWidth , BAR_HEIGHT );
                y += BAR_SPACING+BAR_HEIGHT;
            }
            
            if ( threadIds.isEmpty() ) {
            	canvasHeight = 0;
            } else {
            	canvasHeight = (BAR_SPACING+BAR_HEIGHT)*(threadIds.size()-1)+BAR_HEIGHT-1;
            }
            x1 = xOffset + (int) Math.round( intervalLengthInMillis * scaleX );
            renderLegend(true);
        }
        
        public Dimension getEstimatedSize() 
        {
            int height = Y_OFFSET;
            if ( ! aliveThreadIds.isEmpty() ) 
            {
                height += aliveThreadIds.size()*BAR_HEIGHT+(aliveThreadIds.size()-1)*BAR_SPACING;
            }
            final Dimension legendBounds = renderLegend(false);
            
            final int width  = (int) Math.max( getWidth() , legendBounds.width );
            height += legendBounds.height;
            return new Dimension( width , height );
        }

        private Dimension renderLegend(boolean draw) 
        {
            int x = 5;
            int y = 10;
            for (int i = 0; i < LEGEND_ITEMS.size(); i++) {
                LegendItem item = LEGEND_ITEMS.get(i);
                if ( draw ) {
                    item.render( x , y , graphics );
                }
                x += item.getBounds( graphics ).width;
                if ( x != 0 ) {
                    x+=5;
                }
            }
            return new Dimension(5+x , LEGEND_HEIGHT );
        }

        public void render(FileReader reader) throws IOException {

            updateScaleX();
            
            final HiResInterval visibleInterval = new HiResInterval(intervalStart , intervalStart.plusMilliseconds( intervalLengthInMillis ) );
            reader.visit( this , visibleInterval , aliveThreadIds );
        }

        @Override
        public void visit(ThreadEvent event) 
        {
            final int y0 = threadYOffsetMap.get( event.threadId );
            final double durationMillis = HiResInterval.getDurationInMilliseconds( intervalStart , event.getTimestamp() );
            int x0 = xOffset + (int) Math.round( durationMillis * scaleX);

            final Color color = UIConstants.getLegendItemForEvent( event ).color;
            graphics.setColor( color );
            int barWidth = x1 - x0;
            if ( (x0 + barWidth) >  canvasMaxX ) {
            	barWidth = canvasMaxX - x0;
            }
			graphics.fillRect( x0 ,y0 , barWidth , BAR_HEIGHT );
            if ( x0 < 0 || x1 < 0 || (x1-x0) < 0 ) {
                System.out.println("ERROR: rendering: "+x0+" -"+x1+", len: "+(x1-x0)+" , durationMillis: "+durationMillis+" , thread: "+event.threadId+", timestamp: "+event.getTimestamp());
            }
        }
    }

    @Override
    public Dimension getPreferredScrollableViewportSize()
    {
        return myPreferredSize == null ? getPreferredSize() : myPreferredSize;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return 10;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
    {
        return 10;
    }

    @Override
    public boolean getScrollableTracksViewportWidth()
    {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }
}