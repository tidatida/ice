// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package IceInternal;

class PropertiesAdminI extends Ice._PropertiesAdminDisp
{
    PropertiesAdminI(Ice.Properties properties)
    {
        _properties = properties;
    }
    
    public String
    getProperty(String name, Ice.Current current)
    {
        return _properties.getProperty(name);
    }
    
    public java.util.TreeMap
    getPropertiesForPrefix(String name, Ice.Current current)
    {
        return new java.util.TreeMap(_properties.getPropertiesForPrefix(name));
    }
    
    private final Ice.Properties _properties;
}
