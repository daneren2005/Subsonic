package net.sourceforge.subsonic.backend.dao;

import net.sourceforge.subsonic.backend.dao.schema.Schema;
import net.sourceforge.subsonic.backend.dao.schema.Schema10;
import net.sourceforge.subsonic.backend.dao.schema.Schema20;
import net.sourceforge.subsonic.backend.Util;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.io.File;

/**
 * DAO helper class which creates the data source, and updates the database schema.
 *
 * @author Sindre Mehus
 */
public class DaoHelper {

    private static final Logger LOG = Logger.getLogger(DaoHelper.class);

    private Schema[] schemas = {new Schema10(), new Schema20()};
    private DataSource dataSource;
    private static boolean shutdownHookAdded;

    public DaoHelper() {
        dataSource = createDataSource();
        checkDatabase();
        addShutdownHook();
    }

    private void addShutdownHook() {
        if (shutdownHookAdded) {
            return;
        }
        shutdownHookAdded = true;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("Shutting down database.");
                try {
                    getJdbcTemplate().execute("shutdown");
                    System.err.println("Done.");
                } catch (Throwable x) {
                    System.err.println("Failed to shut down database.");
                    x.printStackTrace();
                }
            }
        });
    }

    /**
     * Returns a JDBC template for performing database operations.
     *
     * @return A JDBC template.
     */
    public JdbcTemplate getJdbcTemplate() {
        return new JdbcTemplate(dataSource);
    }

    private DataSource createDataSource() {
        File home = Util.getBackendHome();
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:file:" + home.getPath() + "/db/subsonic-backend");
        ds.setUsername("sa");
        ds.setPassword("");

        return ds;
    }

    private void checkDatabase() {
        LOG.info("Checking database schema.");
        try {
            for (Schema schema : schemas) {
                schema.execute(getJdbcTemplate());
            }
            LOG.info("Done checking database schema.");
        } catch (Exception x) {
            LOG.error("Failed to initialize database.", x);
        }
    }
}
