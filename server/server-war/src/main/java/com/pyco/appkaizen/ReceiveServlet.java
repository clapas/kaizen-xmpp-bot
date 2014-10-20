package com.pyco.appkaizen;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;

import javax.servlet.http.*;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;

public class ReceiveServlet extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        XMPPService xmpp = XMPPServiceFactory.getXMPPService();
        Message message = xmpp.parseMessage(req);

        JID fromJid = message.getFromJid();
        JID toJid = message.getRecipientJids()[0];
        String body = message.getBody();
        
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLogger");
        logger.log(java.util.logging.Level.WARNING, "Querying jid " + toJid.toString());

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query("Socket").addFilter("jid", Query.FilterOperator.EQUAL, toJid.getId());
        Entity socket = datastore.prepare(query).asSingleEntity();
        String socket_io = (String) socket.getProperty("socket_io");

        URL url = new URL("http://floating-chamber-5424.herokuapp.com/answer?socket.io=" + socket_io + "&message=" + URLEncoder.encode(body, "UTF-8"));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        int status = connection.getResponseCode();
        connection.disconnect();

        logger.log(java.util.logging.Level.WARNING, "message from " + fromJid.getId() + " to " + toJid.getId() + ": " + body + ". Status: " + status);

    }
}
