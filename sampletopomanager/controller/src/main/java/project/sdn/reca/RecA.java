package project.sdn.reca;

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
import org.opendaylight.controller.hosttracker.*;
import org.opendaylight.controller.hosttracker.hostAware.HostNodeConnector;

public class RecA implements IRouting, ITopologyManager {
    private static final Logger logger = LoggerFactory
            .getLogger(RecA.class);
    private ISwitchManager switchManager = null;
    private ITopologyManager topologyManager = null;
   // private IHostTrackerShell hostTracker = null;
    private Map<Edge, Set<Property>> allEdges = new HashMap<Edge, Set<Property>>();
    private List<Switch> switchList;
    private Set<NodeConnector> setOfNodeConnWithHosts;
    private Map<Node, Set<Edge>> edgesForEachNode = new HashMap<Node, Set<Edge>>();
    private Map<Node, Map<Node, NodeConnector>> exitToOtherNetwork;
    private String function = "switch";
    long Timer = 0L;


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
   /* 
    void setHostTracker(HostTracker s) {
        logger.debug("HostTracker set");
        this.hostTracker = s;
    }

    void unsetTopologyManager(HostTracker s) {
        if (this.hostTracker == s) {
            logger.debug("HostTracker removed!");
            this.hostTracker = null;
        }
    }*/
    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
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

    /**
     * Function called by the dependency manager when at least one
     * dependency become unsatisfied or when the component is shutting
     * down because for example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called
     * and after the services provided by the class are registered in
     * the service registry
     *
     */
    void start() {
        logger.info("Started");
        System.out.println("Rec-A started!");
        recAImplementation();

    }

    public void recAImplementation () {
    	Timer = System.currentTimeMillis();
    	while (true) {
    		if (System.currentTimeMillis() - Timer > 60000) {
    			Timer = System.currentTimeMillis();
                exitToOtherNetwork = new HashMap<Node, Map<Node, NodeConnector>>();
                Set<Node> nodesInNetwork = switchManager.getNodes();
                edgesForEachNode = topologyManager.getNodeEdges();
                System.out.println("\n\nRecA::recAImplementation: Obtained edges for each node!" + edgesForEachNode);
                if (!nodesInNetwork.isEmpty()) {
                    obtainExitInterface(nodesInNetwork);
                    System.out.println("\n\nRecA::recAImplementation: The function executed!");
                }
                else {
                    System.out.println("\n\nRecA::recAImplementation: The function did not execute!");
                }
                System.out.println("\n\nRecA::recAImplementation: Exit Interfaces in this network: " + exitToOtherNetwork);

                System.out.println("\n\nRecA::recAImplementation: Printing all nodes in the network = " + nodesInNetwork);
                if (!exitToOtherNetwork.isEmpty())
                {   
                    Map<Node, Map<Node, NodeConnector>> tempExitToOtherNetwork = new HashMap<Node, Map<Node, NodeConnector>>();
                    for (Node everyNode : exitToOtherNetwork.keySet()) {
                        if (nodesInNetwork.contains(everyNode)) {
                            tempExitToOtherNetwork.put(everyNode, exitToOtherNetwork.get(everyNode));
                        }
                    exitToOtherNetwork = tempExitToOtherNetwork;
                    }   
                }
                System.out.println("\n\nRecA::recAImplementation: Printing all nodes in the network after filtering = " + exitToOtherNetwork);
            }
    	}
    }

    public void obtainExitInterface(Set<Node> nodesInNetwork) {
        Node[] arrayNodesInNetwork = nodesInNetwork.toArray(new Node[nodesInNetwork.size()]);
        for ( Node node : arrayNodesInNetwork ) {
            System.out.println("\n\nWorking on node + " + node + "\n\n");
            Map<Node, NodeConnector> switchToPort = new HashMap<Node, NodeConnector>();
            // Obtain set of node connectors for each switch
            Set<NodeConnector> setOfNodeConn = switchManager.getNodeConnectors(node);
            System.out.println("\n\nobtainExitInterface: NodeConnectors for each switch\n\n" + setOfNodeConn);
            // Obtain set of edges for each switch
            Set<Edge> edgesPerSwitch = edgesForEachNode.get(node);
            System.out.println("\n\nobtainExitInterface: Edges for each switch\n\n" + edgesPerSwitch);
            // Convert the set of edges of each switch to an array
            Edge[] arrayEdgePerSwitch = edgesPerSwitch.toArray(new Edge[edgesPerSwitch.size()]);
            System.out.println("\n\nobtainExitInterface: Array Format -- Edges for each switch\n\n" + arrayEdgePerSwitch);

            // Convert the set of node connectors for each switch to an array
            NodeConnector[] arrayOfNodeConn = setOfNodeConn.toArray(new NodeConnector[setOfNodeConn.size()]);
            System.out.println("\n\nobtainExitInterface: Array Format -- Edges for each switch\n\n" + arrayOfNodeConn);

            // Check from the edges of each switch, if there is a uni directional link. If so, we store that as the link going to other
            // controller's network. This is done by checking if there is no reverse link for that specific node
            for (Edge link1 : arrayEdgePerSwitch) {
                boolean check = false;
                for (Edge link2 : arrayEdgePerSwitch) {
                    Edge link2Reverse;
                    try {
                        link2Reverse = new Edge(link2.getHeadNodeConnector(), link2.getTailNodeConnector());
                        //catch(ConstructionException c) {System.out.println("Exception to create edge!");}
                        System.out.println("\n\nRecA::obtainExitInterface: Reversed Edge is = " + link2Reverse);
                        if (link1.equals(link2Reverse)) {
                            System.out.println("\n\nRecA::obtainExitInterface: Check hit true for edge = " + link1);
                            check = true;
                            break;
                        }
                    }
                    catch(ConstructionException c) {System.out.println("\n\nRecA::obtainExitInterface: Exception to create edge!");}
                }
                if (check == false) {
                NodeConnector headNode = link1.getHeadNodeConnector();
                NodeConnector tailNode = link1.getTailNodeConnector();
                Node tempNode = tailNode.getNode();
                switchToPort.put(tempNode, headNode);
                }
            }
            exitToOtherNetwork.put(node, switchToPort);
        }
    }
    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
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

