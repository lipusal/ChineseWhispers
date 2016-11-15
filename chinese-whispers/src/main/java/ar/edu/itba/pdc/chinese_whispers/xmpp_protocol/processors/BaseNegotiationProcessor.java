package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.processors;

import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers.StreamErrorsManager;
import ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.interfaces.OutputConsumer;
import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLStreamReader;

import javax.xml.stream.XMLStreamException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jbellini on 13/11/16.
 */
public abstract class BaseNegotiationProcessor extends BaseXMLInterpreter {


    /**
     * Contains the parameters that were sent in the initiating stream tag.
     */
    private final Map<String, String> initialParameters;
    /**
     * A string builder to generate tags.
     */
    private final StringBuilder stringBuilder;
    /**
     * The plain authentication text.
     */
    private String builtAuthentication = "";
    /**
     * The state machine that will perform action based on the negotiation state.
     */
    private NegotiationStateMachine stateMachine;

    protected BaseNegotiationProcessor(OutputConsumer outputConsumer, NegotiationStateMachine stateMachine) {
        this(outputConsumer);
        if (stateMachine != null) {
            this.stateMachine = stateMachine;
            stateMachine.setNegotiationProcessor(this);
        }
    }

    protected BaseNegotiationProcessor(OutputConsumer outputConsumer) {
        super(outputConsumer);
        this.initialParameters = new HashMap<>();
        this.stringBuilder = new StringBuilder();
        this.stateMachine = null;
    }

    /**
     * Gets the plain authentication text.
     *
     * @return The plain authentication text.
     */
    public String getAuthentication() {
        return builtAuthentication;
    }

    protected void setBuiltAuthentication(String builtAuthentication) {
        this.builtAuthentication = builtAuthentication;
    }

    /**
     * Gets the parameters and values contained in the initiating stream tag.
     *
     * @return The parameters and values contained in the initiating stream tag.
     */
    public Map<String, String> getInitialParameters() {
        return initialParameters;
    }

    protected StringBuilder getStringBuilder() {
        return stringBuilder;
    }

    protected NegotiationStateMachine getStateMachine() {
        return stateMachine;
    }


    protected void setStateMachine(NegotiationStateMachine stateMachine) {
        if (this.stateMachine != null) {
            throw new IllegalStateException("Can't change the state machine to this processor.");
        }
        this.stateMachine = stateMachine;
    }


    @Override
    protected ParserResponse process() throws XMLStreamException {
        if (stateMachine == null) {
            throw new IllegalStateException("Can't process if there is no state machine set to this processor.");
        }
        if (!parser.hasNext()) {
            return ParserResponse.EVERYTHING_NORMAL;
        }
        getStringBuilder().setLength(0); // Clears the string builder

        ParserResponse response = ParserResponse.EVERYTHING_NORMAL;
        while (parser.hasNext()) {
            next();
            if (getParserStatus() == AsyncXMLStreamReader.EVENT_INCOMPLETE) {
                break;
            } else if (getParserStatus() == -1) {
                response = ParserResponse.XML_ERROR;
                break;
            }
            response = this.getStateMachine().negotiate();
            updateStoredBytes(getParserStatus());
            // Stop negotiation if an error occurred.
            if (StreamErrorsManager.getInstance().parserResponseErrors().contains(response)) {
                break;
            }
        }
        return response;
    }

    private AsyncXMLStreamReader<AsyncByteArrayFeeder> getParser() {
        return parser;
    }

    private int consumeOutput(byte[] message) {
        return outputConsumer.consumeMessage(message);
    }

    /**
     * This class represents a state machine which can handle all the negotiation process.
     */
    protected static abstract class NegotiationStateMachine {


        /**
         * Holds the actual state of this state machine
         */
        private NegotiationStateMachine.State state;
        /**
         * The processor that owns this state machine.
         */
        private BaseNegotiationProcessor negotiationProcessor;

        /**
         * Constructor.
         */
        protected NegotiationStateMachine(NegotiationStateMachine.State initialState) {
            if (initialState == null) {
                throw new IllegalArgumentException();
            }
            this.state = initialState;
            initialState.setStateMachine(this);
            this.negotiationProcessor = null;
        }

        protected void setNegotiationProcessor(BaseNegotiationProcessor negotiationProcessor) {
            if (this.negotiationProcessor != null) {
                throw new IllegalArgumentException("Negotiation Processor can't be changed once it's set.");
            }
            this.negotiationProcessor = negotiationProcessor;
        }

        protected BaseNegotiationProcessor getNegotiationProcessor() {
            return negotiationProcessor;
        }


        protected StringBuilder getStringBuilder() {
            return negotiationProcessor.getStringBuilder();
        }

        /**
         * Makes the state machine process the next action, based on its state.
         *
         * @return The {@link ParserResponse} generated by the processed action.
         */
        public ParserResponse negotiate() {
            if (negotiationProcessor == null) {
                throw new IllegalArgumentException("Can't negotiate if there is no Negotiation Processor set.");
            }
            return state.action();
        }

        /**
         * Sets the state of this state machine.
         *
         * @param state The new {@link ServerNegotiationProcessor.NegotiationStateMachine.State} for this state machine.
         */
        protected void setState(NegotiationStateMachine.State state) {
            this.state = state;
        }


        private int getParserStatus() {
            return negotiationProcessor.getParserStatus();
        }

        private AsyncXMLStreamReader<AsyncByteArrayFeeder> getParser() {
            return negotiationProcessor.getParser();
        }

        private int consumeOutput(byte[] message) {
            return negotiationProcessor.consumeOutput(message);
        }




        /**
         * A {@link ServerNegotiationProcessor.NegotiationStateMachine} abstract state. Each state must extends this basic class.
         */
        protected static abstract class State {

            /**
             * The state machine that owns this state.
             */
            private NegotiationStateMachine stateMachine;

            protected State(NegotiationStateMachine stateMachine) {
                this.stateMachine = stateMachine;
            }

            protected State() {
                this(null);
            }

            protected void setStateMachine(NegotiationStateMachine stateMachine) {
                if (this.stateMachine != null) {
                    throw new IllegalStateException("Can't change the owner to this state.");
                }
                this.stateMachine = stateMachine;
            }

            protected NegotiationStateMachine getStateMachine() {
                return stateMachine;
            }

            /**
             * Returns the state of the {@link BaseNegotiationProcessor}
             * that owns the state machine that owns this state
             *
             * @return The processor's state.
             */
            protected int getProcessorStatus() {
                return stateMachine.getParserStatus();
            }

            protected StringBuilder getStringBuilder() {
                return stateMachine.getStringBuilder();
            }

            protected AsyncXMLStreamReader<AsyncByteArrayFeeder> getParser() {
                return stateMachine.getParser();
            }

            protected int consumeOutput(byte[] message) {
                return stateMachine.consumeOutput(message);
            }


            /**
             * Action that is performed when the state machine is on this state.
             *
             * @return The {@link ParserResponse} generated by this action.
             */
            abstract ParserResponse action();
        }
    }
}
