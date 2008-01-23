// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************
package IceGridGUI.LiveDeployment;

import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

import IceGrid.*;
import IceGridGUI.*;

class Service extends ListArrayTreeNode
{
    //
    // Actions
    //
    public boolean[] getAvailableActions()
    {
        boolean[] actions = new boolean[ACTION_COUNT];

        ServerState serverState = ((Server)_parent).getState();

        if(serverState != null)
        {
            actions[RETRIEVE_LOG] = _serviceDescriptor.logs.length > 0;
        }
        if(serverState == ServerState.Active)
        {
            if(((Server)_parent).hasServiceObserver())
            {
                actions[START] = !_started;
                actions[STOP] = _started;
            }
            else
            {
                actions[START] = true;
                actions[STOP] = true;
            }
        }
        
        return actions;
    }

    public void start()
    {
        Ice.ObjectPrx serverAdmin = ((Server)_parent).getServerAdmin();
        
        if(serverAdmin != null)
        {
            final String prefix = "Starting service '" + _id + "'...";
            getCoordinator().getStatusBar().setText(prefix);
            
            IceBox.AMI_ServiceManager_startService cb = new IceBox.AMI_ServiceManager_startService()
            {
                //
                // Called by another thread!
                //
                public void ice_response()
                {
                    amiSuccess(prefix);
                }
                
                public void ice_exception(Ice.UserException e)
                {
                    if(e instanceof IceBox.AlreadyStartedException)
                    {
                        amiSuccess(prefix);
                    }
                    else
                    {
                        amiFailure(prefix, "Failed to start service " + _id, e.toString());
                    }
                }

                public void ice_exception(Ice.LocalException e)
                {
                    amiFailure(prefix, "Failed to start service " + _id, e.toString());
                }
            };
            
            IceBox.ServiceManagerPrx serviceManager = IceBox.ServiceManagerPrxHelper.
                uncheckedCast(serverAdmin.ice_facet("IceBox.ServiceManager"));
        
            try
            {   
                serviceManager.startService_async(cb, _id);
            }
            catch(Ice.LocalException e)
            {
                failure(prefix, "Failed to start service " + _id, e.toString());
            }
        }
    }
    
    public void stop()
    {
        Ice.ObjectPrx serverAdmin = ((Server)_parent).getServerAdmin();
        
        if(serverAdmin != null)
        {
            final String prefix = "Stopping service '" + _id + "'...";
            getCoordinator().getStatusBar().setText(prefix);
            
            IceBox.AMI_ServiceManager_stopService cb = new IceBox.AMI_ServiceManager_stopService()
            {
                //
                // Called by another thread!
                //
                public void ice_response()
                {
                    amiSuccess(prefix);
                }
                
                public void ice_exception(Ice.UserException e)
                {
                    if(e instanceof IceBox.AlreadyStoppedException)
                    {
                        amiSuccess(prefix);
                    }
                    else
                    {
                        amiFailure(prefix, "Failed to stop service " + _id, e.toString());
                    }
                }

                public void ice_exception(Ice.LocalException e)
                {
                    amiFailure(prefix, "Failed to stop service " + _id, e.toString());
                }
            };
            
            IceBox.ServiceManagerPrx serviceManager = IceBox.ServiceManagerPrxHelper.
                uncheckedCast(serverAdmin.ice_facet("IceBox.ServiceManager"));
        
            try
            {   
                serviceManager.stopService_async(cb, _id);
            }
            catch(Ice.LocalException e)
            {
                failure(prefix, "Failed to stop service " + _id, e.toString());
            }
        }
    }


    public void retrieveLog()
    {
        assert _serviceDescriptor.logs.length > 0;

        String path = null;
        
        if(_serviceDescriptor.logs.length == 1)
        {
            path = _resolver.substitute(_serviceDescriptor.logs[0]);
        }
        else
        {
            Object[] pathArray = new Object[_serviceDescriptor.logs.length];
            for(int i = 0; i < _serviceDescriptor.logs.length; ++i)
            {
                pathArray[i] = _resolver.substitute(_serviceDescriptor.logs[i]);
            }
        
            path = (String)JOptionPane.showInputDialog(
                getCoordinator().getMainFrame(), 
                "Which log file do you want to retrieve?", 
                "Retrieve Log File",     
                JOptionPane.QUESTION_MESSAGE, null,
                pathArray, pathArray[0]);
        }
 
        if(path != null)
        {
            final String fPath = path;
          
            getRoot().openShowLogDialog(new ShowLogDialog.FileIteratorFactory()
                {
                    public FileIteratorPrx open(int count)
                        throws FileNotAvailableException, ServerNotExistException, NodeUnreachableException, 
                        DeploymentException
                    {
                        AdminSessionPrx session = getRoot().getCoordinator().getSession();
                        FileIteratorPrx result = session.openServerLog(_parent.getId(), fPath, count);
                        if(getRoot().getCoordinator().getCommunicator().getDefaultRouter() == null)
                        {
                            result = FileIteratorPrxHelper.uncheckedCast(
                                result.ice_endpoints(session.ice_getEndpoints()));
                        }
                        return result;
                    }
                    
                    public String getTitle()
                    {
                        return "Service " + _parent.getId() + "/" + _id + " " + new java.io.File(fPath).getName();
                    }
                    
                    public String getDefaultFilename()
                    {
                        return new java.io.File(fPath).getName();
                    }
                });
        }       
    }

    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) 
    {
        if(_cellRenderer == null)
        {
            _cellRenderer = new DefaultTreeCellRenderer();

            _startedIcon = Utils.getIcon("/icons/16x16/service_started.png");
            _stoppedIcon = Utils.getIcon("/icons/16x16/service.png");
        }
        
        Icon icon = _started ?  _startedIcon : _stoppedIcon;
        
        if(expanded)
        {
            _cellRenderer.setOpenIcon(icon);
        }
        else
        {
            _cellRenderer.setClosedIcon(icon);
        }


        return _cellRenderer.getTreeCellRendererComponent(
            tree, value, sel, expanded, leaf, row, hasFocus);
    }

    public Editor getEditor()
    {
        if(_editor == null)
        {
            _editor = new ServiceEditor(getCoordinator());
        }
        _editor.show(this);
        return _editor;
    }

    public JPopupMenu getPopupMenu()
    {
        LiveActions la = getCoordinator().getLiveActionsForPopup();

        if(_popup == null)
        {
            _popup = new JPopupMenu();
            _popup.add(la.get(START));
            _popup.add(la.get(STOP));
            _popup.addSeparator();
            _popup.add(la.get(RETRIEVE_LOG));
        }
        
        la.setTarget(this);
        return _popup;
    }


    Service(Server parent, String serviceName, Utils.Resolver resolver,
            ServiceInstanceDescriptor descriptor, 
            ServiceDescriptor serviceDescriptor,
            PropertySetDescriptor serverInstancePSDescriptor)
    {
        super(parent, serviceName, 2);
        _resolver = resolver;
        
        _instanceDescriptor = descriptor;
        _serviceDescriptor = serviceDescriptor;
        _serverInstancePSDescriptor = serverInstancePSDescriptor;
        
        _childrenArray[0] = _adapters;
        _childrenArray[1] = _dbEnvs;

        createAdapters();
        createDbEnvs();
    }

    boolean updateAdapter(AdapterDynamicInfo info)
    {
        java.util.Iterator p = _adapters.iterator();
        while(p.hasNext())
        {
            Adapter adapter = (Adapter)p.next();
            if(adapter.update(info))
            {
                return true;
            }
        }
        return false;
    }

    int updateAdapters(java.util.List infoList)
    {
        int result = 0;
        java.util.Iterator p = _adapters.iterator();
        while(p.hasNext() && result < infoList.size())
        {
            Adapter adapter = (Adapter)p.next();
            if(adapter.update(infoList))
            {
                result++;
            }
        }
        return result;
    }

    void nodeDown()
    {
        java.util.Iterator p = _adapters.iterator();
        while(p.hasNext())
        {
            Adapter adapter = (Adapter)p.next();
            adapter.update((AdapterDynamicInfo)null);
        }
    }

    boolean isStarted()
    {
        return _started;
    }

    void started()
    {
        if(!_started)
        {
            _started = true;
            getRoot().getTreeModel().nodeChanged(this);
        }
    }

    void stopped()
    {
        if(_started)
        {
            _started = false;
            getRoot().getTreeModel().nodeChanged(this);
        }
    }

    
    void showRuntimeProperties()
    {
        Ice.ObjectPrx serverAdmin = ((Server)_parent).getServerAdmin();
            
        if(serverAdmin == null)
        {
            _editor.setBuildId("", this);
        }
        else
        {
            Ice.AMI_PropertiesAdmin_getPropertiesForPrefix cb = new Ice.AMI_PropertiesAdmin_getPropertiesForPrefix()
                {
                    public void ice_response(final java.util.Map properties)
                    {
                        SwingUtilities.invokeLater(new Runnable() 
                            {
                                public void run() 
                                {
                                    _editor.setRuntimeProperties((java.util.SortedMap)properties, Service.this);
                                }
                            });
                    }
                
                    public void ice_exception(final Ice.LocalException e)
                    {
                        SwingUtilities.invokeLater(new Runnable() 
                            {
                                public void run() 
                                {
                                    if(e instanceof Ice.ObjectNotExistException)
                                    {
                                        _editor.setBuildId("Error: can't reach the icebox Admin object", Service.this);
                                    }
                                    else if(e instanceof Ice.FacetNotExistException)
                                    {
                                        _editor.setBuildId("Error: this icebox Admin object does not provide a 'Properties' facet for this service", 
                                                           Service.this);
                                    }
                                    else
                                    {
                                        _editor.setBuildId("Error: " + e.toString(), Service.this);
                                    }
                                }
                            });
                    }
                };


            try
            {    
                Ice.PropertiesAdminPrx propAdmin = Ice.PropertiesAdminPrxHelper.uncheckedCast(serverAdmin.ice_facet("IceBox.Service." 
                                                                                                                    + _id + ".Properties"));
                propAdmin.getPropertiesForPrefix_async(cb, "");
            }
            catch(Ice.LocalException e)
            {
                _editor.setBuildId("Error: " + e.toString(), this);
            }
        }
    }

    Utils.Resolver getResolver()
    {
        return _resolver;
    }

    ServiceDescriptor getServiceDescriptor()
    {
        return _serviceDescriptor;
    }

    ServiceInstanceDescriptor getInstanceDescriptor()
    {
        return _instanceDescriptor;
    }

    java.util.SortedMap getProperties()
    {
        java.util.List psList = new java.util.LinkedList();
        Node node = (Node)_parent.getParent();

        String applicationName = ((Server)_parent).getApplication().name;

        psList.add(node.expand(_serviceDescriptor.propertySet,
                               applicationName, _resolver));

        if(_instanceDescriptor != null)
        {
            psList.add(node.expand(_instanceDescriptor.propertySet, 
                                   applicationName, _resolver));
        }          

        if(_serverInstancePSDescriptor != null)
        {
            psList.add(node.expand(_serverInstancePSDescriptor, 
                                   applicationName, _resolver));

        }

        return Utils.propertySetsToMap(psList, _resolver);
    }

    private void createAdapters()
    {
        java.util.Iterator p = _serviceDescriptor.adapters.iterator();
        while(p.hasNext())
        {
            AdapterDescriptor descriptor = (AdapterDescriptor)p.next();
            String adapterName = Utils.substitute(descriptor.name, _resolver);
            
            String adapterId = Utils.substitute(descriptor.id, _resolver);
            Ice.ObjectPrx proxy = null;
            if(adapterId.length() > 0)
            {
                proxy = ((Node)_parent.getParent()).getProxy(adapterId);
            }
            
            insertSortedChild(
                new Adapter(this, adapterName, 
                            _resolver, adapterId, descriptor, proxy),
                _adapters, null);
        }
    }
    
    private void createDbEnvs()
    {
        java.util.Iterator p = _serviceDescriptor.dbEnvs.iterator();
        while(p.hasNext())
        {
            DbEnvDescriptor descriptor = (DbEnvDescriptor)p.next();
            String dbEnvName = Utils.substitute(descriptor.name, _resolver);
            insertSortedChild(
                new DbEnv(this, dbEnvName, _resolver, descriptor), _dbEnvs, null);
        }
    }    

    private final ServiceInstanceDescriptor _instanceDescriptor;
    private final ServiceDescriptor _serviceDescriptor;
    private final PropertySetDescriptor _serverInstancePSDescriptor;
    private final Utils.Resolver _resolver;

    private final java.util.List _adapters = new java.util.LinkedList();
    private final java.util.List _dbEnvs = new java.util.LinkedList();

    private boolean _started = false;

    static private ServiceEditor _editor;
    static private DefaultTreeCellRenderer _cellRenderer;
    static private JPopupMenu _popup;
    static private Icon _startedIcon;
    static private Icon _stoppedIcon;
}
