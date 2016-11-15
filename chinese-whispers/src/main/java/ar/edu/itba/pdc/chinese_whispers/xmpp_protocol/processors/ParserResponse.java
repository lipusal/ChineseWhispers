package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors;

/**
 * Created by drocheg on 29/10/2016.
 */
public enum ParserResponse {
    EVERYTHING_NORMAL,
    STREAM_CLOSED,
    XML_ERROR,
    POLICY_VIOLATION,
    HOST_UNKNOWN,
    INVALID_AUTH_MECHANISM,
    MALFORMED_REQUEST,
    UNSUPPORTED_NEGOTIATION_MECHANISM,
    FAILED_NEGOTIATION,
    NEGOTIATION_END,
    EVENT_INCOMPLETE,
}
