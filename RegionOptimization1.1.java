package sdn.project.ro;

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
    int a = 0;
    int check = 0;
    int count = 0;
    
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
    	while (true) {
    		if (System.currentTimeMillis() - Timer > 10000) {
    			Timer = System.currentTimeMillis();
                
                // Topology for Child Controller C2
                
                
                
                // Topology for Child Controller C1
                
                edgesForEachNode = topologyManager.getNodeEdges();
                System.out.println("\n\nRO::roImplementation: Obtained edges for each node!" + edgesForEachNode);
                
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
    
    public void Switchfailovercheck()
    {
        Map<Node,Integer> temp = new HashMap<Node,Integer>();
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
                        else{
                            System.out.println("\n\n CHANGE IN TOPOLOGY");}
                    }
                    else{
                        System.out.println("\n\n SAME NODES NOT PRESENT IN CURRENT AND NEW TOPOLOGY");
                    }
                }
            }
            else{
                System.out.println("\n\n SAME NUMBER OF NODES NOT PRESENT IN CURRENT AND NEW TOPOLOGY");
            }
        }
        
        temp = newTopology;
        currentTopology = temp;
        System.out.println("\n\n REACHED END OF SWITCHFAILOVER FUNCTION");
        
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

