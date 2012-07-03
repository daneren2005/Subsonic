package net.sourceforge.subsonic.backend.dao;

import net.sourceforge.subsonic.backend.domain.Payment;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;
import org.springframework.jdbc.core.simple.ParameterizedSingleColumnRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Provides database services for PayPal payments.
 *
 * @author Sindre Mehus
 */
public class PaymentDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(PaymentDao.class);
    private static final String COLUMNS = "id, transaction_id, transaction_type, item, " +
                                          "payment_type, payment_status, payment_amount, payment_currency, " +
                                          "payer_email, payer_email_lower, payer_first_name, payer_last_name, payer_country, " +
                                          "processing_status, created, last_updated";

    private RowMapper paymentRowMapper = new PaymentRowMapper();
    private RowMapper listRowMapper = new ParameterizedSingleColumnRowMapper<Integer>();

    /**
     * Returns the payment with the given transaction ID.
     *
     * @param transactionId The transaction ID.
     * @return The payment or <code>null</code> if not found.
     */
    public Payment getPaymentByTransactionId(String transactionId) {
        String sql = "select " + COLUMNS + " from payment where transaction_id=?";
        return queryOne(sql, paymentRowMapper, transactionId);
    }

    /**
     * Returns the payment with the given payer email.
     *
     * @param email The payer email.
     * @return The payment or <code>null</code> if not found.
     */
    public Payment getPaymentByEmail(String email) {
        if (email == null) {
            return null;
        }
        String sql = "select " + COLUMNS + " from payment where payer_email_lower=?";
        return queryOne(sql, paymentRowMapper, email.toLowerCase());
    }

    /**
     * Returns all payments with the given processing status.
     *
     * @param status The status.
     * @return List of payments.
     */
    public List<Payment> getPaymentsByProcessingStatus(Payment.ProcessingStatus status) {
        return query("select " + COLUMNS + " from payment where processing_status=?", paymentRowMapper, status.name());
    }

    /**
     * Creates a new payment.
     *
     * @param payment The payment to create.
     */
    public void createPayment(Payment payment) {
        String sql = "insert into payment (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")";
        update(sql, null, payment.getTransactionId(), payment.getTransactionType(), payment.getItem(),
               payment.getPaymentType(), payment.getPaymentStatus(), payment.getPaymentAmount(),
               payment.getPaymentCurrency(), payment.getPayerEmail(), StringUtils.lowerCase(payment.getPayerEmail()),
                payment.getPayerFirstName(), payment.getPayerLastName(), payment.getPayerCountry(),
                payment.getProcessingStatus().name(), payment.getCreated(), payment.getLastUpdated());
        LOG.info("Created " + payment);
    }

    /**
     * Updates the given payment.
     *
     * @param payment The payment to update.
     */
    public void updatePayment(Payment payment) {
        String sql = "update payment set transaction_type=?, item=?, payment_type=?, payment_status=?, " +
                     "payment_amount=?, payment_currency=?, payer_email=?, payer_email_lower=?, payer_first_name=?, payer_last_name=?, " +
                     "payer_country=?, processing_status=?, created=?, last_updated=? where id=?";
        update(sql, payment.getTransactionType(), payment.getItem(), payment.getPaymentType(), payment.getPaymentStatus(),
               payment.getPaymentAmount(), payment.getPaymentCurrency(), payment.getPayerEmail(), StringUtils.lowerCase(payment.getPayerEmail()),
                payment.getPayerFirstName(), payment.getPayerLastName(), payment.getPayerCountry(), payment.getProcessingStatus().name(),
                payment.getCreated(), payment.getLastUpdated(), payment.getId());
        LOG.info("Updated " + payment);
    }

    public int getPaymentAmount(Date from, Date to) {
        String sql = "select sum(payment_amount) from payment where created between ? and ?";
        return queryForInt(sql, 0, from, to);
    }

    public boolean isBlacklisted(String email) {
        String sql = "select 1 from blacklist where email=?";
        return queryOne(sql, listRowMapper, StringUtils.lowerCase(email)) != null;
    }

    public boolean isWhitelisted(String email) {
        String sql = "select 1 from whitelist where email=?";
        return queryOne(sql, listRowMapper, StringUtils.lowerCase(email)) != null;
    }

    public void whitelist(String email) {
        update("insert into whitelist(email) values (?)", StringUtils.lowerCase(email));
    }

    private static class PaymentRowMapper implements ParameterizedRowMapper<Payment> {

        public Payment mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Payment(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                               rs.getString(6), rs.getInt(7), rs.getString(8), rs.getString(9), rs.getString(11),
                               rs.getString(12), rs.getString(13), Payment.ProcessingStatus.valueOf(rs.getString(14)),
                               rs.getTimestamp(15), rs.getTimestamp(16));
        }
    }
}