package ar.edu.itba.chinese_whispers.connection;

import java.util.Set;

/**
 * Created by jbellini on 24/10/16.
 *
 *
 */
public interface NewConnectionsNotifiable {
    
    
    public void notifyNewConnections(Set<Integer> connectionIds);
}
