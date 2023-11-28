package com.utility.mobile.bluetoothle.utils;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.util.ArrayList;

/**
 *
 * @author jtorres
 * @param <T>
 */
public abstract class EventDispatcher<T> {
    protected final ArrayList<T> listeners;
    protected boolean iterating = false;
    protected ArrayList<Integer> indexesToRemove;

    public EventDispatcher(){
        listeners = new ArrayList<T>();
        indexesToRemove = new ArrayList<Integer>();
    }

    public void addListener(T listener){
        synchronized(listeners){
            listeners.add(listener);
        }
    }

    public void removeListener(T listener) throws Exception {
        synchronized(listeners){
            int index = listeners.indexOf(listener);
            if(index > -1) {
                if (iterating) {
                    indexesToRemove.add(index);
                } else {
                    listeners.remove(index);
                }
            }
        }
    }

    public synchronized void startedIterating(){
        iterating = true;
    }

    public void finishedIterating(){
        synchronized(listeners){
            for(int index : indexesToRemove){
                listeners.remove(index);
            }

            indexesToRemove.clear();
        }
    }

    public ArrayList<T> getListeners(){
        return listeners;
    }
}
