package ar.edu.itba.pdc.chinese_whispers.xmpp_protocol.handlers;

/**
 * This class is responsible of sending stream errors messages.
 * Once sent, the handler will be in normal state again.
 * <p>
 * Created by jbellini on 14/11/16.
 */
public class StanzaErrorsManager extends ErrorsManager {


    private final static String SILENCED_USER = "";


    private static final StanzaErrorsManager singleton = new StanzaErrorsManager();


    /**
     * Private constructor.
     */
    private StanzaErrorsManager() {
        addErrorMessage(XMPPErrors.SILENCED_USER, SILENCED_USER);
    }

    private StanzaErrorsManager getInstance() {
        return singleton;
    }

    @Override
    protected void afterSendingError(XMPPHandler handler) {
        handler.notifyStanzaErrorWasSent();
    }
}
