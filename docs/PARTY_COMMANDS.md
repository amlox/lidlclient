# Party commands

Open `/lidl > QoL features`, right-click **PartyCMDS**, then choose **Edit commands /
replies**. Left-clicking the PartyCMDS module enables/disables all command handling.
Use each command's **On/Off** button in the list to control that command individually.
Those switches preserve your answers and output choice.

## Add or edit a command

- **Add command** creates a new trigger, such as `!hello`.
- Triggers are unique, including disabled commands and built-ins. `!Hello` and
  `!hello` are the same command. A duplicate warns you while typing and cannot be
  saved. To change an existing command, click its entry in the list instead.
- **Add answer**, **Previous**, **Next**, and **Drop** edit a pool of random answers.
  Each command supports up to 32 answers, each up to 256 characters. The local
  library supports 128 definitions.
- Variables: `{player}` is the sender, `{args}` is everything after the trigger,
  and `{roll:1:6}` rolls an inclusive random integer. Multiple rolls are independent.
- **Preview** generates an example without sending anything to party chat.
- **Save** commits the draft; **Cancel** discards it.
- **Remove** deletes a custom definition. For an edited built-in command, this
  restores its original behaviour.

Matching uses the whole trigger, so `!hi` and `!high` can coexist. Creating a second
`!dice` is blocked; editing the built-in `!dice` updates that single command.

## Output destination

The editor's output control selects where the result goes:

- **Output: Party chat** sends the response to the party through `/pc`.
- **Output: /Commands** executes a server slash command directly, without adding
  `/pc`. For example, an answer `/warp hub` runs `/warp hub` when triggered. Every
  answer in this mode must start with `/` followed by a command. Templates such as
  `/warp {args}` work; Preview only shows the result and never executes it.
- **Output: Only me** displays the result in your own chat, prefixed
  `[Party Commands — Only me]`. It does not send the result to the server.

The command still reacts to supported incoming party-chat triggers in either mode.
Output selection controls the destination; the list's On/Off switch controls whether
it runs. **Allow ! replies** bypasses both the saved-answer check and automatic
`[Reply]` prefix for responses starting with `!`. It defaults to Off. Turning it On
allows command chains and may cause replies to trigger each other; the existing
one-second cooldown still applies. There is no second Reply On/Off toggle inside the editor.

Old saved definitions default to Party chat, with slash-command execution and the
loop-protection bypass both off. Output choice persists across restarts,
command toggles and resetting base answers. The 300 ms response delay and one-second
cooldown apply to both output modes. Pending replies are cancelled when disabled
or when the Minecraft connection changes.

## Original bases

Every existing editable command starts with its original answer pool, wording and
random ranges. **Reset base** restores those responses in the draft; Save commits
the reset. Existing saved edits stay intact until you explicitly reset or remove them.
Saving an unchanged base retains the original generated behaviour, including special
player rules, the question requirement, and slot-machine symbols/jackpots. Editing
the base creates a text override. Custom slot replies can use `{slot1}`, `{slot2}`,
`{slot3}` and `{jackpot}` to retain the original symbol pool and jackpot suffix.

`!help` automatically lists enabled commands; `!help` and `!ban` remain reserved
built-in handlers. Their existing command switches are kept.

All definitions and choices are saved in `config/lidlclient.json`. Previously cached
room replies are retained as local definitions without replacing existing local edits;
old sharing connection settings and tokens are removed. There is no room connection,
relay server, WebSocket dependency, or sharing UI.

## In-game checks

1. Create `!hello`, then try creating `!HELLO`; expect a warning and no duplicate.
   Also try adding `!dice`, including when it is disabled. Edit the existing entry
   to change it instead.
2. Set `!hello` to Only me and trigger it in party chat. Only your own client should
   show the result. Switch to Party chat and confirm the party receives the result.
3. Switch the command off, then on; confirm the chosen output mode is retained.
   Restart and verify the saved answer, output mode and enabled state.
4. Edit and reset a base command, and check that Cancel discards the draft. Remove
   a custom override and confirm the original built-in response returns.

## Consecutive-use limit

In a command's editor, type a whole number into **Max in a row (0 = unlimited)**,
then Save. For example, 3 permits three consecutive outputs for that trigger and
silently ignores further attempts until a different command produces an output.
The count is shared across party members, ignores capitalization, and does not
reset when arguments change. Only completed outputs count; dropped, disabled,
unknown, cancelled, and invalid-command attempts do not reset or advance it.
The limit applies to Party chat, Only me, and /Commands, even with Allow ! replies on.

Existing definitions default to 0, preserving their behavior and original answers.
Limits persist across restarts and command On/Off changes; streaks are session-only
and reset on reconnect or when the whole module is turned off. Reset base changes
answers only. Reserved !help and !ban do not have editable definitions; this setting
is available for editable built-in commands and custom commands.
