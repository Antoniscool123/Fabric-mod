# Aetherius — Narrative Bible

> The sky is not gone. It is *unfinished*.

This document is the canon reference for **Aetherius: Echoes of the Shattered Sky**. Every
quest, item description and boss line in the mod defers to it. If a future content pass
contradicts this file, this file is what changes first.

---

## 1. Cosmology

### The Weave
The substrate under ordinary matter: a lattice of intent that spells are cut into. Mana is
what the Weave *gives up* when you pull on it. Astral Tension is the debt left behind.
Tension is not "evil energy" — it is the Weave remembering that it was used.

### The First Convergence
Roughly a hundred years of argument compressed into one event. The sky-gods arrived to
remake the world, could not agree on the shape of it, and broke each other instead. What
they left behind:

| Remainder | Physical form |
|---|---|
| their blood | Aether-Blood, cooling into ore veins under stone |
| their thoughts | the 15 runes |
| their eyes | the Telescope of the Ancients |
| their arguments | the Sky-Temples (Starlight Observatories) |
| their hunger | Nox'Thal |

The Convergence did not end. It is still happening, slowly, at the bottom of the world.

### The Shattered Sky
The dimension is the *inside* of the Convergence: an archipelago of island fragments
orbiting a point where the argument was loudest. Gravity is inconsistent because the
pieces were never supposed to be near each other. The "sky" is the underside of the
world that was destroyed to make this one.

---

## 2. Factions & entities

### The Fallen (sky-gods)
Never named individually except through their schools. They are not characters; they are
positions in an argument. Sol wanted permanence, Luna wanted forgetting, Terra wanted
stillness, Caelum wanted motion, Nox wanted an ending. All five are correct and that is
the problem.

### Nox'Thal, the Eclipse Dragon
Not the strongest of the Fallen — the one that **learned to eat what it killed instead of
mourning it**. It survived the Convergence by consuming light, and it has been eating
light ever since. Its three phases are not "health bars"; they are an argument it is still
having with itself:

1. **The Starlit Herald** — theatrical, bright, contemptuous. Refuses to be hit by
   anything with a flight arc (ranged immunity). It is *performing* divinity.
2. **Gravity Collapse** — takes gravity personally. Inverts it, pins you with Anchors,
   hides behind them. It is *defending* a position it can no longer articulate.
3. **Supernova** — removes the light, because it has decided the light was the problem.
   It is no longer arguing with anyone; it is finishing the sentence.

### The Sentinels
Void Sentinels are the Fallen's border guards. Their shield absorbs 80% of projectile
damage and their counter is a shockwave — the design lesson being "stop shooting it."
They were built to guard doors that no longer have rooms behind them.

### Void Specters
The debt-collectors of the Weave. They spawn when a mage's Astral Tension goes critical,
drain mana on hit, and are the mechanical consequence of the setting's central rule:
**power is borrowed, never owned**.

### Void-Warped Abominations
Blight fauna. Once ordinary mobs, now chewing on the argument. They are the only enemies in
the mod that are unambiguously victims rather than antagonists.

### Aether-Mantas
Fauna of the Shattered Sky. Gentle, rideable, and the only living thing that navigates the
archipelago without instruments. Their drifting pattern is a hint: the islands have a
current.

### Elemental Wardens
Four constructs (Embers/Tides/Stones/Storms) holding the Hearts. They are not guarding the
Hearts from you — they are guarding the Hearts from *being used*, which is a distinction
the player only understands in Act III.

---

## 3. Act I — The Fallen Starlight

**Premise.** You are a miner. You find ore that is warm.

**Beat 1 — A Light Beneath the Stone.** Star-Vein Ore breaks into a Star-fall: a meteor
shower that seeds shards across the surface. The first quest completes when a vein breaks
and two Aether Shards are held. The reward is a *book* — the Chronicle — and the mod's
first statement: this is a story about documentation, not prophecy.

**Beat 2 — The Blood of Gods.** Aether-Blood is ore that behaves like tissue. Four of them
unlock the rune Flaris and the first taste of Sol magic.

**Beat 3 — An Instrument for the Blind.** Astrolabes were built to be read at night by
people who had already lost the sky. Crafting one reveals the Sky-Temples on the Chronicle
map: Starlight Observatories, one per region, each claimable once.

**Beat 4 — The Broken Eye.** Inside an Observatory sits the Telescope of the Ancients.
It is phase-locked: it accepts an offering only on the moon phase matching its etched
rune. Repairing it unlocks Lunaris and Caelum mastery — the player learns that the sky is
*legible*.

**Beat 5 — The Sentinel at the Door.** A Void Sentinel guards the Observatory's lower
door. It is immune to arrows. Killing it with a wand — the only weapon the story has given
you — closes Act I and sets `act1_complete`.

**Lore unlocked:** *The First Convergence*, *On Sky-Temples*, *The Telescope of the
Ancients*, *The Sentinels*.

---

## 4. Act II — The Weave & The Rot

**Premise.** The ore is not a resource. It is an infection with a good publicist.

**Beat 1 — What the Monocle Shows.** Crafting the Aethersight Monocle and wearing it in
the Deep Dark or the Nether attunes you to the Weave. This is the mod's first *perceptual*
gate: the Blight exists before this quest, but you cannot see it, and several systems
(Star-Ash conversions, chunk intensity) stay invisible until attunement. Sets
`act2_attuned`.

**Beat 2 — The Rot Has a Name.** Watch a blighted chunk long enough and cast Void magic ten
times. Void is the school that lets you see the debt side of the Weave; using it near the
Blight is how the player learns that the Blight *is* the debt. Sets `blight_witnessed`.

**Beat 3 — A Machine for Forgiving Yourself.** The 5×5×5 Aetherial Altar of Purification:
a core, a ring of Celestial Pillars, lapis and quartz flooring, an Aether Fountain at one
cardinal. The quest title is the thesis of Act II — the altar does not undo the Blight, it
*forgives* it locally.

**Beat 4 — The First Rite.** One purification: 48-block radius, Star-Ash → Starlit Grass,
intensity reset, Void mobs weakened. Sets `blight_purified`. Reward includes 25% tension
resistance — mercy makes magic cheaper.

**Lore unlocked:** *Aethersight*, *Celestial Blight*, *On Purification*.

---

## 5. Act III — The Shattered Horizon

**Premise.** The four Wardens were right. You do it anyway.

**Beat 1 — Four Keys, Four Wardens.** Kill each Elemental Warden for its Heart. The
Chronicle is honest: the Hearts are keys, and keys imply a lock someone built on purpose.

**Beat 2 — Opening the Horizon.** Four Hearts into a Sky-Rift Anchor opens the portal.
Sets `sky_rift_open`.

**Beat 3 — The Shattered Sky.** Entering the dimension sets `shattered_sky_reached` and
introduces low gravity, temporal storms and the drifting Mantas.

**Beat 4 — Grounding the Unreasonable.** Destroy eight Gravity Anchors. This is a tutorial
for Nox'Thal phase 2 disguised as a quest: anchors are exactly what makes the boss
invulnerable later.

**Beat 5 — The Eclipse Dragon.** Nox'Thal. Hidden until the previous quest completes,
because the Chronicle does not name what it has not yet seen.

**Epilogue — What Is Left.** The Eclipse ends. The sky above the island is still broken,
but it is broken in a way that lets light through, and there is a difference. Somewhere a
telescope reconciles. Somewhere a sentinel lowers its shield because there is finally
nothing to guard against.

> You have not fixed the world. You have made it answerable — which is all a person with
> a wand was ever able to do.

---

## 6. Systems-as-story (design notes)

| Mechanic | Narrative claim |
|---|---|
| Mana/Tension dual meter | power is borrowed, never owned |
| Volatility at 50 tension | the Weave starts *choosing* for you |
| Backfire at 100 | debt collectors are real and they are Void Specters |
| Rune combinatorics | the Fallen argued in sentences; spells are quotations |
| Three-school "Concordance" discount | agreement is cheaper than insistence |
| Blight spread | damage you did not do is still yours |
| Altar purification | repair is local, slow and expensive |
| Phase-2 anchors | the boss defends with the same tools you used to arrive |
| Nox'Thal's ranged immunity in phase 1 | it will not be *examined*, only fought |

---

## 7. Tone guide for future content

- **Second person, past tense for history, present for mechanics.** "You have not fixed the
  world" — never "the player fixes the world."
- **No prophecy, no chosen one.** The player is competent, not destined.
- **Gods are arguments, not people.** Never give a Fallen a personality beyond its school.
- **Mercy is mechanical.** Every act of repair should change a number somewhere.
- **Endings are partial.** The epilogue is a *difference*, not a victory.
- **There is no quest-giver NPC.** The Chronicle is the
  narrator and it is written by someone who was there and did not survive.

---

## 8. Glossary

**Aether-Blood** — cooled divine tissue; ore, catalyst, portal fuel.
**Astral Tension** — the Weave's memory of being used; 0–100.
**Backfire** — what happens at 100 tension: pool explosion, Celestial Blindness, Specters.
**Celestial Blight** — Nox'Thal's digestion, visible only through Aethersight.
**Chronicle** — the book; quest log, lore archive and act structure in one.
**Concordance** — the 10% discount for three distinct schools in one sequence.
**Gravity Anchor** — a Fallen pin; Nox'Thal's phase-2 shield, your Act III homework.
**Heart** — an Elemental Warden's core; a key disguised as a trophy.
**Observatory / Sky-Temple** — a Fallen argument site with a repairable Telescope.
**Rune** — a cooled thought; 15 of them, three schools deep each.
**Star-fall** — the meteor shower released by breaking Star-Vein Ore.
**Temporal storm** — the Shattered Sky's weather; the argument, still loud.
**Weave** — the intent-lattice under matter.
