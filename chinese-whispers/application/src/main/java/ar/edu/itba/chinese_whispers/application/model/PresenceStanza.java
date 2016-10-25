package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.PresenceTypeAttribute;

/**
 * Created by Estela on 25/10/2016.
 */
public class PresenceStanza extends BasicXMPPStanza {

    private PresenceTypeAttribute presenceTypeAttribute;

    public PresenceStanza(String to, String from, long id, String xml_lang, PresenceTypeAttribute presenceTypeAttribute) {
        super(to, from, id, xml_lang);
        this.presenceTypeAttribute = presenceTypeAttribute;
    }
}
