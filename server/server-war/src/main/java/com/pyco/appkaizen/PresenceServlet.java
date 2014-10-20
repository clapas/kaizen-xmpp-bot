package com.pyco.appkaizen;

import java.io.IOException;
import java.util.Properties;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.MessageType;
import com.google.appengine.api.xmpp.Presence;
import com.google.appengine.api.xmpp.PresenceType;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query.FilterOperator;

public class PresenceServlet extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // In the handler for _ah/xmpp/presence/available
        XMPPService xmppService = XMPPServiceFactory.getXMPPService();
        Presence presence = xmppService.parsePresence(req);
                
        JID jid = presence.getFromJid();
        String rsrc = jid.getId().split("/")[1];
        if (rsrc.indexOf("appkaizen") != 0) return;
        
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key sessionKey = KeyFactory.createKey("Session", "default");
        Query query = new Query("Client", sessionKey).addFilter("jid", FilterOperator.EQUAL, jid.getId());
        List<Entity> clients  = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLogger");
        logger.log(java.util.logging.Level.WARNING, "available: from " + jid.getId() + " to " + presence.getToJid().getId());

        String to = presence.getToJid().getId().split("/")[0];
        if (presence.getPresenceType() == PresenceType.UNAVAILABLE) {
            if (to.equals("appkaizen@appspot.com") && !clients.isEmpty())
                datastore.delete(clients.get(0).getKey());
        }
        if (presence.getPresenceType() == PresenceType.AVAILABLE && clients.isEmpty()) {
            Entity client = new Entity("Client", sessionKey);
            client.setProperty("jid", jid.getId());
            client.setProperty("jid_nr", jid.getId().split("/")[0]);
            datastore.put(client);
        }
    }
}
