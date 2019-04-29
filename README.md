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
To persist data, Java programmers tend to annotate Java POJOs with @Entity so that the underlying JPA framework will treat the class as a domain object mapped to a row in a database. 
RavenDB doesn’t use tables. Instead, it represents objects as _documents_, with no constraints on their structure. Similar documents are grouped in _collections_.
In RavenDB, every domain object is mapped to a single document. In this regard, there is no need for special class treatment other than having a default no-args constructor. 
The sample model consists of 4 basic entities. One of these is embedded into another entity as an array to demonstrate the power of grouping and fetching queries in RavenDB.

![UML Diagram](/screenshots/uml.png)
1. Patient - stored as a separate collection
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
JSON representation of Patient document at RavenDB side
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
2. Visit - stored as an array in Patient collection  
```java
public class Visit{
    private Date date;
    private String doctorId;    
    private Type type;
    private String visitSummary;
    private String conditionId;
    private String doctorName;

}    
```
The JSON representation of Visit document at RavenDB side is an array of documents in the Patient document(look above)

3. Condition - list of available conditions
```java
public class Condition {
    private String id;
    private String name;
    private String symptoms;
    private String recommendedTreatment;

}
```
JSON representation of Condition document at RavenDB side
```JSON
{
    "name": "Diabetes",
    "symptoms": "swollen legs, eye sight",
    "recommendedTreatment": "carbon free diet",
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
If the entity doesn't already have an id (represented by a field called 'id'), RavenDB will automatically generate a unique document id on the client side. 
By convention, entities get autogenerated ids in the following format: 'collection/[number tag]', so the programmer is not concerned with the uniqueness of each document within a collection.

## RavenDB Client API
The Java Client API is added as a dependency to pom.xml. 
```
<dependency>
  <groupId>net.ravendb</groupId>
  <artifactId>ravendb</artifactId>
  <version>LATEST</version>
</dependency>
```
It provides the main API object, the Document Store, which sets up the connection with the Server and downloads various configuration metadata.
The Document Store is capable of working with multiple databases; it is recommended that you create only one instance of it per application by implementing the [singleton pattern]() as demonstrated below:
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
For any operation we want to perform on RavenDB, we start by obtaining a new _Session_ object from the Document Store. The Session contains everything we need to perform any operation necessary. Much like the Hibernate implementation of JPA, the RavenDB Session also implements the Unit of Work pattern. This has several implications in the context of a single session:
* The Session batches requests to reduce the number of expensive remote calls.
* A single document (identified by its ID) always resolves to the same instance.
* The Session tracks changes for all the entities that it has either loaded or stored.

In contrast to a Document Store, a Session is a lightweight object and can be created more frequently. For example, in web applications, the recommended pattern is to create one session per request.
This demo application uses page attach/detach events to demarcate the Session's creation and release. The session stays open for the duration of page activity.
```java
public void openSession() {
    if(session==null){
         session = RavenDBDocumentStore.getStore().openSession();
    }
}
public void releaseSession() {
    session.close();
}
```
## CRUD operations
This patient entity is given as an example only. 

![Patient CRUD](/screenshots/p_edit.png)

Create operation inserts a new document. Each document contains a unique ID that identifies it, data and adjacent metadata, both stored in JSON format. The metadata contains information describing the document, e.g. the last modification date (`@last-modified` property) or the collection (`@collection` property) assignment. As already mentioned we will use RavenDB's default algorithm to generate a unique ID for our entities. 

```java
public void create(PatientAttachment patientAttachment) {
    Patient patient = patientAttachment.getPatient();
    Attachment attachment = patientAttachment.getAttachment();
    session.store(patient);

    if (attachment != null) {
        session.advanced().attachments().store(patient, attachment.getName(),
                    attachment.getInputStream(), attachment.getMimeType());
    }
        
    session.saveChanges();
    session.advanced().clear();
}
```
Update operation is worth noting - it handles optimistic concurrency control and throws `ConcurrecyException` if another 
update has already changed the record. The method also handles attachments as a 1:1 relationship with each patient. 

```java
public void update(PatientAttachment patientAttachment) throws ConcurrencyException {
    // enable oca
    session.advanced().setUseOptimisticConcurrency(true);
    Patient patient=patientAttachment.getPatient();
    Attachment attachment=patientAttachment.getAttachment();
    session.store(patient);

    // delete previous attachments
    AttachmentName[] names = session.advanced().attachments().getNames(patient);
    if (names.length > 0) {
        session.advanced().attachments().delete(patient, names[0].getName());
    }
    session.saveChanges();

    if (attachment != null) {
        session.advanced().attachments().store(patient, 
            attachment.getName(),
            attachment.getInputStream(),
            attachment.getMimeType());
    }

    session.saveChanges();
    session.advanced().clear();
}
```

```java
public void delete(Patient patient) {
        
    session.delete(patient.getId());
    session.saveChanges();     
        
}
```

## Paging on large record sets
Paging through large amounts of data is one of the most common operations with RavenDB. A typical scenario is the need to display results in chunks in a lazy loading or pageable grid. The grid is configured to first obtain the total amount of records to show and then as the user scrolls up and down to lazily obtain records by batches of 50. There is a convenient statistics method to obtain the total count of the documents while querying at the same time thus making a one-time remote request only! For the patients grid, the corresponding attachments are also obtained and streamed into a convenient byte array to show in one of the grid columns. 

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

## BLOB handling - attachments
When binary data(images, documents, media) needs to be associated with the document, RavenDB provides the `Attachment API`.
Attachments are completely decoupled from documents. They can be updated and changed separately from the document and do not participate in transactions. Following POJO represents attachments on the client side.
```java
public class Attachment {

    String name;
    String mimeType;
    byte[] bytes; 
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    public byte[] getBytes(){
        return bytes;
    }
    
    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
    public InputStream getInputStream(){
        return new ByteArrayInputStream(bytes);
    }
    public StreamResource getStreamResource(){
      ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
      return new StreamResource(name, () -> bis);
    }
    
}
```
In Patient entity, image is attached to the document using the `Session.Advanced.Attachments.Store` method.
Attachments, just like documents, are a part of the session and will be only saved on the Server when `DocumentSession.SaveChanges` is executed.

```java
    Patient patient=patientAttachment.getPatient();
    Attachment attachment=patientAttachment.getAttachment();

        InputStream inputStream=attachment.getInputStream();
    String name=attachment.getName();
    String mimeType=attachment.getMimeType();
    session.advanced().attachments().store(patient,name,inputStream,mimeType);
    session.saveChanges();
```

This operation is used to get an attachment from a patient document.
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
RavenDB uses indexes to retrieve data but they don't work the same way as relational database indexes work. The main difference is that RavenDB's indexes are schema-less documented oriented. RavenDB requires an index to solve a query;that is why indexe is needed. The great think is that a programmer is not required to manually create indexes - RavenDB can deduct and create required index dynamically, by analyzing query at run time.
All query samples that follow are based on dynamic indexes, generated by RavenDB's search engine. 
The provided Patient type as the generic type parameter does not only define the type of returned results, but it also indicates that the queried collection will be Patients.
```java
        
    Patient patient = session.load(Patient.class, id);
    return patient;
        
```
When there is a 'relationship' between documents, those documents can be loaded in a single request call using the `Include + Load` methods. The following code snippet shows how to obtain Patient's visit data and associated Doctor in a single session request.
When the Doctors collection is requested it is fetched from the local session cache thus avoiding a second round trip to RavenDB.
The query transforms the Patients visits data into custom result class of type DoctorVisit by using projection `ofType`. This is a powerful technique to construct any result type of the queried document collection. 
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

