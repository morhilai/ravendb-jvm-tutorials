package net.ravendb.demo.presenters;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import net.ravendb.client.documents.session.IDocumentQuery;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.QueryStatistics;
import net.ravendb.client.primitives.Reference;
import net.ravendb.demo.command.PatientVisit;
import net.ravendb.demo.db.RavenDBDocumentStore;
import net.ravendb.demo.model.Patient;
import net.ravendb.demo.presenters.VisitsViewable.VisitsViewListener;

public class VisitsPresenter implements VisitsViewListener {

    private IDocumentSession session;

    public VisitsPresenter() {}

    @Override
    public Pair<Collection<PatientVisit>, Integer> getVisitsList(int offset, int limit, boolean order) {
//        session.advanced().clear();
        Reference<QueryStatistics> statsRef = new Reference<>();
        IDocumentQuery<PatientVisit> visits = session.query(Patient.class)
                .groupBy("visits[].doctorName", "visits[].date", "firstName", "lastName", "visits[].visitSummary")
                .selectKey("visits[].doctorName", "doctorName").selectKey("visits[].date", "date")
                .selectKey("visits[].visitSummary", "visitSummary").selectKey("firstName", "firstName")
                .selectKey("lastName", "lastName")
                .selectCount()
                .ofType(PatientVisit.class)
                .whereNotEquals("date", null)
                .skip(offset)
                .take(limit)
                .statistics(statsRef);

        if (order) {
            visits.orderByDescending("date");
        } else {
            visits.orderBy("date");
        }

        List<PatientVisit> list = visits.toList();

        int totalResults = statsRef.value.getTotalResults();

        return new ImmutablePair<Collection<PatientVisit>, Integer>(list, totalResults);
    }


    @Override
    public Pair<Collection<PatientVisit>, Integer> searchVisitsList(int offset, int limit, String term, boolean order) {
//        session.advanced().clear();
        Reference<QueryStatistics> statsRef = new Reference<>();
        IDocumentQuery<PatientVisit> visits = session.advanced().documentQuery(Patient.class)
                .groupBy("visits[].doctorName", "visits[].date", "firstName", "lastName", "visits[].visitSummary")
                .selectKey("visits[].doctorName", "doctorName").selectKey("visits[].date", "date")
                .selectKey("visits[].visitSummary", "visitSummary").selectKey("firstName", "firstName")
                .selectKey("lastName", "lastName")
                .selectCount()
                .ofType(PatientVisit.class)
                .whereNotEquals("date", null)
                .whereStartsWith("doctorName", term)
                .skip(offset)
                .take(limit)
                .statistics(statsRef);

        if (order) {
            visits.orderByDescending("date");
        } else {
            visits.orderBy("date");
        }

        List<PatientVisit> list = visits.toList();
        int totalResults = statsRef.value.getTotalResults();

        return new ImmutablePair<Collection<PatientVisit>, Integer>(list, totalResults);
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
