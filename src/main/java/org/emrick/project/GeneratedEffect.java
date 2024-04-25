package org.emrick.project;

import java.util.ArrayList;

public abstract class GeneratedEffect extends Effect {
    protected ArrayList<Performer> performers;
    public GeneratedEffect(long startTimeMsec, long endTimeMsec) {
        super(startTimeMsec);
        performers = new ArrayList<>();
    }

    public ArrayList<Performer> getPerformers() {
        return performers;
    }

    public void setPerformers(ArrayList<Performer> performers) {
        this.performers = performers;
    }

    public void addPerformer(Performer p) {
        performers.add(p);
    }

    public abstract void generate();
}
