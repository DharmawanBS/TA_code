/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.ignoredpackage;

/**
 *
 * @author dharmawan
 */
public class PoolReceivedItem {
    public double maxdataValue;
    public double mindataValue;
    public double avgdataValue;
    public long countdataValue;
    public int queryID;
    public int priority;

    public PoolReceivedItem(double max,double min,double avg,long count,int queryID,int priority) {
        this.queryID = queryID;
        this.maxdataValue = max;
        this.mindataValue = min;
        this.avgdataValue = avg;
        this.countdataValue = count;
        this.priority = priority;
    }

    public void putAVG(double data,long count) {
        avgdataValue = (avgdataValue*countdataValue + data*count)/(countdataValue+count);
        countdataValue+=count;
    }

    public void putMAX(double data) {
        maxdataValue = (maxdataValue < data) ? data : maxdataValue;
    }

    public void putMIN(double data) {
        mindataValue = (mindataValue > data) ? data : mindataValue;
    }
}