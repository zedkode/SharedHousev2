# Privacy and Regulatory Compliance Baseline

## 1. Status of this document

This is an engineering and operational compliance baseline for launch planning in the United Kingdom, European Union/EEA and United States. It is not a legal opinion or certification. Qualified legal/privacy review is required before public launch, material data-purpose changes, payment initiation, children’s access, advertising profiling or expansion into new regulated services.

## 2. Privacy roles

The operating company is normally the controller/business for account, security, support, subscription and platform-operation data. For ordinary household records it will commonly remain a controller because it determines core processing means and product purposes, though exact role analysis must be documented. Vendors such as hosting, email, analytics and support providers are processors/service providers where contract and facts support that role.

Do not describe the company as a mere processor for all household data without legal analysis.

## 3. Data inventory and purposes

| Data | Purpose | Typical necessity |
|---|---|---|
| Email and account credentials | Account creation, security and communication | Required |
| Display name and avatar | Household identification and assignments | Name required; avatar optional |
| Household membership | Authorisation and collaboration | Required |
| Bills, allocations and payment declarations | Household ledger | User-created core data |
| Chores, calendar and shopping | Coordination | User-created core data |
| Device/push token | Notifications | Optional capability |
| Receipts/meter photos | Evidence | Optional |
| Store transaction IDs | Subscription verification | Required for paid access |
| Diagnostics/security events | Reliability, fraud and defence | Minimise and retain briefly |
| Analytics | Product improvement | Use privacy-minimised, configurable collection |

## 4. UK and EU principles

Design for lawfulness, fairness, transparency, purpose limitation, data minimisation, accuracy, storage limitation, integrity/confidentiality and accountability. Document a lawful basis per processing purpose. Contract necessity may apply to core account/service processing; legitimate interests require a balancing assessment; consent must be specific and withdrawable where used.

Special-category data is not required by the product and should not be solicited. Free-text fields can nevertheless contain it, so access, retention and support procedures must treat user content carefully.

## 5. User rights

Provide processes for access/export, correction, deletion, restriction/objection where applicable, consent withdrawal and complaints. Identity verification must be proportionate. Track statutory deadlines by jurisdiction, pauses/extensions and communication evidence.

Account export and deletion are self-service where feasible, with assisted support fallback. The deletion page must explain data shared within a household, retained audit/financial references and store-subscription separation.

## 6. United States baseline

Maintain a state-law matrix rather than assuming one national consumer privacy law. Provide transparent notice, access/correction/deletion workflows, appeal where required and controls for sale/sharing/targeted advertising if ever applicable. The safest MVP is no sale of personal data and no cross-context behavioural advertising.

California applicability and thresholds require annual legal review. Even when a statute does not apply by threshold, the product should preserve consistent privacy controls where operationally practical.

## 7. Children

The MVP is intended for adults aged 18 and over. Do not knowingly create child accounts. If the product later serves families/minors, conduct a dedicated review for the UK Age Appropriate Design Code, COPPA and applicable US state/EU child-consent rules before enabling access.

## 8. International transfers

Map where data and support access occur. For UK-restricted transfers use an applicable adequacy regulation, UK IDTA/Addendum or another lawful mechanism plus transfer risk assessment as required. For EEA transfers use adequacy, Standard Contractual Clauses or another valid mechanism plus supplementary measures where needed. Do not claim that encryption alone resolves every transfer requirement.

## 9. Vendors

Before onboarding a vendor, record purpose, data fields, locations, subprocessors, security controls, retention, deletion, incident terms and transfer mechanism. Execute appropriate data-processing terms. Keep a public subprocessor list if the chosen transparency model requires it.

## 10. Privacy by design gates

A privacy review is mandatory for new data categories, analytics/advertising SDKs, location, contact access, AI profiling, automated decisions, payment initiation, public sharing, children, biometrics, message scanning or support-content access. High-risk processing may require a DPIA or equivalent assessment.

## 11. Notices and consent

Publish a layered privacy notice in clear English and Romanian, with legal English controlling only if counsel approves that arrangement. In-product just-in-time explanations appear before optional photo, notification, analytics or other sensitive collection. Consent choices must not use deceptive defaults.

## 12. Breach response

Maintain detection, containment, assessment, evidence and notification workflows. UK/EU regulator and individual notification decisions depend on risk and legal deadlines; US breach laws vary by state. The incident team must involve privacy/legal decision-makers promptly rather than relying on a fixed universal timer.
