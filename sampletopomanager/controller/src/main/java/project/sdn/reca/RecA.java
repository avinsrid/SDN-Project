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
    private Map<Edge, Set<Property>> allEdges= new HashMap<Edge, Set<Property>>();
    List<Switch> switchList;
    Set<HostNodeConnector> hostsList;
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
        RecAImplementation();

    }

    public void RecAImplementation () {
    	Timer = System.currentTimeMillis();
    	while (true) {
    		if (System.currentTimeMillis() - Timer > 10000) {
    			Timer = System.currentTimeMillis();
    			allEdges = topologyManager.getEdges();
    			// See ODL Documentation. Above will return a map of edges --> to properties of every edge
    			// We print the edges and their properties below
    			System.out.println("Printing all edges \n)" + allEdges);
    			
    			// If we want to obtain all nodes (switches connected to controller)
    			switchList = switchManager.getNetworkDevices();
    			System.out.println("Printing all switches connected to the controller \n" + switchList);
    			System.out.println("\n\n\n");
    			
    			// Get all hosts in this network. The hosts will only be printed if they are learnt by the controller, else nothing is displayed
    			// HostTracker does not work as ODL cannot find org.opendaylight.controller.internal.HostTracker , looks lie a bug
    			// Instead we will pull this information using REST API here. We need to download apache REST HTTP binaries for this.
    			// Have to work on this.
    			
    			System.out.println("\n\n\n");
    		}
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

