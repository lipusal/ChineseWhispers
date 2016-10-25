package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.MessageTypeAttribute;

/**
 * Created by Estela on 25/10/2016.
 */
public class MessageStanza extends basicXMPPStanza {

    private MessageTypeAttribute messageTypeAttribute;

    public MessageStanza(String to, String from, long id, String xml_lang, MessageTypeAttribute messageTypeAttribute) {
        super(to, from, id, xml_lang);
        this.messageTypeAttribute = messageTypeAttribute;
    }


}
