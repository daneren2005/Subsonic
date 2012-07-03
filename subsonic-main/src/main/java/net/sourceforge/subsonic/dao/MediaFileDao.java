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
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MediaLibraryStatistics;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static net.sourceforge.subsonic.domain.MediaFile.MediaType;
import static net.sourceforge.subsonic.domain.MediaFile.MediaType.*;

/**
 * Provides database services for media files.
 *
 * @author Sindre Mehus
 */
public class MediaFileDao extends AbstractDao {

    private static final Logger LOG = Logger.getLogger(MediaFileDao.class);
    private static final String COLUMNS = "id, path, folder, type, format, title, album, artist, album_artist, disc_number, " +
            "track_number, year, genre, bit_rate, variable_bit_rate, duration_seconds, file_size, width, height, cover_art_path, " +
            "parent_path, play_count, last_played, comment, created, changed, last_scanned, children_last_updated, present, version";

    private static final int VERSION = 1;

    private final RowMapper rowMapper = new MediaFileMapper();
    private final RowMapper musicFileInfoRowMapper = new MusicFileInfoMapper();

    /**
     * Returns the media file for the given path.
     *
     * @param path The path.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(String path) {
        return queryOne("select " + COLUMNS + " from media_file where path=?", rowMapper, path);
    }

    /**
     * Returns the media file for the given ID.
     *
     * @param id The ID.
     * @return The media file or null.
     */
    public MediaFile getMediaFile(int id) {
        return queryOne("select " + COLUMNS + " from media_file where id=?", rowMapper, id);
    }

    /**
     * Returns the media file that are direct children of the given path.
     *
     * @param path The path.
     * @return The list of children.
     */
    public List<MediaFile> getChildrenOf(String path) {
        return query("select " + COLUMNS + " from media_file where parent_path=? and present", rowMapper, path);
    }

    public List<MediaFile> getFilesInPlaylist(int playlistId) {
        return query("select " + prefix(COLUMNS, "media_file") + " from media_file, playlist_file where " +
                "media_file.id = playlist_file.media_file_id and " +
                "playlist_file.playlist_id = ? and " +
                "media_file.present order by playlist_file.id", rowMapper, playlistId);
    }

    public List<MediaFile> getSongsForAlbum(String artist, String album) {
        return query("select " + COLUMNS + " from media_file where album_artist=? and album=? and present and type in (?,?,?) order by track_number", rowMapper,
                artist, album, MUSIC.name(), AUDIOBOOK.name(), PODCAST.name());
    }

    public List<MediaFile> getVideos(int size, int offset) {
        return query("select " + COLUMNS + " from media_file where type=? and present order by title limit ? offset ?", rowMapper,
                VIDEO.name(), size, offset);
    }

    /**
     * Creates or updates a media file.
     *
     * @param file The media file to create/update.
     */
    public synchronized void createOrUpdateMediaFile(MediaFile file) {
        String sql = "update media_file set " +
                "folder=?," +
                "type=?," +
                "format=?," +
                "title=?," +
                "album=?," +
                "artist=?," +
                "album_artist=?," +
                "disc_number=?," +
                "track_number=?," +
                "year=?," +
                "genre=?," +
                "bit_rate=?," +
                "variable_bit_rate=?," +
                "duration_seconds=?," +
                "file_size=?," +
                "width=?," +
                "height=?," +
                "cover_art_path=?," +
                "parent_path=?," +
                "play_count=?," +
                "last_played=?," +
                "comment=?," +
                "changed=?," +
                "last_scanned=?," +
                "children_last_updated=?," +
                "present=?, " +
                "version=? " +
                "where path=?";

        int n = update(sql,
                file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                file.getAlbumArtist(), file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                file.getChanged(), file.getLastScanned(), file.getChildrenLastUpdated(), file.isPresent(), VERSION, file.getPath());

        if (n == 0) {

            // Copy values from obsolete table music_file_info.
            MediaFile musicFileInfo = getMusicFileInfo(file.getPath());
            if (musicFileInfo != null) {
                file.setComment(musicFileInfo.getComment());
                file.setLastPlayed(musicFileInfo.getLastPlayed());
                file.setPlayCount(musicFileInfo.getPlayCount());
            }

            update("insert into media_file (" + COLUMNS + ") values (" + questionMarks(COLUMNS) + ")", null,
                    file.getPath(), file.getFolder(), file.getMediaType().name(), file.getFormat(), file.getTitle(), file.getAlbumName(), file.getArtist(),
                    file.getAlbumArtist(), file.getDiscNumber(), file.getTrackNumber(), file.getYear(), file.getGenre(), file.getBitRate(),
                    file.isVariableBitRate(), file.getDurationSeconds(), file.getFileSize(), file.getWidth(), file.getHeight(),
                    file.getCoverArtPath(), file.getParentPath(), file.getPlayCount(), file.getLastPlayed(), file.getComment(),
                    file.getCreated(), file.getChanged(), file.getLastScanned(),
                    file.getChildrenLastUpdated(), file.isPresent(), VERSION);
        }

        int id = queryForInt("select id from media_file where path=?", null, file.getPath());
        file.setId(id);
    }

    private MediaFile getMusicFileInfo(String path) {
        return queryOne("select play_count, last_played, comment from music_file_info where path=?", musicFileInfoRowMapper, path);
    }

    @Deprecated
    public List<String> getArtists() {
        return queryForStrings("select distinct artist from media_file where artist is not null and present order by artist");
    }

    public void deleteMediaFile(String path) {
        update("update media_file set present=false, children_last_updated=? where path=?", new Date(0L), path);
    }

    public List<String> getGenres() {
        return queryForStrings("select distinct genre from media_file where genre is not null and present order by genre");
    }

    /**
     * Returns the most frequently played albums.
     *
     * @param offset Number of albums to skip.
     * @param count  Maximum number of albums to return.
     * @return The most frequently played albums.
     */
    public List<MediaFile> getMostFrequentlyPlayedAlbums(int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type=? and play_count > 0 and present " +
                "order by play_count desc limit ? offset ?", rowMapper, ALBUM.name(), count, offset);
    }

    /**
     * Returns the most recently played albums.
     *
     * @param offset Number of albums to skip.
     * @param count  Maximum number of albums to return.
     * @return The most recently played albums.
     */
    public List<MediaFile> getMostRecentlyPlayedAlbums(int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type=? and last_played is not null and present " +
                "order by last_played desc limit ? offset ?", rowMapper, ALBUM.name(), count, offset);
    }

    /**
     * Returns the most recently added albums.
     *
     * @param offset Number of albums to skip.
     * @param count  Maximum number of albums to return.
     * @return The most recently added albums.
     */
    public List<MediaFile> getNewestAlbums(int offset, int count) {
        return query("select " + COLUMNS + " from media_file where type=? and present order by created desc limit ? offset ?",
                rowMapper, ALBUM.name(), count, offset);
    }

    /**
     * Returns albums in alphabetical order.
     *
     * @param offset   Number of albums to skip.
     * @param count    Maximum number of albums to return.
     * @param byArtist Whether to sort by artist name
     * @return Albums in alphabetical order.
     */
    public List<MediaFile> getAlphabetialAlbums(int offset, int count, boolean byArtist) {
        String orderBy = byArtist ? "artist, album" : "album";
        return query("select " + COLUMNS + " from media_file where type=? and artist != '' and present order by " + orderBy + " limit ? offset ?",
                rowMapper, ALBUM.name(), count, offset);
    }

    /**
     * Returns the most recently starred albums.
     *
     * @param offset   Number of albums to skip.
     * @param count    Maximum number of albums to return.
     * @param username Returns albums starred by this user.
     * @return The most recently starred albums for this user.
     */
    public List<MediaFile> getStarredAlbums(int offset, int count, String username) {
        return query("select " + prefix(COLUMNS, "media_file") + " from media_file, starred_media_file where media_file.id = starred_media_file.media_file_id and " +
                "media_file.present and media_file.type=? and starred_media_file.username=? order by starred_media_file.created desc limit ? offset ?",
                rowMapper, ALBUM.name(), username, count, offset);
    }

    /**
     * Returns the most recently starred directories.
     *
     * @param offset   Number of directories to skip.
     * @param count    Maximum number of directories to return.
     * @param username Returns directories starred by this user.
     * @return The most recently starred directories for this user.
     */
    public List<MediaFile> getStarredDirectories(int offset, int count, String username) {
        return query("select " + prefix(COLUMNS, "media_file") + " from media_file, starred_media_file where media_file.id = starred_media_file.media_file_id and " +
                "media_file.present and media_file.type=? and starred_media_file.username=? order by starred_media_file.created desc limit ? offset ?",
                rowMapper, DIRECTORY.name(), username, count, offset);
    }

    /**
     * Returns the most recently starred files.
     *
     * @param offset   Number of files to skip.
     * @param count    Maximum number of files to return.
     * @param username Returns files starred by this user.
     * @return The most recently starred files for this user.
     */
    public List<MediaFile> getStarredFiles(int offset, int count, String username) {
        return query("select " + prefix(COLUMNS, "media_file") + " from media_file, starred_media_file where media_file.id = starred_media_file.media_file_id and " +
                "media_file.present and media_file.type in (?,?,?,?) and starred_media_file.username=? order by starred_media_file.created desc limit ? offset ?",
                rowMapper, MUSIC.name(), PODCAST.name(), AUDIOBOOK.name(), VIDEO.name(), username, count, offset);
    }

    public void starMediaFile(int id, String username) {
        unstarMediaFile(id, username);
        update("insert into starred_media_file(media_file_id, username, created) values (?,?,?)", id, username, new Date());
    }

    public void unstarMediaFile(int id, String username) {
        update("delete from starred_media_file where media_file_id=? and username=?", id, username);
    }

    public Date getMediaFileStarredDate(int id, String username) {
        return queryForDate("select created from starred_media_file where media_file_id=? and username=?", null, id, username);
    }

    /**
     * Returns media library statistics, including the number of artists, albums and songs.
     *
     * @return Media library statistics.
     */
    public MediaLibraryStatistics getStatistics() {
        int artistCount = queryForInt("select count(1) from artist where present", 0);
        int albumCount = queryForInt("select count(1) from album where present", 0);
        int songCount = queryForInt("select count(1) from media_file where type in (?, ?, ?, ?) and present", 0, VIDEO.name(), MUSIC.name(), AUDIOBOOK.name(), PODCAST.name());
        long totalLengthInBytes = queryForLong("select sum(file_size) from media_file where present", 0L);
        long totalDurationInSeconds = queryForLong("select sum(duration_seconds) from media_file where present", 0L);

        return new MediaLibraryStatistics(artistCount, albumCount, songCount, totalLengthInBytes, totalDurationInSeconds);
    }

    public void markPresent(String path, Date lastScanned) {
        update("update media_file set present=?, last_scanned=? where path=?", true, lastScanned, path);
    }

    public void markNonPresent(Date lastScanned) {
        int minId = queryForInt("select id from media_file where true limit 1", 0);
        int maxId = queryForInt("select max(id) from media_file", 0);

        final int batchSize = 1000;
        Date childrenLastUpdated = new Date(0L);  // Used to force a children rescan if file is later resurrected.
        for (int id = minId; id <= maxId; id += batchSize) {
            update("update media_file set present=false, children_last_updated=? where id between ? and ? and last_scanned != ? and present",
                    childrenLastUpdated, id, id + batchSize, lastScanned);
        }
    }

    public void expunge() {
        int minId = queryForInt("select id from media_file where true limit 1", 0);
        int maxId = queryForInt("select max(id) from media_file", 0);

        final int batchSize = 1000;
        for (int id = minId; id <= maxId; id += batchSize) {
            update("delete from media_file where id between ? and ? and not present", id, id + batchSize);
        }
        update("checkpoint");
    }

    private static class MediaFileMapper implements ParameterizedRowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MediaFile(
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    MediaType.valueOf(rs.getString(4)),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7),
                    rs.getString(8),
                    rs.getString(9),
                    rs.getInt(10) == 0 ? null : rs.getInt(10),
                    rs.getInt(11) == 0 ? null : rs.getInt(11),
                    rs.getInt(12) == 0 ? null : rs.getInt(12),
                    rs.getString(13),
                    rs.getInt(14) == 0 ? null : rs.getInt(14),
                    rs.getBoolean(15),
                    rs.getInt(16) == 0 ? null : rs.getInt(16),
                    rs.getLong(17) == 0 ? null : rs.getLong(17),
                    rs.getInt(18) == 0 ? null : rs.getInt(18),
                    rs.getInt(19) == 0 ? null : rs.getInt(19),
                    rs.getString(20),
                    rs.getString(21),
                    rs.getInt(22),
                    rs.getTimestamp(23),
                    rs.getString(24),
                    rs.getTimestamp(25),
                    rs.getTimestamp(26),
                    rs.getTimestamp(27),
                    rs.getTimestamp(28),
                    rs.getBoolean(29));
        }
    }

    private static class MusicFileInfoMapper implements ParameterizedRowMapper<MediaFile> {
        public MediaFile mapRow(ResultSet rs, int rowNum) throws SQLException {
            MediaFile file = new MediaFile();
            file.setPlayCount(rs.getInt(1));
            file.setLastPlayed(rs.getTimestamp(2));
            file.setComment(rs.getString(3));
            return file;
        }
    }

}
