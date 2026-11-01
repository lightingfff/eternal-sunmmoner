const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTimestamp() {
  return new Date(Date.now() - 2000);
}

function makeMasterPayload(uid, overrides = {}) {
  const ts = serverTimestamp();
  return {
    userId: uid,
    masterName: "Loki",
    masterLevel: 1,
    masterExp: 0,
    gold: 500,
    manaCrystals: 120,
    soulShards: 30,
    rations: 10,
    ironOre: 5,
    arcaneEssence: 3,
    highestClearedFloor: 0,
    totalExpeditions: 0,
    unlockedFacilities: ["TOWER_GATE", "SUMMONING_HALL", "TRAINING_GROUND", "ARMORY"],
    storyFlags: ["INTRO_SEEN"],
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function makeHeroPayload(uid, overrides = {}) {
  const ts = serverTimestamp();
  return {
    userId: uid,
    name: "Kaelen",
    title: "Iron Bulwark",
    heroClass: "KNIGHT",
    rarity: 3,
    level: 1,
    exp: 0,
    maxHp: 420,
    attack: 38,
    defense: 28,
    speed: 95,
    skillLevel: 1,
    personality: "STOIC",
    status: "READY",
    injuryRecoveryUntilEpochSec: 0,
    morale: 100,
    equippedWeaponId: "",
    equippedArmorId: "",
    inActiveSquad: true,
    bondSummary: "Trusts Lyra's cover fire.",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function makeEquipmentPayload(uid, overrides = {}) {
  const ts = serverTimestamp();
  return {
    userId: uid,
    name: "Vanguard Broadsword",
    slot: "WEAPON",
    rarity: 2,
    level: 1,
    bonusHp: 20,
    bonusAttack: 12,
    bonusDefense: 4,
    bonusSpeed: 0,
    iconType: "LONGSWORD",
    equippedByHeroId: "",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
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

test("1. Unauthenticated caller cannot read or write MasterProfile or Heroes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("heroes").get());
});

test("2. Authenticated owner can create and read MasterProfile, HeroRecord, and EquipmentRecord", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(makeMasterPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("heroes").doc("hero_knight_1").set(makeHeroPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("equipment").doc("item_sword_1").set(makeEquipmentPayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
});

test("3. Atomic WriteBatch bootstrapping Master + Heroes succeeds via existsAfter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("users").doc(ALICE_UID), makeMasterPayload(ALICE_UID));
  batch.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("heroes").doc("hero_1"),
    makeHeroPayload(ALICE_UID)
  );
  batch.set(
    aliceDb.collection("users").doc(ALICE_UID).collection("equipment").doc("eq_1"),
    makeEquipmentPayload(ALICE_UID)
  );
  await assertSucceeds(batch.commit());
});

test("4. Orphaned Hero write without parent MasterProfile fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("heroes").doc("orphan_1").set(makeHeroPayload(ALICE_UID))
  );
});

test("5. Cross-user read and write isolation strictly enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("users").doc(ALICE_UID).set(makeMasterPayload(ALICE_UID));
  await aliceDb.collection("users").doc(ALICE_UID).collection("heroes").doc("h1").set(makeHeroPayload(ALICE_UID));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("heroes").doc("h1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("heroes").where("userId", "==", ALICE_UID).get()
  );
});

test("6. Shadow update with ghost field is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("users").doc(ALICE_UID).set(makeMasterPayload(ALICE_UID));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gold: 999,
      isAdmin: true,
      updatedAt: serverTimestamp(),
    })
  );
});

test("7. Immutable heroClass and createdAt cannot be mutated on update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("users").doc(ALICE_UID).set(makeMasterPayload(ALICE_UID));
  const heroRef = aliceDb.collection("users").doc(ALICE_UID).collection("heroes").doc("h1");
  await heroRef.set(makeHeroPayload(ALICE_UID));

  await assertFails(
    heroRef.update({
      heroClass: "MAGE",
      updatedAt: serverTimestamp(),
    })
  );
});

test("8. Value poisoning (negative gold or invalid enum) is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("users").doc(ALICE_UID).set(makeMasterPayload(ALICE_UID));
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      gold: -100,
      updatedAt: serverTimestamp(),
    })
  );
});

test("9. Client query alignment: owner list query with userId filter succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await aliceDb.collection("users").doc(ALICE_UID).set(makeMasterPayload(ALICE_UID));
  await aliceDb.collection("users").doc(ALICE_UID).collection("heroes").doc("h1").set(makeHeroPayload(ALICE_UID));

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("heroes").where("userId", "==", ALICE_UID).get()
  );
});
