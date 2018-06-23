/*
 * MessageDataP2P.java
 *
 * Created on December 18, 2007, 3:58 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package sidnet.stack.users.ZRP_route.app;

import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import sidnet.core.misc.NCS_Location2D;

/**
 *
 * @author Oliver
 */
public class MessageDataValue implements Message {
    public final double dataValue;
    public final int queryId;
    public final long sequenceNumber;
    public int producerNodeId;

    public String tipeSensor;

    public int fromRegion;
    
    public int zone_id;
    public NetAddress CH_ip;
    public NCS_Location2D CH_loc;
    
    public MessageDataValue(double dataValue) {
        this.dataValue = dataValue;
        queryId        = -1;
        sequenceNumber = -1;
        this.CH_ip = null;
        this.CH_loc = null;
    }
    
    public MessageDataValue(double dataValue, int queryId,
    					  long sequenceNumber, int producerNodeId) {
        this.dataValue = dataValue;
        this.queryId   = queryId;
        this.sequenceNumber = sequenceNumber;
        this.producerNodeId = producerNodeId;
        this.CH_ip = null;
        this.CH_loc = null;
    }
    
    /** {@inheritDoc} */
    public int getSize() 
    { 
      int size = 0;
      size += 4; // double dataValue;
      size += 2; // double sequenceNumber;
      size += 4; // double sequenceNumber;
      size += 2; // int producerNodeId;
  
      return size;
    }
    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset)
    {
      throw new RuntimeException("not implemented");
    }
  } // class: MessageP2P