package org.highreshud.core;

/** A removable registration returned by the public core buses. */
public interface Subscription {
    void unsubscribe();
}
