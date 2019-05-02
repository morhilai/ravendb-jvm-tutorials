# RavenDB Hospital Tutorial
RavenDB is an open-source NoSQL document store database. It is fully transactional, multi-platform, and high availability. It supports clients for a variety of programming languages, 
including Java. RavenDB is very easy to administer and deploy. The following is an example hospital management app that uses the RavenDB Java client to communicate with the server.  

As a NoSQL database, RavenDB manages data in these ways:  
* Stores data in JSON documents  
* Schemaless - the structure of a document can be changed by simply adding new fields or deleting existing ones  
* Dynamically generates indexes to facilitate fast data retrieval  
* Uses map-reduce to process large sets of documents  

Contents:  
* [How to Install RavenDB Community Edition]()  
* [How to run the demo]()  
* [Domain Entity description]()  
* [Session and Unit of Work pattern]()  
* [CRUD operations]()  
* [Paging on large record sets]()  
* [BLOB handling - attachments]()  
* [Queries]()  

## How to Install RavenDB Community Edition  
1. Download the zip bundle from https://ravendb.net/download and unzip in a local drive folder  
2. Register a community edition free license from https://ravendb.net/buy  
3. In PowerShell, run either .\run.ps1 (console mode app) or .\setup-as-service.ps1 (service mode app) and follow the installation instructions  
4. Once installed, the RavenDB Studio will automatically launch in your default browser. Open the 'about' tab to register your license  
5. Create your first database  

## How to run the demo  
Once RavenDB is installed, start a server instance on port 18080 with this command:  
```
./Raven.Server.exe `
--ServerUrl=http://127.0.0.1:18080
```
Type `openbrowser` to launch the studio in your default browser. [Create a database](https://ravendb.net/docs/article-page/4.2/csharp/studio/server/databases/create-new-database/general-flow) 
with the name 'Hospital'. In the project root, there is an import file `hospital.ravendbdump`. Follow [these instructions](https://ravendb.net/docs/article-page/4.1/java/studio/database/settings/import-data-file) 
to import `hospital.ravendbdump` into 'Hospital'.  

The project code sources can be fetched from GitHub using this git tool command:  
```
$ git clone https://github.com/sergei-iliev/ravendb.git
```
Once the database is created, the default configuration data is imported, and the sources are available locally, start the application by executing:  
```
$ mvn jetty:run
```
The web app will now be available at http://127.0.0.1:8889/  
![App Homepage](/screenshots/p_home.png)

## Entities, tables, collections, and documents
To persist data, Java programmers usually annotate Java POJOs with @Entity so that the underlying JPA framework will treat the class as a domain object mapped to a row in a database. 
RavenDB doesn’t use tables. Instead, it represents objects as _documents_, with no constraints on their structure. Similar documents are grouped in _collections_ rather than tables.
In RavenDB, every domain object is mapped to a single document. In this regard, there is no need for special class treatment other than having a default no-args constructor. 
This sample model consists of 4 basic entities. To demonstrate the power of grouping and fetching queries in RavenDB, one of these entities is embedded as an array in another entity.

![UML Diagram](/screenshots/uml.png)
1. Client-side Patient entity:  
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
(Getters and setters are omitted for brevity)

Server-side JSON representation of a sample Patient (containing an array of Visits):
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
2. Client-side Visit entity:
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

3. Condition - list of available conditions
```java
public class Condition {
    private String id;
    private String name;
    private String symptoms;
    private String recommendedTreatment;

}
```
Server-side JSON representation of a sample Condition
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
4. Doctor - stored in a separate collection
```java
public class Doctor{
        private String id;
        private String name;
        private String department;
       private int age; 
 }
 ```
 JSON representation of Doctor document at RavenDB side
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

If an entity doesn't already have an id (represented by a field called 'id'), RavenDB will automatically generate a unique document id on the client side. 
By convention, entities get autogenerated ids in the following format: 'collection/[number tag]', so the programmer doesn't need to be concerned with the
uniqueness of each document within a collection.

## RavenDB Client API
The Java Client API is added as a dependency to pom.xml. 
```
<dependency>
  <groupId>net.ravendb</groupId>
  <artifactId>ravendb</artifactId>
  <version>LATEST</version>
</dependency>
```
It provides the main API object, the _Document Store_, which sets up the connection with the Server and downloads various configuration metadata. The Document Store is capable of
working with multiple databases; it is recommended that you create only one instance of it per application by implementing the [singleton pattern]() as demonstrated below:
```java
public final class RavenDBDocumentStore {
    
    private static IDocumentStore store;

    static {
    store = new DocumentStore(new String[]{ 
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
## Session and Unit of Work pattern
For any operation we want to perform on RavenDB, we start by obtaining a new _Session_ object from the Document Store.
Much like the Hibernate implementation of JPA, the RavenDB Session also implements the Unit of Work pattern. This has several implications in the context of a single session:
* The Session tracks changes for all the entities that it has either loaded or stored  
* The Session batches requests to reduce the number of expensive remote calls  
* A single document (identified by its ID) always resolves to the same instance  

In contrast to a Document Store, a Session is a lightweight object and can be created more frequently. This demo application uses page attach/detach events to demarcate the Session's
creation and release. The session stays open for the duration of page activity. For the purposes of this demo, we will use optimistic concurrency control.
```java
public void openSession() {
    if(session==null){
        session = RavenDBDocumentStore.getStore().openSession();
        session.advanced().setUseOptimisticConcurrency(true);
    }
}

public void releaseSession() {
    session.close();
}
```
## CRUD operations
An example patient entity:

![Patient CRUD](/screenshots/p_edit.png)

The create operation inserts a new document. Each document contains a unique ID that identifies it, data, and adjacent metadata - all stored in JSON format. The metadata contains information 
describing the document, e.g. the last modification date (`@last-modified` property) or the collection  it belongs to (`@collection` property). As already mentioned, we will use RavenDB's  
default algorithm to generate a unique ID for our entities.  

```java
public void create(PatientAttachment patientAttachment) {
    Patient patient=patientAttachment.getPatient();
    Attachment attachment=patientAttachment.getAttachment();
    session.store(patient);

    if (attachment != null) {
        session.advanced().attachments().store(patient, attachment.getName(),
                attachment.getInputStream(), attachment.getMimeType());
    }

    session.saveChanges();
}
```
Update operation. The method also handles attachments as a 1:1 relationship with each patient.

```java
public void update(PatientAttachment patientAttachment) throws ConcurrencyException {
    Patient patient=patientAttachment.getPatient();
    Attachment attachment=patientAttachment.getAttachment();
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
```

```java
public void delete(Patient patient) {
    session.delete(patient.getId());
    session.saveChanges();
}
```

## Paging on large record sets
Paging through large amounts of data is one of the most common operations in RavenDB. A typical scenario is the need to display results in batches in a lazy loading or pageable grid. In this app, the grid
is configured to first obtain the total amount of records to show and then to lazily obtain records by batches of 10 as the user navigates from page to page. There is a convenient method, `statistics`, to
obtain the total count of the documents querying at the same time thus making a one-time remote request only! For the patients grid, the corresponding attachments are also obtained and
streamed into a convenient byte array to show in one of the grid columns.  

![Patient CRUD](/screenshots/p_paging.png)

```java
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
        
        Collection<PatientAttachment> patientAttachments=new ArrayList<>();
        for (Patient patient : list) {
            PatientAttachment patientAttachment=new PatientAttachment(patient);
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
                    logger.log(Level.SEVERE,"", e);
                }
            }
            patientAttachments.add(patientAttachment);
        }
    return new ImmutablePair<Collection<PatientAttachment>, Integer>(patientAttachments, totalResults);
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

    public InputStream getInputStream(){
        return new ByteArrayInputStream(bytes);
    }
    public StreamResource getStreamResource(){
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		return new StreamResource(name, () -> bis);
    }
}
```
(Getters and setters are omitted for brevity)

In the Patient entity, a portrait of the patient is attached to the document using the `Session.Advanced.Attachments.Store` method.
Attachments, just like documents, are tracked by the session and will be only saved to the Server when `Session.SaveChanges` is executed.
Changes to attachments and changes to documents made in the context of the same session will be executed as part of one ACID transaction.

```java
Patient patient = patientAttachment.getPatient();
Attachment attachment = patientAttachment.getAttachment();

InputStream inputStream = attachment.getInputStream();
String name = attachment.getName();
String mimeType = attachment.getMimeType();
session.advanced().attachments().store(patient,name,inputStream,mimeType);
session.saveChanges();
```

This operation gets an attachment from a patient document:
```java
try(CloseableAttachmentResult result= session.advanced().attachments().get(patient,names[0].getName())){
    Attachment attachment = new Attachment();
    attachment.setName(names[0].getName());
    attachment.setMimeType(names[0].getContentType());
    byte[] bytes = IOUtils.toByteArray(result.getData());
    attachment.setBytes(bytes);
    patientAttachment.setAttachment(attachment);
}
```

## Queries
RavenDB uses indexes to execute queries, but they don't work the same way as relational database indexes. The main difference is that RavenDB's indexes are schema-less and documented oriented.
RavenDB requires an index to solve a query. The great think is that a programmer is not required to manually create indexes - RavenDB can deduct and create
required index dynamically, by analyzing query at run time. All query samples that follow are based on dynamic indexes, generated by RavenDB's search engine. 
The provided Patient type as the generic type parameter does not only define the type of returned results, but it also indicates that the queried collection will be Patients.  
```java
        
    Patient patient = session.load(Patient.class, id);
    return patient;
        
```
When there is a 'relationship' between documents, those documents can be loaded in a single request call using the `Include + Load` methods. The following code snippet shows how to obtain Patient visit data and the associated Doctor document with a single request.
When the Doctors documents are requested they are fetched from the local session cache thus avoiding a second round trip to the server.
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

