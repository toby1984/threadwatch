/**
 * Copyright 2012 Tobias Gierke <tobias.gierke@code-sourcery.de>
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
import java.awt.Rectangle;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public abstract class PercentageBar
{
    private LinkedHashMap<Color,IPercentageProvider> percentages = new LinkedHashMap<>();
    private final Color background;
    
    public interface IPercentageProvider 
    {
        public double getPercentageValue();
    }
    
    public PercentageBar(Color background) {
        this.background = background;
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
    
    public void render(int leftX,int topY , int width , int height , Graphics g) {
        
        g.setColor( background );
        g.fillRect( leftX ,topY , width,height);
        
        g.setColor(Color.BLACK);
        g.drawRect( leftX ,topY , width,height);
        
        double scaleX = width / 100.0;
        double lastX = leftX;
        for ( Entry<Color, IPercentageProvider> entry : percentages.entrySet() ) 
        {
            final double currentX = lastX+ entry.getValue().getPercentageValue()*scaleX;
            g.setColor(entry.getKey());
            final int boxX = (int) Math.round( lastX );
            final int boxWidth = (int) Math.round( currentX - lastX );
            g.fillRect( boxX , topY , boxWidth , height );
            g.setColor( Color.BLACK );
            g.drawRect( boxX , topY , boxWidth , height );    
            valueAdded( entry.getValue() , new Rectangle( boxX , topY , boxWidth , height ) );
            lastX = currentX;
        }
    }
 
    protected abstract void valueAdded(IPercentageProvider value, Rectangle rect);
}
