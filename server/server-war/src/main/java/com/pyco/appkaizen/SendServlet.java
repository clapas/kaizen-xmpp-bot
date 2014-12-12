package com.pyco.appkaizen;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.net.HttpURLConnection;

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
//import java.lang.StringBuilder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.xmpp.JID;
import com.google.appengine.api.xmpp.Message;
import com.google.appengine.api.xmpp.MessageBuilder;
import com.google.appengine.api.xmpp.XMPPService;
import com.google.appengine.api.xmpp.XMPPServiceFactory;
import com.google.appengine.api.xmpp.MessageType;
import com.google.appengine.api.xmpp.SendResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.FetchOptions;

public class SendServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String subsc_ak_id;
    if ((subsc_ak_id = req.getParameter("create_subscriber_ak_id")) != null) {
        Key appkaizenKey = KeyFactory.createKey("AppKaizen", subsc_ak_id);
        Entity subscriber = new Entity("Subscriber", appkaizenKey);
        subscriber.setProperty("ak_id", subsc_ak_id);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(subscriber);
        Entity operator = new Entity("Operator", subscriber.getKey());
        operator.setProperty("email", "claudio.pascual@gmail.com");
        datastore.put(operator);
        /*
        operator = new Entity("Operator", subscriber.getKey());
        operator.setProperty("email", "rupertenecesito@gmail.com");
        datastore.put(operator);
        operator = new Entity("Operator", subscriber.getKey());
        operator.setProperty("email", "laovejitanegra@gmail.com");
        datastore.put(operator);
        */
    }
    if ((subsc_ak_id = req.getParameter("subscriber_ak_id")) != null) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key appkaizenKey = KeyFactory.createKey("AppKaizen", subsc_ak_id);
        // Run an ancestor query to ensure we see the most up-to-date view of the Greetings belonging to the selected Guestbook.
        Query query = new Query("Subscriber", appkaizenKey);//.addSort("date", Query.SortDirection.DESCENDING);
        List<Entity> subscribers = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(5));
        resp.setContentType("text/plain");
        resp.getWriter().println("subscribers is empty? " + subscribers.isEmpty());
        for (Entity subscriber: subscribers) {
            resp.getWriter().println(subscriber.getProperty("ak_id") + ": " + subscriber.getProperty("ak_id"));
            resp.getWriter().println(("id") + ": " + subscriber.getKey().getId());
            resp.getWriter().println("Los chikillos:");
            query = new Query("Operator", subscriber.getKey());//.addSort("date", Query.SortDirection.DESCENDING);
            List<Entity> operators = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(5));
            for (Entity operator: operators) {
                resp.getWriter().println("email: " + operator.getProperty("email"));
            }
        }
        return;
    }
    if (req.getParameter("testing") == null) {
      resp.setContentType("text/plain");
      resp.getWriter().println("Hello, this is a testing servlet. \n\n");
      Properties p = System.getProperties();
      p.list(resp.getWriter());

    } else {
      UserService userService = UserServiceFactory.getUserService();
      User currentUser = userService.getCurrentUser();

      if (currentUser != null) {
        resp.setContentType("text/plain");
        resp.getWriter().println("Hello, " + currentUser.getNickname());
      } else {
        resp.sendRedirect(userService.createLoginURL(req.getRequestURI()));
      }
    }
  }
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws IOException {
    String msg = req.getParameter("message");
    String user_id = req.getParameter("user_id");
    String socket_io = req.getParameter("socket.io");
    String from;

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    Key appkaizenKey = KeyFactory.createKey("AppKaizen", user_id);
    Query query = new Query("Subscriber", appkaizenKey);
    List<Entity> subscribers = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10));
    if (subscribers.isEmpty()) return;

    query = new Query("Operator", subscribers.get(0).getKey());
    List<Entity> operators = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10));
    if (operators.isEmpty()) return;

    Key sessionKey = KeyFactory.createKey("Session", "default");
    List<Entity> clients = null;
    String to;
    for (Entity o: operators) {
        to = (String) o.getProperty("email");
        query = new Query("Client", sessionKey).addFilter("jid_nr", FilterOperator.EQUAL, to);
        if (clients == null) clients = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3));
        else clients.addAll(datastore.prepare(query).asList(FetchOptions.Builder.withLimit(3)));
    }
    if (clients.isEmpty()) {
        resp.setStatus(503);
        return;
    }
    if (msg == null || msg.isEmpty()) return; // used to test for available clients

    //Key socketKey = KeyFactory.createKey("Socket", user_id);
    //query = new Query("Socket", socketKey).addFilter("socket_io", FilterOperator.EQUAL, socket_io);
    query = new Query("Socket").addFilter("socket_io", FilterOperator.EQUAL, socket_io);
    List<Entity> sockets = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
    JID from_jid;

    List<JID> to_jids = new ArrayList<JID>();
    for (Entity c: clients) {
        to = (String) c.getProperty("jid");
        JID to_jid = new JID(to);
        to_jids.add(to_jid);
    }

    Properties props = new Properties();
    InputStream input = null;

    String chatdom;
    try {
        input = getServletContext().getResourceAsStream("/WEB-INF/config.properties");
        props.load(input);
        chatdom = props.getProperty("bot_chat_domain");
    } catch (Exception e) {
        return;
    }
    XMPPService xmppService = XMPPServiceFactory.getXMPPService();
    if (sockets.isEmpty()) {
        RandomString rs = new RandomString(12);
        from = rs.nextString() + "@" + chatdom;
        from_jid = new JID(from);
        //Entity socket = new Entity("Socket", socketKey);
        Entity socket = new Entity("Socket");
        socket.setProperty("jid", from);
        socket.setProperty("socket_io", socket_io);
        socket.setProperty("client", (Key) null);
        datastore.put(socket);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLogger");
        for (JID to_jid: to_jids) { xmppService.sendInvitation(to_jid, from_jid);
        logger.log(java.util.logging.Level.WARNING, "send invitation to " + to_jid.getId());
        }
    } else {
        from = (String) sockets.get(0).getProperty("jid");
        from_jid = new JID(from);
    }
    
    Message reply = new MessageBuilder()
        .withRecipientJids(to_jids.toArray(new JID[to_jids.size()]))
        .withFromJid(from_jid)
        .withMessageType(MessageType.NORMAL)
        .withBody(msg)
        .build();

    SendResponse status = xmppService.sendMessage(reply);
    resp.getWriter().println(status.getStatusMap().get(to_jids.get(0)).name());
  }
}
