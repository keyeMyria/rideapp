package com.yrek.rideapp.config;

import java.util.HashMap;
import java.util.Properties;

import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.yrek.rideapp.facebook.FacebookOAuth2Client;
import com.yrek.rideapp.facebook.FacebookOAuth2Session;
import com.yrek.rideapp.oauth2.OAuth2Client;
import com.yrek.rideapp.oauth2.OAuth2Session;
import com.yrek.rideapp.storage.JCacheStorage;
import com.yrek.rideapp.storage.JDOStorage;
import com.yrek.rideapp.storage.Storage;

class LocalAppEngineModule extends BaseModule {
    LocalAppEngineModule(Properties properties) {
        super(properties);
    }

    @Override
    protected void defineBindings() {
        bind(OAuth2Client.class).to(FacebookOAuth2Client.class);
        bind(OAuth2Session.class).to(FacebookOAuth2Session.class);
        bind(Storage.class).to(JCacheStorage.class);
    }

    @Provides @Singleton
    JCacheStorage provideJCacheStorage() throws Exception {
        return new JCacheStorage(new JDOStorage());
    }
}
