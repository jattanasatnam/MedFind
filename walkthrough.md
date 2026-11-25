# Security Summary

**Reviewed Files**: 14
**Issues Resolved**: 10

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| Client Role Spoofing | `app/src/main/java/com/example/ui/screens/WelcomeAuthScreen.kt` | 48 | Sign-in screen let any user choose "Pharmacy" mode before authentication, bypassing role onboarding. | High | Fixed | Removed the self-selectable Customer/Pharmacy toggle so every sign-in uses a single verified Google Sign-In flow and derives access from server records. |
| Self-Service Role Switcher & Legacy Bypass | `app/src/main/java/com/example/ui/screens/ProfileScreen.kt` | 91 | Profile exposed Customer/Pharmacy switching to unverified customers, and unused `RoleDashboardScreen.kt` retained an unverified portal bypass flag. | High | Fixed | Deleted `RoleDashboardScreen.kt`; restricted the workspace switcher in `ProfileScreen` to server-verified Pharmacy Partners (`canAccessPharmacyPortal`) and Admins, and replaced customer access with a drug-license onboarding application (`PENDING`). |
| Workspace Elevation & Mode Persistence | `app/src/main/java/com/example/ui/viewmodel/AuthenticationViewModel.kt` | 100 | `SharedPreferences` workspace mode was trusted too broadly and did not clear on sign-out or check `status == ACTIVE` or `APPROVED` pharmacy ownership. | High | Fixed | Added `resolveAuthorizedMode`, tracked live `/users/{uid}` status plus `/pharmacies` ownership (`APPROVED`), revoked `KEY_WORKSPACE_MODE` on sign-out, and blocked `SUSPENDED` accounts in `MainActivity.kt:199`. |
| Raw Database Explorer Exposure | `app/src/main/java/com/example/MainActivity.kt` | 116 | Non-admin pharmacy bottom navigation included `ROUTE_PHARMACY_DB` ("Live DB"), exposing raw Firestore collection tooling outside Admin Governance. | Medium | Fixed | Removed `ROUTE_PHARMACY_DB` from pharmacy navigation so `FirestoreDataExplorerPane` is strictly gated to verified Platform Admins (`AppWorkspaceMode.ADMIN`). |
| Customer Medicine Spoofing | `firestore.rules` | 252 | Any signed-in customer could create `/medicines` entries (`PENDING_REVIEW`) unrelated to any approved pharmacy. | Medium | Fixed | Restricted `create` on `/medicines/{medicineId}` to verified Admins or active owners of an `APPROVED` pharmacy (`isApprovedPharmacyOwner(incoming().submittedPharmacyId)`), mirrored in `MedFindRepository.kt:241`. |
| Random Inventory IDs & Invalid Stock States | `firestore.rules` | 344 | Non-deterministic inventory document IDs allowed duplicate listings, unverified medicine references, or `OUT_OF_STOCK` items with non-zero quantities. | High | Fixed | Enforced deterministic document IDs (`pharmacyId + '_' + medicineId`), verified target medicine `catalogueStatus == 'VERIFIED'`, required `quantity == 0` when `OUT_OF_STOCK`, and locked `delete` to Admins (`firestore.rules:401`). |
| Atomic Audit History Enforcement | `firestore.rules` | 412 | `inventory_history` creation verified inventory existence after commit without matching the parent `ownerUserId`, `pharmacyId`, and `medicineId`. | Medium | Fixed | Strengthened `getAfter` assertions on `/inventory/{inventoryId}` to confirm matching `ownerUserId`, `pharmacyId`, `medicineId`, plus `isActiveUser()` and `isApprovedPharmacyOwner(...)`. |
| Suspension Bypass via Self-Delete / Store Edits | `firestore.rules` | 229 | Users could delete their own `/users/{uid}` profile to reset `SUSPENDED` status, and suspended pharmacies retained write flexibility. | High | Fixed | Restricted `/users/{uid}` deletion to Admins (`firestore.rules:229`), locked `role`/`status`/`email` on owner updates (`216-225`), and limited pharmacy profile edits to `PENDING` or `APPROVED` stores (`318`). |
| Feedback Report Fabrication | `firestore.rules` | 456 | Users could forge `feedback_reports` referencing mismatched pharmacy/medicine IDs or pre-populated `resolutionNote` / `reviewedBy` fields. | Medium | Fixed | Validated `pharmacyId`, `medicineId`, `pharmacyOwnerUserId`, and `pharmacyVerified == true` via `get(/inventory/$(inventoryId))` and prohibited resolution fields on report creation. |
| Device Backup Leakage & Debug PII | `app/src/main/AndroidManifest.xml` | 30 | `android:allowBackup="true"` permitted adb/cloud backups of local app state, and `FirestoreErrorHandler.kt:62` logged user email in error telemetry. | Low | Fixed | Set `android:allowBackup="false"`, redacted `email` in non-debug `FirestoreErrorHandler` logs, and replaced hardcoded console project ID in `ConsoleExplorerAndOverview.kt:194` with runtime `FirebaseApp` project metadata. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Google Sign-In & Auth Gate | `app/src/main/java/com/example/MainActivity.kt` | Unauthenticated users are strictly confined to `WelcomeAuthScreen` using Jetpack `CredentialManager` Google Sign-In; anonymous auth is disabled. |
| Three-Tier RBAC Enforcement | `firestore.rules` | Customers, Admin-approved Pharmacy Partners, and Admins (`jattanasatnam823@gmail.com` with verified email or `/admins/{uid}`) are enforced server-side on every read/write. |
| Batch Role Synchronization | `app/src/main/java/com/example/data/repository/MedFindRepository.kt` | Admin pharmacy approval/suspension atomically updates `/pharmacies`, `/users/{ownerUserId}.role`, `/inventory.pharmacyVerified`, and `/admin_actions`. |
| Release Integrity & Secrets | `app/build.gradle.kts` | `BuildConfig.GEMINI_API_KEY` is excluded in favor of Firebase AI Logic + Play Integrity / Debug App Check (`AppCheckInitializer.kt`). |
| Verified Rules Unit Suite | `firestore.test.js` | All 5 rule test suites (`5/5` passing against the local Firestore Emulator) verify positive functional flows and negative attack scenarios. |

---

## PoC Verification

### 1. Customer Attempting Unauthorized Pharmacy Portal Access & Stock Tampering
#### Summary
| Field | Value |
|---|---|
| Issue Type | Missing Role Boundary & Client-Side Privilege Escalation |
| Impact | High |
| Affected File | `app/src/main/java/com/example/ui/viewmodel/AuthenticationViewModel.kt:100` & `firestore.rules:344` |
| Test Scenario | A regular Customer (`CUSTOMER` role, no `APPROVED` pharmacy) attempts to switch into `PHARMACY` mode in client state or directly write a stock listing to `/inventory/pharm1_med1` (`ownerUserId = customerUid`). |

#### How It Was Fixed
`AuthenticationViewModel.resolveAuthorizedMode` and `ProfileScreen` deny `AppWorkspaceMode.PHARMACY` unless `hasApprovedPharmacy` (from live `/pharmacies` snapshots) or `UserRole.ADMIN` is verified, while `firestore.rules` cross-checks `isApprovedPharmacyOwner(incoming().pharmacyId)` server-side.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Customer taps Profile or calls `setWorkspaceMode(AppWorkspaceMode.PHARMACY)` | Mode selector is hidden (`ProfileScreen.kt:188`), and `resolveAuthorizedMode` downgrades request to `AppWorkspaceMode.CUSTOMER` (`AuthenticationViewModel.kt:117`). |
| 2 | Modified client skips UI and writes `/inventory/pharm1_med1` | Hits `allow create` at `firestore.rules:344`. |
| 3 | Server evaluates `isApprovedPharmacyOwner(incoming().pharmacyId)` (`firestore.rules:46-51`) | Store `ownerUserId != request.auth.uid` (and/or `verificationStatus != 'APPROVED'`) -> **PERMISSION_DENIED (Verified)** |

#### Conclusion
**Fix Verified** — Customers cannot open the Pharmacy Store Portal in the UI and cannot create or modify stock documents in Firestore.

---

### 2. Pending / Suspended Pharmacy Publishing Stock or Medicines
#### Summary
| Field | Value |
|---|---|
| Issue Type | Insufficient Approval Status Enforcement |
| Impact | High |
| Affected File | `firestore.rules:252, 344, 377` & `app/src/main/java/com/example/ui/viewmodel/AuthenticationViewModel.kt:178` |
| Test Scenario | A store owner submits a new pharmacy (`verificationStatus: 'PENDING'`) or has an active store switched to `SUSPENDED` by an Admin, then attempts to submit a new medicine to `/medicines` or increment stock quantity in `/inventory`. |

#### How It Was Fixed
Both `/medicines` creation and `/inventory` creation/update require `isActiveUser()` and `isApprovedPharmacyOwner(...)`, while real-time Firestore listeners revoke `hasApprovedPharmacy` immediately upon Admin suspension.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Owner registers store -> stored with `verificationStatus = 'PENDING'` (`firestore.rules:302`) | `hasApprovedPharmacy` remains `false`; Pharmacy Portal stays locked (`MainActivity.kt:225`). |
| 2 | Owner attempts `/medicines` create with `submittedPharmacyId` of `PENDING` store | `isApprovedPharmacyOwner(...)` fails at `firestore.rules:264` -> **PERMISSION_DENIED**. |
| 3 | Admin later suspends an approved store via `moderatePharmacy` (`MedFindRepository.kt:487`) | `pharmacies/{id}.verificationStatus` becomes `'SUSPENDED'`; live listener updates `canAccessPharmacyPortal = false` (`AuthenticationViewModel.kt:188`), and `allow update` fails at `firestore.rules:392` -> **Blocked on Client & Server**. |

#### Conclusion
**Fix Verified** — Only stores actively holding `APPROVED` verification from an Admin can submit medicines or update inventory.

---

### 3. Self-Promotion to Admin or Role Escalation via `/users` Profile Write
#### Summary
| Field | Value |
|---|---|
| Issue Type | Direct Object / Field Manipulation |
| Impact | High |
| Affected File | `firestore.rules:192-229` |
| Test Scenario | Authenticated user sends a Firestore `set`/`update` to `/users/{uid}` setting `role: 'ADMIN'` or `'PHARMACY'`, or attempts `deleteDoc('/users/' + uid)` after being suspended (`status: 'SUSPENDED'`). |

#### How It Was Fixed
`/users/{userId}` restricts `create` to `role == 'CUSTOMER' && status == 'ACTIVE'`, blocks modifications to `role`, `status`, and `email` on owner `update`, limits `delete` strictly to `isAdmin()`, and validates Admin elevation via token email verification (`jattanasatnam823@gmail.com`) or Admin-written `/admins/{uid}`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | User writes `role: 'ADMIN'` on `/users/{uid}` create or update | `incoming().role == 'CUSTOMER'` (`firestore.rules:195`) or `incoming().role == existing().role` (`218`) rejects write -> **PERMISSION_DENIED**. |
| 2 | Suspended user updates `displayName` or deletes `/users/{uid}` to reset status | `existing().status == 'ACTIVE'` (`firestore.rules:217`) and `allow delete: if isAdmin()` (`229`) reject both -> **PERMISSION_DENIED**. |

#### Conclusion
**Fix Verified** — Users cannot elevate their own role, spoof email claims, or wipe suspension state.

---

### 4. Non-Deterministic Inventory Spam, Unverified Medicine Linking & Out-of-Stock Spoofing
#### Summary
| Field | Value |
|---|---|
| Issue Type | Data Integrity & Referential Validation Bypass |
| Impact | Medium |
| Affected File | `firestore.rules:133, 347, 372` |
| Test Scenario | Approved pharmacy owner attempts to create `/inventory/random_spam_id`, link an unverified (`PENDING_REVIEW`) medicine, or publish `availabilityStatus: 'OUT_OF_STOCK'` with `quantity: 50`. |

#### How It Was Fixed
`firestore.rules` enforces deterministic doc IDs (`pharmacyId + '_' + medicineId`), checks `medicines/$(medicineId).catalogueStatus == 'VERIFIED'` via `get()`, and enforces `(data.availabilityStatus != 'OUT_OF_STOCK' || data.quantity == 0)` in `isValidInventory()`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Approved owner writes `/inventory/random_id_99` | Fails `inventoryId == (incoming().pharmacyId + '_' + incoming().medicineId)` at `firestore.rules:347` -> **PERMISSION_DENIED**. |
| 2 | Approved owner creates inventory referencing a `PENDING_REVIEW` medicine | Fails `get(.../medicines/...).data.catalogueStatus == 'VERIFIED'` at `firestore.rules:373` -> **PERMISSION_DENIED**. |
| 3 | Approved owner sets `OUT_OF_STOCK` with `quantity: 50` | Fails `isValidInventory()` at `firestore.rules:133` -> **PERMISSION_DENIED**. |

#### Conclusion
**Fix Verified** — Public inventory search results only contain unique `(pharmacy, medicine)` pairs referencing Admin-verified medicines with accurate stock status semantics.

---

### 5. Forged Customer Correction Report Targeting Unrelated Pharmacy
#### Summary
| Field | Value |
|---|---|
| Issue Type | Cross-Entity Reference Spoofing |
| Impact | Medium |
| Affected File | `firestore.rules:456-479` |
| Test Scenario | Authenticated customer submits a `/feedback_reports` document naming `pharmacyId: 'other_store'` while referencing a different `inventoryId`, or sets `status: 'RESOLVED'` and `reviewedBy` at creation time. |

#### How It Was Fixed
`/feedback_reports` `create` rule requires `status == 'OPEN'`, forbids `resolutionNote`/`reviewedBy` keys, and fetches `/inventory/$(incoming().inventoryId)` on the server to verify `pharmacyId`, `medicineId`, `pharmacyOwnerUserId`, and `pharmacyVerified == true`.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Customer submits report with forged `pharmacyId` or `status: 'RESOLVED'` | Caught by `incoming().status == 'OPEN'` (`firestore.rules:460`) and `get(.../inventory/...).data.pharmacyId == incoming().pharmacyId` (`475`) -> **PERMISSION_DENIED**. |
| 2 | Verified report payload passes all server assertions | Stored with `OPEN` status; pharmacy owner views read-only report while only Admin can resolve (`firestore.rules:480-492`) -> **Handled Safely**. |

#### Conclusion
**Fix Verified** — Executed all 5 test suites in `firestore.test.js` (`node --test firestore.test.js`) against the Firestore Emulator (`127.0.0.1:8085`) with 5/5 passing, deployed hardened rules via `DeployRules`, and confirmed `compile_applet` build succeeded.
