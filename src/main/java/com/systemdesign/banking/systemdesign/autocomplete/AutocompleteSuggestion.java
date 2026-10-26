package com.systemdesign.banking.systemdesign.autocomplete;

import java.io.Serializable;

public class AutocompleteSuggestion implements Serializable, Comparable<AutocompleteSuggestion> {

    private String term;
    private String category;
    private String referenceCode;
    private int score;

    public AutocompleteSuggestion() {
    }

    public AutocompleteSuggestion(String term, String category, String referenceCode, int score) {
        this.term = term;
        this.category = category;
        this.referenceCode = referenceCode;
        this.score = score;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @Override
    public int compareTo(AutocompleteSuggestion o) {
        // Descending by score, then alphabetical
        int cmp = Integer.compare(o.score, this.score);
        return cmp != 0 ? cmp : this.term.compareToIgnoreCase(o.term);
    }
}
