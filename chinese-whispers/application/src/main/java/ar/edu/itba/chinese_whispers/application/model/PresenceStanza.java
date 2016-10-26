package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.PresenceTypeAttribute;

import java.util.HashMap;


public class PresenceStanza extends BasicXMPPStanza {


    private PresenceTypeAttribute presenceTypeAttribute;

    //TODO continue reading

    private ShowPresenceElement showPresenceElement;

    private HashMap<String,String> presenceStatuses; //xml:lang -> Status

    private int presencePriority; // The value MUST be an integer between -128 and +127
}
