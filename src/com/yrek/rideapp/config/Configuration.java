package com.yrek.rideapp.config;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;

import com.sun.jersey.api.container.filter.LoggingFilter;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import com.yrek.rideapp.facebook.FacebookClient;
import com.yrek.rideapp.facebook.FacebookOAuth2Client;
import com.yrek.rideapp.facebook.FacebookOAuth2Session;
import com.yrek.rideapp.oauth2.OAuth2Client;
import com.yrek.rideapp.oauth2.OAuth2Session;
import com.yrek.rideapp.oauth2.OAuth2.OAuth2Filter;
import com.yrek.rideapp.oauth2.OAuth2.OAuth2Servlet;
import com.yrek.rideapp.rest.RESTAuthFilter;
import com.yrek.rideapp.rest.RESTAPI;
import com.yrek.rideapp.servlet.PingServlet;
import com.yrek.rideapp.servlet.SetAttributesFilter;
import com.yrek.rideapp.servlet.UploadServlet;

public class Configuration extends GuiceServletContextListener {
    private static final Logger LOG = Logger.getLogger(Configuration.class.getName());

    private ArrayList<Closeable> closeables = new ArrayList<Closeable>();
    private Properties properties;

    @Override
    protected Injector getInjector() {
        properties = new Properties();
        try {
            properties.load(getClass().getClassLoader().getResourceAsStream("rideapp.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Guice.createInjector(new ServletModule() {
            private static final long serialVersionUID = 0L;

            @Override
            protected void configureServlets() {
                addBindings();
                bind(RESTAPI.class);

                serve("/oauth2").with(OAuth2Servlet.class);
                filter("*.jsp").through(OAuth2Filter.class);
                filter("*.jsp").through(SetAttributesFilter.class);
                filter("/rest/*").through(RESTAuthFilter.class);
                serve("/ping").with(PingServlet.class);
                serve("/rest/upload").with(UploadServlet.class);
                serve("/rest/*").with(GuiceContainer.class, jerseyParams());
            }

            private HashMap<String,String> jerseyParams() {
                HashMap<String,String> properties = new HashMap<String,String>();
                properties.put("com.sun.jersey.spi.container.ContainerRequestFilters", LoggingFilter.class.getName());
                properties.put("com.sun.jersey.spi.container.ContainerResponseFilters", LoggingFilter.class.getName());
                return properties;
            }

            private void addBindings() {
                bind(OAuth2Client.class).to(FacebookOAuth2Client.class);
                bind(OAuth2Session.class).to(FacebookOAuth2Session.class);
            }

            @Provides
            FacebookOAuth2Client provideFacebookOAuth2Client() {
                FacebookOAuth2Client facebookOAuth2Client = new FacebookOAuth2Client(properties.getProperty("facebook.clientID"), properties.getProperty("facebook.clientSecret"), properties.getProperty("facebook.canvasURL"));
                closeables.add(facebookOAuth2Client);
                return facebookOAuth2Client;
            }

            @Provides
            FacebookClient provideFacebookClient(OAuth2Session oAuth2Session) {
                FacebookClient facebookClient = new FacebookClient(oAuth2Session);
                closeables.add(facebookClient);
                return facebookClient;
            }

            @Provides @Singleton
            SetAttributesFilter provideSetAttributesFilter(FacebookClient facebookClient) {
                return new SetAttributesFilter(facebookClient, properties.getProperty("garmin.garminUnlock"));
            }
        });
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        for (Closeable closeable : closeables)
            try {
                closeable.close();
            } catch (Exception e) {
                LOG.log(Level.SEVERE,"",e);
            }
        super.contextDestroyed(servletContextEvent);
    }
}
