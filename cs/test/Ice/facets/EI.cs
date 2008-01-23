// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

using Test;

public sealed class EI : EDisp_
{
    public EI()
    {
    }
    
    public override string callE(Ice.Current current)
    {
        return "E";
    }
}
