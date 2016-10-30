package ar.edu.itba.pdc.chinese_whispers.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

//TODO DIEGO NO BORRAR DIEGO DIEGO TODO DIEGO TODO TODO DIEGO

public class Configurations {

    private static Configurations configurationsInstance;

    private boolean isL337;
    private Set<String> silencedUsers;
    private Map<String,String> multiplexedUsers;
    private String defaultServer;

    private Configurations(){
        silencedUsers=new HashSet<>();
        silencedUsers.add("fede@pc");
        isL337=true;
        multiplexedUsers=new HashMap<>();
    }

    public static Configurations getConfigurations(){
        if(configurationsInstance==null){
            configurationsInstance=new Configurations();
        }
        return  configurationsInstance;
    }

    public boolean isL337() {
        return isL337;
    }

    public void setIsL337(boolean isL337) {
        isL337 = isL337;
    }
    //TODO wat if default server == null?
    public String getServerDestination(String clientJID){
        if(multiplexedUsers.containsKey(clientJID)) return multiplexedUsers.get(clientJID);
        else return defaultServer;
    }

    public boolean isSilenced(String clientJID){
        return silencedUsers.contains(clientJID);
    }
}
