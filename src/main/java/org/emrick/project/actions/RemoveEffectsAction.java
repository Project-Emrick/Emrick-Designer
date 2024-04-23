package org.emrick.project.actions;

import org.emrick.project.Effect;
import org.emrick.project.Performer;

import java.util.List;

public class RemoveEffectsAction implements UndoableAction {

    private final Effect effect;
    private final List<Performer> performers;

    public RemoveEffectsAction(Effect effect, List<Performer> performers) {
        this.effect = effect;
        this.performers = performers;
    }

    @Override
    public void execute() {
        performers.forEach(performer -> performer.getEffects().remove(effect));
    }

    @Override
    public void undo() {
        performers.forEach(performer -> performer.getEffects().add(effect));
    }

    @Override
    public void redo() {
        execute();
    }
}
