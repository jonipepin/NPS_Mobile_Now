/** EmailChecker 
 *
 *  Author: 		Joni Pepin, jpepin@nps.edu
 *  Date:			22 Dec 2011
 *  Description: 	Provides ability to connect to user's email account
 *  				and check to see if they've received a muster confirmation
 *  				email for today.  		
 *
 *  Library Requirements:
 * 		Javamail - http://www.oracle.com/technetwork/java/index-138643.html
 *
 *  Reference: http://www.oracle.com/technetwork/java/sslnotes-150073.txt
 */

package com.pepinonline.mc.muster;

import android.content.Context;

import com.pepinonline.mc.Logger;
import com.sun.mail.util.MailSSLSocketFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.AndTerm;
import javax.mail.search.FromStringTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SentDateTerm;
import javax.mail.search.SubjectTerm;

/*
 * References: http://www.oracle.com/technetwork/java/sslnotes-150073.txt
 * @author Pepin
 */
public class EmailChecker {

    public Date musterDate;
    private Context context;
    private String username;
    private String password;

    public EmailChecker(Context context, String user, String pwd) {
        this.username = user;
        this.password = pwd;
        this.context = context;
    }

    public int musterCheck(String emailFolder) {

        // return 0 = mustered, 1 = not mustered, 2 = auth error, 3 = other error
        boolean mustered = false;
        try {
            Session session = createNewSession();
            //session.setDebug(true);
            Store store = getSessionStore(session);
            connectToStore(store, username, password);

            if (emailFolder != null) {
                Folder f = store.getFolder(emailFolder);
                mustered = searchFolder(f);
            } else {
                Folder[] allFolders = getAllFolders(store);
                saveEmailFolderNamesFile(folderArrayToStringArray(allFolders));
                for (Folder f : allFolders) {
                    if (mustered = searchFolder(f)) {
                        break;
                    }
                }
            }
            disconnectFromStore(store);

        } catch (javax.mail.AuthenticationFailedException ae) {
            Logger.e(ae.toString());
            return 2;
        } catch (MessagingException me) {
            Logger.e(me.toString());
            return 3;
        } catch (Exception e) {
            Logger.e(e.toString());
            return 3;
        }

        if (mustered) {
            return 0;
        }
        return 1;
    }

    /**
     * Using Array of Folder Objects, create String Array of folder names.
     *
     * @param folders
     * @return
     */
    private String[] folderArrayToStringArray(Folder[] folders) {
        String[] folderNames = null;
        folderNames = new String[folders.length];
        try {
            for (int i = 0; i < folders.length; i++) {
                if (folders[i].getType() != 0) {
                    folderNames[i] = folders[i].toString();
                }
            }
        } catch (Exception e) {
            Logger.e("Error converting folder list to string; ");
        }
        return folderNames;
    }

    /**
     * Retrieve String array of folder names
     *
     * @return
     */
    public String[] getEmailFolderNames() {
        String[] folderNames = null;
        try {
            Session session = createNewSession();
            Store store = getSessionStore(session);
            connectToStore(store, username, password);
            Folder[] folders = getAllFolders(store);
            folderNames = folderArrayToStringArray(folders);
            disconnectFromStore(store);
        } catch (Exception e) {
            Logger.e(e.toString());
        }
        return folderNames;
    }

    /**
     * Save list of folder names to file on device
     *
     * @param folders String array of folder names
     */
    private void saveEmailFolderNamesFile(String[] folders) {
        if (folders != null) {
            try {
                FileOutputStream fos = context.openFileOutput("EmailFolderNames", Context.MODE_PRIVATE);
                ObjectOutputStream oos = new ObjectOutputStream(fos);

                oos.writeObject(folders);
                oos.close();

                Logger.i("Email Folder List Saved");

            } catch (IOException e) {
                Logger.e(e.getMessage());
            }
        }
    }

    private Session createNewSession()
            throws GeneralSecurityException {
        // Set the socket factory to trust all hosts
        MailSSLSocketFactory sf = new MailSSLSocketFactory();
        sf.setTrustAllHosts(true);

        // create the properties for the Session
        Properties props = new Properties();
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.imap.ssl.socketFactory", sf);

        // Get session instance
        return Session.getInstance(props, null);
    }

    private Store getSessionStore(Session s)
            throws NoSuchProviderException {
        // Get the sessions message store
        return s.getStore("imap");
    }

    private void connectToStore(Store s, String username, String password)
            throws MessagingException, javax.mail.AuthenticationFailedException {
        Logger.i("Establishing connection with IMAP server.");
        s.connect("smtp.nps.edu", 993, username, password);
        Logger.i("Connection established with IMAP server.");
    }

    private void disconnectFromStore(Store s)
            throws MessagingException {
        s.close();
        Logger.i("Connection closed");
    }

    /**
     * Get array of all folders in user's email account
     *
     * @param s Email store
     * @return Array of Folder objects
     * @throws MessagingException
     */
    private Folder[] getAllFolders(Store s)
            throws MessagingException {
        return s.getDefaultFolder().list("*");
    }

    /**
     * Search for muster confirmation email dated today
     *
     * @param f Folder to be searched
     * @return true if found, false otherwise
     * @throws MessagingException
     */
    private boolean searchFolder(Folder f)
            throws MessagingException {
        if ((f.getType() & Folder.HOLDS_MESSAGES) != 0) {

            Logger.i("Checking inside " + f.getFullName());

            f.open(Folder.READ_ONLY);

            SearchTerm[] terms = {
                    new SubjectTerm("Muster Confirmation Email for:"),
                    new OrTerm(new FromStringTerm("sso@nps.edu"),
                            new FromStringTerm("NPS DOSS Officers")),
                    new SentDateTerm(3, new Date()), // 3 means EQ

            };
            SearchTerm st = new AndTerm(terms);

            Message[] message = f.search(st);

            for (int i = 0, n = message.length; i < n; i++) {
                if (message[i].getAllRecipients()[0].toString().contains(username)) {
                    Logger.i(message[i].getSentDate().toString());
                    musterDate = message[i].getSentDate();
                    Logger.i("Muster email found");
                    f.close(false);
                    return true;
                }
            }
            f.close(false);
        }
        Logger.i("Muster email not found");
        return false;
    }

}