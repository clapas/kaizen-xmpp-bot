package com.pyco.appkaizen;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.appengine.api.xmpp.PresenceType;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.EntityNotFoundException;

public class DisconnectServlet extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String socket_io = req.getParameter("socket.io");
    String user_id = req.getParameter("user_id");

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    Key appkaizenKey = KeyFactory.createKey("AppKaizen", user_id);
    Query query = new Query("Subscriber", appkaizenKey);
    List<Entity> subscribers = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10));
    if (subscribers.isEmpty()) return;

    //Key socketKey = KeyFactory.createKey("Socket", user_id);
    //query = new Query("Socket", socketKey).addFilter("socket_io", FilterOperator.EQUAL, socket_io);
    query = new Query("Socket").addFilter("socket_io", FilterOperator.EQUAL, socket_io);
    List<Entity> sockets = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
    if (sockets.isEmpty()) return;
    String from = (String) sockets.get(0).getProperty("jid");
    JID from_jid = new JID(from);

    Key client_key = (Key) sockets.get(0).getProperty("client");
    List<Entity> clients = null;
    if (client_key != null) {
        clients = new ArrayList<Entity>();
        try {
            clients.add(datastore.get(client_key));
        } catch (EntityNotFoundException e) {}
    } else {
        query = new Query("Operator", subscribers.get(0).getKey());
        List<Entity> operators = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10));
        if (operators.isEmpty()) return;

        Key sessionKey = KeyFactory.createKey("Session", "default");
        String to;
        for (Entity o: operators) {
            to = (String) o.getProperty("email");
            query = new Query("Client", sessionKey).addFilter("jid_nr", FilterOperator.EQUAL, to);
            if (clients == null) clients = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
            else clients.addAll(datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3)));
        }
        if (clients.isEmpty()) return;
    }
    datastore.delete(sockets.get(0).getKey());

    XMPPService xmppService = XMPPServiceFactory.getXMPPService();
    String to;
    for (Entity c: clients) {
        to = (String) c.getProperty("jid");
        JID to_jid = new JID(to);
        xmppService.sendPresence(to_jid, PresenceType.UNAVAILABLE, null, null, from_jid);
    }
  }
}
