package forward;

import reca.*;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.sal.flowprogrammer.Flow;
import org.opendaylight.controller.sal.packet.ARP;
import org.opendaylight.controller.sal.packet.BitBufferHelper;
import org.opendaylight.controller.sal.packet.Ethernet;
import org.opendaylight.controller.sal.packet.IPv4;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.address.DataLinkAddress;
import org.opendaylight.controller.sal.packet.Packet;
import org.opendaylight.controller.sal.packet.PacketResult;
import org.opendaylight.controller.sal.packet.RawPacket;
import org.opendaylight.controller.sal.action.Action;
import org.opendaylight.controller.sal.action.Output;
import org.opendaylight.controller.sal.action.Flood;
import org.opendaylight.controller.sal.match.Match;
import org.opendaylight.controller.sal.match.MatchType;
import org.opendaylight.controller.sal.match.MatchField;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Subnet;

public class L2Forward implements IListenDataPacket {
    private static final Logger logger = LoggerFactory
            .getLogger(L2Forward.class);

    // L2Forward members for the learning switch implementation
    private ISwitchManager switchManager = null;
    private IFlowProgrammerService programmer = null;
    private IDataPacketService dataPacketService = null;
    private Map<Node, Map<Long, NodeConnector>> mac_to_port_per_switch = new HashMap<Node, Map<Long, NodeConnector>>();
    private String function = "switch";
    private Map<Long, Long> numberOfMacs = new HashMap<Long, Long>();
    private long count = 0L;
    private RecA reca_object = new RecA();
    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setFlowProgrammerService(IFlowProgrammerService s)
    {
        this.programmer = s;
    }

    public void unsetFlowProgrammerService(IFlowProgrammerService s) {
        if (this.programmer == s) {
            this.programmer = null;
        }
    }

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
        System.out.println("L2Forward::start() Started");
    }

    /**
     * Function called by the dependency manager before the services
     * exported by the component are unregistered, this will be
     * followed by a "destroy ()" calls
     *
     */
    void stop() {
        logger.info("Stopped");
        System.out.println("L2Forward::stop(): Stopped");
    }


    // Execute this function only when flooding occurs
    private void floodPacket(RawPacket inPkt) {
        NodeConnector incoming_connector = inPkt.getIncomingNodeConnector();
        Node incoming_node = incoming_connector.getNode();

        Set<NodeConnector> nodeConnectors =
                this.switchManager.getUpNodeConnectors(incoming_node);

        for (NodeConnector p : nodeConnectors) {
            if (!p.equals(incoming_connector)) {
                try {
                    RawPacket destPkt = new RawPacket(inPkt);
                    destPkt.setOutgoingNodeConnector(p);
                    this.dataPacketService.transmitDataPacket(destPkt);
                } catch (ConstructionException e2) {
                    continue;
                }
            }
        }
        System.out.println("L2Forward::floodPacket(): Executed flooding");
    }

    // We subscribe to receiveDataPacket to listen in to PACKET_IN events from OF Switch
    public PacketResult receiveDataPacket(RawPacket inPkt) {
        if (inPkt == null) {
            return PacketResult.IGNORED;
        }
        logger.trace("Received a frame of size: {}",
                        inPkt.getPacketData().length);
        System.out.println("L2Forward::receiveDataPacket(): Received a packet");
        Packet formattedPak = this.dataPacketService.decodeDataPacket(inPkt);
        NodeConnector incoming_connector = inPkt.getIncomingNodeConnector();
        Node incoming_node = incoming_connector.getNode();

        if (formattedPak instanceof Ethernet) {
            byte[] srcMAC = ((Ethernet)formattedPak).getSourceMACAddress();
            byte[] dstMAC = ((Ethernet)formattedPak).getDestinationMACAddress();

            // Hub implementation
            if (function.equals("hub")) {
                floodPacket(inPkt);
                return PacketResult.CONSUME;
            }

            // Switch Implementation
            else {
                int srcIP = 0;
                int dstIP = 0;
                boolean checkIfDestInNetwork = true;
                long srcMAC_val = BitBufferHelper.toNumber(srcMAC);
                long dstMAC_val = BitBufferHelper.toNumber(dstMAC);
                System.out.println("L2Forward::receiveDataPacket(): Source MAC = " + srcMAC_val);
                System.out.println("L2Forward::receiveDataPacket(): Destination MAC = " + dstMAC_val);
                Object formattedPakL3 = formattedPak.getPayload();

                if (formattedPakL3 instanceof IPv4) {
                    
                    IPv4 ipv4Pkt = (IPv4) formattedPakL3;
                    // We will retrieve the destination and source IP from the payload of PACKET_IN
                    dstIP = ipv4Pkt.getDestinationAddress();
                    srcIP = ipv4Pkt.getSourceAddress();
                    System.out.println("L2Forward::receiveDataPacket(): Source IP = " + srcIP);
                    System.out.println("L2Forward::receiveDataPacket(): Destination IP = " + dstIP);
                }

                Match match = new Match();
                match.setField( new MatchField(MatchType.IN_PORT, incoming_connector) );
                match.setField( new MatchField(MatchType.DL_DST, dstMAC.clone()) );

                // Create mapping for MAP(Switch, MAP(Host, nodeConnector))
                if (!this.mac_to_port_per_switch.containsKey(incoming_node)) {
                    this.mac_to_port_per_switch.put(incoming_node, new HashMap<Long, NodeConnector>());
                }
                this.mac_to_port_per_switch.get(incoming_node).put(srcMAC_val, incoming_connector);
                System.out.println("L2Forward::receiveDataPacket(): Mapped entries for source MAC " + srcMAC_val + " and destination MAC " + dstMAC_val);
    
                NodeConnector dst_connector = this.mac_to_port_per_switch.get(incoming_node).get(dstMAC_val);
		        System.out.println("L2Forward::receiveDataPacket(): Destination Node Connector = " + dst_connector);

                // We check if the destination IP we are reaching is in same network or not
                if (srcIP != 0 && dstIP != 0) {
                    checkIfDestInNetwork = checkIfHostsInNetwork(srcIP, dstIP);
                }

                // Check if we know the node connector to send packet out to reach destination
                if (dst_connector != null) {
                	System.out.println("L2Forward::ReceiveDataPacket(): Executing Flow Mod");
                    List<Action> actions = new ArrayList<Action>();
                    actions.add(new Output(dst_connector));

                    Flow f = new Flow(match, actions);

                    // Modify the flow on the network node
                    Status status = programmer.addFlow(incoming_node, f);
                    if (!status.isSuccess()) {
                        logger.warn(
                                "SDN Plugin failed to program the flow: {}. The failure is: {}",
                                f, status.getDescription());
                        return PacketResult.IGNORED;
                    }
                    logger.info("Installed flow {} in node {}",
                            f, incoming_node);
                }
                else {
                    if (checkIfDestInNetwork) {
                        floodPacket(inPkt);
                        System.out.println("L2Forward::receiveDataPacket() Flooded");
                    }
                    else {
                        // Find out the port that this has to go to by calling Sareena's PortDiscovery class
                    }                
                }
            }
        }
        return PacketResult.IGNORED;
    }

    // Check if the destination IP we are trying to reach is within our network or not
    public boolean checkIfHostsInNetwork(int sourceIP, int destinationIP) {
        Set<Node> allNodes = switchManager.getNodes();

        // Determine the switch we are connected to,
        Node destinationHostNode = findNodeHostConnectedTo(destinationIP);

        // Check if the destination switch we are trying to reach is within our topology network of switches currently
        if (destinationIP >= 167772160 && destinationIP <= 167772192) {
            if (sourceIP >= 167772160 && sourceIP <= 167772192) {
                System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                return true;
            }
            else {
                if (allNodes.contains(destinationHostNode)) {
                    System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                    return true;
                }
                else return false;
            }
        }
        else if (destinationIP >= 167772192 && destinationIP <= 167772224) {
            if (sourceIP >= 167772192 && sourceIP <= 167772224) {
                System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                return true;
            }
            else {
                if (allNodes.contains(destinationHostNode)) {
                    System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                    return true;
                }
                else return false;
            }
        }
        else if (destinationIP >= 167772224 && destinationIP <= 167772256) {
            if (sourceIP >= 167772224 && sourceIP <= 167772256) {
                System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                return true;
            }
            else {
                if (allNodes.contains(destinationHostNode)) {
                    System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                    return true;
                }
                else return false;
            }
        }
        else if (destinationIP >= 167772256 && destinationIP <= 167772288) {
            if (sourceIP >= 167772256 && sourceIP <= 167772288) {
                System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                return true;
            }
            else {
                if (allNodes.contains(destinationHostNode)) {
                    System.out.println("L2Forward::checkIfHostsInNetwork() Destination IP " + destinationIP + " is in same network as this child controller");
                    return true;
                }
                else return false;
            }
        }
        return false;
    }

    // Through destination IP, determine which switch we are connected to. We are hardcoding this as per topology as HostTracker does not work for
    // northbound API. It works for only REST API.
    public Node findNodeHostConnectedTo(int destIP) {
        System.out.println("L2Forward::findNodeHostConnectedTo(): Determining the destination host " + destIP + "'s domain switch");
        if (destIP >= 167772160 && destIP <= 167772192) {
            return Node.fromString("OF|00:00:00:00:00:00:00:01");
        }
        else if (destIP >= 167772192 && destIP <= 167772224) {
            return Node.fromString("OF|00:00:00:00:00:00:00:02");
        }
        else if (destIP >= 167772224 && destIP <= 167772256) {
            return Node.fromString("OF|00:00:00:00:00:00:00:03");
        }
        else if (destIP >= 167772256 && destIP <= 167772288) {
            return Node.fromString("OF|00:00:00:00:00:00:00:04");
        }
        return null;
    }
}

