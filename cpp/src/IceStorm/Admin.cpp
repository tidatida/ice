// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#include <IceUtil/DisableWarnings.h>
#include <IceUtil/Options.h>
#include <Ice/Application.h>
#include <Ice/SliceChecksums.h>
#include <IceStorm/Parser.h>

#include <fstream>

using namespace std;
using namespace Ice;
using namespace IceStorm;

class Client : public Application
{
public:

    void usage();
    virtual int run(int, char*[]);
};

int
main(int argc, char* argv[])
{
    Client app;
    int rc = app.main(argc, argv);
    return rc;
}

void
Client::usage()
{
    cerr << "Usage: " << appName() << " [options]\n";
    cerr <<     
        "Options:\n"
        "-h, --help           Show this message.\n"
        "-v, --version        Display the Ice version.\n"
        "-DNAME               Define NAME as 1.\n"
        "-DNAME=DEF           Define NAME as DEF.\n"
        "-UNAME               Remove any definition for NAME.\n"
        "-IDIR                Put DIR in the include file search path.\n"
        "-e COMMANDS          Execute COMMANDS.\n"
        "-d, --debug          Print debug messages.\n"
        ;
}

int
Client::run(int argc, char* argv[])
{
    string commands;
    bool debug;

    IceUtilInternal::Options opts;
    opts.addOpt("h", "help");
    opts.addOpt("v", "version");
    opts.addOpt("e", "", IceUtilInternal::Options::NeedArg, "", IceUtilInternal::Options::Repeat);
    opts.addOpt("d", "debug");

    vector<string> args;
    try
    {
        args = opts.parse(argc, (const char**)argv);
    }
    catch(const IceUtilInternal::BadOptException& e)
    {
        cerr << e.reason << endl;
        usage();
        return EXIT_FAILURE;
    }
    if(!args.empty())
    {
        cerr << argv[0] << ": too many arguments" << endl;
        usage();
        return EXIT_FAILURE;
    }

    if(opts.isSet("help"))
    {
        usage();
        return EXIT_SUCCESS;
    }
    if(opts.isSet("version"))
    {
        cout << ICE_STRING_VERSION << endl;
        return EXIT_SUCCESS;
    }
    if(opts.isSet("e"))
    {
        vector<string> optargs = opts.argVec("e");
        for(vector<string>::const_iterator i = optargs.begin(); i != optargs.end(); ++i)
        {
            commands += *i + ";";
        }
    }
    debug = opts.isSet("debug");

    // The complete set of Ice::Identity -> manager proxies.
    map<Ice::Identity, IceStorm::TopicManagerPrx> managers;
    
    // IceStorm.TopicManager.Proxy is the "default" manager.
    PropertiesPtr properties = communicator()->getProperties();
    const char* managerProxy= "IceStorm.TopicManager.Proxy";
    string managerProxyValue = properties->getProperty(managerProxy);
    IceStorm::TopicManagerPrx defaultManager;
    if(!managerProxyValue.empty())
    {
        defaultManager = IceStorm::TopicManagerPrx::checkedCast(communicator()->propertyToProxy(managerProxy));
        if(!defaultManager)
        {
            cerr << appName() << ": `" << managerProxyValue << "' is not running" << endl;
            return EXIT_FAILURE;
        }
        managers.insert(map<Ice::Identity, IceStorm::TopicManagerPrx>::value_type(
                            defaultManager->ice_getIdentity(), defaultManager));
    }

    //
    // Get remaining managers.
    //
    Ice::PropertyDict props = communicator()->getProperties()->getPropertiesForPrefix("IceStormAdmin.TopicManager.");
    {
        for(Ice::PropertyDict::const_iterator p = props.begin(); p != props.end(); ++p)
        {
            //
            // Ignore proxy property settings. eg IceStormAdmin.TopicManager.*.LocatorCacheTimeout
            //
            if(p->first.find('.', strlen("IceStormAdmin.TopicManager.")) == string::npos)
            {
                try
                {
                    IceStorm::TopicManagerPrx manager = IceStorm::TopicManagerPrx::uncheckedCast(
                        communicator()->propertyToProxy(p->first));
                    managers.insert(map<Ice::Identity, IceStorm::TopicManagerPrx>::value_type(
                                        manager->ice_getIdentity(), manager));
                }
                catch(const Ice::ProxyParseException&)
                {
                    cerr << appName() << ": malformed proxy: " << p->second << endl;
                    return EXIT_FAILURE;
                }
            }
        }
        if(props.empty() && !defaultManager)
        {
            cerr << appName() << ": no manager proxies configured" << endl;
            return EXIT_FAILURE;
        }

        if(!defaultManager)
        {
            string managerProxy = properties->getProperty("IceStormAdmin.TopicManager.Default");
            if(!managerProxy.empty())
            {
                defaultManager = IceStorm::TopicManagerPrx::uncheckedCast(
                    communicator()->stringToProxy(managerProxy));
            }
            else
            {
                defaultManager = managers.begin()->second;
            }
        }
    }

    // Check slice checksums for each manager.
    {
        for(map<Ice::Identity, IceStorm::TopicManagerPrx>::const_iterator p = managers.begin(); p != managers.end();
            ++p)
        {
            try
            {
                Ice::SliceChecksumDict serverChecksums = p->second->getSliceChecksums();
                Ice::SliceChecksumDict localChecksums = Ice::sliceChecksums();
                for(Ice::SliceChecksumDict::const_iterator q = localChecksums.begin(); q != localChecksums.end(); ++q)
                {
                    Ice::SliceChecksumDict::const_iterator r = serverChecksums.find(q->first);
                    if(r == serverChecksums.end())
                    {
                        cerr << appName() << ": " << communicator()->identityToString(p->first)
                             << " is using unknown Slice type `" << q->first << "'" << endl;
                    }
                    else if(q->second != r->second)
                    {
                        cerr << appName() << ": " << communicator()->identityToString(p->first)
                             << " is using a different Slice definition of `" << q->first << "'" << endl;
                    }
                }
            }
            catch(const Ice::Exception& ex)
            {
                cerr << communicator()->identityToString(p->first) << ": " << ex << endl;
            }
        }
    }
        
    ParserPtr p = Parser::createParser(communicator(), defaultManager, managers);
    int status = EXIT_SUCCESS;

    if(!commands.empty()) // Commands were given
    {
        int parseStatus = p->parse(commands, debug);
        if(parseStatus == EXIT_FAILURE)
        {
            status = EXIT_FAILURE;
        }
    }
    else // No commands, let's use standard input
    {
        p->showBanner();

        int parseStatus = p->parse(stdin, debug);
        if(parseStatus == EXIT_FAILURE)
        {
            status = EXIT_FAILURE;
        }
    }

    return status;
}
