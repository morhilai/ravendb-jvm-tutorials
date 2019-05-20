# RavenDB Hospital Tutorial
RavenDB is an open-source NoSQL document store database. It is fully transactional, multi-platform, and high availability. It supports  clients for a variety of programming languages, including Java. RavenDB is very easy to administer and deploy. The following is an introduction to RavenDB, and an overview of the Client API. As an example, we will be looking at a demo hospital management application built on the Java client.

Contents:
* [How to Install RavenDB Community Edition](./README.md#how-to-install-ravendb-community-edition)
* [How to run the demo](./README.md#how-to-run-the-demo)
* [Entities, tables, collections, and documents](./README.md#entities-tables-collections-and-documents)
* [RavenDB Client API](./README.md#ravendb-client-api)
* [Session and Unit of Work pattern](./README.md#session-and-unit-of-work-pattern)
* [CRUD operations](./README.md#crud-operations)
* [Paging on large record sets](./README.md#paging-through-large-record-sets)
* [BLOB handling - attachments](./README.md#blob-handling---attachments)
* [Queries](./README.md#queries)

## How to Install RavenDB Community Edition
1. Download the zip bundle from https://ravendb.net/download and unzip
2. Register a free community license from https://ravendb.net/buy
3. In PowerShell, run `.\run.ps1` (or `.\setup-as-service.ps1` to launch as a service)
4. Once installed the management studio, called RavenDB Studio, will automatically launch in your default browser on port 8080
5. Click the `about` tab in the bottom left to register your license
6. [Create your first database](https://ravendb.net/docs/article-page/4.2/csharp/studio/server/databases/create-new-database/general-flow)

## How to run the demo
Once RavenDB is installed, start a server instance on port 18080 with this command:
```
./Raven.Server.exe --ServerUrl=http://127.0.0.1:18080 --Setup.Mode=None --License.Eula.Accepted=true
```
Type `openbrowser` to launch the studio in your default browser. [Create a database](https://ravendb.net/docs/article-page/4.2/csharp/studio/server/databases/create-new-database/general-flow)
with the name `Hospital`. Next you'll need to import some configuration data into `Hospital` from a file located in the project root called `hospital.ravendbdump` by following [these instructions](https://ravendb.net/docs/article-page/4.1/java/studio/database/settings/import-data-file).

Fetch the project code sources with:
```
$ git clone https://github.com/sergei-iliev/ravendb.git
```
Once the database is created, the default configuration data is imported, and the sources are available locally, start the application with:
```
$ mvn jetty:run
```
The demo web application will now be available at http://127.0.0.1:8889/. It should look like this:
![App Homepage](/screenshots/p_home.png)

## Entities, tables, collections, and documents
As a NoSQL database, RavenDB manages data in these ways:
* Stores data in JSON documents
* Schemaless - documents can have any structure
* Dynamically generates indexes to facilitate fast data retrieval
* Uses map-reduce to process large sets of documents

Java programmers are used to persisting POJOs by annotating them with `@Entity`. This makes the underlying JPA framework treat the class as a domain object mapped to a row in a database. RavenDB doesn’t use tables. Instead, it represents objects as _documents_, with no constraints on their structure. Similar documents are grouped in _collections_.
In RavenDB, every domain object is mapped to a single document. There is no need for special class treatment other than having a no-args constructor.
The model for this demo consists of 4 entities. To demonstrate the power of grouping and fetching queries in RavenDB, one of these entities is embedded as an array in another entity.

![UML Diagram](/screenshots/uml.png)

Here are the type definitions of these entities on the client side, accompanied by examples of JSON documents on the server side.
Getters and setters are omitted for brevity.

1. The Patient entity:
```java
public class Patient {
    private String id;
    private String firstName,lastName;
    private Date birthDate;
    private Gender gender;

    private String email;
    private Address address;
    private List<Visit> visits;
}
```

A document containing an example Patient, which contains an array of Visits:
```JSON
{
    "firstName": "Megi",
    "lastName": "Devasko",
    "birthDate": "2016-11-30T22:00:00.0000000Z",
    "gender": "FEMALE",
    "email": "sss@box.com",
    "address": null,
    "visits": [
        {
            "date": "2019-02-26T22:00:00.0000000Z",
            "doctorId": "doctors/1-A",
            "type": "HOUSE",
            "visitSummary": "just a minor pain",
            "conditionId": "conditions/1-A",
            "doctorName": "Sergiz Ovesian"
        },
        {
            "date": "2019-01-31T22:00:00.0000000Z",
            "doctorId": "doctors/2-A",
            "type": "EMERGENCYROOM",
            "visitSummary": "never worry",
            "conditionId": "conditions/2-A",
            "doctorName": "Megalo Karimov"
        }
    ],
    "@metadata": {
        "@collection": "Patients",
        "@flags": "HasAttachments",
        "Raven-Java-Type": "net.ravendb.demo.model.Patient"
    }
}
```
2. Visit entity:
```java
public class Visit {
    private Date date;
    private String doctorId;
    private Type type;
    private String visitSummary;
    private String conditionId;
    private String doctorName;
}
```
On the server side this entity is embedded as an array within the Patient documents, see above.

3. Condition entity:
```java
public class Condition {
    private String id;
    private String name;
    private String symptoms;
    private String recommendedTreatment;
}
```
Example Condition:
```JSON
{
    "name": "Diabetes",
    "symptoms": "swollen legs, impaired eyesight",
    "recommendedTreatment": "sugar-free diet",
    "@metadata": {
        "@collection": "Conditions",
        "Raven-Java-Type": "net.ravendb.demo.model.Condition"
    }
}
```
4. Doctor entity:
```java
public class Doctor {
    private String id;
    private String name;
    private String department;
    private int age;
}
```
Example Doctor:
```JSON
{
    "name": "Sergiz Ovesian",
    "department": "LV",
    "age": 45,
    "@metadata": {
        "@collection": "Doctors",
        "Raven-Java-Type": "net.ravendb.demo.model.Doctor"
    }
}
```

If an entity doesn't already have an id field, RavenDB will automatically generate a unique document id on the client side.
By default, entities get autogenerated ids in the following format: `collection/[number_tag]`, which makes them human readable, and makes it simple to ensure they are unique database-wide.

## RavenDB Client API
The Java Client API is added as a dependency to `pom.xml`.
```xml
<dependency>
    <groupId>net.ravendb</groupId>
    <artifactId>ravendb</artifactId>
    <version>LATEST</version>
</dependency>
```
The Client API provides the main API object, the _Document Store_, which sets up the connection with the Server. It is recommended that you create only one Document Store instance per application by implementing the singleton pattern as demonstrated below:
```java
public final class RavenDBDocumentStore {
    private static IDocumentStore store;

    static {
    	store = new DocumentStore(new String[] {
			"http://127.0.0.1:18080",
			"http://127.0.0.1:18081",
			"http://127.0.0.1:18082"},
			"Hospital");

    	store.initialize();
    }

    public static IDocumentStore getStore() {
        return store;
    }
}
```
## Session and Unit of Work Pattern
For any operation we want to perform on RavenDB, we start by obtaining a new _Session_ object from the Document Store.
Much like the Hibernate implementation of JPA, the RavenDB Session implements the Unit of Work pattern. This has several implications in the context of a single session:
* The Session tracks changes for all the entities that it has either loaded or stored
* The Session batches requests to reduce the number of expensive remote calls
* A single document (identified by its id) always resolves to the same instance

In contrast to a Document Store, a Session is a lightweight object and can be created more frequently. This demo uses page attach/detach events to demarcate the Session's creation and release. The session stays open for the duration of page activity. For the purposes of this demo, we will enable optimistic concurrency control.
```java
public void openSession() {
    if (session == null) {
        session = RavenDBDocumentStore.getStore().openSession();
        session.advanced().setUseOptimisticConcurrency(true);
    }
}

public void releaseSession() {
    session.close();
}
```
## CRUD operations
Building on the Client API, this demo application implements the basic CRUD functions.

The create operation inserts a new document. Each document contains a unique id, data, and adjacent metadata - all stored in JSON format. The metadata contains information describing the document, e.g. the last modification date (`@last-modified` property) or the collection  it belongs to (`@collection` property). We will use RavenDB's default algorithm to generate unique ids for our entities.

```java
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
```
Update operation. This method ensures there is at most one portrait profilePicture per Patient.

```java
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
```
Delete operation:
```java
public void delete(Patient patient) {
    session.delete(patient.getId());
    session.saveChanges();
}
```
To 'read' documents, we simply use `session.load(/*documentId*/)`

## Paging Through Large Record Sets
Paging through large amounts of data is one of the most common operations in RavenDB. A typical scenario is the need to display results in batches in a lazy loading or pageable grid. In this app, the grid is configured to first obtain the total amount of records to show and then to lazily obtain records by batches of 10 as the user navigates from page to page. There is a convenient method, `statistics`, to obtain the total count of the documents querying at the same time thus making a one-time remote request only! For the patients grid, the corresponding attachments are also obtained and streamed into a convenient byte array to show in one of the grid columns.

![Patient CRUD](/screenshots/p_paging.png)

```java
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
todo
public Pair<Collection<PatientAttachment>,Integer> getPatientsList(int offset, int limit, boolean order) {

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

    Collection<PatientAttachment> patientWithPictures=new ArrayList<>();

    for (Patient patient : list) {
        PatientAttachment patientWithPicture=new PatientAttachment(patient);
        AttachmentName[] names = session.advanced().attachments().getNames(patient);

        if (names.length > 0) {
            try (CloseableAttachmentResult result = session.advanced().attachments().get(patient,
							                                names[0].getName())) {
	            Attachment profilePicture = new Attachment();
	            profilePicture.setName(names[0].getName());
	            profilePicture.setMimeType(names[0].getContentType());

                byte[] bytes = IOUtils.toByteArray(result.getData());
                profilePicture.setBytes(bytes);
                patientWithPicture.setAttachment(profilePicture);
	        } catch (IOException e) {
	            logger.log(Level.SEVERE,"", e);
            }
        }
        patientWithPictures.add(patientWithPicture);
    }

    return new ImmutablePair<Collection<PatientAttachment>, Integer>(patientWithPictures, totalResults);
}
```

## BLOB Handling - Attachments
Binary data that cannot be stored as JSON (such as images, audio, etc.) can be associated with a document as one or more _attachments_.
Attachments can be loaded and edited separately from the document.
This POJO represents attachments on the client side:
```java
public class Attachment {

    String name;
    String mimeType;
    byte[] bytes;

    public InputStream getInputStream() {
        return new ByteArrayInputStream(bytes);
    }

    public StreamResource getStreamResource() {
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		return new StreamResource(name, () -> bis);
    }
}
```
(Getters and setters are omitted for brevity)

In the Patient entity, a portrait of the patient is attached to the document using the `Session.Advanced.Attachments.Store` method.
Attachments, just like documents, are tracked by the session and will only be saved to the Server when `Session.SaveChanges` is called.
Changes to attachments and changes to documents made in the context of the same session will be executed as part of one ACID transaction.

```java
Patient patient = patientWithPicture.getPatient();
Attachment profilePicture = patientWithPicture.getAttachment();

InputStream inputStream = profilePicture.getInputStream();
String name = profilePicture.getName();
String mimeType = profilePicture.getMimeType();
session.advanced().attachments().store(patient,name,inputStream,mimeType);
session.saveChanges();
```

This operation gets an profilePicture from a patient document:
```java
try (CloseableAttachmentResult result = session.advanced().attachments().get(patient,names[0].getName())) {
    Attachment profilePicture = new Attachment();
    profilePicture.setName(names[0].getName());
    profilePicture.setMimeType(names[0].getContentType());

    byte[] bytes = IOUtils.toByteArray(result.getData());
    profilePicture.setBytes(bytes);
    patientWithPicture.setAttachment(profilePicture);
}
```

## Queries
todo

from Doctors
from Conditions
from Patients where startsWith(firstName, $p0) or startsWith(lastName, $p1)
from Patients group by visits[].doctorId where doctorId != $p0 order by count desc select visits[].doctorId as doctorId, count() as count include 'visits[].doctorId'

RavenDB uses indexes, but they don't work quite like relational database indexes. The main difference is that RavenDB's indexes are schema-less and documented oriented. RavenDB requires indexes to execute queries, but the programmer is not required to manually create them - RavenDB can automatically create the required index by analyzing query at runtime. In the following query, the parameter `Patient.class` defines the type of returned results, and also indicates that the queried collection will be Patients.
```java
Patient patient = session.load(Patient.class, id);
return patient;
```
When one document contains the id of another document, both of them can be loaded in a single request call using the 'Include + Load' methods. The following code snippet shows how to obtain Patient visit data and the associated Doctor documents with a single request.
When the Doctors documents are requested they are fetched from the local session cache, avoiding a second round trip to the server.
The query transforms the Patients visits data into a custom class of type `DoctorVisit` by using projection `ofType`. This is a powerful technique to construct any result type of the queried data.
```java
public Collection<DoctorVisit> getDoctorVisitsList() {
    List<DoctorVisit> results = session.query(Patient.class)
                                        .groupBy("visits[].doctorId")
                                        .selectKey("visits[].doctorId", "doctorId")
                                        .selectCount()
                                        .whereNotEquals("doctorId", null)
                                        .orderByDescending("count")
                                        .ofType(DoctorVisit.class)
                                        .include("visits[].doctorId")
                                        .toList();
    // fetch doctors by batch

    Set<String> doctorIds = results.stream().map(p -> p.getDoctorId()).collect(Collectors.toSet());
    Map<String, Doctor> map = session.load(Doctor.class, doctorIds);

    results.forEach(v -> {
        v.setDoctorName(map.get(v.getDoctorId()).getName());
    });

    assert (session.advanced().getNumberOfRequests() == 1);
    return results;
}
```

