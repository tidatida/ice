#!/usr/bin/env python
# **********************************************************************
#
# Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

import os, sys, time, threading, re

for toplevel in [".", "..", "../..", "../../..", "../../../.."]:
    toplevel = os.path.normpath(toplevel)
    if os.path.exists(os.path.join(toplevel, "config", "TestUtil.py")):
        break
else:
    raise "can't find toplevel directory!"

sys.path.append(os.path.join(toplevel, "config"))
import TestUtil

name = os.path.join("IceStorm", "stress")
testdir = os.path.dirname(os.path.abspath(__file__))
exedir = testdir

iceBox = TestUtil.getIceBox(exedir)
iceBoxAdmin = os.path.join(TestUtil.getBinDir(__file__), "iceboxadmin")
iceStormAdmin = os.path.join(TestUtil.getBinDir(__file__), "icestormadmin")

iceBoxEndpoints = ' --IceBox.ServiceManager.Endpoints="default -p 12010" --Ice.Default.Locator='

iceStormService = " --IceBox.Service.IceStorm=IceStormService," + TestUtil.getIceSoVersion() + ":createIceStorm" + \
                  ' --IceStorm.TopicManager.Endpoints="default -p 12011"' + \
                  ' --IceStorm.Publish.Endpoints="default -p 12012"' + \
                  ' --IceStorm.InstanceName=TestIceStorm1 ' + \
                  ' --IceStorm.Discard.Interval=2' + \
                  ' --IceBox.PrintServicesReady=IceStorm' + \
                  " --IceBox.InheritProperties=1"
iceStormReference = ' --IceStorm.TopicManager.Proxy="TestIceStorm1/TopicManager: default -p 12011"'

iceBoxEndpoints2 = ' --IceBox.ServiceManager.Endpoints="default -p 12020" --Ice.Default.Locator='

iceStormService2 = " --IceBox.Service.IceStorm=IceStormService," + TestUtil.getIceSoVersion() + ":createIceStorm" + \
                  ' --IceStorm.TopicManager.Endpoints="default -p 12021"' + \
                  ' --IceStorm.Publish.Endpoints="default -p 12022"' + \
                  ' --IceStorm.InstanceName=TestIceStorm2 ' + \
                  ' --IceStorm.Discard.Interval=2' + \
                  ' --IceBox.PrintServicesReady=IceStorm' + \
                  " --IceBox.InheritProperties=1"
iceStormReference2 = ' --IceStorm.TopicManager.Proxy="TestIceStorm2/TopicManager: default -p 12021"'

adminIceStormReference = ' --IceStormAdmin.TopicManager.Proxy="TestIceStorm1/TopicManager: default -p 12011" ' + \
    '--IceStormAdmin.TopicManager.Proxy2="TestIceStorm2/TopicManager: default -p 12021"'

def doTest(subOpts, pubOpts):
    global testdir
    global iceStormReference
    global iceStormReference2

    publisher = os.path.join(testdir, "publisher")
    subscriber = os.path.join(testdir, "subscriber")

    subscriberPipes = []
    if type(subOpts) != type([]):
        subOpts = [ subOpts ]
    for opts in subOpts:
        # We don't want the subscribers to time out.
        pipe = TestUtil.startServer(subscriber, r' --Ice.ServerIdleTime=0 ' + opts + " 2>&1")
        TestUtil.getServerPid(pipe)
        TestUtil.getAdapterReady(pipe)
        subscriberPipes.append(pipe)

    publisherPipe = TestUtil.startClient(publisher, iceStormReference + ' ' + pubOpts + " 2>&1")

    TestUtil.printOutputFromPipe(publisherPipe)

    publisherStatus = TestUtil.closePipe(publisherPipe)
    if publisherStatus:
        print "(publisher failed)",
        return publisherStatus
    for p in subscriberPipes:
        try:
            sys.stdout.flush()
            subscriberStatus = TestUtil.specificServerStatus(p)
        except:
            print "(subscriber failed)",
            return 1
        if subscriberStatus:
            print "(subscriber failed)",
            return subscriberStatus

    return 0

def startServers(additionalArgs=""):
    global iceBox
    global iceBoxEndpoints
    global iceBoxEndpoints2
    global iceStormService
    global iceStormService2
    global iceStormDBEnv
    global iceStormDBEnv2
    print "starting icestorm services...",
    sys.stdout.flush()
    # Clear the idle timeout otherwise the IceBox ThreadPool will timeout.wA
    command = iceBoxEndpoints + iceStormService + iceStormDBEnv + ' --Ice.ServerIdleTime=0' + additionalArgs
    iceBoxPipe = TestUtil.startServer(iceBox, command + " 2>&1")
    TestUtil.getServerPid(iceBoxPipe)
    TestUtil.waitServiceReady(iceBoxPipe, "IceStorm")
    command =  iceBoxEndpoints2 + iceStormService2 + iceStormDBEnv2 + ' --Ice.ServerIdleTime=0' + additionalArgs
    iceBoxPipe2 = TestUtil.startServer(iceBox, command + " 2>&1")
    TestUtil.getServerPid(iceBoxPipe2)
    TestUtil.waitServiceReady(iceBoxPipe2, "IceStorm")
    print "ok"

    return iceBoxPipe, iceBoxPipe2

def stopServers(p1, p2 = None):
    global iceBox
    global iceBoxAdmin
    global iceBoxEndpoints
    global iceBoxEndpoints2
    print "shutting down icestorm services...",
    sys.stdout.flush()
    command =  iceBoxEndpoints + r' shutdown'
    pipe = TestUtil.startClient(iceBoxAdmin, command + " 2>&1")
    status = TestUtil.closePipe(pipe)
    if status or TestUtil.specificServerStatus(p1):
        TestUtil.killServers()
        sys.exit(1)
    if p2:
        command =  iceBoxEndpoints2 + r' shutdown'
        pipe = TestUtil.startClient(iceBoxAdmin, command + " 2>&1")
        status = TestUtil.closePipe(pipe)
        if status or TestUtil.specificServerStatus(p2):
            TestUtil.killServers()
            sys.exit(1)
    print "ok"

def runAdmin(cmd, desc = None):
    global iceStormAdmin
    global iceStormAdminReference
    if desc:
        print desc,
        sys.stdout.flush()
    command = adminIceStormReference + r' -e "' + cmd + '"'
    pipe = TestUtil.startClient(iceStormAdmin, command + " 2>&1")
    status = TestUtil.closePipe(pipe)
    if status:
        TestUtil.killServers()
        sys.exit(1)
    if desc:
        print "ok"

dbHome = os.path.join(testdir, "db")
TestUtil.cleanDbDir(dbHome)
iceStormDBEnv=" --Freeze.DbEnv.IceStorm.DbHome=" + dbHome

dbHome2 = os.path.join(testdir, "db2")
TestUtil.cleanDbDir(dbHome2)
iceStormDBEnv2=" --Freeze.DbEnv.IceStorm.DbHome=" + dbHome2

server1, server2 = startServers()

runAdmin("create TestIceStorm1/fed1 TestIceStorm2/fed1", "setting up the topics...")

print "Sending 5000 ordered events... ",
sys.stdout.flush()
status = doTest('--events 5000 --qos "reliability,ordered" ' + iceStormReference, '--events 5000')
if status:
    print "failed!"
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("link TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 5000 ordered events across a link... ",
sys.stdout.flush()
status = doTest('--events 5000 --qos "reliability,ordered" ' + iceStormReference2, '--events 5000')
if status:
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("unlink TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered events... ",
sys.stdout.flush()
status = doTest('--events 20000 ' + iceStormReference, '--events 20000 --oneway')
if status:
    print "failed!"
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("link TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered events across a link... ",
sys.stdout.flush()
status = doTest('--events 20000 ' + iceStormReference2, '--events 20000 --oneway')
if status:
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("unlink TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered batch events... ",
sys.stdout.flush()
status = doTest('--events 20000 --qos "reliability,batch" ' + iceStormReference, '--events 20000 --oneway')
if status:
    print "failed!"
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("link TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered batch events across a link... ",
sys.stdout.flush()
status = doTest('--events 20000 --qos "reliability,batch" ' + iceStormReference2, '--events 20000 --oneway')
if status:
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("unlink TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered events with slow subscriber... ",
status = doTest(['--events 2 --slow ' + iceStormReference, '--events 20000 ' + iceStormReference], '--events 20000 --oneway')
if status:
    print "failed!"
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("link TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered events with slow subscriber & link... ",
status = doTest(['--events 2 --slow' + iceStormReference, '--events 20000' + iceStormReference, '--events 2 --slow' + iceStormReference2, '--events 20000' + iceStormReference2], '--events 20000 --oneway')
if status:
    print "failed!"
    TestUtil.killServers()
    sys.exit(1)
print "ok"

#
# The erratic tests emit lots of connection warnings so they are
# disabled here. The IceStorm servers are stopped and restarted so the
# settings will take effect.
#
stopServers(server1, server2)
server1, server2 = startServers(" --Ice.Warn.Connections=0")

runAdmin("unlink TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered events with erratic subscriber... ",
sys.stdout.flush()
status = doTest(\
    [ '--erratic 5 --qos "reliability,ordered" --events 20000' + iceStormReference, \
      '--erratic 5 --events 20000' + iceStormReference, \
      '--events 20000' + iceStormReference], \
      '--events 20000 --oneway')
if status:
    print "failed!"
    TestUtil.killServers()
    sys.exit(1)
print "ok"

runAdmin("link TestIceStorm1/fed1 TestIceStorm2/fed1")
print "Sending 20000 unordered events with erratic subscriber across a link... ",
sys.stdout.flush()
status = doTest( \
     [ '--events 20000' + iceStormReference, \
       '--erratic 5 --qos "reliability,ordered" --events 20000 ' + iceStormReference, \
       '--erratic 5 --events 20000 ' + iceStormReference, \
       '--events 20000' + iceStormReference2, \
       '--erratic 5 --qos "reliability,ordered" --events 20000 ' + iceStormReference2, \
       '--erratic 5 --events 20000 ' + iceStormReference2], \
       '--events 20000 --oneway ')
if status:
    print "failed!"
    TestUtil.killServers()
    sys.exit(1)
print "ok"

#
# Shutdown icestorm.
#
stopServers(server1, server2)
if TestUtil.serverStatus():
    TestUtil.killServers()
    sys.exit(1)

sys.exit(0)
