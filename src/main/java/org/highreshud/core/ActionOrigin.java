package org.highreshud.core;

/** Identifies who initiated an action observed at the Wurm connection hook. */
public enum ActionOrigin {
    USER,
    KEYBINDER,
    HUD,
    WAYPOINTER,
    THIRD_PERSON,
    INTERNAL,
    UNKNOWN
}
