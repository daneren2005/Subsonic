/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2010 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.backend.service;

import net.sourceforge.subsonic.backend.Util;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.AddressException;
import java.util.Properties;
import java.util.List;

/**
 * @author Sindre Mehus
 */
public class EmailSession {

    private static final String SMTP_MAIL_SERVER = "smtp.gmail.com";
    private static final String POP_MAIL_SERVER = "pop.gmail.com";
    private static final String IMAP_MAIL_SERVER = "imap.gmail.com";
    private static final String USER = "subsonic@activeobjects.no";

    private Session session;
    private String password;

    public EmailSession() throws Exception {
        Properties props = new Properties();
//        props.setProperty("mail.debug", "true");
        props.setProperty("mail.store.protocol", "pop3s");
        props.setProperty("mail.smtps.host", SMTP_MAIL_SERVER);
        props.setProperty("mail.smtps.auth", "true");
        props.setProperty("mail.smtps.timeout", "10000");
        props.setProperty("mail.smtps.connectiontimeout", "10000");
        props.setProperty("mail.pop3s.timeout", "10000");
        props.setProperty("mail.pop3s.connectiontimeout", "10000");

        session = Session.getDefaultInstance(props, null);
        password = Util.getPassword("gmailpwd.txt");
    }

    public void sendMessage(String from, List<String> to, List<String> cc, List<String> bcc, List<String> replyTo,
                            String subject, String text) throws MessagingException {
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(from));
        message.setReplyTo(new Address[]{new InternetAddress(from)});
        message.setRecipients(Message.RecipientType.TO, convertAddress(to));
        message.setRecipients(Message.RecipientType.CC, convertAddress(cc));
        message.setRecipients(Message.RecipientType.BCC, convertAddress(bcc));
        message.setReplyTo(convertAddress(replyTo));
        message.setSubject(subject);
        message.setText(text);

        // Send the message
        Transport transport = null;
        try {
            transport = session.getTransport("smtps");
            transport.connect(USER, password);
            transport.sendMessage(message, message.getAllRecipients());
        } finally {
            if (transport != null) {
                transport.close();
            }
        }
    }

    public Folder getFolder(String name) throws Exception {
        Store store = session.getStore("imaps");
        store.connect(IMAP_MAIL_SERVER, USER, password);
        Folder folder = store.getFolder(name);
        folder.open(Folder.READ_ONLY);
        return folder;
    }

    private Address[] convertAddress(List<String> addresses) throws AddressException {
        if (addresses == null) {
            return null;
        }
        Address[] result = new Address[addresses.size()];
        for (int i = 0; i < addresses.size(); i++) {
            result[i] = new InternetAddress(addresses.get(i));
        }
        return result;
    }
}
