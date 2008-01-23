// **********************************************************************
//
// Copyright (c) 2003-2008 ZeroC, Inc. All rights reserved.
//
// This copy of Ice is licensed to you under the terms described in the
// ICE_LICENSE file included in this distribution.
//
// **********************************************************************

package Ice;

public final class PluginManagerI implements PluginManager
{
    private static String _kindOfObject = "plugin";

    public synchronized void
    initializePlugins()
    {
        if(_initialized)
        {
            InitializationException ex = new InitializationException();
            ex.reason = "plugins already initialized";
            throw ex;
        }

        //
        // Invoke initialize() on the plugins, in the order they were loaded.
        //
        java.util.ArrayList initializedPlugins = new java.util.ArrayList();
        try
        {
            java.util.Iterator i = _initOrder.iterator();
            while(i.hasNext())
            {
                Plugin p = (Plugin)i.next();
                p.initialize();
                initializedPlugins.add(p);
            }
        }
        catch(RuntimeException ex)
        {
            //
            // Destroy the plugins that have been successfully initialized, in the
            // reverse order.
            //
            java.util.ListIterator i = initializedPlugins.listIterator(initializedPlugins.size());
            while(i.hasPrevious())
            {
                Plugin p = (Plugin)i.previous();
                try
                {
                    p.destroy();
                }
                catch(RuntimeException e)
                {
                    // Ignore.
                }
            }
            throw ex;
        }

        _initialized = true;
    }

    public synchronized Plugin
    getPlugin(String name)
    {
        if(_communicator == null)
        {
            throw new CommunicatorDestroyedException();
        }

        Plugin p = (Plugin)_plugins.get(name);
        if(p != null)
        {
            return p;
        }
        NotRegisteredException ex = new NotRegisteredException();
        ex.id = name;
        ex.kindOfObject = _kindOfObject;
        throw ex;
    }

    public synchronized void
    addPlugin(String name, Plugin plugin)
    {
        if(_communicator == null)
        {
            throw new CommunicatorDestroyedException();
        }

        if(_plugins.containsKey(name))
        {
            AlreadyRegisteredException ex = new AlreadyRegisteredException();
            ex.id = name;
            ex.kindOfObject = _kindOfObject;
            throw ex;
        }
        _plugins.put(name, plugin);
    }

    public synchronized void
    destroy()
    {
        if(_communicator != null)
        {
            java.util.Iterator i = _plugins.values().iterator();
            while(i.hasNext())
            {
                Plugin p = (Plugin)i.next();
                p.destroy();
            }

            _logger = null;
            _communicator = null;
        }
    }

    public
    PluginManagerI(Communicator communicator)
    {
        _communicator = communicator;
        _initialized = false;
    }

    public void
    loadPlugins(StringSeqHolder cmdArgs)
    {
        assert(_communicator != null);

        //
        // Load and initialize the plug-ins defined in the property set
        // with the prefix "Ice.Plugin.". These properties should
        // have the following format:
        //
        // Ice.Plugin.name[.<language>]=entry_point [args]
        //
        // If the Ice.PluginLoadOrder property is defined, load the
        // specified plugins in the specified order, then load any
        // remaining plugins.
        //
        final String prefix = "Ice.Plugin.";
        Properties properties = _communicator.getProperties();
        java.util.Map plugins = properties.getPropertiesForPrefix(prefix);

        final String loadOrder = properties.getProperty("Ice.PluginLoadOrder");
        if(loadOrder.length() > 0)
        {
            String[] names = loadOrder.split("[, \t\n]+");
            for(int i = 0; i < names.length; ++i)
            {
                if(_plugins.containsKey(names[i]))
                {
                    PluginInitializationException ex = new PluginInitializationException();
                    ex.reason = "plugin `" + names[i] + "' already loaded";
                    throw ex;
                }

                String key = "Ice.Plugin." + names[i] + ".java";
                boolean hasKey = plugins.containsKey(key);
                if(hasKey)
                {
                    plugins.remove("Ice.Plugin." + names[i]);
                }
                else
                {
                    key = "Ice.Plugin." + names[i];
                    hasKey = plugins.containsKey(key);
                }
                
                if(hasKey)
                {
                    final String value = (String)plugins.get(key);
                    loadPlugin(names[i], value, cmdArgs);
                    plugins.remove(key);
                }
                else
                {
                    PluginInitializationException ex = new PluginInitializationException();
                    ex.reason = "plugin `" + names[i] + "' not defined";
                    throw ex;
                }
            }
        }

        //
        // Load any remaining plugins that weren't specified in PluginLoadOrder.
        //
        while(!plugins.isEmpty())
        {
            java.util.Iterator p = plugins.entrySet().iterator();
            java.util.Map.Entry entry = (java.util.Map.Entry)p.next();

            String name = ((String)entry.getKey()).substring(prefix.length());

            int dotPos = name.lastIndexOf('.');
            if(dotPos != -1)
            {
                String suffix = name.substring(dotPos + 1);
                if(suffix.equals("cpp") || suffix.equals("clr"))
                {
                    //
                    // Ignored
                    //
                    p.remove();
                }
                else if(suffix.equals("java"))
                {
                    name = name.substring(0, dotPos);
                    String value = (String)entry.getValue();
                    loadPlugin(name, value, cmdArgs);
                    p.remove();
                }
                else
                {
                    //
                    // Name is just a regular name that happens to contain a dot
                    //
                    dotPos = -1;
                }
            }
            
            if(dotPos == -1)
            {
                //
                // Is there a .java entry?
                //
                String value = (String)entry.getValue();
                p.remove();

                String javaValue = (String)plugins.remove("Ice.Plugin." + name + ".java");
                if(javaValue != null)
                {
                    value = javaValue;
                }
                
                loadPlugin(name, value, cmdArgs);
            }
        }

        //
        // An application can set Ice.InitPlugins=0 if it wants to postpone
        // initialization until after it has interacted directly with the
        // plugins.
        //
        if(properties.getPropertyAsIntWithDefault("Ice.InitPlugins", 1) > 0)
        {
            initializePlugins();
        }
    }

    private void
    loadPlugin(String name, String pluginSpec, StringSeqHolder cmdArgs)
    {
        assert(_communicator != null);

        //
        // Separate the entry point from the arguments.
        //
        String className;
        String[] args;
        int pos = pluginSpec.indexOf(' ');
        if(pos == -1)
        {
            pos = pluginSpec.indexOf('\t');
        }
        if(pos == -1)
        {
            pos = pluginSpec.indexOf('\n');
        }
        if(pos == -1)
        {
            className = pluginSpec;
            args = new String[0];
        }
        else
        {
            className = pluginSpec.substring(0, pos);
            args = pluginSpec.substring(pos).trim().split("[ \t\n]+", pos);
        }

        //
        // Convert command-line options into properties. First we
        // convert the options from the plug-in configuration, then
        // we convert the options from the application command-line.
        //
        Properties properties = _communicator.getProperties();
        args = properties.parseCommandLineOptions(name, args);
        cmdArgs.value = properties.parseCommandLineOptions(name, cmdArgs.value);

        //
        // Instantiate the class.
        //
        PluginFactory pluginFactory = null;
        try
        {
            Class c = Class.forName(className);
            java.lang.Object obj = c.newInstance();
            try
            {
                pluginFactory = (PluginFactory)obj;
            }
            catch(ClassCastException ex)
            {
                PluginInitializationException e = new PluginInitializationException();
                e.reason = "class " + className + " does not implement Ice.PluginFactory";
                e.initCause(ex);
                throw e;
            }
        }
        catch(ClassNotFoundException ex)
        {
            PluginInitializationException e = new PluginInitializationException();
            e.reason = "class " + className + " not found";
            e.initCause(ex);
            throw e;
        }
        catch(IllegalAccessException ex)
        {
            PluginInitializationException e = new PluginInitializationException();
            e.reason = "unable to access default constructor in class " + className;
            e.initCause(ex);
            throw e;
        }
        catch(InstantiationException ex)
        {
            PluginInitializationException e = new PluginInitializationException();
            e.reason = "unable to instantiate class " + className;
            e.initCause(ex);
            throw e;
        }

        //
        // Invoke the factory.
        //
        Plugin plugin = null;
        try
        {
            plugin = pluginFactory.create(_communicator, name, args);
        }
        catch(PluginInitializationException ex)
        {
            throw ex;
        }
        catch(Throwable ex)
        {
            PluginInitializationException e = new PluginInitializationException();
            e.reason = "exception in factory " + className;
            e.initCause(ex);
            throw e;
        }

        if(plugin == null)
        {
            PluginInitializationException e = new PluginInitializationException();
            e.reason = "failure in factory " + className;
            throw e;
        }

        if(name.equals("Logger"))
        {
            try
            {
                LoggerPlugin loggerPlugin = (LoggerPlugin)plugin;
                _logger = loggerPlugin.getLogger();
            }
            catch(ClassCastException ex)
            {
                PluginInitializationException e = new PluginInitializationException();
                e.reason = "Ice.Plugin.Logger does not implement an Ice.LoggerPlugin";
                e.initCause(ex);
                throw e;
            }
        }

        _plugins.put(name, plugin);
        _initOrder.add(plugin);
    }

    public Logger
    getLogger()
    {
        return _logger;
    }

    private Communicator _communicator;
    private java.util.HashMap _plugins = new java.util.HashMap();
    private java.util.ArrayList _initOrder = new java.util.ArrayList();
    private Logger _logger = null;
    private boolean _initialized;
}
