package de.codesourcery.threadwatcher.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

public abstract class HorizontalSelectionHelper<T> {
    
    public static final int DRAG_RADIUS_IN_PIXELS = 10;
    
    public static final int NOT_SET = -1;
    
    private int xDragStart=NOT_SET;
    private int xDragEnd=NOT_SET; 
    private boolean selectionMarked = false;        
    private final Color selectionXORColor;
    
    private SelectedInterval lastSelection;
    private DraggedMarker draggedMarker = DraggedMarker.NONE;
    
    public static final class SelectedInterval 
    {
        public final int xMin;
        public final int xMax;
        
        public SelectedInterval(int xMin, int xMax)
        {
            this.xMin = xMin;
            this.xMax = xMax;
        }
        
        public SelectedInterval withMinX(int newMinX) 
        {
            return new SelectedInterval( newMinX , xMax );
        }
        
        public SelectedInterval withMaxX(int newMaxX) 
        {
            return new SelectedInterval( xMin , newMaxX );
        }        
    }
    
    public static enum DraggedMarker {
        NONE,
        START_AND_END,
        START,
        END;
    }
    
    public final DraggedMarker getDraggedMarker()
    {
        return draggedMarker;
    }
    
    public final void setDraggedMarker(DraggedMarker marker) 
    {
        if (marker == null) {
            throw new IllegalArgumentException("marker must not be NULL.");
        }
        this.draggedMarker = marker;
    }
    
    public final DraggedMarker getDragMarkerForPoint(Point p) 
    {
        if ( isCloseToLastSelectionStart( p ) ) {
            return DraggedMarker.START;
        }
        if ( isCloseToLastSelectionEnd( p ) ) 
        {
            return DraggedMarker.END;
        }
        if ( isInsideLastSelection( p ) ) {
            return DraggedMarker.START_AND_END;
        }
        return DraggedMarker.NONE;
    }
    
    private final boolean isInsideLastSelection(Point p)
    {
        return getLastSelection() != null && ( p.x >= getLastSelection().xMin && p.x <= getLastSelection().xMax );
    }

    public final boolean isCloseToLastSelectionStart(Point point) 
    {
        if ( getLastSelection() != null  && isValid( point ) ) 
        {
            return Math.abs( getLastSelection().xMin - point.x ) <= DRAG_RADIUS_IN_PIXELS;
        }
        return false;
    }
    
    public final boolean isCloseToLastSelectionEnd(Point point) 
    {
        if ( getLastSelection() != null && isValid( point ) ) 
        {
            return Math.abs( getLastSelection().xMax - point.x ) <= DRAG_RADIUS_IN_PIXELS;
        }
        return false;
    }    
    
    public HorizontalSelectionHelper(Color selectionXORColor) {
        this.selectionXORColor = selectionXORColor;
    }
    
    public final boolean isSelecting() {
        return xDragStart != NOT_SET;
    }
    
    public final boolean isSelectionAvailable() {
        return xDragStart != NOT_SET && xDragEnd != NOT_SET;
    }
    
    protected abstract T getLastSelectionModelObject();    
    
    protected abstract int getMinX();
    
    protected abstract int getMaxX();
    
    public final SelectedInterval getLastSelection() 
    {
        return lastSelection;
    }
    
    public SelectedInterval setLastSelection(SelectedInterval interval) 
    {
        this.lastSelection = interval;
        return interval;
    }    
    
    public final void stopSelecting(Point point , Graphics graphics,int height) 
    {
        if ( isSelectionAvailable() )
        {
            final int end = isValid(point) ? point.x : xDragEnd;
            final int min = Math.min(xDragStart,end);
            final int max = Math.max(xDragStart,end);
            
            lastSelection = new SelectedInterval(min,max);
            selectionFinished( min , max );
            
            if ( selectionMarked ) 
            {
                renderSelection(graphics,NOT_SET,height); // clear selection
            }
            
            xDragStart = xDragEnd = NOT_SET;
            selectionMarked = false;            
        } 
        else {
            clearSelection(graphics, height);
        }
    }
    
    protected final boolean isValid(Point p) 
    {
        return p.x >= getMinX() && p.x < getMaxX();
    }
     
    protected abstract void selectionFinished(int start,int end);
    
    public void updateSelection(Point point,Graphics graphics,int height) 
    {
        if ( ! isValid(point) ) {
            return;
        }
        if ( xDragStart == NOT_SET ) {
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
            graphics.fillRect( xmin , NOT_SET , xmax-xmin , height+1 );
            selectionMarked=true;
        }
    }
    
    public final void paintSelection(Graphics graphics,int xmin , int xmax , int height) 
    {
        graphics.setXORMode(selectionXORColor);
        graphics.fillRect( xmin , NOT_SET , xmax-xmin , height+1 );
    }
    
    private final void renderSelection( Graphics graphics, int newXDragEnd , int height) 
    {
        graphics.setXORMode(selectionXORColor);
        if ( selectionMarked ) // clear old selection
        {
            int xmin = Math.min( xDragStart,xDragEnd);
            int xmax = Math.max( xDragStart,xDragEnd);
            graphics.fillRect( xmin , NOT_SET , xmax-xmin , height+1 );
            selectionMarked = false;
        }
        
        if ( newXDragEnd != NOT_SET ) { // mark new selection
            int xmin = Math.min( xDragStart,newXDragEnd);
            int xmax = Math.max( xDragStart,newXDragEnd);
            graphics.fillRect( xmin , NOT_SET , xmax-xmin , height+1 );
            selectionMarked = true;                      
        } 
        this.xDragEnd = newXDragEnd;
    }        
    
    public final void clearSelection(Graphics graphics,int height)
    {
        if ( selectionMarked ) 
        {
            renderSelection(graphics,NOT_SET,height); // clear selection
        }
        
        xDragStart = xDragEnd = NOT_SET;
        selectionMarked = false;
        selectionCleared();
    }   
    
    protected abstract void selectionCleared();
}