package org.emrick.project;

import java.util.*;

/**
 * Responsible for managing color change mementos
 */
class ColorChangeCaretaker {

    private final Stack<ColorChangeMemento> undoMementos = new Stack<>();
    private final Stack<ColorChangeMemento> redoMementos = new Stack<>();
    private boolean hasUndoneSomeChanges;

    public void addMemento(ColorChangeMemento memento) {
        // When the user has undone some changes and makes a new change
        // drop all the undone changes
        if (hasUndoneSomeChanges) {
            dropUndoneChanges();
        } else {
            undoMementos.push(memento);
        }
        // Clear redo stack when a new change is made
        redoMementos.clear();
    }

    public void dropUndoneChanges() {
        undoMementos.clear();
    }

    public ColorChangeMemento undo() {
        // When the user hits Ctrl-Z, the top of the session-local list
        // should be undone (do the opposite)
        if (!undoMementos.isEmpty()) {
            ColorChangeMemento memento = undoMementos.pop();
            redoMementos.push(memento);
            hasUndoneSomeChanges = true;
            return memento;
        }
        return null;
    }

    public ColorChangeMemento redo() {
        // When the user hits Ctrl-Y, the current index of the session-local
        // list should be redone (apply the color again)
        if (!redoMementos.isEmpty()) {
            ColorChangeMemento memento = redoMementos.pop();
            undoMementos.push(memento);
            return memento;
        }
        return null;
    }

}