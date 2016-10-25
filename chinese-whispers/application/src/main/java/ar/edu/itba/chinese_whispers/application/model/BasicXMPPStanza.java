package ar.edu.itba.chinese_whispers.application.model;

import java.util.List;
import java.util.Map;

public abstract class BasicXMPPStanza {

    private String to;
    private String from;
    private String id;
    private String xml_lang;
    private Map<String, String> extraAttributes;
    private List<Content> contents;
    private List<ExtendedContent> extendedContents; //TODO can non-extended content have childs? If not, differentiate.

}
