package com.yrek.rideapp.rest;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ContentDisposition;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.inject.Inject;

import com.yrek.rideapp.facebook.User;
import com.yrek.rideapp.facebook.Users;

@Path("/")
public class RESTAPI {
    private static final Logger LOG = Logger.getLogger(RESTAPI.class.getName());

    @GET @Path("/friends") @Produces({MediaType.APPLICATION_XML,MediaType.APPLICATION_JSON})
    public User[] friends(@Context HttpServletRequest request) {
        return ((Users) request.getSession().getAttribute("friends")).getData();
    }

    @POST @Path("/upload") @Consumes(MediaType.MULTIPART_FORM_DATA)
    public void upload(@Context HttpServletRequest request, @HeaderParam("Content-Type") String contentType, InputStream upload) throws Exception {
        StringWriter sw = new StringWriter();
        InputStreamReader in = new InputStreamReader(getPart("file", contentType, upload));
        char[] buffer = new char[8192];
        int count;
        while ((count = in.read(buffer)) >= 0)
            sw.write(buffer, 0, count);
        LOG.fine("file="+sw);
    }

    private InputStream getPart(String name, final String contentType, final InputStream inputStream) throws Exception {
        MimeMultipart mimeMultipart = new MimeMultipart(new DataSource() {
            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            @Override
            public OutputStream getOutputStream() {
                return null;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public String getName() {
                return null;
            }
        });
        for (int i = 0; i < mimeMultipart.getCount(); i++) {
            ContentDisposition contentDisposition = new ContentDisposition(mimeMultipart.getBodyPart(i).getHeader("Content-Disposition")[0]);
            LOG.fine("i="+i+",contentDisposition="+contentDisposition);
            if (name.equals(contentDisposition.getParameter("name")))
                return mimeMultipart.getBodyPart(i).getInputStream();
        }
        return null;
    }
}
