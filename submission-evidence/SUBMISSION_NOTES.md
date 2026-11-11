\# ITRI623 Service Desk and SOC MVP — Submission Notes



\## 1. Project overview



This project extends the Service Desk microservices application with a Security Operations Centre (SOC) monitoring and alerting capability.



\## 2. Implemented functionality



\- SOC event ingestion and retrieval through REST endpoints.

\- PostgreSQL persistence for security events and alerts.

\- Three scheduled, rule-based detection mechanisms:

&#x20; - Repeated failed logins from the same source IP.

&#x20; - Repeated HTTP 401/403 access denials associated with a user.

&#x20; - Spikes in HTTP 500-or-higher service errors.

\- Alert retrieval through the SOC REST API.

\- Neo4j graph synchronisation linking alerts to events, services, users, threat descriptions, endpoints, and recommended controls.

\- Graph-backed question answering through `/api/rag/ask`.

\- Evidence-based responses with an insufficient-evidence response when no sufficiently relevant alert is found.



\## 3. Verification performed



The following were verified during development:



\- SOC event and alert endpoints returned HTTP 200.

\- Three alert types were generated using controlled test events.

\- Alert records were persisted in PostgreSQL.

\- Graph synchronisation and key graph relationships were verified in Neo4j.

\- A question about the asset-service error spike returned the associated alert and supporting event IDs.

\- An unrelated question returned an insufficient-evidence response.



See the accompanying JSON files for captured API responses.



\## 4. Testing limitations



The detection rules identify suspicious patterns and service reliability issues. They do not independently prove that a cyberattack occurred.



The demonstration events are controlled test data and must not be presented as real security incidents.



During the latest RAG test, the answer to the asset-service question was relevant, but unrelated alerts also appeared in the returned evidence and aggregated lists. Question-specific evidence filtering remains a limitation requiring further testing.



\## 5. Security and configuration



Real environment files, passwords, API tokens, and signing secrets must not be included in the submission ZIP.



Replace any credentials previously exposed during development before using the application beyond local testing.



\## 6. Suggested demonstration evidence



1\. Docker Compose services running.

2\. Eureka service registry with SOC-EVENT-SERVICE registered.

3\. SOC events API response.

4\. SOC alerts API response showing all three alert types.

5\. Neo4j graph showing alerts connected to affected services and triggering events.

6\. Neo4j graph showing threat descriptions and recommended controls.

7\. Successful RAG response with supporting event evidence.

8\. RAG insufficient-evidence response.



\## 7. Scope statement



This is a development MVP demonstrating event ingestion, heuristic detection, alert persistence, graph relationships, and evidence-grounded retrieval. Production readiness, live threat intelligence integration, and fully validated question-specific RAG filtering are not claimed.

