package regopt;

import java.util.Hashtable;
import java.util.Dictionary;
import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.sal.packet.IListenDataPacket;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.flowprogrammer.IFlowProgrammerService;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.topologymanager.ITopologyManager;
import org.opendaylight.controller.topologymanager.ITopologyManagerAware;
import org.opendaylight.controller.sal.routing.IRouting;


public class Activator extends ComponentActivatorAbstractBase {
    protected static final Logger logger = LoggerFactory
    .getLogger(Activator.class);
    
    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     *
     */
    public void init() {
        
    }
    
    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase
     *
     */
    public void destroy() {
        
    }
    
    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container
     *
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    public Object[] getImplementations() {
        Object[] res = { RO.class };
        return res;
    }
    
    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    public void configureInstance(Component c, Object imp, String containerName) {
        if (imp.equals(RO.class)) {
            // export the services
            Dictionary<String, String> props = new Hashtable<String, String>();
            props.put("salListenerName", "ro");
            c.setInterface(new String[] { IListenDataPacket.class.getName() }, props);
            c.setInterface(new String[] { ITopologyManagerAware.class.getName() }, props);
            c.setInterface(new String[] { IRouting.class.getName() }, props);
            
            // register dependent modules
            c.add(createContainerServiceDependency(containerName).setService(
                                                                             ISwitchManager.class).setCallbacks("setSwitchManager","unsetSwitchManager").setRequired(true));
            
            c.add(createContainerServiceDependency(containerName).setService(
                                                                             ITopologyManager.class).setCallbacks("setTopologyManager", "unsetTopologyManager")
                  .setRequired(true));
            /* c.add(createContainerServiceDependency(containerName).setService(
             HostTracker.class).setCallbacks(
             "setHostTracker", "unsetHostTracker")
             .setRequired(true));*/
        }
    }
}
