/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.routing;

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
    private static SequenceGenerator seq;
    public static Zone zone;
    private static final boolean is_pause = false;
    private static final boolean use_agg = false;

    public static final long INTERVAL_TIMING_SEND = 5 * Constants.SECOND;
    public static final long TIMING_DELAY_SEND = 200 * Constants.MILLI_SECOND;
    
    public static final int LIMIT_PACKET_ID_SIZE = 500;
    
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
    
    /*Pool received, key hashmap "QUERYID-NODEID"
     * contoh QUERYID = 10; NODEID = 6
     * jadinya 10-6 (STRING tipe)
     */
    
    private class poolReceivedItem {
        public double dataValue;
        public double maxdataValue;
        public double mindataValue;
        public double avgdataValue;
        public long countdataValue;
        public final long sequenceNumber;
        public int queryID;
        public int fromRegion;
        public int procedureID;

        public poolReceivedItem(double dataValue,long sequenceNumber,int queryID,int fromRegion,int procedureID) {
            this.dataValue = dataValue;
            this.sequenceNumber = sequenceNumber;
            this.queryID = queryID;
            this.fromRegion = fromRegion;
            this.procedureID = procedureID;
            this.maxdataValue = dataValue;
            this.mindataValue = dataValue;
            this.avgdataValue = dataValue;
            this.countdataValue = 1;
        }
        
        public void putAVG(double data) {
            avgdataValue = (avgdataValue*countdataValue + data)/(countdataValue+1);
        }
        
        public void putMAX(double data) {
            maxdataValue = (maxdataValue < data) ? data : maxdataValue;
        }
        
        public void putMIN(double data) {
            mindataValue = (mindataValue > data) ? data : mindataValue;
        }
    }
    
    private Map<String, poolReceivedItem> rcvPool = new HashMap<String, poolReceivedItem>();
    private ArrayList<poolReceivedItem> lstItemPool = new ArrayList<poolReceivedItem>();
    
    private NCS_Location2D arrowHead = null;
    private NCS_Location2D[] arrowHead_pool = new NCS_Location2D[16];
    
    private Color[] color = new Color[16];
    
    private boolean tepi = false;
    private boolean is_CH_now = false;
    
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
        
        //zone.zone.get(myNode.ZoneId).count++;
        zone.zone.get(myNode.ZoneId).changeCH();

        if (!rcvPool.isEmpty()) {
            
            lstItemPool.clear();
            
            Iterator i = rcvPool.entrySet().iterator();

            while (i.hasNext()) {
                Entry item = (Entry) i.next();
                poolReceivedItem pri = (poolReceivedItem)item.getValue();

                i.remove();
                lstItemPool.add(pri);
            }
            
            for (poolReceivedItem pri : lstItemPool) {
                long sequenceNumber = seq.getandincrement();
                
                MessagePoolDataValue mpdv = new MessagePoolDataValue(pri.dataValue,sequenceNumber);
                mpdv.queryID = pri.queryID;
                mpdv.sinkIP = detailQueryProcessed.get(pri.queryID).sinkIP;
                mpdv.sinkLocation = detailQueryProcessed.get(pri.queryID).sinkLocation;
                mpdv.producerNodeId = pri.procedureID;
                mpdv.zone_id = myNode.ZoneId;

                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(mpdv);
                pmw.setS_seq(sequenceNumber);
                
                stats.markPacketSent("DATA_"+myNode.ZoneId, sequenceNumber);
                stats.markPacketSent("DATA", sequenceNumber);
                System.out.println("create pool di "+myNode.getID()+" "+sequenceNumber);

                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), detailQueryProcessed.get(mpdv.queryID).sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                
                //if (loncat) sendToLinkLayer(nmip, nextHop.getIp());
                //else 
                
                JistAPI.sleep(TIMING_DELAY_SEND);
                
                handleMessagePool(nmip);
            }
            nodeDiscover();
            
            //handleChangeCH(myNode.getNCS_Location2D(),myNode.getEnergyManagement().getBattery().getPercentageEnergyLevel(),null);
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
        
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel() == 0) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (is_pause) myNode.getSimControl().setSpeed(SimManager.PAUSED);
        }
        
        // process only if there are energy reserves
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;
        
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
                
                if (use_agg) {
                    MessagePoolDataValue temp = (MessagePoolDataValue) sndMsg.getPayload();
                    stats.markPacketReceived("DATA", temp.sequenceNumber);
                    stats.markPacketReceived("DATA_"+temp.zone_id, temp.sequenceNumber);
                    System.out.println("receive pool di "+myNode.getID()+" "+temp.sequenceNumber);
                    
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
        
        NodeEntry next = findNextHop(mpdv.sinkLocation,mpdv.sinkIP,false);
        
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
                ((RouteInterface.ZRP_Route)self).timingSend(this.INTERVAL_TIMING_SEND);

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
    
    private NodeEntry findNextHop(NCS_Location2D loc,NetAddress ip,boolean intra) {
        
        NodeEntry nextHop = (intra) ? findNextHop_in(loc) : findNextHop_out(loc,ip);
        
        if (nextHop == null) {
            String a = (intra) ? "in" : "out";
            System.out.println("Random "+a+" "+myNode.getID());
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

        ArrayList<NodeEntry> list = new ArrayList<NodeEntry>();
        ArrayList<NodeEntry> list2 = new ArrayList<NodeEntry>();

        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();

        for(NodeEntry neighbourEntry: neighboursLinkedList) {
            //if (listTetangga.containsKey(neighbourEntry.ip)) {
                if (neighbourEntry.getNCS_Location2D().distanceTo(loc) < distance) {
                    list.add(neighbourEntry);
                }
                else list2.add(neighbourEntry);
            //}
        }

        double energi = -1;

        for(NodeEntry item : list) {
            if (listTetangga.containsKey(item.ip)) {
                if (listTetangga.get(item.ip).energyLeft > energi) {
                    nextHop = item;
                    energi = listTetangga.get(item.ip).energyLeft;
                }
            }
        }
        
        if (nextHop == null)  {
            for(NodeEntry item : list2) {
                if (listTetangga.containsKey(item.ip)) {
                    if (listTetangga.get(item.ip).energyLeft > energi) {
                        nextHop = item;
                        energi = listTetangga.get(item.ip).energyLeft;
                    }
                }
            }
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
        ip_ncsloc myCH = getMyClusterHead();
        
        NCS_Location2D CH_loc = myCH.getLoc();
        NetAddress CH_ip = myCH.getIp();
        
        if (msg.CH_ip == null) {
            msg.CH_ip = CH_ip;
            msg.CH_loc = CH_loc;
        }
        else {
            CH_ip = msg.CH_ip;
            CH_loc = msg.CH_loc;
        }
        
        if (arrowHead != null) {
            //topologyGUI.removeLink(myNode.getNCS_Location2D(),arrowHead, 1);
        }
        
        //tambahkan informasi region
        //msg.fromRegion = detailQueryProcessed.get(msg.queryId).regionProcessed;

        //  CH diri sendiri
        if (CH_ip == myNode.getIP()) {
            //System.out.println("Di node "+myNode.getID()+" pesan dari "+msg.producerNodeId+" CH diri sendiri "+CH_ip+" pool di "+myNode.getIP());
            
            stats.markPacketReceived("DATA", msg.sequenceNumber);
            stats.markPacketReceived("DATA_"+msg.zone_id, msg.sequenceNumber);
            System.out.println("receive value di "+myNode.getID()+" "+msg.sequenceNumber);
            
            poolHandleMessageDataValue(msg);
        }
        else {
            //cari next hop;
            NodeEntry nextHop = findNextHop(CH_loc,null,true);
            
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
        String hashMapKey = String.valueOf(msg.zone_id);

        //cek jika sudah terdapat
        if (rcvPool.containsKey(hashMapKey)) {
            //System.out.println(myNode.getID()+" duplicate pool "+ msg.sequenceNumber+" "+rcvPool.size());
            
            if (use_agg) {
                poolReceivedItem item_pool = rcvPool.get(hashMapKey);
                item_pool.putAVG(msg.dataValue);
                item_pool.putMAX(msg.dataValue);
                item_pool.putMIN(msg.dataValue);
                item_pool.countdataValue++;
            }
        }
        else {
            //tidak terdapat key
            //bikin entry queue langsung masukan

            //System.out.println("Node:" + myNode.getID() + " create new hashmapkey:" + hashMapKey);

            poolReceivedItem priNew = new poolReceivedItem(msg.dataValue,msg.sequenceNumber,1,msg.fromRegion,msg.producerNodeId);
            rcvPool.put(hashMapKey, priNew);
        }
    }
    
    private void poolHandleMessageDataValue(MessageDataValue msg) {
        //buat keyHashMap
        String hashMapKey = String.valueOf(msg.zone_id);

        //cek jika sudah terdapat
        if (rcvPool.containsKey(hashMapKey)) {
            //System.out.println(myNode.getID()+" duplicate pool "+ msg.sequenceNumber+" "+rcvPool.size());
            
            if (use_agg) {
                poolReceivedItem item_pool = rcvPool.get(hashMapKey);
                item_pool.putAVG(msg.dataValue);
                item_pool.putMAX(msg.dataValue);
                item_pool.putMIN(msg.dataValue);
                item_pool.countdataValue++;
            }
        }
        else {
            //tidak terdapat key
            //bikin entry queue langsung masukan

            //System.out.println("Node:" + myNode.getID() + " create new hashmapkey:" + hashMapKey);

            poolReceivedItem priNew = new poolReceivedItem(msg.dataValue,msg.sequenceNumber,msg.queryId,msg.fromRegion,msg.producerNodeId);
            rcvPool.put(hashMapKey, priNew);
        }
    }
    
    private class ip_ncsloc {
        private NetAddress ip;
        private NCS_Location2D loc;

        public ip_ncsloc(NetAddress ip, NCS_Location2D loc) {
            this.ip = ip;
            this.loc = loc;
        }

        public NetAddress getIp() {
            return ip;
        }

        public NCS_Location2D getLoc() {
            return loc;
        }
    }
    
    private ip_ncsloc getMyClusterHead() {
        //LinkedList<NodeEntry> pilihanCH = zone.zone.get(myNode.ZoneId).getZone_item().getAsLinkedList();
        
        //NodeEntry CH = pilihanCH.get((int) (zone.zone.get(myNode.ZoneId).count%pilihanCH.size()));
        NodeEntry CH = zone.zone.get(myNode.ZoneId).CH;
        this.is_CH_now = (CH.ip == myNode.getIP());
        
        return new ip_ncsloc(CH.ip,CH.getNCS_Location2D());
    }

    public void receive(Message msg, NetAddress src, MacAddress lastHop, byte macId, NetAddress dst, byte priority, byte ttl) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel() == 0) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (is_pause) myNode.getSimControl().setSpeed(SimManager.PAUSED);
        }
        
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;
        
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
        if (reason == Reason.UNDELIVERABLE || reason == Reason.MAC_BUSY)
            System.out.println("WARNING: Cannot relay packet to the destination node " + nextHopMac);
        
        // wait 5 seconds and try again
        JistAPI.sleepBlock(500 * Constants.MILLI_SECOND);
        netEntity.send((NetMessage.Ip)msg, Constants.NET_INTERFACE_DEFAULT, nextHopMac);
        this.send((NetMessage)msg);
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
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel() == 0) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (is_pause) myNode.getSimControl().setSpeed(SimManager.PAUSED);
        }
        
    	// ignore if not enough energy
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return;

        appInterface.receive(msg, src, null, (byte)-1,
        					 NetAddress.LOCAL, (byte)-1, (byte)-1);
    }
    
    public byte sendToLinkLayer(NetMessage.Ip ipMsg, NetAddress nextHopDestIP)
    {
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel() == 0) {
            System.out.println("Mati "+myNode.getID()+" "+JistAPI.getTime());
            if (is_pause) myNode.getSimControl().setSpeed(SimManager.PAUSED);
        }
        
        if (myNode.getEnergyManagement()
        		  .getBattery()
        		  .getPercentageEnergyLevel()< 2)
            return 0;
     
        /*myNode.getSimManager().getSimGUI()
		  .getAnimationDrawingTool()
		 	  .animate("ExpandingFadingCircle",
				       myNode.getNCS_Location2D());*/
        
//        if (myNode.getID() == 164)
//            System.out.println("route packet to " + nextHopDestIP);

        if (nextHopDestIP == null)
            System.err.println("NULL nextHopDestIP");
        if (nextHopDestIP == NetAddress.ANY)
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, MacAddress.ANY);
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
            netEntity.send(ipMsg, Constants.NET_INTERFACE_DEFAULT, macAddress);
        }
        
        return this.SUCCESS;
    }
    
    public RouteInterface getProxy()
    {
        return self;
    }
    
    private void memoryControllerPacketID() {
        if (this.receivedDataId.size() >= this.LIMIT_PACKET_ID_SIZE) {
            this.receivedDataId.remove(0);
        }
    }
}