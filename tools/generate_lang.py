#!/usr/bin/env python3
"""Writes the en_us localisation file, including the full Chronicle narrative text."""
import json
import os

RES = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   "src", "main", "resources")
OUT = os.path.join(RES, "assets", "aetherius", "lang", "en_us.json")

L = {}

# ------------------------------------------------------------------ meta
L["itemGroup.aetherius.aetherius"] = "Aetherius: Echoes of the Shattered Sky"
L["category.aetherius"] = "Aetherius"
L["key.aetherius.chronicle"] = "Open the Chronicle"
L["key.aetherius.inscribe"] = "Inscribe next rune (Sneak: clear)"

# ------------------------------------------------------------------ blocks
blocks = {
    "star_vein_ore": "Star-Vein Ore",
    "aether_blood_ore": "Aether-Blood Ore",
    "star_ash": "Star-Ash",
    "starlit_grass_block": "Starlit Grass",
    "mana_lily": "Mana Lily",
    "aether_fountain": "Aether Fountain",
    "celestial_pillar": "Celestial Pillar",
    "aether_forge": "Aether Forge",
    "aether_altar_core": "Aetherial Altar Core",
    "telescope": "Telescope of the Ancients",
    "rune_lantern": "Rune Lantern",
    "sky_rift_anchor": "Sky-Rift Anchor",
    "sky_rift_portal": "Sky-Rift",
}
for key, value in blocks.items():
    L[f"block.aetherius.{key}"] = value

# ------------------------------------------------------------------ items
items = {
    "aether_shard": "Aether Shard",
    "aether_blood": "Aether-Blood",
    "void_dust": "Void Dust",
    "star_ash_clump": "Star-Ash Clump",
    "heart_of_sol": "Heart of Sol",
    "heart_of_luna": "Heart of Luna",
    "heart_of_terra": "Heart of Terra",
    "heart_of_caelum": "Heart of Caelum",
    "astral_wand": "Astral Wand",
    "eclipse_wand": "Eclipse Wand",
    "astrolabe": "Astrolabe of the Void",
    "aethersight_monocle": "Aethersight Monocle",
    "chronicle_tome": "Chronicle Tome",
    "starlight_elixir": "Starlight Elixir",
    "purified_starlight_elixir": "Purified Starlight Elixir",
    "aether_manta_spawn_egg": "Aether-Manta Spawn Egg",
    "void_sentinel_spawn_egg": "Void Sentinel Spawn Egg",
    "void_specter_spawn_egg": "Void Specter Spawn Egg",
    "void_warped_abomination_spawn_egg": "Void-Warped Abomination Spawn Egg",
    "nox_thal_spawn_egg": "Nox'Thal Spawn Egg",
}
for key, value in items.items():
    L[f"item.aetherius.{key}"] = value

L["item.aetherius.aethersight_monocle.desc"] = "A single lens of pressed starlight. It shows the Weave beneath the world."
L["item.aetherius.aethersight_monocle.hint"] = "Wear it in the Deep Dark or the Nether to see what eats the sky."
L["item.aetherius.astrolabe.desc"] = "Points toward the nearest recorded Observatory, and toward any open Sky-Rift."
L["item.aetherius.chronicle_tome.desc"] = "Your record of the Fallen Starlight. It writes itself as you walk."
L["item.aetherius.chronicle_tome.hint"] = "Right-click to read the Chronicle."
L["item.aetherius.wand.tier.astral"] = "Astral - steady, forgiving"
L["item.aetherius.wand.tier.eclipse"] = "Eclipse - sharper, hungrier"
L["item.aetherius.wand.inscription"] = "Inscription: %s"
L["item.aetherius.wand.preview"] = "Resolves to: %s"
L["item.aetherius.wand.controls"] = "Right-click: cast   |   Sneak + right-click: clear   |   R: inscribe"
L["item.aetherius.starlight_elixir.desc"] = "Restores 40 Mana. Tastes faintly of cold iron."
L["item.aetherius.purified_starlight_elixir.desc"] = "Refills Mana and vents 30 Astral Tension. Grants Starcleanse."
L["aetherius.wand.empty"] = "The wand is uninscribed."
L["aetherius.monocle.blight_revealed"] = "The Weave is torn here."

# ------------------------------------------------------------------ entities
entities = {
    "aether_manta": "Aether-Manta",
    "void_sentinel": "Void Sentinel",
    "void_specter": "Void Specter",
    "void_warped_abomination": "Void-Warped Abomination",
    "embers_warden": "Warden of Embers",
    "tides_warden": "Warden of Tides",
    "stones_warden": "Warden of Stones",
    "storms_warden": "Warden of Storms",
    "gravity_anchor": "Gravity Anchor",
    "nox_thal": "Nox'Thal, the Eclipse Dragon",
}
for key, value in entities.items():
    L[f"entity.aetherius.{key}"] = value

# ------------------------------------------------------------------ runes
runes = {
    "ember": ("Ignis", "A low Sol rune. Heat, kindled and thrown."),
    "flare": ("Flaris", "A middle Sol rune. It splits stone and pride alike."),
    "sunfire": ("Solaris", "A high Sol rune. The old sun's last instruction."),
    "tide": ("Unda", "A low Luna rune. Water remembers being a sky."),
    "frost": ("Gelu", "A middle Luna rune. Stillness, taught to blood."),
    "moonwell": ("Lunaris", "A high Luna rune. A well that fills from the far side."),
    "stone": ("Lapis", "A low Terra rune. The world's refusal to move."),
    "wall": ("Murus", "A middle Terra rune. A promise made of rock."),
    "deepstone": ("Terranox", "A high Terra rune. The weight beneath the weight."),
    "gale": ("Ventus", "A low Caelum rune. The sky's smallest opinion."),
    "lightning": ("Fulgur", "A middle Caelum rune. A judgement, spoken in light."),
    "skybreak": ("Caelus", "A high Caelum rune. The authority to open the roof."),
    "shadow": ("Umbra", "A low Nox rune. The cost of being seen."),
    "rift": ("Hiatus", "A middle Nox rune. A door that resents being used."),
    "eclipse": ("Eclipse", "A high Nox rune. The dragon's own signature."),
}
for key, (title, desc) in runes.items():
    L[f"aetherius.rune.{key}"] = title

# ------------------------------------------------------------------ spells
spells = {
    "ember_lance": ("Ember Lance", "A spear of Sol that burns and weakens what it pierces."),
    "tide_of_renewal": ("Tide of Renewal", "Luna mends the caster and slows everything nearby."),
    "stone_bulwark": ("Stone Bulwark", "Terra raises a wall exactly where you are not standing."),
    "gale_step": ("Gale Step", "Caelum throws you forward and cushions the landing."),
    "void_rend": ("Void Rend", "Nox tears life out of a target and pours part of it into you."),
    "solar_flare": ("Solar Flare", "Sol + Sol. A close nova of fire and knockback."),
    "stormfire": ("Stormfire", "Sol + Caelum. Lightning that sets the ground alight."),
    "frost_lance": ("Frost Lance", "Luna + Caelum. Freezes movement, then the marrow."),
    "rampart": ("Rampart", "Terra + Terra. A siege wall, five wide and four tall."),
    "gravity_well": ("Gravity Well", "Nox + Nox. A singularity that pulls enemies into one bad place."),
    "mire_of_stars": ("Mire of Stars", "Terra + Luna. Roots your enemies and shields you."),
    "umbral_step": ("Umbral Step", "Caelum + Nox. A short blink with a concussive wake."),
    "nova_warp": ("Nova Warp", "Caelum + Sol + Nox. Blink to your aim, land in a blinding fireburst."),
    "starfall_judgement": ("Starfall Judgement", "Sol + Sol + Caelum. Eight meteors fall where you look."),
    "aegis_of_tides": ("Aegis of Tides", "Luna + Terra + Terra. A healing shell that punishes melee."),
    "eclipse_devour": ("Eclipse Devour", "Nox + Nox + Nox. Mass life-drain. The fastest road to a backfire."),
    "skybreak_ascension": ("Skybreak Ascension", "Caelum + Caelum + Caelum. Flight, then a lightning crown."),
    "forgeheart_transmutation": ("Forgeheart Transmutation", "Terra + Terra + Sol. Turns stone into ore veins."),
    "resonant_echo": ("Resonant Echo", "An unmatched sequence. Each rune gives what it can."),
}
for key, (title, desc) in spells.items():
    L[f"aetherius.spell.{key}"] = title
    L[f"aetherius.spell.{key}.desc"] = desc

# ------------------------------------------------------------------ cast feedback
L["aetherius.cast.ok"] = ""
L["aetherius.cast.client_side"] = "That did not happen."
L["aetherius.cast.unknown_combination"] = "The runes refuse each other."
L["aetherius.cast.not_enough_mana"] = "Not enough Mana."
L["aetherius.cast.on_cooldown"] = "The Weave has not recovered."
L["aetherius.cast.silenced"] = "Something is holding the Weave shut."
L["aetherius.cast.no_wand"] = "You need a wand in hand."
L["aetherius.cast.backfiring"] = "Your mana pool is still screaming."
L["aetherius.inscribe.added"] = "Rune inscribed."
L["aetherius.inscribe.cleared"] = "Inscription cleared."
L["aetherius.inscribe.unknown_rune"] = "You do not know that rune."
L["aetherius.inscribe.preview"] = "Sequence resolves to %s"
L["aetherius.inscribe.selected"] = "Selected rune: %s"

# ------------------------------------------------------------------ HUD
L["aetherius.hud.volatile"] = "MANA VOLATILITY"

# ------------------------------------------------------------------ Chronicle
L["aetherius.chronicle.title"] = "The Chronicle of the Shattered Sky"
L["aetherius.chronicle.chapter.act_i"] = "I. The Fallen Starlight"
L["aetherius.chronicle.chapter.act_ii"] = "II. The Weave & the Rot"
L["aetherius.chronicle.chapter.act_iii"] = "III. The Shattered Horizon"
L["aetherius.chronicle.chapter.appendix"] = "Appendix"
L["aetherius.chronicle.objectives"] = "Undertakings"
L["aetherius.chronicle.rewards"] = "Boon"
L["aetherius.chronicle.footer"] = "Act %s   Runes %s   Tension %s"
L["aetherius.chronicle.no_selection"] = "Choose an entry."
L["aetherius.chronicle.close"] = "Close"
L["aetherius.chronicle.lore_header"] = "Recovered pages"
L["aetherius.chronicle.milestone_reached"] = "Milestone reached."
L["aetherius.chronicle.epilogue_badge"] = "Eclipse ended"
L["aetherius.chronicle.tracked"] = "Tracking %s"
L["aetherius.chronicle.unknown"] = "an unknown entry"
L["aetherius.chronicle.quest_complete"] = "Chronicle updated: %s"

# ------------------------------------------------------------------ guide tab
L["aetherius.chronicle.chapter.guide"] = "Guide"
L["aetherius.guide.pick"] = "Pick a page on the left."
L["aetherius.guide.start.title"] = "Getting started"
L["aetherius.guide.start.p1"] = ("Press K at any time to open this Chronicle. It does not pause the "
                                 "world, so close it with Esc before a fight.")
L["aetherius.guide.start.p2"] = ("The Astral Wand is the tool of the trade: a stick and two Aether "
                                 "Shards in a diagonal shape. Hold it and right-click to cast the "
                                 "runes you have inscribed.")
L["aetherius.guide.start.p3"] = ("Aether Shards come from Star-Vein Ore, found under the ground of "
                                 "the Shattered Sky, and from most Aetherius creatures. A Chronicle "
                                 "Tome (book + shard + ink sac) opens this book from your hand.")
L["aetherius.guide.start.p4"] = ("Commands, if you play with cheats: /aetherius spells, /aetherius "
                                 "quests, /aetherius rune list, /aetherius mana fill.")
L["aetherius.guide.runes.title"] = "Runes and spells"
L["aetherius.guide.runes.p1"] = ("Magic is written in runes. Fifteen of them exist, and every "
                                 "combination of up to three carves out a spell - eighteen spells "
                                 "in all, from light to gravity.")
L["aetherius.guide.runes.p2"] = ("Press R to inscribe the next rune you know, Shift+R to clear the "
                                 "line. The HUD by the crosshair shows the line and which spell it "
                                 "will cast.")
L["aetherius.guide.runes.p3"] = ("Casting costs mana, which refills on its own. It also builds "
                                 "Astral Tension: past 50 your spells hit twice as hard and cost "
                                 "twice as much, and at 100 the pool backfires. Stop casting and "
                                 "the tension bleeds away.")
L["aetherius.guide.runes.p4"] = ("New runes are Chronicle rewards. /aetherius rune list prints what "
                                 "you know; /aetherius rune unlock takes one without cheating the "
                                 "quest that grants it.")
L["aetherius.guide.world.title"] = "The Shattered Sky"
L["aetherius.guide.world.p1"] = ("The Shattered Sky is an Aetherius dimension with ordinary, "
                                 "walkable ground: Starlit Meadows, the broken highlands of the "
                                 "Shattered Sky itself, and the Blighted Barrens where the rot has "
                                 "taken hold.")
L["aetherius.guide.world.p2"] = ("To get there the honest way, place four Sky-Rift Anchors within "
                                 "twelve blocks of each other and socket a different Elemental "
                                 "Heart into each; the portal opens in the middle of the ring.")
L["aetherius.guide.world.p3"] = ("Or use /aetherius dimension to travel, and /aetherius dimension "
                                 "return to come back. You always arrive on solid ground.")
L["aetherius.guide.world.p4"] = ("The Barrens spread blight over time; /aetherius blight info "
                                 "reports the current world state. An Aether Altar rite burns it "
                                 "back, and an Aether Fountain refills mana and bleeds off tension.")
L["aetherius.guide.boss.title"] = "The Nox'Thal"
L["aetherius.guide.boss.p1"] = ("Nox'Thal, the Star-Eater, circles the arena above the Shattered Sky "
                                 "highlands. It has three phases and the bar at the top of the "
                                 "screen changes colour with each one.")
L["aetherius.guide.boss.p2"] = ("In its second phase it cannot be reached in the air: it summons "
                                 "gravity anchors. Destroy every anchor and it is pulled down, and "
                                 "from then on it can be hit.")
L["aetherius.guide.boss.p3"] = ("The third phase is the Supernova. The sky goes black and the screen "
                                 "dims to a slot - glowing Rune Lanterns are what you can still see, "
                                 "so place them before the fight.")
L["aetherius.guide.boss.p4"] = ("Between phases it shields itself for a few seconds. Do not waste "
                                 "cooldowns on the transition; it is invulnerable until the bar "
                                 "settles into its new colour.")
L["aetherius.guide.craft.title"] = "Forges, altars, lanterns"
L["aetherius.guide.craft.p1"] = ("The Aether Forge (shards and lapis around deepslate) is where "
                                 "Aetherius weapons and tools are made; anything the Astral Wand "
                                 "cannot do, the forge can.")
L["aetherius.guide.craft.p2"] = ("The Aether Altar runs the purification rite: build the ring it "
                                 "asks for, then right-click it holding two Aether Blood (or one "
                                 "Purified Starlight Elixir). It burns the blight out of the ground "
                                 "around it and leaves you Star-Cleansed.")
L["aetherius.guide.craft.p3"] = ("Everything else - the Celestial Pillar, the Telescope, the "
                                 "Astrolabe, the Rune Lantern, the Aether Fountain - is craftable "
                                 "from vanilla materials plus shards. Read the recipe book in the "
                                 "Astral Wand's crafting output.")

objectives = {
    "break_star_vein": "Break a Star-Vein vein",
    "collect_aether_shard": "Collect Aether Shards",
    "collect_aether_blood": "Collect Aether-Blood",
    "craft_astrolabe": "Craft the Astrolabe of the Void",
    "find_observatory": "Find a Starlight Observatory",
    "repair_telescope": "Repair the Telescope of the Ancients",
    "slay_void_sentinel": "Slay a Void Sentinel",
    "wear_monocle_underground": "Wear the Monocle where the world is deep",
    "wear_monocle_deep_dark": "Wear the Monocle in the Deep Dark",
    "wear_monocle_nether": "Wear the Monocle in the Nether",
    "witness_blight": "Witness Celestial Blight with the Monocle",
    "cast_void": "Cast void magic",
    "build_altar": "Raise the Aetherial Altar of Purification",
    "perform_rite": "Perform a purification rite",
    "heart_sol": "Claim the Heart of Sol",
    "heart_luna": "Claim the Heart of Luna",
    "heart_terra": "Claim the Heart of Terra",
    "heart_caelum": "Claim the Heart of Caelum",
    "open_sky_rift": "Open the Sky-Rift",
    "enter_shattered_sky": "Cross into the Shattered Sky",
    "destroy_anchors": "Destroy Gravity Anchors",
    "slay_nox_thal": "End Nox'Thal",
}
for key, value in objectives.items():
    L[f"aetherius.objective.{key}"] = value

L["aetherius.reward.rune"] = "Rune knowledge"
L["aetherius.reward.max_mana"] = "Maximum Mana"
L["aetherius.reward.mastery"] = "School mastery"
L["aetherius.reward.experience"] = "Experience"
L["aetherius.reward.lore"] = "Recovered lore"
L["aetherius.reward.tension_resistance"] = "Tension resistance"

quests = {
    "act1_starfall": ("A Light Beneath the Stone",
        "You broke a vein that had no business glowing. For three heartbeats the cavern filled with falling stars, and something very old noticed you noticing it."),
    "act1_blood": ("The Blood of Gods",
        "The ore weeps when struck. The old texts called it Aether-Blood: what a sky-god leaves behind when it dies above a world it loved."),
    "act1_astrolabe": ("An Instrument for the Blind",
        "Astrolabes were built to be read at night, by people who had already lost the sky. Build one, and it will point at the temples the gods left behind."),
    "act1_observatory": ("The Broken Eye",
        "The Observatories saw the First Convergence coming and chose to watch anyway. Their telescopes are cracked; their lenses are not."),
    "act1_herald": ("The Sentinel at the Door",
        "Something has been guarding the Observatories since before your language existed. It will not recognise your credentials."),
    "act2_sight": ("What the Monocle Shows",
        "Wear the Monocle where light cannot lie. The Weave becomes visible - and so does the rot eating it."),
    "act2_rot": ("The Rot Has a Name",
        "Aspect Corruption. Every void spell you have thrown has been a small incision in the world, and something has been living in the wound."),
    "act2_altar": ("A Machine for Forgiving Yourself",
        "Five by five by five. Quartz to hold the shape, lapis to remember it, pillars to carry the load, and a core that does not care what you did."),
    "act2_purify": ("The First Rite",
        "The altar will not erase your choices. It will only stop the grass from turning to ash."),
    "act3_hearts": ("Four Keys, Four Wardens",
        "Each Warden has kept a Heart since the sky broke. They were told the Hearts would be needed. They were not told by whom."),
    "act3_portal": ("Opening the Horizon",
        "Four Hearts, four anchors, one wound. The Sky-Rift is not a door - it is a seam, and you are the needle."),
    "act3_arrival": ("The Shattered Sky",
        "Gravity here is a suggestion. Time is a rumour. Below the islands there is no below."),
    "act3_anchors": ("Grounding the Unreasonable",
        "Nox'Thal cannot be struck while it is held up by its anchors. Break the anchors and the dragon becomes a thing with weight again."),
    "act3_nox": ("The Eclipse Dragon",
        "The last sky-god did not shatter at the First Convergence. It hid. It has been eating the light of every world since, waiting for someone to be angry enough to find it."),
}
for key, (title, lore) in quests.items():
    L[f"aetherius.quest.{key}.title"] = title
    L[f"aetherius.quest.{key}.lore"] = lore

lore = {
    "first_convergence": ("The First Convergence",
        "When the sky-gods fell, they did not fall in one place. They fell in argument. Each one insisted the world below should be remade in their own image, and the argument lasted a hundred years and ended with all of them breaking. Their blood cooled in the dark and became ore; their thoughts cooled into runes. Whatever is left of the argument is still happening, quietly, in the veins under your feet."),
    "sky_temples": ("On Sky-Temples",
        "The Observatories were not places of worship. They were places of record - built so that someone, someday, would know precisely how the sky had broken. Their builders expected to be remembered correctly. The statues were never finished."),
    "telescope": ("The Telescope of the Ancients",
        "It does not magnify. It reconciles. Pointed at the moon, a working telescope shows what the moon was before the Convergence and will be after - and the lens remembers which runes were invented under each phase."),
    "void_sentinels": ("The Sentinels",
        "The Sentinels were made to guard the temples from pilgrims. They are 80% immune to things thrown at them, and entirely immune to being told they are obsolete. Their shields are not magic; they are grief with a very good shape."),
    "aethersight": ("Aethersight",
        "Wearing the Monocle is uncomfortable. The Weave is not a web of light - it is a web of promises, most of them broken. Look too long and you learn exactly how many immortals have lied to you."),
    "celestial_blight": ("Celestial Blight",
        "Blight is not poison. It is exposure. Every void cast opens the world a little wider, and something behind the Weave leans in to look. Grass dies first because grass has the least to say for itself. Animals go next, but they come back wrong and angry, wearing the blight as a personality."),
    "purification": ("On Purification",
        "The altar works by offering the world a better alternative to the rot - not by destroying the rot. Star-Ash remembers being grass. Give it one honest day of quartz and lapis and moonlight, and it will remember out loud."),
    "elemental_hearts": ("The Elemental Hearts",
        "Four Hearts, one per surviving school of creation. They are not organs. They are opinions so concentrated they became physical: Sol never stopped burning, Luna never stopped pulling, Terra never stopped holding, Caelum never stopped leaving. The Sky-Rift has no opinion. It is therefore indifferent to being asked."),
    "shattered_sky": ("The Shattered Sky",
        "The Shattered Sky is what is above the world when the sky is not a lie. Islands hold their shape out of habit. Storms come from the future and leave going backwards. Do not trust the horizon; it is a memory that flatters itself."),
    "aether_mantas": ("Aether-Mantas",
        "The mantas are older than the first attempt at a calendar. They swim through nothing because nothing never told them it was impossible. They are also, perversely, the easiest thing in the Shattered Sky to befriend: they like gentle hands and slow conversation."),
    "nox_thal": ("Nox'Thal, the Eclipse Dragon",
        "Nox'Thal is not the strongest of the fallen. It is the one that learned to eat what it killed instead of mourning it. In its first phase it is a herald - bright, theatrical, immune to anything with a flight arc. In its second phase it takes gravity personally. In its third phase it removes the light, because it has decided the light was the problem."),
    "epilogue": ("Epilogue: What Is Left",
        "The Eclipse ends. The sky above the island is still broken, but it is broken in a way that lets light through, and there is a difference. Somewhere a telescope reconciles. Somewhere a sentinel lowers its shield because there is finally nothing to guard against. You have not fixed the world. You have made it answerable - which is all a person with a wand was ever able to do."),
}
for key, (title, body) in lore.items():
    L[f"aetherius.lore.{key}.title"] = title
    L[f"aetherius.lore.{key}.body"] = body

# ------------------------------------------------------------------ bosses + systems
boss = {
    "phase1": "Phase I - The Starlit Herald",
    "phase2": "Phase II - Gravity Collapse",
    "phase3": "Phase III - Supernova",
    "invulnerable": "Nox'Thal reshapes, indifferent to your timing.",
    "ranged_immune": "The Eclipse scales refuse projectiles.",
    "anchored": "The Gravity Anchors hold the dragon up. Break them.",
    "anchors_summoned": "Four Gravity Anchors chained the air. Destroy them all.",
    "grounded": "The anchors break. Nox'Thal falls into a shape that can bleed.",
    "gravity_flip": "Gravity inverts.",
    "supernova": "The light is gone. Navigate by rune.",
    "anchor_hint": "Break the Gravity Anchors to ground the dragon",
    "defeated": "The Eclipse is over.",
}
for key, value in boss.items():
    L[f"aetherius.boss.nox_thal.{key}"] = value

L["aetherius.blight.seeded"] = "Celestial Blight seeded (intensity %s)"
L["aetherius.blight.purified"] = "Purified %s chunk(s)"
L["aetherius.forge.incomplete"] = "The forge is not formed (%s blocks out of place)"
L["aetherius.forge.awaiting_item"] = "Hold an item for the forge to work on."
L["aetherius.forge.needs_blood"] = "The forge is thirsty for Aether-Blood."
L["aetherius.forge.wand_ascended"] = "Your wand drinks the forge light: Eclipse."
L["aetherius.forge.already_infused"] = "This item has already been infused."
L["aetherius.forge.infused"] = "Infusion complete."
L["aetherius.forge.invalid_item"] = "The forge does not know what to do with that."
L["aetherius.altar.incomplete"] = "The Altar of Purification is not fully built (5x5x5)."
L["aetherius.altar.cooldown"] = "The altar is cooling: %s"
L["aetherius.altar.needs_offering"] = "Offer a Purified Starlight Elixir or two Aether-Blood."
L["aetherius.altar.rite_complete"] = "Purification complete: %s chunk(s) cleansed."
L["aetherius.telescope.repair_requirements"] = "The lens needs 4 Aether Shards and 1 Aether-Blood."
L["aetherius.telescope.repaired"] = "The Telescope reconciles once more."
L["aetherius.telescope.wait_for_moon"] = "This phase has nothing new to teach today."
L["aetherius.telescope.already_known"] = "You already know %s."
L["aetherius.telescope.rune_granted"] = "The moon teaches you %s."
L["aetherius.anchor.socketed"] = "Socketed %s"
L["aetherius.anchor.filled"] = "This anchor already holds a Heart."
L["aetherius.anchor.needs_heart"] = "The anchor needs an Elemental Heart."
L["aetherius.anchor.rift_open"] = "The Sky-Rift tears open."
L["aetherius.astrolabe.no_signal"] = "The needle spins and finds no temple."
L["aetherius.astrolabe.ping"] = "Temple at %s blocks, bearing %s (%s)"
L["aetherius.starfall.beginning"] = "The sky remembers how to fall."
L["aetherius.storm.begin"] = "A temporal storm begins."
L["aetherius.storm.announce"] = "Time is moving sideways. Keep to the islands."
L["aetherius.rift.unavailable"] = "The Shattered Sky is not loaded."
L["aetherius.warden.heart_dropped"] = "The Warden yields %s"
L["aetherius.command.rune_unlocked"] = "Unlocked %s"
L["aetherius.command.dimension_enter"] = "Stepped through the Sky Rift into the Shattered Sky"
L["aetherius.command.dimension_return"] = "Returned to the Overworld"
L["aetherius.command.dimension_missing"] = "The Shattered Sky dimension is not registered"

with open(OUT, "w", encoding="utf-8") as fh:
    json.dump(L, fh, indent=2, ensure_ascii=False)
    fh.write("\n")

print(f"wrote {len(L)} translation keys to {OUT}")
