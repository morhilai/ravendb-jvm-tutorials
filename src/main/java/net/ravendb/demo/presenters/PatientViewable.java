package net.ravendb.demo.presenters;

import java.util.Collection;

import net.ravendb.demo.command.PatientWithPicture;
import org.apache.commons.lang3.tuple.Pair;

import net.ravendb.demo.model.Address;

public interface PatientViewable {

    public interface PatientViewListener {
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
}
