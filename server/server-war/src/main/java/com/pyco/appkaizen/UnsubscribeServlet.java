package com.pyco.appkaizen;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.xmpp.Subscription;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UnsubscribeServlet extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
  {
    XMPPService xmppService = XMPPServiceFactory.getXMPPService();
    Subscription subscription = xmppService.parseSubscription(req);
    String to = subscription.getToJid().getId();
    
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    
    Query query = new Query("Socket").addFilter("jid", Query.FilterOperator.EQUAL, to);
    List<Entity> clients = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
    if (clients.isEmpty()) {
      return;
    }
    datastore.delete(clients.get(0).getKey());
  }
}
