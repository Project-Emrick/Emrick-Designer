package org.emrick.project;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

/**
 * Scroll-click drag panning adapter implementation
 * Provides smooth, precise panning of Swing components using the scroll-click (middle mouse button)
 * This implementation directly manipulates the viewport position to ensure content
 * moves exactly with the mouse cursor, providing the most natural panning experience
 */
class PanningMouseAdapter extends MouseAdapter {
    private final JComponent component;
    private final JScrollPane scrollPane;
    private Point holdPoint;
    private Runnable afterPanAction;
    
    /**
     * Creates a new panning adapter for the specified component and scroll pane
     * 
     * @param component The component to enable panning for
     * @param scrollPane The scroll pane containing the component
     */
    public PanningMouseAdapter(JComponent component, JScrollPane scrollPane) {
        this.component = component;
        this.scrollPane = scrollPane;
        
        // Initialize mouse listeners safely
        initializeListeners();
    }
    
    /**
     * Creates a new panning adapter with a callback that runs after each pan movement
     * 
     * @param component The component to enable panning for
     * @param scrollPane The scroll pane containing the component
     * @param afterPanAction Action to run after each pan movement (e.g., repainting other components)
     */
    public PanningMouseAdapter(JComponent component, JScrollPane scrollPane, Runnable afterPanAction) {
        this.component = component;
        this.scrollPane = scrollPane;
        this.afterPanAction = afterPanAction;
        
        // Initialize mouse listeners safely
        initializeListeners();
    }
    
    /**
     * Safely initialize the mouse listeners to avoid "leaking this" warning
     */
    private void initializeListeners() {
        // Using a separate method to add listeners avoids the "leaking this in constructor" warning
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON2) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            holdPoint = e.getPoint();
            e.consume();
        }
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        if (holdPoint != null && e.getButton() == MouseEvent.BUTTON2) {
            component.setCursor(null);
            holdPoint = null;
            e.consume();
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent e) {
        if (holdPoint != null) {
            // Get current point
            Point dragPoint = e.getPoint();
            
            // Find viewport
            JViewport viewport = scrollPane.getViewport();
            
            if (viewport != null) {
                // Get current viewport position
                Point viewPos = viewport.getViewPosition();
                
                // Calculate maximum bounds
                int maxViewPosX = component.getWidth() - viewport.getWidth();
                int maxViewPosY = component.getHeight() - viewport.getHeight();
                
                // Handle horizontal scrolling
                if (component.getWidth() > viewport.getWidth()) {
                    viewPos.x -= (dragPoint.x - holdPoint.x);
                    
                    // Handle boundaries
                    if (viewPos.x < 0) {
                        viewPos.x = 0;
                        holdPoint.x = dragPoint.x;
                    }
                    
                    if (viewPos.x > maxViewPosX) {
                        viewPos.x = maxViewPosX;
                        holdPoint.x = dragPoint.x;
                    }
                }
                
                // Handle vertical scrolling
                if (component.getHeight() > viewport.getHeight()) {
                    viewPos.y -= (dragPoint.y - holdPoint.y);
                    
                    // Handle boundaries
                    if (viewPos.y < 0) {
                        viewPos.y = 0;
                        holdPoint.y = dragPoint.y;
                    }
                    
                    if (viewPos.y > maxViewPosY) {
                        viewPos.y = maxViewPosY;
                        holdPoint.y = dragPoint.y;
                    }
                }
                
                // Apply new position
                viewport.setViewPosition(viewPos);
                
                // Execute after-pan action if provided (e.g., repaint other components)
                if (afterPanAction != null) {
                    afterPanAction.run();
                }
                
                e.consume();
            }
        }
    }
    
    /**
     * Makes a component transparent to middle mouse events and passes them through
     * to a target component. This allows clicking through widgets like buttons and labels
     * to enable panning on the underlying component.
     * 
     * @param component The component to make transparent to middle mouse events
     * @param target The target component to which events should be forwarded
     * @return The MouseAdapter that was added to the component for event forwarding
     */
    public static MouseAdapter passMiddleMouseEvents(JComponent component, JComponent target) {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    // Forward the event to the target component
                    Point point = SwingUtilities.convertPoint(component, e.getPoint(), target);
                    target.dispatchEvent(new MouseEvent(
                        target, 
                        e.getID(), 
                        e.getWhen(), 
                        e.getModifiersEx(), 
                        point.x, point.y, 
                        e.getClickCount(), 
                        e.isPopupTrigger(), 
                        e.getButton()
                    ));
                    e.consume();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    // Forward the event to the target component
                    Point point = SwingUtilities.convertPoint(component, e.getPoint(), target);
                    target.dispatchEvent(new MouseEvent(
                        target, 
                        e.getID(), 
                        e.getWhen(), 
                        e.getModifiersEx(), 
                        point.x, point.y, 
                        e.getClickCount(), 
                        e.isPopupTrigger(), 
                        e.getButton()
                    ));
                    e.consume();
                }
            }
        };
        
        MouseMotionAdapter motionAdapter = new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if ((e.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
                    // Forward the event to the target component
                    Point point = SwingUtilities.convertPoint(component, e.getPoint(), target);
                    target.dispatchEvent(new MouseEvent(
                        target, 
                        e.getID(), 
                        e.getWhen(), 
                        e.getModifiersEx(), 
                        point.x, point.y, 
                        e.getClickCount(), 
                        e.isPopupTrigger(), 
                        e.getButton()
                    ));
                    e.consume();
                }
            }
        };
        
        component.addMouseListener(mouseAdapter);
        component.addMouseMotionListener(motionAdapter);
        
        return mouseAdapter; // Return the adapter for potential removal later
    }
}
