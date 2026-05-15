## Diagram 1: Alert Generation System

![Diagram 1](Diagram_1_Alert_Generation_System.svg)

The Alert Generation System is responsible for real-time evaluation of incoming patient data and the creation and routing of alerts to medical staff. The central class, AlertGenerator, holds a reference to DataStorage and actively queries it to retrieve recent records for a given patient. It contains dedicated methods for checking specific cardiovascular conditions (heart rate saturation, blood pressure trends, ECG anomalies, and critical threshold breaches) reflecting the variety of alert rules required in a hospital setting. This design keeps all detection logic in one place, which simplifies maintenance while allowing new check methods to be added without affecting other parts of the system.
AlertThreshold encapsulates the minimum and maximum acceptable values for a named metric (e.g. heart rate, blood pressure). Each threshold instance belongs to a patient and can be evaluated independently via isExceeded(). This makes it straightforward to support per-patient threshold customization.
When a threshold is exceeded, AlertGenerator constructs an Alert object — a simple data container holding the patient ID, a description of the condition, and a timestamp — and passes it to AlertManager. The AlertManager maintains a log of all alerts and is responsible for dispatching them to medical staff via notifyStaff(). This separation ensures that detection and notification are independent responsibilities, making it easy to swap in a different notification channel (e.g. SMS, pager) without touching alert logic.
The diagram shows clear links to the DataStorage subsystem and uses appropriate UML relationships: dependency arrows for usage, dashed arrows for object creation, and aggregation for the AlertManager–Alert relationship.

## Diagram 2: Data Storage System

![Diagram 2](Diagram_2_Data_Storage_System.svg)

The Data Storage System provides a structured, access-controlled layer for persisting and retrieving patient vital records. The top-level entry point is DataStorage, which maps patient IDs to Patient objects and exposes methods to add records and query by time range. This design allows all other subsystems to interact with storage through a single, consistent interface.
Patient acts as a container for a patient's full record history, holding a list of PatientRecord objects. Each PatientRecord is a timestamped measurement of a single vital sign, identified by metricType (e.g. "heartRate"), enabling both real-time monitoring and historical trend analysis. The composition relationships from DataStorage to Patient to PatientRecord model the fact that records only exist in the context of a patient, and patients only exist in the context of the storage system.
DataRetriever provides a clean querying interface for medical staff, supporting both full patient history lookups and time-bounded range queries. It depends on DataStorage rather than accessing the underlying data structures directly, enforcing encapsulation.
AccessController manages role-based access permissions, exposing a single canAccess(role, patientId) method that gates data retrieval. This ensures that only authorised roles (e.g. attending physicians) can access specific patient data, which is critical in a privacy-sensitive medical setting.
DeletionPolicy enforces data retention rules by determining whether a given record has exceeded its maximum storage age. By separating this logic from the storage class itself, the policy can be adjusted or replaced independently, for example to comply with evolving hospital data regulations.

## Diagram 3: Patient Identification System

![Diagram 3](Diagram_3_Patient_Identification_System.svg)

The Patient Identification System ensures that every incoming data point from the simulator is correctly matched to a verified hospital patient before being passed on to storage or alert subsystems. This is a safety-critical function: a mismatch could result in incorrect treatment decisions.
PatientIdentifier is the primary entry point for incoming data. It receives a raw patient ID string from the data stream, validates it via isValid(), and attempts to resolve it to a HospitalPatient record via matchIncoming(). If the match succeeds, it returns a fully populated HospitalPatient object. If the match fails, it reports the anomaly to the IdentityManager.
HospitalPatient represents a patient record as stored in the hospital database, containing not just the patient's ID but also their name and a list of prior medical history entries. All these fields are private, exposed only through getters, to prevent unauthorised access to sensitive personal data. The class maps to an internal Patient object used by the data layer, keeping the domain model separate from the data storage model.
IdentityManager oversees system integrity. It handles mismatches by calling logAnomaly() and creating a MismatchLog entry — a timestamped record of the unmatched ID and the reason for failure. It also provides a verifyIntegrity() method for periodic auditing. Separating mismatch handling from matching logic means the PatientIdentifier can remain focused on the happy path, while edge cases are managed by a dedicated class. This is especially important in a real hospital setting where unmatched data must be flagged and investigated rather than silently dropped.

## Diagram 4: Data Access Layer

![Diagram 4](Diagram_4_Data_Access_Layer.svg)

The Data Access Layer abstracts away the mechanism by which raw data enters the system, allowing the rest of the CHMS to be entirely unaware of whether data arrives via TCP, WebSocket, or a log file. This follows the principle of programming to an abstraction: the abstract class DataListener defines the shared interface (startListening(), stopListening(), onDataReceived()) that all concrete listeners must implement.
The three subclasses — TCPDataListener, WebSocketDataListener, and FileDataListener — each carry only the configuration attributes specific to their protocol (e.g. port and host for TCP, url for WebSocket, filePath for file-based input). They override startListening() and onDataReceived() with protocol-specific logic. This makes it trivial to add a new data source in the future without modifying any existing class.
When raw data is received, it is passed as a string to DataParser, which is responsible for detecting the format (JSON or CSV via detectFormat()) and parsing it into a structured PatientRecord object. This keeps parsing logic centralized and format-agnostic from the perspective of other components.
DataSourceAdapter then receives the parsed record and forwards it to DataStorage via forwardToStorage(). It also implements the DataReader interface, which defines a single readData(storage) method. Using an interface here means the adapter can be substituted or mocked in testing without changing the rest of the system.
Stub classes for PatientRecord and DataStorage appear at the bottom of the diagram to make cross-subsystem dependencies explicit, while signalling that their full definitions belong to other diagrams.