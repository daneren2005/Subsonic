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

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.backend.controller;

import org.apache.log4j.Logger;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.params.HttpConnectionParams;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Date;
import java.io.UnsupportedEncodingException;

import net.sourceforge.subsonic.backend.domain.Payment;
import net.sourceforge.subsonic.backend.dao.PaymentDao;

/**
 * Processes IPNs (Instant Payment Notifications) from PayPal.
 *
 * @author Sindre Mehus
 */
public class IPNController implements Controller {

    private static final Logger LOG = Logger.getLogger(IPNController.class);

    private static final String PAYPAL_URL = "https://www.paypal.com/cgi-bin/webscr";

    private PaymentDao paymentDao;

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {

            LOG.info("Incoming IPN from " + request.getRemoteAddr());

            String url = createValidationURL(request);
            if (validate(url)) {
                LOG.info("Verified payment. " + url);
            } else {
                LOG.warn("Failed to verify payment. " + url);
            }
            createOrUpdatePayment(request);

            return null;
        } catch (Exception x) {
            LOG.error("Failed to process IPN.", x);
            throw x;
        }
    }

    private String createValidationURL(HttpServletRequest request) throws UnsupportedEncodingException {
        Enumeration<?> en = request.getParameterNames();
        StringBuilder url = new StringBuilder(PAYPAL_URL).append("?cmd=_notify-validate");
        String encoding = request.getParameter("charset");
        if (encoding == null) {
            encoding = "ISO-8859-1";
        }

        while (en.hasMoreElements()) {
            String paramName = (String) en.nextElement();
            String paramValue = request.getParameter(paramName);
            url.append("&").append(paramName).append("=").append(URLEncoder.encode(paramValue, encoding));
        }

        return url.toString();
    }

    private void createOrUpdatePayment(HttpServletRequest request) {
        String item = request.getParameter("item_number");
        if (item == null) {
            item = request.getParameter("item_number1");
        }
        String paymentStatus = request.getParameter("payment_status");
        String paymentType = request.getParameter("payment_type");
        int paymentAmount = Math.round(new Float(request.getParameter("mc_gross")));
        String paymentCurrency = request.getParameter("mc_currency");
        String txnId = request.getParameter("txn_id");
        String txnType = request.getParameter("txn_type");
        String payerEmail = request.getParameter("payer_email");
        String payerFirstName = request.getParameter("first_name");
        String payerLastName = request.getParameter("last_name");
        String payerCountry = request.getParameter("address_country");

        Payment payment = paymentDao.getPaymentByTransactionId(txnId);
        if (payment == null) {
            payment = new Payment(null, txnId, txnType, item, paymentType, paymentStatus,
                                  paymentAmount, paymentCurrency, payerEmail, payerFirstName, payerLastName,
                                  payerCountry, Payment.ProcessingStatus.NEW, new Date(), new Date());
            paymentDao.createPayment(payment);
        } else {
            payment.setTransactionType(txnType);
            payment.setItem(item);
            payment.setPaymentType(paymentType);
            payment.setPaymentStatus(paymentStatus);
            payment.setPaymentAmount(paymentAmount);
            payment.setPaymentCurrency(paymentCurrency);
            payment.setPayerEmail(payerEmail);
            payment.setPayerFirstName(payerFirstName);
            payment.setPayerLastName(payerLastName);
            payment.setPayerCountry(payerCountry);
            payment.setLastUpdated(new Date());
            paymentDao.updatePayment(payment);
        }

        LOG.info("Received " + payment);
    }

    private boolean validate(String url) throws Exception {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 60000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 60000);
        HttpGet method = new HttpGet(url);
        String content;
        try {
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            content = client.execute(method, responseHandler);

            LOG.info("Validation result: " + content);
            return "VERIFIED".equals(content);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    public void setPaymentDao(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
    }

}
