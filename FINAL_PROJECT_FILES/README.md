COMSE-6998 SDN Project Fall 2014
   Team: Avinash Sridhar (as4626), Nachiket Rau (nnr2107), Shruti Ramesh (sr3155), Sareena Abdul Razak (sta2378)

README FILE
===============================================================================================================

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
be sure to modify the tomcat REST API port bindings to a port apart from 8080 else you will receive exceptions in log. To change this port, go to 

distribution/opendaylight-osgi/target/distribution-osgi-1.0.0-osgipackage/opendaylight/configuration/ and modify the tomcat-server.xml file and make tomcat
listening port to 8081 or 8082.




 

