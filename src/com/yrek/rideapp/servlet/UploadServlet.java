package com.yrek.rideapp.servlet;

import java.io.StringWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ContentDisposition;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

@Singleton
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 0L;
    private static final Logger LOG = Logger.getLogger(UploadServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uploadStatus = "ok";
        try {
            StringWriter sw = new StringWriter();
            InputStreamReader in = new InputStreamReader(getPart("file", request.getHeader("Content-Type"), request.getInputStream()));
            char[] buffer = new char[8192];
            int count;
            while ((count = in.read(buffer)) >= 0)
                sw.write(buffer, 0, count);
            LOG.fine("file="+sw);
        } catch (Exception e) {
            LOG.log(Level.SEVERE,"",e);
            uploadStatus = "error";
        }
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/uploadStatus.jsp?uploadStatus=" + uploadStatus));
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
