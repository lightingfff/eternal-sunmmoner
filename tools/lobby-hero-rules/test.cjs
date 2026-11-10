const { createRequire } = require('node:module');
const { readFileSync } = require('node:fs');
const path = require('node:path');
const req = createRequire(process.env.LOBBY_RULES_PACKAGE || path.join(__dirname, 'package.json'));
const { initializeTestEnvironment, assertSucceeds, assertFails } = req('@firebase/rules-unit-testing');
const { doc, setDoc, updateDoc, getDoc, collection, query, where, getDocs, runTransaction, writeBatch, increment, Timestamp } = req('firebase/firestore');
const projectId = 'demo-eternal-lobby';
const eventId = n => `00000000-0000-0000-0000-${String(n).padStart(12,'0')}`;
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
    await check('dedicated survival and social metadata accepted',()=>assertSucceeds(updateDoc(doc(db,'users/owner/heroes/h2'),{
      survivedExpeditions:3,returnedUnder15PctHp:true,recentDisdainEventIds:[eventId(1)],hiddenTrait:{...trait,unlocked:true}
    })));
    await check('negative survival count rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{survivedExpeditions:-1})));
    await check('fractional survival count rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{survivedExpeditions:1.5})));
    await check('nonboolean critical return rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{returnedUnder15PctHp:'true'})));
    await check('unbounded social dedupe history rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:Array(33).fill(eventId(1))})));
    await check('malformed social history tail rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1),{bad:"map"}]})));
    await check('numeric social history tail rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1),7]})));
    await check('empty social history tail rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1),'']})));
    await check('oversized social history tail rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1),'x'.repeat(81)]})));
    await check('nested social array rejected by Firestore SDK',async()=>{
      let rejected=false;
      try {await updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1),[eventId(2)]]});}
      catch(error){rejected=error.code==='invalid-argument';}
      if(!rejected) throw Error('nested arrays must not be serializable');
    });
    await check('boolean social history tail rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1),true]})));
    await check('non UUID event ID rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1),'invalid-event-id']})));
    await check('multiple UUIDs hidden in one array element rejected',()=>assertFails(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:[eventId(1)+'|'+eventId(2)]})));
    await check('complete 32 entry social history accepted',()=>assertSucceeds(updateDoc(doc(db,'users/owner/heroes/h2'),{recentDisdainEventIds:Array.from({length:32},(_,i)=>eventId(i))})));
    await check('cloud treatment payload serializes Gold and current HP', async()=> {
      await env.withSecurityRulesDisabled(async c=> {
        const seed=c.firestore(); await updateDoc(doc(seed,'users/owner'),{gold:80});
        await setDoc(doc(seed,'users/owner/heroes/patient'),{...hero,rarity:1,currentHp:10});
      });
      const heal=()=>runTransaction(db,async tx=> {
        const p=await tx.get(doc(db,'users/owner')); const h=await tx.get(doc(db,'users/owner/heroes/patient'));
        if(p.data().gold<80) throw Error('insufficient-gold');
        tx.update(p.ref,{gold:p.data().gold-80,lobbyRosterRevision:(p.data().lobbyRosterRevision||0)+1});
        tx.update(h.ref,{currentHp:Math.min(h.data().maxHp,h.data().currentHp+175),status:'READY',injuryRecoveryUntilEpochSec:0,
          currentAssignment:'UNASSIGNED',restStartedAtEpochSec:0,survivedExpeditions:0,returnedUnder15PctHp:false,hiddenTrait:trait});
      });
      const result=await Promise.allSettled([heal(),heal()]);
      if(result.filter(x=>x.status==='fulfilled').length!==1) throw Error('concurrent treatment double spent');
      if((await getDoc(doc(db,'users/owner'))).data().gold!==0) throw Error('Gold lost');
      if((await getDoc(doc(db,'users/owner/heroes/patient'))).data().currentHp!==185) throw Error('HP stale');
    });
    await check('synthesis transaction deletes material unbinds gear and cannot consume twice', async()=> {
      await setDoc(doc(db,'users/owner/heroes/fusion_target'),{...hero,rarity:1,currentHp:20});
      await setDoc(doc(db,'users/owner/heroes/fusion_mat'),{...hero,rarity:1});
      await setDoc(doc(db,'users/owner/equipment/fusion_gear'),{userId:'owner',name:'Sword',slot:'WEAPON',rarity:1,level:1,
        bonusHp:0,bonusAttack:10,bonusDefense:0,bonusSpeed:0,iconType:'LONGSWORD',equippedByHeroId:'fusion_mat',createdAt:at,updatedAt:at});
      await assertFails(getDocs(query(collection(db,'users/owner/equipment'),where('equippedByHeroId','in',['fusion_mat']))));
      const materialGear=await assertSucceeds(getDocs(query(collection(db,'users/owner/equipment'),
        where('userId','==','owner'),where('equippedByHeroId','in',['fusion_mat']))));
      if(materialGear.size!==1) throw Error('material equipment query missed equipped gear');
      const fuse=()=>runTransaction(db,async tx=> {
        const p=await tx.get(doc(db,'users/owner')); const target=await tx.get(doc(db,'users/owner/heroes/fusion_target'));
        const mat=await tx.get(doc(db,'users/owner/heroes/fusion_mat')); const gear=await tx.get(doc(db,'users/owner/equipment/fusion_gear'));
        if(!mat.exists()) throw Error('material-already-consumed');
        tx.update(target.ref,{attack:42,defense:24,maxHp:420,currentHp:20,exp:0,level:2,
          hiddenTrait:trait,morale:90,sanityState:'NORMAL',survivedExpeditions:0,returnedUnder15PctHp:false,recentDisdainEventIds:[]});
        tx.delete(mat.ref); tx.update(gear.ref,{equippedByHeroId:''});
        tx.update(p.ref,{lobbyRosterRevision:(p.data().lobbyRosterRevision||0)+1});
      });
      await assertSucceeds(fuse());
      let rejected=false;try {await fuse();} catch(e){rejected=true;}
      if(!rejected) throw Error('material consumed twice');
      if((await getDocs(query(collection(db,'users/owner/heroes'),where('userId','==','owner')))).docs.some(d=>d.id==='fusion_mat')) throw Error('permadeath missing');
      if((await getDoc(doc(db,'users/owner/equipment/fusion_gear'))).data().equippedByHeroId!=='') throw Error('dangling equipped hero');
      const target=(await getDoc(doc(db,'users/owner/heroes/fusion_target'))).data();
      if(target.attack!==42||target.currentHp!==20) throw Error('duplicate transfer or free heal');
    });
    await check('equipment membership revision rejects stale fusion query before any deletion', async()=> {
      const pRef=doc(db,'users/owner');
      await setDoc(doc(db,'users/owner/heroes/race_material'),{...hero,equippedWeaponId:'race_a'});
      const gearBase={userId:'owner',name:'Sword',slot:'WEAPON',rarity:1,level:1,bonusHp:0,bonusAttack:10,
        bonusDefense:0,bonusSpeed:0,iconType:'LONGSWORD',createdAt:at,updatedAt:at};
      const aRef=doc(db,'users/owner/equipment/race_a'),bRef=doc(db,'users/owner/equipment/race_b');
      await setDoc(aRef,{...gearBase,equippedByHeroId:'race_material'});await setDoc(bRef,{...gearBase,equippedByHeroId:''});
      const before=await getDoc(pRef);
      const oldGear=await getDocs(query(collection(db,'users/owner/equipment'),where('userId','==','owner'),where('equippedByHeroId','in',['race_material'])));
      if(oldGear.docs.map(d=>d.id).join()!=='race_a') throw Error('bad race fixture');
      // The actual equip/unequip source now updates this same revision in its atomic batch.
      const equip=writeBatch(db);equip.update(aRef,{equippedByHeroId:'fusion_target'});equip.update(bRef,{equippedByHeroId:'race_material'});
      equip.update(doc(db,'users/owner/heroes/race_material'),{equippedWeaponId:'race_b'});
      equip.update(doc(db,'users/owner/heroes/fusion_target'),{equippedWeaponId:'race_a'});
      equip.update(pRef,{lobbyRosterRevision:increment(1)});await equip.commit();
      let staleRejected=false;
      try {await runTransaction(db,async tx=>{
        const current=await tx.get(pRef);
        if(current.data().lobbyRosterRevision!==before.data().lobbyRosterRevision) throw Error('membership-changed');
        throw Error('stale query accepted');
      });} catch(e){staleRejected=e.message.includes('membership-changed');}
      if(!staleRejected) throw Error('fusion did not reject equipment membership drift');
      if(!(await getDoc(doc(db,'users/owner/heroes/race_material'))).exists()) throw Error('stale read deleted material');
      const freshGear=await getDocs(query(collection(db,'users/owner/equipment'),where('userId','==','owner'),where('equippedByHeroId','in',['race_material'])));
      if(freshGear.docs.map(d=>d.id).join()!=='race_b') throw Error('retry missed newly equipped gear');
      if((await getDoc(aRef)).data().equippedByHeroId!=='fusion_target') throw Error('survivor gear unexpectedly unbound');
    });
    await check('cross-owner material removal rejects whole transaction', async()=> {
      await env.withSecurityRulesDisabled(c=>setDoc(doc(c.firestore(),'users/intruder/heroes/foreign'),{...hero,userId:'intruder'}));
      const targetRef=doc(db,'users/owner/heroes/fusion_target');
      await assertFails(runTransaction(db,async tx=>{
        await tx.get(targetRef); tx.update(targetRef,{attack:999}); tx.delete(doc(db,'users/intruder/heroes/foreign'));
      }));
      if((await getDoc(targetRef)).data().attack!==42) throw Error('partial unauthorized fusion');
    });
    await check('durable social event payload applies once after retry', async()=> {
      const ref=doc(db,'users/owner/heroes/patient');
      const scorn=id=>runTransaction(db,async tx=> {
        const h=await tx.get(ref); const ids=h.data().recentDisdainEventIds||[];
        if(ids.includes(id)) return;
        tx.update(ref,{morale:h.data().morale-2,sanityState:'NORMAL',recentDisdainEventIds:[...ids,id]});
      });
      await scorn(eventId(100));await scorn(eventId(100));await scorn(eventId(101));
      if((await getDoc(ref)).data().morale!==96) throw Error('retry applied morale more than once');
    });
    console.log(JSON.stringify({tests:count,result:'PASS',project:projectId,emulator:true}));
  } finally { await env.cleanup(); }
})().catch(error=>{console.error(error);process.exitCode=1;});
