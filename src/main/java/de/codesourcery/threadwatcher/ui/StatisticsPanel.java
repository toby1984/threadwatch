package de.codesourcery.threadwatcher.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.Scrollable;

import de.codesourcery.threadwatcher.FileReader;
import de.codesourcery.threadwatcher.FileReader.FileVisitor;
import de.codesourcery.threadwatcher.HiResInterval;
import de.codesourcery.threadwatcher.PerThreadStatistics;
import de.codesourcery.threadwatcher.ThreadEvent;
import de.codesourcery.threadwatcher.ui.PercentageBar.IPercentageProvider;

public final class StatisticsPanel extends JPanel implements Scrollable
{
    private static final int BOX_WIDTH = 150;
    private static final int BOX_HEIGHT = 10;
    private static final int X_OFFSET = 10;
    private static final int Y_OFFSET = 10;    
    
    private static final int VERTICAL_SPACING = 5;    
    
    private FileReader reader;
    private HiResInterval interval;
    
    private Map<Integer,PerThreadStatistics> statistics;
    
    private Dimension myPreferredSize;
    
    // @GuardedBy( statisticsByRect )
    private final Map<Rectangle,LegendItemProvider> statisticsByRect = new HashMap<>();
    
    private final MouseAdapter mouseAdapter = new MouseAdapter() 
    {
        public void mouseMoved(java.awt.event.MouseEvent e) 
        {
            synchronized( statisticsByRect ) {
                for ( Entry<Rectangle, LegendItemProvider> entry : statisticsByRect.entrySet() ) 
                {
                    if ( entry.getKey().contains( e.getPoint() ) ) 
                    {
                        setToolTipText( entry.getValue().item.title+" : "+entry.getValue().getPercentageValue()+" %" );
                        return;
                    }
                }
                setToolTipText( null );
            }
        }
    };
    
    public StatisticsPanel(FileReader reader) {
        this.reader = reader;
        addMouseMotionListener( mouseAdapter );
    }
    
    public void setInterval(HiResInterval interval) throws IOException 
    {
        this.interval = interval;
        recalcStatistics();
        repaint();
    }
    
    private void recalcStatistics() throws IOException {
        
        if ( interval == null ) {
            statistics = null;
            return;
        }
        
        final Map<Integer,PerThreadStatistics> statistics = new HashMap<>();
        
        final Set<Integer> threadIds = reader.getAliveThreadsInInterval( interval );
        for ( int threadId : threadIds ) {
            statistics.put(threadId,new PerThreadStatistics(threadId));
        }
        
        final FileVisitor statisticsVisitor = new FileVisitor() {
            
            @Override
            public void visit(ThreadEvent event)
            {
                final PerThreadStatistics stat = statistics.get(event.threadId);
                stat.processEvent( event );
            }
        };
        reader.visit( statisticsVisitor , interval , threadIds );
        
        for ( PerThreadStatistics s : statistics.values() ) 
        {
            s.finish( interval );
        }        
        
        this.statistics = statistics;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        synchronized (statisticsByRect) {
            statisticsByRect.clear();
        }
        
        if ( statistics != null ) 
        {
            g.setColor(Color.BLACK);
            final double totalIntervalDurationMillis = interval.getDurationInMilliseconds();
            
            String longestName = getLongestThreadName( statistics.keySet() );
            int y = Y_OFFSET;
            
            final String s = interval.toUIString();
            g.drawString( s , X_OFFSET , y );
            
            y =  y + (int) Math.round( g.getFontMetrics().getStringBounds(  s , g ).getHeight()*1.2d );
            
            int x = X_OFFSET + (int) Math.ceil( getWidth(longestName,g)*1.1 );
            
            synchronized (statisticsByRect) 
            {
                for ( Entry<Integer, PerThreadStatistics> entry : statistics.entrySet() ) 
                {
                    final PerThreadStatistics statistics = entry.getValue();
                    final Integer threadId = entry.getKey();
                    
                    if ( statistics.containsData ) 
                    {                
                        final String threadName = reader.getThreadNamesByID().get( threadId )+" ("+threadId+")";
                        Point p = LegendItem.centerTextVertically( threadName , X_OFFSET , y , BOX_HEIGHT , (Graphics2D) g);
                        
                        g.setColor(Color.BLACK);
                        g.drawString( threadName ,p.x,p.y);
                        
                        final PercentageBar bar = new PercentageBar( getBackground() ) 
                        {
                            @Override
                            protected void valueAdded(IPercentageProvider value, Rectangle rect)
                            {
                                statisticsByRect.put( rect , (LegendItemProvider) value);
                            }
                        };
    
                        for ( Entry<LegendItem, Double> v : statistics.sumDurationInMillis.entrySet() ) 
                        {
                            double percentage = 100.0*( v.getValue() / totalIntervalDurationMillis );
                            LegendItemProvider provider = new LegendItemProvider( v.getKey() , percentage );
                            bar.setPercentage( v.getKey().color , provider );
                        }
                        
                        bar.render( x , y , BOX_WIDTH , BOX_HEIGHT , g );
                        y += BOX_HEIGHT+VERTICAL_SPACING;                    
                    }
                }
            }
            
            // update preferred size
            Dimension newSize = new Dimension( x + BOX_WIDTH+5, y );
            if ( ! getPreferredSize().equals(newSize ) ) 
            {
                myPreferredSize = newSize;
                setPreferredSize( newSize );
                revalidate();
            }
        }
    }
    
    protected static final class LegendItemProvider implements IPercentageProvider {

        public final LegendItem item;
        public final double percentageValue;
        
        private LegendItemProvider(LegendItem item, double percentageValue)
        {
            this.item = item;
            this.percentageValue = percentageValue;
        }

        @Override
        public double getPercentageValue()
        {
            return percentageValue;
        }
    }
    
    private double getWidth(String s,Graphics g) 
    {
        return g.getFontMetrics().getStringBounds(s,g).getWidth();
    }
    
    private String getLongestThreadName(Set<Integer> threadIds) 
    { 
        String longest = "";
        for ( Integer id : threadIds ) 
        {
            String name = getThreadName(id);
            if ( name.length() > longest.length() ) {
                longest = name;
            }
        }
        return longest;
    }
    
    private String getThreadName(Integer threadId) 
    {
        return reader.getThreadNamesByID().get( threadId)+" ("+threadId+")";
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
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportHeight()
    {
        return false;
    }
}
