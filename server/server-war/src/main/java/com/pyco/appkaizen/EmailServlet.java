package com.pyco.appkaizen;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;

public class EmailServlet extends HttpServlet {
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = req.getParameter("message");
        String user_id = req.getParameter("user_id");
        String from = req.getParameter("from");

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("MyLogger");

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    
        Key appkaizenKey = KeyFactory.createKey("AppKaizen", user_id);
        Query query = new Query("Subscriber", appkaizenKey);
        List<Entity> subscribers = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(10));
        if (subscribers.isEmpty()) return;
    
        query = new Query("Operator", subscribers.get(0).getKey());
        Entity operator = datastore.prepare(query).asSingleEntity();
        if (operator == null) return;
    
        String to = (String) operator.getProperty("email");

        Properties props = new Properties();
        InputStream input = null;

        String sender;
        try {
            input = getServletContext().getResourceAsStream("/WEB-INF/config.properties");
            props.load(input);
            sender = props.getProperty("mail_sender");
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "real path is " + getServletContext().getRealPath("config.properties"));
            return;
        }

        props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        logger.log(java.util.logging.Level.WARNING, "sending email: from " + from + " to " + to);
        try {
            Message msg = new MimeMessage(session);
            msg.addHeader("Reply-To", from);
            msg.setFrom(new InternetAddress(sender));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            msg.setSubject("Offline message from AppKaizen");
            msg.setText(body);
            Transport.send(msg);
        } catch (Exception e) {
            logger.log(java.util.logging.Level.WARNING, "failed to send email: " + e.getMessage());
        }
    }
}

