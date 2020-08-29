package SilkCentralMaven.SilkCentralMaven;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SilkCentralMaven {
	

    public static void SCRunExecution(int SC_ExecutionNodeId, String SC_Host, String SC_Token) {
        try {
        	URL url = new URL( SC_Host + "/Services1.0/execution/executionplanruns?nodeId=" + SC_ExecutionNodeId);//your url i.e fetch data from .
        	
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json;charset=UTF-8");
            conn.setRequestProperty("SC-SESSION-ID", SC_Token);
            conn.setRequestProperty("Content-Type", "application/json");
            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP Error code : "
                        + conn.getResponseCode());
            }
            
            InputStreamReader in = new InputStreamReader(conn.getInputStream());
            BufferedReader br = new BufferedReader(in);
            String output;
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }
            conn.disconnect();

        } catch (Exception e) {
            System.out.println("Exception in NetClientGet:- " + e);
        }
    }
}