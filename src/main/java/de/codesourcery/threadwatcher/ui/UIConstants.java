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
import java.util.ArrayList;
import java.util.List;

import de.codesourcery.threadwatcher.JVMTIThreadState;
import de.codesourcery.threadwatcher.ThreadEvent;

public class UIConstants
{
    // legend stuff
    public static final List<LegendItem> LEGEND_ITEMS = new ArrayList<>();
    
    private static final Color COLOR_RUNNABLE = Color.GREEN;
    private static final Color COLOR_BLOCKED = Color.RED;
    private static final Color COLOR_WAITING_GENERAL = Color.YELLOW;
    private static final Color COLOR_WAITING_TIMEOUT= Color.BLUE;
    private static final Color COLOR_WAITING_INDEFINITELY = Color.CYAN;
    private static final Color COLOR_DEAD = Color.LIGHT_GRAY;
    private static final Color COLOR_SUSPENDED = Color.BLACK;
    private static final Color COLOR_SLEEPING = Color.PINK;

    public static final int LEGEND_BOX_WIDTH = 10;
    public static final int LEGEND_BOX_HEIGHT = 10;
    public static final int LEGEND_BOX_TO_LABEL_DISTANCE= 7;
    
    public static final LegendItem LEGENDITEM_RUNNABLE;
    public static final LegendItem LEGENDITEM_BLOCKED;
    public static final LegendItem LEGENDITEM_WAITING_GENERAL;
    public static final LegendItem LEGENDITEM_WAITING_TIMEOUT;
    public static final LegendItem LEGENDITEM_WAITING_INDEFINITELY;
    public static final LegendItem LEGENDITEM_DEAD;
    public static final LegendItem LEGENDITEM_SUSPENDED;
    public static final LegendItem LEGENDITEM_SLEEPING;
    
    static 
    {
        LEGENDITEM_RUNNABLE = addLegendItem("Runnable" , COLOR_RUNNABLE);
        LEGENDITEM_BLOCKED = addLegendItem("Blocked" , COLOR_BLOCKED);
        LEGENDITEM_WAITING_GENERAL = new LegendItem("Waiting" , COLOR_WAITING_GENERAL);
        LEGENDITEM_WAITING_TIMEOUT = addLegendItem("Waiting (timeout)" , COLOR_WAITING_TIMEOUT);
        LEGENDITEM_WAITING_INDEFINITELY = addLegendItem("Waiting (indef.)" , COLOR_WAITING_INDEFINITELY);
        LEGENDITEM_DEAD = addLegendItem("Dead" , COLOR_DEAD);
        LEGENDITEM_SUSPENDED = addLegendItem("Suspended" , COLOR_SUSPENDED);
        LEGENDITEM_SLEEPING = addLegendItem("Sleeping" , COLOR_SLEEPING);
    }
    
    private static LegendItem addLegendItem(String title,Color color) 
    {
        final LegendItem result = new LegendItem(title,color);
        LEGEND_ITEMS.add( result );
        return result;
    }
    
    public static final int BAR_SPACING = 2;   
    public static final int Y_OFFSET = 35;
    public static final int X_OFFSET = 10;
    
    public static LegendItem getLegendItemForEvent(ThreadEvent event) 
    {
        if ( event.type == ThreadEvent.THREAD_START ) {
            return LEGENDITEM_RUNNABLE;
        } 
        
        if ( event.type == ThreadEvent.THREAD_DEATH ) {
            return LEGENDITEM_DEAD;
        } 
        
        final int state = event.threadStateMask;
        if ( JVMTIThreadState.ALIVE.isSet( state ) ) 
        {
            if ( JVMTIThreadState.WAITING.isSet( state ) ) 
            {
                if ( JVMTIThreadState.WAITING_WITH_TIMEOUT.isSet( state ) ) {
                    return LEGENDITEM_WAITING_TIMEOUT;
                } 
                if ( JVMTIThreadState.WAITING_INDEFINITELY.isSet( state ) )  {
                    return LEGENDITEM_WAITING_INDEFINITELY;
                } 
                return LEGENDITEM_WAITING_GENERAL;
            } 
            
            if ( JVMTIThreadState.BLOCKED_ON_MONITOR_ENTER.isSet( state ) ) 
            {
                return LEGENDITEM_BLOCKED;
            } 
            
            if ( JVMTIThreadState.SUSPENDED.isSet( state ) ) 
            {
                return LEGENDITEM_SUSPENDED;
            } 
            if ( JVMTIThreadState.SLEEPING.isSet( state )  ) 
            {
                return LEGENDITEM_SLEEPING;
            } 
            if ( JVMTIThreadState.RUNNABLE.isSet( state ) ) {
                return LEGENDITEM_RUNNABLE;
            }                    
        } 
        return LEGENDITEM_DEAD;
    }
}
