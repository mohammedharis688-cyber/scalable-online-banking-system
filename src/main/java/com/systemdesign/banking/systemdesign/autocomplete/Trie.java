package com.systemdesign.banking.systemdesign.autocomplete;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Trie {

    private final TrieNode root = new TrieNode();

    public synchronized void insert(String term, String category, String referenceCode, int score) {
        if (term == null || term.isBlank()) {
            return;
        }

        String normalized = term.trim().toLowerCase();
        AutocompleteSuggestion suggestion = new AutocompleteSuggestion(term, category, referenceCode, score);

        TrieNode current = root;
        for (char ch : normalized.toCharArray()) {
            current = current.getChildren().computeIfAbsent(ch, c -> new TrieNode());

            // Maintain top suggestions at each prefix node
            List<AutocompleteSuggestion> list = current.getSuggestions();
            // Check if already present to prevent duplicates
            list.removeIf(s -> s.getTerm().equalsIgnoreCase(term));
            list.add(suggestion);
            Collections.sort(list);
            if (list.size() > 20) {
                list.remove(list.size() - 1);
            }
        }
        current.setEndOfWord(true);
    }

    public List<AutocompleteSuggestion> search(String prefix, int limit) {
        if (prefix == null || prefix.isBlank()) {
            return Collections.emptyList();
        }

        String normalized = prefix.trim().toLowerCase();
        TrieNode current = root;

        for (char ch : normalized.toCharArray()) {
            TrieNode child = current.getChildren().get(ch);
            if (child == null) {
                return Collections.emptyList();
            }
            current = child;
        }

        List<AutocompleteSuggestion> results = new ArrayList<>(current.getSuggestions());
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }
}
