package com.reucon.openfire.plugins.userstatus;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.ServerSessionEventDispatcher;
import org.jivesoftware.openfire.event.ServerSessionEventListener;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.session.ClientSession;
import org.jivesoftware.openfire.session.IncomingServerSession;
import org.jivesoftware.openfire.session.OutgoingServerSession;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.PresenceEventDispatcher;
import org.jivesoftware.openfire.user.PresenceEventListener;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Presence;

/**
 * UserStatus plugin for Openfire.
 */
public class UserStatusPlugin implements Plugin, PropertyEventListener, SessionEventListener, PresenceEventListener, PersistenceManager
{
    /**
   * TODO add a Description.
   *
   * @author Anno van Vliet
   *
   */
  enum Direction {
    UNKNOWN,
    IN,
    OUT;
  }

    private static Logger Log = LoggerFactory.getLogger(UserStatusPlugin.class.getName());
  
    public static final String HISTORY_DAYS_PROPERTY = "user-status.historyDays";
    public static final int DEFAULT_HISTORY_DAYS = -1;

    private Collection<PersistenceManager> persistenceManagers;
    private ServerSessionEventListener myServerSessionListener = new MyServerSessionEventListener();

    public void initializePlugin(PluginManager manager, File pluginDirectory)
    {
        int historyDays = JiveGlobals.getIntProperty(HISTORY_DAYS_PROPERTY, DEFAULT_HISTORY_DAYS);
        PropertyEventDispatcher.addListener(this);

        persistenceManagers = new ArrayList<PersistenceManager>();
        persistenceManagers.add(new DefaultPersistenceManager());

        try
        {
            Class.forName("com.reucon.openfire.phpbb3.PhpBB3AuthProvider");
            persistenceManagers.add(new PhpBB3PersistenceManager());
        }
        catch (ClassNotFoundException e)
        {
            // ignore
        }

        setAllOffline();
        setHistoryDays(historyDays);

        for (ClientSession session : SessionManager.getInstance().getSessions())
        {
            sessionCreated(session);
        }

        SessionEventDispatcher.addListener(this);
        PresenceEventDispatcher.addListener(this);
        ServerSessionEventDispatcher.addListener(myServerSessionListener);
    }

    public void destroyPlugin()
    {
        ServerSessionEventDispatcher.removeListener(myServerSessionListener);
        PresenceEventDispatcher.removeListener(this);
        SessionEventDispatcher.removeListener(this);
    }

    @Override
    public void sessionCreated(Session session)
    {
        if (!XMPPServer.getInstance().getUserManager().isRegisteredUser(session.getAddress()))
        {
            return;
        }

        setOnline(session);
    }

    @Override
    public void sessionDestroyed(Session session)
    {
        if (!XMPPServer.getInstance().getUserManager().isRegisteredUser(session.getAddress()))
        {
            return;
        }

        setOffline(session, new Date());
    }

    @Override
    public void anonymousSessionCreated(Session session)
    {
        // we are not interested in anonymous sessions
    }

    @Override
    public void anonymousSessionDestroyed(Session session)
    {
        // we are not interested in anonymous sessions
    }

    @Override
    public void resourceBound(Session session)
    {
        // not interested
    }

    @Override
    public void availableSession(ClientSession session, Presence presence)
    {
        updatePresence(session, presence);
    }

    @Override
    public void unavailableSession(ClientSession session, Presence presence)
    {
        updatePresence(session, presence);
    }

//    @Override
//    public void presencePriorityChanged(ClientSession session, Presence presence)
//    {
//        // we are not interested in priority changes
//    }
//
    @Override
    public void presenceChanged(ClientSession session, Presence presence)
    {
        updatePresence(session, presence);
    }

    @Override
    public void subscribedToPresence(JID subscriberJID, JID authorizerJID)
    {
        // we are not interested in subscription updates
    }

    @Override
    public void unsubscribedToPresence(JID unsubscriberJID, JID recipientJID)
    {
        // we are not interested in subscription updates
    }

    @Override
    public void propertySet(String property, Map<String, Object> params)
    {
        if (HISTORY_DAYS_PROPERTY.equals(property))
        {
            final Object value = params.get("value");
            if (value != null)
            {
                try
                {
                    setHistoryDays(Integer.valueOf(value.toString()));
                }
                catch (NumberFormatException e)
                {
                    setHistoryDays(DEFAULT_HISTORY_DAYS);
                }
                deleteOldHistoryEntries();
            }
        }
    }

    @Override
    public void propertyDeleted(String property, Map<String, Object> params)
    {
        if (HISTORY_DAYS_PROPERTY.equals(property))
        {
            setHistoryDays(DEFAULT_HISTORY_DAYS);
            deleteOldHistoryEntries();
        }
    }

    @Override
    public void xmlPropertySet(String property, Map<String, Object> params)
    {
        // we don't use xml properties
    }

    @Override
    public void xmlPropertyDeleted(String property, Map<String, Object> params)
    {
        // we don't use xml properties
    }

    private void updatePresence(ClientSession session, Presence presence)
    {
        final String presenceText;

        if (!XMPPServer.getInstance().getUserManager().isRegisteredUser(session.getAddress()))
        {
            return;
        }

        if (Presence.Type.unavailable.equals(presence.getType()))
        {
            presenceText = presence.getType().toString();
        }
        else if (presence.getShow() != null)
        {
            presenceText = presence.getShow().toString();
        }
        else if (presence.isAvailable())
        {
            presenceText = "available";
        }
        else
        {
            return;
        }

        setPresence(session, presenceText);
    }

    // implementation of PersistenceManager

    public void setHistoryDays(int historyDays)
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.setHistoryDays(historyDays);
        }
    }

    public void setAllOffline()
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.setAllOffline();
        }
    }

    public void setOnline(Session session)
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.setOnline(session);
        }
    }

    public void setOffline(Session session, Date logoffDate)
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.setOffline(session, logoffDate);
        }
    }

    public void setServerOnline(Session session, Direction direction)
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.setServerOnline(session, direction);
        }
    }

    public void setServerOffline(Session session, Date logoffDate, Direction direction)
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.setServerOffline(session, logoffDate, direction);
        }
    }

    public void setPresence(Session session, String presenceText)
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.setPresence(session, presenceText);
        }
    }

    public void deleteOldHistoryEntries()
    {
        for (PersistenceManager pm : persistenceManagers)
        {
            pm.deleteOldHistoryEntries();
        }
    }
    
    private class MyServerSessionEventListener implements ServerSessionEventListener {
      
      @Override
      public void sessionDestroyed(Session session) {
        
        Direction direction = Direction.UNKNOWN;
        if (session instanceof IncomingServerSession ) direction = Direction.IN; 
        if (session instanceof OutgoingServerSession ) direction = Direction.OUT; 
                
        setServerOffline(session, new Date(), direction);
      }
      
      @Override
      public void sessionCreated(Session session) {
        
        try {
          Log.info("sessionCreated: {} {} {}", session, session.getHostName(), session.getServerName());
        } catch (UnknownHostException e) {
          Log.info("sessionCreated: " + session + " - " );
        }
        
        Direction direction = Direction.UNKNOWN;
        if (session instanceof IncomingServerSession ) direction = Direction.IN; 
        if (session instanceof OutgoingServerSession ) direction = Direction.OUT; 
                
        setServerOnline(session, direction);
        
      }
    }
    
    
}
