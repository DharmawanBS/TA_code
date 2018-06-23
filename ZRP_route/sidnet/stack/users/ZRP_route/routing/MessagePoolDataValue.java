/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.routing;

import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import sidnet.core.misc.NCS_Location2D;

/**
 *
 * @author ZRP
 */
public class MessagePoolDataValue implements Message {

    public final double dataValue;
    
    public int queryID;
    
    public final long sequenceNumber;

    public int fromRegion;
    
    public int producerNodeId;

    public NetAddress sinkIP;
    public NCS_Location2D sinkLocation;
    
    public int zone_id;
    
    public MessagePoolDataValue(double dataValue,long sequenceNumber) {
        this.dataValue = dataValue;
        this.sequenceNumber = sequenceNumber;
    }
    
    /** {@inheritDoc} */
    public int getSize() { 
        return 17;
    }
    /** {@inheritDoc} */
    public void getBytes(byte[] b, int offset) {
        throw new RuntimeException("not implemented");
    }
}