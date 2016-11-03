package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser;

/**
 * Created by drocheg on 29/10/2016.
 */
public enum ParserResponse {
    EVERYTHING_NORMAL,
    STREAM_CLOSED,
    XML_ERROR,
    NEGOTIATION_END,
    NEGOTIATION_ERROR,

    // TODO: I would change it to parser state
}
