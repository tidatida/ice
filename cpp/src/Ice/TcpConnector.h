// **********************************************************************
//
// Copyright (c) 2003-2007 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#ifndef ICE_TCP_CONNECTOR_H
#define ICE_TCP_CONNECTOR_H

#include <Ice/TransceiverF.h>
#include <Ice/InstanceF.h>
#include <Ice/TraceLevelsF.h>
#include <Ice/LoggerF.h>
#include <Ice/Connector.h>

#ifdef _WIN32
#   include <winsock2.h>
#else
#   include <netinet/in.h> // For struct sockaddr_in
#endif

namespace IceInternal
{

class TcpConnector : public Connector
{
public:
    
    virtual TransceiverPtr connect(int);
    virtual Ice::Short type() const;
    virtual std::string toString() const;

    virtual bool operator==(const Connector&) const;
    virtual bool operator!=(const Connector&) const;
    virtual bool operator<(const Connector&) const;

    bool equivalent(const std::string&, int) const;
    
private:
    
    TcpConnector(const InstancePtr&, const struct sockaddr_in&, Ice::Int, const std::string&);
    virtual ~TcpConnector();
    friend class TcpEndpointI;

    const InstancePtr _instance;
    const TraceLevelsPtr _traceLevels;
    const ::Ice::LoggerPtr _logger;
    struct sockaddr_in _addr;
    const Ice::Int _timeout;
    const std::string _connectionId;
};

}

#endif
