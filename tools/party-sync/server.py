"""Private-room reply sharing relay. No Minecraft connection or command execution."""
import argparse
import asyncio
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import time
from websockets.asyncio.server import serve
from websockets.exceptions import ConnectionClosed

MAX_COMMANDS = 64
MAX_MESSAGE = 2_097_152

def validate_reply(value):
    if not isinstance(value, dict):
        raise ValueError('Expected reply object')
    trigger = value.get('trigger')
    if not isinstance(trigger, str) or not re.fullmatch(r'![A-Za-z0-9_-]{1,24}', trigger):
        raise ValueError('Invalid trigger')
    trigger = trigger.lower()
    if trigger in ('!ban', '!help'):
        raise ValueError('Reserved command')
    answers = value.get('answers')
    if type(value.get('enabled')) is not bool or not isinstance(answers, list) or not 1 <= len(answers) <= 32:
        raise ValueError('Invalid reply')
    for answer in answers:
        if (not isinstance(answer, str) or not answer.strip() or len(answer.encode('utf-16-le')) // 2 > 256
                or any(ord(c) < 32 or 127 <= ord(c) <= 159 for c in answer) or answer.lstrip().startswith('!')):
            raise ValueError('Invalid answer')
        for low, high in re.findall(r'\{roll:(-?\d{1,6}):(-?\d{1,6})}', answer):
            if int(low) > int(high):
                raise ValueError('Invalid roll range')
    return dict(trigger=trigger, answers=answers, enabled=value['enabled'])

class Relay:
    def __init__(self, path):
        self.path = Path(path)
        self.rooms = {}
        self.clients = {}
        self.claims = {}
        self.lock = asyncio.Lock()
        if self.path.exists():
            saved = json.loads(self.path.read_text())
            for name, room in saved.items():
                replies = [validate_reply(v) for v in room['commands']]
                if len(replies) > MAX_COMMANDS:
                    raise ValueError('Stored room exceeds limit')
                self.rooms[name] = dict(secret=room['secret'], revision=room['revision'], commands={v['trigger']: v for v in replies})

    def persist(self):
        saved = {key: dict(secret=r['secret'], revision=r['revision'], commands=list(r['commands'].values()))
                 for key, r in self.rooms.items()}
        self.path.parent.mkdir(parents=True, exist_ok=True)
        temporary = self.path.with_suffix('.tmp')
        temporary.write_text(json.dumps(saved, ensure_ascii=False))
        os.chmod(temporary, 0o600)
        temporary.replace(self.path)

    @staticmethod
    def snapshot(room, request_id=None, kind='snapshot'):
        return dict(type=kind, revision=room['revision'], commands=list(room['commands'].values()), requestId=request_id)

    async def send(self, client, message):
        await client.send(json.dumps(message, ensure_ascii=False))

    async def handler(self, client):
        room_key = None
        try:
            hello = json.loads(await asyncio.wait_for(client.recv(), 10))
            room_name, token = hello.get('room'), hello.get('token')
            if (hello.get('type') != 'join' or hello.get('protocol') != 1
                    or not isinstance(room_name, str) or not re.fullmatch(r'[A-Za-z0-9_-]{1,48}', room_name)
                    or not isinstance(token, str) or not 12 <= len(token) <= 128):
                await client.close(1008, 'Invalid room or token (12-128 characters required)')
                return
            room_key = room_name
            digest = hashlib.sha256(token.encode()).hexdigest()
            async with self.lock:
                if room_key not in self.rooms:
                    if len(self.rooms) >= 128:
                        await client.close(1008, 'Server room limit reached')
                        return
                    self.rooms[room_key] = dict(secret=digest, revision=0, commands={})
                    self.persist()
                room = self.rooms[room_key]
                if not secrets.compare_digest(room['secret'], digest):
                    room_key = None
                    await client.close(1008, 'Incorrect room token')
                    return
                members = self.clients.setdefault(room_key, set())
                if len(members) >= 32:
                    await client.close(1008, 'Room connection limit reached')
                    return
                members.add(client)
                await self.send(client, self.snapshot(room))
            # Bound per-connection traffic; normal use is far below 20 operations/second.
            window, count = time.monotonic(), 0
            async for raw in client:
                now = time.monotonic()
                if now-window >= 1:
                    window, count = now, 0
                count += 1
                if count > 20:
                    await client.close(1008, 'Too many messages')
                    return
                message = {}
                try:
                    message = json.loads(raw)
                    if not isinstance(message, dict):
                        raise ValueError('Expected object')
                    request_id = message.get('requestId')
                    if not isinstance(request_id, str) or len(request_id) > 64:
                        raise ValueError('Missing request ID')
                    async with self.lock:
                        if message.get('type') == 'claim':
                            key = message.get('key')
                            if not isinstance(key, str) or not re.fullmatch('[0-9a-f]{64}', key):
                                raise ValueError('Invalid event hash')
                            claims = self.claims.setdefault(room_key, {})
                            claims = self.claims[room_key] = {k: expiry for k, expiry in claims.items() if expiry > now}
                            granted = key not in claims
                            if granted:
                                claims[key] = now + 2
                            await self.send(client, dict(type='claim', requestId=request_id, granted=granted))
                            continue
                        if message.get('type') not in ('put', 'delete'):
                            raise ValueError('Unknown operation')
                        if type(message.get('baseRevision')) is not int or message['baseRevision'] != room['revision']:
                            await self.send(client, self.snapshot(room, request_id, 'conflict'))
                            continue
                        commands = dict(room['commands'])
                        if message['type'] == 'put':
                            reply = validate_reply(message.get('command'))
                            commands[reply['trigger']] = reply
                            if len(commands) > MAX_COMMANDS:
                                raise ValueError('Room is full')
                        else:
                            trigger = message.get('trigger')
                            if not isinstance(trigger, str):
                                raise ValueError('Missing trigger')
                            commands.pop(trigger, None)
                        # Persist successfully before acknowledging or broadcasting changes.
                        previous = room.copy()
                        room.update(commands=commands, revision=room['revision']+1)
                        try:
                            self.persist()
                        except OSError:
                            room.update(previous)
                            raise ValueError('Could not save room')
                        snapshot = self.snapshot(room, request_id)
                        for member in tuple(self.clients.get(room_key, ())):
                            try:
                                await asyncio.wait_for(self.send(member, snapshot), 3)
                            except (ConnectionClosed, TimeoutError):
                                await member.close()
                except (ValueError, TypeError, KeyError) as error:
                    await self.send(client, dict(type='error', requestId=message.get('requestId') if isinstance(message, dict) else None,
                                                 message=str(error)))
        except (ConnectionClosed, TimeoutError, ValueError, TypeError, AttributeError):
            await client.close(1008, 'Invalid handshake')
        finally:
            if room_key is not None:
                self.clients.get(room_key, set()).discard(client)

async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--data', default=str(Path(__file__).with_name('rooms.json')))
    args = parser.parse_args()
    relay = Relay(args.data)
    async with serve(relay.handler, args.host, args.port, max_size=MAX_MESSAGE, compression=None):
        print(f'Party reply relay listening on {args.host}:{args.port}', flush=True)
        await asyncio.Future()

if __name__ == '__main__':
    asyncio.run(main())
