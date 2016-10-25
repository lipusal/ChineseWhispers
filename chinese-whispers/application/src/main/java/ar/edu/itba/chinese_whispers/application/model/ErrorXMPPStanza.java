package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.ErrorType;

/**
 * Created by dgrimau on 25/10/16.
 */
public class ErrorXMPPStanza {

    private ErrorType errorType;
    private StanzaKind stanzaKind;

    //Opcional
    private String by;
    private String descripcion;

}
