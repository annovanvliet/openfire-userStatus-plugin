package com.reucon.openfire.plugins.userstatus;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.SequenceManager;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.reucon.openfire.plugins.userstatus.ConnectionStatus.Direction;

/**
 * Default implementation of the PersistenceManager interface.
 */
public class DefaultPersistenceManager implements PersistenceManager
{
  private static final Logger Log = LoggerFactory.getLogger(DefaultPersistenceManager.class);
  
    private static final int SEQ_ID = 510;
    private static final int S_SEQ_ID = 511;

    private static final String ADD_USER_STATUS =
            "INSERT INTO userStatus (username, resource, online, lastIpAddress, lastLoginDate) " +
                    "VALUES (?, ?, 1, ?, ?)";

    private static final String UPDATE_USER_STATUS =
            "UPDATE userStatus SET online = 1, lastIpAddress = ?, lastLoginDate = ? " +
                    "WHERE username = ? AND resource = ?";

    private static final String SET_PRESENCE =
            "UPDATE userStatus SET presence = ? WHERE username = ? AND resource = ?";

    private static final String SET_OFFLINE =
            "UPDATE userStatus SET online = 0, lastLogoffDate = ? WHERE username = ? AND resource = ?";

    private static final String SET_ALL_OFFLINE =
            "UPDATE userStatus SET online = 0 WHERE online = 1";

    private static final String ADD_USER_STATUS_HISTORY =
            "INSERT INTO userStatusHistory (historyID, username, resource, lastIpAddress," +
                    "lastLoginDate, lastLogoffDate) " +
            "SELECT ?, username, resource, lastipaddress, lastlogindate, lastlogoffdate " +
            "from userStatus WHERE username = ? and resource = ?";

    private static final String DELETE_OLD_USER_STATUS_HISTORY =
            "DELETE from userStatusHistory WHERE lastLogoffDate < ?";

    private static final String ADD_SERVER_STATUS_HISTORY = 
            "INSERT INTO serverStatusHistory (historyID, streamID, address, ipAddress, online, type, eventDate)" +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String DELETE_OLD_SERVER_STATUS_HISTORY =
        "DELETE from serverStatusHistory WHERE lastLogoffDate < ?";

    private static final String SELECT_SERVER_STATUS_HISTORY_VIEW = "SELECT t1.address, t1.ipaddress, t1.min, t1.max, t2.max  "
        + "FROM "
        + "(SELECT address, ipaddress, MIN(eventdate) AS min,  MAX(eventdate) AS max "
        + "FROM serverStatusHistory where online = 1 "
        + "GROUP BY address, ipaddress) AS t1 "
        + "LEFT JOIN "
        + "( SELECT address, ipaddress, MAX(EVENTDATE) AS max "
        + "FROM serverStatusHistory AS t2 where online = 0 "
        + "GROUP BY address, ipaddress ) AS t2 "
        + "ON t1.address = t2.address AND t1.ipaddress = t2.ipaddress";
   
    private static final String SELECT_SERVER_STATUS_HISTORY_VIEW_COUNT = "SELECT COUNT(distinct address) FROM serverStatusHistory ";


    
    /**
     * Number of days to keep history entries.<p>
     * 0 for no history entries, -1 for unlimited.
     */
    private int historyDays = -1;

    public void setHistoryDays(int historyDays)
    {
        this.historyDays = historyDays;
    }

    public void setAllOffline()
    {
        Connection con = null;
        PreparedStatement pstmt = null;

        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SET_ALL_OFFLINE);
            pstmt.executeUpdate();
        }
        catch (SQLException e)
        {
            Log.error("Unable to clean up user status", e);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }


    public void setOnline(Session session)
    {
        Connection con = null;
        PreparedStatement pstmt = null;
        int rowsUpdated = 0;

        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(UPDATE_USER_STATUS);
            pstmt.setString(1, getHostAddress(session));
            pstmt.setString(2, StringUtils.dateToMillis(session.getCreationDate()));
            pstmt.setString(3, session.getAddress().getNode());
            pstmt.setString(4, session.getAddress().getResource());
            rowsUpdated = pstmt.executeUpdate();
        }
        catch (SQLException e)
        {
            Log.error("Unable to update user status for " + session.getAddress(), e);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }

        // if update did not affect any rows insert a new row
        if (rowsUpdated == 0)
        {
            try
            {
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(ADD_USER_STATUS);
                pstmt.setString(1, session.getAddress().getNode());
                pstmt.setString(2, session.getAddress().getResource());
                pstmt.setString(3, getHostAddress(session));
                pstmt.setString(4, StringUtils.dateToMillis(session.getCreationDate()));
                pstmt.executeUpdate();
            }
            catch (SQLException e)
            {
                Log.error("Unable to insert user status for " + session.getAddress(), e);
            }
            finally
            {
                DbConnectionManager.closeConnection(pstmt, con);
            }
        }
    }

    public void setOffline(Session session, Date logoffDate)
    {
        Connection con = null;
        PreparedStatement pstmt = null;

        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SET_OFFLINE);
            pstmt.setString(1, StringUtils.dateToMillis(logoffDate));
            pstmt.setString(2, session.getAddress().getNode());
            pstmt.setString(3, session.getAddress().getResource());
            pstmt.executeUpdate();
        }
        catch (SQLException e)
        {
            Log.error("Unable to update user status for " + session.getAddress(), e);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }

        // write history entry
        if (historyDays != 0)
        {
            try
            {
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(ADD_USER_STATUS_HISTORY);
                pstmt.setLong(1, SequenceManager.nextID(SEQ_ID));
                pstmt.setString(2, session.getAddress().getNode());
                pstmt.setString(3, session.getAddress().getResource());
                pstmt.executeUpdate();
            }
            catch (SQLException e)
            {
                Log.error("Unable to add user status history for " + session.getAddress(), e);
            }
            finally
            {
                DbConnectionManager.closeConnection(pstmt, con);
            }
        }

        deleteOldHistoryEntries();
    }

    public void setPresence(Session session, String presenceText)
    {
        Connection con = null;
        PreparedStatement pstmt = null;

        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SET_PRESENCE);
            pstmt.setString(1, presenceText);
            pstmt.setString(2, session.getAddress().getNode());
            pstmt.setString(3, session.getAddress().getResource());
            pstmt.executeUpdate();
        }
        catch (SQLException e)
        {
            Log.error("Unable to update presence for " + session.getAddress(), e);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }
    }


    public void deleteOldHistoryEntries()
    {
        Connection con = null;
        PreparedStatement pstmt = null;

        if (historyDays > 0)
        {
            final Date deleteBefore;

            deleteBefore = new Date(System.currentTimeMillis() - historyDays * 24L * 60L * 60L * 1000L);

            try
            {
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(DELETE_OLD_USER_STATUS_HISTORY);
                pstmt.setString(1, StringUtils.dateToMillis(deleteBefore));
                pstmt.executeUpdate();
            }
            catch (SQLException e)
            {
                Log.error("Unable to delete old user status history", e);
            }
            finally
            {
                DbConnectionManager.closeConnection(pstmt, con);
            }
        }
    }

    @Override
    public void setServerOnline(Session session, Direction direction)
    {
      writeServerStatus(session, true, StringUtils.dateToMillis(session.getCreationDate()), direction);

    }

    /**
     * @param session
     * @param b
     * @param dateToMillis
     */
    private void writeServerStatus(Session session, boolean online, String dateToMillis, Direction direction) {
      Connection con = null;
      PreparedStatement pstmt = null;

      try
      {
          con = DbConnectionManager.getConnection();
          pstmt = con.prepareStatement(ADD_SERVER_STATUS_HISTORY);
          pstmt.setLong(1, SequenceManager.nextID(S_SEQ_ID));
          pstmt.setString(2, session.getStreamID().getID());
          pstmt.setString(3, getHostName(session));
          pstmt.setString(4, getHostAddress(session));
          pstmt.setInt(5, (online ? 1 : 0));
          pstmt.setInt(6, getDirection(direction));
          pstmt.setString(7, dateToMillis);
          pstmt.executeUpdate();
      }
      catch (SQLException e)
      {
          Log.error("Unable to add server status history for " + session.getStreamID(), e);
      }
      finally
      {
          DbConnectionManager.closeConnection(pstmt, con);
      }
      
    }

    @Override
    public void setServerOffline(Session session, Date logoffDate, Direction direction)
    {
      writeServerStatus(session, false, StringUtils.dateToMillis(logoffDate), direction);

      deleteOldServerHistoryEntries();
    }

    public void deleteOldServerHistoryEntries()
    {
        Connection con = null;
        PreparedStatement pstmt = null;

        if (historyDays > 0)
        {
            final Date deleteBefore;

            deleteBefore = new Date(System.currentTimeMillis() - historyDays * 24L * 60L * 60L * 1000L);

            try
            {
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(DELETE_OLD_SERVER_STATUS_HISTORY);
                pstmt.setString(1, StringUtils.dateToMillis(deleteBefore));
                pstmt.executeUpdate();
            }
            catch (SQLException e)
            {
                Log.error("Unable to delete old server status history", e);
            }
            finally
            {
                DbConnectionManager.closeConnection(pstmt, con);
            }
        }
    }
    
    
    private String getHostAddress(Session session)
    {
        try
        {
            return session.getHostAddress();
        }
        catch (UnknownHostException e)
        {
            return "";
        }
    }
    
    private String getHostName(Session session)
    {
        try
        {
            return session.getHostName();
        }
        catch (UnknownHostException e)
        {
            return session.getAddress().getDomain();
        }
    }
    
    private int getDirection(com.reucon.openfire.plugins.userstatus.ConnectionStatus.Direction direction) {
      
      switch (direction) {
      case IN: return 1;
      case OUT: return 2;
      case BOTH: return 3;
      case UNKNOWN:
      default:
        break;
      }
      return 0;
    }

  /**
   * @param start
   * @param maxIndex
   * @return
   */
  public List<ConnectionStatus> retrieveConnections(int start, int maxIndex) {
    List<ConnectionStatus> result = new ArrayList<ConnectionStatus>();

    Connection con = null;
    PreparedStatement pstmt = null;
    ResultSet resultSet = null;
    
    try {
      con = DbConnectionManager.getConnection();
      pstmt = con.prepareStatement(SELECT_SERVER_STATUS_HISTORY_VIEW);
//      pstmt.setInt(1, start);
//      pstmt.setInt(2, maxIndex);
      resultSet = pstmt.executeQuery();
      
      while (resultSet.next()) {
        
        ConnectionStatus connectionStatus = new ConnectionStatus(
            resultSet.getString(1), 
            resultSet.getString(2), 
            millisToDate(resultSet.getString(3)), 
            millisToDate(resultSet.getString(4)), 
            millisToDate(resultSet.getString(5)));
        
        result.add(connectionStatus);
        
      }
      
    } catch (SQLException e) {
      Log.error("Unable to retrieve server session history", e);
    } finally {
      DbConnectionManager.closeConnection(resultSet, pstmt, con);
    }

    return result;
  }

  /**
   * @param int1
   * @return
   */
  private Direction intToDirection(int int1) {
    switch (int1) {
    case 1: return Direction.IN;
    case 2: return Direction.OUT;
    case 3: return Direction.BOTH;
    default:
      break;
    }
    return Direction.UNKNOWN;
  }

  private Date millisToDate(String l) {
    if (l != null) {
      try {
        return new Date(Long.parseLong(l));
      } catch (NumberFormatException e) {
        Log.error("Not a valid number:" + l , e);
      }
    }
    return null;
  }

    /**
     * @return
     */
    public int retrieveConnectionsCount() {
      
      int result = 0;
      Connection con = null;
      PreparedStatement pstmt = null;
      ResultSet resultSet  = null;
      
      try {
        con = DbConnectionManager.getConnection();
        pstmt = con.prepareStatement(SELECT_SERVER_STATUS_HISTORY_VIEW_COUNT);
        resultSet = pstmt.executeQuery();
        
        if (resultSet.next()) {
          result = resultSet.getInt(1);
        }
        
      } catch (SQLException e) {
        Log.error("Unable to retrieve old server status history count", e);
      } finally {
        DbConnectionManager.closeConnection(resultSet, pstmt, con);
      }
      return result;
    }

    
}
