#!/usr/bin/env python
# **********************************************************************
#
# Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

import os, sys

for toplevel in [".", "..", "../..", "../../..", "../../../.."]:
    toplevel = os.path.normpath(toplevel)
    if os.path.exists(os.path.join(toplevel, "config", "TestUtil.py")):
        break
else:
    raise "can't find toplevel directory!"

sys.path.append(os.path.join(toplevel, "config"))
import TestUtil

name = os.path.join("Ice", "gc")
testdir = os.path.dirname(os.path.abspath(__file__))

client = os.path.join(testdir, "client")

print "starting client...",
seedfile = testdir + "/seed"
clientPipe = os.popen(TestUtil.getCommandLine(client, TestUtil.DriverConfig("client"), TestUtil.getTestEnv()) + " " + seedfile + " 2>&1")
print "ok"

TestUtil.printOutputFromPipe(clientPipe)
    
clientStatus = TestUtil.closePipe(clientPipe)

if clientStatus:
    sys.exit(1)

os.remove(seedfile)

sys.exit(0)
