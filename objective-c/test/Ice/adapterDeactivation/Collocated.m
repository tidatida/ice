// **********************************************************************
//
// Copyright (c) 2003-2017 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#import <objc/Ice.h>
#import <adapterDeactivation/TestI.h>
#import <TestCommon.h>

static int
run(id<ICECommunicator> communicator)
{
    [[communicator getProperties] setProperty:@"TestAdapter.Endpoints" value:@"default -p 12010"];
    id<ICEObjectAdapter> adapter = [communicator createObjectAdapter:@"TestAdapter"];
    [adapter add:[TestAdapterDeactivationI testIntf] identity:[ICEUtil stringToIdentity:@"test"]];

    id<TestAdapterDeactivationTestIntfPrx> adapterDeactivationAllTests(id<ICECommunicator>);
    adapterDeactivationAllTests(communicator);

    [adapter waitForDeactivate];
    return EXIT_SUCCESS;
}

#if TARGET_OS_IPHONE
#  define main adapterDeactivationCollocated
#endif

int
main(int argc, char* argv[])
{
#ifdef ICE_STATIC_LIBS
    ICEregisterIceSSL(YES);
#if TARGET_OS_IPHONE && !TARGET_IPHONE_SIMULATOR
    ICEregisterIceIAP(YES);
#endif
#endif

    int status;
    @autoreleasepool
    {
        id<ICECommunicator> communicator = nil;

        @try
        {
            ICEInitializationData* initData = [ICEInitializationData initializationData];
            initData.properties = defaultServerProperties(&argc, argv);
#if TARGET_OS_IPHONE
            initData.prefixTable_ = [NSDictionary dictionaryWithObjectsAndKeys:
                                      @"TestAdapterDeactivation", @"::Test",
                                      nil];
#endif
            communicator = [ICEUtil createCommunicator:&argc argv:argv initData:initData];
            status = run(communicator);
        }
        @catch(ICEException* ex)
        {
            tprintf("%@\n", ex);
            status = EXIT_FAILURE;
        }

        if(communicator)
        {
            [communicator destroy];
        }
    }
    return status;
}
