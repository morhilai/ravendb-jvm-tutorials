# RavenDB Java Client Tutorial
RavenDB is an open-source NoSQL document store database. It is fully transactional, multi-platform, and highly available.
It supports clients for a variety of programming languages, including Java. RavenDB is very easy to administer and deploy.
The following tutorial is an introduction to RavenDB and an overview of the Client API. We will be looking at a demo hospital
management app built on the Java client.

This app is implemented using the [Vaadin Flow](https://vaadin.com/flow) framework.
This framework addressed both client and server side development of web applications, allowing to implement using java only. //todo: make it readable(sorry)
This application implements the MVP pattern, which in combination of Vaadin's page state persistence is very convenient to demonstrate the Unit Of Work features of RavenDB.

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
1. Register a free community license from https://ravendb.net/buy
2. Download the zip bundle from https://ravendb.net/download and extract
3. In PowerShell, type `.\run.ps1` (or `.\setup-as-service.ps1` to launch as a service)
4. Once installed, the [Setup Wizard](https://ravendb.net/docs/article-page/4.2/java/start/installation/setup-wizard) will
automatically launch on your default browser
5. After setup, the [RavenDB Management Studio](https://ravendb.net/docs/article-page/4.2/java/studio/overview) will launch.
Click the `about` tab to register your license:

![Register License](/screenshots/manage-license-1.png)

More detailed installation and setup instructions can be found in [RavenDB's online documentation](https://ravendb.net/docs/article-page/4.2/java/start/getting-started).

## How to run the demo
Once RavenDB is installed, start a server instance with this command:
```
./Raven.Server.exe --ServerUrl=http://127.0.0.1:18080 --Setup.Mode=None --License.Eula.Accepted=true
```
(This will launch the server instance on port 18080 and skip the [Setup Wizard](https://ravendb.net/docs/article-page/4.2/java/start/installation/setup-wizard)
and license agreement prompt)

Type `openbrowser` to launch the management studio in your default browser.
[Create a new database](https://ravendb.net/docs/article-page/4.2/java/studio/server/databases/create-new-database/general-flow)
with the name `Hospital`. Next you'll need to import some configuration data into `Hospital` from a file located in the
project root called `hospital.ravendbdump` by going to Settings > Import Data:

![Import Data](/screenshots/ravendbdump.png)

Fetch the code sources for this project with:
```
$ git clone https://github.com/ravendb/ravendb-jvm-tutorials.git
```
Once the database is created, the default configuration data is imported, and the sources are available locally, start the
application with:
```
$ mvn jetty:run todo?
```
The demo web application will now be available at http://127.0.0.1:8889/. It should look like this:
![App Homepage](/screenshots/p_home.png)

## Entities, tables, collections, and documents
Java programmers are used to persisting POJOs by annotating them with `@Entity`. This makes the underlying JPA framework
treat the class as a domain object mapped to a row in a database. RavenDB doesn’t use tables. Instead, it represents objects
as _documents_, with no constraints on their structure. Similar documents are grouped in _collections_. In RavenDB, every
domain object maps to a single document. There is no need for special class treatment other than having a no-args constructor.
The model for this demo consists of 4 entities. To demonstrate the power of grouping and fetching queries in RavenDB, one
of these entities is embedded as an array in another entity.

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
A JSON document containing an example Patient, which contains an array of Visits:
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
On the server side this entity is embedded as an array within Patient documents, see above example Patient.

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
By default, ids are generated in the following format: `collection/[number_tag]`, which makes them human readable, and
helps to ensure that they are unique database-wide.

## RavenDB Client API
The Java Client is included as a dependency in `pom.xml`.
```xml
<dependency>
    <groupId>net.ravendb</groupId>
    <artifactId>ravendb</artifactId>
    <version>LATEST</version>
</dependency>
```
The Client API provides the main API object, the _Document Store_, which sets up the connection with the Server. It is
recommended that you create only one Document Store instance per application by implementing the singleton pattern as
demonstrated below:
```java
public final class RavenDBDocumentStore {
    private static IDocumentStore store;

    static { //todo
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
Much like the Hibernate implementation of JPA, the RavenDB Session implements the Unit of Work pattern. This has several
implications in the context of a single session:
* The Session tracks changes for all the entities that it has either loaded or stored
* The Session batches requests to reduce the number of expensive remote calls
* A single document (identified by its id) always resolves to the same instance

//todo: move somewhere else: In contrast to a Document Store, a Session is a lightweight object and can be created more frequently.

This demo uses Vaadin infrastructure for //todo: explain what it uses, what is the boundaries of a page life, when onAttach and onDetach happens

The session stays open for the duration of page activity. This demo uses page attach/detach events to demarcate the Session's creation and release. For the purposes of this demo, we will enable optimistic concurrency control.  todo
```java
public void openSession() {
    if (session == null) {
        session = RavenDBDocumentStore.getStore().openSession();
        session.advanced().setUseOptimisticConcurrency(true); todo
    }
}

public void releaseSession() {
    session.close();
}
```
## CRUD operations

// todo: start from explaining that we are going to show Patient crud, that is in the SO AND SO PRESETER that is being held in the so and so view.
Building on the Client API, this demo application implements the basic CRUD functions.

The **create** operation inserts a new document. Each document contains a unique id, data, and adjacent metadata - all stored
in JSON format. The metadata contains information describing the document, such as the last modification date (`@last-modified`)
or the collection it belongs to (`@collection`). We will use RavenDB's default algorithm to generate unique ids for our
entities.


```java
public void create(PatientWithPicture patientWithPicture) { todo
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
**Update** operation. This method ensures there is at most one profile picture per Patient.
// todo: talk about entity tracking after creation etc.
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
**Delete** operation:
// todo: talk about entity tracking after creation etc.
```java
public void delete(Patient patient) {
    session.delete(patient.getPatient());
    session.saveChanges();
}
```
To **read** documents, we simply use `session.load([some_document_id])`

## Paging Through Large Record Sets
Paging through large amounts of data is one of the most common operations in RavenDB. For example, let's say we need to
display results in batches in a lazy loading or pageable grid. In this app, the grid is configured to obtain the
total amount of results to show and then to lazily load the results in batches of 10 as the user navigates from page to page.
We can use `statistics()` to access useful data about the query, including the total number of results. For the patients
grid, the corresponding attachments are also obtained and streamed into a byte array.

![Patient Paging](/screenshots/p_paging.png)

`getPatientsList()`, with error handling omitted for brevity:
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
}
```

## BLOB Handling - Attachments
Binary data that cannot be stored as JSON (such as images, audio, etc.) can be associated with a document as one or more _attachments_.
Attachments can be loaded and edited separately from the document. This POJO represents attachments on the client side:
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

## Queries TODO
In RavenDB, a query can only be satisfied by an index. If no appropriate index exists, RavenDB will automatically create
one on the fly. The indexes are continuously optimized to satisfy all previous queries. By default,
indexes are updated asynchronously so that they don't impact write speed. In most use cases, this improves performance,
but you can always have a write operation wait for the index to update before returning using
`session.advanced().waitForIndexesAfterSaveChanges();`.

Queries are created using `session.query()` and executed by calling `.toList()`. Before being sent to the server, every
query is translated to [RQL (RavenDB Query Language)](https://ravendb.net/docs/article-page/4.2/java/indexes/querying/what-is-rql)
- our SQL-like language. RQL was designed to expose RavenDB's query pipeline to the user. You can write your queries in RQL
using `session.advanced().rawQuery([RQL_string_here])`. A query can be converted to RQL by calling `.toString()` before
`.toList()`. Here are some example queries constructed in java, accompanied by the equivalent RQL:

### 1. Retrieve all documents from a collection
```java
session.query(Doctor.class).toList();
```
This is the simplest possible query, with no filtering, paging, or projection. The parameter `Doctor.class` indicates the
type of the entities retrieved, and also that the collection being queried is `Doctors`.

Equivalent RQL:
```SQL
from Doctors
```

### 2. Paging and query statistics
```java
Reference<QueryStatistics> myQueryStats = new Reference<>();
IDocumentQuery<Patient> query = session.query(Patient.class)
                                       .skip(20)
                                       .take(10)
                                       .statistics(myQueryStats)
                                       .toList();
```
The grid that displays our list of patients holds 10 patients per page. To display the third page, we tell
our query to `.skip(20)` - skip the first two pages of patients - and `.take(10)` - send the next ten patients to our
application.

We also need to know how many pages there are in total so we can render the page buttons. Some useful data, called the
_query statistics_, are automatically sent to the client along with the response to each query. To access them we need to
call `.statistics(myQueryStats)` on our query, and then we get the total number of results with `myQueryStats.value.getTotalResults()`.

Equivalent RQL:
```SQL
from Patients
limit 20,10
```
The command `limit` takes the number of results to skip as its first argument, and the number of results to keep as its
second. No request for query statistics needs to be sent to the server, they are sent automatically.

### 3.Filtering
```java
session.query(Patient.class)
       .whereEquals("firstName", "John")
       .andAlso()
       .whereEquals("lastName", "Doe")
       .toList();
```
This query retrieves patients from the `Patients` collection only if their name is "John Doe".

Equivalent RQL:
```SQL
from Patients
where firstName = 'John' and lastName = 'Doe'
```

### 4. Projecting, aggregating, and including related documents
```java
session.query(Patient.class)
       .groupBy("visits[].doctorId")
       .selectKey("visits[].doctorId", "doctorId")
       .selectCount()
       .whereNotEquals("doctorId", null)
       .orderByDescending("count")
       .ofType(DoctorVisit.class)
       .include("visits[].doctorId")
       .toList();
```
In this example query from the demo, we want to rank doctors by the number of visits they have scheduled with patients.
We perform a map-reduce operation on the `Patients` collection, grouping by doctors. However, since we don't want all the
data in the patient documents, it would be a waste to send it over the network. With the first argument in
`.selectKey("visits[].doctorId", "doctorId")` we retrieve only the doctorIds listed in the patients' visit arrays. With the second
argument, we give the field a shorter, more readable name - `doctorId`. With `.selectCount()`, we retrieve the result of the
map-reduce, the number of visits per doctor. With `.ofType(DoctorVisit.class)` we define the type of the resulting projection:
a custom class that contains only a `doctorId`, `doctorName`, and `count`.

Finally, this query demonstrates how to load related documents. RavenDB doesn't support joins like SQL databases do, but
if one document contains the id of another document, that document can be included in the query response without an additional
request to the server.

Equivalent RQL:
```SQL
from Patients
group by visits[].doctorId
where doctorId != null
order by count desc
select visits[].doctorId as doctorId, count() as count
include 'visits[].doctorId'
```