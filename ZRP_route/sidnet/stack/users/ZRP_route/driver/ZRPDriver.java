/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.driver;

import java.awt.Color;
import jist.runtime.JistAPI;
import jist.swans.Constants;
import jist.swans.field.Fading;
import jist.swans.field.Field;
import jist.swans.field.Mobility;
import jist.swans.field.PathLoss;
import jist.swans.field.Placement;
import jist.swans.field.Spatial;
import jist.swans.mac.MacAddress;
import jist.swans.misc.Location;
import jist.swans.misc.Mapper;
import jist.swans.misc.Util;
import jist.swans.net.NetAddress;
import jist.swans.net.NetIp;
import jist.swans.net.PacketLoss;
import jist.swans.radio.RadioInfo;
import sidnet.core.gui.GroupSelectionTool;
import sidnet.core.gui.PanelContext;
import sidnet.core.gui.SimGUI;
import sidnet.core.gui.TopologyGUI;
import sidnet.core.interfaces.GPS;
import sidnet.core.misc.GPSimpl;
import sidnet.core.misc.Location2D;
import sidnet.core.misc.LocationContext;
import sidnet.core.misc.Node;
import sidnet.core.misc.NodeEntry;
import sidnet.core.simcontrol.SimManager;
import sidnet.models.energy.batteries.Battery;
import sidnet.models.energy.batteries.BatteryUtils;
import sidnet.models.energy.batteries.IdealBattery;
import sidnet.models.energy.energyconsumptionmodels.EnergyConsumptionModel;
import sidnet.models.energy.energyconsumptionmodels.EnergyConsumptionModelImpl;
import sidnet.models.energy.energyconsumptionmodels.EnergyManagement;
import sidnet.models.energy.energyconsumptionmodels.EnergyManagementImpl;
import sidnet.models.energy.energyconsumptionparameters.ElectricParameters;
import sidnet.models.energy.energyconsumptionparameters.EnergyConsumptionParameters;
import sidnet.models.senseable.phenomena.GenericDynamicPhenomenon;
import sidnet.models.senseable.phenomena.PhenomenaLayerInterface;
import sidnet.stack.std.mac.ieee802_15_4.Mac802_15_4Impl;
import sidnet.stack.std.mac.ieee802_15_4.Phy802_15_4Impl;
import sidnet.stack.std.routing.heartbeat.HeartbeatProtocol;
import sidnet.utilityviews.energymap.EnergyMap;
import sidnet.utilityviews.statscollector.StatEntry_Time;
import sidnet.utilityviews.statscollector.StatsCollector;
import sidnet.utilityviews.statscollector.StatEntry_EnergyLeftPercentage;

import sidnet.stack.users.ZRP_route.app.AppLayer;
import sidnet.stack.users.ZRP_route.app.CSV_Statistics;
import sidnet.stack.users.ZRP_route.app.Konstanta;
import sidnet.stack.users.ZRP_route.colorprofile.ColorProfileZRP;
import sidnet.stack.users.ZRP_route.ignoredpackage.CSV_NodeDie;
import sidnet.stack.users.ZRP_route.routing.RoutingProtocol;
import sidnet.utilityviews.statscollector.StatEntry_GeneralPurposeContor;
import sidnet.utilityviews.statscollector.StatEntry_PacketDeliveryLatency;
import sidnet.utilityviews.statscollector.StatEntry_PacketReceivedContor;
import sidnet.utilityviews.statscollector.StatEntry_PacketReceivedPercentage;
import sidnet.utilityviews.statscollector.StatEntry_PacketSentContor;

/**
 *
 * @author ZRP
 */
public class ZRPDriver {
    public static Cluster[] listCluster = new Cluster[16];
    private static CSV_Statistics csv_stats;
    private static CSV_NodeDie csv_nodedie;
    
    public static TopologyGUI topologyGUI = new TopologyGUI();
    public static int nodes, fieldLength, time;
    
    public static SequenceGenerator seq = new SequenceGenerator();
    
    /** Define the battery-type for the nodes 75mAh should give enough juice for 24-48h */
    public static Battery battery = new IdealBattery(BatteryUtils.mAhToMJ(75, 3), 3);
    public static Battery battery_sink = new IdealBattery(BatteryUtils.mAhToMJ(Konstanta.BATTERY_SINK, 3), 3);
    
    /** Define the power-consumption characteristics of the nodes, based on Mica Mote MPR500CA */
    public static EnergyConsumptionParameters eCostParam = new EnergyConsumptionParameters(
                                                        new ElectricParameters(     8,   // ProcessorCurrentDrawn_ActiveMode [mA],
                                                                                0.015,   // ProcessorCurrentDrawn_SleepMode [mA],
                                                                                   27,   // RadioCurrentDrawn_TransmitMode [mA],
                                                                                   10,   // RadioCurrentDrawn_ReceiveMode [mA],
                                                                                    3,   // RadioCurrentDrawn_ListenMode [mA],
                                                                                  0.5,   // RadioCurrentDrawn_SleepMode [mA],
                                                                                   10,   // SensorCurrentDrawn_ActiveMode [mA]
                                                                                  0.01   // SensorCurrentDrawn_PassiveMode [mA]
                                                                           ),
                                                        battery.getVoltage());
      
    /** This is the entry point in the program */
    public static void main(String[] args)
    {
        /** Command line arguments is the best way to configure run-time parameters, for now */        
        if(args.length < 3)
        {
           System.out.println("syntax: swans driver.Driver_SampleP2P_w802_15_4 <nodes> <field-length [m]> <max-simulation time>");
           System.out.println("    eg: swans driver.Driver_SampleP2P_w802_15_4    5          100                  50000");
           return;
        }
        
        System.out.println("Driver initialization started ... ");
        
        /* Parse command line arguments */
        nodes  = Integer.parseInt(args[0]);
        fieldLength = Integer.parseInt(args[1]);
        time   = Integer.parseInt(args[2]);
        
        /** Computing some statistics basic */
        float density = nodes / (float)(fieldLength/1000.0 * fieldLength/1000.0);
        System.out.println("nodes   = "+nodes);
        System.out.println("size    = "+fieldLength+" x "+fieldLength);
        System.out.println("time    = "+time+" seconds");
        System.out.print("Creating simulation nodes ... ");
        
        /** Create the simulation */
        Field f = createSim(nodes, fieldLength);
        
        System.out.println("Average density = "+f.computeDensity()*1000*1000+"/km^2");
        System.out.println("Average sensing = "+f.computeAvgConnectivity(true));
        System.out.println("Average receive = "+f.computeAvgConnectivity(false));
        
        /** Indicates WHEN the JiST simulation should self-terminate (automatically) */
        JistAPI.endAt(time * Constants.SECOND); /* so it will self-terminate after "time" seconds. Not the way we specify the unit of time */
        
        System.out.println("Driver initialization complete!");     
    }
  
  /**
   * Initialize simulation environment and field
   *
   * @param nodes number of nodes
   * @param length length of field
   * @return simulation field
   */
  public static Field createSim(int nodes, int length)
  {
    System.out.println("[ZRP Routing] : createSim()");
    
    // create cluster
    cluster(length);
      
    /** Launch the SIDnet main graphical interface and set-up the title */       
    SimGUI simGUI = new SimGUI();
    simGUI.appendTitle("ZRP Routing");
    
    /** Internal stuff: configure and start the simulation manager. Hook up control for GUI panels*/
    SimManager simManager = new SimManager(simGUI, null, SimManager.DEMO);
    
    /** Configure the SWANS: */
    
    /** Nodes deployment: random (but it can be XML-based, grid, manual place, air-dropped, etc */
    Location.Location2D bounds = new Location.Location2D(length, length);
    Placement placement = new Placement.Random(bounds);
    
    /** Nodes mobility: static (but nodes can move if you need to */
    Mobility mobility   = new Mobility.Static();
    
    /** Some other internals: Spatial configuration */
    Spatial spatial = new Spatial.HierGrid(bounds, 5);
    Fading fading = new Fading.None();
    PathLoss pathloss = new PathLoss.FreeSpace();
    Field field = new Field(spatial, fading, pathloss, mobility, Constants.PROPAGATION_LIMIT_DEFAULT);

    /** Configure the radio environment properties */
    
    //  for node
    RadioInfo.RadioInfoShared radioInfoShared = RadioInfo.createShared(
        Constants.FREQUENCY_DEFAULT, Konstanta.BANDWIDTH_SRC /* BANDWIDTH bps - it will be overloaded when using 802_15_4  */,
        -12 /* dBm for Mica Z */, Constants.GAIN_DEFAULT,
        Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT),
        Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);
    //  for sink node
    RadioInfo.RadioInfoShared radioInfoShared_sink = RadioInfo.createShared(
        Constants.FREQUENCY_DEFAULT, Konstanta.BANDWIDTH_SINK /* BANDWIDTH bps - it will be overloaded when using 802_15_4  */,
        -12 /* dBm for Mica Z */, Constants.GAIN_DEFAULT,
        Util.fromDB(Constants.SENSITIVITY_DEFAULT), Util.fromDB(Constants.THRESHOLD_DEFAULT),
        Constants.TEMPERATURE_DEFAULT, Constants.TEMPERATURE_FACTOR_DEFAULT, Constants.AMBIENT_NOISE_DEFAULT);

    /** Build up the networking stack: APP, NETWORK, MAC
     *  Technically, at the Network Layer you may have several "protocols". 
     *  We keep a mapping of these protocols (indexed) so that a packet may be forwarded to the proper protocol to be handled */
    Mapper protMap = new Mapper(Constants.NET_PROTOCOL_MAX);
    protMap.mapToNext(Constants.NET_PROTOCOL_HEARTBEAT); // Constants.NET_PROTOCOL_HEARTBEAT is just a numerical value to uniquely identify (index) one of the protocols (the node discovery one)
    protMap.mapToNext(Constants.NET_PROTOCOL_INDEX_1); // and this will be the other protocol, which is, in this case, a shortest-path routing protocol.
    
    /** We'll assume no packet loss due to "random" conditions. Packets may still be lost due to collisions though
     *  This should be the case when developing the first-time implementation, then you can remove this constraint if you want to test your rezilience 
     */
    PacketLoss pl = new PacketLoss.Zero();
  
    /* ******************************************
     * Create the SIDnet-specific simulation environment  *
     * ******************************************/
    
    /* Creating the SIDnet nodes */
    Node[] myNode = new Node[nodes];
    LocationContext fieldContext = new LocationContext(length, length);
    
    /** StatsCollector Hook-up - to allow you to see a quick-stat including elapsed time, number of packet lost, and so on. Also used to perform run-time logging */
    StatsCollector statistics = new StatsCollector(myNode, length, 0, 30 * Constants.SECOND);
    statistics.monitor(new StatEntry_Time());
    
    statistics.monitor(new StatEntry_EnergyLeftPercentage("ALL-NODES", StatEntry_EnergyLeftPercentage.MODE.AVG));
    
    statistics.monitor(new StatEntry_AliveNodesCount("NCA", 5));
    statistics.monitor(new StatEntry_DeadNodesCount("NCD", 5));
    
    StatEntry_GeneralPurposeContor createdCounter = new StatEntry_GeneralPurposeContor("Created");
    StatEntry_GeneralPurposeContor receivedCounter = new StatEntry_GeneralPurposeContor("Received");
    
    statistics.monitor(createdCounter);
    statistics.monitor(receivedCounter);
    
    statistics.monitor(new StatEntry_PacketSentContor("DATA"));
    statistics.monitor(new StatEntry_PacketReceivedContor("DATA"));
    statistics.monitor(new StatEntry_PacketReceivedPercentage("DATA"));
    statistics.monitor(new StatEntry_PacketDeliveryLatency("DATA", StatEntry_PacketDeliveryLatency.MODE.MAX));
    statistics.monitor(new StatEntry_PacketDeliveryLatency("DATA", StatEntry_PacketDeliveryLatency.MODE.AVG));
    
    /*StatEntry_GeneralPurposeContor createdCounter_All = new StatEntry_GeneralPurposeContor("AV_Created");
    StatEntry_GeneralPurposeContor receivedCounter_All = new StatEntry_GeneralPurposeContor("AV_Received");*/
    
    /*StatEntry_GeneralPurposeContor[] createdCounter = new StatEntry_GeneralPurposeContor[ZONE_COUNT];
    StatEntry_GeneralPurposeContor[] receivedCounter = new StatEntry_GeneralPurposeContor[ZONE_COUNT];*/
    /*for (int i=0;i<Konstanta.ZONE_COUNT;i++) {
        statistics.monitor(new StatEntry_PacketSentContor("DATA_"+i));
        statistics.monitor(new StatEntry_PacketReceivedContor("DATA_"+i));
        statistics.monitor(new StatEntry_PacketReceivedPercentage("DATA_"+i));
        statistics.monitor(new StatEntry_PacketDeliveryLatency("DATA_"+i, StatEntry_PacketDeliveryLatency.MODE.MAX));
    }*/
    
    StatEntry_GeneralPurposeContor createdCounter1 = new StatEntry_GeneralPurposeContor("Created_PRI_1");
    StatEntry_GeneralPurposeContor receivedCounter1 = new StatEntry_GeneralPurposeContor("Received_PRI_1");
    
    statistics.monitor(createdCounter1);
    statistics.monitor(receivedCounter1);
    
    statistics.monitor(new StatEntry_PacketSentContor("DATA_PRI_1"));
    statistics.monitor(new StatEntry_PacketReceivedContor("DATA_PRI_1"));
    statistics.monitor(new StatEntry_PacketReceivedPercentage("DATA_PRI_1"));
    statistics.monitor(new StatEntry_PacketDeliveryLatency("DATA_PRI_1", StatEntry_PacketDeliveryLatency.MODE.MAX));
    statistics.monitor(new StatEntry_PacketDeliveryLatency("DATA_PRI_1", StatEntry_PacketDeliveryLatency.MODE.AVG));
    
    StatEntry_GeneralPurposeContor createdCounter2 = new StatEntry_GeneralPurposeContor("Created_PRI_2");
    StatEntry_GeneralPurposeContor receivedCounter2 = new StatEntry_GeneralPurposeContor("Received_PRI_2");
    
    statistics.monitor(createdCounter2);
    statistics.monitor(receivedCounter2);
    
    statistics.monitor(new StatEntry_PacketSentContor("DATA_PRI_2"));
    statistics.monitor(new StatEntry_PacketReceivedContor("DATA_PRI_2"));
    statistics.monitor(new StatEntry_PacketReceivedPercentage("DATA_PRI_2"));
    statistics.monitor(new StatEntry_PacketDeliveryLatency("DATA_PRI_2", StatEntry_PacketDeliveryLatency.MODE.MAX));
    statistics.monitor(new StatEntry_PacketDeliveryLatency("DATA_PRI_2", StatEntry_PacketDeliveryLatency.MODE.AVG));
    
    /*statistics.monitor(createdCounter_All);
    statistics.monitor(receivedCounter_All);
    statistics.monitor(new StatEntry_PacketDeliveryRatio("AV_RatioDelivery", createdCounter_All, receivedCounter_All));*/
    
    /** Create the sensor nodes (each at a time). Initialize each node's data and network stack */
        
    csv_stats = new CSV_Statistics(statistics);
    csv_nodedie = new CSV_NodeDie();
    
    int sink = Konstanta.SINK;
    //int sink = (int) (Math.random() * nodes + 0);
      System.out.println("sink = "+sink);
    for(int i=0; i<nodes; i++) {
        if (i == sink) {
            myNode[i] = createSink(
                i, 
                field, 
                placement, 
                protMap, 
                radioInfoShared_sink, 
                pl, 
                pl, 
                simGUI.getSensorsPanelContext(), 
                fieldContext, 
                simManager, 
                statistics, 
                topologyGUI);
        }
        else {
            myNode[i] = createNode(
                i, 
                field, 
                placement, 
                protMap, 
                radioInfoShared, 
                pl, 
                pl, 
                simGUI.getSensorsPanelContext(), 
                fieldContext, 
                simManager, 
                statistics, 
                topologyGUI);
        }
        
        int zone_id = generate_zone_id(myNode[i].getLocation2D(),
                        length,
                        (int) Math.ceil(Math.sqrt(Konstanta.ZONE_COUNT)));
        
        myNode[i].setZoneId(zone_id);
        myNode[i].csv_deathnode = csv_nodedie;
        bobotCluster(myNode[i]);
        //System.out.println(i+" "+myNode[i].getLocation2D().getX()+" "+myNode[i].getLocation2D().getY()+" "+posisi);
    }
    
    simManager.registerAndRun(statistics, simGUI.getUtilityPanelContext2()); // Indicate where do you want this to show up on the GUI
    simManager.registerAndRun(topologyGUI, simGUI.getSensorsPanelContext());
    topologyGUI.setNodeList(myNode);
    
    /** Configuring the sensorial layer - give the node something to sense, measure */
    PhenomenaLayerInterface phenomenaLayer = new GenericDynamicPhenomenon(); // but it can be something else, such as a moving-objects field
    simManager.registerAndRun(phenomenaLayer,simGUI.getSensorsPanelContext());     // needs to be done ... internals
    
    topologyGUI.addLink(new Location2D(length/4,0).toNCS(fieldContext), new Location2D(length/4,length).toNCS(fieldContext), 0, Color.RED, TopologyGUI.HeadType.NO_ARROW);
    topologyGUI.addLink(new Location2D(length/2,0).toNCS(fieldContext), new Location2D(length/2,length).toNCS(fieldContext), 0, Color.RED, TopologyGUI.HeadType.NO_ARROW);
    topologyGUI.addLink(new Location2D(length*3/4,0).toNCS(fieldContext), new Location2D(length*3/4,length).toNCS(fieldContext), 0, Color.RED, TopologyGUI.HeadType.NO_ARROW);
    topologyGUI.addLink(new Location2D(0,length/4).toNCS(fieldContext), new Location2D(length,length/4).toNCS(fieldContext), 0, Color.RED, TopologyGUI.HeadType.NO_ARROW);
    topologyGUI.addLink(new Location2D(0,length/2).toNCS(fieldContext), new Location2D(length,length/2).toNCS(fieldContext), 0, Color.RED, TopologyGUI.HeadType.NO_ARROW);
    topologyGUI.addLink(new Location2D(0,length*3/4).toNCS(fieldContext), new Location2D(length,length*3/4).toNCS(fieldContext), 0, Color.RED, TopologyGUI.HeadType.NO_ARROW);
    
    /** All the nodes will measure the same environment in this case, but this is not a limitation. You can have them heterogeneous */
    for (int i = 0; i < nodes; i++) {
        myNode[i].addSensor(phenomenaLayer);
        myNode[i].myCluster = listCluster[myNode[i].ClusterId];
        System.out.println("Node "+i+" "+myNode[i].ClusterId+" "+myNode[i].myCluster.id);
        //System.out.println("Node "+i+" Zone "+myNode[i].ClusterId+" "+(listCluster[myNode[i].ClusterId].CHCluster.contains(myNode[i].getIP()) ? "CH" : "non"));
    }
    
    for (int i = 0; i < Konstanta.ZONE_COUNT; i++) {
        System.out.println("ZONE "+i+" horizontal "+listCluster[i].horizontal+" vertical "+listCluster[i].vertical+" CH "+listCluster[i].CHCluster.size()+" Anggota "+listCluster[i].myCluster.size());
    }
    /** Allow simManager to handle nodes' GUI (internals)*/
    simManager.register(myNode);
          
    /** EnergyMap hookup - give an overall view of the energy levels in the networks */
    EnergyMap energyMap = new EnergyMap(myNode); 
    simManager.registerAndRun(energyMap, simGUI.getUtilityPanelContext1()); // Indicate where do you want this to show up on the GUI
    
    /** Add GroupInteraction capability - if you may want to be able to select a group of nodes */
    GroupSelectionTool gst = new GroupSelectionTool(myNode);
    simManager.registerAndRun(gst, simGUI.getSensorsPanelContext());
    myNode[0].getNodeGUI().setGroupSelectionTool(gst); // internals 
   
    /** Starts the core (GUI) engine */
    simManager.getProxy().run();
    
    System.out.println("Simulation Started");
    
    return field;
  }
  
    private static void cluster(int length) {
        double interval = length/Math.round(Math.sqrt(Konstanta.ZONE_COUNT));
        int i=0;
        double horizontal=interval/2,vertical=interval/2;
        while (i < Konstanta.ZONE_COUNT) {
            listCluster[i] = new Cluster(i,new Location2D(horizontal,vertical));
            listCluster[i].color = Konstanta.color[i];
            i++;
            horizontal+=interval;
            if (horizontal > length) {
                horizontal = interval/2;
                vertical+=interval;
            }
        }
    }
    
    private static void bobotCluster(Node myNode) {
        double mid_x = listCluster[myNode.ClusterId].middle.getX();
        double mid_y = listCluster[myNode.ClusterId].middle.getY();
        
        double node_x = myNode.getLocation2D().getX();
        double node_y = myNode.getLocation2D().getY();
        
        NodeEntry item = new NodeEntry(null,myNode.getIP(),myNode.getNCS_Location2D());
        if (myNode.getLocation2D().distanceTo(listCluster[myNode.ClusterId].middle) <= Konstanta.TRESHOLD) {
            System.out.println("masukkan "+myNode.getID()+" ke list CH "+myNode.ClusterId);
            listCluster[myNode.ClusterId].addCHCluster(myNode.getIP(),item);
        }
        listCluster[myNode.ClusterId].addMyCluster(myNode.getIP(), item);
        
        if (node_x > mid_x) listCluster[myNode.ClusterId].horizontal++;
        else if (node_x < mid_x) listCluster[myNode.ClusterId].horizontal--;
        
        if (node_y > mid_y) listCluster[myNode.ClusterId].vertical++;
        else if (node_y < mid_y) listCluster[myNode.ClusterId].vertical--;
    }
  
  private static int generate_zone_id(Location2D loc,int length,int count) {
      double distance = 0;
      int cluster = -1;
      for(int i=0;i<Konstanta.ZONE_COUNT;i++) {
          double temp = loc.distanceTo(listCluster[i].middle);
          if (cluster == -1) {
              cluster = i;
              distance = temp;
          }
          else if (temp < distance) {
              cluster = i;
              distance = temp;
          }
      }
      
      return cluster;
//      double x = loc.getX();
//      double y = loc.getY();
//      
//      return (int) (Math.floor(count*x/length) + count*(Math.floor(count*y/length)));
  }
  
   /**
   * Configures each node representation and network stack
   *
     * @param id    a numerical value to represent the id of a node. Will correspond to the IP address representation
     * @param field the field properties
     * @param placement information regarding positions length of field
     * @param protMap   network stack mapper
     * @param radioInfoShared   configuration of the radio
     * @param plIn        property of the PacketLoss for incoming data packet
     * @param stats
     * @param plOut       property of the PacketLoss for outgoing data packet
     * @param hostPanelContext    the context of the panel this node will be drawn
     * @param fieldContext        the context of the actual field this node is in (for GPS)
     * @param simControl          handle to the simulation manager
     * @param topologyGUI
     * @return 
   */
    public static Node createNode(int id,
                                  Field field,
                                  Placement placement,
                                  Mapper protMap,
                                  RadioInfo.RadioInfoShared radioInfoShared,
                                  PacketLoss plIn,
                                  PacketLoss plOut, 
                                  PanelContext hostPanelContext,
                                  LocationContext fieldContext,
                                  SimManager simControl,
                                  StatsCollector stats,
                                  TopologyGUI topologyGUI)
  {
    /** create entities (gives a physical location) */
    Location nextLocation = placement.getNextLocation(); 
        
    /** Create an individual battery, since no two nodes can be powered by the same battery. The specs of the battery are the same though */
    Battery individualBattery = new IdealBattery(battery.getCapacity_mJ(), battery.getVoltage());;

    /** Set the battery and the energy consumption profile */
    EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
    energyConsumptionModel.setID(id);
    
    /** Create the energy management unit */
    EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);

    /** Create the node and nodeGUI interface for this node */
    Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new ColorProfileZRP(), simControl);
    node.enableRelocation(field); // if you want to be able to relocate, by mouse, the node in the field at run time.
    //RadioNoiseIndep radio = new RadioNoiseIndep(id, radioInfoShared); // uncomment this if you want noisy environments
    
    /** Put a 'GPS' (must to) to obtain the location information (for this assignment, for gaphical purposes only 
     *  Now, really, this is not a GPS per-se, just a 'logical' way of obtaining location information from the simulator
     */
    GPS gps = new GPSimpl(new Location2D((int)nextLocation.getX(), (int)nextLocation.getY()));
    gps.configure(new LocationContext(fieldContext));
    node.setGPS(gps);    
    
     /* *** Configuring the ISO layers - more or less self-explanatory *** */
                /* APP layer configuration */
                AppLayer app = new AppLayer(node, Constants.NET_PROTOCOL_INDEX_1, stats,seq,csv_stats,listCluster);
                
                if (app.topologyGUI == null)
                    app.topologyGUI = topologyGUI;

                /* NET layer configuration - this is where the node gets its "ip" address */
                NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);

                /* ROUTING protocols configuration */
                HeartbeatProtocol heartbeatProtocol 
                	= new HeartbeatProtocol(net.getAddress(), 
                							node,
                							hostPanelContext,
                							30 * Constants.MINUTE);
                
                //if(heartbeatProtocol.topologyGUI == null) heartbeatProtocol.topologyGUI = topologyGUI;
                
                //ShortestGeoPathRouting shortestGeographicalPathRouting = new ShortestGeoPathRouting(node);
                
                RoutingProtocol routingProtocol = new RoutingProtocol(node,stats,seq,listCluster);

                if(routingProtocol.topologyGUI == null) routingProtocol.topologyGUI = topologyGUI;
                
                node.setIP(net.getAddress());

                /* MAC layer configuration */
                Mac802_15_4Impl mac = new Mac802_15_4Impl(new MacAddress(id), radioInfoShared, node.getEnergyManagement(), node);

                /* PHY layer configuration */
                Phy802_15_4Impl phy = new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0 * Constants.SECOND);
                
                /* RADIO "layer configuration */
                field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
                field.startMobility(phy.getRadioInfo().getUnique().getID());
    
                /* *** Hooking up the ISO layers *** */
                /* APP <- Routing hookup */
                //shortestGeographicalPathRouting.setAppInterface(app.getAppProxy());
                routingProtocol.setAppInterface(app.getAppProxy());
                
                /* APP -> NET hookup */
                app.setNetEntity(net.getProxy());
                
                /* NET<->Routing hookup */
                heartbeatProtocol.setNetEntity(net.getProxy());
                //shortestGeographicalPathRouting.setNetEntity(net.getProxy());
                routingProtocol.setNetEntity(net.getProxy());
                //net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, shortestGeographicalPathRouting.getProxy());
                net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, routingProtocol.getProxy());
                net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
                //net.setMacEntity(mac);

                /* net-MAC-phy hookup */
                byte intId = net.addInterface(mac.getProxy());
                mac.setNetEntity(net.getProxy(), intId);
                mac.setPhyEntity(phy.getProxy());

                /* PHY-RADIO hookup */
                phy.setFieldEntity(field.getProxy());
                phy.setMacEntity(mac.getProxy());
    
    /* Here we actually start this node's application layer execution. It is important to observe
       that we don't actually call the app's run() method directly, but through its proxy, which allows JiST engine to actually decide when this call will
       be actually made (based on the simulation time)*/
    app.getAppProxy().run(null);
    
    return node;
  } 
    
  public static Node createSink(int id,
                                Field field,
                                Placement placement,
                                Mapper protMap,
                                RadioInfo.RadioInfoShared radioInfoShared,
                                PacketLoss plIn,
                                PacketLoss plOut, 
                                PanelContext hostPanelContext,
                                LocationContext fieldContext,
                                SimManager simControl,
                                StatsCollector stats,
                                TopologyGUI topologyGUI)
  {
    /** create entities (gives a physical location) */
    Location nextLocation = placement.getNextLocation(); 
        
    /** Create an individual battery, since no two nodes can be powered by the same battery. The specs of the battery are the same though */
    Battery individualBattery = new IdealBattery(battery_sink.getCapacity_mJ(), battery_sink.getVoltage());;

    /** Set the battery and the energy consumption profile */
    EnergyConsumptionModel energyConsumptionModel = new EnergyConsumptionModelImpl(eCostParam, individualBattery);
    energyConsumptionModel.setID(id);
    
    /** Create the energy management unit */
    EnergyManagement energyManagementUnit = new EnergyManagementImpl(energyConsumptionModel, individualBattery);

    /** Create the node and nodeGUI interface for this node */
    Node node = new Node(id, energyManagementUnit, hostPanelContext, fieldContext, new ColorProfileZRP(), simControl);
    node.enableRelocation(field); // if you want to be able to relocate, by mouse, the node in the field at run time.
    //RadioNoiseIndep radio = new RadioNoiseIndep(id, radioInfoShared); // uncomment this if you want noisy environments
    
    /** Put a 'GPS' (must to) to obtain the location information (for this assignment, for gaphical purposes only 
     *  Now, really, this is not a GPS per-se, just a 'logical' way of obtaining location information from the simulator
     */
    GPS gps = new GPSimpl(new Location2D((int)nextLocation.getX(), (int)nextLocation.getY()));
    gps.configure(new LocationContext(fieldContext));
    node.setGPS(gps);    
    
     /* *** Configuring the ISO layers - more or less self-explanatory *** */
                /* APP layer configuration */
                AppLayer app = new AppLayer(node, Constants.NET_PROTOCOL_INDEX_1, stats,seq,csv_stats,listCluster);
                
                if (app.topologyGUI == null)
                    app.topologyGUI = topologyGUI;

                /* NET layer configuration - this is where the node gets its "ip" address */
                NetIp net = new NetIp(new NetAddress(id), protMap, plIn, plOut);

                /* ROUTING protocols configuration */
                HeartbeatProtocol heartbeatProtocol 
                	= new HeartbeatProtocol(net.getAddress(), 
                							node,
                							hostPanelContext,
                							30 * Constants.MINUTE);
                
                //if(heartbeatProtocol.topologyGUI == null) heartbeatProtocol.topologyGUI = topologyGUI;
                
                //ShortestGeoPathRouting shortestGeographicalPathRouting = new ShortestGeoPathRouting(node);
                
                RoutingProtocol routingProtocol = new RoutingProtocol(node,stats,seq,listCluster);

                if(routingProtocol.topologyGUI == null) routingProtocol.topologyGUI = topologyGUI;
                
                node.setIP(net.getAddress());

                /* MAC layer configuration */
                Mac802_15_4Impl mac = new Mac802_15_4Impl(new MacAddress(id), radioInfoShared, node.getEnergyManagement(), node);

                /* PHY layer configuration */
                Phy802_15_4Impl phy = new Phy802_15_4Impl(id, radioInfoShared, energyManagementUnit, node, 0 * Constants.SECOND);
                
                /* RADIO "layer configuration */
                field.addRadio(phy.getRadioInfo(), phy.getProxy(), nextLocation);
                field.startMobility(phy.getRadioInfo().getUnique().getID());
    
                /* *** Hooking up the ISO layers *** */
                /* APP <- Routing hookup */
                //shortestGeographicalPathRouting.setAppInterface(app.getAppProxy());
                routingProtocol.setAppInterface(app.getAppProxy());
                
                /* APP -> NET hookup */
                app.setNetEntity(net.getProxy());
                
                /* NET<->Routing hookup */
                heartbeatProtocol.setNetEntity(net.getProxy());
                //shortestGeographicalPathRouting.setNetEntity(net.getProxy());
                routingProtocol.setNetEntity(net.getProxy());
                //net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, shortestGeographicalPathRouting.getProxy());
                net.setProtocolHandler(Constants.NET_PROTOCOL_INDEX_1, routingProtocol.getProxy());
                net.setProtocolHandler(Constants.NET_PROTOCOL_HEARTBEAT, heartbeatProtocol.getProxy());
                //net.setMacEntity(mac);

                /* net-MAC-phy hookup */
                byte intId = net.addInterface(mac.getProxy());
                mac.setNetEntity(net.getProxy(), intId);
                mac.setPhyEntity(phy.getProxy());

                /* PHY-RADIO hookup */
                phy.setFieldEntity(field.getProxy());
                phy.setMacEntity(mac.getProxy());
    
    /* Here we actually start this node's application layer execution. It is important to observe
       that we don't actually call the app's run() method directly, but through its proxy, which allows JiST engine to actually decide when this call will
       be actually made (based on the simulation time)*/
    app.getAppProxy().run(null);
    
    //warnai sink node dengan warna yang berbeda
    node.getNodeGUI().colorCode.mark(new ColorProfileZRP(),ColorProfileZRP.SINK, ColorProfileZRP.FOREVER);
    
    return node;
  }
}