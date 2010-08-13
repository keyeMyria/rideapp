package com.yrek.rideapp.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;

/**
 * Facebook returns text/javascript rather than application/json.
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json", "text/javascript"})
@Produces({MediaType.APPLICATION_JSON, "text/json", "text/javascript"})
public class JaxbJsonProvider extends JacksonJaxbJsonProvider {
    @Override
    protected boolean isJsonType(MediaType mediaType) {
        if (mediaType != null && "javascript".equals(mediaType.getSubtype()))
            return true;
        return super.isJsonType(mediaType);
    }
}
