package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.NegotiationConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * Created by Droche on 30/10/2016.
 */
public class XMPPServerNegotiator extends XMPPNegotiator {


    /**
     * Constructs a new XMPP Server Negotiator.
     *
     * @param negotiationConsumer The object that will consume negotiation messages.
     */
    public XMPPServerNegotiator(NegotiationConsumer negotiationConsumer) {
        super(negotiationConsumer);
        this.negotiationStatus = NegotiationStatus.START;
    }


    // TODO: Esto mete leeted tambien?

    /**
     * Processes all fed data. Transforms messages if leeted, ignores messages if silenced, and sets an error state on
     * invalid XML. Sends all processed data to the Deque specified upon instantiation.
     *
     * @return The number of bytes offered to the output Deque, or -1 if the interpreter is in error state.
     */
    @Override
    protected ParserResponse process() throws XMLStreamException {
        if (!parser.hasNext()) {
            return ParserResponse.EVERYTHING_NORMAL;
        }
        StringBuilder readXML = new StringBuilder();
        while (parser.hasNext()) {
            next();
            if (negotiationStatus == NegotiationStatus.START) {
                switch (status) {
                    case AsyncXMLStreamReader.START_ELEMENT:
                        //Update status when starting a non-nested element

                        if (parser.getLocalName().equals("stream")) {
                            readXML.append("<stream:stream");
                            int attrCount = parser.getAttributeCount();
                            if (attrCount > 0) {
                                readXML.append(" ");
                                for (int i = 0; i < attrCount; i++) {
                                    StringBuilder attributeFullName = new StringBuilder();
                                    if (!parser.getAttributePrefix(i).isEmpty()) {
                                        attributeFullName.append(parser.getAttributePrefix(i))
                                                .append(":");
                                    }
                                    attributeFullName.append(parser.getAttributeLocalName(i));
                                    if (attributeFullName.toString().equals("to")) readXML.append("from");
                                    else if (attributeFullName.toString().equals("from")) readXML.append("to");
                                    else readXML.append(attributeFullName);
                                    readXML.append("=\"")
                                            .append(parser.getAttributeValue(i))
                                            .append("\"")
                                            .append(i < attrCount - 1 ? " " : "");
                                    if (!initialParameters.containsKey(attributeFullName.toString())) {
                                        initialParameters.put(attributeFullName.toString(), parser.getAttributeValue(i));
                                    }
                                }
                            }
                            readXML.append("id='randomgenerated' xmlns:stream=\"http://etherx.jabber.org/streams\" xmlns=\"jabber:client\" xmlns:xml=\"http://www.w3.org/XML/1998/namespace\" "); //TODO random
                            readXML.append("> ");
                            readXML.append("<stream:features> " +
                                    "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'> " +
                                    "<mechanism>PLAIN</mechanism> " +
                                    "</mechanisms> " +
                                    "</stream:features>");
                            negotiationStatus = NegotiationStatus.AUTH;
                            System.out.println(readXML);
                            negotiationConsumer.consumeNegotiationMessage(readXML.toString().getBytes());
                            while (parser.hasNext() && status != AsyncXMLStreamReader.EVENT_INCOMPLETE)
                                next(); //TODO handle more?
                            return ParserResponse.EVERYTHING_NORMAL;
                        } else {
                            //TODO handle error?
                        }
                        break;
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return ParserResponse.EVERYTHING_NORMAL;
                    case -1:
                        //TODO throw exception? Remove sout
                        System.out.println("XML interpreter entered error state (invalid XML)");
                        return ParserResponse.XML_ERROR;
                }
            } else if (negotiationStatus == NegotiationStatus.AUTH) {
                switch (status) { //TODO check it is really plain and not other shit
                    case AsyncXMLStreamReader.CHARACTERS:
                        authorization = parser.getText();


                        String response = "<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>";
                        System.out.println(response);
                        System.out.println("Connection with client was a SUCCESS");
                        negotiationConsumer.consumeNegotiationMessage(response.getBytes());
                        while (parser.hasNext() && status != AsyncXMLStreamReader.EVENT_INCOMPLETE)
                            next(); //TODO handle more?
                        return ParserResponse.NEGOTIATION_END;
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return ParserResponse.EVERYTHING_NORMAL;
                    case -1:
                        //TODO throw exception? Remove sout
                        System.out.println("XML interpreter entered error state (invalid XML)");
                        return ParserResponse.XML_ERROR;
                }

            }

        }
        System.out.println(readXML);
        negotiationConsumer.consumeNegotiationMessage(readXML.toString().getBytes());
        return ParserResponse.EVERYTHING_NORMAL;
    }
}
