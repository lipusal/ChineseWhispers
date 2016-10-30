package ar.edu.itba.pdc.chinese_whispers.xml;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.ParserResponse;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Created by Estela on 30/10/2016.
 */
public abstract class XMPPNegotiator {
    protected final AsyncXMLInputFactory inputFactory;
    protected final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser;

    protected Map<String,String> initialParameters;
    protected String authorization;

    protected int status = 0;
    protected NegotiationStatus negotiationStatus;
    protected Deque<Byte> output;

    //Example usage. TODO remove.
//    public static void main(String[] args) throws XMLStreamException {
//        Deque<Byte> deque = new ArrayDeque<>();
//        XmlInterpreter i = new XmlInterpreter(deque);
//        i.setL337ed(true);
//        i.feed("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><message><body>hola ke ase me gusta mucho la papa y comer todos los días es sano l0l0l0lolololol</body></message>".getBytes());
//        System.out.println("Processed " + i.process() + " bytes");
//        while(!deque.isEmpty()) {
//            System.out.print((char)(byte)deque.poll());
//        }
//    }

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
            return ParserResponse.XML_ERROR;
        }


    }

    /**
     * @return Whether this interpreter has data left to read.
     */
    protected boolean hasData() {
        try {
            return parser.hasNext();
        } catch (XMLStreamException e) {
            //TODO log this or something
            e.printStackTrace();
            return false;
        }
    }

    protected abstract ParserResponse process();


    /**
     * Checks whether this interpreter is in error state. An error state is reached when reading invalid XML, at which
     * point the interpreter becomes invalid and stops processing the stream further. The stream may be considered
     * invalid and discarded.
     *
     * @return Whether this interpreter is in error state.
     */
    protected boolean isInErrorState() {
        return status == -1;
    }

    /**
     * Reads until the next XML event, as specified by {@link AsyncXMLStreamReader#next()}.
     *
     * @return The current event code.
     */
    protected int next() {
        try {
            status = parser.next();
        } catch (XMLStreamException e) {
            e.printStackTrace();
            status = -1;
        }
        return status;
    }
}
