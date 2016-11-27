package com.microservices.research;

/**
 * Created by alim20 on 8/11/16.
 */
public class PaperAuthor {

    String firstName;
    String lastName;

    String affiliation;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }

    @Override
    public String toString() {

        return "Firstname: "+firstName +", Surname: "+lastName+". Affiliation: "+affiliation;
    }
}
