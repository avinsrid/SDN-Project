/*
 SDN-Project [Rec-A and Region Optimization for controller tree mechanism]
 By: Avinash Sridhar, Nachiket Rau, Shruti Ramesh, Sareena Abdul Razak
 This program servers the following in SDN Project:
 1) It opens 4 sockets in the range 411xx and 412xx for Rec-A and Region Optimization applications of child controllers c1, and c2
 2) Above is implpemented using four different threads
 int port1 = 41101; (REC-A Child-Controller 1)
 int port2 = 41102; (REG-OPT Child-Controller 1)
 int port3 = 41201; (REC-A Child-Controller 2)
 int port4 = 41202; (REG-OPT Child-Controller 2)
 3) We also have a a switch failover mechanism that happens every 10 seconds. In case the switch-controller connection is detected to be broken with
 , we will automatically point the switch to the slave controller.
 NOTE: This program is run on the machine where Mininet is running
 */

import java.net.*;
import java.io.*;
import java.lang.Runtime;
import java.lang.Process;

public class IncomingCmd extends Thread {
    private ServerSocket serverSocket;
    private long currentTime;
    
    public IncomingCmd(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setSoTimeout(0);
    }
    
    public IncomingCmd(long timeValue) throws IOException {
        currentTime = timeValue;
        System.out.println("IncomingCmd:: Timer thread that checks switch master-slave failover implemented");
    }
    
    public void run() {
        while (true) {
            
            // We need to ensure that only thread values 0-3 are focussed here else we will get null pointer exception as Thread-4 does not have a
            // listening socket
            if (!Thread.currentThread().getName().equals("Thread-4")) {
                System.out.println("IncomingCmd::run(): Thread Started " + Thread.currentThread().getName());
                try {
                    System.out.println("IncomingCmd::run(): Listening on socket " + serverSocket.getLocalPort());
                    Socket server = serverSocket.accept();
                    DataInputStream in = new DataInputStream(server.getInputStream());
                    String checkInput = in.readUTF();
                    System.out.println(checkInput);
                    // Based on the string we receive from other end, the Rec-A application will execute one of the below commands on the switch gs1 or gs2
                    if (checkInput.equals("Delete-gs1")) {
                        try {
                            Process p = Runtime.getRuntime().exec("sudo ovs-vsctl del-port gs1 gs1-eth1");
                        }
                        catch (IOException e) { System.out.println("IncomingCmd::run(): Error deleting G-Switch 1 port gs1-eth1"); }
                    }
                    else if (checkInput.equals("Delete-gs2")) {
                        try {
                            Process p = Runtime.getRuntime().exec("sudo ovs-vsctl del-port gs2 gs2-eth1");
                        }
                        catch (IOException e) {System.out.println("IncomingCmd::run(): Error deleting G-Switch 2 port gs2-eth1"); }
                    }
                    else if (checkInput.equals("Pull-gs1")) {
                        try {
                            Process p = Runtime.getRuntime().exec("sudo ovs-vsctl add-port gs1 gs1-eth1");
                        }
                        catch (IOException e) {System.out.println("IncomingCmd::run(): Error adding G-Switch 1 port gs1-eth1"); }
                    }
                    else if (checkInput.equals("Pull-gs2")) {
                        try {
                            Process p = Runtime.getRuntime().exec("sudo ovs-vsctl add-port gs2 gs2-eth1");
                        }
                        catch(IOException e) {System.out.println("IncomingCmd::run(): Error adding G-Switch 2 port gs2-eth1"); }
                    }
                    else if (checkInput.startsWith("Switch-Controller"))
                    {
                        String switchNumber = checkInput.substring(18, 19);
                        switchController(switchNumber);
                    }
                }
                // We should not run into socket timeout exceptions as we use infinite time out. However, catching exception as a precaution
                catch (SocketTimeoutException s) {System.out.println("IncomingCmd::run(): Socket Timed Out"); }
                catch (IOException e) {
                    System.out.println("IncomingCmd::run() Error receiving data from socket");
                    e.printStackTrace();
                }
            }
            
            // Only for the failover 5th thread that handles failover of switch with master-slave controllers
            else if (Thread.currentThread().getName().equals("Thread-4") && (System.currentTimeMillis() - currentTime) > 10000) {
                currentTime = System.currentTimeMillis();
                String[] switchList = {"s1", "s2", "s3", "s4"};
                for ( String switchID : switchList) {
                    checkFailover(switchID);
                }
            }
        }
    }
    
    // Function to switch the switch's controller role
    public void switchController(String switchID) {
        
        // If the child controller 1 is initiating switch to fail over to new controller,
        if (serverSocket.getLocalPort() == 41102) {
            try {
                Process p = Runtime.getRuntime().exec("sudo ovs-vsctl del-controller " + switchID);
                
                // Note that the below controller IP may change if needed based on testing
                p = Runtime.getRuntime().exec("sudo ovs-vsctl set-controller " + switchID + " tcp:192.168.56.101:6633");
            }
            catch(IOException e) {System.out.println("IncomingCmd::switchController(): Error performing controller commands for switch " + switchID); }
        }
        else if (serverSocket.getLocalPort() == 41202) {
            try {
                Process p = Runtime.getRuntime().exec("sudo ovs-vsctl del-controller " + switchID);
                
                // Note that the below controller IP may change if needed based on testing
                p = Runtime.getRuntime().exec("sudo ovs-vsctl set-controller " + switchID + " tcp:192.168.56.102:6633");
            }
            catch(IOException e) {System.out.println("IncomingCmd::switchController(): Error performing controller commands for switch " + switchID); }
        }
    }
    
    // Function to check if failover of switch is required. If so, do the needful from master to slave and vice versa
    public void checkFailover(String switchID) {
        
        // Obtain the current switch's current controller
        try {
            Process p = Runtime.getRuntime().exec("sudo ovs-vsctl get-controller " + switchID);
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String controller = input.readLine();
            System.out.println("IncomingCmd::checkFailover(): " + switchID + " connected to controller " + controller);
            
            // Based on the switch's current controller, check if it's connectivity is true/false. If false, do the failover to slave controller.
            // Below check command is obtained from mininet Node.py code --> https://github.com/mininet/mininet/blob/master/mininet/node.py under
            // def connected().
            // There is another issue here where we have to take care of a null pointer exception in case the above command does not return a current controller
            // This seems to happen probably because the ovs-vsctl set-controller command for a specific switch is not being taken into effect.
            // Maybe the commands are executing too fast, leading to some sort of a race condition. Hence we need to ensure we give some gap of 1 second,
            // to prevent the race condition. This will be done by making the thread sleep for a gap of 1 second as safety. We also check for controller string to
            // not be null in order to ensure we don't run into Null Pointer Exception. This way entire program won't crash.
            if (controller != null) {
                if (controller.equals("tcp:192.168.56.21:6633")) {
                    p = Runtime.getRuntime().exec("sudo ovs-vsctl -- get Controller " + switchID + " is_connected");
                    BufferedReader newInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String newInputString = newInput.readLine();
                    System.out.println("IncomingCmd::checkFailover(): Is " + switchID + " connected to current controller? " + newInputString);
                    if (newInputString.equals("false")) {
                        p = Runtime.getRuntime().exec("sudo ovs-vsctl del-controller " + switchID);
                        Thread.sleep(1000);
                        p = Runtime.getRuntime().exec("sudo ovs-vsctl set-controller " + switchID + " tcp:192.168.56.1:6633");
                    }
                }
                else if (controller.equals("tcp:192.168.56.1:6633")) {
                    p = Runtime.getRuntime().exec("sudo ovs-vsctl -- get Controller " + switchID + " is_connected");
                    BufferedReader newInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String newInputString = newInput.readLine();
                    System.out.println("IncomingCmd::checkFailover(): Is " + switchID + " connected to current controller? " + newInputString);
                    if (newInputString.equals("false")) {			
                        p = Runtime.getRuntime().exec("sudo ovs-vsctl del-controller " + switchID);
                        Thread.sleep(1000);
                        p = Runtime.getRuntime().exec("sudo ovs-vsctl set-controller " + switchID + " tcp:192.168.56.21:6633");
                    }
                }
            }
        }					
        
        catch(IOException e) {System.out.println("IncomingCmd::checkFailover(): Error executing commands for switch " + switchID); }
        catch(InterruptedException ie) {System.out.println("IncomingCmd::checkFailover() Error with thread sleep "); }
    }
    
    public static void main(String[] args) {
        System.out.println("IncomingCmd::main() Started");
        
        // 411xx is the listening socket for Rec-A and Region Optimization modules of child controller c1. 412xx are the listening sockets for same
        // applications for child controller c2.
        int port1 = 41101;
        int port2 = 41102;
        int port3 = 41201;
        int port4 = 41202;
        long timer = System.currentTimeMillis();
        try {
            Thread t1 = new IncomingCmd(port1);
            Thread t2 = new IncomingCmd(port2);
            Thread t3 = new IncomingCmd(port3);
            Thread t4 = new IncomingCmd(port4);
            Thread t5 = new IncomingCmd(timer);
            t1.start();
            t2.start();
            t3.start();
            t4.start();
            t5.start();
            
        }
        catch (IOException e) {
            System.out.println("IncomingCmd::main() Error starting thread");
            e.printStackTrace();
        }
    }
}
