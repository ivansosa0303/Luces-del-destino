const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
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

test("unauthenticated user cannot read or write projects", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users/user1/projects").get());
  await assertFails(unauthedDb.doc("users/user1/projects/p1").set({
    userId: "user1",
    name: "Test",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("authenticated user can create and read their own project", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  await assertSucceeds(user1Db.doc("users/user1/projects/p1").set({
    userId: "user1",
    name: "Mi Proyecto",
    description: "Notas",
    colorHex: "#2563EB",
    createdAt: new Date(),
    updatedAt: new Date()
  }));
  await assertSucceeds(user1Db.collection("users/user1/projects").get());
});

test("user cannot access another user's projects", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  await user1Db.doc("users/user1/projects/p1").set({
    userId: "user1",
    name: "Proyecto Privado",
    createdAt: new Date(),
    updatedAt: new Date()
  });

  const user2Db = testEnv.authenticatedContext("user2").firestore();
  await assertFails(user2Db.doc("users/user1/projects/p1").get());
  await assertFails(user2Db.collection("users/user1/projects").get());
});

test("authenticated user can create document in project", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  await assertSucceeds(user1Db.doc("users/user1/documents/doc1").set({
    userId: "user1",
    projectId: "p1",
    title: "Manual",
    fileName: "manual.md",
    extension: "md",
    mimeType: "text/markdown",
    fileSizeBytes: 1200,
    sha256Checksum: "abcdef123456",
    textSummaryOrPreview: "Contenido",
    orderIndex: 0,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});

test("user cannot inject invalid document size or fake user id", async () => {
  const user1Db = testEnv.authenticatedContext("user1").firestore();
  // Negative size
  await assertFails(user1Db.doc("users/user1/documents/doc2").set({
    userId: "user1",
    title: "Invalido",
    fileName: "file.txt",
    extension: "txt",
    fileSizeBytes: -50,
    createdAt: new Date(),
    updatedAt: new Date()
  }));

  // Spoofed user id
  await assertFails(user1Db.doc("users/user1/documents/doc3").set({
    userId: "user2",
    title: "Spoof",
    fileName: "file.txt",
    extension: "txt",
    fileSizeBytes: 10,
    createdAt: new Date(),
    updatedAt: new Date()
  }));
});
