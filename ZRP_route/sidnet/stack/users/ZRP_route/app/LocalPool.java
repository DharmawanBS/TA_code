/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.ZRP_route.app;

import java.util.ArrayList;
import java.util.List;
import sidnet.stack.users.ZRP_route.ignoredpackage.PoolReceivedItem;

/**
 *
 * @author dharmawan
 */
public class LocalPool {
    private List<Double> data_1;
    public List<Double> data_2;
    private PoolReceivedItem pool_1;
    private PoolReceivedItem pool_2;

    private int sizeLimit_1;
    private int sizeLimit_2;

    private void generate_size() {
        if (Konstanta.USE_LOCAL_POOL) {
            this.sizeLimit_1 = Konstanta.START_LIMIT_1;
            this.sizeLimit_2 = Konstanta.START_LIMIT_2;
        }
        else {
            this.sizeLimit_1 = Konstanta.START_LIMIT_NON;
            this.sizeLimit_2 = Konstanta.START_LIMIT_NON;
        }
    }
    
    public LocalPool() {
        this.data_1 = new ArrayList<Double>();
        this.pool_1 = null;
        
        this.data_2 = new ArrayList<Double>();
        this.pool_2 = null;
        
        generate_size();
    }
    
    private int getPriority(double Value) {
        if (Value >= Konstanta.MIN_PRI_1 && Value <= Konstanta.MAX_PRI_1) {
            return 1;
        }
        else if (Value >= Konstanta.MIN_PRI_2 && Value <= Konstanta.MAX_PRI_2) {
            return 2;
        }
        else {
            return -1;
        }
    }

    public void putValue(double Value,int queryID) {
        int priority = getPriority(Value);
        
        switch (priority) {
            case 1:
                this.data_1.add(Value);
                if (this.pool_1 == null) pool_1 = new PoolReceivedItem(Value,Value,Value,1,queryID,priority);
                else {
                    pool_1.putAVG(Value, 1);
                    pool_1.putMAX(Value);
                    pool_1.putMIN(Value);
                }
                break;
            case 2:
                this.data_2.add(Value);
                if (this.pool_2 == null) pool_2 = new PoolReceivedItem(Value,Value,Value,1,queryID,priority);
                else {
                    pool_2.putAVG(Value, 1);
                    pool_2.putMAX(Value);
                    pool_2.putMIN(Value);
                }
                break;
            default:
                break;
        }
    }
    
    public boolean isPoolFull(int priority) {
        switch (priority) {
            case 1:
                return (data_1.size() == sizeLimit_1);
            case 2:
                return (data_2.size() == sizeLimit_2);
            default:
                return false;
        }
    }
    
    public boolean isEmpty(int priority) {
        switch (priority) {
            case 1:
                return (data_1.isEmpty());
            case 2:
                return (data_2.isEmpty());
            default:
                return false;
        }
    }

    public PoolReceivedItem getData(int priority) {
        PoolReceivedItem out;
        switch (priority) {
            case 1:
                out = new PoolReceivedItem(pool_1.maxdataValue,pool_1.mindataValue,pool_1.avgdataValue,pool_1.countdataValue,pool_1.queryID,pool_1.priority);
                data_1.clear();
                pool_1 = null;
                break;
            case 2:
                out = new PoolReceivedItem(pool_2.maxdataValue,pool_2.mindataValue,pool_2.avgdataValue,pool_2.countdataValue,pool_2.queryID,pool_2.priority);
                data_2.clear();
                pool_2 = null;
                break;
            default:
                out = null;
                break;
        }
        return out;
    }

    public void reducePoolSize(int priority) {
        if ( ! Konstanta.DIRECT) {
            switch (priority) {
                case 1:
                    if (this.sizeLimit_1 > Konstanta.MIN_LIMIT_1) this.sizeLimit_1--;
                    break;
                case 2:
                    if (this.sizeLimit_2 > Konstanta.MIN_LIMIT_2) this.sizeLimit_2--;
                    break;
                default:
                    break;
            }
        }
    }

    public void increasePoolSize(int priority) {
        if ( ! Konstanta.DIRECT) {
            switch (priority) {
                case 1:
                    this.sizeLimit_1++;
                    break;
                case 2:
                    this.sizeLimit_2++;
                    break;
            }
        }
    }
}