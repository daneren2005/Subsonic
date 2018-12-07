package github.daneren2005.dsub.domain;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

public class GenreComparatorTest extends TestCase {

	/**
	 * Sort genres which doesn't have name 
	 */
	public void testSortGenreWithoutNameComparator() {
		Genre g1 = new Genre();
		g1.setName("Genre");

		Genre g2 = new Genre();
		
		List<Genre> genres = new ArrayList<>();
		genres.add(g1);
		genres.add(g2);
		
		List<Genre> sortedGenre = Genre.GenreComparator.sort(genres);
		assertEquals(sortedGenre.get(0), g2);
	}

	/**
	 * Sort genre with same name
	 */
	public void testSortGenreWithSameName() {
		Genre g1 = new Genre();
		g1.setName("Genre");

		Genre g2 = new Genre();
		g2.setName("genre");
		
		List<Genre> genres = new ArrayList<>();
		genres.add(g1);
		genres.add(g2);
		
		List<Genre> sortedGenre = Genre.GenreComparator.sort(genres);
		assertEquals(sortedGenre.get(0), g1);
	}
	
	/**
	 * test nominal genre sort
	 */
	public void testSortGenre() {
		Genre g1 = new Genre();
		g1.setName("Rock");

		Genre g2 = new Genre();
		g2.setName("Pop");
		
		Genre g3 = new Genre();
		g3.setName("Rap");
		
		List<Genre> genres = new ArrayList<>();
		genres.add(g1);
		genres.add(g2);
		genres.add(g3);
		
		List<Genre> sortedGenre = Genre.GenreComparator.sort(genres);
		assertEquals(sortedGenre.get(0), g2);
		assertEquals(sortedGenre.get(1), g3);
		assertEquals(sortedGenre.get(2), g1);
	}
}