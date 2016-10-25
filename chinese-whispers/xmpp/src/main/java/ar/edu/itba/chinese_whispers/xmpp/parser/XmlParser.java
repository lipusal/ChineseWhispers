package ar.edu.itba.chinese_whispers.xmpp.parser;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncByteBufferFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Created by juanlipuma on Oct/24/16.
 */
public class XmlParser implements /*Consumer<Byte[]>,*/ Supplier<String> {

//    private final ReaderConfig config = new ReaderConfig();
//
//    private XMLStreamReader2 readerr = new StreamReaderImpl(new Utf8Scanner());

    private final AsyncXMLInputFactory inputFactory;
    private final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser;

    private int status = 0;
    private int depth = 0;

    //TODO decide whether to use this
    private AsyncXMLStreamReader<AsyncByteBufferFeeder> parser2;

    public XmlParser() throws XMLStreamException {
        inputFactory = new InputFactoryImpl();
        parser = inputFactory.createAsyncForByteArray();

        //TODO delete if unused
        parser2 = inputFactory.createAsyncForByteBuffer();
    }

    public XmlParser(byte[] initialData) throws XMLStreamException {
        inputFactory = new InputFactoryImpl();
        parser = inputFactory.createAsyncFor(initialData);
        next();
    }

    public void feed(byte[] data) throws XMLStreamException {
        parser.getInputFeeder().feedInput(data, 0, data.length);
    }

    public void feed(ByteBuffer buffer) throws XMLStreamException {
        parser2.getInputFeeder().feedInput(buffer);
    }

    public boolean hasData() {
        try {
            return parser.hasNext() /*&& status != AsyncXMLStreamReader.EVENT_INCOMPLETE*/;
        } catch (XMLStreamException e) {
            //TODO log this or something
            e.printStackTrace();
            return false;
        }
    }

    public String nextStanza() {
        if (!hasData()) {
            return null;
        }
        boolean done = false;
        while (hasData() && !done) {
            next();
            switch (status) {
                case AsyncXMLStreamReader.START_ELEMENT:
                    depth++;
                    System.out.print(parser.getName());
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        System.out.print(parser.getAttributeName(i) + "=" + parser.getAttributeValue(i));
                    }
                    try {
                        if (parser.isEmptyElement()) {
                            System.out.println("Empty element");
                        }
                    } catch (XMLStreamException e) {
                        e.printStackTrace();
                    }
                    break;
                case AsyncXMLStreamReader.CHARACTERS:
                    System.out.print(parser.getText());
                    break;
                case AsyncXMLStreamReader.END_ELEMENT:
                    if (--depth == 0) {
                        done = true;
                    }
                    System.out.println("End " + parser.getName());
                    break;
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    done = true;
                    return null;
                case -1:
                    return "Error";
            }
        }

        return null;
//        QName qualifiedName = parser.getName();
//        System.out.println("Read " + qualifiedName.getLocalPart() + " element");
//        return qualifiedName.getLocalPart();
    }

    /*@Override
    public void accept(Byte[] bytes) {
        feed(bytes);
    }*/

    @Override
    public String get() {
        return null;
    }

    public static void main(String[] args) throws XMLStreamException, UnsupportedEncodingException {
        XmlParser p = new XmlParser("<root caca='caca'><inner>HELLO WORLD</inner><empty /></root>".getBytes());
        System.out.println(p.nextStanza());
        System.out.println(p.nextStanza());
    }

    private int next() {
        try {
            status = parser.next();
        } catch (XMLStreamException e) {
            e.printStackTrace();
            status = -1;
        }
        return status;
    }
}
