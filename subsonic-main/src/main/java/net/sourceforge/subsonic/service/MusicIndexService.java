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
package net.sourceforge.subsonic.service;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import net.sourceforge.subsonic.dao.MediaFileDao;
import net.sourceforge.subsonic.domain.MediaFile;
import net.sourceforge.subsonic.domain.MusicFolder;
import net.sourceforge.subsonic.domain.MusicIndex;
import net.sourceforge.subsonic.domain.MusicIndex.Artist;

/**
 * Provides services for grouping artists by index.
 *
 * @author Sindre Mehus
 */
public class MusicIndexService {

    private SettingsService settingsService;
    private MediaFileService mediaFileService;
    private MediaFileDao mediaFileDao;

    /**
     * Returns a map from music indexes to sets of artists that are direct children of the given music folders.
     *
     * @param folders The music folders.
     * @return A map from music indexes to sets of artists that are direct children of this music file.
     * @throws IOException If an I/O error occurs.
     */
    public SortedMap<MusicIndex, SortedSet<Artist>> getIndexedArtists(List<MusicFolder> folders) throws IOException {

        String[] ignoredArticles = settingsService.getIgnoredArticlesAsArray();
        String[] shortcuts = settingsService.getShortcutsAsArray();
        final List<MusicIndex> indexes = createIndexesFromExpression(settingsService.getIndexString());

        Comparator<MusicIndex> indexComparator = new MusicIndexComparator(indexes);
        SortedSet<Artist> artists = createArtists(folders, ignoredArticles, shortcuts);
        SortedMap<MusicIndex, SortedSet<Artist>> result = new TreeMap<MusicIndex, SortedSet<Artist>>(indexComparator);

        for (Artist artist : artists) {
            MusicIndex index = getIndex(artist, indexes);
            SortedSet<Artist> artistSet = result.get(index);
            if (artistSet == null) {
                artistSet = new TreeSet<Artist>();
                result.put(index, artistSet);
            }
            artistSet.add(artist);
        }

        return result;
    }

    /**
     * Creates a new instance by parsing the given expression.  The expression consists of an index name, followed by
     * an optional list of one-character prefixes. For example:<p/>
     * <p/>
     * The expression <em>"A"</em> will create the index <em>"A" -&gt; ["A"]</em><br/>
     * The expression <em>"The"</em> will create the index <em>"The" -&gt; ["The"]</em><br/>
     * The expression <em>"A(A&Aring;&AElig;)"</em> will create the index <em>"A" -&gt; ["A", "&Aring;", "&AElig;"]</em><br/>
     * The expression <em>"X-Z(XYZ)"</em> will create the index <em>"X-Z" -&gt; ["X", "Y", "Z"]</em>
     *
     * @param expr The expression to parse.
     * @return A new instance.
     */
    protected MusicIndex createIndexFromExpression(String expr) {
        int separatorIndex = expr.indexOf('(');
        if (separatorIndex == -1) {

            MusicIndex index = new MusicIndex(expr);
            index.addPrefix(expr);
            return index;
        }

        MusicIndex index = new MusicIndex(expr.substring(0, separatorIndex));
        String prefixString = expr.substring(separatorIndex + 1, expr.length() - 1);
        for (int i = 0; i < prefixString.length(); i++) {
            index.addPrefix(prefixString.substring(i, i + 1));
        }
        return index;
    }

    /**
     * Creates a list of music indexes by parsing the given expression.  The expression is a space-separated list of
     * sub-expressions, for which the rules described in {@link #createIndexFromExpression} apply.
     *
     * @param expr The expression to parse.
     * @return A list of music indexes.
     */
    protected List<MusicIndex> createIndexesFromExpression(String expr) {
        List<MusicIndex> result = new ArrayList<MusicIndex>();

        StringTokenizer tokenizer = new StringTokenizer(expr, " ");
        while (tokenizer.hasMoreTokens()) {
            MusicIndex index = createIndexFromExpression(tokenizer.nextToken());
            result.add(index);
        }

        return result;
    }

    private SortedSet<Artist> createArtists(List<MusicFolder> folders, String[] ignoredArticles, String[] shortcuts) throws IOException {
        return settingsService.isOrganizeByFolderStructure() ?
                createArtistsByFolderStructure(folders, ignoredArticles, shortcuts) :
                createArtistsByTagStructure(folders, ignoredArticles, shortcuts);
    }

    private SortedSet<Artist> createArtistsByFolderStructure(List<MusicFolder> folders, String[] ignoredArticles, String[] shortcuts) {
        SortedMap<String, Artist> artistMap = new TreeMap<String, Artist>();
        Set<String> shortcutSet = new HashSet<String>(Arrays.asList(shortcuts));

        for (MusicFolder folder : folders) {

            MediaFile root = mediaFileService.getMediaFile(folder.getPath(), true);
            List<MediaFile> children = mediaFileService.getChildrenOf(root, false, true, true, true);
            for (MediaFile child : children) {
                if (shortcutSet.contains(child.getName())) {
                    continue;
                }

                String sortableName = createSortableName(child.getName(), ignoredArticles);
                Artist artist = artistMap.get(sortableName);
                if (artist == null) {
                    artist = new Artist(child.getName(), sortableName);
                    artistMap.put(sortableName, artist);
                }
                artist.addMediaFile(child);
            }
        }

        return new TreeSet<Artist>(artistMap.values());
    }

    private SortedSet<Artist> createArtistsByTagStructure(List<MusicFolder> folders, String[] ignoredArticles, String[] shortcuts) {
        Set<String> shortcutSet = new HashSet<String>(Arrays.asList(shortcuts));
        SortedSet<Artist> artists = new TreeSet<Artist>();

        // TODO: Filter by folder
        for (String artistName : mediaFileDao.getArtists()) {

            if (shortcutSet.contains(artistName)) {
                continue;
            }

            String sortableName = createSortableName(artistName, ignoredArticles);
            Artist artist = new Artist(artistName, sortableName);
            artists.add(artist);
        }

        return artists;
    }

    private String createSortableName(String name, String[] ignoredArticles) {
        String uppercaseName = name.toUpperCase();
        for (String article : ignoredArticles) {
            if (uppercaseName.startsWith(article.toUpperCase() + " ")) {
                return name.substring(article.length() + 1) + ", " + article;
            }
        }
        return name;
    }

    /**
     * Returns the music index to which the given artist belongs.
     *
     * @param artist  The artist in question.
     * @param indexes List of available indexes.
     * @return The music index to which this music file belongs, or {@link MusicIndex#OTHER} if no index applies.
     */
    private MusicIndex getIndex(Artist artist, List<MusicIndex> indexes) {
        String sortableName = artist.getSortableName().toUpperCase();
        for (MusicIndex index : indexes) {
            for (String prefix : index.getPrefixes()) {
                if (sortableName.startsWith(prefix.toUpperCase())) {
                    return index;
                }
            }
        }
        return MusicIndex.OTHER;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setMediaFileService(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }

    public void setMediaFileDao(MediaFileDao mediaFileDao) {
        this.mediaFileDao = mediaFileDao;
    }

    private static class MusicIndexComparator implements Comparator<MusicIndex>, Serializable {

        private List<MusicIndex> indexes;

        public MusicIndexComparator(List<MusicIndex> indexes) {
            this.indexes = indexes;
        }

        public int compare(MusicIndex a, MusicIndex b) {
            int indexA = indexes.indexOf(a);
            int indexB = indexes.indexOf(b);

            if (indexA == -1) {
                indexA = Integer.MAX_VALUE;
            }
            if (indexB == -1) {
                indexB = Integer.MAX_VALUE;
            }

            if (indexA < indexB) {
                return -1;
            }
            if (indexA > indexB) {
                return 1;
            }
            return 0;
        }
    }
}