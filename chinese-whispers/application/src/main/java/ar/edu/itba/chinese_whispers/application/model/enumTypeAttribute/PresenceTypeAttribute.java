package ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute;

public enum PresenceTypeAttribute {
    UNAVAILABLE,    // -- Signals that the entity is no longer available for communication.
    SUBSCRIBE,      // -- The sender wishes to subscribe to the recipient's presence.
    SUBSCRIBED,     //-- The sender has allowed the recipient to receive their presence.
    UNSUBSCRIBE,    //-- The sender is unsubscribing from another entity's presence.
    UNSUBSCRIBED,   //-- The subscription request has been denied or a previously-granted subscription has been cancelled.
    PROBE,          //-- A request for an entity's current presence; SHOULD be generated only by a server on behalf of a user.
    ERROR           //-- An error has occurred regarding processing or delivery of a previously-sent presence stanza.
}
