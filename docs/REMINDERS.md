# Timers and reminders

Use **/reminders** or **/rem** to open the in-game GUI. These are local client
commands; they do not send chat to the server.

- **Mode: PC time** accepts a 24-hour time such as **18:30** and a custom message.
  It schedules the next occurrence in your computer's local timezone: today if
  still ahead, otherwise tomorrow. These are one-shot reminders, not daily repeats.
- **Mode: Timer** accepts durations such as **5m**, **30s**, **1h30m**, or **1d**.
  Enter the message and click **Start timer**.
- The active list shows both kinds, remaining time for timers, and the scheduled
  local day/time for reminders. Use the page arrows for longer lists and **Cancel**
  beside an entry to remove it. Text drafts survive list updates and resizing.
- **Test sound** plays the notification sound so you can check your game volume.

From chat, use **/timer 5m** for the default “Timer finished!” message, or
**/timer 5m Check the oven** for a custom one. **/timer** without arguments opens
the GUI. Durations accept d/h/m/s, including combinations, from 1 second to 365 days.
At most 128 pending entries can be saved; messages may contain up to 256 characters.

When due, an entry displays its message in local chat and above the hotbar and
shows a popup that remains visible over GUIs, and plays a chime. Nothing is sent to party chat. Simultaneous alarms are shown one
at a time, three seconds apart, so each gets a visible notification.

Pending entries persist in the existing LidlClient config across disconnects and
game restarts. Deadlines use the PC clock (changing that clock changes the wait).
The game cannot notify you while closed; overdue entries are delivered when you
next enter a world. Entries stay pending at the title screen. Completed/cancelled
entries are removed and do not repeat. Sound respects Minecraft's sound settings.
Clock reminders use the timezone at creation; daylight-saving gaps follow Java's
local-time adjustment rules.

## Party-command use

Set a party reply's output to **/Commands** and its answer to:
`/timer {args} Take a break!`

Then **!timer 5m** starts a local five-minute timer. To accept a bare number such as
**!timer 5**, use `/timer {args}m Take a break!` instead. Invalid/missing durations
show a local error. The new timer/reminder commands run locally when used as party
replies; other slash commands retain their existing server-command behavior.
The existing command switches, cooldown, and consecutive-use limit still apply.

## In-game checks

1. Run /timer 3s Test; verify exactly one local message, hotbar notification, and chime.
2. Open /rem, enter a PC time a minute ahead, and check the listed due time and alert.
3. Start two timers, cancel one, and verify the remaining one still fires.
4. Restart with a pending timer, wait past its deadline, and rejoin to see it once.
5. Add enough entries to page the list; resize and confirm drafts and Cancel remain correct.
6. Test a party reply with /timer {args}, including an invalid duration and its use limit.
