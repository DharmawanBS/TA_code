/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.app;

import jist.swans.Constants;

/**
 *
 * @author dharmawan
 */
public class Konstanta {
    public static int ZONE_COUNT = 16;
    
    public static int SINK = 49;
    
    public static int BANDWIDTH_SRC = 4000;
    public static int BANDWIDTH_SINK = 400000;
    
    public static int BATTERY_SINK = 9999999;
    
    public static final boolean IS_PAUSE = false;
    public static final boolean USE_AGG = false;
    public static final boolean USE_POOL = false;
    public static final boolean USE_PRIORITY = false;
    public static final boolean DIRECT_SEND = false;

    public static long INTERVAL_TIMING_SEND = 5 * Constants.SECOND;
    public static final long TIMING_DELAY_SEND = 300 * Constants.MILLI_SECOND;
   
    public static final int MAXIMUM_RETRY_SEND_MESSAGE = 3;
    public static final long INTERVAL_WAITING_BEFORE_RETRY = 10 * Constants.SECOND;
    
    public static final int LIMIT_PACKET_ID_SIZE = 500;
    
    public static final String FILE_NAME = "data/data";
    public static final String FILE_EXT = ".csv";
    public static final String FILE_SPLIT = ",";
    
    public static final int START_LIMIT_1 = 30;
    public static final int MIN_LIMIT_1 = 10;
    
    public static final int START_LIMIT_2 = 60;
    public static final int MIN_LIMIT_2 = 30;

    public static final int MIN_PRI_1 = 39;
    public static final int MAX_PRI_1 = 45;
    
    public static final int MIN_PRI_2 = 18;
    public static final int MAX_PRI_2 = 38;
    
    public static final long TIMING_DELAY_SEND_PRI = 300 * Constants.MILLI_SECOND;
}