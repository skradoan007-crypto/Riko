# Genesis P3 — persist DNA profiles

Audited actual Room v5 entities, migration chain, database provider and play_history writer.
Added v6 dna_profile (songKey, seed, algoVersion, featureBlob, generation), additive
MIGRATION_5_6, DAO with insert-if-absent identity protection, monotonic generation
updates and a repository validating P1's versioned/checksummed serialization.
Removed destructive migration fallback: unsupported older schemas now fail safely
rather than erasing user data. Existing migrations 2->3->4->5 retained.

History audit: current writer inserts track-start events with duration=0. New count
query requires an explicitly positive listened-ms threshold and excludes those rows.
It does not invent completed listens. Accurate duration accumulation remains P6 work.

Validation: SQLite executed the exact migration SQL and preserved a sentinel favorite;
P1's 17 JVM contract groups and 3 independent golden vectors passed. Android test
creates a genuine Room v5 database, opens it through v6 migration, checks old table
counts/favorite value, history filtering, and profile identity/generation preservation.
No exported v5 schema/real-user fixture was present. The generated Room v5 fixture
and Android validation are NOT executed here: Gradle/SDK unavailable. No APK or
real-device performance evidence is claimed. Existing user tables are not modified.

Files: RikoDatabase.kt, DatabaseModule.kt, Daos.kt; new DnaMigration.kt,
DnaProfileEntity.kt, DnaProfileDao.kt, DnaProfiles.kt, DnaMigrationTest.kt and this report.
Timeline persistence is not included in featureBlob yet; it stores the P1 summary.
No screenshot is relevant to this storage phase. P4 proceeds under user's explicit
P3-through-P4 authorization; no intermediate user build is requested.
