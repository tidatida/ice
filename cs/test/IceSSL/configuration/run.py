#!/usr/bin/env python
# **********************************************************************
#
# Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

import os, sys, getopt

for toplevel in [".", "..", "../..", "../../..", "../../../..", "../../../../.."]:
    toplevel = os.path.normpath(toplevel)
    if os.path.exists(os.path.join(toplevel, "config", "TestUtil.py")):
        break
else:
    raise "can't find toplevel directory!"

sys.path.append(os.path.join(toplevel, "config"))
import TestUtil

name = os.path.join("IceSSL", "configuration")

testdir = os.path.dirname(os.path.abspath(__file__))

#
# The drive letter needs to be removed on Windows or loading the SSL
# plugin will not work.
#
TestUtil.clientServerTestWithOptions(name, "", " " + os.path.splitdrive(testdir)[1])
sys.exit(0)
