# **********************************************************************
#
# Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

!if "$(ICE_HOME)" == ""
ICE_DIR		= $(top_srcdir)\..
USE_SRC_DIR	= 1
!else
ICE_DIR 	= $(ICE_HOME)
!endif

#
# Select an installation base directory. The directory will be created
# if it does not exist.
#

prefix			= C:\IcePy-$(VERSION)

#
# Define OPTIMIZE as yes if you want to build with optimization.
# Otherwise the Ice extension is built with debug information.
#

OPTIMIZE		= yes

#
# Specify your C++ compiler. Supported values are:
# VC60, VC71, VC80, VC80_EXPRESS
#
!if "$(CPP_COMPILER)" == ""
CPP_COMPILER            = VC80
!endif

#
# Set PYTHON_HOME to your Python installation directory.
#
!if "$(PYTHON_HOME)" == ""
PYTHON_HOME		= C:\Python24
!endif

#
# STLPort is required if using MSVC++ 6.0. Change if STLPort
# is located in a different location.
#
!if "$(CPP_COMPILER)" == "VC60" && "$(STLPORT_HOME)" == ""
STLPORT_HOME            = C:\Ice-$(VERSION)-ThirdParty-VC60
!endif

#
# For VC80 and VC80 Express it is necessary to set the location of the
# manifest tool. This must be the 6.x version of mt.exe, not the 5.x
# version!
#
# For VC80 Express mt.exe 6.x is provided by the Windows Platform SDK. 
# It is necessary to set the location of the Platform SDK through the
# PDK_HOME environment variable (see INSTALL for details).
#
!if "$(CPP_COMPILER)" == "VC80"
MT = "$(VS80COMNTOOLS)bin\mt.exe"
!elseif "$(CPP_COMPILER)" == "VC80_EXPRESS"
MT = "$(PDK_HOME)\bin\mt.exe"
!endif

# ----------------------------------------------------------------------
# Don't change anything below this line!
# ----------------------------------------------------------------------

SHELL			= /bin/sh
VERSION			= 3.3.0
SOVERSION		= 33
libdir			= $(top_srcdir)\python

install_slicedir	= $(prefix)\slice
install_pythondir	= $(prefix)\python

THIRDPARTY_HOME		= $(STLPORT_HOME)

!if "$(CPP_COMPILER)" != "VC60" && "$(CPP_COMPILER)" != "VC71" && \
    "$(CPP_COMPILER)" != "VC80" && "$(CPP_COMPILER)" != "VC80_EXPRESS"
!error Invalid setting for CPP_COMPILER: $(CPP_COMPILER)
!endif

!include $(top_srcdir)\..\cpp\config\Make.rules.msvc

!if "$(OPTIMIZE)" != "yes"
LIBSUFFIX       = $(LIBSUFFIX)d
PYLIBSUFFIX     = _$(LIBSUFFIX)
!endif

ICE_LIBS		= ice$(LIBSUFFIX).lib iceutil$(LIBSUFFIX).lib slice$(LIBSUFFIX).lib

!if "$(USE_SRC_DIR)" == "1"
ICE_CPPFLAGS		= -I"$(ICE_DIR)\cpp\include"
ICE_LDFLAGS		= /LIBPATH:"$(ICE_DIR)\cpp\lib"
!else
ICE_CPPFLAGS		= -I"$(ICE_DIR)\include"
ICE_LDFLAGS		= /LIBPATH:"$(ICE_DIR)\lib"
!endif

slicedir                = $(top_srcdir)\..\slice

PYTHON_CPPFLAGS		= -I"$(PYTHON_HOME)\include"
PYTHON_LDFLAGS		= /LIBPATH:"$(PYTHON_HOME)\libs"

install_libdir		= $(prefix)\python
libsubdir		= lib

ICECPPFLAGS		= -I"$(slicedir)"
SLICE2PYFLAGS		= $(ICECPPFLAGS)

!if "$(USE_SRC_DIR)" == "1"
SLICE2PY		= "$(ICE_DIR)\cpp\bin\slice2py.exe"
!else
SLICE2PY		= "$(ICE_DIR)\bin\slice2py.exe"
!endif

EVERYTHING		= all clean install

.SUFFIXES:
.SUFFIXES:		.cpp .obj .py

all:: $(SRCS)

.cpp.obj::
	$(CXX) /c $(CPPFLAGS) $(CXXFLAGS) $<

clean::
	del /q $(TARGETS) *.obj *.pyc *.bak

all:: $(SRCS) $(TARGETS)

install::
