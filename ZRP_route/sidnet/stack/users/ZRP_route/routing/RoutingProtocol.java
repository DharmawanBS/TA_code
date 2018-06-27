/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.routing;

import sidnet.stack.users.ZRP_route.ignoredpackage.PoolReceivedItem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import sidnet.core.interfaces.AppInterface;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Message;
import jist.swans.net.NetAddress;
import jist.swans.net.NetInterface;
import jist.swans.net.NetMessage;
import jist.swans.route.RouteInterface;
import sidnet.colorprofiles.ColorProfileGeneric;
import sidnet.core.gui.TopologyGUI;
import sidnet.core.misc.NCS_Location2D;
import sidnet.stack.users.ZRP_route.colorprofile.ColorProfileZRP;
import sidnet.core.misc.Node;
import sidnet.core.misc.NodeEntry;
import sidnet.core.misc.NodesList;
import sidnet.core.misc.Reason;
import sidnet.core.simcontrol.SimManager;
import sidnet.stack.users.ZRP_route.app.DropperNotifyAppLayer;
import sidnet.stack.users.ZRP_route.app.Konstanta;
import sidnet.stack.users.ZRP_route.app.MessageDataValue;
import sidnet.stack.users.ZRP_route.app.MessageQuery;
import sidnet.stack.users.ZRP_route.driver.SequenceGenerator;
import sidnet.stack.users.ZRP_route.driver.Zone;
import sidnet.utilityviews.statscollector.StatsCollector;

/**
 *
 * @author ZRP
 */
public class RoutingProtocol implements RouteInterface.ZRP_Route {
    
    public static final byte ERROR = -1;
    public static final byte SUCCESS = 0;
    private SequenceGenerator seq;
    private Zone zone;
    
    private final Node myNode; // The SIDnet handle to the node representation 
    private final StatsCollector stats;
    
    private boolean netQueueFULL = false;
    
    // entity hook-up (network stack)
    /** Network entity. */
    private NetInterface netEntity;
   
    /** Self-referencing proxy entity. */
    private RouteInterface self; 
    
    /** The proxy-entity for this application interface */
    private AppInterface appInterface;
    
    // DO NOT MAKE THIS STATIC
    private ColorProfileZRP colorProfile = new ColorProfileZRP(); 
    
    //Showing topology
    public static TopologyGUI topologyGUI = null;
    
    //node entry discover hashmap, keperluan penentuan my cluster head
    private HashMap<NetAddress, NodeEntryDiscovery> listTetangga = new HashMap<NetAddress, NodeEntryDiscovery>();
    private HashMap<NetAddress, NodeEntryDiscovery> listTepi = new HashMap<NetAddress, NodeEntryDiscovery>();
    
    //anti-duplicate list
    ArrayList<String> receivedDataId = new ArrayList<String>();
    
    //hashmap hitung maksimum retry
    HashMap<Long, Integer> dataRetry = new HashMap<Long, Integer>();
    
    //list dari node ini proses query apa saja
    private ArrayList<Integer> queryProcessed = new ArrayList<Integer>();
    
    //hashmap key:queryid item:sinkIP,sinkLocation
    private class DestinationSink {
        public NetAddress sinkIP;
        public NCS_Location2D sinkLocation;
        public int regionProcessed;

        public DestinationSink (NetAddress sinkIP, NCS_Location2D sinkLocation, int regionProcessed) {
            this.sinkIP = sinkIP;
            this.sinkLocation = sinkLocation;
            this.regionProcessed = regionProcessed;
        }
    }
    private HashMap<Integer, DestinationSink> detailQueryProcessed = new HashMap<Integer, DestinationSink>();
    
    private Map<String, PoolReceivedItem> rcvPool = new HashMap<String, PoolReceivedItem>();
    private ArrayList<PoolReceivedItem> lstItemPool = new ArrayList<PoolReceivedItem>();
    
    private NCS_Location2D arrowHead = null;
    private NCS_Location2D[] arrowHead_pool = new NCS_Location2D[16];
    
    private Color[] color = new Color[16];
    
    ArrayList<NodeEntry> list = new ArrayList<NodeEntry>();
    ArrayList<NodeEntry> list2 = new ArrayList<NodeEntry>();
    
    ArrayList<NodeEntry> list_in = new ArrayList<NodeEntry>();
    ArrayList<NodeEntry> list_out = new ArrayList<NodeEntry>();
    
    /** Creates a new instance
     *
     * @param Node    the SIDnet node handle to access 
     * 				  its GUI-primitives and shared environment
     */
    public RoutingProtocol(Node myNode,StatsCollector stats,SequenceGenerator seq,Zone zone) {
        this.myNode = myNode;
        this.stats = stats;
        this.seq = seq;
        this.zone = zone;
        
        /** Create a proxy for the application layer of this node */
        self = (RouteInterface.ZRP_Route)JistAPI.proxy(this, RouteInterface.ZRP_Route.class);
        color[0] = Color.BLUE;
        color[1] = Color.CYAN;
        color[2] = Color.GRAY;
        color[3] = Color.GREEN;
        color[4] = Color.WHITE;
        color[5] = Color.MAGENTA;
        color[6] = Color.ORANGE;
        color[7] = Color.PINK;
        color[8] = Color.BLUE;
        color[9] = Color.CYAN;
        color[10] = Color.GRAY;
        color[11] = Color.GREEN;
        color[12] = Color.WHITE;
        color[13] = Color.MAGENTA;
        color[14] = Color.ORANGE;
        color[15] = Color.PINK;
    }

    public void timingSend(long interval) {
        //topologyGUI.removeGroup(myNode.ZoneId+3);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        JistAPI.sleepBlock(interval);

        if (!rcvPool.isEmpty()) {
            
            zone.zone.get(myNode.ZoneId).changeCH();
            
            lstItemPool.clear();
            
            Iterator i = rcvPool.entrySet().iterator();

            while (i.hasNext()) {
                Entry item = (Entry) i.next();
                PoolReceivedItem pri = (PoolReceivedItem)item.getValue();

                i.remove();
                lstItemPool.add(pri);
            }
            
            for (PoolReceivedItem pri : lstItemPool) {
                long sequenceNumber = seq.getandincrement();
                
                MessagePoolDataValue mpdv = new MessagePoolDataValue();
                mpdv.avgdataValue = pri.avgdataValue;
                mpdv.maxdataValue = pri.maxdataValue;
                mpdv.mindataValue = pri.mindataValue;
                mpdv.countdataValue = pri.countdataValue;
                mpdv.queryID = pri.queryID;
                mpdv.sinkIP = detailQueryProcessed.get(pri.queryID).sinkIP;
                mpdv.sinkLocation = detailQueryProcessed.get(pri.queryID).sinkLocation;
                mpdv.zone_id = myNode.ZoneId;
                mpdv.sequenceNumber = sequenceNumber;
                mpdv.priority = pri.priority;

                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(mpdv);
                pmw.setS_seq(sequenceNumber);
                
                //stats.markPacketSent("DATA_"+myNode.ZoneId, sequenceNumber);
                stats.markPacketSent("DATA", sequenceNumber);
                stats.markPacketSent("DATA_PRI_"+mpdv.priority, sequenceNumber);
                System.out.println("create pool di "+mpdv.priority+" "+myNode.getID()+" "+sequenceNumber);

                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), detailQueryProcessed.get(mpdv.queryID).sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                
                JistAPI.sleep(Konstanta.TIMING_DELAY_SEND);
                
                handleMessagePool(nmip);
            }
        }
        
        ((RouteInterface.ZRP_Route)self).timingSend(interval);
    }
    
    public void selfMonitor() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void peek(NetMessage msg, MacAddress lastHop) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void send(NetMessage msg) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        // process only if there are energy reserves
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (Konstanta.IS_PAUSE) myNode.getSimControl().setSpeed(SimManager.PAUSED);
            return;
        }
        
        if (this.netQueueFULL)
            return;
      
        //  jika pesan tidak dikenali, tolak
        if (!(((NetMessage.Ip)msg).getPayload() instanceof ProtocolMessageWrapper)) {
            return; // ignore non-specific messages
        }
        
        // update visuals        
    	myNode.getNodeGUI().colorCode.mark(colorProfile,ColorProfileZRP.RECEIVE, 2000);
        
        //Extract pesan ketipe wrapper
        ProtocolMessageWrapper sndMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();

        if (sndMsg.getPayload() instanceof MessageDataValue) {
            //tipe pesan ini diteruskan ke fungsi handleMessageDataValue
            if (!receivedDataId.contains(String.valueOf(sndMsg.getS_seq()))) {
                receivedDataId.add(String.valueOf(sndMsg.getS_seq()));
                memoryControllerPacketID();
                handleMessageDataValue((MessageDataValue)sndMsg.getPayload());
            }
        }
        else if (sndMsg.getPayload() instanceof MessageQuery) {
            //tipe pesan query, cek apakah pesan query sudah pernah diterima sebelumnya
            //jika belum berikan ke fungsi handleMessageQuery
            //jika sudah abaikan
            if (!receivedDataId.contains(String.valueOf(sndMsg.getS_seq()))) {
                receivedDataId.add(String.valueOf(sndMsg.getS_seq()));
                memoryControllerPacketID();
                handleMessageQuery(sndMsg);
            }
        }
        else if (sndMsg.getPayload() instanceof MessagePoolDataValue) {
            //tipe pesan aggregate diteruskan ke fungsi handleMessageAggregatedDataValue
            if (!receivedDataId.contains(String.valueOf(sndMsg.getS_seq()))) {
                receivedDataId.add(String.valueOf(sndMsg.getS_seq()));
                memoryControllerPacketID();
                /*System.out.println("pool dari "
                        +((MessagePoolDataValue) sndMsg.getPayload()).zone_id+" "
                        +((MessagePoolDataValue) sndMsg.getPayload()).producerNodeId+" "
                        +myNode.ZoneId+" "+myNode.getID());*/
                
                if (Konstanta.USE_AGG) {
                    MessagePoolDataValue temp = (MessagePoolDataValue) sndMsg.getPayload();
                    stats.markPacketReceived("DATA", sndMsg.getS_seq());
                    stats.markPacketReceived("DATA_PRI_"+temp.priority, sndMsg.getS_seq());
                    //stats.markPacketReceived("DATA_"+temp.zone_id, sndMsg.getS_seq());
                    System.out.println("receive pool di "+myNode.getID()+" "+sndMsg.getS_seq());
                    System.out.println("receive pool di "+temp.priority+" "+myNode.getID()+" "+sndMsg.getS_seq());
                    
                    poolHandleMessagePoolDataValue(temp);
                }
                else handleMessagePool(msg);
            }
        }
    }
    
    private void handleMessagePool(NetMessage msg) {
        //Extract pesan ketipe wrapper
        ProtocolMessageWrapper sndMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();

        //extract ke tipe mpdv
        MessagePoolDataValue mpdv = (MessagePoolDataValue)sndMsg.getPayload();
        
        //System.out.println("pool "+myNode.getID());
        
        /*NodesList nextHop = getNextHop(mpdv.sinkLocation,mpdv.sinkIP);
        LinkedList<NodeEntry> nextList = nextHop.getAsLinkedList();
        NodeEntry next = nextList.get((int) (myNode.myZone.count%nextList.size()));*/
        
        NodeEntry next = findNextHop(mpdv.sinkLocation,mpdv.sinkIP,false,null);
        
        NetMessage.Ip nmip = new NetMessage.Ip(sndMsg, myNode.getIP(), mpdv.sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
        sendToLinkLayer(nmip, next.ip);

        arrowHead_pool[myNode.ZoneId] = next.getNCS_Location2D();
        Color colour = color[mpdv.zone_id];
        topologyGUI.addLink(myNode.getNCS_Location2D(), arrowHead_pool[myNode.ZoneId], mpdv.zone_id+3,colour, TopologyGUI.HeadType.LEAD_ARROW);
    }
    
    private void nodeDiscover() {
        MessageNodeDiscover mnd = new MessageNodeDiscover(myNode.getID(), myNode.getIP(), myNode.neighboursList.size(), myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel());
        mnd.addQueryProcessed(this.queryProcessed);

        ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(mnd);
        String unikID = String.valueOf(seq.getandincrement());
        pmw.setS_seq(Long.parseLong(unikID));
        NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), NetAddress.ANY, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
        sendToLinkLayer(nmip, NetAddress.ANY);
    }
    
    private void handleMessageQuery(ProtocolMessageWrapper msg) {
        /*
         * Ketika mendapatkan query message, node memeriksa apakah dia masuk
         * didalam region tersebut, jika iya maka node akan broadcast
         * ke tetangga bahwa ia masuk dalam region baru
         */

        MessageQuery query = (MessageQuery)msg.getPayload();

        if (query.getQuery().getSinkIP().hashCode() != myNode.getIP().hashCode())
            if (query.getQuery().getRegion().isInside(myNode.getNCS_Location2D())) {
                
                nodeDiscover();
                
                //start timing send
                ((RouteInterface.ZRP_Route)self).timingSend(Konstanta.INTERVAL_TIMING_SEND);

                this.queryProcessed.add(query.getQuery().getID());
                DestinationSink ds = new DestinationSink(query.getQuery().getSinkIP(), query.getQuery().getSinkNCSLocation2D(), query.getQuery().getRegion().getID());
                this.detailQueryProcessed.put(query.getQuery().getID(), ds);

                //if (myNode.getID() == 22) {
                //setelah di broadcast, query diteruskan ke app layer
                sendToAppLayer(query, null);
                //}
            }
        
        //give a breath
        //JistAPI.sleepBlock(2 * Constants.SECOND);
        
        //sebarkan query
        //System.out.println("Node " + myNode.getID() + " broadcasting query.");
        NetMessage.Ip nmip = new NetMessage.Ip(msg, myNode.getIP(), NetAddress.ANY, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
        sendToLinkLayer(nmip, NetAddress.ANY);
    }
    
    private NodeEntry findNextHop(NCS_Location2D loc,NetAddress ip,boolean intra,MessageDataValue msg) {
        
        if (Konstanta.DIRECT_SEND && msg != null) {
            NodeEntry sink = myNode.neighboursList.get(detailQueryProcessed.get(msg.queryId).sinkIP);
            if (sink != null) return sink;
        }
        
        NodeEntry nextHop = (intra) ? findNextHop_in(loc) : findNextHop_out(loc,ip);
        
        if (nextHop == null) {
            String a = (intra) ? "in" : "out";
            //System.out.println("Random "+a+" "+myNode.getID());
            nextHop = myNode.neighboursList.getAsLinkedList().get((int) (Math.random()*myNode.neighboursList.size()));
        }
        
        return nextHop;
    }
    
    private NodeEntry findNextHop_in(NCS_Location2D loc) {
        NodeEntry nextHop = null;
        
        double shortestNodeDistance = -1;

        myNode.neighbourZoneList = new NodesList();

        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        //ambil list tetangga yang dibuat oleh zone
        LinkedList<NodeEntry> zoneLinkedList = zone.zone.get(myNode.ZoneId).getZone_item().getAsLinkedList();

        for(NodeEntry neighbourEntry: neighboursLinkedList) {
            for(NodeEntry zoneEntry: zoneLinkedList) {
                if (zoneEntry.ip == neighbourEntry.ip) {
                    myNode.neighbourZoneList.add(neighbourEntry.ip,neighbourEntry);
                }
            }
        }

        LinkedList<NodeEntry> neighboursZoneLinkedList = myNode.neighbourZoneList.getAsLinkedList();
        if (neighboursZoneLinkedList.size() > 0) {
            for(NodeEntry nodeEntry: neighboursZoneLinkedList) {
                if (shortestNodeDistance == -1) {
                    shortestNodeDistance = nodeEntry.getNCS_Location2D().distanceTo(loc);
                    nextHop = nodeEntry;
                }
                else if (shortestNodeDistance > nodeEntry.getNCS_Location2D().distanceTo(loc)) {
                    shortestNodeDistance = nodeEntry.getNCS_Location2D().distanceTo(loc);
                    nextHop = nodeEntry;
                }
            }
        }
        
        return nextHop;
    }

    private NodeEntry findNextHop_out(NCS_Location2D loc,NetAddress ip) {
        NodeEntry nextHop = null;
        
        if (myNode.neighboursList.contains(ip)) {
            return myNode.neighboursList.get(ip);
        }
        
        double distance = myNode.getNCS_Location2D().distanceTo(loc);

        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        if (list.isEmpty() || list2.isEmpty()) {
            list.clear();
            list2.clear();
            for(NodeEntry neighbourEntry: neighboursLinkedList) {
                //if (listTetangga.containsKey(neighbourEntry.ip)) {
                    if (neighbourEntry.getNCS_Location2D().distanceTo(loc) < distance) {
                        list.add(neighbourEntry);
                    }
                    else list2.add(neighbourEntry);
                //}
            }
        }
        
        if (list_in.isEmpty() && list_out.isEmpty()) {
            list_in = list;
            list_out = list2;
        }

        double energi = -1;

        if (!list_in.isEmpty()) {
            nextHop = list_in.get(0);
            list_in.remove(0);
            /*for(NodeEntry item : list_in) {
                if (listTetangga.containsKey(item.ip)) {
                    if (listTetangga.get(item.ip).energyLeft > energi) {
                        nextHop = item;
                        energi = listTetangga.get(item.ip).energyLeft;
                    }
                }
            }*/
        }
        
        if (nextHop == null)  {
            nextHop = list_out.get(0);
            list_out.remove(0);
            /*for(NodeEntry item : list2) {
                if (listTetangga.containsKey(item.ip)) {
                    if (listTetangga.get(item.ip).energyLeft > energi) {
                        nextHop = item;
                        energi = listTetangga.get(item.ip).energyLeft;
                    }
                }
            }*/
        }

        return nextHop;
    }
    
    private void handleMessageDataValue(MessageDataValue msg) {
        /*if (myNode.neighboursList.contains(detailQueryProcessed.get(msg.queryId).sinkIP)) {
            ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(msg);
            pmw.setS_seq(msg.sequenceNumber);
            NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), detailQueryProcessed.get(msg.queryId).sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
            sendToLinkLayer(nmip, detailQueryProcessed.get(msg.queryId).sinkIP);

            arrowHead = detailQueryProcessed.get(msg.queryId).sinkLocation;
            topologyGUI.addLink(myNode.getNCS_Location2D(),arrowHead, 1, Color.BLACK, TopologyGUI.HeadType.LEAD_ARROW);
            return;
        }*/

        //cari cluster head
        NodeEntry myCH = getMyClusterHead(msg);
        
        NCS_Location2D CH_loc = myCH.getNCS_Location2D();
        NetAddress CH_ip = myCH.ip;
        
        if (msg.CH_ip == null) {
            msg.CH_ip = CH_ip;
            msg.CH_loc = CH_loc;
        }
        else {
            CH_ip = msg.CH_ip;
            CH_loc = msg.CH_loc;
        }
        
        if (arrowHead != null) {
            topologyGUI.removeLink(myNode.getNCS_Location2D(),arrowHead, 1);
        }
        
        //tambahkan informasi region
        //msg.fromRegion = detailQueryProcessed.get(msg.queryId).regionProcessed;

        //  CH diri sendiri
        if (CH_ip == myNode.getIP()) {
            //System.out.println("Di node "+myNode.getID()+" pesan dari "+msg.producerNodeId+" CH diri sendiri "+CH_ip+" pool di "+myNode.getIP());
            
            stats.markPacketReceived("DATA", msg.sequenceNumber);
            stats.markPacketReceived("DATA_PRI_"+msg.priority, msg.sequenceNumber);
            //stats.markPacketReceived("DATA_"+msg.zone_id, msg.sequenceNumber);
            System.out.println("receive value di "+msg.priority+" "+myNode.getID()+" "+msg.sequenceNumber);
            
            poolHandleMessageDataValue(msg);
        }
        else {
            //cari next hop;
            NodeEntry nextHop = findNextHop(CH_loc,null,true,msg);
            
            //System.out.println("Di node "+myNode.getID()+" pesan dari "+msg.producerNodeId+" CH node lain "+CH_ip+" kirim ke "+nextHop.ip);
            ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(msg);
            pmw.setS_seq(msg.sequenceNumber);
            NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), detailQueryProcessed.get(msg.queryId).sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
            sendToLinkLayer(nmip, nextHop.ip);

            arrowHead = nextHop.getNCS_Location2D();
            topologyGUI.addLink(myNode.getNCS_Location2D(),arrowHead, 1, Color.BLACK, TopologyGUI.HeadType.LEAD_ARROW);
        }
    }
    
    private void poolHandleMessagePoolDataValue(MessagePoolDataValue msg) {
        //buat keyHashMap
        String hashMapKey = msg.priority+"-"+String.valueOf(msg.zone_id);

        //cek jika sudah terdapat
        if (rcvPool.containsKey(hashMapKey)) {
            //System.out.println(myNode.getID()+" duplicate pool "+ msg.sequenceNumber+" "+rcvPool.size());
            
            if (Konstanta.USE_AGG) {
                PoolReceivedItem item_pool = rcvPool.get(hashMapKey);
                item_pool.putAVG(msg.avgdataValue,msg.countdataValue);
                item_pool.putMAX(msg.maxdataValue);
                item_pool.putMIN(msg.mindataValue);
            }
        }
        else {
            //tidak terdapat key
            //bikin entry queue langsung masukan

            //System.out.println("Node:" + myNode.getID() + " create new hashmapkey:" + hashMapKey);

            PoolReceivedItem priNew = new PoolReceivedItem(msg.maxdataValue,msg.mindataValue,msg.avgdataValue,msg.countdataValue,msg.queryID,msg.priority);
            rcvPool.put(hashMapKey, priNew);
        }
    }
    
    private void poolHandleMessageDataValue(MessageDataValue msg) {
        //buat keyHashMap
        String hashMapKey;
        if (Konstanta.POOL_ALL) hashMapKey = Konstanta.KEY;
        else if (Konstanta.USE_AGG) hashMapKey = String.valueOf(msg.zone_id);
        else hashMapKey = String.valueOf(msg.sequenceNumber);

        //cek jika sudah terdapat
        if (rcvPool.containsKey(hashMapKey)) {
            //System.out.println(myNode.getID()+" duplicate pool "+ msg.sequenceNumber+" "+rcvPool.size());
            
            if (Konstanta.POOL_ALL || Konstanta.USE_AGG) {
                PoolReceivedItem item_pool = rcvPool.get(hashMapKey);
                item_pool.putAVG(msg.dataValue,msg.count);
                item_pool.putMAX(msg.maxdataValue);
                item_pool.putMIN(msg.mindataValue);
            }
        }
        else {
            //tidak terdapat key
            //bikin entry queue langsung masukan

            //System.out.println("Node:" + myNode.getID() + " create new hashmapkey:" + hashMapKey);

            PoolReceivedItem priNew = new PoolReceivedItem(msg.maxdataValue,msg.mindataValue,msg.dataValue,msg.count,msg.queryId,msg.priority);
            rcvPool.put(hashMapKey, priNew);
        }
    }
    
    private NodeEntry getMyClusterHead(MessageDataValue msg) {
        //LinkedList<NodeEntry> pilihanCH = zone.zone.get(myNode.ZoneId).getZone_item().getAsLinkedList();
        
        //NodeEntry CH = pilihanCH.get((int) (zone.zone.get(myNode.ZoneId).count%pilihanCH.size()));
        
        if (Konstanta.DIRECT_SEND) {
            NodeEntry sink = myNode.neighboursList.get(detailQueryProcessed.get(msg.queryId).sinkIP);
            if (sink != null) return sink;
        }
        
        return zone.zone.get(myNode.ZoneId).CH;
    }

    public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority, byte ttl) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (Konstanta.IS_PAUSE) myNode.getSimControl().setSpeed(SimManager.PAUSED);
            return;
        }
        
        if (this.netQueueFULL)
            return;

        //Reject semua pesan yang diterima jika tipe pesan tidak dikenali
        if (!(msg instanceof ProtocolMessageWrapper)) {
            return;
        }
        
        // Provide a basic visual feedback on the fact that 
        //this node has received a message */
        myNode.getNodeGUI().colorCode.mark(colorProfile,ColorProfileZRP.RECEIVE, 20);
        
        //Extract pesan ketipe wrapper
        ProtocolMessageWrapper rcvMsg = (ProtocolMessageWrapper)msg;

        //Operasi sesuai dengan tipe pesan
        if (rcvMsg.getPayload() instanceof MessageNodeDiscover) {
            //tipe pesan discovery, berikan ke fungsi handle MessageNodeDiscover
            //System.out.println("receive discover Node " + myNode.getID() + " got from Node " + ((MessageNodeDiscover)rcvMsg.getPayload()).nodeID);
            if (!receivedDataId.contains(String.valueOf(rcvMsg.getS_seq()))) {
                receivedDataId.add(String.valueOf(rcvMsg.getS_seq()));
                memoryControllerPacketID();
                handleMessageNodeDiscover((MessageNodeDiscover)rcvMsg.getPayload(),rcvMsg.getS_seq());
            }
        }
        else if (rcvMsg.getPayload() instanceof MessageQuery) {
            //tipe pesan query, cek apakah pesan query sudah pernah diterima sebelumnya
            //jika belum berikan ke fungsi handleMessageQuery
            //jika sudah abaikan
            if (!receivedDataId.contains(String.valueOf(rcvMsg.getS_seq()))) {
                receivedDataId.add(String.valueOf(rcvMsg.getS_seq()));
                memoryControllerPacketID();
                handleMessageQuery(rcvMsg);
                //System.out.println("receive query Node " + myNode.getID());
            }
            //System.out.println("receive query");
        }
        else if (rcvMsg.getPayload() instanceof MessagePoolDataValue) {
            //jika pesan duplicate, abaikan
            if (this.receivedDataId.contains(String.valueOf(rcvMsg.getS_seq())))
                return;
            //System.out.println("pool tiba di "+myNode.getID());
            receivedDataId.add(String.valueOf(rcvMsg.getS_seq()));
            memoryControllerPacketID();

            //bagian ini biasanya dipanggil jika pesan ini diterima pada sink node
            //lempar ke layer app
            //sendToAppLayer(rcvMsg.getPayload(), src);
            
            sendToAppLayer((MessagePoolDataValue)rcvMsg.getPayload(), null);
            //System.out.println("receive pool Node " + myNode.getID());
        }
        else if (rcvMsg.getPayload() instanceof MessageDataValue) {
            //jika pesan duplicate, abaikan
            if (this.receivedDataId.contains(String.valueOf(rcvMsg.getS_seq())))
                return;
            //System.out.println("pool tiba di "+myNode.getID());
            receivedDataId.add(String.valueOf(rcvMsg.getS_seq()));
            memoryControllerPacketID();

            //bagian ini biasanya dipanggil jika pesan ini diterima pada sink node
            //lempar ke layer app
            //sendToAppLayer(rcvMsg.getPayload(), src);
            
            sendToAppLayer((MessageDataValue)rcvMsg.getPayload(), null);
        }
    }
    
    private void handleMessageNodeDiscover(MessageNodeDiscover msg,long unikID) {
        
        NodeEntryDiscovery ned = new NodeEntryDiscovery(msg.nodeID, msg.ipAddress, msg.totalDiscoveredNode, msg.energyLeft, msg.zone_id);
        ned.addQueryProcessed(msg.queryProcessed);
        listTetangga.put(msg.ipAddress, ned);
        
        /*if (myNode.ZoneId == msg.zone_id && msg.is_tepi) {
            listTepi.put(msg.ipAddress, ned);
            
            ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(msg);
            pmw.setS_seq(unikID);
            NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), NetAddress.ANY, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
            sendToLinkLayer(nmip, NetAddress.ANY);
        }*/
    }
    
    private NetAddress convertMacToIP(MacAddress macAddr) {
        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList
        	= myNode.neighboursList.getAsLinkedList();

        for(NodeEntry nodeEntry: neighboursLinkedList) {
            if (nodeEntry.mac.hashCode() == macAddr.hashCode())
                return nodeEntry.ip;
        }

        return null;
    }

    @Override
    public void dropNotify(Message msg, MacAddress nextHopMac, Reason reason) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        System.out.println("Packet dropped notify");
    	if (reason == Reason.PACKET_SIZE_TOO_LARGE) {
    		System.out.println("WARNING: Packet size too large - unable to transmit");
    		throw new RuntimeException("Packet size too large - unable to transmit");
    	}
        if (reason == Reason.NET_QUEUE_FULL) {
            if (!this.netQueueFULL) {
                this.netQueueFULL = true;
                System.out.println("ERROR: Net Queue full node" + myNode.getID() + " TIME (SEC): " + (JistAPI.getTime() / Constants.SECOND));
                myNode.getNodeGUI().colorCode.mark(new ColorProfileGeneric(), ColorProfileGeneric.DEAD, ColorProfileGeneric.FOREVER);
                //throw new RuntimeException("Net Queue Full");
            }
        }
        if (reason == Reason.UNDELIVERABLE || reason == Reason.MAC_BUSY) {
            ProtocolMessageWrapper xMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();
            System.out.println("WARNING: Cannot relay packet to the destination node " + nextHopMac);
            
            //cek apakah batas retry masih bisa atau tidak
            if (isThisRetryAgain(xMsg.getS_seq())) {

                increaseThisRetry(xMsg.getS_seq());
                System.out.println("NODE:" + myNode.getID() + " Retrying(" + dataRetry.get(xMsg.getS_seq()) + ") PID:" + xMsg.getS_seq() + " send to node " + nextHopMac);

                //sleep before retry
                JistAPI.sleepBlock(Konstanta.INTERVAL_WAITING_BEFORE_RETRY);

                NetMessage.Ip nmip = (NetMessage.Ip) msg;

                if (xMsg.getPayload() instanceof MessageDataValue) {
                    //beritahu app layer agar menambah pool limit
                    MessageDataValue tmp_msg = (MessageDataValue) xMsg.getPayload();
                    DropperNotifyAppLayer dnal = new DropperNotifyAppLayer(false, true,tmp_msg.priority);
                    sendToAppLayer(dnal, myNode.getIP());
                }
                else if (xMsg.getPayload() instanceof MessagePoolDataValue) {
                    this.increment_TIMING_SEND();
                }
                NetAddress retryTo = convertMacToIP(nextHopMac);
                if (retryTo != null)
                    sendToLinkLayer(nmip, retryTo);
                else {
                    
                }
            }
            else {
                System.out.println("NODE:" + myNode.getID() + " Drop packet " + xMsg.getS_seq() + " after retry(" + dataRetry.get(xMsg.getS_seq()) + ") to node " + nextHopMac);
                deleteThisRetry(xMsg.getS_seq());
                myNode.neighboursList.remove(nextHopMac.hashCode());
            }
        }
        
        if (reason == Reason.PACKET_DELIVERED) {
            ProtocolMessageWrapper xMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();
            if (xMsg.getPayload() instanceof MessageDataValue) {
                //beritahu app layer agar mengurangi pool limit
                MessageDataValue tmp_msg = (MessageDataValue) xMsg.getPayload();
                DropperNotifyAppLayer dnal = new DropperNotifyAppLayer(true, false,tmp_msg.priority);
                sendToAppLayer(dnal, myNode.getIP());
            }
            else if (xMsg.getPayload() instanceof MessagePoolDataValue) {
                this.decrement_TIMING_SEND();
            }
            deleteThisRetry(xMsg.getS_seq());
        }
        
        // wait 5 seconds and try again
        /*JistAPI.sleepBlock(500 * Constants.MILLI_SECOND);
        netEntity.send((NetMessage.Ip)msg, Constants.NET_INTERFACE_DEFAULT, nextHopMac);
        this.send((NetMessage)msg);*/
    }

    public void start() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    public void setNetEntity(NetInterface netEntity)
    {
        if(!JistAPI.isEntity(netEntity)) throw new IllegalArgumentException("expected entity");
        if(this.netEntity!=null) throw new IllegalStateException("net entity already set");
        
        this.netEntity = netEntity;
    }

    
    public void setAppInterface(AppInterface appInterface)
    {
        this.appInterface = appInterface;
    }
    
    public void sendToAppLayer(Message msg, NetAddress src)
    {
    	// ignore if not enough energy
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (Konstanta.IS_PAUSE) myNode.getSimControl().setSpeed(SimManager.PAUSED);
            return;
        }

        appInterface.receive(msg, src, null, (byte)-1,
        					 NetAddress.LOCAL, (byte)-1, (byte)-1);
    }
    
    public byte sendToLinkLayer(NetMessage.Ip ipMsg, NetAddress nextHopDestIP)
    {
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (Konstanta.IS_PAUSE) myNode.getSimControl().setSpeed(SimManager.PAUSED);
            return 0;
        }
     
        /*myNode.getSimManager().getSimGUI()
		  .getAnimationDrawingTool()
		 	  .animate("ExpandingFadingCircle",
				       myNode.getNCS_Location2D());*/
        
//        if (myNode.getID() == 164)
//            System.out.println("route packet to " + nextHopDestIP);

        ProtocolMessageWrapper pmw = (ProtocolMessageWrapper)ipMsg.getPayload();
        NetMessage.Ip copyMsg = new NetMessage.Ip(pmw,
			       ((NetMessage.Ip)ipMsg).getSrc(),
                               ((NetMessage.Ip)ipMsg).getDst(),
                               ((NetMessage.Ip)ipMsg).getProtocol(),
                               ((NetMessage.Ip)ipMsg).getPriority(),
                               ((NetMessage.Ip)ipMsg).getTTL(),
                               ((NetMessage.Ip)ipMsg).getId(),
                               ((NetMessage.Ip)ipMsg).getFragOffset());
        ipMsg = null;

        if (nextHopDestIP == null)
            System.err.println("NULL nextHopDestIP");
        if (nextHopDestIP == NetAddress.ANY) {
            netEntity.send(copyMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
            registerIDToHashmapRetry(pmw.getS_seq());
        }
        else
        {
            NodeEntry nodeEntry = myNode.neighboursList.get(nextHopDestIP);
            if (nodeEntry == null)
            {
                 System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 new Exception().printStackTrace();
                 return this.ERROR; 
            }
            MacAddress macAddress = nodeEntry.mac;
            if (macAddress == null)
            {
                 System.err.println("Node #" + myNode.getID() + ": Destination IP (" + nextHopDestIP + ") not in my neighborhood. Please re-route! Are you sending the packet to yourself?");
                 System.err.println("Node #" + myNode.getID() + "has + " + myNode.neighboursList.size() + " neighbors");
                 return this.ERROR;
            }
            myNode.getNodeGUI().colorCode.mark(colorProfile, ColorProfileZRP.TRANSMIT, 2);
            netEntity.send(copyMsg, Constants.NET_INTERFACE_DEFAULT, macAddress);
            registerIDToHashmapRetry(pmw.getS_seq());
        }
        
        return this.SUCCESS;
    }
    
    public RouteInterface getProxy()
    {
        return self;
    }
    
    private void increaseThisRetry(long s_Seq) {
        int x = dataRetry.get(s_Seq) + 1;
        dataRetry.put(s_Seq, x);
    }

    private void deleteThisRetry(long s_Seq) {
        dataRetry.remove(s_Seq);
    }

    private boolean isThisRetryAgain(long s_Seq) {
        if (dataRetry.containsKey(s_Seq)) {
            return dataRetry.get(s_Seq) < Konstanta.MAXIMUM_RETRY_SEND_MESSAGE;
        } else {
            System.out.println("Packet " + s_Seq + " not registered, dropped!");
            return false;
        }
    }

    private void registerIDToHashmapRetry(long s_Seq) {
        if (!dataRetry.containsKey(s_Seq)) {
            dataRetry.put(s_Seq, 0);
        }
    }
    
    private void memoryControllerPacketID() {
        if (this.receivedDataId.size() >= Konstanta.LIMIT_PACKET_ID_SIZE) {
            this.receivedDataId.remove(0);
        }
    }
    
    private void increment_TIMING_SEND() {
        Konstanta.INTERVAL_TIMING_SEND+= (1 * Constants.SECOND);
    }
    
    private void decrement_TIMING_SEND() {
        if (Konstanta.INTERVAL_TIMING_SEND > 1 * Constants.SECOND) Konstanta.INTERVAL_TIMING_SEND-= (1 * Constants.SECOND);
    }
}