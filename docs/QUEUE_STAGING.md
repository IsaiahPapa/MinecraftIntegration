# Queue staging checklist

The queue is intentionally global and in-memory. The Minecraft logical server
owns scheduling, while the client mirrors complete queue snapshots for the HUD.
The activity feed remains capped at five notifications lasting five seconds.

## Automated gate

Run these before opening the staging client:

```bash
./gradlew test
./gradlew build
```

## In-game staging

Use a fresh single-player world with **Queue System**, **Queue Sidebar**, and
**Activity Feed** enabled in the mod config.

1. Join the world and confirm that an empty queue does not show the sidebar.
2. Run `/creati test taunt parkour`; confirm Parkour appears under **ACTIVE**.
3. During Parkour, run:
   - `/creati test taunt tntrun`
   - `/creati test taunt crt`
   - `/creati test taunt punch`
   - `/creati test buff frostbite 30`
4. Confirm **UP NEXT** shows typed `GAME`, `FX`, `TAUNT`, and `BUFF` entries,
   their redeemers, a maximum of four rows, and `+N more` when applicable.
5. Confirm each enqueue creates one activity-feed toast with its queue position.
6. Finish or leave Parkour. Confirm the buff releases, ordinary taunts stagger,
   the next minigame starts, and the visual effect remains paused until no
   minigame is active.
7. Trigger the same visual effect twice. Confirm the second trigger extends it
   and the progress bar restarts from the newly extended remaining duration.
8. Enable and disable Safe Mode. Confirm its HUD updates immediately and clears
   when the timer expires or Safe Mode is disabled.
9. Disable **Queue System** while entries are waiting. Confirm the sidebar clears
   and later taunts fire immediately. Re-enable it and confirm it starts empty.
10. Leave to the title screen, join another world, and confirm no old active or
    queued items appear.
11. Repeat at GUI scales 1 through 4. Confirm the sidebar remains on-screen,
    long names truncate with an ellipsis, and the effect bar stays inside it.
12. Toggle the sidebar and activity feed independently and confirm neither toggle
    changes actual queue scheduling.

## Acceptance criteria

- Every queued minigame, visual effect, ordinary taunt, and buff exists in the
  client snapshot and is represented by the sidebar or its `+N more` count.
- Login, logout, world changes, queue disable, and the final dequeue cannot leave
  stale HUD state.
- No backend connection is required to preserve client/server synchronization
  during the current play session.
- The activity feed never exceeds five visible notifications and does not become
  a persistent event log.
