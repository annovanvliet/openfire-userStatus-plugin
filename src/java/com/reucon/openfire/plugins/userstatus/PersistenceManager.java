package com.reucon.openfire.plugins.userstatus;

import java.util.Date;

import org.jivesoftware.openfire.session.Session;

import com.reucon.openfire.plugins.userstatus.ConnectionStatus.Direction;


/**
 *
 */
public interface PersistenceManager
{
    void setHistoryDays(int historyDays);

    void setAllOffline();

    void setOnline(Session session);

    void setOffline(Session session, Date logoffDate);

    void setPresence(Session session, String presenceText);

    void deleteOldHistoryEntries();

    /**
     * Store creation of a S2S connection.
     * This is a incoming or out-bound s2s connection
     * 
     * @param session
     * @param direction 
     */
    void setServerOnline(Session session, Direction direction);

    /**
     * 
     * Store disconnect of a S2S connection.
     * 
     * @param session
     * @param logoffDate
     * @param direction 
     */
    void setServerOffline(Session session, Date logoffDate, Direction direction);
}
