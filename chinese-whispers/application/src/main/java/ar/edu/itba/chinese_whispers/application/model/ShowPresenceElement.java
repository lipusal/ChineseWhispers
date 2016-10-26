package ar.edu.itba.chinese_whispers.application.model;

/**
 * Created by dgrimau on 25/10/16.
 */
public enum ShowPresenceElement {
    AWAY,       //-- The entity or resource is temporarily away.
    CHAT,       // -- The entity or resource is actively interested in chatting.
    DND,        // -- The entity or resource is busy (dnd = "Do Not Disturb").
    XA          // -- The entity or resource is away for an extended period (xa = "eXtended Away").
}
