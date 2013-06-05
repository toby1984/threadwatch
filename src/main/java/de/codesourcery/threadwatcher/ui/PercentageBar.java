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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

public class PercentageBar 
{
	private static final Insets INSETS = new Insets(2,2,2,2);
	
    private final LinkedHashMap<Color,IPercentageProvider> percentages = new LinkedHashMap<>();
    private final Map<Rectangle,IPercentageProvider> valuesByRect = new HashMap<>();
    
    private Color backgroundColor = Color.WHITE;
    
    public IPercentageProvider getValueForPoint(Point p) 
    {
        for ( Entry<Rectangle, IPercentageProvider> entry : valuesByRect.entrySet() ) 
        {
            if ( entry.getKey().contains( p ) ) 
            {
                return entry.getValue();
            }
        }
        return null;
    }
    
    public interface IPercentageProvider 
    {
        public double getPercentageValue();
    }
    
    public void setPercentage(Color color,IPercentageProvider value) 
    {
        if ( color == null ) 
        {
            throw new IllegalArgumentException("color must not be NULL.");
        }
        if ( value == null ) {
            throw new IllegalArgumentException("value must not be NULL.");
        }
        percentages.put(color,value);
    }
    
    public void render(Graphics g,Rectangle bounds) 
    {
    	g.setColor(backgroundColor);
    	g.fillRect( bounds.x , bounds.y , bounds.width , bounds.height );
    	
        g.setColor(Color.BLACK);
        
        final int xmin = bounds.x + INSETS.left;
        final int ymin = bounds.y + INSETS.top;
        
        final int xmax = xmin + bounds.width - INSETS.right - INSETS.left;
        final int ymax = ymin + bounds.height - INSETS.top - INSETS.bottom;
        
        final int width = xmax - xmin;
        final int height = ymax - ymin;
        g.drawRect( xmin,ymin , width , height); 
        
        double scaleX = width / 100.0;
        double lastX = xmin;
        
        valuesByRect.clear();
        
        for ( Entry<Color, IPercentageProvider> entry : percentages.entrySet() ) 
        {
            final double currentX = lastX+ entry.getValue().getPercentageValue()*scaleX;
            g.setColor(entry.getKey());
            final int boxX = (int) Math.round( lastX );
            final int boxWidth = (int) Math.round( currentX - lastX );
            g.fillRect( boxX , ymin, boxWidth , height );
            g.setColor( Color.BLACK );
            g.drawRect( boxX , ymin , boxWidth , height );    
            
            valuesByRect.put( new Rectangle( boxX , ymin , boxWidth , height ) , entry.getValue() );
            lastX = currentX;
        }
    }
}
