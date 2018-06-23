/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.app;

import jist.swans.misc.Message;
import sidnet.core.misc.NCS_Location2D;

/**
 *
 * @author dharmawan
 */
public class MessageZone implements Message {
    
    public final int zone_id;
    public final NCS_Location2D CH;
    public final double battery;

    public MessageZone(int zone_id, NCS_Location2D CH, double battery) {
        this.zone_id = zone_id;
        this.CH = CH;
        this.battery = battery;
    }
	
    public void getBytes(byte[] msg, int offset) {
        throw new RuntimeException("not implemented");
    }

    public int getSize() {
        int size = 0;
        size += 2; // int            node_id;
        size += 8; // NCS_Location2D CH
        size += 4; // double         battery;
        //NetAddress
        return size;
    }
}
