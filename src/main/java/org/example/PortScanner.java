package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import java.util.TreeMap;

public class PortScanner {
    public static void main(String[] args) {

        String targetHost = "localhost"; // Change this to the target host you want to scan
        int minPort = 1;
        int maxPort = 65535;

        // Create a TreeMap to store port numbers and descriptions
        TreeMap<Integer, String> portData = new TreeMap<>();

        // Read the CSV file outside the loop
        String pathToCsv = "service-names-port-numbers.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(pathToCsv))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Skip lines that don't contain the expected data format
                if (!line.contains(",")) {
                    continue;
                }

                // Use comma as separator
                String[] columns = line.split(",");

                // Check if the line has the expected number of columns
                if (columns.length >= 4) {
                    try {
                        // Extract the port number (column 1) and description (column 3)
                        int portNumber = Integer.parseInt(columns[1].trim());
                        String description = columns[3].trim();

                        // Store the data in the TreeMap if the description is not empty
                        if (!description.isEmpty()) {
                            portData.put(portNumber, description);
                        }
                    } catch (NumberFormatException e) {
                        // Handle parsing errors (no print here)
                    }
                } else {
                    // Handle lines that don't have the expected number of columns (no print here)
                }
            }
        } catch (IOException e) {


            e.printStackTrace();
        }

        // Now, scan the ports and write to Redis
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            for (int port = minPort; port <= maxPort; port++) {
                try {
                    Socket socket = new Socket(targetHost, port);

                    // Check if the port is in the TreeMap and has a non-empty description
                    if (portData.containsKey(port)) {
                        String description = portData.get(port);

                        // Print and create (Set a key-value pair) in Redis only if the description is not empty
                        if (!description.isEmpty()) {
                            System.out.println("Port " + port + ": " + description);
                            jedis.set(String.valueOf(port), description);

                            // Read (Get the value of a key) from Redis
                            String value = jedis.get(String.valueOf(port));
                           // System.out.println(value);
                        }
                    }
                    socket.close();
                } catch (IOException e) {
                    // Port is likely closed or unreachable
                }
            }
        } catch (JedisConnectionException e) {
            System.out.println("Could not connect to Redis: " + e.getMessage());
        }
    }
}