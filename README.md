# OrbitalPlus 🪨💥

Rod call big boom from sky.
Every number in one file. You change. Boom change.

Plugin for **Paper 26.x** Minecraft server. Need **Java 25**.

## What it do

1. Op get magic rod.
2. Op point rod. Right-click.
3. Sky beam show where boom go.
4. Boom fall from sky.

16 kind of boom:

| Boom | What fall |
|---|---|
| `nuke` | TNT rain. Many ring. |
| `stab` | TNT tower go down. Make deep hole. |
| `wither_nuke` | Blue skull rain. Many ring. |
| `wither_stab` | Blue skull wall go straight down. |
| `dripstone` / `dripstone_nuke` | Pointy rock fall. Hurt. |
| `anvil` / `anvil_nuke` | Heavy anvil fall. Bonk. |
| `creeper_nuke` | Angry green friend fall. Hiss. |
| `warden_nuke` | Loud scream wave. Push and hurt. |
| `tntcart_nuke` | TNT cart fall. |
| `wolf` | Wolf pack fall. Bite enemy. |
| `railgun` | Arrow line shoot from your eye. |
| `railgun_wide` | Many arrow line from your eye. |
| `railgun_aerial` | Arrow rain from sky. |
| `stasis` | Everything near stop moving. Frozen. |

## Put in server

1. Get `OrbitalPlus-1.0.0.jar` (make it, see below).
2. Drop in server `plugins` folder.
3. Restart server.
4. Done. Config live in `plugins/OrbitalPlus/config.yml`.

## Shout words (commands)

`/orb` same as `/orbital`. Short.

| Command | What happen | Who can |
|---|---|---|
| `/orbital list` | Show all boom | Anyone |
| `/orbital info <type>` | Show boom numbers | Anyone |
| `/orbital give <type> [player] [amount]` | Give rod. 1 to 64. | `orbitalplus.give` |
| `/orbital kit <type> [player] [color]` | Give box of 27 rod | `orbitalplus.give` |
| `/orbital strike <type> [x y z [world]]` | Boom now. No rod need. | `orbitalplus.admin` |
| `/orbital links` | Show boom-buttons | `orbitalplus.link` |
| `/orbital reload` | Read config again. No restart. | `orbitalplus.admin` |

Example:

```
/orb kit nuke Steve red
/orb give wolf
/orb strike wither_nuke 100 70 -50
```

## Rod use

- **Stand + right-click** → boom where you look. Up to 300 block far.
- Rod wait 3 second between boom.
- Rod never break. (Config can change.)

## Rod + button = trap 🔘

Rod can be friend with button, lever, or pressure plate.
Press it → boom.

**Make friend:**

1. *(Can skip)* Hold rod. Crouch. Right-click where boom should land.
2. Crouch. Right-click button / lever / plate with rod.
3. Friend made. Chat tell you where boom land.

Skip step 1? Boom land on button itself. Good trap. 😈

**Then:**

- Press button → boom.
- Lever up → boom. Lever down → nothing.
- Mob or player step on plate → boom. Pig too. Friend too.
- Wood plate also feel dropped item and arrow.
- Arrow hit button → boom.

**Cave rules:**

- Same button wait 5 second before boom again.
- Break button → friendship over.
- Maker online? Wolf no bite maker. Warden scream and freeze skip maker.
- ⚠️ TNT, skull, anvil, arrow hurt maker too. Stand far.
- `railgun` and `railgun_wide` no can link. They shoot from eye. Button no have eye. Use `railgun_aerial`.
- Only op make friend. Anyone can press.
- Links live in `plugins/OrbitalPlus/links.yml`.

## Who can do what (permissions)

All default to op.

| Permission | Let you |
|---|---|
| `orbitalplus.admin` | Everything. Has all below. |
| `orbitalplus.give` | Give rod and kit |
| `orbitalplus.use` | Shoot rod |
| `orbitalplus.link` | Link rod to button. See `/orbital links`. |

## Change numbers

Open `plugins/OrbitalPlus/config.yml`. Every number there.
More TNT? More wolf? Bigger boom? Change number.
Then type `/orb reload`. No restart.

Some big ones:

```yaml
general:
  max-entities-per-strike: 3000   # most thing one boom can make. Too high = server freeze.
  allow-block-damage: true        # false = boom hurt, no break block
  warning-delay-ticks: 30         # sky beam time before boom. 20 tick = 1 second.
links:
  cooldown-seconds: 5             # button rest time
```

⚠️ **Careful:** big numbers make many thing. Many thing make server slow or freeze. Go up little at a time.

## Make jar yourself

Need JDK 25 and Gradle 9.

```
gradle build
```

Jar show up in `build/libs/`.

(`build.bat` is for maker's own PC. It look for tools in `.tools` folder. You no have that folder. Use `gradle build`.)

## License

MIT. Take it. Use it. Change it. Share it. Keep license file with it. See [LICENSE](LICENSE).
