package net.ravendb.demo.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import net.ravendb.demo.assets.Gender;

public class Patient {

    private String id;
    private String firstName, lastName;
    private Date birthDate;
    private Gender gender;

    private String email;
    private Address address;
    private List<Visit> visits;


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public List<Visit> getVisits() {

        if (visits == null) {
            this.visits = new ArrayList<>();
        }

        return this.visits;
    }

    public void setVisits(List<Visit> visits) {
        this.visits = visits;
    }

    @JsonIgnore
    public void setBirthLocalDate(LocalDate localDate) {

        if (localDate == null) {
            return;
        }

        birthDate = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @JsonIgnore
    public LocalDate getBirthLocalDate() {
        if (birthDate != null)
            return Instant.ofEpochMilli(birthDate.getTime())
                          .atZone(ZoneId.systemDefault()).toLocalDate();
        else
            return null;
    }

    @JsonIgnore
    public static Visit getVisit(Collection<Visit> visits, Date date) {

        for (Visit v : visits) {

            if (v.getDate().equals(date)) {
                return v;
            }
        }

        return null;
    }

}
