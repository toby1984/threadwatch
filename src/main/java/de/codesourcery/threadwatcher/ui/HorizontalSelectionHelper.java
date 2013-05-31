package de.codesourcery.threadwatcher.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

public abstract class HorizontalSelectionHelper<T> {
    
    private int xDragStart=-1;
    private int xDragEnd=-1; 
    private boolean selectionMarked = false;        
    private final Color selectionXORColor;
    
    private SelectedInterval lastSelection;
    
    public static final class SelectedInterval 
    {
        public final int xMin;
        public final int xMax;
        
        public SelectedInterval(int xMin, int xMax)
        {
            this.xMin = xMin;
            this.xMax = xMax;
        }
    }
    
    public HorizontalSelectionHelper(Color selectionXORColor) {
        this.selectionXORColor = selectionXORColor;
    }
    
    public boolean isSelecting() {
        return xDragStart != -1;
    }
    
    public boolean isSelectionAvailable() {
        return xDragStart != -1 && xDragEnd != -1;
    }
    
    protected abstract T getLastSelectionModelObject();    
    
    protected abstract int getMinX();
    
    protected abstract int getMaxX();
    
    public final SelectedInterval getLastSelection() 
    {
        return lastSelection;
    }
    
    public void stopSelecting(Point point , Graphics graphics,int height) 
    {
        if ( isSelectionAvailable() )
        {
            final int end = isValid(point) ? point.x : xDragEnd;
            final int min = Math.min(xDragStart,end);
            final int max = Math.max(xDragStart,end);
            
            lastSelection = new SelectedInterval(min,max);
            selectionFinished( min , max );
        }
        clearSelection(graphics, height);            
    }
    
    protected boolean isValid(Point p) 
    {
        return p.x >= getMinX() && p.x < getMaxX();
    }
     
    protected abstract void selectionFinished(int start,int end);
    
    public void updateSelection(Point point,Graphics graphics,int height) 
    {
        if ( ! isValid(point) ) {
            return;
        }
        if ( xDragStart == -1 ) {
            xDragStart = point.x;
        } else {
            renderSelection( graphics , point.x , height );
        }
    }
    
    public void repaint(Graphics graphics,int height) 
    {
        if ( isSelectionAvailable() ) 
        {
            graphics.setXORMode(selectionXORColor);
            int xmin = Math.min( xDragStart,xDragEnd);
            int xmax = Math.max( xDragStart,xDragEnd);
            graphics.fillRect( xmin , -1 , xmax-xmin , height+1 );
            selectionMarked=true;
        }
    }
    
    public void paintSelection(Graphics graphics,int xmin , int xmax , int height) 
    {
        graphics.setXORMode(selectionXORColor);
        graphics.fillRect( xmin , -1 , xmax-xmin , height+1 );
    }
    
    private void renderSelection( Graphics graphics, int newXDragX , int height) 
    {
        graphics.setXORMode(selectionXORColor);
        if ( selectionMarked ) // clear old selection
        {
            int xmin = Math.min( xDragStart,xDragEnd);
            int xmax = Math.max( xDragStart,xDragEnd);
            graphics.fillRect( xmin , -1 , xmax-xmin , height+1 );
            selectionMarked = false;
        }
        
        if ( newXDragX != -1 ) { // mark new selection
            int xmin = Math.min( xDragStart,newXDragX);
            int xmax = Math.max( xDragStart,newXDragX);
            graphics.fillRect( xmin , -1 , xmax-xmin , height+1 );
            selectionMarked = true;                      
        } 
        xDragEnd = newXDragX;
    }        
    
    public void clearSelection(Graphics graphics,int height)
    {
        if ( selectionMarked ) 
        {
            renderSelection(graphics,-1,height); // clear selection
        }
        
        xDragStart = xDragEnd = -1;
        selectionMarked = false;            
    }        
}