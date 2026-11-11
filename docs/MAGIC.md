# Magic System — Numbers & Rules

Everything here is enforced by `api/magic` (resource model) and `api/spell` (casting).
Constants live as `public static final` fields so a balance patch is a one-line change.

---

## 1. The dual meter

| Constant | Value | Meaning |
|---|---|---|
| `BASE_MAX_MANA` | 100 | base pool; `getMaxMana()` grows with mastery |
| `MANA_REGEN_PER_SECOND` | 2.0 | passive regen |
| `FOUNTAIN_REGEN_MULTIPLIER` | 3.0 | regen within 6 blocks of an Aether Fountain |
| `TENSION_DECAY_PER_SECOND` | 0.4 | tension bleeds off when not casting |
| `VOLATILITY_THRESHOLD` | 50 | Mana Volatility begins here |
| `BACKFIRE_THRESHOLD` | 100 | Astral Backfire |
| `VOLATILITY_DAMAGE_MULTIPLIER` | 2.0 | spell damage while volatile |
| `VOLATILITY_COST_MULTIPLIER` | 2.0 | mana cost while volatile |
| `POST_BACKFIRE_TENSION` | 25 | where tension lands after a backfire |
| `CAST_LOCKOUT_TICKS` | 8 (≈0.4 s) | post-backfire cast lockout |
| `BACKFIRE_DURATION_TICKS` | 60 | shake/flash window |
| `CELESTIAL_BLINDNESS_TICKS` | 220 (≈11 s) | custom effect duration |

Tension is *not* symmetric with mana: it decays four times slower than mana regenerates,
so a burst-heavy build genuinely accumulates risk. The fountain multiplier is the only
lever that makes staying near a structure worth it.

### Mana Volatility (tension ≥ 50)
- Spell damage ×2, spell **mana** cost ×2.
- HUD bar switches to the volatile palette and the tension bar starts pulsing.
- `MagicUser#damageMultiplier()` returns 2.0 — all damage spells route through it, so a
  new spell automatically respects volatility.

### Astral Backfire (tension reaches 100)
Triggered automatically in `tick()` once `astralTension >= 100`, exactly once per cooldown
window (`backfireTicks <= 0`). Effects, in order:

1. `backfireTicks = 60`, `celestialBlindnessTicks = 220`, mana → 0, tension → 25.
2. Cast lockout for 60 ticks; gravity inversion cleared.
3. `ModStatusEffects.CELESTIAL_BLINDNESS` applied (+ vanilla Nausea, Darkness handled
   client-side by `ClientEffects`).
4. Pool explosion at the caster's feet: `world.createExplosion(..., ExplosionSourceType.MAGIC
   → NONE, ...)`, 140 ASTRAL_SPARK particles.
5. `spawnVoidSpecters` — 2–4 Void Specters around the caster.
6. `CelestialBlightManager.infect(world, pos, 3)` — the backfire *seeds the Blight*.
7. `AetherNetworking.sendScreenShake(world, pos, 24.0, 30)` — radius and ticks.
8. Audit log line: `Astral Backfire for <name> at <pos> (tension reset to 25)`.

Design consequence: a player who spams high-tension Void magic is personally responsible
for the Act II problem. That is intentional.

---

## 2. Schools

| School | Id | Theme | Signature runes |
|---|---|---|---|
| Sol / Pyromancy | `sol` | damage, light, ignition | Ignis, Flaris, Solaris |
| Luna / Aquamancy | `luna` | healing, tide, frost | Unda, Gelu, Lunaris |
| Terra / Terramancy | `terra` | walls, shields, transmutation | Lapis, Murus, Terranox |
| Caelum / Aeromancy | `caelum` | mobility, lightning, flight | Ventus, Fulgur, Caelus |
| Nox / Voidmancy | `nox` | drain, gravity, self-risk | Umbra, Hiatus, Eclipse |

Each school has three runes (tiers 1–3). Tension cost is a school property: Nox is by far
the most expensive (12 / 26 / 42 tension against Sol's 1 / 3 / 6).

## 3. Runes (15)

| Rune | School | Tier | Mana | Tension | Key |
|---|---|---|---|---|---|
| Ignis | Sol | 1 | 8 | 1 | `ember` |
| Flaris | Sol | 2 | 18 | 3 | `flare` |
| Solaris | Sol | 3 | 34 | 6 | `sunfire` |
| Unda | Luna | 1 | 7 | 0 | `tide` |
| Gelu | Luna | 2 | 16 | 0 | `frost` |
| Lunaris | Luna | 3 | 30 | 0 | `moonwell` |
| Lapis | Terra | 1 | 9 | 0 | `stone` |
| Murus | Terra | 2 | 20 | 1 | `wall` |
| Terranox | Terra | 3 | 32 | 4 | `deepstone` |
| Ventus | Caelum | 1 | 6 | 1 | `gale` |
| Fulgur | Caelum | 2 | 19 | 3 | `lightning` |
| Caelus | Caelum | 3 | 28 | 5 | `skybreak` |
| Umbra | Nox | 1 | 10 | 12 | `shadow` |
| Hiatus | Nox | 2 | 22 | 26 | `rift` |
| Eclipse | Nox | 3 | 36 | 42 | `eclipse` |

## 4. Cost formula (`MagicCosts.of`)

```
mana = Σ rune.mana
     + Σ (repeated-school rune.mana × 0.25 × streak)
tension = Σ rune.tension
        + Σ (repeated-school rune.tension × 0.15 × streak)
if distinctSchools == 3: mana × 0.9        # "Concordance" discount
final mana = ceil(mana)
```

`streak` increments for each consecutive rune of the same school (1 for the second, 2 for
the third). Practical results:

| Sequence (example runes) | Mana | Tension |
|---|---|---|
| `ignis` | 8 | 1 |
| `ignis+ignis` | 8+8+(8×0.25) = 18 | 1+1+0.15 = 2.15 |
| `ignis+ignis+ignis` | 24+2+4 = 30 | 3+0.15+0.30 = 3.45 |
| `ventus+ignis+umbra` (3 schools) | (6+8+10)×0.9 = 21.6 → 22 | 1+1+12 = 14 |
| `umbra+hiatus+eclipse` | 68+5.5+18 = 91.5 → 92 | 80+3.9+12.6 = 96.5 |

Reading the last row: a single Nox triple from zero tension lands at **96.5 tension** — one
more point and the player backfires. Any Nox triple is a deliberate all-in, which is why
its cooldown is 300 ticks. In general the trio spells are gated by cooldown (200–320 ticks)
rather than by cost alone.

## 5. Spell table (19 registered)

Base spells (one school, one rune):

| Spell | Pattern | School | Cooldown | Effect |
|---|---|---|---|---|
| Ember Lance | `sol` | Sol | 20 t | piercing fire projectile |
| Tide of Renewal | `luna` | Luna | 60 t | heal self + allies in radius |
| Stone Bulwark | `terra` | Terra | 100 t | temporary wall/shield |
| Gale Step | `caelum` | Caelum | 40 t | dash + brief slow-fall |
| Void Rend | `nox` | Nox | 30 t | short-range drain, high tension |

Duo spells (two runes, same or mixed school):

| Spell | Pattern | School | Cooldown | Effect |
|---|---|---|---|---|
| Solar Flare | `sol+sol` | Sol | 80 t | blinding burst around caster |
| Stormfire | `sol+caelum` | Caelum | 100 t | fire + chain lightning |
| Frost Lance | `luna+caelum` | Luna | 70 t | piercing frost line, slows |
| Rampart | `terra+terra` | Terra | 160 t | 5×3 defensive wall |
| Gravity Well | `nox+nox` | Nox | 140 t | pulls entities inward |
| Mire of Stars | `terra+luna` | Terra | 120 t | slowing hazard field |
| Umbral Step | `caelum+nox` | Nox | 90 t | teleport, leaves void trail |

Trio spells (three runes — the "sentences"):

| Spell | Pattern key | School | Cooldown | Effect |
|---|---|---|---|---|
| **Nova Warp** | `caelum+sol+nox` | Caelum | 200 t | blink to aim + fire burst with Blindness |
| Starfall Judgement | `sol+sol+caelum` | Sol | 260 t | meteor barrage at target area |
| Aegis of Tides | `luna+terra+terra` | Terra | 240 t | healing shell that punishes melee |
| Eclipse Devour | `nox+nox+nox` | Nox | 300 t | mass life-drain (fastest route to backfire) |
| Skybreak Ascension | `caelum+caelum+caelum` | Caelum | 280 t | true flight, then a lightning crown |
| Forgeheart Transmutation | `terra+terra+sol` | Terra | 320 t | converts stone to ore veins |

Fallback: any unrecognised sequence resolves to **Resonant Echo** (`caelum`, 120 t) — a cheap
"the Weave shrugs" effect instead of a failed cast, so players always get feedback.

### Resolution rules
1. Rune list is canonicalised (sorted by enum ordinal) before lookup — order never matters.
2. `SpellRegistry` matches exact patterns first, then longest-prefix, then fallback.
3. Mastery gates: a school's tier-3 runes require `mastery >= 1` in that school in most
   cases; the Chronicle grants mastery rewards.
4. `canCast` is checked before costs; costs are checked before execution; cooldown is
   applied only on success.

## 6. Wands

| Tier | Item | Cast behaviour |
|---|---|---|
| `ASTRAL` | Astral Wand | steady: full cost, cooldown as listed, no self-damage |
| `ECLIPSE` | Eclipse Wand | sharper: 8% chance per cast to lose 1 durability (break sound feedback) |

`WandItem` holds the inscribed runes (`RUNES_KEY` NBT list). Player flow:

- **R** — cycle known runes into the wand's sequence.
- **Shift+R** — clear the sequence.
- **Right-click** — cast, resolved from the inscribed sequence.

Inscribing requires knowing the rune (unlocked via Chronicle rewards) and passes through
`InscribeRuneC2SPacket` so the server validates ownership before mutating NBT.

## 7. HUD & feedback

`MagicHudOverlay` (HUD render callback, bottom-left):

- **Mana bar** — `gui/mana_bar` 128×16 fluid texture, width scaled by `mana/maxMana`.
- **Tension bar** — `gui/tension_bar`, with a hard notch at 50% (volatility) and a red
  pulse above 75%.
- **Rune pips** — up to 3 `gui/rune_pip` sprites showing the inscribed sequence.
- **Backfire flash** — full-screen tint driven by `ClientEffects` shake/darkness counters.
- **Cast feedback** — `CastFeedbackS2CPacket` carries success/fail reason to render a short
  action-bar line, so a client can never fake a successful cast.

Screen shake is server-driven (`ScreenShakeS2CPacket` with radius + duration), decayed
client-side in `ClientEffects.tickShake`.

## 8. Magic vs. Blight

- Blight intensity ≥ 40 in the caster's chunk spawns Void-Warped Abominations.
- Void spells near blight raise tension faster (the Weave is already damaged there).
- Backfire always infects the caster's chunk by +3 intensity — the clearest link between
  the two systems.
- Aethersight Monocle is required for the player to *see* intensity (particles + HUD hint);
  the underlying numbers are always live.

## 9. Balance notes

- Sol is the beginner school: cheap tension, low cooldown, good damage.
- Nox is the trap: enormous power, and every point of tension is a future Specter.
- Terra's shields are the only real answer to Void Sentinel shockwaves (ranged doesn't work).
- Caelum is the mobility tax: without Gale Step, Nox'Thal phase 2 will drop you.
- Three-school sequences are the intended endgame: highest cooldowns, best per-cast value
  because of the Concordance discount and volatility doubling.
