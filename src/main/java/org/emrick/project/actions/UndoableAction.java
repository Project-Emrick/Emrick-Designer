package org.emrick.project.actions;

public interface UndoableAction {
    void execute();
    void undo();
    void redo();
}