# Security Specification — Eternal Summoner

## 1. Data Invariants
1. **Strict Multi-Tenant Ownership**: Every `/users/{userId}` document and every subcollection document under `/users/{userId}/heroes/{heroId}` and `/users/{userId}/equipment/{itemId}` MUST belong exclusively to `request.auth.uid == userId` and carry `data.userId == request.auth.uid`.
2. **Parent Existence Invariant**: Neither a `HeroRecord` nor an `EquipmentRecord` can be created unless `/users/{userId}` already exists (`exists(...)`) or is being created atomically in the same batch (`existsAfter(...)`).
3. **Immutable Core Identity**: `userId` and `createdAt` on all entities are strictly immutable on update. Furthermore, `HeroRecord` identity fields (`name`, `title`, `heroClass`, `rarity`, `personality`) and `EquipmentRecord` structural fields (`slot`, `iconType`) cannot be mutated after creation.
4. **Zero PII**: No emails, phone numbers, or addresses are stored in Firestore documents.
5. **Bounded Sizes & Numeric Ranges**: Every string enforces explicit `.size() <= MAX` bounds; bounded lists (`unlockedFacilities`, `storyFlags`) enforce `.size() <= MAX`; every numeric field enforces strict non-negative and upper domain bounds.
6. **Query Enforcement**: `allow list` on `heroes` and `equipment` requires `request.auth.uid == userId && resource.data.userId == request.auth.uid` with zero `get()`/`exists()` calls inside `list`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Scrape**: Unauthenticated `get` or `list` on `/users/alice_123/heroes`. -> Rejected (`isSignedIn()`).
2. **Cross-User Profile Read**: `bob_456` reading `/users/alice_123`. -> Rejected (`request.auth.uid == userId`).
3. **Cross-User Hero Write**: `bob_456` creating `/users/alice_123/heroes/h1` with `userId: "bob_456"`. -> Rejected (`request.auth.uid == userId`).
4. **Identity Spoofing on Create**: `alice_123` creating `/users/alice_123/heroes/h1` with `userId: "bob_456"`. -> Rejected (`incoming().userId == request.auth.uid`).
5. **Orphaned Subcollection Write**: `alice_123` creating `/users/alice_123/heroes/h1` when `/users/alice_123` neither exists nor existsAfter. -> Rejected (`parentExistsOrBatchCreated`).
6. **Shadow Ghost Field Injection**: `alice_123` updating `/users/alice_123` with extra `{ "isAdmin": true }`. -> Rejected (`hasOnly` & `affectedKeys().hasOnly(...)`).
7. **Immutable Timestamp Tampering**: `alice_123` updating `createdAt` on `/users/alice_123`. -> Rejected (`incoming().createdAt == existing().createdAt`).
8. **Immutable Hero Class Mutation**: `alice_123` changing a Knight hero's `heroClass` to `"MAGE"` on update. -> Rejected (`affectedKeys().hasOnly(...)` excludes `heroClass` and `incoming().heroClass == existing().heroClass`).
9. **Resource Poisoning (Oversized ID)**: Creating `/users/alice_123/heroes/<200-char-junk-id>`. -> Rejected (`isValidId(heroId)`).
10. **Value Poisoning (Negative Gold / 1MB Name)**: Updating `gold: -500` or 5000-char `masterName`. -> Rejected (`isValidMasterProfile(incoming())`).
11. **Unbounded Array Injection**: Passing 50 strings in `unlockedFacilities`. -> Rejected (`unlockedFacilities.size() <= 10`).
12. **Unfiltered List Query**: Querying `heroes` where document `userId` mismatches caller or without ownership alignment. -> Rejected (`resource.data.userId == request.auth.uid`).
