const {test} = require('node:test');
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
const source = fs.readFileSync(__dirname + '/content.js', 'utf8');
function fixture({path='/reels/',videos=[],buttons=[]}={}) {
 let listener;
 const context={browser:{runtime:{id:'test',onMessage:{addListener:fn=>listener=fn}}},
 document:{querySelectorAll:selector=>selector==='video'?videos:buttons},location:{pathname:path},
 innerWidth:1000,innerHeight:1000,getComputedStyle:()=>({overflowY:'auto'})};
 vm.runInNewContext(source,context);
 return command=>new Promise(resolve=>listener({command},{id:'test'},resolve));
}
const rect=()=>({left:0,top:0,right:500,bottom:500});
test('toggles the visible video, not offscreen videos',async()=>{
 let played=0,paused=0;
 const video={getBoundingClientRect:rect,paused:true,play:async()=>played++,pause:()=>paused++};
 const run=fixture({videos:[{getBoundingClientRect:()=>({left:0,top:2000,right:500,bottom:2500})},video]});
 assert.equal((await run('TOGGLE_PLAY')).ok,true);assert.equal(played,1);
 video.paused=false;await run('TOGGLE_PLAY');assert.equal(paused,1);
});
test('next and previous use matching navigation buttons',async()=>{
 const clicks=[];
 const buttons=['Next','Previous'].map(label=>({getBoundingClientRect:rect,getAttribute:()=>label,
 querySelectorAll:()=>[],click:()=>clicks.push(label)}));
 const run=fixture({videos:[{getBoundingClientRect:rect}],buttons});
 await run('NEXT');await run('PREVIOUS');assert.deepEqual(clicks,['Next','Previous']);
});
test('localized navigation falls back to scroll container',async()=>{
 const moves=[];const parent={scrollHeight:2000,clientHeight:700,scrollBy:args=>moves.push(args.top)};
 const run=fixture({videos:[{getBoundingClientRect:rect,parentElement:parent}]});
 await run('NEXT');await run('PREVIOUS');assert.deepEqual(moves,[700,-700]);
});
test('reports unavailable controls and ignores non-Reels pages',async()=>{
 assert.equal((await fixture()('NEXT')).ok,false);
 assert.equal((await fixture({path:'/accounts/login/'})('TOGGLE_PLAY')).ok,false);
 assert.equal((await fixture({videos:[{getBoundingClientRect:rect}]})('NEXT')).ok,false);
});
