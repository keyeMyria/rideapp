package com.yrek.rideapp.config;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

public class Configuration extends GuiceServletContextListener {
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    private BaseModule module = null;

    @Override
    protected Injector getInjector() {
        Properties properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("rideapp.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            module = Class.forName(properties.getProperty("module.class")).asSubclass(BaseModule.class).getDeclaredConstructor(Properties.class).newInstance(properties);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Injector injector = Guice.createInjector(module);
        // Stupid Guice bugs 455/522
        com.yrek.rideapp.servlet.UserServlet.objectMapper = injector.getInstance(org.codehaus.jackson.map.ObjectMapper.class);
        com.yrek.rideapp.servlet.UserServlet.restAPI = injector.getInstance(com.yrek.rideapp.rest.RESTAPI.class);
        com.yrek.rideapp.servlet.UserServlet.db = injector.getInstance(com.yrek.rideapp.storage.DB.class);
        return injector;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        if (module != null)
            module.close();
        super.contextDestroyed(servletContextEvent);
    }
}
