# Editable party commands

Open `/lidl > QoL features`, right-click **PartyCMDS**, then choose **Edit commands /
replies**. The module's main toggle still enables/disables all handling. The editor
shows existing built-in commands, local overrides, and downloaded room replies.

- **Add command**: enter a trigger such as `!hello`, then type an answer.
- **Add answer**, **Previous**, **Next**, and **Drop** edit a pool of random answers.
  Each command supports up to 32 answers, each up to 256 characters.
- Variables: `{player}` is the sender, `{args}` is everything after the trigger,
  and `{roll:1:6}` rolls an inclusive random integer. Multiple rolls are independent.
- **Preview** generates an example without sending chat or publishing.
- **Save locally** / **Local only** stores the definition on your device.
- While connected to a room, the primary button becomes **Save & share**. It sends
  that definition to the room and closes only after acceptance. It doesn't upload
  unrelated local commands. Other connected clients download it automatically.
- **On/Off** in the list is a local choice. For a room command it creates a local
  override; to share an enabled/disabled state, edit it and use **Save & share**.
- **Remove** deletes a local override first. If there is no local override, it
  deletes the room definition for everyone (requires a connection). Removing a
  definition restores any shared or built-in fallback underneath it.

Local replies take priority over room replies, which take priority over the old
built-in handlers. Successfully sharing a reply removes that local override so
later room edits reach you. A local override is deliberately not replaced by room
updates. Remove it to follow the room again. Both libraries are bounded to 64
commands; downloaded replies are cached only for the configured server and room.

Every existing editable command starts with its original answer pool, wording and
random ranges. **Reset base** restores those responses in the draft; Save commits
the reset, while Cancel discards it. Existing local and room edits are preserved.
Saving an unchanged base retains the original generated behaviour, including special
player rules, the question requirement, and slot-machine symbols/jackpots. Editing
the base creates a text override. Custom slot replies can use `{slot1}`, `{slot2}`,
`{slot3}` and `{jackpot}` to retain the original symbol pool and jackpot suffix.
`!help` automatically lists enabled commands; `!help` and `!ban` are reserved and
cannot be replaced or distributed through the room. The existing !ban toggle and
special handler remain separate.

Replies are plain party-chat text, not executable scripts or arbitrary Minecraft
commands. Responses retain the existing 300 ms delay and one-second cooldown.
Generated text is limited to 240 characters, controls are removed from arguments,
and command-like replies are prefixed to prevent reply loops. Pending replies are
cancelled when disabled or when the Minecraft connection changes. Raw incoming
chat is no longer written to debug logs.

# Private room sharing

Choose **Shared room** from PartyCMDS or the command editor. Enter:

1. Your relay's WebSocket URL; local test default: `ws://127.0.0.1:8765`.
2. A room name (1–48 letters, digits, underscores or hyphens).
3. A shared room token of 12–128 characters, then **Connect / retry**.

The first connection creates the room and sets its token. Other people use the
same URL, room and token. The field is masked; connection details, including the
token, are saved locally in `config/lidlclient.json`. Do not share that config file.
No external service is contacted until you explicitly connect. Room sharing is off
at each launch; local and matching cached replies still work offline. Use WSS
(TLS) when hosting beyond your own machine; the included relay is a local test
server, not an automatically hosted public service.

Connections retry every five seconds after failure. On reconnect the current room
snapshot is downloaded; offline local edits aren't silently uploaded. **Disconnect**
stops retries and restores standalone reply handling. While a connection is wanted
but unavailable, outgoing automatic replies pause so disconnected clients don't
all answer the same event.

Each saved room update has a revision. If another person edited first, the server
rejects the stale update, downloads the latest room state, and leaves your draft
open. Use **Room** to compare the current first answer, then review and save
again, or use Local only. The relay persists accepted updates
before broadcasting them; restarting it keeps the library and revisions.

Connected clients claim party events using a hash of server address and incoming
message. Only the first claimant is permitted to respond; identical events are
suppressed for two seconds. The relay receives the hash, not party-chat text. This
coordination applies only to clients in the same connected room; independent rooms,
unmodified clients and clients deliberately set to standalone mode aren't coordinated.

# Run a local test

From the repository root, using Python 3.12+:

```sh
python -m venv .venv
.venv/bin/pip install -r tools/party-sync/requirements.txt
.venv/bin/python tools/party-sync/server.py
```

The default server listens only on `127.0.0.1:8765`; its state is saved to
`tools/party-sync/rooms.json` (ignored by git). Use `--port`, `--host`, and `--data`
to change these. For two computers, host the relay where both can reach it and
terminate TLS in front of it for a `wss://` endpoint; localhost on another computer
refers to that other computer, not the relay host.

For the first Minecraft test, use two modded clients/accounts in the same party:

1. Connect both to the same relay room/token; confirm Connected on each.
2. On A add `!hello` with `Hello {player}: {args}`, then Save & share.
3. B should immediately list it as a room command. Test it in party chat; expect
   one response across the two clients, rather than one from each.
4. Edit the reply on B; verify A receives the new response. Remove the room reply
   and verify it disappears on both. Restart the relay and reconnect.
5. Open the same reply editor on both, save on A, then save on B; B must review a
   conflict before overwriting A. Test local overrides, module Off, disconnecting,
   reconnecting, and cancelling drafts too.

Automated relay tests:

```sh
.venv/bin/python -m unittest discover -s tools/party-sync -p 'test_*.py' -v
```

With a running relay, the Java WebSocket integration test is enabled by:

```sh
PARTY_SYNC_TEST_URL=ws://127.0.0.1:8765 ./gradlew test
```

CI runs the relay tests, starts a local relay, and runs the actual Java client
integration test alongside the normal build. These tests cover creation, edits,
deletions, revision conflicts, malformed payloads, room/token isolation, reconnect
snapshots, persistence, and competing response claims. They don't substitute for
the two-account Minecraft/mod-pack test above.
