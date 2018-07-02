/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.ignoredpackage;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import sidnet.stack.users.ZRP_route.app.Konstanta;

/**
 *
 * @author fince
 */
public class CSV_NodeDie {
    private final String filename = Konstanta.CSV_NAME_DEATHNODE;
    
    private FileWriter fileWriter = null;
    
    private ArrayList<Integer> deathNode = new ArrayList<Integer>();

    public CSV_NodeDie() {
        try {
            fileWriter = new FileWriter(this.filename);
            String tulis = header();
            fileWriter.append(tulis);
        } catch (IOException e) {
            System.out.println("Error in CsvFileWriter !!!");
        }
    }
    
    public void write(int id,int zone,long time) throws IOException {
        if (! deathNode.contains(id)) {
            deathNode.add(id);
            fileWriter.append(convert(id,zone,time));
        }
    }
    
    public String header() {
        return "Node"+Konstanta.COMMA_DELIMITER+"Zone"+Konstanta.COMMA_DELIMITER+"Time"+Konstanta.NEW_LINE_SEPARATOR;
    }
    
    public String convert(int id,int zone,long time) {
        return  id  +Konstanta.COMMA_DELIMITER+
                zone+Konstanta.COMMA_DELIMITER+
                time+Konstanta.NEW_LINE_SEPARATOR;
    }
}
