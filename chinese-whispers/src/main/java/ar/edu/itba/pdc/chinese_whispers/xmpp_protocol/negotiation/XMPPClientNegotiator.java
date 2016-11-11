package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation;


import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.Map;

/**
 * Created by Droche on 30/10/2016.
 */
public class XMPPClientNegotiator extends XMPPNegotiator { //TODO checkear si no hay que mandar el STLTST paa que ande.


    /**
     * Constructs a new XMPP client negotiator.
     *
     * @param outputConsumer    The object that will consume output messages.
     * @param authorization     TODO: Diego, completa esto por favor
     * @param initialParameters TODO: Diego, completa esto por favor
     */
    public XMPPClientNegotiator(OutputConsumer outputConsumer,
                                String authorization, Map<String, String> initialParameters) {
        this(outputConsumer);
        if (authorization == null || initialParameters == null) {
            throw new IllegalArgumentException();
        }
        this.authorization = authorization; // TODO: If the other constructor is called, this field will be null.
        this.initialParameters = initialParameters;

    }

    /**
     * Constructs a new XMPP client negotiator.
     *
     * @param outputConsumer The object that will consume output essages.
     */
    public XMPPClientNegotiator(OutputConsumer outputConsumer) {
        super(outputConsumer);
        this.negotiationStatus = NegotiationStatus.AUTH;
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
            switch (status) {
                case AsyncXMLStreamReader.START_ELEMENT:
                    if (negotiationStatus == NegotiationStatus.CHALLENGE) {
                        if (parser.getLocalName().equals("success")) {
//                            System.out.println("Connection with server was a SUCCESS");
                            return ParserResponse.NEGOTIATION_END;
                        }
                    }
                    if (parser.getLocalName().equals("challenge")) {
                        return ParserResponse.NEGOTIATION_ERROR;//TODO unsupported operation?
                    }
                    break;
                case AsyncXMLStreamReader.CHARACTERS:
                    if (negotiationStatus == NegotiationStatus.AUTH) {
                        //Update status when starting a non-nested element
                        String text = parser.getText();
                        if (text.equals("PLAIN")) {//TODO contains vs equals. Do we even check this?
                            String response = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' " +
                                    "mechanism='PLAIN'>" + authorization + "</auth>\n";
                            negotiationStatus = NegotiationStatus.CHALLENGE;
//                            System.out.println("Proxy to Server:" + response);
                            outputConsumer.consumeMessage(response.getBytes());
                        }
                    }
                    break;
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return ParserResponse.EVERYTHING_NORMAL;
                case -1:
                    //TODO throw exception? Remove sout
//                    System.out.println("XML interpreter entered error state (invalid XML)");
                    return ParserResponse.XML_ERROR;


            }

        }
//        System.out.println(readXML);
        outputConsumer.consumeMessage(readXML.toString().getBytes());
        return ParserResponse.EVERYTHING_NORMAL;
    }

}

