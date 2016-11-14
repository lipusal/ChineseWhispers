package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import java.util.*;

/**
 * This class is in charge of performing the negotiation process with XMPP clients.
 * <p>
 * Created by jbellini on 11/11/16.
 */
public class ServerNegotiationProcessor extends BaseNegotiationProcessor {



    /**
     * Constructor.
     *
     * @param outputConsumer An object to which data must be sent when processing the negotiation.
     */
    public ServerNegotiationProcessor(OutputConsumer outputConsumer) {
        super(outputConsumer, new ServerNegotiationStateMachine());
    }




    /**
     * This class represents a state machine which can handle all the negotiation process with xmpp clients.
     */
    private static class ServerNegotiationStateMachine extends BaseNegotiationProcessor.NegotiationStateMachine {


        /**
         * Constructor.
         * Note: The initial state is {@link PrologueState} state.
         */
        private ServerNegotiationStateMachine() {
            super(new PrologueState());
        }

        private void setBuiltAuthentication(String builtAuthentication) {
            ((ServerNegotiationProcessor) getNegotiationProcessor()).setBuiltAuthentication(builtAuthentication);
        }

        private Map<String, String> getInitialParameters() {
            return getNegotiationProcessor().getInitialParameters();
        }


        private static abstract class BaseState extends BaseNegotiationProcessor.NegotiationStateMachine.State {

            private BaseState(ServerNegotiationStateMachine stateMachine) {
                this();
                if (getStateMachine() == null && stateMachine != null) {
                    setStateMachine(stateMachine);
                }
            }

            private BaseState() {
                super();
            }


            protected void setBuiltAuthentication(String builtAuthentication) {
                ((ServerNegotiationStateMachine) getStateMachine()).setBuiltAuthentication(builtAuthentication);
            }


            protected Map<String, String> getInitialParameters() {
                return ((ServerNegotiationStateMachine) getStateMachine()).getInitialParameters();
            }
        }

        /**
         * The {@link ServerNegotiationStateMachine} initial state. It just checks xml document version and encoding
         */
        private static class PrologueState extends BaseState {


            @Override
            protected ParserResponse action() {
                ParserResponse response = ParserResponse.EVERYTHING_NORMAL;
                switch (getProcessorStatus()) {
                    case AsyncXMLStreamReader.START_DOCUMENT:
                        // We check xml version and encoding in order to log any issue.

                        String streamVersion = getParser().getVersion();
                        // If streamVersion is null, we assume 1.0 version.
                        if (streamVersion != null && !streamVersion.equals("1.0")) {
                            System.out.println("Warning: stream version is " + streamVersion); // TODO: use logger
                        }

                        String streamEncoding = getParser().getEncoding();
                        // If streamEncoding is null, we assume UTF-8 encoding.
                        if (streamEncoding != null && !streamEncoding.equals("UTF-8")) {
                            System.out.println("Warning: stream encoding is " + streamEncoding); // TODO: use logger
                        }
                        // After checking the document, proceed as if the status is a START_ELEMENT
                    case AsyncXMLStreamReader.START_ELEMENT:
                        getStateMachine().setState(new StreamState((ServerNegotiationStateMachine) getStateMachine()));
                        break;
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        break;
                    default:
                        response = ParserResponse.XML_ERROR;
                }
                return response;
            }
        }

        /**
         * State in charge of receiving the initial stream sent by an xmpp client.
         * It will check that the "to" parameter is present.
         * It also initiates a stream with the client, sending as a feature the plain mechanism for authenticating.
         */
        private static class StreamState extends BaseState {


            private static final String END_OF_STREAM_TAG = "xmlns:stream=\'http://etherx.jabber.org/streams\' " +
                    "xmlns=\'jabber:client\' " +
                    "xmlns:xml=\'http://www.w3.org/XML/1998/namespace\'>";
            private static final String FEATURES = "<stream:features> " +
                    "<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'> " +
                    "<mechanism>PLAIN</mechanism> " +
                    "</mechanisms> " +
                    "</stream:features>";


            /**
             * Attributes added by this state to the response stream.
             */
            private final Set<String> ownAddedAttributes;


            private StreamState(ServerNegotiationStateMachine stateMachine) {
                super(stateMachine);
                this.ownAddedAttributes = new HashSet<>();
                // Aalto excludes namespace definitions
                ownAddedAttributes.add("id");
            }

            @Override
            protected ParserResponse action() {
                ParserResponse response = ParserResponse.EVERYTHING_NORMAL;

                switch (getProcessorStatus()) {
                    case AsyncXMLStreamReader.START_ELEMENT:
                        if (!getParser().getLocalName().equals("stream")) {
                            return ParserResponse.XML_ERROR;
                        }

                        getStringBuilder().append("<?xml version=\'1.0\' encoding=\'UTF-8\'?>")
                                .append("<stream:stream");
                        int attrCount = getParser().getAttributeCount();
                        if (attrCount > 0) {
                            getStringBuilder().append(" ");

                            StringBuilder attributeFullName = new StringBuilder();
                            for (int i = 0; i < attrCount; i++) {
                                attributeFullName.setLength(0); // Clears the string buffer

                                // Ignore those attributes sent by us
                                if (ownAddedAttributes.contains(getParser().getAttributeLocalName(i).toLowerCase())) {
                                    continue;
                                }

                                if (!getParser().getAttributePrefix(i).isEmpty()) {
                                    attributeFullName.append(getParser().getAttributePrefix(i)).append(":");
                                }
                                attributeFullName.append(getParser().getAttributeLocalName(i));
                                switch (attributeFullName.toString()) {
                                    case "to":
                                        getStringBuilder().append("from");
                                        break;
                                    case "from":
                                        getStringBuilder().append("to");
                                        break;
                                    default:
                                        getStringBuilder().append(attributeFullName);
                                }
                                getStringBuilder().append("=\'")
                                        .append(getParser().getAttributeValue(i))
                                        .append("\'")
                                        .append(i < attrCount - 1 ? " " : "");
                                if (!getInitialParameters().containsKey(attributeFullName.toString())) {
                                    getInitialParameters().put(attributeFullName.toString(), getParser().getAttributeValue(i));
                                }
                            }

                        }
                        if (!getInitialParameters().containsKey("to")) {
                            return ParserResponse.HOST_UNKNOWN; // Do not continue if missing "to" param
                        }
                        getStringBuilder().append(" id=\'")
                                .append(IdGenerator.generateId())
                                .append("\' ");                     // The id param.
                        getStringBuilder().append(END_OF_STREAM_TAG);    // The rest of the stream tag
                        getStringBuilder().append(FEATURES);             // Features

                        consumeOutput(getStringBuilder().toString().getBytes());
                        getStateMachine().setState(new AuthState((ServerNegotiationStateMachine) getStateMachine()));
                        break;
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        break;
                    default:
                        response = ParserResponse.XML_ERROR;
                }
                return response;
            }
        }

        private static class AuthState extends BaseState {


            /**
             * The {@link StringBuilder} that will build the authentication text string.
             */
            private final StringBuilder authenticationBuilder = new StringBuilder();

            private boolean validMechanism = false;


            private AuthState(ServerNegotiationStateMachine stateMachine) {
                super(stateMachine);
            }

            @Override
            ParserResponse action() {
                ParserResponse response = ParserResponse.EVERYTHING_NORMAL;

                switch (getProcessorStatus()) {
                    case AsyncXMLStreamReader.START_ELEMENT:
                        if (!getParser().getLocalName().equals("auth")) {
                            return response;
                        }
                        int attrCount = getParser().getAttributeCount();
                        if (attrCount > 0) {
                            StringBuilder attributeFullName = new StringBuilder();
                            for (int i = 0; i < attrCount; i++) {
                                attributeFullName.setLength(0); // Clears the string buffer

                                if (!getParser().getAttributePrefix(i).isEmpty()) {
                                    attributeFullName.append(getParser().getAttributePrefix(i)).append(":");
                                }
                                attributeFullName.append(getParser().getAttributeLocalName(i));

                                if (attributeFullName.toString().equals("mechanism")) {
                                    String mechanismValue = getParser().getAttributeValue(i);
                                    if (mechanismValue.toUpperCase().equals("PLAIN")) {
                                        validMechanism = true;
                                    }
                                }
                            }
                        }
                        if (!validMechanism) {
                            return ParserResponse.INVALID_AUTH_MECHANISM;
                        }
                        authenticationBuilder.setLength(0);
                        break;
                    case AsyncXMLStreamReader.CHARACTERS:
                        if (validMechanism) {
                            authenticationBuilder.append(getParser().getText());
                        }
                        break;
                    case AsyncXMLStreamReader.END_ELEMENT:
                        String builtAuthentication = authenticationBuilder.toString().trim();

                        setBuiltAuthentication(builtAuthentication);
                        // Checks that something was sent in the auth body
                        if (builtAuthentication.isEmpty()) {
                            return ParserResponse.MALFORMED_REQUEST;
                        }
                        String decodedAuth;
                        // Checks that the body has a correct base64 scheme
                        try {
                            // The authorization content might be invalid (i.e. not be a valid base64 scheme)
                            decodedAuth = new String(Base64.getDecoder().decode(builtAuthentication));
                        } catch (IllegalArgumentException e) {
                            return ParserResponse.MALFORMED_REQUEST;
                        }
                        // Checks that the decoded string can be separated in 2 or 3 elements separated by a zero.
                        int authParametersAmount = decodedAuth.split("\0").length;
                        if (authParametersAmount != 2 && authParametersAmount != 3) {
                            return ParserResponse.MALFORMED_REQUEST;
                        }

                        getStateMachine().setState(new FinalState());
                        response = ParserResponse.NEGOTIATION_END;
                        break;
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        break;
                    default:
                        response = ParserResponse.XML_ERROR;
                }
                return response;
            }
        }

        private static class FinalState extends BaseState {


            @Override
            ParserResponse action() {
                return ParserResponse.NEGOTIATION_END;
            }
        }

    }

    /**
     * This class implements a method to get different ids each time it is called.
     */
    private static class IdGenerator {


        /**
         * This set contains all ids already used
         */
        private final static Set<String> usedIds = new HashSet<>();

        /**
         * Random to get random values
         */
        private final static Random random = new Random();

        /**
         * To get different ids each time, when generating a random value,
         * it is checked if the {@code usedIds} set contains that value.
         * If the set contains the generated value, another value is generated.
         * It might happen (but it is not probable) to enter an infinite loop,
         * so the process is repeated, at most, the amount of times this constant states.
         */
        private final static int MAX_RANDOM_TRIES = 100;
        /**
         * In case the count reached the {@link IdGenerator#MAX_RANDOM_TRIES} value,
         * a fallback strategy is done.
         */
        private static int fallbackIds = 0;


        /**
         * Generates a random and unique id.
         *
         * @return A Stream Id (RFC 6120, section 4.7.3) for the response stream.
         */
        private static String generateId() {

            String result;
            int count = 0;
            do {
                long aux = random.nextLong();
                if (aux < 0) {
                    aux *= -1; // Non negative ids.
                }
                result = String.valueOf(aux);
                count++;
            }
            while (usedIds.contains(result) && count < MAX_RANDOM_TRIES);
            if (count >= MAX_RANDOM_TRIES) {
                result = "NotRandom" + String.valueOf(fallbackIds++);
            }
            usedIds.add(result);
            return result;
        }
    }
}
