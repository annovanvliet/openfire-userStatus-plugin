/*******************************************************************************
 *
 *      ___------------
 *    --  __-----------     NATO
 *    | --  __---------                  NCI AGENCY
 *    | | --          /\####    ###      P.O. box 174
 *    | | |          / #\   #    #       2501 CD The Hague
 *    | | |         <  # >       # 
 *     \ \ \         \ #/   #    #       Command and Control Systems Division
 *      \ \ \         \/####    ###
 *       \ \ \---------
 *        \ \---------     AGENCY        Project: JChat
 *         ----------     The Hague
 *
 * //******************************************************************************
 *
 *  * Copyright 2019 NCI Agency, Inc. All Rights Reserved.
 *  *
 *  * This software is proprietary to the NCI Agency.
 *  * It should be used only by the NCI Agency and the NATO Programming
 *  * Centre for the development and maintenance of the ICC system
 *  * (NATO-wide Integrated Command and Control software for air operations).
 *  * This software cannot be copied neither distributed to other parties.
 *  * Any other use of this software is subject to license terms, which
 *  * should be specified in a separate document, signed by NCIA and the
 *  * other party.
 *  *
 *  * This file is NATO UNCLASSIFIED
 *******************************************************************************/
package com.reucon.openfire.plugins.userstatus;

import java.util.Calendar;
import java.util.Date;

import org.jivesoftware.util.JiveGlobals;

/**
 * A condensed Server connection status report.
 *
 * @author Anno van Vliet
 *
 */
public class ConnectionStatus {

  /**
   * Specify how the connection is setup.
   *
   */
  public enum Direction {
    UNKNOWN,
    IN,
    OUT,
    BOTH;
  }

  private final String host;
  private final String address;
  private final Date firstActive;
  private final Date lastActive;
  private final Date lastDisconnect;

  /**
   * @param host
   * @param address
   * @param firstActive
   * @param lastActive
   * @param lastDisconnect
   */
  public ConnectionStatus(String host, String address, Date firstActive, Date lastActive, Date lastDisconnect) {
    super();
    this.host = host;
    this.address = address;
    this.firstActive = firstActive;
    this.lastActive = lastActive;
    this.lastDisconnect = lastDisconnect;
  }

  /**
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * @return the address
   */
  public String getAddress() {
    return address;
  }

  /**
   * @return the firstActive
   */
  public Date getFirstActive() {
    return firstActive;
  }

  
  /**
   * @return the firstActive
   */
  public String getFirstActiveAsString() {
    return dateAsString(firstActive);
  }

  /**
   * @return the lastActive
   */
  public Date getLastActive() {
    return lastActive;
  }

  /**
   * @return the lastActive
   */
  public String getLastActiveAsString() {
    return dateAsString(lastActive);
  }

  /**
   * @return the lastDisconnect
   */
  public Date getLastDisconnect() {
    return lastDisconnect;
  }
  
  public String getLastDisconnectAsString() {
    
    return dateAsString(lastDisconnect);
    

  }

  /**
   * @param lastDisconnect2
   * @return
   */
  private String dateAsString(Date date ) {
    if ( date != null ) {
      Calendar nowCal = Calendar.getInstance();
      Calendar sameDisconnectCal = Calendar.getInstance();
      sameDisconnectCal.setTime(date);
      boolean sameDisconnectDay = nowCal.get(Calendar.DAY_OF_YEAR) == sameDisconnectCal.get(Calendar.DAY_OF_YEAR) && nowCal.get(Calendar.YEAR) == sameDisconnectCal.get(Calendar.YEAR);

      return (sameDisconnectDay ? JiveGlobals.formatTime(date) : JiveGlobals.formatDateTime(date) );
    }
    return "--/--";
  }

}
