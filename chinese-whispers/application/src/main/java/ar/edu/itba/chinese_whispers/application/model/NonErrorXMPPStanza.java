package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.BasicXMPPStanza;

import java.util.List;
import java.util.Map;

/**
 * Created by dgrimau on 25/10/16.
 */
public abstract class NonErrorXMPPStanza extends BasicXMPPStanza {
    private Map<String, String> extraAttributes;
    //private List<Content> contents; //Borrar
    private List<ExtendedContent> extendedContents; //TODO can non-extended content have childs? If not, differentiate.
}
