package de.codesourcery.threadwatcher.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JPanel;

import de.codesourcery.threadwatcher.FileReader;
import de.codesourcery.threadwatcher.FileReader.FileVisitor;
import de.codesourcery.threadwatcher.HiResInterval;
import de.codesourcery.threadwatcher.PerThreadStatistics;
import de.codesourcery.threadwatcher.ThreadEvent;

public final class StatisticsPanel extends JPanel
{
    private static final int BOX_HEIGHT = 10;
    private static final int X_OFFSET = 10;
    private static final int Y_OFFSET = 10;    
    
    private static final int VERTICAL_SPACING = 5;    
    
    private FileReader reader;
    private HiResInterval interval;
    private Map<Integer,PerThreadStatistics> statistics;
    
    public StatisticsPanel(FileReader reader) {
        this.reader = reader;
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
        
        final List<Integer> ids = new ArrayList<>(statistics.keySet());
        Collections.sort(ids);
        double totalIntervalDurationMillis = interval.getDurationInMilliseconds();
        for ( Integer threadId : ids ) {
            PerThreadStatistics stats = statistics.get(threadId);
            if ( stats.containsData ) {
                System.out.println( stats.getAsString( totalIntervalDurationMillis  ) );
            }
        }      
        
        this.statistics = statistics;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
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
                    
                    final PercentageBar bar = new PercentageBar( getBackground() );

                    for ( Entry<LegendItem, Double> v : statistics.sumDurationInMillis.entrySet() ) 
                    {
                        double percentage = 100.0*( v.getValue() / totalIntervalDurationMillis );
                        bar.setPercentage( v.getKey().color , percentage );
                    }
                    
                    bar.render( x , y , 150 , BOX_HEIGHT , g );
                    
                    System.out.println("====> Rendered at "+y);
                    y += BOX_HEIGHT+VERTICAL_SPACING;                    
                }
            }
        }
    }
    
    private double getWidth(String s,Graphics g) 
    {
        return g.getFontMetrics().getStringBounds(s,g).getWidth();
    }
    
    private String getLongestThreadName(Set<Integer> threadIds) {
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
}
