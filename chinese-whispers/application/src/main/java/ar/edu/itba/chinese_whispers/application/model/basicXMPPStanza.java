package ar.edu.itba.chinese_whispers.application.model;

/**
 * Created by Estela on 25/10/2016.
 */
public abstract class basicXMPPStanza {

    private String to;
    private String from;
    private long id;
    private String xml_lang;

    public basicXMPPStanza(String to, String from, long id, String xml_lang) {
        this.to = to;
        this.from = from;
        this.id = id;
        this.xml_lang = xml_lang;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getXml_lang() {
        return xml_lang;
    }

    public void setXml_lang(String xml_lang) {
        this.xml_lang = xml_lang;
    }
}
