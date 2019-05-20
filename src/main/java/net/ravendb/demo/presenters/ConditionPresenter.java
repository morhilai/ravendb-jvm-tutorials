package net.ravendb.demo.presenters;

import java.util.Collection;
import java.util.List;

import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.demo.command.DoctorVisit;
import net.ravendb.demo.model.Doctor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import net.ravendb.demo.db.RavenDBDocumentStore;
import net.ravendb.demo.model.Condition;
import net.ravendb.demo.model.Patient;
import net.ravendb.demo.presenters.ConditionViewable.ConditionViewListener;

public class ConditionPresenter implements ConditionViewListener {
    private IDocumentSession session;

    public ConditionPresenter() {
        try{IDocumentStore store = new DocumentStore(new String[]{ "http://127.0.0.1:18080" },"Hospital").initialize();
            IDocumentSession session = store.openSession();
            Reference<QueryStatistics> statsRef = new Reference<>();

            IDocumentQuery<Condition> conditions = session.query(Condition.class)
                    .skip(5)
                    .take(5)
                    .statistics(statsRef);

            IDocumentQuery<Patient> query = session.query(Patient.class)
                    .whereStartsWith("firstName", "term")
                    .orElse()
                    .whereStartsWith("lastName", "term")
                    .skip(5)
                    .take(5)
                    .statistics(statsRef);

            IDocumentQuery<DoctorVisit> results = session.query(Patient.class)
                    .groupBy("visits[].doctorId")
                    .selectKey("visits[].doctorId", "doctorId")
                    .selectCount()
                    .whereNotEquals("doctorId", null)
                    .orderByDescending("count")
                    .ofType(DoctorVisit.class)
                    .include("visits[].doctorId");

            System.out.println(session.query(Doctor.class));
            System.out.println(conditions);
            System.out.println(query);
            System.out.println(results);
        }catch(Exception e){}

    }

    @Override
    public void delete(Condition condition) {
        session.delete(condition);
        session.saveChanges();
    }

    @Override
    public Patient getPatientById(String id) {
        Patient patient = session.load(Patient.class, id);
        return patient;
    }

    @Override
    public void save(Condition condition) {
        session.store(condition);
        session.saveChanges();
    }

    @Override
    public Condition getConditionById(String id) {
        return null;
    }

    @Override
    public Pair<Collection<Condition>, Integer> getConditionsList(int offset, int limit, String term) {
        session.advanced().clear();
        Reference<QueryStatistics> statsRef = new Reference<>();
        IDocumentQuery<Condition> conditions = session.query(Condition.class)
                                                      .skip(offset)
                                                      .take(limit)
                                                      .statistics(statsRef);

        if (term != null && term.length() > 0) {
            conditions.whereStartsWith("description", term);
        }

        List<Condition> list = conditions.toList();
        int totalResults = statsRef.value.getTotalResults();

        return new ImmutablePair<Collection<Condition>, Integer>(list, totalResults);

    }

    @Override
    public void openSession() {

        if (session == null) {
            session = RavenDBDocumentStore.getStore().openSession();
            session.advanced().setUseOptimisticConcurrency(true);
        }
    }

    @Override
    public void releaseSession() {
        session.close();
    }

}
