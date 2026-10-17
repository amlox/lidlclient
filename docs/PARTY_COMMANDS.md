# Party command switches

Open /lidl > Social and right-click **PartyCMDS** to show each command's toggle.
Each command, including !ban and !tesco, can be enabled or disabled separately.
Left-click the PartyCMDS header to enable/disable all command handling.
Individual choices are preserved while the main switch is off.

All switches default on to preserve the previous actual command behavior.
They persist in config/lidlclient-party-commands.json.
Disabled commands do not queue responses; queued replies are canceled if disabled
before sending. !ban is gated in party, co-op and both directions of private chat.
The manual fake-ban test also respects the !ban switch.
!help lists only currently enabled commands.
