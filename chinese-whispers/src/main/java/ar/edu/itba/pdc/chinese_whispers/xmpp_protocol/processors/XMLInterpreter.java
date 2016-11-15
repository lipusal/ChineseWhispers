package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors;

import ar.edu.itba.pdc.chinese_whispers.application.LogHelper;
import ar.edu.itba.pdc.chinese_whispers.application.MetricsManager;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.XMPPReadWriteHandler;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.ApplicationProcessor;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import org.slf4j.Logger;

import javax.xml.stream.XMLStreamException;

/**
 * Basic byte-level XML interpreter. Handles reading incomplete and invalid XML, as well as "l33ting" messages when
 * appropriate, and ignoring messages when silenced.
 */
public class XMLInterpreter extends BaseXMLInterpreter {

    private int status = 0;
    private boolean isSilenced;
    private boolean silenceRequested;
    private boolean isInBodyTag;
    private boolean isInMessageTag;


    /**
     * Object that will perform data processing.
     */
    private final ApplicationProcessor applicationProcessor;

    /**
     * The {@link XMPPReadWriteHandler} that owns this handler.
     */
    private final XMPPReadWriteHandler ownerHandler;

    private Logger logger;

    /**
     * Constructs a new interpreter.
     *
     * @param applicationProcessor Object that will perform data processing.
     * @param outputConsumer       The object that will consume output (i.e. parsed) data.
     */
    public XMLInterpreter(ApplicationProcessor applicationProcessor, OutputConsumer outputConsumer,
                          XMPPReadWriteHandler ownerHandler) {
        super(outputConsumer);
        this.applicationProcessor = applicationProcessor;
        logger = LogHelper.getLogger(getClass());
        this.ownerHandler = ownerHandler;
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
            status = parser.next();
            updateStoredBytes(status);

            switch (status) {
                case AsyncXMLStreamReader.START_ELEMENT:
                    //Update status when starting a non-nested element
                    if (parser.getDepth() <= 2) {
                        isSilenced = silenceRequested;
                    }
                    if (parser.getLocalName().equals("body")) {
                        isInBodyTag = true;
                    } else if (parser.getLocalName().equals("message")) {
                        isInMessageTag = true;
                    }

                    // Only process content if NOT message tag or NOT silenced
                    if (!(isInMessageTag && isSilenced)) {
                        readXML.append("<");
                        //Name (and namespace prefix if necessary)
                        if (!parser.getName().getPrefix().isEmpty()) {
                            appendEscapedCharacters(readXML,parser.getPrefix());
                            readXML.append(":");
                        }
                        readXML.append(parser.getLocalName());

                        // Namespaces
                        int namespaceCount = parser.getNamespaceCount();
                        if (namespaceCount > 0) {
                            readXML.append(" ");
                            for (int i = 0; i < namespaceCount; i++) {
                                readXML.append("xmlns");
                                if (!parser.getNamespacePrefix(i).isEmpty()) {
                                    readXML.append(":");
                                    appendEscapedCharacters(readXML,parser.getNamespacePrefix(i));
                                }
                                readXML.append("=\'");
                                appendEscapedCharacters(readXML,parser.getNamespaceURI(i));
                                readXML.append("\'")
                                        .append(i < namespaceCount - 1 ? " " : "");
                            }
                        }

                        // Attributes (with namespace prefixes if necessary)
                        int attrCount = parser.getAttributeCount();
                        if (attrCount > 0) {
                            readXML.append(" ");
                            for (int i = 0; i < attrCount; i++) {
                                if (!parser.getAttributePrefix(i).isEmpty()) {
                                    appendEscapedCharacters(readXML,parser.getAttributePrefix(i));
                                    readXML.append(":");
                                }
                                appendEscapedCharacters(readXML,parser.getAttributeLocalName(i));
                                readXML.append("=\'");
                                appendEscapedCharacters(readXML,parser.getAttributeValue(i));
                                readXML.append("\'")
                                        .append(i < attrCount - 1 ? " " : "");
                            }
                        }
                        readXML.append(">");
                    } else {
                        if (parser.getLocalName().equals("message")) {
                            //ownerHandler.notifyStanzaError(generateErrorMessage());
                            ownerHandler.consumeMessage(generateErrorMessage().getBytes());
                            MetricsManager.getInstance().addNumSilencedMessages(1); //TODO user producer
                        }
                    }
                    break;
                case AsyncXMLStreamReader.CHARACTERS:
                    //Only process content if NOT message tag or NOT silenced
                    if (!(isInMessageTag && isSilenced)) {
                        //Append l337ed or normal characters as appropriate
                        applicationProcessor.processMessageBody(readXML, parser.getText().toCharArray(), isInBodyTag);
                    }
                    break;
                case AsyncXMLStreamReader.END_ELEMENT:
                    //Only process content if NOT message tag or NOT silenced
                    if (!(isInMessageTag && isSilenced)) {
                        readXML.append("</");
                        if (!parser.getName().getPrefix().isEmpty()) {
                            readXML.append(parser.getPrefix()).append(":");
                        }
                        readXML.append(parser.getLocalName());
                        readXML.append(">");
                    }

                    //Update status
                    if (parser.getLocalName().equals("body")) {
                        isInBodyTag = false;
                    } else if (parser.getLocalName().equals("message")) {
                        isInMessageTag = false;
                    }
                    break;
                case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                    String processedXML = readXML.toString();
                    byte[] bytes = processedXML.getBytes();
                    outputConsumer.consumeMessage(bytes);
                    return ParserResponse.EVENT_INCOMPLETE;
                case -1:
                    logger.warn("XML interpreter {} entered error state (invalid XML)", this);
                    return ParserResponse.XML_ERROR;
            }
        }
        byte[] bytes = readXML.toString().getBytes();
        outputConsumer.consumeMessage(bytes);
        return ParserResponse.EVERYTHING_NORMAL;
    }


    private String generateErrorMessage() {
        StringBuilder silencedErrorBuilder = new StringBuilder();
        silencedErrorBuilder.append("<message");
        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            if (parser.getAttributePrefix(i).isEmpty()) {
                if (parser.getAttributeLocalName(i).equals("to")) {
                    silencedErrorBuilder.append(" from='");
                    appendEscapedCharacters(silencedErrorBuilder,parser.getAttributeValue(i));
                           silencedErrorBuilder.append("'");
                }
                if (parser.getAttributeLocalName(i).equals("from")) {
                    silencedErrorBuilder.append(" to='");
                    appendEscapedCharacters(silencedErrorBuilder,parser.getAttributeValue(i));
                    silencedErrorBuilder.append("'");
                }
                if (parser.getAttributeLocalName(i).equals("id")) {
                    silencedErrorBuilder.append(" id='");
                    appendEscapedCharacters(silencedErrorBuilder,parser.getAttributeValue(i));
                    silencedErrorBuilder.append("'");
                }
            }
        }
        silencedErrorBuilder.append("><error type='wait'><policy-violation xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error></message>");

        return silencedErrorBuilder.toString();
    }



    /**
     * Sets whether this stream is silenced. Silenced streams discard all <message> stanzas.
     * <b>NOTE:</b> This setting takes effect upon reaching the next stanza.
     *
     * @param silenced Whether this stream is silenced.
     */
    public void setSilenced(boolean silenced) {
        silenceRequested = silenced;
    }


}
