package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation;

//<<<<<<< 2a208fef4234855d3346792f9276a1ffaf3e220e

import ar.edu.itba.pdc.chinese_whispers.application.Configurations;
//=======
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
//>>>>>>> Partial commit
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;

/**
 * Created by Droche on 30/10/2016.
 */
public class XMPPServerNegotiator extends XMPPNegotiator {

    StringBuilder authorizationBuilder;

    /**
     * Constructs a new XMPP Server Negotiator.
     *
     * @param outputConsumer The object that will consume output messages.
     */
    public XMPPServerNegotiator(OutputConsumer outputConsumer) {
        super(outputConsumer);
        this.negotiationStatus = NegotiationStatus.START;
        authorizationBuilder = new StringBuilder();
    }


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
            if (status == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                return ParserResponse.EVERYTHING_NORMAL;
            }
            if (status == -1) {
                //TODO throw exception? Remove sout
                System.out.println("XML interpreter entered error state (invalid XML)");
                return ParserResponse.XML_ERROR;
            }
            if (negotiationStatus == NegotiationStatus.START) {
                switch (status) {
//                    case AsyncXMLStreamReader.START_DOCUMENT:
//                        System.out.println("version: " + parser.getVersion());
//                        System.out.println("encoding: " + parser.getEncoding());
//                        break;
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
                                    readXML.append("=\'")
                                            .append(parser.getAttributeValue(i))
                                            .append("\'")
                                            .append(i < attrCount - 1 ? " " : "");
                                    if (!initialParameters.containsKey(attributeFullName.toString())) {
                                        initialParameters.put(attributeFullName.toString(), parser.getAttributeValue(i));
                                    }
                                }
                            }
                            long newId = Configurations.getInstance().getNewId(); //TODO change this.
                            readXML.append(" id='").append(newId).append("' xmlns:stream=\'http://etherx.jabber.org/streams\' xmlns=\'jabber:client\' xmlns:xml=\'http://www.w3.org/XML/1998/namespace\'>\n");
                            readXML.append("<stream:features> " +
                                    "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'> " +
                                    "<mechanism>PLAIN</mechanism> " +
                                    "</mechanisms> " +
                                    "</stream:features>");
                            // TODO: Make each line as an append
                            negotiationStatus = NegotiationStatus.AUTH;
                            // System.out.println(readXML);
                            outputConsumer.consumeMessage(readXML.toString().getBytes());
                            while (parser.hasNext() && status != AsyncXMLStreamReader.EVENT_INCOMPLETE)
                                next(); //TODO handle more?
                            return ParserResponse.EVERYTHING_NORMAL;
                        } else {
                            //TODO handle error?
                        }
                        break;
                }
            } else if (negotiationStatus == NegotiationStatus.AUTH) {
                switch (status) {
                    case AsyncXMLStreamReader.START_ELEMENT:
                        boolean validMechanism = false;
                        if (parser.getLocalName().equals("auth")) {
                            int attrCount = parser.getAttributeCount();
                            if (attrCount > 0) {
                                for (int i = 0; i < attrCount; i++) {
                                    StringBuilder attributeFullName = new StringBuilder();
                                    if (!parser.getAttributePrefix(i).isEmpty()) {
                                        attributeFullName.append(parser.getAttributePrefix(i))
                                                .append(":");
                                    }
                                    attributeFullName.append(parser.getAttributeLocalName(i));

                                    if (attributeFullName.toString().equals("mechanism")) {
                                        String mechanismValue = parser.getAttributeValue(i);
                                        if (mechanismValue.equals("PLAIN")) {
                                            validMechanism = true;
                                        }
                                    }
                                }
                            }
                            if (validMechanism) {
                                negotiationStatus = NegotiationStatus.AUTH_2;
                            } else {
                                String negotiationError = "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" +
                                        "        <invalid-mechanism/>" +
                                        "      </failure>";
                                outputConsumer.consumeMessage(negotiationError.getBytes());
                                return ParserResponse.NEGOTIATION_ERROR;
                            }
                        }

                        break;
                }
            } else if (negotiationStatus == NegotiationStatus.AUTH_2) {
                switch (status) {
                    case AsyncXMLStreamReader.CHARACTERS:
                        authorizationBuilder.append(parser.getText());
                        break;
                    case AsyncXMLStreamReader.END_ELEMENT:
                        if (authorizationBuilder.toString().isEmpty()) {//TODO maybe not here?
                            String negotiationError = "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>\n" +
                                    "        <malformed-request/>\n" +
                                    "      </failure>";
                            outputConsumer.consumeMessage(negotiationError.getBytes());
                            return ParserResponse.NEGOTIATION_ERROR;
                        }
                        System.out.println("Connection with client was a SUCCESS");
                        authorization = authorizationBuilder.toString();
                        while (parser.hasNext() && status != AsyncXMLStreamReader.EVENT_INCOMPLETE)
                            next();
                        return ParserResponse.NEGOTIATION_END;

                }
            }

        }
        outputConsumer.consumeMessage(readXML.toString().getBytes());
        return ParserResponse.EVERYTHING_NORMAL;
    }
}
