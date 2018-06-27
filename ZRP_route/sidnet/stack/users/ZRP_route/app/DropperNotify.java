/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sidnet.stack.users.ZRP_route.app;

import jist.swans.misc.Message;

/**
 *
 * @author invictus
 */
public class DropperNotify implements Message {

    public boolean reduceWindow;
    public boolean increaseWindow;
    public int priority;
    
    public DropperNotify (boolean reduceWindow, boolean increaseWindow, int priority) {
        this.increaseWindow = increaseWindow;
        this.reduceWindow = reduceWindow;
        this.priority = priority;
    }

    public int getSize() {
        return 2;
    }

    public void getBytes(byte[] msg, int offset) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
