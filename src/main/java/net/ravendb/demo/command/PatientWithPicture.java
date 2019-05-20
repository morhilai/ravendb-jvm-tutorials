package net.ravendb.demo.command;

import net.ravendb.demo.model.Patient;

public class PatientWithPicture {

    private Patient patient;
    private ProfilePicture profilePicture;

    public PatientWithPicture() {
        this.patient = new Patient();
    }

    public PatientWithPicture(Patient patient) {
        this.patient = patient;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public ProfilePicture getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(ProfilePicture profilePicture) {
        this.profilePicture = profilePicture;
    }

}
