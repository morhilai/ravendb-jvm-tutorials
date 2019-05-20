package net.ravendb.demo.presenters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ravendb.demo.command.PatientWithPicture;
import net.ravendb.demo.command.ProfilePicture;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.ravendb.client.documents.operations.attachments.AttachmentName;
import net.ravendb.client.documents.operations.attachments.CloseableAttachmentResult;
import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.client.primitives.Reference;
import net.ravendb.demo.db.RavenDBDocumentStore;
import net.ravendb.demo.model.Address;
import net.ravendb.demo.model.Configuration;
import net.ravendb.demo.model.Patient;
import net.ravendb.demo.presenters.PatientViewable.PatientViewListener;

public class PatientPresenter implements PatientViewListener {
    private static Logger logger = Logger.getLogger(PatientPresenter.class.getSimpleName());

    private IDocumentSession session;

    public PatientPresenter() {}

    @Override
    public Pair<Collection<PatientWithPicture>, Integer> getPatientsList(int offset, int limit, boolean order) {
        Reference<QueryStatistics> statsRef = new Reference<>();
        IDocumentQuery<Patient> query = session.query(Patient.class)
                .skip(offset)
                .take(limit)
                .statistics(statsRef);

        if (order) {
            query.orderBy("birthDate");
        }

        Collection<Patient> list = query.toList();
        int totalResults = statsRef.value.getTotalResults();

        Collection<PatientWithPicture> patientWithPictures = new ArrayList<>();

        for (Patient patient : list) {
            PatientWithPicture patientWithPicture = new PatientWithPicture(patient);
            AttachmentName[] names = session.advanced().attachments().getNames(patient);

            if (names.length > 0) {

                try (CloseableAttachmentResult result = session.advanced().attachments().get(patient,
                        names[0].getName())) {
                    ProfilePicture profilePicture = new ProfilePicture();
                    profilePicture.setName(names[0].getName());
                    profilePicture.setMimeType(names[0].getContentType());
                    byte[] bytes = IOUtils.toByteArray(result.getData());
                    profilePicture.setBytes(bytes);
                    patientWithPicture.setProfilePicture(profilePicture);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "", e);
                }
            }
            patientWithPictures.add(patientWithPicture);
        }

        return new ImmutablePair<Collection<PatientWithPicture>, Integer>(patientWithPictures, totalResults);
    }

    @Override
    public Pair<Collection<PatientWithPicture>, Integer> searchPatientsList(
           int offset, int limit, String term, boolean order) {

        Reference<QueryStatistics> statsRef = new Reference<>();
        session.advanced().clear();

        IDocumentQuery<Patient> query = session.query(Patient.class)
                .whereStartsWith("firstName", term)
                .skip(offset)
                .take(limit)
                .statistics(statsRef);

        if (order) {
            query.orderBy("birthDate");
        }

        Collection<Patient> list = query.toList();
        int totalResults = statsRef.value.getTotalResults();

        Collection<PatientWithPicture> patientWithPictures = new ArrayList<>();

        for (Patient patient : list) {
            PatientWithPicture patientWithPicture = new PatientWithPicture(patient);
            AttachmentName[] names = session.advanced().attachments().getNames(patient);

            if (names.length > 0) {

                try (CloseableAttachmentResult result = session.advanced()
                                                               .attachments()
                                                               .get(patient,
                                                               names[0].getName())) {
                    ProfilePicture profilePicture = new ProfilePicture();
                    profilePicture.setName(names[0].getName());
                    profilePicture.setMimeType(names[0].getContentType());

                    byte[] bytes = IOUtils.toByteArray(result.getData());
                    profilePicture.setBytes(bytes);
                    patientWithPicture.setProfilePicture(profilePicture);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "", e);
                }

            }
            patientWithPictures.add(patientWithPicture);
        }

        return new ImmutablePair<Collection<PatientWithPicture>, Integer>(patientWithPictures, totalResults);
    }

    @Override
    public Collection<String> getRegionsList() {

        Configuration condition = session.query(Configuration.class).first();

        if (condition != null) {
            return condition.getRegions();
        } else {
            return Collections.EMPTY_LIST;
        }
    }

    @Override
    public void create(PatientWithPicture patientWithPicture) {
        Patient patient = patientWithPicture.getPatient();
        ProfilePicture profilePicture = patientWithPicture.getProfilePicture();
        session.store(patient);

        if (profilePicture != null) {
            session.advanced().attachments().store(patient, profilePicture.getName(),
                    profilePicture.getInputStream(), profilePicture.getMimeType());
        }

        session.saveChanges();
    }

    @Override
    public void update(PatientWithPicture patientWithPicture) throws ConcurrencyException {
        Patient patient = patientWithPicture.getPatient();
        ProfilePicture profilePicture = patientWithPicture.getProfilePicture();
        session.store(patient);

        // delete previous attachments
        AttachmentName[] names = session.advanced().attachments().getNames(patient);

        if (names.length > 0) {
            session.advanced().attachments().delete(patient, names[0].getName());
        }

        if (profilePicture != null) {
            session.advanced().attachments().store(patient, profilePicture.getName(),
                    profilePicture.getInputStream(), profilePicture.getMimeType());
        }

        session.saveChanges();
    }

    @Override
    public void save(String patientId, Address address) {
        Patient patient = session.load(Patient.class, patientId);
        patient.setAddress(address);
        session.store(patient);
        session.saveChanges();
    }

    @Override
    public void delete(PatientWithPicture patient) {
        session.delete(patient.getPatient());
        session.saveChanges();
    }

    @Override
    public void openSession() {
        if (session == null) {
            session = RavenDBDocumentStore.getStore().openSession();
            // enable optimistic concurrency
            session.advanced().setUseOptimisticConcurrency(true);
        }
    }

    @Override
    public void releaseSession() {
        session.close();
    }

}
