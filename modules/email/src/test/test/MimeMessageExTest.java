package test;

import junit.framework.TestCase;

import javax.mail.Session;

import dalma.endpoints.email.MimeMessageEx;

public class MimeMessageExTest extends TestCase {
    Session session = Session.getInstance(System.getProperties());

    public void testMainContent1() throws Exception {
        // mail1.txt contains multipart/signed
        MimeMessageEx msg = new MimeMessageEx(session, getClass().getResourceAsStream("mail1.txt"));
        String mc = msg.getMainContent();
        assertTrue(mc.startsWith("Christian Ullenboom wrote:"));
        assertTrue(mc.endsWith("Sun Microsystems                   kohsuke.kawaguchi@sun.com\r\n"));
    }

    public void testMainContent2() throws Exception {
        // mail2.txt contains multipart/report
        MimeMessageEx msg = new MimeMessageEx(session, getClass().getResourceAsStream("mail2.txt"));
        String mc = msg.getMainContent();
        assertTrue(mc.startsWith("This is the Postfix program at"));
        assertTrue(mc.endsWith("User unknown (in reply to RCPT TO\r\n    command)\r\n"));
    }

    public void testMainContent3() throws Exception {
        // mail3.txt contains multipart/mixed and multipart/signed
        MimeMessageEx msg = new MimeMessageEx(session, getClass().getResourceAsStream("mail3.txt"));
        String mc = msg.getMainContent();
        System.out.println(mc);
        assertEquals("this is the main part\r\n",mc);
    }
}
