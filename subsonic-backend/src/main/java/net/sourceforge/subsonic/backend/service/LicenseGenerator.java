package net.sourceforge.subsonic.backend.service;

import net.sourceforge.subsonic.backend.dao.PaymentDao;
import net.sourceforge.subsonic.backend.domain.Payment;
import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs a task at regular intervals, checking for incoming donations and sending
 * out license keys by email.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class LicenseGenerator {

    private static final Logger LOG = Logger.getLogger(LicenseGenerator.class);
    private static final long DELAY = 60; // One minute.

    private PaymentDao paymentDao;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public void init() {
        Runnable task = new Runnable() {
            public void run() {
                try {
                    LOG.info("Starting license generator.");
                    processPayments();
                    LOG.info("Completed license generator.");
                } catch (Throwable x) {
                    LOG.error("Failed to process license emails.", x);
                }
            }
        };
        executor.scheduleWithFixedDelay(task, DELAY, DELAY, TimeUnit.SECONDS);
        LOG.info("Scheduled license generator to run every " + DELAY + " seconds.");
    }

    private void processPayments() throws Exception {
        List<Payment> payments = paymentDao.getPaymentsByProcessingStatus(Payment.ProcessingStatus.NEW);
        LOG.info(payments.size() + " new payment(s).");
        if (payments.isEmpty()) {
            return;
        }

        EmailSession emailSession = new EmailSession();
        for (Payment payment : payments) {
            processPayment(payment, emailSession);
        }
    }

    private void processPayment(Payment payment, EmailSession emailSession) {
        try {
            LOG.info("Processing " + payment);
            String email = payment.getPayerEmail();
            if (email == null) {
                throw new Exception("Missing email address.");
            }

            boolean eligible = isEligible(payment);
            boolean ignorable = isIgnorable(payment);
            if (eligible) {
                sendLicenseTo(email, emailSession);
                LOG.info("Sent license key for " + payment);
            } else {
                LOG.info("Payment not eligible for " + payment);
            }

            if (eligible || ignorable) {
                payment.setProcessingStatus(Payment.ProcessingStatus.COMPLETED);
                payment.setLastUpdated(new Date());
                paymentDao.updatePayment(payment);
            }

        } catch (Throwable x) {
            LOG.error("Failed to process " + payment, x);
        }
    }

    private boolean isEligible(Payment payment) {
        String status = payment.getPaymentStatus();
        if ("echeck".equalsIgnoreCase(payment.getPaymentType())) {
            return "Pending".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status);
        }
        return "Completed".equalsIgnoreCase(status);
    }

    private boolean isIgnorable(Payment payment) {
        String status = payment.getPaymentStatus();
        return "Denied".equalsIgnoreCase(status) || 
                "Reversed".equalsIgnoreCase(status) ||
                "Refunded".equalsIgnoreCase(status);
    }

    public void sendLicenseTo(String to, EmailSession emailSession) throws MessagingException {
        emailSession.sendMessage("subsonic_donation@activeobjects.no",
                                 Arrays.asList(to),
                                 null,
                                 Arrays.asList("subsonic_donation@activeobjects.no", "sindre@activeobjects.no"),
                                 Arrays.asList("subsonic_donation@activeobjects.no"),
                                 "Subsonic License",
                                 createLicenseContent(to));
        LOG.info("Sent license to " + to);
    }

    private String createLicenseContent(String to) {
        String license = md5Hex(to.toLowerCase());

        return "Dear Subsonic donor,\n" +
                "\n" +
                "Many thanks for your kind donation to Subsonic!\n" +
                "Please find your license key below.\n" +
                "\n" +
                "Email: " + to + "\n" +
                "License: " + license + " \n" +
                "\n" +
                "To install the license key, click the \"Donate\" link in the top right corner of the Subsonic web interface.\n" +
                "\n" +
                "More info here: http://subsonic.org/pages/getting-started.jsp#3\n" +
                "\n" +
                "This license is valid for personal, non-commercial of Subsonic. For commercial use, please contact us for licensing options.\n" +
                "\n" +
                "Thanks again for supporting the project!\n" +
                "\n" +
                "Best regards,\n" +
                "The Subsonic team";
    }

    /**
     * Calculates the MD5 digest and returns the value as a 32 character hex string.
     *
     * @param s Data to digest.
     * @return MD5 digest as a hex string.
     */
    private String md5Hex(String s) {
        if (s == null) {
            return null;
        }

        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            return new String(Hex.encodeHex(md5.digest(s.getBytes("UTF-8"))));
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage(), x);
        }
    }

    public void setPaymentDao(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
    }

    public static void main(String[] args) throws Exception {
        String address = args[0];
//        String license = md5Hex(address.toLowerCase());
//        System.out.println("Email: " + address);
//        System.out.println("License: " + license);

        LicenseGenerator generator = new LicenseGenerator();
        generator.sendLicenseTo(address, new EmailSession());       
    }
}
