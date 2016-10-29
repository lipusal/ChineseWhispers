package ar.edu.itba.pdc.chinese_whispers.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Estela on 29/10/2016.
 */
public class Configurations {

    private static Configurations configurationsInstance;

    private boolean isL337;
    private Set<String> silencedUsers;
    private Map<String,String> multiplexedUsers;
    private String defaultServer;

    private Configurations(){
        silencedUsers=new HashSet<>();
        isL337=false;
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

    public String getServerDestination(String clientJID){
        if(multiplexedUsers.containsKey(clientJID)) return multiplexedUsers.get(clientJID);
        else return defaultServer;
    }

    public boolean isSilenced(String clientJID){
        return silencedUsers.contains(clientJID);
    }
}
