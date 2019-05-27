package net.ravendb.demo.presenters;

import net.ravendb.demo.command.DoctorVisit;
import net.ravendb.demo.command.PatientVisit;
import net.ravendb.demo.command.PatientWithPicture;
import net.ravendb.demo.model.*;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;

public interface ViewListener {

    interface ConditionViewListener {

        Condition getConditionById(String id);

        Patient getPatientById(String id);

        void save(Condition condition);

        void delete(Condition condition);

        Pair<Collection<Condition>, Integer> getConditionsList(int offset, int limit, String term);

        void openSession();

        void releaseSession();
    }

    interface DoctorViewListener {

        Collection<Doctor> getDoctorsList();

        Collection<String> getDepartments();

        void save(Doctor doctor);

        void delete(Doctor doctor);

        Collection<DoctorVisit> getDoctorVisitsList();

        void openSession();

        void releaseSession();
    }

    interface PatientViewListener {

        Pair<Collection<PatientWithPicture>, Integer> getPatientsList(int offset, int limit, boolean order);

        Collection<String> getRegionsList();

        void create(PatientWithPicture patientWithPicture);

        void update(PatientWithPicture patientWithPicture);

        void save(String patientId, Address address);

        void delete(PatientWithPicture patient);

        Pair<Collection<PatientWithPicture>, Integer> searchPatientsList(int offset, int limit, String term, boolean order);

        void openSession();

        void releaseSession();
    }

    interface PatientVisitViewListener {

        Collection<PatientVisit> getVisitsList(String patientId, String term, boolean order);

        Condition getConditionById(String conditionId);

        void save(String patientId, Visit visit);

        Patient getPatientById(String id);

        Collection<Doctor> getDoctorsList();

        Collection<Condition> getConditionsList();

        Collection<String> getLocationsList();

        void openSession();

        void releaseSession();
    }

    interface VisitsViewListener {

        Pair<Collection<PatientVisit>, Integer> getVisitsList(int offset, int limit, boolean order);

        Pair<Collection<PatientVisit>, Integer> searchVisitsList(int offset, int limit, String term, boolean order);

        void openSession();

        void releaseSession();
    }
}
