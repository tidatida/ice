# **********************************************************************
#
# Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

top_srcdir	= ..\..

NAME		= $(top_srcdir)\bin\slice2py.exe

TARGETS		= $(NAME)

OBJS		= Main.obj

SRCS		= $(OBJS:.obj=.cpp)

!include $(top_srcdir)/config/Make.rules.mak

CPPFLAGS	= -I. $(CPPFLAGS) -DWIN32_LEAN_AND_MEAN

!if "$(CPP_COMPILER)" != "BCC2006" && "$(OPTIMIZE)" != "yes"
PDBFLAGS        = /pdb:$(NAME:.exe=.pdb)
!endif

$(NAME): $(OBJS)
	$(LINK) $(LD_EXEFLAGS) $(PDBFLAGS) $(OBJS) $(SETARGV) $(PREOUT)$@ $(PRELIBS)slice$(LIBSUFFIX).lib $(BASELIBS)
	@if exist $@.manifest echo ^ ^ ^ Embedding manifest using $(MT) && \
	    $(MT) -nologo -manifest $@.manifest -outputresource:$@;#1 && del /q $@.manifest

clean::
	del /q $(NAME:.exe=.*)

install:: all
	copy $(NAME) $(install_bindir)

!if "$(OPTIMIZE)" != "yes"

!if "$(CPP_COMPILER)" == "BCC2006"

install:: all
	copy $(NAME:.exe=.tds) $(install_bindir)

!else

install:: all
	copy $(NAME:.exe=.pdb) $(install_bindir)

!endif

!endif

!include .depend
