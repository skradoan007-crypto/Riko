# P6 — evolution and scheduled analysis
Generation advances transactionally once when a history row crosses a meaningful
listen threshold: half a track, minimum 30 seconds and maximum 4 minutes. Counted
elapsed playing time excludes pauses/buffering and seek distance. Duration checkpoints
are written every five seconds; up to five seconds can be lost on stop/process death.
Generation is capped at 64 and cannot decrease. Profile identity/seed is unchanged.
The renderer gradually increases surface detail and fades in bounded extra layers.

Added native JobScheduler service (no WorkManager existed): charging/battery/storage
constraints, thermal and low-memory checks, eight-song batches, persisted cursor,
one shared decoder, cancellation and rescheduling. Explicit Start/Stop controls live
in MDNA Lab. Invalid files advance the cursor; future requests revisit them. Existing
profiles are reused. Reboot persistence requires RECEIVE_BOOT_COMPLETED permission.

Actual ESSL regression probe passed: six variation renders, independent band response,
18 viewport/tier stress cases. No Android build executed, per user instruction.
Android JobScheduler/Hilt, Room transaction, playback-background/process recovery,
thermal and battery behaviour remain unverified. Generation currently accrues only
for profiles already analyzed; previous unprofiled listens are retained in history
but not backfilled. Listening observation uses the existing PlayerViewModel lifetime.
These two limitations must be resolved for full service-owned evolution integration.
