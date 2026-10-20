package com.systemdesign.banking.systemdesign.autocomplete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrieAutocompleteTest {

    private Trie trie;

    @BeforeEach
    void setUp() {
        trie = new Trie();
        trie.insert("Apex City Power & Electric", "ELECTRICITY", "ELEC-01", 100);
        trie.insert("Metropolitan Water Works", "WATER", "WATER-01", 100);
        trie.insert("FiberLink High-Speed Internet", "INTERNET", "NET-01", 100);
        trie.insert("Apex Cellular Mobile", "MOBILE", "MOB-01", 90);
        trie.insert("Alice Smith", "BENEFICIARY", "ACC-1001-10000002", 95);
    }

    @Test
    @DisplayName("Should return matching suggestions for valid prefix")
    void testPrefixMatching() {
        List<AutocompleteSuggestion> results = trie.search("apex", 10);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(s -> s.getReferenceCode().equals("ELEC-01")));
        assertTrue(results.stream().anyMatch(s -> s.getReferenceCode().equals("MOB-01")));
    }

    @Test
    @DisplayName("Should handle case-insensitive matching")
    void testCaseInsensitivity() {
        List<AutocompleteSuggestion> lower = trie.search("metro", 5);
        List<AutocompleteSuggestion> upper = trie.search("METRO", 5);

        assertFalse(lower.isEmpty());
        assertEquals(lower.size(), upper.size());
        assertEquals("Metropolitan Water Works", lower.get(0).getTerm());
    }

    @Test
    @DisplayName("Should return empty list for non-matching prefix")
    void testNonMatchingPrefix() {
        List<AutocompleteSuggestion> results = trie.search("xyz123", 5);
        assertTrue(results.isEmpty());
    }
}
