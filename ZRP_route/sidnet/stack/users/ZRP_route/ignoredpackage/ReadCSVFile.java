/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sidnet.stack.users.ZRP_route.ignoredpackage;

import java.util.HashMap;
import sidnet.stack.users.ZRP_route.app.Konstanta;

/**
 *
 * @author dharmawan
 */
public class ReadCSVFile {
    private HashMap<Integer,HashMap> output_file;
    
    public ReadCSVFile() {
        output_file = new HashMap<Integer,HashMap>();
        
        for (int i = 0;i<Konstanta.ZONE_COUNT;i++) {
            output_file.put(i, LoadDataSkenario(i));
        }
    }
    
    public double getData(int i,int time) {
        if (output_file.containsKey(i)) {
            HashMap<Integer,Integer> file = output_file.get(i);
            if (file.containsKey(time)) {
                Integer data = file.get(time);
                switch(data) {
                    case 1:
                        return Math.random()*(Konstanta.MAX_PRI_1-Konstanta.MIN_PRI_1-1)+Math.min(Konstanta.MAX_PRI_1,Konstanta.MIN_PRI_1);
                    case 2:
                        return Math.random()*(Konstanta.MAX_PRI_2-Konstanta.MIN_PRI_2-1)+Math.min(Konstanta.MAX_PRI_2,Konstanta.MIN_PRI_2);
                    default:
                        return -1;
                }
            }
        }
        return -1;
    }
    
    public static HashMap<Integer,Integer> LoadDataSkenario(int i) {
        String filename = Konstanta.FILE_NAME + i + Konstanta.FILE_EXT;

        java.io.BufferedReader br = null;
        String lines = "";
        HashMap<Integer,Integer> data = new HashMap<Integer,Integer>();

        try {
            br = new java.io.BufferedReader(new java.io.FileReader(filename));
            while ((lines = br.readLine()) != null) {
                String[] line = lines.split(Konstanta.FILE_SPLIT);
                //System.out.println(""+Integer.parseInt(line[0])+" "+Integer.parseInt(line[1])+"=>");
                data.put(Integer.parseInt(line[0]), Integer.parseInt(line[1]));
            }
            //System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
		if (br != null) {
			try {
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
        return data;
    }
}
