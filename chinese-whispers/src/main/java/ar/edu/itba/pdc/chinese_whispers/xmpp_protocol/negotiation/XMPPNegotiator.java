package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.negotiation;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors.ParserResponse;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Droche on 30/10/2016.
 */
@Deprecated
public abstract class XMPPNegotiator {

    /**
     * Says how many bytes this interpreter can hold at most.
     */
    public final static int MAX_AMOUNT_OF_BYTES = 10 * 1024; // We allow up to 10 KiB data inside the parser.


    protected final AsyncXMLInputFactory inputFactory;
    protected final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser;



    /**
     * TODO: Complete JAVADOC
     */
    protected Map<String, String> initialParameters;
    /**
     * TODO: Complete JAVADOC
     */
    protected String authorization;

    /**
     * TODO: Complete JAVADOC
     */
    protected int status = 0;
    /**
     * TODO: Complete JAVADOC
     */
    protected NegotiationStatus negotiationStatus;


    /**
     * Holds how many bytes the parser has in its internal buffer.
     */
    protected int amountOfStoredBytes;


    /**
     * The object that will consume negotiation messages.
     */
    protected final OutputConsumer outputConsumer;



    protected final StringBuilder authorizationBuilder;

    /**
     * Constructs a new XMPP negotiator.
     *
     * @param outputConsumer The object that will consume output messages.
     */
    public XMPPNegotiator(OutputConsumer outputConsumer) {
        if (outputConsumer == null) {
            throw new IllegalArgumentException();
        }
        inputFactory = new InputFactoryImpl();
        parser = inputFactory.createAsyncForByteArray();
        this.outputConsumer = outputConsumer;
        this.initialParameters = new HashMap<>();
        authorizationBuilder = new StringBuilder();
    }


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
     * Returns how many bytes can be fed to this interpreter.
     *
     * @return The amount of bytes that can be fed to this interpreter
     */
    public int remainingSpace() {
        return (outputConsumer.remainingSpace() - amountOfStoredBytes) / 4;
    }


    /**
     * Adds bytes to be processed by the interpreter.
     *
     * @param data The data to process.
     */
    public ParserResponse feed(byte[] data, int length) {

        // Send one byte

        if (data == null || length < 0 || length > data.length) {
            throw new IllegalArgumentException();
        }
        if (length > remainingSpace()) {
            return ParserResponse.POLICY_VIOLATION;
        }

        // TODO: check repeated code.
        ParserResponse response = ParserResponse.EVERYTHING_NORMAL;
        try {
            for (int offset = 0; offset < length && response != ParserResponse.NEGOTIATION_END; offset++) {
                parser.getInputFeeder().feedInput(data, offset, 1);
                response = process();
                // TODO: check order of this lines...
                if (amountOfStoredBytes >= MAX_AMOUNT_OF_BYTES || parser.getDepth() > 10000) {
                    return ParserResponse.POLICY_VIOLATION;
                }
            }
        } catch (XMLStreamException e) {
            return ParserResponse.XML_ERROR;
        }
        return response;


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
