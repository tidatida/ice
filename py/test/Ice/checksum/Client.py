#!/usr/bin/env python
# **********************************************************************
#
# Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

import os, sys, traceback

for toplevel in [".", "..", "../..", "../../..", "../../../.."]:
    toplevel = os.path.normpath(toplevel)
    if os.path.exists(os.path.join(toplevel, "python", "Ice.py")):
        break
else:
    raise "can't find toplevel directory!"

sys.path.insert(0, os.path.join(toplevel, "python"))
sys.path.insert(0, os.path.join(toplevel, "lib"))

import Ice

#
# Find Slice directory.
#
slice_dir = os.path.join(os.path.join(toplevel, "..", "slice"))
if not os.path.exists(slice_dir):
    home_dir = os.getenv('ICEPY_HOME', '')
    if len(home_dir) == 0 or not os.path.exists(os.path.join(home_dir, "slice")):
        home_dir = os.getenv('ICE_HOME', '')
    if len(home_dir) == 0 or not os.path.exists(os.path.join(home_dir, "slice")):
        print sys.argv[0] + ': Slice directory not found. Define ICEPY_HOME or ICE_HOME.'
        sys.exit(1)
    slice_dir = os.path.join(home_dir, "slice")

Ice.loadSlice('-I' + slice_dir + ' --checksum Test.ice CTypes.ice')
import Test, AllTests

def run(args, communicator):
    checksum = AllTests.allTests(communicator)
    checksum.shutdown()
    return True

try:
    communicator = Ice.initialize(sys.argv)
    status = run(sys.argv, communicator)
except:
    traceback.print_exc()
    status = False

if communicator:
    try:
        communicator.destroy()
    except:
        traceback.print_exc()
        status = False

sys.exit(not status)
