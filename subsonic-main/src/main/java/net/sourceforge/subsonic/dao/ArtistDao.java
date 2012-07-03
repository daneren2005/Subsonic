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
package net.sourceforge.subsonic.dao;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.Artist;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

/**
 * Provides database services for artists.
 *
 * @author Sindre Mehus
 */
public class ArtistDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(ArtistDao.class);
    private static final String COLUMNS = "id, name, cover_art_path, album_count, last_scanned, present";

    private final RowMapper rowMapper = new ArtistMapper();

    /**
     * Returns the artist with the given name.
     *
     * @param artistName The artist name.
     * @return The artist or null.
     */
    public Artist getArtist(String artistName) {
        return queryOne("select " + COLUMNS + " from artist where name=?", rowMapper, artistName);
    }

    /**
     * Returns the artist with the given ID.
     *
     * @param id The artist ID.
     * @return The artist or null.
     */
    public Artist getArtist(int id) {
        return queryOne("select " + COLUMNS + " from artist where id=?", rowMapper, id);
    }

    /**
     * Creates or updates an artist.
     *
     * @param artist The artist to create/update.
     */
    public synchronized void createOrUpdateArtist(Artist artist) {
        String sql = "update artist set " +
                "cover_art_path=?," +
                "album_count=?," +
                "last_scanned=?," +
                "present=? " +
                "where name=?";

        int n = update(sql, artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(), artist.isPresent(), artist.getName());

        if (n == 0) {

            update("insert into artist (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")", null,
                    artist.getName(), artist.getCoverArtPath(), artist.getAlbumCount(), artist.getLastScanned(), artist.isPresent());
        }

        int id = queryForInt("select id from artist where name=?", null, artist.getName());
        artist.setId(id);
    }

    /**
     * Returns artists in alphabetical order.
     *
     * @param offset Number of artists to skip.
     * @param count  Maximum number of artists to return.
     * @return Artists in alphabetical order.
     */
    public List<Artist> getAlphabetialArtists(int offset, int count) {
        return query("select " + COLUMNS + " from artist where present order by name limit ? offset ?", rowMapper, count, offset);
    }

    /**
     * Returns the most recently starred artists.
     *
     * @param offset   Number of artists to skip.
     * @param count    Maximum number of artists to return.
     * @param username Returns artists starred by this user.
     * @return The most recently starred artists for this user.
     */
    public List<Artist> getStarredArtists(int offset, int count, String username) {
        return query("select " + prefix(COLUMNS, "artist") + " from artist, starred_artist where artist.id = starred_artist.artist_id and " +
                "artist.present and starred_artist.username=? order by starred_artist.created desc limit ? offset ?",
                rowMapper, username, count, offset);
    }

    public void markPresent(String artistName, Date lastScanned) {
        update("update artist set present=?, last_scanned=? where name=?", true, lastScanned, artistName);
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select id from artist where true limit 1", 0);
        int maxId = queryForInt("select max(id) from artist", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update artist set present=false where id between ? and ? and last_scanned != ? and present", id, id + batchSize, lastScanned);
        }
    }

    public void expunge() {
        int minId = queryForInt("select id from artist where true limit 1", 0);
        int maxId = queryForInt("select max(id) from artist", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("delete from artist where id between ? and ? and not present", id, id + batchSize);
        }
    }

    public void starArtist(int artistId, String username) {
        unstarArtist(artistId, username);
        update("insert into starred_artist(artist_id, username, created) values (?,?,?)", artistId, username, new Date());
    }

    public void unstarArtist(int artistId, String username) {
        update("delete from starred_artist where artist_id=? and username=?", artistId, username);
    }

    public Date getArtistStarredDate(int artistId, String username) {
        return queryForDate("select created from starred_artist where artist_id=? and username=?", null, artistId, username);
    }

    private static class ArtistMapper implements ParameterizedRowMapper<Artist> {
        public Artist mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Artist(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4),
                    rs.getTimestamp(5),
                    rs.getBoolean(6));
        }
    }
}
