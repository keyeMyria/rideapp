package com.yrek.rideapp.config;

import java.util.Properties;

import com.yrek.rideapp.facebook.FacebookOAuth2Client;
import com.yrek.rideapp.facebook.FacebookOAuth2Session;
import com.yrek.rideapp.oauth2.OAuth2Client;
import com.yrek.rideapp.oauth2.OAuth2Session;
import com.yrek.rideapp.storage.MemcachedStorage;
import com.yrek.rideapp.storage.Storage;

class LocalS3Module extends BaseModule {
    LocalS3Module(Properties properties) {
        super(properties);
    }

    @Override
    protected void defineBindings() {
        bind(OAuth2Client.class).to(FacebookOAuth2Client.class);
        bind(OAuth2Session.class).to(FacebookOAuth2Session.class);
        bind(Storage.class).to(MemcachedStorage.class);
    }
}
