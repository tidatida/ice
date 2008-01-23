// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

using System;

namespace IceBox
{

public class Server : Ice.Application
{
    private static void usage()
    {
        Console.Error.WriteLine("Usage: iceboxnet [options] --Ice.Config=<file>\n");
        Console.Error.WriteLine(
            "Options:\n" +
            "-h, --help           Show this message.\n"
        );
    }

    public override int run(string[] args)
    {
        for(int i = 0; i < args.Length; ++i)
        {
            if(args[i].Equals("-h") || args[i].Equals("--help"))
            {
                usage();
                return 0;
            }
            else if(!args[i].StartsWith("--"))
            {
                Console.Error.WriteLine("Server: unknown option `" + args[i] + "'");
                usage();
                return 1;
            }
        }

        ServiceManagerI serviceManagerImpl = new ServiceManagerI(args);
        return serviceManagerImpl.run();
    }

    public static void Main(string[] args)
    {
        Ice.InitializationData initData = new Ice.InitializationData();
        initData.properties = Ice.Util.createProperties();
        initData.properties.setProperty("Ice.Admin.DelayCreation", "1");

        Server server = new Server();
        int status = server.main(args, initData);
        if(status != 0)
        {
            System.Environment.Exit(status);
        }
    }
}
}
