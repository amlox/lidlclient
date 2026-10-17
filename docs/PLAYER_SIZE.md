# Player size

1. Open /lidl, enable **Player size** in General, then right-click it.
2. Drag **Your size** from 0.25x to 3x. It changes only your own model.
3. Run /lidsize copy to copy your current size code to the clipboard.
4. Send that code to a friend. They need this version of LidlClient.
5. They run /lidsize import <code> to see your chosen size on their client.

Codes identify players by UUID. Importing a code enables Player size. Imported
sizes do not change the recipient's own slider, and importing your own code is rejected.
New size changes require copying and importing an updated code; there is no automatic
chat scanning or server synchronization. Codes are presets, not proof of authorship.

Use /lidsize reset to return your own size to 1x, and /lidsize clear to remove all
imported sizes. Disabling Player size turns off local and imported visual scaling.
Settings persist in config/lidlclient-player-size.json.

This is visual model scaling, visible in third-person and player previews. It
does not change hitboxes, reach, movement, camera height, first-person hands or server
attributes. Vanilla clients cannot see the changes.
