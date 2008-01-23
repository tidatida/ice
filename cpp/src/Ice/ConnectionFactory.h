// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

#ifndef ICE_CONNECTION_FACTORY_H
#define ICE_CONNECTION_FACTORY_H

#include <IceUtil/Mutex.h>
#include <IceUtil/Monitor.h>
#include <IceUtil/Thread.h> // For ThreadPerIncomingConnectionFactory.
#include <Ice/ConnectionFactoryF.h>
#include <Ice/ConnectionI.h>
#include <Ice/InstanceF.h>
#include <Ice/ObjectAdapterF.h>
#include <Ice/EndpointIF.h>
#include <Ice/Endpoint.h>
#include <Ice/ConnectorF.h>
#include <Ice/AcceptorF.h>
#include <Ice/TransceiverF.h>
#include <Ice/RouterInfoF.h>
#include <Ice/EventHandler.h>
#include <Ice/EndpointI.h>
#include <list>
#include <set>

namespace Ice
{

class LocalException;
class ObjectAdapterI;

}

namespace IceInternal
{

class OutgoingConnectionFactory : virtual public IceUtil::Shared, public IceUtil::Monitor<IceUtil::Mutex>
{
public:

    class CreateConnectionCallback : virtual public IceUtil::Shared
    {
    public:
        
        virtual void setConnection(const Ice::ConnectionIPtr&, bool) = 0;
        virtual void setException(const Ice::LocalException&) = 0;
    };
    typedef IceUtil::Handle<CreateConnectionCallback> CreateConnectionCallbackPtr; 

    void destroy();

    void waitUntilFinished();

    Ice::ConnectionIPtr create(const std::vector<EndpointIPtr>&, bool, bool, Ice::EndpointSelectionType, bool&);
    void create(const std::vector<EndpointIPtr>&, bool, bool, Ice::EndpointSelectionType, 
                const CreateConnectionCallbackPtr&);
    void setRouterInfo(const RouterInfoPtr&);
    void removeAdapter(const Ice::ObjectAdapterPtr&);
    void flushBatchRequests();

private:

    OutgoingConnectionFactory(const InstancePtr&);
    virtual ~OutgoingConnectionFactory();
    friend class Instance;

    struct ConnectorInfo
    {
        ConnectorInfo(const ConnectorPtr& c, const EndpointIPtr& e, bool t) :
            connector(c), endpoint(e), threadPerConnection(t)
        {
        }

        bool operator<(const ConnectorInfo& other) const;

        ConnectorPtr connector;
        EndpointIPtr endpoint;
        bool threadPerConnection;
    };

    class ConnectCallback : public Ice::ConnectionI::StartCallback, public IceInternal::EndpointI_connectors
    {
    public:

        ConnectCallback(const OutgoingConnectionFactoryPtr&, const std::vector<EndpointIPtr>&, bool, 
                        const CreateConnectionCallbackPtr&, Ice::EndpointSelectionType, bool);

        virtual void connectionStartCompleted(const Ice::ConnectionIPtr&);
        virtual void connectionStartFailed(const Ice::ConnectionIPtr&, const Ice::LocalException&);

        virtual void connectors(const std::vector<ConnectorPtr>&);
        virtual void exception(const Ice::LocalException&);

        void getConnectors();
        void nextEndpoint();

        void getConnection();
        void nextConnector();

        bool operator<(const ConnectCallback&) const;
        
    private:

        const OutgoingConnectionFactoryPtr _factory;
        const SelectorThreadPtr _selectorThread;
        const std::vector<EndpointIPtr> _endpoints;
        const bool _hasMore;
        const CreateConnectionCallbackPtr _callback;
        const Ice::EndpointSelectionType _selType;
        const bool _threadPerConnection;
        std::vector<EndpointIPtr>::const_iterator _endpointsIter;
        std::vector<ConnectorInfo> _connectors;
        std::vector<ConnectorInfo>::const_iterator _iter;
    };
    typedef IceUtil::Handle<ConnectCallback> ConnectCallbackPtr;
    friend class ConnectCallback;

    std::vector<EndpointIPtr> applyOverrides(const std::vector<EndpointIPtr>&);
    Ice::ConnectionIPtr findConnection(const std::vector<EndpointIPtr>&, bool, bool&);
    void incPendingConnectCount();
    void decPendingConnectCount();
    Ice::ConnectionIPtr getConnection(const std::vector<ConnectorInfo>&, const ConnectCallbackPtr&, bool&);
    void finishGetConnection(const std::vector<ConnectorInfo>&, const ConnectCallbackPtr&, const Ice::ConnectionIPtr&);
    Ice::ConnectionIPtr findConnection(const std::vector<ConnectorInfo>&, bool&);
    Ice::ConnectionIPtr createConnection(const TransceiverPtr&, const ConnectorInfo&);

    void handleException(const Ice::LocalException&, bool);
    void handleException(const Ice::LocalException&, const ConnectorInfo&, const Ice::ConnectionIPtr&, bool);

    const InstancePtr _instance;
    bool _destroyed;

    std::multimap<ConnectorInfo, Ice::ConnectionIPtr> _connections;
    std::map<ConnectorInfo, std::set<ConnectCallbackPtr> > _pending;

    std::multimap<EndpointIPtr, Ice::ConnectionIPtr> _connectionsByEndpoint;
    int _pendingConnectCount;
};

class IncomingConnectionFactory : public EventHandler, 
                                  public Ice::ConnectionI::StartCallback,
                                  public IceUtil::Monitor<IceUtil::Mutex>
                                      
{
public:

    void activate();
    void hold();
    void destroy();

    void waitUntilHolding() const;
    void waitUntilFinished();

    EndpointIPtr endpoint() const;
    std::list<Ice::ConnectionIPtr> connections() const;
    void flushBatchRequests();

    //
    // Operations from EventHandler
    //
    virtual bool datagram() const;
    virtual bool readable() const;
    virtual bool read(BasicStream&);
    virtual void message(BasicStream&, const ThreadPoolPtr&);
    virtual void finished(const ThreadPoolPtr&);
    virtual void exception(const Ice::LocalException&);
    virtual std::string toString() const;

    virtual void connectionStartCompleted(const Ice::ConnectionIPtr&);
    virtual void connectionStartFailed(const Ice::ConnectionIPtr&, const Ice::LocalException&);
    
private:

    IncomingConnectionFactory(const InstancePtr&, const EndpointIPtr&, const Ice::ObjectAdapterPtr&,
                              const std::string&);
    virtual ~IncomingConnectionFactory();
    friend class Ice::ObjectAdapterI;

    enum State
    {
        StateActive,
        StateHolding,
        StateClosed
    };

    void setState(State);
    void registerWithPool();
    void unregisterWithPool();

    void run();

    class ThreadPerIncomingConnectionFactory : public IceUtil::Thread
    {
    public:
        
        ThreadPerIncomingConnectionFactory(const IncomingConnectionFactoryPtr&);
        virtual void run();

    private:
        
        IncomingConnectionFactoryPtr _factory;
    };
    friend class ThreadPerIncomingConnectionFactory;
    IceUtil::ThreadPtr _threadPerIncomingConnectionFactory;

    AcceptorPtr _acceptor;
    const TransceiverPtr _transceiver;
    const EndpointIPtr _endpoint;

    Ice::ObjectAdapterPtr _adapter;

    bool _registeredWithPool;
    int _finishedCount;

    const bool _warn;

    std::list<Ice::ConnectionIPtr> _connections;

    State _state;

    bool _threadPerConnection;
    size_t _threadPerConnectionStackSize;
};

}

#endif
