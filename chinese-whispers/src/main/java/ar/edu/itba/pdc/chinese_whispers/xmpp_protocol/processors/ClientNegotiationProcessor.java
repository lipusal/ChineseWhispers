package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import java.util.Map;

/**
 * Created by jbellini on 13/11/16.
 */
public class ClientNegotiationProcessor extends BaseNegotiationProcessor {

    private static final String PARTIAL_INITIAL_MESSAGE = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<stream:stream " +
            "xmlns:stream=\'http://etherx.jabber.org/streams\' " +
            "xmlns=\'jabber:client\' " +
            "xmlns:xml=\'http://www.w3.org/XML/1998/namespace\'";


    private boolean initialMessageSent;

    public ClientNegotiationProcessor(OutputConsumer outputConsumer,
                                      String authentication,
                                      Map<String, String> initialParameters) {
        super(outputConsumer, new ClientNegotiationStateMachine());
        initialMessageSent = false;
        setBuiltAuthentication(authentication);
        getInitialParameters().putAll(initialParameters); // The super class initial parameters field is final
    }


    private boolean initialMessageWasSent() {
        return initialMessageSent;
    }


    public void sendInitialMessage() {
        getStringBuilder().setLength(0); // Clears the string builder
        getStringBuilder().append(PARTIAL_INITIAL_MESSAGE);

        // Adds to the initial message all parameters sent by the client connected to the proxy
        for (String attributeKey : getInitialParameters().keySet()) {
            getStringBuilder().append(" ")
                    .append(attributeKey)
                    .append("=\'")
                    .append(getInitialParameters().get(attributeKey))
                    .append("\'");
        }
        getStringBuilder().append(">");
        outputConsumer.consumeMessage(getStringBuilder().toString().getBytes()); // TODO: check if enough space
        initialMessageSent = true;
        getStateMachine().negotiate();
    }



    private static class ClientNegotiationStateMachine extends BaseNegotiationProcessor.NegotiationStateMachine {

        /**
         * Constructor.
         */
        protected ClientNegotiationStateMachine() {
            super(new InitialStreamState());
        }


        private boolean initialMessageWasSent() {
            return ((ClientNegotiationProcessor) getNegotiationProcessor()).initialMessageWasSent();
        }


        private String getAuthentication() {
            return getNegotiationProcessor().getAuthentication();
        }

        private static abstract class BaseState extends BaseNegotiationProcessor.NegotiationStateMachine.State {

            private BaseState(ClientNegotiationStateMachine stateMachine) {
                this();
                if (getStateMachine() == null && stateMachine != null) {
                    setStateMachine(stateMachine);
                }
            }

            private BaseState() {
                super();
            }

            protected String getAuthentication() {
                return ((ClientNegotiationStateMachine) getStateMachine()).getAuthentication();
            }


            protected boolean initialMessageWasSent() {
                return ((ClientNegotiationStateMachine) getStateMachine()).initialMessageWasSent();
            }


        }

        /**
         *
         */
        private static class InitialStreamState extends BaseState {

            @Override
            ParserResponse action() {
                if (!initialMessageWasSent()) {
                    throw new IllegalStateException("Can't process this state till the initial message is sent.");
                }
                getStateMachine().setState(new AuthState((ClientNegotiationStateMachine) getStateMachine()));
                return ParserResponse.EVERYTHING_NORMAL;
            }
        }


        private static class AuthState extends BaseState {

            private final static String PARTIAL_AUTH_RESPONSE = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' " +
                    "mechanism='PLAIN'>";


            private final StringBuilder authMechanisms;

            private boolean isMechanism;

            private boolean streamError;

            private AuthState(ClientNegotiationStateMachine stateMachine) {
                super(stateMachine);
                authMechanisms = new StringBuilder();
                this.isMechanism = true;
                this.streamError = true;
            }


            @Override
            ParserResponse action() {
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
                        break;
                    case AsyncXMLStreamReader.START_ELEMENT:
                        switch (getParser().getLocalName()) {
                            case "error":
                                streamError = true;
                                break;
                            case "host-gone":
                            case "host-unknown":
                                response = ParserResponse.HOST_UNKNOWN;
                                System.out.println("host unknown");
                                break;
                            case "mechanism":
                                isMechanism = true;
                        }
                        break;
                    case AsyncXMLStreamReader.CHARACTERS:
                        if (isMechanism) {
                            authMechanisms.append(getParser().getText());
                        }
                        break;
                    case AsyncXMLStreamReader.END_ELEMENT:
                        if (!isMechanism) {
                            break;
                        }
                        if (!authMechanisms.toString().toUpperCase().trim().equals("PLAIN")) {
                            // No more mechanisms...
                            if (getParser().getLocalName().equals("mechanisms")) {
                                response = ParserResponse.UNSUPPORTED_NEGOTIATION_MECHANISM;
                                break;
                            }
                            authMechanisms.setLength(0); // Clears the string buffer to get next auth mechanism.
                            break;
                        }
                        isMechanism = false;
                        getStringBuilder().setLength(0);
                        getStringBuilder().append(PARTIAL_AUTH_RESPONSE)
                                .append(getAuthentication())
                                .append("</auth>");
                        consumeOutput(getStringBuilder().toString().getBytes());
                        getStateMachine()
                                .setState(new ChallengeState((ClientNegotiationStateMachine) getStateMachine()));
                        break;
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return ParserResponse.EVENT_INCOMPLETE;
                    default:
                        response = ParserResponse.XML_ERROR;
                }
                return response;
            }
        }

        private static class ChallengeState extends BaseState {


            private ChallengeState(ClientNegotiationStateMachine stateMachine) {
                super(stateMachine);
            }

            @Override
            ParserResponse action() {
                ParserResponse response = ParserResponse.EVERYTHING_NORMAL;

                switch (getProcessorStatus()) {
                    case AsyncXMLStreamReader.START_ELEMENT:
                        switch (getParser().getLocalName()) {
                            case "success":
                                getStateMachine().setState(new FinalState());
                                response = ParserResponse.NEGOTIATION_END;
                                break;
                            case "failure":
                                response = ParserResponse.FAILED_NEGOTIATION;
                                break;
                            case "challenge":
                                response = ParserResponse.UNSUPPORTED_NEGOTIATION_MECHANISM;
                                break;

                        }
                        break;

                    case AsyncXMLStreamReader.END_ELEMENT:
                    case AsyncXMLStreamReader.CHARACTERS:
                        break;
                    case AsyncXMLStreamReader.EVENT_INCOMPLETE:
                        return ParserResponse.EVENT_INCOMPLETE;
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
}
