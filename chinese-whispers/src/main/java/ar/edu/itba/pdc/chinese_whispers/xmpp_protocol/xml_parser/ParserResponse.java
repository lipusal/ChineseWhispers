package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser;

/**
 * Created by drocheg on 29/10/2016.
 */
public enum ParserResponse {
    EVERYTHING_NORMAL,
    STREAM_CLOSED,
    XML_ERROR,
    POLICY_VIOLATION,
    NEGOTIATION_END,
    NEGOTIATION_ERROR,

    // TODO: I would change it to parser state

    // TODO: I will separate normal parser states from negotiation states
}
