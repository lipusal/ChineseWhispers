package ar.edu.itba.chinese_whispers.application.model;

import ar.edu.itba.chinese_whispers.application.model.enumTypeAttribute.MessageTypeAttribute;

import java.util.HashMap;

public class MessageStanza extends NonErrorXMPPStanza {

    private MessageTypeAttribute messageTypeAttribute;

    private HashMap<String,String> messageSubjects; //xml:lang -> Subject

    private HashMap<String,String> messageBodies; //xml:lang -> Body

    private String messageThreads; //xml:lang -> Thread





}
