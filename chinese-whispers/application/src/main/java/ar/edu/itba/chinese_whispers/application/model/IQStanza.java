package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.IQTypeAttribute;

public class IQStanza extends BasicXMPPStanza {

    private IQTypeAttribute iqTypeAttribute;

    public IQStanza(String to, String from, long id, String xml_lang, IQTypeAttribute iqTypeAttribute) {
        super(to, from, id, xml_lang);
        this.iqTypeAttribute = iqTypeAttribute;
    }
}
