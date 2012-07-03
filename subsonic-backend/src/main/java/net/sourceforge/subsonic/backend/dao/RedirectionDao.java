package net.sourceforge.subsonic.backend.dao;

import net.sourceforge.subsonic.backend.domain.Redirection;
import org.apache.log4j.Logger;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides database services for xxx.subsonic.org redirections.
 *
 * @author Sindre Mehus
 */
public class RedirectionDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(RedirectionDao.class);
    private static final String COLUMNS = "id, license_holder, server_id, redirect_from, redirect_to, local_redirect_to, trial, trial_expires, last_updated, last_read, read_count";

    private RedirectionRowMapper rowMapper = new RedirectionRowMapper();

    /**
     * Returns the redirection with the given "redirect from".
     *
     * @param redirectFrom The "redirect from" string.
     * @return The redirection or <code>null</code> if not found.
     */
    public Redirection getRedirection(String redirectFrom) {
        String sql = "select " + COLUMNS + " from redirection where redirect_from=?";
        return queryOne(sql, rowMapper, redirectFrom);
    }

    /**
     * Returns all redirections with respect to the given row offset and count.
     *
     * @param offset Number of rows to skip.
     * @param count  Maximum number of rows to return.
     * @return Redirections with respect to the given row offset and count.
     */
    public List<Redirection> getAllRedirections(int offset, int count) {
        if (count < 1) {
            return new ArrayList<Redirection>();
        }
        String sql = "select " + COLUMNS + " from redirection " +
                     "order by id " +
                     "limit " + count + " offset " + offset;
        return query(sql, rowMapper);
    }

    /**
     * Creates a new redirection.
     *
     * @param redirection The redirection to create.
     */
    public void createRedirection(Redirection redirection) {
        String sql = "insert into redirection (" + COLUMNS + ") values (null, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        update(sql, redirection.getLicenseHolder(), redirection.getServerId(), redirection.getRedirectFrom(),
                redirection.getRedirectTo(), redirection.getLocalRedirectTo(), redirection.isTrial(),
                redirection.getTrialExpires(), redirection.getLastUpdated(),
                redirection.getLastRead(), redirection.getReadCount());
        LOG.info("Created redirection " + redirection.getRedirectFrom() + " -> " + redirection.getRedirectTo());
    }

    /**
     * Updates the given redirection.
     *
     * @param redirection The redirection to update.
     */
    public void updateRedirection(Redirection redirection) {
        String sql = "update redirection set license_holder=?, server_id=?, redirect_from=?, redirect_to=?, " +
                     "local_redirect_to=?, trial=?, trial_expires=?, last_updated=?, last_read=?, read_count=? where id=?";
        update(sql, redirection.getLicenseHolder(), redirection.getServerId(), redirection.getRedirectFrom(),
               redirection.getRedirectTo(), redirection.getLocalRedirectTo(), redirection.isTrial(), redirection.getTrialExpires(),
               redirection.getLastUpdated(), redirection.getLastRead(), redirection.getReadCount(), redirection.getId());
    }

    /**
     * Deletes all redirections with the given server ID.
     *
     * @param serverId The server ID.
     */
    public void deleteRedirectionsByServerId(String serverId) {
        update("delete from redirection where server_id=?", serverId);
        LOG.info("Deleted redirections for server ID " + serverId);
    }

    private static class RedirectionRowMapper implements ParameterizedRowMapper<Redirection> {
        public Redirection mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Redirection(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5),
                    rs.getString(6), rs.getBoolean(7), rs.getTimestamp(8), rs.getTimestamp(9), rs.getTimestamp(10),
                    rs.getInt(11));
        }
    }
}
