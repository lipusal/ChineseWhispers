package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation;


import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.enums.ParserResponse;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.Deque;
import java.util.Map;

/**
 * Created by Droche on 30/10/2016.
 */
public class XMPPClientNegotiator extends XMPPNegotiator { //TODO checkear si no hay que mandar el STLTST paa que ande.


    /**
     * Constructs a new interpreter.
     *
     * @param output Where to send processed output.
     */
    public XMPPClientNegotiator(Deque<Byte> output, String autorization, Map initialParameters) {
        super(output);
        if(autorization==null || initialParameters==null) throw new IllegalArgumentException();
        this.authorization = autorization;
        this.initialParameters = initialParameters;
        this.negotiationStatus=NegotiationStatus.AUTH;
    }

    public XMPPClientNegotiator(Deque<Byte> output){

        super(output);
        this.negotiationStatus=NegotiationStatus.AUTH;
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
            if(negotiationStatus==NegotiationStatus.AUTH){
                switch (status) {
                    case AsyncXMLStreamReader.CHARACTERS:
                        //Update status when starting a non-nested element
                        String text =  parser.getText();
                        if(text.equals("PLAIN")){
                            String response = "<auth xmlns='urn:ietf:params:xml_parser:ns:xmpp-sasl' " +
                                    "mechanism='PLAIN'>"+authorization+"</auth>";
                            negotiationStatus=NegotiationStatus.CHALLENGE;
                            System.out.println("Proxy to Server:" + response);
                            byte[] bytes = response.getBytes();
                            for (byte b : bytes) {
                                output.offer(b);
                            }
                            while (parser.hasNext()  && status!=AsyncXMLStreamReader.EVENT_INCOMPLETE)next(); //TODO handle more?
                            return ParserResponse.EVERYTHING_NORMAL;
                        }
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return ParserResponse.EVERYTHING_NORMAL;
                    case -1:
                        //TODO throw exception? Remove sout
                        System.out.println("XML interpreter entered error state (invalid XML)");
                        return ParserResponse.XML_ERROR;
                }
                //TODO do what if NO PLAIN?

            }else  if(negotiationStatus==NegotiationStatus.CHALLENGE){
                switch (status) { //TODO check it is really plain and not other shit
                    case AsyncXMLStreamReader.START_ELEMENT:

                        if(parser.getLocalName().equals("success")){
                            System.out.println("Connection with server was a SUCCESS");
                            return ParserResponse.NEGOTIATION_END;
                        }
                        //TODO see other cases and Negotiation.
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
        byte[] bytes = readXML.toString().getBytes();
        for (byte b : bytes) {
            output.offer(b);
        }
        return ParserResponse.EVERYTHING_NORMAL;
    }

}

