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
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;

public final class LegendItem {

    public final String title;
    public final Color color;
    
    public LegendItem(String title, Color color)
    {
        this.title = title;
        this.color = color;
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if ( obj instanceof LegendItem) {
            return this.title.equals( ((LegendItem) obj).title );
        }
        return false; 
    }
    
    @Override
    public int hashCode()
    {
        return title.hashCode();
    }
    
    public Rectangle getBounds(Graphics2D g) 
    {
        Rectangle2D bounds = g.getFontMetrics().getStringBounds( title , g );
        double width = bounds.getWidth()+LEGEND_BOX_WIDTH+LEGEND_BOX_TO_LABEL_DISTANCE;
        double height = Math.max( bounds.getHeight() , LEGEND_BOX_HEIGHT );
        return new Rectangle(0,0, (int) Math.round(width),(int) Math.round(height));
    }

    public void render(int leftX,int topY,Graphics2D g) 
    {
        g.setColor(color);
        g.fillRect(leftX,topY,LEGEND_BOX_WIDTH,LEGEND_BOX_HEIGHT);
        
        g.setColor(Color.BLACK);
        g.drawRect(leftX,topY,LEGEND_BOX_WIDTH,LEGEND_BOX_HEIGHT);
        
        final Point p = centerTextVertically(title,leftX+LEGEND_BOX_WIDTH+LEGEND_BOX_TO_LABEL_DISTANCE,topY,LEGEND_BOX_HEIGHT,g);
        g.drawString(title,p.x , p.y );
    }
    
    public static Point centerTextVertically(String text,int x,int y,int drawRectHeight,Graphics2D g) 
    {
        LineMetrics metrics = g.getFontMetrics().getLineMetrics(text , g);
        
        final int top = y;
        final int bottom = y+LEGEND_BOX_HEIGHT;
        double baseline=(top+((bottom+1-top)/2.0)) - ( (metrics.getAscent() + metrics.getDescent() )/2.0 ) + metrics.getAscent();        
        final int yCenter = (int) Math.round( baseline );    
        return new Point(x,yCenter);
    }
} 