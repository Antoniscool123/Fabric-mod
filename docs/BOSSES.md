# Mobs, Bosses & Encounter Design

All numbers are pulled from the entities' `createMobAttributes()` and their ability constants;
this file is the human-readable mirror of the code. If you retune a constant, update the table.

---

## 1. Roster

| Entity | HP | Armor | ATK | Speed | Role |
|---|---|---|---|---|---|
| Aether-Manta | 24 | – | – | 0.25 / fly 0.5 | passive flying fauna, rideable |
| Void Specter | 24 | – | 5 | 0.30 | debt-collector; drains mana on hit |
| Void-Warped Abomination | 32 | 2 | 6 | 0.24 | blight fauna, spawns from infected chunks |
| Void Sentinel | 80 | 8 | 9 | 0.26 | anti-projectile guard |
| Elemental Warden | 180 | 12 | 14 | 0.27 | four variants, drops Act III hearts |
| Gravity Anchor | 60 | – | – | 0.0 | immobile phase-2 key |
| **Nox'Thal** | **900** | **20** | **26** | **0.30 / fly 0.6** | final boss, three phases |

Common attributes across the roster: follow ranges 28–80 blocks, knockback resistance
0.6–1.0 for the heavy units, and all Void mobs are immune to their own damage source
(`DamageTypeTags`-checked where relevant).

---

## 2. Aether-Manta

**Behaviour.** `MantaDriftGoal` (in `entity/ai`) picks a point in a 24×8×24 box around the
manta, steers by direct velocity (no navigation graph — the Shattered Sky has no ground
path), and idles 60–180 ticks between drifts. `travel()` applies buoyancy so the manta
settles near its spawn altitude.

**Riding.** Right-click to mount. `RIDE_SPEED = 0.34`; the rider steers with look direction;
sneaking dismounts. Mantas are the intended way to cross the archipelago before Caelum
magic unlocks true flight.

**Spawn.** `creature` spawns in `aetherius:shattered_sky` (weight 12) and
`aetherius:starlit_meadows` (weight 8).

---

## 3. Void Specter

**Trigger.** Spawned by `PlayerMagicCapability.spawnVoidSpecters` on backfire (2–4),
and naturally in blighted chunks / the Shattered Sky.

**Special.** Each successful hit drains **8 mana and adds 6 tension** to the victim, and
applies brief Nausea. This is the mechanic that closes the loop: backfire spawns Specters,
Specters raise tension, tension causes another backfire.

**Counter.** Kill them fast, or leave the chunk. They are fragile (24 HP) but fast.

---

## 4. Void-Warped Abomination

Blight's foot soldiers. Spawn when chunk intensity ≥ 40, weight 20 in
`aetherius:blighted_barrens`. They leave Star-Ash behind and are *cured* (despawned) inside
a purified radius. Slow, tanky for their tier, dangerous in groups of 4+.

---

## 5. Void Sentinel — "the door with opinions"

| Constant | Value |
|---|---|
| `PROJECTILE_ABSORPTION` | 0.80 (80% of projectile damage ignored) |
| `SHOCKWAVE_COOLDOWN` | 160 t (8 s) |
| `SHOCKWAVE_CHARGE` | 18 t telegraph |
| `SHIELD_DOWN_AFTER_FIRE` | 60 t (3 s) vulnerability window) |
| Shockwave radius | 13 blocks |

**Loop.** Arrows and thrown potions are a waste of arrows: 80% absorption turns a 10-damage
shot into 2. The Sentinel fires a shockwave every 8 seconds after an 18-tick charge
(audible/visible tell), then drops its shield for 3 seconds. Melee and spells during the
window do full damage.

**Design intent.** Teach the player that the mod's answer to problems is the wand, not the
bow — which is exactly what Act I's fifth quest asks for.

**Loot.** Act I quest credit on kill (`VOID_SENTINEL_KILLED`), Void Dust, occasional
Aether Shard.

---

## 6. Elemental Warden (4 variants)

Single entity class with a `Variant` enum (`SOL`, `LUNA`, `TERRA`, `CAELUM`), so all four
share AI and only differ in ability payload. Ability cadence: every `200 × castScale`
ticks, where the variant's ability is:

| Variant | Ability | Effect |
|---|---|---|
| Embers (Sol) | Ember Nova | radial fire damage + ignition |
| Tides (Luna) | Undertow | pull + slow, heals itself |
| Stones (Terra) | Bulwark | raises cover, gains resistance |
| Storms (Caelum) | Stormcall | lightning at the target, knockback |

Each variant plays its own cast sound (`SPELL_SOL/LUNA/TERRA/CAELUM`) — the audio is the
tell, so a player who has learned the schools can read the fight without a HUD.

**Drops.** `heart_of_sol/luna/terra/caelum` (+ Chronicle counter `WARDENS_SLAIN`).

---

## 7. Gravity Anchor

| Property | Value |
|---|---|
| HP | 60 |
| Movement | 0 (immobile, knockback-immune) |
| `PULSE_INTERVAL` | 80 t |
| `PULSE_INVERSION_TICKS` | 70 t |
| Ring radius (boss summon) | 14 blocks from arena centre |
| Count summoned | 4 per phase-2 entry |

**Behaviour.** Every 80 ticks an anchor pulses: nearby players get Levitation/inverted
gravity for 70 ticks and are pulled toward the anchor. Destroying one notifies the boss
(`onAnchorDestroyed`) so the "grounded" state can flip.

**Why it exists.** Anchors are a rehearsal. Act III quest 4 asks the player to destroy 8 of
them; the phase-2 fight then uses the identical mechanic under pressure, with the twist
that the boss is invulnerable until all four die.

---

## 8. Nox'Thal, the Eclipse Dragon

### Attributes & arena
`900 HP · 20 armor · 26 attack · fly speed 0.6 · follow range 80 · knockback resistance 1.0`.
Arena radius **48 blocks** (`ARENA_RADIUS`) — outside it the boss disengages, and every
announcement/AOE is scoped to that sphere.

### Phase thresholds
| Phase | Health window | Name | Bar colour |
|---|---|---|---|
| 1 | 100%–67% | Starlit Herald | gold |
| 2 | 67%–34% | Gravity Collapse | cyan `#4DD2FF` |
| 3 | 34%–0% | Supernova | orange `#FF8A3D` |

Transitions cost **50 ticks of invulnerability** (`TRANSITION_INVULNERABILITY`), during
which the boss announces the phase and the custom boss bar redraws with a notch at the
threshold. The vanilla `BossBar.Color` enum cannot express per-phase RGB, so
`NoxThalBossBarOverlay` draws the real bar from live entity health plus
`ClientEffects.bossPhase`.

### Shared state
`NoxThalEntity` owns everything world-level: anchors list, `grounded`, `supernovaActive`,
`supernovaTicks`, elytra-lock timers, and a `BossBlackboard` refreshed once per tick
(boss, target, distance, visibility, tick counter, short/long memory maps).

### Phase 1 — The Starlit Herald
- **Ranged immunity**: any damage whose source is a projectile is deflected with the
  message `aetherius.boss.nox_thal.ranged_immune` (`DamageTypeTags.IS_PROJECTILE`).
- **Behaviour tree**: has-target → cooldown(`swoop`, 100 t) → cooldown(`cosmic_fire`, 140 t)
  → cooldown(`reposition`, 80 t). Swoop is a 12-damage strafing pass; Cosmic Fire drops a
  line of `AreaEffectCloud` fire along the approach.
- **Tell**: roar + gold bar + the fact that your arrows bounce off.
- **Lesson**: close the distance, use direct-damage spells.

### Phase 2 — Gravity Collapse
- **Enter**: summons **4 Gravity Anchors** in a ring of radius 14, roars, locks elytra for
  30 s, and becomes **damage-immune while any anchor lives** (damage is deflected with
  `aetherius.boss.nox_thal.anchored`).
- **Gravity inversion**: every `GRAVITY_INVERSION_PERIOD = 400 t (20 s)` for
  `GRAVITY_INVERSION_DURATION = 70 t`, applied arena-wide.
- **Elytra suppression**: refreshed every 40 ticks for 20 s at a time — flight is the
  phase's whole point, so the boss takes it away.
- **Tree**: hover-high (120 t) while anchors live → ground slam (90 t) once grounded →
  tail sweep (140 t) fallback.
- **Lesson**: kill anchors under gravity pressure; the ground slam is the reward window.

### Phase 3 — Supernova
- **Enter**: `beginSupernova()` — 600 ticks of client-side darkness
  (`SUPERNOVA_DARKEN` boss event), refreshed every 240 ticks so night vision cannot
  trivialise it.
- **Star strikes**: `actionStarStrike()` telegraphs then detonates at up to 6 spots
  (every player plus every Rune Lantern within range), 26-tick delay, **9 magic damage**,
  **2.6-block radius**. Rune Lanterns are visible in the dark — placing them before the
  fight is real preparation.
- **Elytra**: re-locked every 80 ticks for 15 s.
- **Tension surge**: every 200 ticks, `forceTensionSurgeArena()` pushes nearby players'
  Astral Tension up, which can cause a backfire *mid-fight* — the boss's cruellest move.
- **Tree**: star strike (45 t) → eclipse claw within 12 blocks (70 t) → drift (100 t).
- **Ranged immunity is OFF** in phase 3: it is a damage race against visibility.

### Defeat
`onDeath` (via mixin `LivingEntityDeathMixin` → `ChronicleManager`) sets
`FLAG_NOX_THAL_SLAIN`, awards the hidden quest `act3_nox`, announces the epilogue line and
plays the defeat sound. The boss bar is removed, darkness cleared and screen shake reset
through `BossEventS2CPacket.reset()`.

### Behaviour-tree API (used by all three phases)

```java
Behaviors.selector(
    Behaviors.sequence(
        Behaviors.condition("anchors alive", bb -> ((NoxThalEntity) bb.boss()).hasActiveAnchors()),
        Behaviors.cooldown("reposition_high", 120,
            Behaviors.action("hover", bb -> ((NoxThalEntity) bb.boss()).actionReposition(22.0)))),
    Behaviors.cooldown("tail_sweep", 140,
        Behaviors.action("tail sweep", bb -> ((NoxThalEntity) bb.boss()).actionTailSweep())));
```

- `BTStatus` = `SUCCESS | FAILURE | RUNNING`.
- Nodes: `SelectorNode`, `SequenceNode`, `InverterNode`, `ConditionNode`, `ActionNode`,
  `WaitNode`, `CooldownNode`. `CooldownNode` keys its timer in blackboard memory as
  `"cd:" + key`, so a tree can be reset safely without losing pacing.
- `BehaviorNode#reset()` is called on phase enter/exit, never per tick.
- Every node carries a `label()` used by `/aetherius boss phase <0-2>` to force a branch (the branch name is printed in the boss bar).

---

## 9. Encounter checklist (for players)

| Fight | Bring |
|---|---|
| Void Sentinel | a wand, not a bow; patience for the 3 s window |
| Elemental Warden | the school that opposes the variant's element |
| Gravity Anchors | a pick or any Terra spell; watch your elytra timer |
| Nox'Thal P1 | direct-damage spells, no arrows |
| Nox'Thal P2 | anti-fall (slow falling / Gale Step), anchor-breaking DPS |
| Nox'Thal P3 | Rune Lanterns placed beforehand, a full mana pool, low tension |

The last row is the whole design thesis: **prepare, don't react**. Nox'Thal never introduces
a mechanic the player hasn't already solved once in the open world.
