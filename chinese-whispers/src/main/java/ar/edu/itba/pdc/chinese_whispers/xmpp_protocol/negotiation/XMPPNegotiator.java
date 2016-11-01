package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.xml_parser.ParserResponse;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Droche on 30/10/2016.
 */
public abstract class XMPPNegotiator {
    protected final AsyncXMLInputFactory inputFactory;
    protected final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser;

    protected Map<String,String> initialParameters;
    protected String authorization;

    protected int status = 0;
    protected NegotiationStatus negotiationStatus;
    protected Deque<Byte> output;




    public Map<String, String> getInitialParameters() {
        return initialParameters;
    }

    public void setInitialParameters(Map<String, String> initialParameters) {
        this.initialParameters = initialParameters;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }

    /**
     * Constructs a new interpreter.
     *
     * @param output Where to send processed output.
     */
    public XMPPNegotiator(Deque<Byte> output) {
        inputFactory = new InputFactoryImpl();
        parser = inputFactory.createAsyncForByteArray();
        this.output = output;
        initialParameters =  new HashMap<>();
    }

    /**
     * Adds bytes to be processed by the interpreter.
     *
     * @param data The data to process.
     */
    public ParserResponse feed(byte[] data) {
        try {
            parser.getInputFeeder().feedInput(data, 0, data.length);
            return process();
        }catch (XMLStreamException e){
            //TODO catch
            e.printStackTrace();
            return ParserResponse.XML_ERROR;
        }


    }



    protected abstract ParserResponse process() throws XMLStreamException;


    /**
     * Reads until the next XML event, as specified by {@link AsyncXMLStreamReader#next()}.
     *
     * @return The current event code.
     */
    protected void next() throws XMLStreamException {
        status = parser.next();
    }
}
