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
package net.sourceforge.subsonic.backend.dao.schema;

import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Used for creating and evolving the database schema.
 * This class implementes the database schema for Subsonic Backend version 2.0.
 *
 * @author Sindre Mehus
 */
public class Schema20 extends Schema {
    private static final Logger LOG = Logger.getLogger(Schema20.class);

    public void execute(JdbcTemplate template) {

        if (!tableExists(template, "payment")) {
            LOG.info("Database table 'payment' not found.  Creating it.");
            template.execute("create cached table payment (" +
                             "id identity," +
                             "transaction_id varchar not null," +
                             "transaction_type varchar," + // cart, web_accept
                             "item varchar," +
                             "payment_type varchar," +  // echeck, instant
                             "payment_status varchar," + // Completed, Pending, Denied, Failed, ...
                             "payment_amount int," +
                             "payment_currency varchar," +
                             "payer_email varchar," +
                             "payer_first_name varchar," +
                             "payer_last_name varchar," +
                             "payer_country varchar," +
                             "processing_status varchar not null," +
                             "created datetime," +
                             "last_updated datetime," +
                             "unique(transaction_id))");
            template.execute("create index idx_payment_transaction_id on payment(transaction_id)");
            template.execute("create index idx_payment_created on payment(created)");
            template.execute("create index idx_payment_payer_email on payment(payer_email)");

            LOG.info("Database table 'payment' was created successfully.");
        }

        if (!columnExists(template, "payer_email_lower", "payment")) {
            LOG.info("Database column 'payment.payer_email_lower' not found.  Creating it.");
            template.execute("alter table payment " +
                             "add payer_email_lower varchar");
            template.execute("update payment set payer_email_lower=lcase(payer_email)");
            template.execute("create index idx_payment_payer_email_lower on payment(payer_email_lower)");
            LOG.info("Database column 'payment.payer_email_lower' was added successfully.");
        }

        if (!tableExists(template, "whitelist")) {
            LOG.info("Database table 'whitelist' not found.  Creating it.");
            template.execute("create cached table whitelist (" +
                             "id identity," +
                             "email varchar not null)");
            template.execute("create index idx_whitelist_email on whitelist(email)");

            LOG.info("Database table 'whitelist' was created successfully.");
        }

        if (!tableExists(template, "blacklist")) {
            LOG.info("Database table 'blacklist' not found.  Creating it.");
            template.execute("create cached table blacklist (" +
                             "id identity," +
                             "email varchar not null)");
            template.execute("create index idx_blacklist_email on blacklist(email)");

            LOG.info("Database table 'blacklist' was created successfully.");
        }
    }
}