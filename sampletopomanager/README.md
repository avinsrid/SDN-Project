############################################################
INSTRUCTIONS TO COMPILE AND RUN THIS
############################################################

1) Go to sampletopomanager directory.
2) Run 'mvn package'
3) You should see no errors. Works on my system.
4) Run the run.sh script from opendaylight-source directory. Refer to
build-compile-instructions.txt for this.
5) Install the reca-0.1.jar and start the process.
6) You will see edges and list of switches displayed every ten seconds.
This is polled from ODL NIB.

For getting hosts (active / inactive), we should use REST API. Download
apache RESTful client API and program the requests as mentioned in
Python file testapiconnectors.py
