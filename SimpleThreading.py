#!/usr/bin/python

"""
    SDN-Project
    Code to run multiple controllers and make switches talk to them
    By: Avinash Sridhar, Nachiket Rau, Shruti Ramesh, Sareena Abdul Razak
    To execute,
    sudo ./mininet_test_scenario.py
    
    Thread 1 will run mininet and start our controllers C0 -> Master , C1, C2 -> Child Controllers
    
    Thread 2 will run script for handover / region optimization
    """

import threading
import time

from mininet.net import Mininet
from mininet.node import Controller, RemoteController, OVSSwitch
from mininet.cli import CLI
from mininet.log import setLogLevel

def multiControllerNet():
    "Create a network from semi-scratch with multiple controllers."
    
    net = Mininet( controller=Controller, switch=OVSSwitch )
    
    print "*** Creating (reference) controllers On 192.168.2.50/51 Change if necessary"
    c0 = RemoteController( 'c0', ip='192.168.2.51', port=6634 )  #This is parent controller (change tomcat 8081)
    c1 = RemoteController( 'c1', ip='192.168.2.51', port=6633 )
    c2 = RemoteController( 'c2', ip='192.168.2.50', port=6633 )
    
    
    print "*** Creating switches"
    s1 = net.addSwitch( 's1' )
    s2 = net.addSwitch( 's2' )
    s3 = net.addSwitch( 's3' )
    s4 = net.addSwitch( 's4' )
    gs1 = net.addSwitch( 'gs1' )
    gs2 = net.addSwitch( 'gs2' )
    
    print "*** Creating hosts"
    hosts1 = [ net.addHost( 'h%d' % n ) for n in 1, 2, 3, 4 ]
    hosts2 = [ net.addHost( 'h%d' % n ) for n in 5, 6, 7, 8 ]
    hosts3 = [ net.addHost( 'h%d' % n ) for n in 9, 10, 11, 12 ]
    hosts4 = [ net.addHost( 'h%d' % n ) for n in 13, 14, 15, 16 ]
    
    
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
    
    print "*** Running CLI"
    CLI( net )

def runHandover():
    "Start a function to invoke region optimization after set time"
    print "Started thread 2"
    present = time.time()
    timeout = 5
        # if(time.time() - present)>timeout:
        # print "Timed Out"
    while 1:
        if (time.time() - present) > timeout:
            present=time.time()
            print "hello"

#Create new threads

threads = []
thread1 = threading.Thread(target=multiControllerNet)
thread2 = threading.Thread(target=runHandover)

#start threads
thread1.start()

#give delay before starting thread 2 if required.
timeout=5
presentTime = time.time()
while 1:
    if (time.time() - presentTime) > timeout:
        thread2.start()
        break


