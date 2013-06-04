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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import de.codesourcery.threadwatcher.ui.LegendItem;
import de.codesourcery.threadwatcher.ui.UIConstants;

public final class PerThreadStatistics 
{
    public final int threadId;
    
    private LegendItem lastThreadState;
    
    private long lastEventSeconds;
    private long lastEventNanos;
    
    public boolean containsData=false;
    
    public final Map<LegendItem,Double> sumDurationInMillis = new HashMap<>();
    
    public PerThreadStatistics(int threadId)
    {
        this.threadId = threadId;
        for ( LegendItem item : UIConstants.LEGEND_ITEMS) {
            sumDurationInMillis.put( item , new Double(0) );
        }
    }
    
    public void processEvent(ThreadEvent event) 
    {
        if ( lastThreadState != null ) 
        {
            Double total = sumDurationInMillis.get( lastThreadState );
            total = total + HiResInterval.getDurationInMilliseconds( lastEventSeconds,lastEventNanos , event.timestampSeconds,event.timestampNanos);
            sumDurationInMillis.put( lastThreadState , total );
        }
        lastThreadState = UIConstants.getLegendItemForEvent( event );
        lastEventSeconds = event.timestampSeconds;
        lastEventNanos = event.timestampNanos;
        containsData=true;        
    }
    
    public void finish(HiResInterval statisticsInterval) 
    {
        if ( lastThreadState != null ) 
        {
            final HiResTimestamp end = statisticsInterval.end;
            Double total = sumDurationInMillis.get( lastThreadState );
            total = total + HiResInterval.getDurationInMilliseconds( lastEventSeconds,lastEventNanos , end.secondsSinceEpoch,end.nanoseconds);
            sumDurationInMillis.put( lastThreadState , total );            
            lastThreadState = null;
        }
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if ( obj != null && obj.getClass() == PerThreadStatistics.class ) {
            return this.threadId == ((PerThreadStatistics) obj).threadId;
        }
        return false;
    }
    
    public String getAsString(double totalIntervalMillis)
    {
        StringBuilder result = new StringBuilder();
        result.append("Thread "+threadId).append("\n");
        result.append("-------------").append("\n");
        
        final DecimalFormat DF = new DecimalFormat("##0.0##");
        for (Iterator<Entry<LegendItem, Double>> it = sumDurationInMillis.entrySet().iterator(); it.hasNext();) 
        {
            final Entry<LegendItem, Double> entry = it.next();
            double millis = entry.getValue();
            if ( millis != 0.0 ) {
                double percentage = 100.0*( millis / totalIntervalMillis );
                result.append( StringUtils.leftPad( entry.getKey().title , 20 )+": "+DF.format( percentage )+" % ("+millis+" ms)");
                if ( it.hasNext() ) {
                    result.append("\n");
                }                
            }
        }
        return result.toString();
    }
    
    @Override
    public int hashCode()
    {
        return threadId;
    }
}