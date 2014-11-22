import json
#import networkx as nx
#from networkx.readwrite import json_graph
import httplib2

baseUrl = 'http://192.168.56.101:8080/controller/nb/v2/'
containerName = 'default/'

h = httplib2.Http(".cache")
h.add_credentials('admin', 'admin')

# Get all the edges/links
resp, content = h.request(baseUrl + 'topology/' + containerName, "GET")
edgeProperties = json.loads(content)
odlEdges = edgeProperties['edgeProperties']
print json.dumps(odlEdges, indent = 2)
for edge in odlEdges:
	e = (edge['edge']['headNodeConnector']['node']['id'])
	print e

#Get all the nodes/switches
resp, content = h.request(baseUrl + 'switchmanager/' + containerName + 'nodes/', "GET")
nodeProperties = json.loads(content)
odlNodes = nodeProperties['nodeProperties']
print json.dumps(nodeProperties, indent = 2)

# Get all the active hosts connected to respective switches
resp, content = h.request(baseUrl + 'hosttracker/' + containerName + 'hosts/active', "GET")
hostProperties = json.loads(content)
#odlEdges = edgeProperties['edgeProperties']
print json.dumps(hostProperties, indent = 2)

"""
resp, content = h.request(baseUrl + 'hosttracker/' + containerName + 'hosts/inactive', "GET")
inactive_hostProperties = json.loads(content)
#odlEdges = edgeProperties['edgeProperties']
print json.dumps(inactive_hostProperties, indent = 2)
"""
# User links
"""
resp, content = h.request(baseUrl + 'topology/' + containerName + 'userLinks', "GET")
userLinks = json.loads(content)
#odlEdges = edgeProperties['edgeProperties']
print json.dumps(userLinks, indent = 2)
"""