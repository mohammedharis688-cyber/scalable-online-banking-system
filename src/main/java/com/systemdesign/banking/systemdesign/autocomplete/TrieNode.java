package com.systemdesign.banking.systemdesign.autocomplete;

import java.util.*;

public class TrieNode {

    private final Map<Character, TrieNode> children = new HashMap<>();
    private final List<AutocompleteSuggestion> suggestions = new ArrayList<>();
    private boolean isEndOfWord = false;

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public List<AutocompleteSuggestion> getSuggestions() {
        return suggestions;
    }

    public boolean isEndOfWord() {
        return isEndOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        isEndOfWord = endOfWord;
    }
}
