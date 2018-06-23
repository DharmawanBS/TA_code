/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.driver;

import sidnet.core.misc.Node;
import sidnet.utilityviews.statscollector.NodeBasedStatEntry;
import sidnet.utilityviews.statscollector.StatEntry;
import sidnet.utilityviews.statscollector.StatEntry_GeneralPurposeContor;

/**
 *
 * @author dharmawan
 */
public class StatEntry_SumPurposeContor extends StatEntry implements NodeBasedStatEntry {
    protected double sum = 0;
    private static final String TAG = "Contor";

    private StatEntry_GeneralPurposeContor[] count;

    public StatEntry_SumPurposeContor(String key, StatEntry_GeneralPurposeContor[] count) {
        super(key, TAG);
        this.count = count;
    }

    @Override
    public String getValueAsString() {
        return "" + this.sum;
    }

    public void update(Node[] nodes) {
        int sum = 0;
        if (count.length > 0) {
            for(int i=0;i<count.length;i++) {
                sum+=count[i].getValue();
            }
        }
        this.sum = sum;
    }
}