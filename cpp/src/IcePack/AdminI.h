// **********************************************************************
//
// Copyright (c) 2001
// MutableRealms, Inc.
// Huntsville, AL, USA
//
// All Rights Reserved
//
// **********************************************************************

#ifndef ADMIN_I_H
#define ADMIN_I_H

#include <IcePack/Admin.h>
#include <map>

class AdminI : public IcePack::Admin, public JTCMutex
{
public:

    AdminI(const Ice::CommunicatorPtr&);

    virtual void add(const IcePack::ServerDescriptionPtr&);
    virtual void remove(const Ice::ObjectPrx&);
    virtual IcePack::ServerDescriptionPtr find(const Ice::ObjectPrx&);
    virtual void shutdown();

private:

    Ice::CommunicatorPtr _communicator;
    std::map<Ice::ObjectPrx, IcePack::ServerDescriptionPtr> _map;
};

#endif
