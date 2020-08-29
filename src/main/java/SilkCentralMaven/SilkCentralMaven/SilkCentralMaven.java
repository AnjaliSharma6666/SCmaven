package SilkCentralMaven.SilkCentralMaven;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SilkCentralMaven {
	

	
	public static void main(String[] args) {
		
		SilkCentralMaven.SCRunExecution(45, "http://desktop-5ivatg8:19120", "327981e9-d9f0-4e00-9a78-1bcb86078ec2");

			}



	public static void SCRunExecution(int SC_ExecutionNodeId, String SC_Host, String SC_Token) {
        try {
        	URL url = new URL( SC_Host + "/Services1.0/execution/executionplanruns?nodeId=" + SC_ExecutionNodeId);//your url i.e fetch data from .
        	
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json;charset=UTF-8");
            conn.setRequestProperty("SC-SESSION-ID", SC_Token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP Error code : "
                        + conn.getResponseCode());
            }
            
            else {
            	BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8")); 
            		    StringBuilder response = new StringBuilder();
            		    String responseLine = null;
            		    while ((responseLine = br.readLine()) != null) {
            		        response.append(responseLine.trim());
            		    }
            		    System.out.println(response.toString());
            		
            conn.disconnect();
        }

        } catch (Exception e) {
            System.out.println("Exception:- " + e);
        }
    }
}