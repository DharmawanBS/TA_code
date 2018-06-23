/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.driver;

import java.util.ArrayList;
import jist.swans.net.NetAddress;
import sidnet.core.misc.NCS_Location2D;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.NodesList;

/**
 *
 * @author dharmawan
 */
public class Zone {    
    public ArrayList<Zone_info> zone;

    public Zone(int count) {
        zone = new ArrayList<Zone_info>();
        for (int i=0;i<count;i++) {
            zone.add(new Zone_info(i));
        }
    }
    
    public void removeZoneInfo (int id) {
        if (!zone.isEmpty()) {
            for(int i=0;i<zone.size();i++) {
                if (zone.get(i).getZone_id() == id) {
                    zone.remove(i);
                    break;
                }
            }
        }
    }
        
    public class Zone_info {
        
        public int zone_id;
        public NodesList zone_item;
        public int count;
        public NodeEntry CH;

        public Zone_info(int id) {
            this.zone_id = id;
            this.zone_item = new NodesList();
            this.count = 0;
        }

        public int getZone_id() {
            return zone_id;
        }

        public NodesList getZone_item() {
            return zone_item;
        }

        public void setZone_id(int zone_id) {
            this.zone_id = zone_id;
        }

        public void setZone_item(NodesList zone_item) {
            this.zone_item = zone_item;
        }
        
        public void addZoneItem(NetAddress ip,NodeEntry newEntry) {
            if (zone_item.size() == 0) CH = newEntry;
            this.zone_item.add(ip,newEntry);
        }
        
        public void changeCH() {
            count++;
            if (count == zone_item.size()) count = 0;
            
            CH = zone_item.getAsLinkedList().get(count);
        }
    }
}
