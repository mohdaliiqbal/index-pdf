package com.microservices.research;

import java.util.Date;
import java.util.List;

/**
 * Created by alim20 on 8/11/16.
 */
public class Paper {

    String id;
    String title;
    String abstracts;
    String content;
    List<String> keywords;
    private Date creationDate = new Date();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAbstracts() {
        return abstracts;
    }

    public void setAbstracts(String abstracts) {
        this.abstracts = abstracts;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getKeywords() {
        if(keywords == null || keywords.size()<=0)
            return null;

        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<PaperAuthor> getAuthors() {
        return authors;
    }

    public void setAuthors(List<PaperAuthor> authors) {
        this.authors = authors;
    }

    public List<Bibliography> getBibliographies() {
        return bibliographies;
    }

    public void setBibliographies(List<Bibliography> bibliographies) {
        this.bibliographies = bibliographies;
    }

    public Date getCreationDate(){
        return creationDate;
    }
    List<PaperAuthor> authors;
    List<Bibliography> bibliographies ;

}
