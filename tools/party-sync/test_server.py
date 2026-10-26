import asyncio
import json
from pathlib import Path
import tempfile
import unittest
from websockets.asyncio.client import connect
from websockets.asyncio.server import serve
from websockets.exceptions import ConnectionClosed
from server import Relay, validate_reply

TOKEN = 'test-room-token-123456'
REPLY = dict(trigger='!hello', answers=['Hello {player}!'], enabled=True)

class RelayTests(unittest.IsolatedAsyncioTestCase):
    async def asyncSetUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.path = Path(self.temp.name) / 'rooms.json'
        self.relay = Relay(self.path)
        self.server = await serve(self.relay.handler, '127.0.0.1', 0, max_size=2_097_152)
        self.url = f'ws://127.0.0.1:{self.server.sockets[0].getsockname()[1]}'
        self.clients = []

    async def asyncTearDown(self):
        for client in self.clients:
            await client.close()
        self.server.close()
        await self.server.wait_closed()
        self.temp.cleanup()

    async def join(self, room='friends', token=TOKEN):
        client = await connect(self.url)
        self.clients.append(client)
        await client.send(json.dumps(dict(type='join', protocol=1, room=room, token=token)))
        return client, await self.receive(client)

    async def receive(self, client):
        return json.loads(await asyncio.wait_for(client.recv(), 3))

    async def send(self, client, **message):
        await client.send(json.dumps(message))

    async def test_create_edit_delete_and_reconnect_persist(self):
        a, _ = await self.join()
        b, _ = await self.join()
        await self.send(a, type='put', baseRevision=0, requestId='a', command=REPLY)
        self.assertEqual((await self.receive(a))['revision'], 1)
        self.assertEqual((await self.receive(b))['commands'], [REPLY])
        edited = dict(REPLY, answers=['New answer'], enabled=False)
        await self.send(b, type='put', baseRevision=1, requestId='b', command=edited)
        self.assertEqual((await self.receive(a))['commands'], [edited])
        await self.receive(b)
        self.assertEqual(Relay(self.path).rooms['friends']['commands']['!hello'], edited)
        c, snapshot = await self.join()
        self.assertEqual(snapshot['commands'], [edited])
        await self.send(a, type='delete', baseRevision=2, requestId='delete', trigger='!hello')
        self.assertEqual((await self.receive(a))['commands'], [])
        self.assertEqual((await self.receive(b))['commands'], [])
        self.assertEqual((await self.receive(c))['commands'], [])

    async def test_stale_revision_keeps_current_room(self):
        a, _ = await self.join()
        b, _ = await self.join()
        await self.send(a, type='put', baseRevision=0, requestId='a', command=REPLY)
        await self.receive(a)
        await self.receive(b)
        await self.send(b, type='put', baseRevision=0, requestId='stale', command=dict(REPLY, answers=['stale']))
        conflict = await self.receive(b)
        self.assertEqual(conflict['type'], 'conflict')
        self.assertEqual(conflict['commands'], [REPLY])
        self.assertEqual(conflict['revision'], 1)

    async def test_rooms_and_tokens_are_isolated(self):
        a, _ = await self.join()
        _, other = await self.join('other')
        self.assertEqual(other['commands'], [])
        with self.assertRaises(ConnectionClosed):
            await self.join(token='wrong-token-123456')
        await self.send(a, type='put', baseRevision=0, requestId='a', command=REPLY)
        await self.receive(a)
        _, other = await self.join('other')
        self.assertEqual(other['commands'], [])

    async def test_invalid_payload_and_reserved_commands_do_not_break_connection(self):
        a, _ = await self.join()
        await a.send('not json')
        self.assertEqual((await self.receive(a))['type'], 'error')
        await self.send(a, type='put', baseRevision=0, requestId='bad', command=dict(REPLY, trigger='!ban'))
        self.assertEqual((await self.receive(a))['type'], 'error')
        await self.send(a, type='put', baseRevision=0, requestId='ok', command=REPLY)
        self.assertEqual((await self.receive(a))['revision'], 1)

    async def test_only_one_client_claims_identical_party_event(self):
        a, _ = await self.join()
        b, _ = await self.join()
        await asyncio.gather(self.send(a, type='claim', requestId='a', key='a'*64),
                             self.send(b, type='claim', requestId='b', key='a'*64))
        results = [(await self.receive(a))['granted'], (await self.receive(b))['granted']]
        self.assertEqual(sorted(results), [False, True])
        # Different messages can be answered immediately.
        await self.send(b, type='claim', requestId='new', key='b'*64)
        self.assertTrue((await self.receive(b))['granted'])

    def test_validation_bounds_and_rolls(self):
        for bad in [dict(REPLY, answers=['!hello']), dict(REPLY, answers=['{roll:4:1}']),
                    dict(REPLY, enabled='true'), dict(REPLY, answers=['x'*257]), dict(REPLY, answers=['a\nb'])]:
            with self.assertRaises(ValueError):
                validate_reply(bad)
        self.assertEqual(validate_reply(REPLY), REPLY)

if __name__ == '__main__':
    unittest.main()
