const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "customer_alice_123";
const PHARMACY_OWNER_UID = "pharmacy_bob_456";
const OTHER_UID = "other_eve_789";
const ADMIN_UID = "admin_satnam_001";
const ADMIN_EMAIL = "jattanasatnam823@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(
    unauthDb.collection("medicines").where("catalogueStatus", "==", "VERIFIED").get()
  );
  await assertFails(
    unauthDb.collection("pharmacies").where("verificationStatus", "==", "APPROVED").get()
  );
  await assertFails(
    unauthDb.collection("inventory").where("pharmacyVerified", "==", true).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Anon",
      locationSharingEnabled: false,
      role: "CUSTOMER",
      status: "ACTIVE",
      recentSearches: [],
      savedPharmacyIds: [],
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Users collection: owner create/update works; role/email spoofing, delete & cross-user access fail", async () => {
  const aliceDb = testEnv
    .authenticatedContext(CUSTOMER_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(PHARMACY_OWNER_UID, { email: "bob@example.com", email_verified: true })
    .firestore();

  const created = pastDate(5000);
  // Valid Kotlin-mirrored user profile create
  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Alice",
      email: "alice@example.com",
      preferredLocality: "Indiranagar",
      preferredPincode: "560038",
      locationSharingEnabled: false,
      role: "CUSTOMER",
      status: "ACTIVE",
      recentSearches: ["Paracetamol"],
      savedPharmacyIds: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Self role escalation to PHARMACY or ADMIN on create must fail
  await assertFails(
    bobDb.collection("users").doc(PHARMACY_OWNER_UID).set({
      userId: PHARMACY_OWNER_UID,
      displayName: "Bob",
      email: "bob@example.com",
      locationSharingEnabled: false,
      role: "PHARMACY",
      status: "ACTIVE",
      recentSearches: [],
      savedPharmacyIds: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Email spoofing on user create must fail
  await assertFails(
    bobDb.collection("users").doc(PHARMACY_OWNER_UID).set({
      userId: PHARMACY_OWNER_UID,
      displayName: "Bob",
      email: ADMIN_EMAIL,
      locationSharingEnabled: false,
      role: "CUSTOMER",
      status: "ACTIVE",
      recentSearches: [],
      savedPharmacyIds: [],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid partial update of allowed profile preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_UID).update({
      preferredLocality: "Koramangala",
      recentSearches: ["Paracetamol", "Amoxicillin"],
      updatedAt: pastDate(1000),
    })
  );

  // Owner cannot elevate own role or delete own profile
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_UID).update({
      role: "PHARMACY",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(aliceDb.collection("users").doc(CUSTOMER_UID).delete());

  // Cross-user read/write must fail
  await assertFails(bobDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(
    bobDb.collection("users").doc(CUSTOMER_UID).update({
      displayName: "Hijacked",
      updatedAt: pastDate(500),
    })
  );
});

test("Email spoofing test: unverified admin email cannot act as admin", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();

  await assertFails(spoofedAdminDb.collection("admin_actions").get());
  await assertFails(
    spoofedAdminDb.collection("medicines").doc("med_1").set({
      genericName: "Paracetamol",
      brandName: "Dolo",
      strength: "650 mg",
      dosageForm: "Tablet",
      category: "Analgesic",
      keywords: ["paracetamol", "dolo", "650"],
      catalogueStatus: "VERIFIED",
      submittedBy: "spoof_uid",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Medicines catalogue: regular customers cannot add medicines; only approved pharmacies or admin can", async () => {
  const aliceDb = testEnv
    .authenticatedContext(CUSTOMER_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(PHARMACY_OWNER_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  const created = pastDate(8000);

  // Plain customer cannot submit medicine (even as PENDING_REVIEW)
  await assertFails(
    aliceDb.collection("medicines").doc("med_cust").set({
      genericName: "Ibuprofen",
      brandName: "Brufen",
      strength: "400 mg",
      dosageForm: "Tablet",
      category: "Analgesic",
      keywords: ["ibuprofen"],
      catalogueStatus: "PENDING_REVIEW",
      submittedBy: CUSTOMER_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob submits pharmacy application as PENDING
  await assertSucceeds(
    bobDb.collection("pharmacies").doc("pharm_bob").set({
      ownerUserId: PHARMACY_OWNER_UID,
      name: "CarePlus Pharmacy",
      licenseNumber: "DL-KA-9921",
      address: "12 MG Road, Indiranagar",
      locality: "Indiranagar",
      pincode: "560038",
      latitude: 12.9784,
      longitude: 77.6408,
      phone: "+919876543210",
      openingHours: "08:00 AM - 10:00 PM",
      verificationStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Pending pharmacy CANNOT submit medicine yet
  await assertFails(
    bobDb.collection("medicines").doc("med_pending").set({
      genericName: "Ibuprofen",
      brandName: "Brufen",
      strength: "400 mg",
      dosageForm: "Tablet",
      category: "Analgesic",
      keywords: ["ibuprofen", "brufen"],
      catalogueStatus: "PENDING_REVIEW",
      submittedBy: PHARMACY_OWNER_UID,
      submittedPharmacyId: "pharm_bob",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Admin approves Bob's pharmacy
  await assertSucceeds(
    adminDb.collection("pharmacies").doc("pharm_bob").update({
      verificationStatus: "APPROVED",
      adminNote: "License verified",
      updatedAt: pastDate(4000),
    })
  );

  // Approved pharmacy can submit PENDING_REVIEW medicine referencing own approved store
  await assertSucceeds(
    bobDb.collection("medicines").doc("med_pending").set({
      genericName: "Ibuprofen",
      brandName: "Brufen",
      strength: "400 mg",
      dosageForm: "Tablet",
      category: "Analgesic",
      keywords: ["ibuprofen", "brufen"],
      catalogueStatus: "PENDING_REVIEW",
      submittedBy: PHARMACY_OWNER_UID,
      submittedPharmacyId: "pharm_bob",
      createdAt: pastDate(3000),
      updatedAt: pastDate(3000),
    })
  );

  // Approved pharmacy cannot self-verify own medicine
  await assertFails(
    bobDb.collection("medicines").doc("med_pending").update({
      catalogueStatus: "VERIFIED",
      updatedAt: pastDate(),
    })
  );

  // Admin verifies medicine
  await assertSucceeds(
    adminDb.collection("medicines").doc("med_pending").update({
      catalogueStatus: "VERIFIED",
      updatedAt: pastDate(),
    })
  );

  // Customer filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("medicines").where("catalogueStatus", "==", "VERIFIED").get()
  );
  await assertFails(aliceDb.collection("medicines").get());
});

test("Strict RBAC: Pharmacies, deterministic Inventory IDs, Atomic History, Suspension & Reports", async () => {
  const bobDb = testEnv
    .authenticatedContext(PHARMACY_OWNER_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(CUSTOMER_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const eveDb = testEnv
    .authenticatedContext(OTHER_UID, { email: "eve@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  const created = pastDate(10000);

  // Admin seeds verified medicine + unverified pending medicine
  await assertSucceeds(
    adminDb.collection("medicines").doc("med_para").set({
      genericName: "Paracetamol",
      brandName: "Calpol",
      strength: "500 mg",
      dosageForm: "Tablet",
      category: "Analgesic",
      keywords: ["paracetamol", "calpol"],
      catalogueStatus: "VERIFIED",
      submittedBy: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("medicines").doc("med_unverified").set({
      genericName: "UnknownHerb",
      brandName: "UnverifiedX",
      strength: "100 mg",
      dosageForm: "Tablet",
      category: "Other",
      keywords: ["herb"],
      catalogueStatus: "PENDING_REVIEW",
      submittedBy: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Bob cannot self-approve or inject adminNote on pharmacy registration
  await assertFails(
    bobDb.collection("pharmacies").doc("pharm_1").set({
      ownerUserId: PHARMACY_OWNER_UID,
      name: "CarePlus Pharmacy",
      licenseNumber: "DL-KA-9921",
      address: "12 MG Road, Indiranagar",
      locality: "Indiranagar",
      pincode: "560038",
      latitude: 12.9784,
      longitude: 77.6408,
      phone: "+919876543210",
      openingHours: "08:00 AM - 10:00 PM",
      verificationStatus: "APPROVED",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(
    bobDb.collection("pharmacies").doc("pharm_1").set({
      ownerUserId: PHARMACY_OWNER_UID,
      name: "CarePlus Pharmacy",
      licenseNumber: "DL-KA-9921",
      address: "12 MG Road, Indiranagar",
      locality: "Indiranagar",
      pincode: "560038",
      latitude: 12.9784,
      longitude: 77.6408,
      phone: "+919876543210",
      openingHours: "08:00 AM - 10:00 PM",
      verificationStatus: "PENDING",
      adminNote: "Spoofed Admin Note",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid PENDING registration succeeds
  await assertSucceeds(
    bobDb.collection("pharmacies").doc("pharm_1").set({
      ownerUserId: PHARMACY_OWNER_UID,
      name: "CarePlus Pharmacy",
      licenseNumber: "DL-KA-9921",
      address: "12 MG Road, Indiranagar",
      locality: "Indiranagar",
      pincode: "560038",
      latitude: 12.9784,
      longitude: 77.6408,
      phone: "+919876543210",
      openingHours: "08:00 AM - 10:00 PM",
      verificationStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );

  const invId = "pharm_1_med_para";
  const invPayload = {
    pharmacyId: "pharm_1",
    pharmacyName: "CarePlus Pharmacy",
    pharmacyAddress: "12 MG Road, Indiranagar",
    pharmacyLocality: "Indiranagar",
    pharmacyPincode: "560038",
    pharmacyPhone: "+919876543210",
    pharmacyHours: "08:00 AM - 10:00 PM",
    pharmacyLat: 12.9784,
    pharmacyLon: 77.6408,
    ownerUserId: PHARMACY_OWNER_UID,
    pharmacyVerified: true,
    medicineId: "med_para",
    medicineName: "Paracetamol 500 mg (Calpol)",
    genericName: "Paracetamol",
    brandName: "Calpol",
    strength: "500 mg",
    dosageForm: "Tablet",
    availabilityStatus: "REPORTED_AVAILABLE",
    quantity: 45,
    unit: "strips",
    updatedBy: PHARMACY_OWNER_UID,
    updatedByName: "Bob",
    createdAt: created,
    updatedAt: created,
  };

  // Unapproved pharmacy CANNOT publish inventory
  await assertFails(bobDb.collection("inventory").doc(invId).set(invPayload));

  // Admin approves pharmacy
  await assertSucceeds(
    adminDb.collection("pharmacies").doc("pharm_1").update({
      verificationStatus: "APPROVED",
      adminNote: "License verified",
      updatedAt: pastDate(4000),
    })
  );

  // Non-deterministic inventory ID (spam duplicate) MUST fail
  await assertFails(
    bobDb.collection("inventory").doc("random_spam_inv_id").set(invPayload)
  );

  // Publishing unverified medicine to inventory MUST fail
  await assertFails(
    bobDb.collection("inventory").doc("pharm_1_med_unverified").set({
      ...invPayload,
      medicineId: "med_unverified",
    })
  );

  // OUT_OF_STOCK with non-zero quantity MUST fail
  await assertFails(
    bobDb.collection("inventory").doc(invId).set({
      ...invPayload,
      availabilityStatus: "OUT_OF_STOCK",
      quantity: 12,
    })
  );

  // Now approved pharmacy publishes deterministic inventory + atomic history in WriteBatch
  const batch = bobDb.batch();
  batch.set(bobDb.collection("inventory").doc(invId), invPayload);
  batch.set(bobDb.collection("inventory_history").doc("hist_1"), {
    inventoryId: invId,
    pharmacyId: "pharm_1",
    pharmacyName: "CarePlus Pharmacy",
    medicineId: "med_para",
    medicineName: "Paracetamol 500 mg (Calpol)",
    ownerUserId: PHARMACY_OWNER_UID,
    oldStatus: "NONE",
    newStatus: "REPORTED_AVAILABLE",
    oldQuantity: 0,
    newQuantity: 45,
    unit: "strips",
    changedBy: PHARMACY_OWNER_UID,
    changedByName: "Bob",
    changedAt: pastDate(3000),
  });
  await assertSucceeds(batch.commit());

  // Pharmacy owner cannot silently delete inventory record (only Admin can delete)
  await assertFails(bobDb.collection("inventory").doc(invId).delete());

  // Other user (Eve / Pharmacy B) cannot modify Bob's inventory
  await assertFails(
    eveDb.collection("inventory").doc(invId).update({
      quantity: 0,
      availabilityStatus: "OUT_OF_STOCK",
      updatedBy: OTHER_UID,
      updatedByName: "Eve",
      updatedAt: pastDate(1000),
    })
  );

  // Customer queries verified inventory by medicineId (aligned with Kotlin query)
  await assertSucceeds(
    aliceDb
      .collection("inventory")
      .where("medicineId", "==", "med_para")
      .where("pharmacyVerified", "==", true)
      .get()
  );

  // Customer cannot spoof pharmacyOwnerUserId or pre-inject reviewedBy on feedback_report
  await assertFails(
    aliceDb.collection("feedback_reports").doc("rep_spoof").set({
      userId: CUSTOMER_UID,
      reporterName: "Alice",
      pharmacyId: "pharm_1",
      pharmacyName: "CarePlus Pharmacy",
      inventoryId: invId,
      medicineId: "med_para",
      medicineName: "Paracetamol 500 mg (Calpol)",
      pharmacyOwnerUserId: OTHER_UID,
      reportType: "OUT_OF_STOCK_ON_VISIT",
      message: "Spoofed owner",
      status: "OPEN",
      createdAt: pastDate(1500),
      updatedAt: pastDate(1500),
    })
  );

  // Valid customer stock correction report succeeds
  await assertSucceeds(
    aliceDb.collection("feedback_reports").doc("rep_1").set({
      userId: CUSTOMER_UID,
      reporterName: "Alice",
      pharmacyId: "pharm_1",
      pharmacyName: "CarePlus Pharmacy",
      inventoryId: invId,
      medicineId: "med_para",
      medicineName: "Paracetamol 500 mg (Calpol)",
      pharmacyOwnerUserId: PHARMACY_OWNER_UID,
      reportType: "OUT_OF_STOCK_ON_VISIT",
      message: "Visited at 6 PM and counter said sold out.",
      status: "OPEN",
      createdAt: pastDate(1500),
      updatedAt: pastDate(1500),
    })
  );

  // Unrelated user cannot read Alice's feedback report
  await assertFails(eveDb.collection("feedback_reports").doc("rep_1").get());

  // Pharmacy owner CANNOT resolve their own customer report
  await assertFails(
    bobDb.collection("feedback_reports").doc("rep_1").update({
      status: "DISMISSED",
      resolutionNote: "Self dismissed",
      reviewedBy: PHARMACY_OWNER_UID,
      updatedAt: pastDate(1000),
    })
  );

  // Admin resolves report + logs admin_action
  await assertSucceeds(
    adminDb.collection("feedback_reports").doc("rep_1").update({
      status: "RESOLVED",
      resolutionNote: "Pharmacy contacted and updated stock.",
      reviewedBy: ADMIN_UID,
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state locking: cannot re-open RESOLVED report
  await assertFails(
    adminDb.collection("feedback_reports").doc("rep_1").update({
      status: "OPEN",
      updatedAt: pastDate(500),
    })
  );

  // Admin suspends pharmacy -> Bob loses ability to update inventory immediately
  await assertSucceeds(
    adminDb.collection("pharmacies").doc("pharm_1").update({
      verificationStatus: "SUSPENDED",
      adminNote: "Suspended for repeated stock mismatch",
      updatedAt: pastDate(400),
    })
  );
  await assertFails(
    bobDb.collection("inventory").doc(invId).update({
      quantity: 99,
      availabilityStatus: "REPORTED_AVAILABLE",
      updatedBy: PHARMACY_OWNER_UID,
      updatedByName: "Bob",
      updatedAt: pastDate(200),
    })
  );

  // Admin audit log is append-only (create succeeds, update/delete fail)
  await assertSucceeds(
    adminDb.collection("admin_actions").doc("act_1").set({
      adminUserId: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      targetType: "REPORT",
      targetId: "rep_1",
      action: "RESOLVE_REPORT",
      reason: "Confirmed with pharmacy",
      createdAt: pastDate(500),
    })
  );
  await assertFails(
    adminDb.collection("admin_actions").doc("act_1").update({
      reason: "Tampered",
    })
  );
});

test("Admin CRUD and non-admin delete/escalation denials across users, pharmacies, medicines, and inventory", async () => {
  const aliceDb = testEnv
    .authenticatedContext(CUSTOMER_UID, { email: "alice@example.com", email_verified: true })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(PHARMACY_OWNER_UID, { email: "bob@example.com", email_verified: true })
    .firestore();
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();

  const created = pastDate(10000);

  // Create user, pharmacy, medicine, and inventory records
  await assertSucceeds(
    bobDb.collection("users").doc(PHARMACY_OWNER_UID).set({
      userId: PHARMACY_OWNER_UID,
      displayName: "Bob",
      email: "bob@example.com",
      locationSharingEnabled: false,
      role: "CUSTOMER",
      status: "ACTIVE",
      recentSearches: [],
      savedPharmacyIds: [],
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    bobDb.collection("pharmacies").doc("pharm_crud").set({
      ownerUserId: PHARMACY_OWNER_UID,
      name: "Crud Care Pharmacy",
      licenseNumber: "DL-CRUD-01",
      address: "10 Main St, Indiranagar",
      locality: "Indiranagar",
      pincode: "560038",
      latitude: 12.9784,
      longitude: 77.6408,
      phone: "+919800000001",
      openingHours: "08:00 AM - 09:00 PM",
      verificationStatus: "PENDING",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("pharmacies").doc("pharm_crud").update({
      verificationStatus: "APPROVED",
      adminNote: "Approved for CRUD verification",
      updatedAt: pastDate(5000),
    })
  );
  await assertSucceeds(
    adminDb.collection("medicines").doc("med_crud").set({
      genericName: "Azithromycin",
      brandName: "Azithral",
      strength: "500 mg",
      dosageForm: "Tablet",
      category: "Antibiotic",
      keywords: ["azithromycin", "azithral"],
      catalogueStatus: "VERIFIED",
      submittedBy: ADMIN_UID,
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    adminDb.collection("inventory").doc("pharm_crud_med_crud").set({
      pharmacyId: "pharm_crud",
      pharmacyName: "Crud Care Pharmacy",
      pharmacyAddress: "10 Main St, Indiranagar",
      pharmacyLocality: "Indiranagar",
      pharmacyPincode: "560038",
      pharmacyPhone: "+919800000001",
      pharmacyHours: "08:00 AM - 09:00 PM",
      pharmacyLat: 12.9784,
      pharmacyLon: 77.6408,
      ownerUserId: PHARMACY_OWNER_UID,
      pharmacyVerified: true,
      medicineId: "med_crud",
      medicineName: "Azithromycin 500 mg (Azithral)",
      genericName: "Azithromycin",
      brandName: "Azithral",
      strength: "500 mg",
      dosageForm: "Tablet",
      availabilityStatus: "REPORTED_AVAILABLE",
      quantity: 20,
      unit: "strips",
      updatedBy: ADMIN_UID,
      updatedByName: "Admin",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Non-admins cannot delete pharmacies, medicines, or users, nor update another user's role
  await assertFails(bobDb.collection("pharmacies").doc("pharm_crud").delete());
  await assertFails(bobDb.collection("medicines").doc("med_crud").delete());
  await assertFails(aliceDb.collection("users").doc(PHARMACY_OWNER_UID).delete());
  await assertFails(
    aliceDb.collection("users").doc(PHARMACY_OWNER_UID).update({
      role: "PHARMACY",
      updatedAt: pastDate(500),
    })
  );

  // Admin can update user role/status and medicine details
  await assertSucceeds(
    adminDb.collection("users").doc(PHARMACY_OWNER_UID).update({
      role: "PHARMACY",
      status: "ACTIVE",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    adminDb.collection("medicines").doc("med_crud").update({
      brandName: "Azithral 500",
      catalogueStatus: "VERIFIED",
      updatedAt: pastDate(500),
    })
  );

  // Pharmacy owner can update allowed profile fields when APPROVED
  await assertSucceeds(
    bobDb.collection("pharmacies").doc("pharm_crud").update({
      openingHours: "24 Hours",
      updatedAt: pastDate(400),
    })
  );

  // Admin can delete inventory, medicine, pharmacy, and user records
  await assertSucceeds(adminDb.collection("inventory").doc("pharm_crud_med_crud").delete());
  await assertSucceeds(adminDb.collection("medicines").doc("med_crud").delete());
  await assertSucceeds(adminDb.collection("pharmacies").doc("pharm_crud").delete());
  await assertSucceeds(adminDb.collection("users").doc(PHARMACY_OWNER_UID).delete());
});

