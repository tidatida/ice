// **********************************************************************
//
// Copyright (c) 2001
// MutableRealms, Inc.
// Huntsville, AL, USA
//
// All Rights Reserved
//
// **********************************************************************

#ifndef ICE_PACK_ADMIN_ICE
#define ICE_PACK_ADMIN_ICE

/**
 *
 * The Ice object locator and activator.
 *
 **/
module IcePack
{

/**
 *
 * Data describing a server one or more objects implemented by that
 * server.
 *
 **/
class ServerDescription
{
    /**
     *
     * The server object or object template. Any non-administrative
     * IcePack request that matches the identity of
     * <literal>object</literal> will be forwarded to
     * <literal>object</literal>. If <literal>regex</literal> is set
     * to <literal>true</literal>, <literal>object</literal>'s
     * identity is interpreted as a regular expression, and each
     * request that matches this expression will be forwarded.
     *
     * @see regex
     *
     **/
    Object* object;

    /**
     *
     * If set to true, the identity contained in
     * <literal>object</literal> will be interpreted as regular
     * expression.
     *
     * @see object
     *
     **/
    bool regex;
    
    /**
     *
     * The optional server host name. If none is given, no automatic
     * activation will be performed.
     *
     * @see path
     *
     **/
    string host;

    /**
     *
     * The optional server path. If none is given, no automatic
     * activation will be performed.
     *
     * @see host
     *
     **/
    string path;
};

/**
 *
 * The IcePack administrative interface. <warning><para>Allowing
 * access to this interface is a security risk! Please see the IcePack
 * documentation for further information.</para></warning>
 *
 **/
class Admin
{
    /**
     *
     * Add a server and objects implemented by that server to IcePack.
     *
     * @param description The server's description.
     *
     * @see remove
     *
     **/
    void add(ServerDescription description);

    /**
     *
     * Remove a server and objects implemented by that server from IcePack.
     *
     * @param object Must match the field <literal>object</literal> of
     * the <literal>ServerDescription</literal> data to remove.
     *
     * @see add
     *
     **/
    void remove(Object* object);

    /**
     *
     * Find a server and objects implemented by that server from IcePack.
     *
     * @param object Must match the field <literal>object</literal> of
     * the <literal>ServerDescription</literal> data to find.
     *
     * @return The server description, or null if no description was found.
     *
     * @see add
     *
     **/
    ServerDescription find(Object* object);

    /**
     *
     * Shut down IcePack.
     *
     **/
    void shutdown();
};

};

#endif
