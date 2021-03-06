# **********************************************************************
#
# Copyright (c) 2003-2017 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

import Ice, Test, Dispatcher, sys, threading, random

def test(b):
    if not b:
        raise RuntimeError('test assertion failed')

class Callback:
    def __init__(self):
        self._called = False
        self._cond = threading.Condition()
        self._mainThread = threading.current_thread()

    def check(self):
        with self._cond:
            while not self._called:
                self._cond.wait()
            self._called = False

    def called(self):
        with self._cond:
            self._called = True
            self._cond.notify()

    def response(self, f):
        test(f.exception() is None)
        test(Dispatcher.Dispatcher.isDispatcherThread())
        self.called()

    def exception(self, f):
        test(isinstance(f.exception(), Ice.NoEndpointException))
        test(Dispatcher.Dispatcher.isDispatcherThread())
        self.called()

    def exceptionEx(self, f):
        test(isinstance(f.exception(), Ice.InvocationTimeoutException))
        test(Dispatcher.Dispatcher.isDispatcherThread())
        self.called()

    def payload(self, f):
        if f.exception():
            test(isinstance(f.exception(), Ice.CommunicatorDestroyedException))
        else:
            test(Dispatcher.Dispatcher.isDispatcherThread())

    def checkThread(self):
        #
        # A done callback can be invoked recursively from the calling thread if the request was already complete.
        #
        test(Dispatcher.Dispatcher.isDispatcherThread() or threading.current_thread() == self._mainThread)

def allTests(communicator, collocated):
    sref = "test:default -p 12010"
    obj = communicator.stringToProxy(sref)
    test(obj)

    p = Test.TestIntfPrx.uncheckedCast(obj)

    sref = "testController:default -p 12011"
    obj = communicator.stringToProxy(sref)
    test(obj)

    testController = Test.TestIntfControllerPrx.uncheckedCast(obj)

    dispatcher = Dispatcher.Dispatcher.instance()

    sys.stdout.write("testing dispatcher... ")
    sys.stdout.flush()

    p.op()

    cb = Callback()

    p.opAsync().add_done_callback(cb.response, dispatcher.dispatchSync)
    cb.check()

    #
    # Expect NoEndpointException.
    #
    i = p.ice_adapterId("dummy")
    i.opAsync().add_done_callback(cb.exception, dispatcher.dispatchSync)
    cb.check()

    #
    # Expect InvocationTimeoutException.
    #
    to = p.ice_invocationTimeout(250);
    to.sleepAsync(500).add_done_callback(cb.exceptionEx, dispatcher.dispatchSync)
    cb.check()

    testController.holdAdapter()

    if sys.version_info[0] == 2:
        b = [chr(random.randint(0, 255)) for x in range(0, 1024)]
        seq = ''.join(b)
    else:
        b = [random.randint(0, 255) for x in range(0, 1024)]
        seq = bytes(b)

    f = None
    while True:
        f = p.opWithPayloadAsync(seq)
        f.add_done_callback(cb.payload, dispatcher.dispatchSync)
        if not f.is_sent_synchronously():
            break
    testController.resumeAdapter()
    f.result()

    print("ok")

    p.shutdown()
