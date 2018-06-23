/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.routing;

import jist.swans.misc.Message;

/**
 *
 * @author dharmawan
 */
public class MessagePoolData implements Message {
    public final double dataValue;
    public final int queryId;
    public final long sequenceNumber;
    public int producerNodeId;

    public String tipeSensor;

    public int fromRegion;
    public int fromZone;
    
    public MessagePoolData(double dataValue) {
        this.dataValue = dataValue;
        queryId        = -1;
        sequenceNumber = -1;
    }
    
    public MessagePoolData(double dataValue, int queryId, long sequenceNumber, int producerNodeId) {
        this.dataValue = dataValue;
        this.queryId   = queryId;
        this.sequenceNumber = sequenceNumber;
        this.producerNodeId = producerNodeId;
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
}