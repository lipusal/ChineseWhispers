package ar.edu.itba.pdc.chinese_whispers.parser;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

import javax.xml.stream.XMLStreamException;
import java.io.UnsupportedEncodingException;

/**
 * Basic XML parser that interprets start and end of elements, and recognizes when more bytes are needed to complete
 * an element, character, etc.
 */
public class XmlParser /*implements Consumer<Byte[]>, Supplier<String>*/ {

    private final AsyncXMLInputFactory inputFactory;
    private final AsyncXMLStreamReader<AsyncByteArrayFeeder> parser;

    private int status = 0;
    private boolean isSilenced;
    private boolean isL337ed = true;
    private boolean isInBodyTag = false;
    private StringBuilder readXML = new StringBuilder();

    public XmlParser() throws XMLStreamException {
        inputFactory = new InputFactoryImpl();
        parser = inputFactory.createAsyncForByteArray();
    }

    public XmlParser(byte[] initialData) throws XMLStreamException {
        inputFactory = new InputFactoryImpl();
        parser = inputFactory.createAsyncFor(initialData);
        next();
    }

    public void feed(byte[] data) throws XMLStreamException {
        parser.getInputFeeder().feedInput(data, 0, data.length);
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

    public void parse() {
        if (!hasData()) {
            return;
        }
        while (hasData()) {
            next();
            switch (status) {
                case AsyncXMLStreamReader.START_ELEMENT:
                    if (parser.getLocalName().equals("body")) {
                        isInBodyTag = true;
                    }
                    readXML.append("<");
                    if (!parser.getName().getPrefix().isEmpty()) {
                        readXML.append(parser.getPrefix()).append(":");
                    }
                    readXML.append(parser.getLocalName());
                    int attrCount = parser.getAttributeCount();
                    if (attrCount > 0) {
                        readXML.append(" ");
                        for (int i = 0; i < attrCount; i++) {
                            readXML.append(parser.getAttributeName(i)).append("=\"").append(parser.getAttributeValue(i)).append(i < attrCount - 1 ? "\" " : "\"");
                        }
                    }
                    readXML.append(">");
                    break;
                case AsyncXMLStreamReader.CHARACTERS:
                    if (isInBodyTag && isL337ed) {
                        for (char c : parser.getTextCharacters()) {
                            switch (c) {
                                case 'a':
                                    readXML.append("4");
                                    break;
                                case 'e':
                                    readXML.append("3");
                                    break;
                                case 'i':
                                    readXML.append("1");
                                case 'o':
                                    readXML.append("0");
                                    break;
                                case 'c':
                                    readXML.append("&lt;");
                                    break;
                                default:
                                    readXML.append(c);
                                    break;
                            }
                        }
                    } else {
                        readXML.append(parser.getText());
                    }
                    break;
                case AsyncXMLStreamReader.END_ELEMENT:
                    if (parser.getLocalName().equals("body")) {
                        isInBodyTag = false;
                    }
                    readXML.append("</");
                    if (!parser.getName().getPrefix().isEmpty()) {
                        readXML.append(parser.getPrefix()).append(":");
                    }
                    readXML.append(parser.getLocalName());
                    readXML.append(">");
                    break;
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    return;
                case -1:
                    //TODO set error state
                    System.out.println("EXPLOTAR POR LOS AIRES!");
                    return;
            }
        }
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

    public static void main(String[] args) throws XMLStreamException, UnsupportedEncodingException {
        XmlParser parser = new XmlParser("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><message><body>hola ke ase me gusta mucho la papa y comer todos los días es sano l0l0l0lolololol</body></message>".getBytes());
        parser.parse();
        System.out.println(parser.readXML.toString());
        parser.parse();
        System.out.println(parser.readXML.toString());
    }

    /**
     * @return Whether this stream is silenced. Silenced streams discard all <message> stanzas.
     */
    public boolean isSilenced() {
        return isSilenced;
    }

    /**
     * Sets whether this stream is silenced. Silenced streams discard all <message> stanzas.
     *
     * @param silenced Whether this stream is silenced.
     */
    public void setSilenced(boolean silenced) {
        isSilenced = silenced;
    }

    /**
     * @return Whether this stream is "leeted." Leeted streams transform certain alphabetic characters inside <body>
     *          stanzas into similar-looking numbers.
     */
    public boolean isL337ed() {
        return isL337ed;
    }

    /**
     * Sets whether this stream is "leeted." Leeted streams transform certain alphabetic characters inside <body>
     * stanzas into similar-looking numbers.
     *
     * @param l337ed Whether this stream is leeted.
     */
    public void setL337ed(boolean l337ed) {
        isL337ed = l337ed;
    }
}
