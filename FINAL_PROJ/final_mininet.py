#!/usr/bin/python

"""
    SDN-Project
    Code to run multiple controllers and make switches talk to them
    By: Avinash Sridhar, Nachiket Rau, Shruti Ramesh, Sareena Abdul Razak
    To execute,
    sudo ./mininet_test_scenario.py
    """

from mininet.net import Mininet
from mininet.node import Controller, RemoteController, OVSSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel
import time
import threading
from random import randint
import subprocess
import os

def addHost1( net, N ):
    "Create host hN and add to net."
    name = 'h%d' % N
    ip = '10.0.0.%d' % N
    mac = '00:00:00:00:00:%d' % N
    return net.addHost( name, ip=ip, mac=mac )

def addHost2( net, N ):
    "Create host hN and add to net."
    name = 'h%d' % N
    temp = N+32
    ip = '10.0.0.%d' % temp
    mac = '00:00:00:00:00:%d' % N
    return net.addHost( name, ip=ip, mac=mac )

def addHost3( net, N ):
    "Create host hN and add to net."
    name = 'h%d' % N
    temp = N+64
    ip = '10.0.0.%d' % temp
    mac = '00:00:00:00:00:%d' % N
    return net.addHost( name, ip=ip, mac=mac )

def addHost4( net, N ):
    "Create host hN and add to net."
    name = 'h%d' % N
    temp = N+96
    ip = '10.0.0.%d' % temp
    mac = '00:00:00:00:00:%d' % N
    return net.addHost( name, ip=ip, mac=mac )

def BeginSimulation() :

    # We begin mininet initially
    net = Mininet( controller=Controller, switch=OVSSwitch)

    print "*** Creating (reference) controllers On 192.168.2.50/51 Change if necessary"
    c0 = RemoteController( 'c0', ip='192.168.56.102', port=6633 )  #This is parent controller (change tomcat 8081)
    c1 = RemoteController( 'c1', ip='192.168.56.102', port=6633 )
    c2 = RemoteController( 'c2', ip='192.168.56.102', port=6633 )

    print "*** Creating switches"
    s1 = net.addSwitch( 's1' )
    s2 = net.addSwitch( 's2' )
    s3 = net.addSwitch( 's3' )
    s4 = net.addSwitch( 's4' )
    gs1 = net.addSwitch( 'gs1' )
    gs2 = net.addSwitch( 'gs2' )

    print "*** Creating hosts"
    print "*** Creating hosts"
    hosts1 = [ addHost1( net, n) for n in 1, 2, 3, 4 ]
    hosts2 = [ addHost2( net, n) for n in 5, 6, 7, 8 ]
    hosts3 = [ addHost3( net, n) for n in 9, 10, 11, 12 ]
    hosts4 = [ addHost4( net, n) for n in 13, 14, 15, 16 ]

    # These are the hsots that move to other side. We will bring the links up when we want to show the host movement
    s3_temp_host1 = net.addHost( 'h17', ip = '10.0.0.66', mac = '00:00:00:00:00:1') 
    s3_temp_host2 = net.addHost( 'h18', ip = '10.0.0.67', mac = '00:00:00:00:00:2') 

    print "*** Creating links"
    for h in hosts1:
        net.addLink( s1, h )
    for h in hosts2:
        net.addLink( s2, h )
    for h in hosts3:
        net.addLink( s3, h )
    for h in hosts4:
        net.addLink( s4, h )

    net.addLink( s1, s2 )
    net.addLink( s3, s4 )

    net.addLink( s2, s3 )

    net.addLink( gs1, gs2 )

    # Additional links to simlulate host movement (handover). Note that this link will be down initially
    net.addLink( s3, s3_temp_host1)
    net.addLink( s3, s3_temp_host2)

    print "*** Starting network"
    net.build()
    c0.start()
    c1.start()
    c2.start()
    s1.start( [ c1 ] )
    s2.start( [ c1 ] )
    s3.start( [ c2 ] )
    s4.start( [ c2 ] )
    gs1.start( [ c0 ] )
    gs2.start( [ c0 ] )

    # Remember that these two links will be down initially and needs to be manually pulled up whenever we need handover
    net.configLinkStatus('h17', 's3', 'down')
    net.configLinkStatus('h18', 's3', 'down')
    CLI(net)

if __name__ == '__main__' :
    BeginSimulation()


