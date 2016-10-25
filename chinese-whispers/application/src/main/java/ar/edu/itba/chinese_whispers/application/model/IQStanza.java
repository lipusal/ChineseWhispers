package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.IQTypeAttribute;

/**
 * Created by Estela on 25/10/2016.
 */
public class IQStanza extends basicXMPPStanza {

    private IQTypeAttribute iqTypeAttribute;

    public IQStanza(String to, String from, long id, String xml_lang, IQTypeAttribute iqTypeAttribute) {
        super(to, from, id, xml_lang);
        this.iqTypeAttribute = iqTypeAttribute;
    }
}
