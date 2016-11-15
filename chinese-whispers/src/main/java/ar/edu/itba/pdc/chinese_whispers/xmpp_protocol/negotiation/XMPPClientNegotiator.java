package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation;


import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.Map;

/**
 * Created by Droche on 30/10/2016.
 */
@Deprecated
public class XMPPClientNegotiator extends XMPPNegotiator {//TODO DELETE class


    /**
     * Constructs a new XMPP client negotiator.
     *
     * @param outputConsumer    The object that will consume output messages.
     * @param authorization
     * @param initialParameters
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
                        return ParserResponse.UNSUPPORTED_NEGOTIATION_MECHANISM;//TODO unsupported operation?
                    }
                    if (negotiationStatus == NegotiationStatus.AUTH) {
                        if(parser.getLocalName().equals("mechanism")){
                            negotiationStatus=NegotiationStatus.AUTH_2;
                        }
                    }
                    break;
                case AsyncXMLStreamReader.CHARACTERS:
                    if (negotiationStatus == NegotiationStatus.AUTH_2) {
                        //Update status when starting a non-nested element
                        authorizationBuilder.append(parser.getText());
                    }
                    break;
                case AsyncXMLStreamReader.END_ELEMENT:
                    if(negotiationStatus == NegotiationStatus.AUTH_2){
                        if (authorizationBuilder.toString().contains("PLAIN")) {//TODO contains vs equals. Do we even check this?
                            String response = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' " +
                                    "mechanism='PLAIN'>" + authorization + "</auth>\n";
                            negotiationStatus = NegotiationStatus.CHALLENGE;
//                            System.out.println("Proxy to Server:" + response);
                            outputConsumer.consumeMessage(response.getBytes());
                        }else{
                            //TODO mechanism not suported?
                        }
                    }
                    break;

                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return ParserResponse.EVENT_INCOMPLETE;
                case -1:
                    //TODO throw exception?
                    logger.warn("XML interpreter entered error state (invalid XML)");   //TODO for which connection?
                    return ParserResponse.XML_ERROR;

            }
        }
//        System.out.println(readXML);
        outputConsumer.consumeMessage(readXML.toString().getBytes());
        return ParserResponse.EVERYTHING_NORMAL;
    }

}

