// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#include <Ice/Ice.h>
#include <IceStorm/IceStorm.h>

#include <CounterI.h>

#include <map>

using namespace std;
using namespace Demo;

class Server : public Ice::Application
{
public:

    virtual int run(int, char*[]);
};

int
main(int argc, char* argv[])
{
    Server app;
    return app.main(argc, argv, "config.server");
}

int
Server::run(int argc, char* argv[])
{
    if(argc > 1)
    {
        cerr << appName() << ": too many arguments" << endl;
        return EXIT_FAILURE;
    }

    Ice::PropertiesPtr properties = communicator()->getProperties();

    const string proxyProperty = "IceStorm.TopicManager.Proxy";
    string proxy = properties->getProperty(proxyProperty);
    if(proxy.empty())
    {
        cerr << appName() << ": property `" << proxyProperty << "' not set" << endl;
        return EXIT_FAILURE;
    }

    IceStorm::TopicManagerPrx manager = IceStorm::TopicManagerPrx::checkedCast(communicator()->stringToProxy(proxy));
    if(!manager)
    {
        cerr << appName() << ": invalid proxy" << endl;
        return EXIT_FAILURE;
    }

    IceStorm::TopicPrx topic;
    try
    {
        topic = manager->retrieve("counter");
    }
    catch(const IceStorm::NoSuchTopic&)
    {
        try
        {
            topic = manager->create("counter");
        }
        catch(const IceStorm::TopicExists&)
        {
            cerr << appName() << ": topic exists, please try again." << endl;
            return EXIT_FAILURE;
        }
    }

    //
    // Create the servant to receive the events.
    //
    Ice::ObjectAdapterPtr adapter = communicator()->createObjectAdapter("Counter");
    adapter->add(new CounterI(topic), communicator()->stringToIdentity("counter"));
    adapter->activate();

    communicator()->waitForShutdown();

    return EXIT_SUCCESS;
}
