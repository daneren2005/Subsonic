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
package net.sourceforge.subsonic.backend.domain;

import java.util.Date;

/**
 * @author Sindre Mehus
 */
public class Payment {

    private String id;
    private String transactionId;
    private String transactionType;
    private String item;
    private String paymentType;
    private String paymentStatus;
    private int paymentAmount;
    private String paymentCurrency;
    private String payerEmail;
    private String payerFirstName;
    private String payerLastName;
    private String payerCountry;
    private ProcessingStatus processingStatus;
    private Date created;
    private Date lastUpdated;

    public Payment(String id, String transactionId, String transactionType, String item, String paymentType,
                   String paymentStatus, int paymentAmount, String paymentCurrency, String payerEmail,
                   String payerFirstName, String payerLastName, String payerCountry, ProcessingStatus processingStatus,
                   Date created, Date lastUpdated) {
        this.id = id;
        this.transactionId = transactionId;
        this.transactionType = transactionType;
        this.item = item;
        this.paymentType = paymentType;
        this.paymentStatus = paymentStatus;
        this.paymentAmount = paymentAmount;
        this.paymentCurrency = paymentCurrency;
        this.payerEmail = payerEmail;
        this.payerFirstName = payerFirstName;
        this.payerLastName = payerLastName;
        this.payerCountry = payerCountry;
        this.processingStatus = processingStatus;
        this.created = created;
        this.lastUpdated = lastUpdated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public int getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(int paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public String getPaymentCurrency() {
        return paymentCurrency;
    }

    public void setPaymentCurrency(String paymentCurrency) {
        this.paymentCurrency = paymentCurrency;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getPayerFirstName() {
        return payerFirstName;
    }

    public void setPayerFirstName(String payerFirstName) {
        this.payerFirstName = payerFirstName;
    }

    public String getPayerLastName() {
        return payerLastName;
    }

    public void setPayerLastName(String payerLastName) {
        this.payerLastName = payerLastName;
    }

    public String getPayerCountry() {
        return payerCountry;
    }

    public void setPayerCountry(String payerCountry) {
        this.payerCountry = payerCountry;
    }

    public ProcessingStatus getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(ProcessingStatus processingStatus) {
        this.processingStatus = processingStatus;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "tx='" + transactionId + '\'' +
                ", type='" + paymentType + '\'' +
                ", status='" + paymentStatus + '\'' +
                ", amount=" + paymentAmount +
                ", email='" + payerEmail + '\'' +
                '}';
    }

    public enum ProcessingStatus {
        NEW,
        COMPLETED
    }
}
