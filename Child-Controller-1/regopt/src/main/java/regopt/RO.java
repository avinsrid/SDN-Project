package regopt;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.lang.String;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.lang.Runtime;
import java.lang.Process;
import java.io.IOException;


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.lang.String;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Runtime;
import java.lang.Process;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.lang.Object;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Host;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Path;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.Edge;
import org.opendaylight.controller.sal.core.Host.*;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.topology.TopoEdgeUpdate;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.controller.switchmanager.Subnet;
import org.opendaylight.controller.sal.routing.IRouting;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.opendaylight.controller.topologymanager.TopologyUserLinkConfig;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;


public class RO implements IRouting, ITopologyManager {
    private static final Logger logger = LoggerFactory
    .getLogger(RO.class);
    private ISwitchManager switchManager = null;
    private ITopologyManager topologyManager = null;
    private List<Host> hostList;
    private Set<NodeConnector> setOfNodeConnWithHosts = new HashSet<NodeConnector>();
    private Set<NodeConnector> setOfNodeconnectors = new HashSet<NodeConnector>();
    private Map<Node, Set<Edge>> edgesForEachNode = new HashMap<Node, Set<Edge>>();
    private Map<Node, Set<NodeConnector>> nodeswithNodeConnectorHosts = new HashMap<Node, Set<NodeConnector>>();
    private Map<Node,Integer> currentTopology = new HashMap<Node,Integer>();
    private Map<Node,Integer> newTopology = new HashMap<Node,Integer>();
    private Set<Node> setOfNodeswithHosts = new HashSet<Node>();
    private String function = "switch";
    
    long Timer = 0L;
    int old = 0;
    int neww = 0;
    
    void setSwitchManager(ISwitchManager s) {
        logger.debug("SwitchManager set");
        this.switchManager = s;
    }
    
    void unsetSwitchManager(ISwitchManager s) {
        if (this.switchManager == s) {
            logger.debug("SwitchManager removed!");
            this.switchManager = null;
        }
    }
    
    void setTopologyManager(ITopologyManager s) {
        logger.debug("TopologyManager set");
        this.topologyManager = s;
    }
    
    void unsetTopologyManager(ITopologyManager s) {
        if (this.topologyManager == s) {
            logger.debug("TopologyManager removed!");
            this.topologyManager = null;
        }
    }
    
    void init() {
        logger.info("Initialized");
        // Disabling the SimpleForwarding and ARPHandler bundle to not conflict with this one
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        for(Bundle bundle : bundleContext.getBundles()) {
            if (bundle.getSymbolicName().contains("simpleforwarding")) {
                try {
                    bundle.uninstall();
                } catch (BundleException e) {
                    logger.error("Exception in Bundle uninstall "+bundle.getSymbolicName(), e);
                }
            }
        }
        
    }
    
    
    void start() {
        logger.info("Started");
        System.out.println("Region-Optimization started!");
        roImplementation();
    }
    
    public void roImplementation () {
    	Timer = System.currentTimeMillis();
        Integer g;
        // TOPOLOGY FOR CHILD CONTROLLER C2
        
        old = Topologychild();
        System.out.println("\n\n\n Old Size returned = " + old);
        
    	while (true) {
    		if (System.currentTimeMillis() - Timer > 10000) {
    			Timer = System.currentTimeMillis();
                
                // Topology for Child Controller C1
                
                nodeswithNodeConnectorHosts = topologyManager.getNodesWithNodeConnectorHost();
                System.out.println("\n\nRO::roImplementation: Obtained Node Connectors with hosts for each node!" + nodeswithNodeConnectorHosts);
                
                setOfNodeconnectors = topologyManager.getNodeConnectorWithHost();
                System.out.println("\n\n RO::roImplementation: Set of Node Connectors " + setOfNodeconnectors);
                
                
                // Map of Hosts per Node
                
                if(!nodeswithNodeConnectorHosts.isEmpty())
                {
                    System.out.println("\n\n nodeswithNodeConnectorHosts is a Not Empty Set");
                    Set<Node> setOfNodeswithHosts = nodeswithNodeConnectorHosts.keySet();
                    System.out.println("\n\n Obtained Set of Nodes with Host " + setOfNodeswithHosts);
                    Node[] arrayOfNodeswithHosts = setOfNodeswithHosts.toArray(new Node[setOfNodeswithHosts.size()]);
                    for(Node N: arrayOfNodeswithHosts){
                        System.out.println ("\n\n Node is " + N);
                        Set<NodeConnector> setofNodeConn = nodeswithNodeConnectorHosts.get(N);
                        newTopology.put(N,setofNodeConn.size());
                    }
                    System.out.println("\n\n New Topology Map of Nodes and Number of Hosts is " + newTopology);
                    Switchfailovercheck();
                    System.out.println("\n\n Exceuted SwitchFailoverCheck Function");
                }
                
            }
            
            
        }
        
    }
     
     public Integer Topologychild()
     {
         Integer a = 0;
          JSONObject HostConfig = gethostTracker("admin","admin","http://192.168.56.20:8080/controller/nb/v2/hosttracker/default/hosts/active");
          System.out.println("\n\n Obtained JSON HostConfig from getHostTracker()" + HostConfig);
        
         try {
         JSONArray hostArray = HostConfig.getJSONArray("hostConfig");
         System.out.println("\n\n\n size = " + hostArray.length());
             a = hostArray.length();
         }
         catch (JSONException e) { logger.error("Exception in JSON creation" + e); }
         return a;
     }
    
     public static JSONObject gethostTracker(String user, String password, String baseURL) {
     
     StringBuffer result = new StringBuffer();
     try {
     
     // Create URL = base URL
     URL url = new URL(baseURL);
     
     // Create authentication string and encode it to Base64
     String authStr = user + ":" + password;
     String encodedAuthStr = Base64.encodeBase64String(authStr
     .getBytes());
     
     // Create Http connection
     HttpURLConnection connection = (HttpURLConnection) url
     .openConnection();
     
     // Set connection properties
     connection.setRequestMethod("GET");
     connection.setRequestProperty("Authorization", "Basic " + encodedAuthStr);
     connection.setRequestProperty("Accept", "application/json");
     
     // Get the response from connection's inputStream
     InputStream content = (InputStream) connection.getInputStream();
     BufferedReader in = new BufferedReader(new InputStreamReader(content));
     String line = "";
     while ((line = in.readLine()) != null) {
     result.append(line);
     }
     
     JSONObject hostconfigg = new JSONObject(result.toString());
     System.out.println("PortDiscovery::gethostTracker(): Returned HostConfig to getPort()");
     return hostconfigg;
     }
     catch (Exception e) {
     e.printStackTrace();
     }
     
     return null;
     }
     
    
     
    public void Switchfailovercheck()
    {
        System.out.println("\n\n Current Topology Map of Nodes and Number of Hosts is " + currentTopology);
        
        if(!currentTopology.isEmpty())
        {
            Set<Node> setofNodesinCT = currentTopology.keySet();
            Set<Node> setofNodesinNT = newTopology.keySet();
            System.out.println("\n\n Obtained Set of Nodes in Current Topology" + setofNodesinCT);
            System.out.println("\n\n Obtained Set of Nodes in New Topology" + setofNodesinNT);
            Node[] arrayofNodesinCT = setofNodesinCT.toArray(new Node[setofNodesinCT.size()]);
            Node[] arrayofNodesinNT = setofNodesinNT.toArray(new Node[setofNodesinNT.size()]);
            Integer a,b,c,d;
            Integer fval;
            boolean p,q;
            a=arrayofNodesinCT.length;
            b=arrayofNodesinNT.length;
            System.out.println("\n\n New Topology Map of Nodes and Number of Hosts is " + newTopology);
            System.out.println("\n\n Current Topology Map of Nodes and Number of Hosts is " + currentTopology);
            if(a==b){
                for(Node N : arrayofNodesinCT){
                    p=currentTopology.containsKey(N);
                    q=newTopology.containsKey(N);
                    if(p==true && q==true){
                        c=currentTopology.get(N);
                        d=newTopology.get(N);
                        if(c==d){
                            System.out.println("\n\n No CHANGE IN TOPOLOGY");}
                        else
                         {
                            System.out.println("\n\n CHANGE IN TOPOLOGY");
                            neww = Topologychild();
                             System.out.println("Value of Old Size " + old + " Value of New Size " + neww);
                             if(old != neww)
                              {
                                 fval = neww - old;
                                 System.out.println("\n\n Number of Hosts Added " + fval);
                                  old = neww;
                                  System.out.println("Value of Old Size " + old + " Value of New Size " + neww);
                              }
                       }
                    }
                    else{
                        System.out.println("\n\n SAME NODES NOT PRESENT IN CURRENT AND NEW TOPOLOGY");
                        neww = Topologychild();
                        System.out.println("Value of Old Size " + old + " Value of New Size " + neww);
                        if(old != neww)
                        {
                            fval = neww - old;
                            System.out.println("\n\n Number of Hosts Added " + fval);
                            old = neww;
                            System.out.println("Value of Old Size " + old + " Value of New Size " + neww);
                        }
                    }
                }
            }
            else{
                System.out.println("\n\n SAME NUMBER OF NODES NOT PRESENT IN CURRENT AND NEW TOPOLOGY");
            }
        }
        // #7 Fix, please try, this should hopefully fix it
        for (Node everyNode : newTopology.keySet())
        {
        	int temp = newTopology.get(everyNode);
        	if (currentTopology.containsKey(everyNode)) {
        		currentTopology.remove(everyNode);
        		currentTopology.put(everyNode, temp);
        	}
                else {
                    currentTopology.put(everyNode, temp);
                }
                
                System.out.println("\n\n REACHED END OF SWITCHFAILOVER FUNCTION");
                }
                }
                
                void stop() {
                logger.info("Stopped");
            }
                
                public Status addUserLink(TopologyUserLinkConfig arg0) {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Status deleteUserLink(String arg0) {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Map<Edge, Set<Property>> getEdges() {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Host getHostAttachedToNodeConnector(NodeConnector arg0) {
                // TODO Auto-generated method stub
                return null;
            }
                
                public List<Host> getHostsAttachedToNodeConnector(NodeConnector arg0) {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Set<NodeConnector> getNodeConnectorWithHost() {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Map<Node, Set<Edge>> getNodeEdges() {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Map<Node, Set<NodeConnector>> getNodesWithNodeConnectorHost() {
                // TODO Auto-generated method stub
                return null;
            }
                
                public ConcurrentMap<String, TopologyUserLinkConfig> getUserLinks() {
                // TODO Auto-generated method stub
                return null;
            }
                
                public boolean isInternal(NodeConnector arg0) {
                // TODO Auto-generated method stub
                return false;
            }
                
                public Status saveConfig() {
                // TODO Auto-generated method stub
                return null;
            }
                
                public void updateHostLink1(NodeConnector arg0, Host arg1, UpdateType arg2,
                                            Set<Property> arg3) {
                // TODO Auto-generated method stub
                
            }
                
                public void clear() {
                // TODO Auto-generated method stub
                
            }
                
                public void clearMaxThroughput() {
                // TODO Auto-generated method stub
                
            }
                
                public Path getMaxThroughputRoute(Node arg0, Node arg1) {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Path getRoute(Node arg0, Node arg1) {
                // TODO Auto-generated method stub
                return null;
            }
                
                public Path getRoute(Node arg0, Node arg1, Short arg2) {
                // TODO Auto-generated method stub
                return null;
            }
                
                public void initMaxThroughput(Map<Edge, Number> arg0) {
                // TODO Auto-generated method stub
                
            }
                
                public void updateHostLink(NodeConnector arg0, Host arg1, UpdateType arg2,
                                           Set<Property> arg3) {
                // TODO Auto-generated method stub
                
            }
                
                }
