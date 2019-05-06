package net.ravendb.demo.presenters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import net.ravendb.demo.command.Attachment;
import net.ravendb.demo.command.PatientAttachment;
import net.ravendb.demo.components.editor.PatientEditorDialog;
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
    public Pair<Collection<PatientAttachment>, Integer> getPatientsList(int offset, int limit, boolean order) {
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

        Collection<PatientAttachment> patientAttachments = new ArrayList<>();

        for (Patient patient : list) {
            PatientAttachment patientAttachment = new PatientAttachment(patient);
            AttachmentName[] names = session.advanced().attachments().getNames(patient);

            if (names.length > 0) {

                try (CloseableAttachmentResult result = session.advanced().attachments().get(patient,
                        names[0].getName())) {
                    Attachment attachment = new Attachment();
                    attachment.setName(names[0].getName());
                    attachment.setMimeType(names[0].getContentType());
                    byte[] bytes = IOUtils.toByteArray(result.getData());
                    attachment.setBytes(bytes);
                    patientAttachment.setAttachment(attachment);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "", e);
                }
            }
            patientAttachments.add(patientAttachment);
        }

        return new ImmutablePair<Collection<PatientAttachment>, Integer>(patientAttachments, totalResults);
    }

    @Override
    public Pair<Collection<PatientAttachment>, Integer> searchPatientsList(
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

        Collection<PatientAttachment> patientAttachments = new ArrayList<>();

        for (Patient patient : list) {
            PatientAttachment patientAttachment = new PatientAttachment(patient);
            AttachmentName[] names = session.advanced().attachments().getNames(patient);

            if (names.length > 0) {

                try (CloseableAttachmentResult result = session.advanced()
                                                               .attachments()
                                                               .get(patient,
                                                               names[0].getName())) {
                    Attachment attachment = new Attachment();
                    attachment.setName(names[0].getName());
                    attachment.setMimeType(names[0].getContentType());

                    byte[] bytes = IOUtils.toByteArray(result.getData());
                    attachment.setBytes(bytes);
                    patientAttachment.setAttachment(attachment);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "", e);
                }

            }
            patientAttachments.add(patientAttachment);
        }

        return new ImmutablePair<Collection<PatientAttachment>, Integer>(patientAttachments, totalResults);
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
    public void create(PatientAttachment patientAttachment) {
        Patient patient = patientAttachment.getPatient();
        Attachment attachment = patientAttachment.getAttachment();
        session.store(patient);

        if (attachment != null) {
            session.advanced().attachments().store(patient, attachment.getName(),
                    attachment.getInputStream(), attachment.getMimeType());
        }

        session.saveChanges();
    }

    @Override
    public void update(PatientAttachment patientAttachment) throws ConcurrencyException {
        Patient patient = patientAttachment.getPatient();
        Attachment attachment = patientAttachment.getAttachment();
        session.store(patient);

        // delete previous attachments
        AttachmentName[] names = session.advanced().attachments().getNames(patient);

        if (names.length > 0) {
            session.advanced().attachments().delete(patient, names[0].getName());
        }

        if (attachment != null) {
            session.advanced().attachments().store(patient, attachment.getName(),
                    attachment.getInputStream(), attachment.getMimeType());
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
    public void delete(PatientAttachment patient) {
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
