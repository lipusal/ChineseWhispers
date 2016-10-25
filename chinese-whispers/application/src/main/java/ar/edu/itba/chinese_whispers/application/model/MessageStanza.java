package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.MessageTypeAttribute;

public class MessageStanza extends BasicXMPPStanza {

    private MessageTypeAttribute messageTypeAttribute;

    public MessageStanza(String to, String from, long id, String xml_lang, MessageTypeAttribute messageTypeAttribute) {
        super(to, from, id, xml_lang);
        this.messageTypeAttribute = messageTypeAttribute;
    }


}
