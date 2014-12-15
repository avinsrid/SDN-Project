COMSE-6998 SDN Project Fall 2014
   Team: Avinash Sridhar (as4626), Nachiket Rau (nnr2107), Shruti Ramesh (sr3155), Sareena Abdul Razak (sta2378)

README FILE
===============================================================================================================

OUR SETUP
===============================================================================================================
Child Controller-1 : 192.168.56.20 OF Port: 6633 REST: 8080
##########################
Child Controller-2 : 192.168.56.21 OF Port: 6633 REST: 8080
##########################
Parent Controller (Base Machine): 192.168.56.1 OF Port: 6633 REST: 8081
##########################

INSTRUCTIONS TO COMPILE / RUN
===============================================================================================================

CHILD CONTROLLER
===============================================================================================================

1) Firstly, we need three VMS to run the two child controllers + parent controller. We can also use two VMs for Child Controllers
	and a parent controller on the base PC machine. We used the second scenario for testing, as three VM took up too much load on our
	Desktop.
##########################

2) Perform the same activity on Child-Controller-2 directory as you would with Child-Controller-1

##########################

a) Navigate to Child-Controller-1/

##########################

b) Ensure maven is installed. This is mandatory for the build. You can verify by doing 'mvn -version'

##########################

c) Run 'mvn clean install' or 'mvn clean package'

##########################

d) You should not see any compilation errors. Now navigate to 'distribution/opendaylight-osgi/target/distribution-osgi-1.0.0-osgipackage/opendaylight/
   ' and execute the run.sh script. Controller will now start

##########################

e) Perform the same for Child-Controller-2 also

##########################

PARENT CONTROLLER INSTRUCTIONS:
===============================================================================================================
We can use any existing parent controller, as there is no special code for this.

You may clone the SDN-Hub-Tutorial and run this PARENT Controller on a separate VM or base machine PC. If you are running this on base machine PC,

git clone https://github.com/avinsrid/SDNHub_Opendaylight_Tutorial.git

be sure to modify the tomcat REST API port bindings to a port apart from 8080 else you will receive exceptions in log. To change this port, go to 
distribution/opendaylight-osgi/target/distribution-osgi-1.0.0-osgipackage/opendaylight/configuration/ and modify the tomcat-server.xml file and make tomcat listening port to 8081 or 8082.

MININET and MssM (Multi Threaded Smart Socket Master)
==============================================================================================================

Run mininet on a separate VM. In our testing we ran the mininet on 192.168.56.101
####################################

The MssM application is a java program and can be run the following ways:
####################################
1) Copy the MssM.java to Mininet machine.
####################################
2) Compile the MssM --> sudo javac MssM.java
####################################
3) Run the MssM --> sudo java MssM
####################################

For any queries, please feel free to contact Avinash, Nachiket, Shruti, Sareena


 

