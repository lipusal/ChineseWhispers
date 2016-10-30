package ar.edu.itba.pdc.chinese_whispers.xml;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.ParserResponse;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import java.util.Deque;

/**
 * Created by Estela on 30/10/2016.
 */
public class XMPPServerNegotitator extends XMPPNegotiator {


    /**
     * Constructs a new interpreter.
     *
     * @param output Where to send processed output.
     */
    public XMPPServerNegotitator(Deque<Byte> output) {
        super(output);
        this.negotiationStatus=NegotiationStatus.START;
    }

    /**
     * Processes all fed data. Transforms messages if leeted, ignores messages if silenced, and sets an error state on
     * invalid XML. Sends all processed data to the Deque specified upon instantiation.
     *
     * @return The number of bytes offered to the output Deque, or -1 if the interpreter is in error state.
     */
    @Override
    protected ParserResponse process() {
        if (!hasData()) {
            return ParserResponse.EVERYTHING_NORMAL;
        }
        StringBuilder readXML = new StringBuilder();
        while (hasData()) {
            next();
            if(negotiationStatus==NegotiationStatus.START){
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
                            negotiationStatus=NegotiationStatus.AUTH;
                            System.out.println(readXML);
                            byte[] bytes = readXML.toString().getBytes();
                            for (byte b : bytes) {
                                output.offer(b);
                            }
                            while (hasData() && status!=AsyncXMLStreamReader.EVENT_INCOMPLETE)next(); //TODO handle more?
                            return ParserResponse.EVERYTHING_NORMAL;
                        } else {
                            //TODO handle error?
                        }
                        break;
                    case -1:
                        //TODO throw exception? Remove sout
                        System.out.println("XML interpreter entered error state (invalid XML)");
                        return ParserResponse.XML_ERROR;
                }
            }else  if(negotiationStatus==NegotiationStatus.AUTH){
                switch (status) { //TODO check it is really plain and not other shit
                    case AsyncXMLStreamReader.CHARACTERS:
                        authorization = parser.getText();


                        String response = "<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>";
                        System.out.println(response);
                        System.out.println("Conexion with server was a SUCCESS");
                        byte[] bytes = response.getBytes();
                        for (byte b : bytes) {
                            output.offer(b);
                        }
                        while (hasData()  && status!=AsyncXMLStreamReader.EVENT_INCOMPLETE)next(); //TODO handle more?
                        return ParserResponse.NEGOTIATION_END;
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
