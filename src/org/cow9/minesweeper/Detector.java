package org.cow9.minesweeper;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoundedRangeModel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

abstract public class Detector implements BoundedRangeModel {
    private final int sweeperDelay = 200;
    private int value = 0;
    private List<ChangeListener> listeners =
        new ArrayList<ChangeListener>();
    private final ChangeEvent ev = new ChangeEvent(this);
    
    abstract public void fire();
    public void start() {
        if (timer.isRunning()) {
            value = 0;
            timer.restart();
            broadcast();
        } else {
            timer.start();
        }
    }
    public void stop() {
        if (timer.isRunning()) {
            value = 0;
            timer.stop();
            broadcast();
        }
    }
    
    private final Timer timer = new Timer(20, new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            value = (value + 1) % sweeperDelay;
            if (value == 0) {
            	fire();
                timer.stop();
            }
            broadcast();
        }
    });
    
    private void broadcast() {
        for (ChangeListener lis: listeners)
            lis.stateChanged(ev);
    }
    
    @Override public int getValue() { return value; }
    @Override public int getMinimum() { return 0; }
    @Override public int getMaximum() { return sweeperDelay; }
    @Override public int getExtent() { return 0; }
    @Override public void addChangeListener(ChangeListener lis) { listeners.add(lis); }
    @Override public void removeChangeListener(ChangeListener lis) { listeners.remove(lis); }
    @Override public boolean getValueIsAdjusting() { return value == 0; }
    @Override public void setValue(int value) {}
    @Override public void setExtent(int extent) {}
    @Override public void setMinimum(int min) {}
    @Override public void setMaximum(int max) {}
    @Override public void setRangeProperties(int v, int e, int min, int max, boolean adj) {}
    @Override public void setValueIsAdjusting(boolean adj) {}
}
