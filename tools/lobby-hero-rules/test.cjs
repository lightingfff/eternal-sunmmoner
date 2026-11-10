const { createRequire } = require('node:module');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const req = createRequire(process.env.LOBBY_RULES_PACKAGE || path.join(__dirname, 'package.json'));
const { initializeTestEnvironment, assertSucceeds, assertFails } = req('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, collection, query, where, getDocs, runTransaction, Timestamp } = req('firebase/firestore');
const projectId = 'demo-eternal-lobby';
const at = Timestamp.fromMillis(Date.now() - 2000);
const trait = {id:'unyielding_soul',name:'Linh hồn bất khuất',minimumLevel:10,minimumMorale:60,requiresSelfAwareness:false,unlocked:false};
const profile = {userId:'owner',masterName:'Master',masterLevel:1,masterExp:0,gold:600,manaCrystals:150,soulShards:40,rations:12,ironOre:8,arcaneEssence:6,highestClearedFloor:0,totalExpeditions:0,unlockedFacilities:[],storyFlags:[],createdAt:at,updatedAt:at};
const hero = {userId:'owner',name:'Kaelen',title:'Guard',heroClass:'KNIGHT',rarity:3,level:1,exp:0,maxHp:350,attack:35,defense:20,speed:100,skillLevel:1,personality:'LOYAL',status:'READY',injuryRecoveryUntilEpochSec:0,morale:100,equippedWeaponId:'',equippedArmorId:'',inActiveSquad:false,bondSummary:'',createdAt:at,updatedAt:at};
(async () => {
  const env = await initializeTestEnvironment({projectId,firestore:{host:'127.0.0.1',port:8085,rules:readFileSync(path.resolve(__dirname,'../../firestore.rules'),'utf8')}});
  const db = env.authenticatedContext('owner').firestore();
  let count = 0;
  const check = async (name, body) => { await body(); count++; process.stdout.write(`PASS ${name}\n`); };
  try {
    await env.withSecurityRulesDisabled(async c => {
      const seed=c.firestore(); await setDoc(doc(seed,'users/owner'),profile);
      for(let i=1;i<=5;i++) await setDoc(doc(seed,`users/owner/heroes/h${i}`),{...hero,inActiveSquad:i<5});
    });
    await check('legacy own hero and constrained roster query readable', async()=> {
      await assertSucceeds(getDoc(doc(db,'users/owner/heroes/h1')));
      const result=await assertSucceeds(getDocs(query(collection(db,'users/owner/heroes'),where('userId','==','owner'))));
      if(result.size!==5) throw Error('missing roster');
    });
    await check('full legacy trait migration and new lobby fields update',()=>assertSucceeds(updateDoc(doc(db,'users/owner/heroes/h1'),{currentHp:80,sanityState:'SELF_AWARE',currentAssignment:'IN_PARTY',partyJoinedOrder:1,restStartedAtEpochSec:0,hiddenTrait:trait})));
    await check('same transaction replaces party member and updates profile revision',()=>assertSucceeds(runTransaction(db,async tx=>{
      const p=await tx.get(doc(db,'users/owner'));
      const old=await tx.get(doc(db,'users/owner/heroes/h1'));
      const next=await tx.get(doc(db,'users/owner/heroes/h5'));
      tx.update(old.ref,{inActiveSquad:false,currentAssignment:'UNASSIGNED',partyJoinedOrder:0});
      tx.update(next.ref,{inActiveSquad:true,currentAssignment:'IN_PARTY',partyJoinedOrder:5,currentHp:350,hiddenTrait:trait});
      tx.update(p.ref,{lobbyRosterRevision:1});
    })));
    await check('house rest fields allowed',()=>assertSucceeds(updateDoc(doc(db,'users/owner/heroes/h5'),{inActiveSquad:false,status:'RESTING',currentAssignment:'RESTING',restStartedAtEpochSec:1000})));
    await check('new personality at owned hero creation allowed',()=>assertSucceeds(setDoc(doc(db,'users/owner/heroes/new'),{...hero,personality:'COWARD',currentHp:-1,hiddenTrait:trait})));
    await check('negative HP rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h1'),{currentHp:-2})));
    await check('HP beyond max rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h1'),{currentHp:351})));
    await check('invalid sanity rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h1'),{sanityState:'BAD'})));
    await check('BeingPickedUp cannot become persisted assignment',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h1'),{currentAssignment:'BEING_PICKED_UP'})));
    await check('invalid trait condition rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h1'),{hiddenTrait:{...trait,minimumMorale:101}})));
    await check('partial hidden trait map rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h1'),{hiddenTrait:{unlocked:true}})));
    await check('invalid revision rejected',()=>assertFails(updateDoc(doc(db,'users/owner'),{lobbyRosterRevision:-1})));
    await check('cross-account write denied',()=>assertFails(updateDoc(doc(env.authenticatedContext('intruder').firestore(),'users/owner/heroes/h1'),{currentHp:60})));
    await check('anonymous read denied',()=>assertFails(getDoc(doc(env.unauthenticatedContext().firestore(),'users/owner/heroes/h1'))));
    console.log(JSON.stringify({tests:count,result:'PASS',project:projectId,emulator:true}));
  } finally { await env.cleanup(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
