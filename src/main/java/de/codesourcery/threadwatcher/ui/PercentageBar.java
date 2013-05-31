package de.codesourcery.threadwatcher.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class PercentageBar
{
    private LinkedHashMap<Color,Double> percentages = new LinkedHashMap<>();
    private final Color background;
    
    public PercentageBar(Color background) {
        this.background = background;
    }
    
    public void setPercentage(Color color,Double value) 
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
        for ( Entry<Color, Double> entry : percentages.entrySet() ) 
        {
            final double currentX = lastX+ entry.getValue()*scaleX;
            g.setColor(entry.getKey());
            g.fillRect( (int) Math.round( lastX ) , topY , (int) Math.round( currentX - lastX ) , height );
            g.setColor( Color.BLACK );
            g.drawRect( (int) Math.round( lastX ) , topY , (int) Math.round( currentX - lastX ) , height );            
            lastX = currentX;
        }
    }
}
