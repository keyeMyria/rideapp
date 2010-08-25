package com.yrek.rideapp.servlet;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.activation.DataSource;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ContentDisposition;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.yrek.rideapp.facebook.User;
import com.yrek.rideapp.storage.DB;

@Singleton
public class UploadServlet extends HttpServlet {
    private static final long serialVersionUID = 0L;
    private static final Logger LOG = Logger.getLogger(UploadServlet.class.getName());

    private static final Pattern TIMESTAMP = Pattern.compile("[0-9]{4}-?[0-9]{2}-?[0-9]{2}T[0-9]{2}:?[0-9]{2}:?[0-9]{2}([.][0-9]{3})?Z");

    @Inject private DB db;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String uploadStatus = "error";
        boolean isFile = true;
        try {
            isFile = request.getHeader("Content-Type").startsWith("multipart/form-data");
            InputStream in = request.getInputStream();
            if (isFile)
                in = getPart("file", request.getHeader("Content-Type"), in);
            User user = (User) request.getSession().getAttribute("user");
            if (user == null)
                uploadStatus = "sessionexpired";
            else
                uploadStatus = doUpload(in, user.getId());
        } catch (Exception e) {
            LOG.log(Level.SEVERE,"",e);
            uploadStatus = "error";
        }
        if (isFile) {
            response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/uploadStatus.jsp?uploadStatus=" + uploadStatus));
        } else {
            response.setContentType("text/plain");
            response.getWriter().write(uploadStatus);
        }
    }

    private String doUpload(InputStream in, String userId) throws Exception {
        final int maxTrackPoints = db.getMaxTrackPoints(userId);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(bytes);
        final String[] status = { "ok" };
        out.print("[");
        SAXParserFactory.newInstance().newSAXParser().parse(in, new DefaultHandler() {
            private int points = 0;
            private int trkpt = 0;
            private int time = 0;
            private String lat = null;
            private String lon = null;
            private StringBuilder timeContent = new StringBuilder();

            @Override
            public void characters(char[] ch, int start, int length) {
                if (trkpt == 1 && time == 1)
                    for (int i = 0; i < length; i++)
                        timeContent.append(ch[i + start]);
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) {
                if ("time".equals(qName)) {
                    time++;
                    timeContent.setLength(0);
                } else if ("trkpt".equals(qName)) {
                    trkpt++;
                    lat = attributes.getValue("lat");
                    lon = attributes.getValue("lon");
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if ("time".equals(qName)) {
                    time--;
                } else if ("trkpt".equals(qName)) {
                    trkpt--;
                    if (validTime(timeContent) && validDouble(lat) && validDouble(lon)) {
                        points++;
                        if (points > maxTrackPoints) {
                            status[0] = "tracktruncated";
                        } else {
                            if (points > 1)
                                out.print(",");
                            out.print("{\"lat\":"+lat+",\"lon\":"+lon+",\"t\":\""+timeContent+"\"}");
                        }
                    }
                }
                assert time >= 0;
                assert trkpt >= 0;
            }
        });
        out.print("]");
        out.flush();
        byte[] data = bytes.toByteArray();
        if (data.length <= 2)
            return "nodata";
        db.addTrack(userId, data);
        return status[0];
    }

    private boolean validTime(StringBuilder timeContent) {
        return TIMESTAMP.matcher(timeContent).matches();
    }

    private boolean validDouble(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (Exception e) {
            return false;
        }
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
