/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.app;

import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import sidnet.utilityviews.statscollector.StatEntry;
import sidnet.utilityviews.statscollector.StatsCollector;

/**
 *
 * @author fince
 */
public class CSV_Statistics {
    private final LinkedList<StatEntry> stats_data;
    
    private final String filename = Konstanta.CSV_NAME_STATISTICS;
    private final String separator = Konstanta.COMMA_DELIMITER;
    private final String newline = Konstanta.NEW_LINE_SEPARATOR;
    
    private String last_time;
    private FileWriter fileWriter = null;

    public CSV_Statistics(StatsCollector stats) {
        this.last_time = "0";
        this.stats_data = stats.getStatistic();
        
        try {
            fileWriter = new FileWriter(this.filename);
            fileWriter.append(convert(last_time));
        } catch (IOException e) {
            System.out.println("Error in CsvFileWriter !!!");
        }
    }
    
    public void write(long hour,long minute) throws IOException {
        if (this.last_time.equals(hour+"."+minute)) return;
        else {
            this.last_time = hour+"."+minute;
            fileWriter.append(convert(last_time));
        }
    }
    
    private String convert(String time) {
        if (stats_data == null) {
            return "";
        }
        else {
            int size = stats_data.size();
            
            String output = time + this.separator;
            String header = "Time" + this.separator;
            for (int i=0;i<size;i++) {
                if (i > 0) {
                    header = header + this.separator;
                    output = output + this.separator;
                }
                header = header + stats_data.get(i).getKey();
                output = output + stats_data.get(i).getValueAsString();
            }
            if (time.equals("0")) return header + this.newline;
            else return output + this.newline;
        }
    }
}
