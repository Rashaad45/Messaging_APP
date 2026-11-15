// Security rules tests. Each test acts as a (possibly malicious) client and checks the rules
// allow exactly what the app needs and nothing more.
// Run: `npm test` (starts the Firestore emulator, runs these, shuts it down).

import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  deleteDoc,
  doc,
  getDoc,
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-messenger',
    firestore: { rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') },
  });
});

after(async () => env?.cleanup());

beforeEach(async () => env.clearFirestore());

const db = (uid) => env.authenticatedContext(uid).firestore();
const anon = () => env.unauthenticatedContext().firestore();

/** The same write the Android app does: profile + username lock in one batch. */
function claim(firestore, uid, username, displayName = 'Ada Lovelace', overrides = {}) {
  const batch = writeBatch(firestore);
  batch.set(doc(firestore, `users/${uid}`), {
    username,
    displayName,
    displayNameLower: displayName.toLowerCase(),
    createdAt: serverTimestamp(),
    ...overrides,
  });
  batch.set(doc(firestore, `usernames/${username}`), { uid });
  return batch.commit();
}

/** Seed data directly, bypassing rules. */
async function seedUser(uid, username) {
  await env.withSecurityRulesDisabled(async (ctx) => {
    const f = ctx.firestore();
    await setDoc(doc(f, `users/${uid}`), {
      username,
      displayName: 'Seeded',
      displayNameLower: 'seeded',
      createdAt: new Date(),
    });
    await setDoc(doc(f, `usernames/${username}`), { uid });
  });
}

describe('username claim', () => {
  test('a user can claim a free username together with their profile', async () => {
    await assertSucceeds(claim(db('alice'), 'alice', 'alice_1'));
  });

  test('also works inside a transaction (as the app does)', async () => {
    const f = db('alice');
    await assertSucceeds(
      runTransaction(f, async (tx) => {
        const nameRef = doc(f, 'usernames/alice');
        if ((await tx.get(nameRef)).exists()) throw new Error('taken');
        tx.set(doc(f, 'users/alice'), {
          username: 'alice',
          displayName: 'Alice',
          displayNameLower: 'alice',
          createdAt: serverTimestamp(),
        });
        tx.set(nameRef, { uid: 'alice' });
      }),
    );
  });

  test('a taken username cannot be claimed again', async () => {
    await seedUser('alice', 'ada');
    await assertFails(claim(db('bob'), 'bob', 'ada'));
  });

  test('username lock alone (without the profile) is rejected', async () => {
    await assertFails(setDoc(doc(db('alice'), 'usernames/alice'), { uid: 'alice' }));
  });

  test('profile alone (without the username lock) is rejected', async () => {
    await assertFails(
      setDoc(doc(db('alice'), 'users/alice'), {
        username: 'alice',
        displayName: 'Alice',
        displayNameLower: 'alice',
        createdAt: serverTimestamp(),
      }),
    );
  });

  test('cannot claim a username on behalf of someone else', async () => {
    const f = db('mallory');
    const batch = writeBatch(f);
    batch.set(doc(f, 'users/mallory'), {
      username: 'victim',
      displayName: 'M',
      displayNameLower: 'm',
      createdAt: serverTimestamp(),
    });
    batch.set(doc(f, 'usernames/victim'), { uid: 'alice' });
    await assertFails(batch.commit());
  });

  test('cannot create another user\'s profile', async () => {
    await assertFails(claim(db('mallory'), 'alice', 'alice'));
  });

  test('a second username cannot be claimed once a profile exists', async () => {
    await seedUser('alice', 'alice');
    await assertFails(setDoc(doc(db('alice'), 'usernames/alice2'), { uid: 'alice' }));
  });

  test('invalid usernames are rejected', async () => {
    for (const bad of ['ab', 'a'.repeat(21), 'Alice', 'has space', 'dash-name', 'émile']) {
      await env.clearFirestore();
      await assertFails(claim(db('alice'), 'alice', bad), `should reject "${bad}"`);
    }
  });

  test('profile must use server time and only known fields', async () => {
    await assertFails(claim(db('alice'), 'alice', 'alice', 'Alice', { createdAt: new Date(0) }));
    await assertFails(claim(db('alice'), 'alice', 'alice', 'Alice', { isAdmin: true }));
    await assertFails(claim(db('alice'), 'alice', 'alice', 'Alice', { displayNameLower: 'not-matching' }));
    await assertFails(claim(db('alice'), 'alice', 'alice', ''));
  });

  test('username claims cannot be changed or released by clients', async () => {
    await seedUser('alice', 'alice');
    await assertFails(setDoc(doc(db('alice'), 'usernames/alice'), { uid: 'bob' }));
    await assertFails(deleteDoc(doc(db('alice'), 'usernames/alice')));
  });

  test('signed-out clients cannot claim', async () => {
    await assertFails(setDoc(doc(anon(), 'usernames/anon'), { uid: 'x' }));
  });
});

describe('profiles', () => {
  test('signed-in users can read profiles and usernames; signed-out cannot', async () => {
    await seedUser('alice', 'alice');
    await assertSucceeds(getDoc(doc(db('bob'), 'users/alice')));
    await assertSucceeds(getDoc(doc(db('bob'), 'usernames/alice')));
    await assertFails(getDoc(doc(anon(), 'users/alice')));
  });

  test('owner can change their display name', async () => {
    await seedUser('alice', 'alice');
    await assertSucceeds(
      updateDoc(doc(db('alice'), 'users/alice'), { displayName: 'Alice B', displayNameLower: 'alice b' }),
    );
  });

  test('username is immutable and others cannot edit the profile', async () => {
    await seedUser('alice', 'alice');
    await assertFails(updateDoc(doc(db('alice'), 'users/alice'), { username: 'queen' }));
    await assertFails(
      updateDoc(doc(db('bob'), 'users/alice'), { displayName: 'Hacked', displayNameLower: 'hacked' }),
    );
  });

  test('clients cannot delete profiles', async () => {
    await seedUser('alice', 'alice');
    await assertFails(deleteDoc(doc(db('alice'), 'users/alice')));
  });
});

describe('default deny', () => {
  test('unknown collections are not accessible', async () => {
    await assertFails(setDoc(doc(db('alice'), 'secrets/x'), { a: 1 }));
    await assertFails(getDoc(doc(db('alice'), 'secrets/x')));
  });
});
