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
import sidnet.stack.users.ZRP_route.app.DropperNotify;
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
    
    NetAddress sinkIP;
    public NCS_Location2D sinkLocation;
    
    private Map<String, PoolReceivedItem> rcvPool = new HashMap<String, PoolReceivedItem>();
    private ArrayList<PoolReceivedItem> lstItemPool = new ArrayList<PoolReceivedItem>();
    
    private NCS_Location2D arrowHead = null;
    private NCS_Location2D[] arrowHead_pool = new NCS_Location2D[16];
    
    ArrayList<NodeEntry> list_in = new ArrayList<NodeEntry>();
    ArrayList<NodeEntry> list_out = new ArrayList<NodeEntry>();
    
    /** Creates a new instance
     *
     * @param myNode              the SIDnet node handle to access 
     * 				  its GUI-primitives and shared environment
     * @param stats
     * @param seq
     * @param zone
     */
    public RoutingProtocol(Node myNode,StatsCollector stats,SequenceGenerator seq,Zone zone) {
        this.myNode = myNode;
        this.stats = stats;
        this.seq = seq;
        this.zone = zone;
        
        /** Create a proxy for the application layer of this node */
        self = (RouteInterface.ZRP_Route)JistAPI.proxy(this, RouteInterface.ZRP_Route.class);
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
                mpdv.sinkIP = this.sinkIP;
                mpdv.sinkLocation = this.sinkLocation;
                mpdv.zone_id = myNode.ZoneId;
                mpdv.sequenceNumber = sequenceNumber;
                mpdv.priority = pri.priority;

                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(mpdv);
                pmw.setS_seq(sequenceNumber);
                
                //stats.markPacketSent("DATA_"+myNode.ZoneId, sequenceNumber);
                stats.markPacketSent("DATA", sequenceNumber);
                stats.markPacketSent("DATA_PRI_"+mpdv.priority, sequenceNumber);
                System.out.println("create pool di "+mpdv.priority+" "+myNode.getID()+" "+sequenceNumber);

                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), this.sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                
                JistAPI.sleep(Konstanta.TIMING_DELAY_SEND);
                
                sendMessage(nmip);
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
                
                if (((MessagePoolDataValue) sndMsg.getPayload()).priority == 2) {
                    MessagePoolDataValue temp = (MessagePoolDataValue) sndMsg.getPayload();
                    stats.markPacketReceived("DATA", sndMsg.getS_seq());
                    stats.markPacketReceived("DATA_PRI_"+temp.priority, sndMsg.getS_seq());
                    //stats.markPacketReceived("DATA_"+temp.zone_id, sndMsg.getS_seq());
                    //System.out.println("receive pool di "+myNode.getID()+" "+sndMsg.getS_seq());
                    //System.out.println("receive pool di "+temp.priority+" "+myNode.getID()+" "+sndMsg.getS_seq());
                    
                    poolMessage(
                            temp.priority,
                            temp.zone_id,
                            temp.sequenceNumber,
                            temp.avgdataValue,
                            temp.countdataValue,
                            temp.maxdataValue,
                            temp.mindataValue,
                            temp.queryID
                    );
                    
                }
                else sendMessage(msg);
            }
        }
    }
    
    private void sendMessage(NetMessage msg) {
        //Extract pesan ketipe wrapper
        ProtocolMessageWrapper sndMsg = (ProtocolMessageWrapper)((NetMessage.Ip)msg).getPayload();

        NodeEntry next = null;
        long seq = 0;
        
        //extract pesan
        if (sndMsg.getPayload() instanceof MessageDataValue) {
            MessageDataValue this_msg = (MessageDataValue)sndMsg.getPayload();
            
            seq = this_msg.sequenceNumber;
            
            //cari next hop;
            NodeEntry nextHop = findNextHop(sinkLocation,null,true);
            if (nextHop == null) {
                System.out.println("NODE:" + myNode.getID() + " Drop packet " + seq + " tidak punya node tetangga");
                myNode.stop = true;
            }
            else {
                //System.out.println("Di node "+myNode.getID()+" pesan dari "+msg.producerNodeId+" CH node lain "+CH_ip+" kirim ke "+nextHop.ip);
                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(msg);
                pmw.setS_seq(seq);
                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), this.sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                sendToLinkLayer(nmip, nextHop.ip);

                arrowHead = nextHop.getNCS_Location2D();
                topologyGUI.addLink(myNode.getNCS_Location2D(),arrowHead, 1, Color.BLACK, TopologyGUI.HeadType.LEAD_ARROW);
            }
        }
        else if (sndMsg.getPayload() instanceof MessagePoolDataValue) {
            MessagePoolDataValue this_msg = (MessagePoolDataValue)sndMsg.getPayload();
            
            seq = this_msg.sequenceNumber;
            sinkIP = this_msg.sinkIP;
            
            next = findNextHop(this_msg.sinkLocation,this_msg.sinkIP,false);
            
            if (next != null) {
                NetMessage.Ip nmip = new NetMessage.Ip(sndMsg, myNode.getIP(), this_msg.sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                sendToLinkLayer(nmip, next.ip);

                arrowHead_pool[myNode.ZoneId] = next.getNCS_Location2D();
                Color colour = Konstanta.color[this_msg.zone_id];
                topologyGUI.addLink(myNode.getNCS_Location2D(), arrowHead_pool[myNode.ZoneId], this_msg.zone_id+3,colour, TopologyGUI.HeadType.LEAD_ARROW);
            }
        }
        
        if (next == null) {
            System.out.println("NODE:" + myNode.getID() + " Drop packet " + seq + " tidak punya node tetangga");
            myNode.stop = true;
        }
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

                this.sinkIP = query.getQuery().getSinkIP();
                this.sinkLocation = query.getQuery().getSinkNCSLocation2D();
                
                this.queryProcessed.add(query.getQuery().getID());

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
        
        return nextHop;
    }
    
    private NodeEntry findNextHop_in(NCS_Location2D loc) {
        NodeEntry nextHop = null;
        
        double shortestNodeDistance = -1;
        
        //ambil list tetangga yang dibuat oleh heartbeat
        LinkedList<NodeEntry> neighboursLinkedList = myNode.neighboursList.getAsLinkedList();
        
        //ambil list tetangga yang dibuat oleh zone
        LinkedList<NodeEntry> zoneLinkedList = zone.zone.get(myNode.ZoneId).getZone_item().getAsLinkedList();
        
        LinkedList<NodeEntry> neighboursZoneLinkedList = null;

        if (Konstanta.USE_CH_POOL) {
            myNode.neighbourZoneList = new NodesList();

            for(NodeEntry neighbourEntry: neighboursLinkedList) {
                for(NodeEntry zoneEntry: zoneLinkedList) {
                    if (zoneEntry.ip == neighbourEntry.ip) {
                        myNode.neighbourZoneList.add(neighbourEntry.ip,neighbourEntry);
                    }
                }
            }

            neighboursZoneLinkedList = myNode.neighbourZoneList.getAsLinkedList();
        }
        else {
            neighboursZoneLinkedList = myNode.neighboursList.getAsLinkedList();
        }
        if (neighboursZoneLinkedList != null && neighboursZoneLinkedList.size() > 0) {
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

        if (list_in.isEmpty() && list_out.isEmpty()) {
            list_in.clear();
            list_out.clear();
            for(NodeEntry neighbourEntry: neighboursLinkedList) {
                //if (listTetangga.containsKey(neighbourEntry.ip)) {
                    if (neighbourEntry.getNCS_Location2D().distanceTo(loc) < distance) {
                        list_in.add(neighbourEntry);
                    }
                    else list_out.add(neighbourEntry);
                //}
            }
        }

        if (!list_in.isEmpty()) {
            nextHop = list_in.get(0);
            list_in.remove(0);
        }
        
        if (nextHop == null)  {
            nextHop = list_out.get(0);
            list_out.remove(0);
        }

        return nextHop;
    }
    
    private void handleMessageDataValue(MessageDataValue msg) {
        //cari cluster head
        NodeEntry myCH = getMyClusterHead();
        
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
            //System.out.println("receive value di "+msg.priority+" "+myNode.getID()+" "+msg.sequenceNumber);
            
            poolMessage(
                    msg.priority,
                    msg.zone_id,
                    msg.sequenceNumber,
                    msg.dataValue,
                    msg.count,
                    msg.maxdataValue,
                    msg.mindataValue,
                    msg.queryId
            );
        }
        else {
            //cari next hop;
            NodeEntry nextHop = findNextHop(CH_loc,null,true);
            if (nextHop == null) {
                System.out.println("NODE:" + myNode.getID() + " Drop packet " + msg.sequenceNumber + " tidak punya node tetangga");
                myNode.stop = true;
            }
            else {
                //System.out.println("Di node "+myNode.getID()+" pesan dari "+msg.producerNodeId+" CH node lain "+CH_ip+" kirim ke "+nextHop.ip);
                ProtocolMessageWrapper pmw = new ProtocolMessageWrapper(msg);
                pmw.setS_seq(msg.sequenceNumber);
                NetMessage.Ip nmip = new NetMessage.Ip(pmw, myNode.getIP(), sinkIP, Constants.NET_PROTOCOL_INDEX_1, Constants.NET_PRIORITY_NORMAL, (byte)100);
                sendToLinkLayer(nmip, nextHop.ip);

                arrowHead = nextHop.getNCS_Location2D();
                topologyGUI.addLink(myNode.getNCS_Location2D(),arrowHead, 1, Color.BLACK, TopologyGUI.HeadType.LEAD_ARROW);
            }
        }
    }
    
    private void poolMessage(int priority,int zone_id,long seq,double avg,long count,double max,double min,int queryID) {
        //buat keyHashMap
        String hashMapKey = priority+"-"+String.valueOf(zone_id);
        if (! Konstanta.USE_CH_POOL) hashMapKey = hashMapKey+"-"+seq;

        //cek jika sudah terdapat
        if (rcvPool.containsKey(hashMapKey)) {
            //System.out.println(myNode.getID()+" duplicate pool "+ msg.sequenceNumber+" "+rcvPool.size());
            PoolReceivedItem item_pool = rcvPool.get(hashMapKey);
            item_pool.putAVG(avg,count);
            item_pool.putMAX(max);
            item_pool.putMIN(min);
        }
        else {
            //tidak terdapat key
            //bikin entry queue langsung masukan
            PoolReceivedItem priNew = new PoolReceivedItem(max,min,avg,count,queryID,priority);
            rcvPool.put(hashMapKey, priNew);
        }
    }
    
    private NodeEntry getMyClusterHead() {
        //LinkedList<NodeEntry> pilihanCH = zone.zone.get(myNode.ZoneId).getZone_item().getAsLinkedList();
        
        //NodeEntry CH = pilihanCH.get((int) (zone.zone.get(myNode.ZoneId).count%pilihanCH.size()));
        
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
            sendToAppLayer((MessageDataValue)rcvMsg.getPayload(), null);
        }
    }
    
    private void handleMessageNodeDiscover(MessageNodeDiscover msg,long unikID) {
        
        NodeEntryDiscovery ned = new NodeEntryDiscovery(msg.nodeID, msg.ipAddress, msg.totalDiscoveredNode, msg.energyLeft, msg.zone_id);
        ned.addQueryProcessed(msg.queryProcessed);
        listTetangga.put(msg.ipAddress, ned);
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
                    DropperNotify dnal = new DropperNotify(false, true,tmp_msg.priority);
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
                DropperNotify dnal = new DropperNotify(true, false,tmp_msg.priority);
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