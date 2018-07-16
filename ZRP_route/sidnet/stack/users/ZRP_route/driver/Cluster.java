/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.driver;

import java.awt.Color;
import jist.swans.net.NetAddress;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.NodesList;

/**
 *
 * @author Asus
 */
public class Cluster {
    public int id;
    public Location2D middle;
    public NodesList myCluster;
    public NodesList CHCluster;
    public Color color;
    public int vertical,horizontal;
    public int CH_now = 0;

    public Cluster(int id, Location2D middle) {
        this.id = id;
        this.middle = middle;
        this.myCluster = new NodesList();
        this.CHCluster = new NodesList();
        this.color = Color.BLACK;
        this.vertical = 0;
        this.horizontal = 0;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    public void addMyCluster(NetAddress ip,NodeEntry item) {
        this.myCluster.add(ip,item);
    }
    
    public void addCHCluster(NetAddress ip,NodeEntry item) {
        this.CHCluster.add(ip,item);
    }
    
    public void changeCH() {
        CH_now++;
        if (CH_now == CHCluster.size()) CH_now = 0;
    }
}
