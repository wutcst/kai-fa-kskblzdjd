<template>
  <div class="game-root" @contextmenu.prevent>
    <div class="loading" v-if="!connected">
      <div class="load-logo">W·O·R·L·D</div>
      <div class="load-logo2">O·F</div>
      <div class="load-logo3">Z·U·U·L</div>
      <div class="load-bar-wrap"><div class="load-bar"></div></div>
      <div class="load-text">{{ disconnected ? '重连中...' : '加载中...' }}</div>
    </div>

    <canvas ref="canvas" class="game-canvas" :style="{display:connected?'block':'none'}"></canvas>

    <div class="hud-layer" v-if="connected">
      <div class="rank-panel">
        <div class="rank-title">⚔ 击杀排行</div>
        <div class="rank-list">
          <div v-for="(r,i) in rankings" :key="r.userId" class="rank-row" :class="{me:r.userId===player.userId}">
            <span class="rank-idx" :class="{top1:i===0,top2:i===1,top3:i===2}">{{ i+1 }}</span>
            <span class="rank-name">{{ r.playerName }}</span>
            <span class="rank-kills">{{ r.kills }}杀</span>
          </div>
          <div v-if="!rankings||rankings.length===0" class="rank-empty">暂无击杀</div>
        </div>
      </div>
      <div class="hud-tl">
        <div class="player-name-tag">{{ player.playerName || 'Player' }}</div>
        <div class="hp-bar-bg">
          <div class="hp-bar-fill" :style="{width:hpPct+'%'}"></div>
          <span class="hp-bar-text">{{ player.currentHealth }} / {{ player.maxHealth }}</span>
        </div>
        <div class="stat-row">
          <div class="stat-badge atk"><span class="stat-icon">⚔</span> ATK {{ player.attack }}</div>
          <div class="stat-badge def"><span class="stat-icon">🛡</span> DEF {{ player.defense }}</div>
        </div>
      </div>

      <div class="hud-tr">
        <div class="round-timer" :class="{warn:roundTime<60}">下一轮 {{ roundDisplay }}</div>
        <div class="minimap" v-if="room.tiles">
          <div v-for="(row,y) in room.tiles" :key="y" class="mm-row">
            <div v-for="(tile,x) in row" :key="x" class="mm-cell" :class="{
              wall:tile===1, floor:tile!=1, door:tile>=2&&tile<=5,
              player:tile!=1&&x===player.posX&&y===player.posY,
              otherPlayer:hasOtherPlayerAt(x,y),
              itemDot:hasItemAt(x,y)
            }"></div>
          </div>
        </div>
        <div class="room-label">{{ room.roomName ? room.roomName.toUpperCase() : '' }}</div>
        <div class="player-count">{{ room.players ? room.players.length : 0 }} 人</div>
      </div>

      <transition name="room-fade">
        <div class="room-banner" v-if="room.roomName">{{ room.roomName }}</div>
      </transition>

      <div class="hud-br">
        <div class="eq-slot" @click="unequip('w')" :class="{filled:player.equippedWeapon}">
          <span class="eq-icon">{{ player.equippedWeapon ? '⚔' : '▢' }}</span>
          <span class="eq-label">武器</span>
          <span class="eq-name" v-if="player.equippedWeapon">{{ player.equippedWeapon.displayName || player.equippedWeapon.name }}</span>
          <span class="eq-name dim" v-else>空</span>
        </div>
        <div class="eq-slot" @click="unequip('a')" :class="{filled:player.equippedArmor}">
          <span class="eq-icon">{{ player.equippedArmor ? '🛡' : '▢' }}</span>
          <span class="eq-label">防具</span>
          <span class="eq-name" v-if="player.equippedArmor">{{ player.equippedArmor.displayName || player.equippedArmor.name }}</span>
          <span class="eq-name dim" v-else>空</span>
        </div>
        <div class="eq-slot bag-btn gm-btn" @click="openGm()">
          <span class="eq-icon">⚙</span>
          <span class="eq-label">GM</span>
          <span class="eq-name" v-if="gmAuthed">ON</span>
          <span class="eq-name dim" v-else>OFF</span>
        </div>
        <div class="eq-slot bag-btn" @click="showBag=!showBag">
          <span class="eq-icon">🎒</span>
          <span class="eq-label">背包</span>
          <span class="eq-name">{{ player.bag ? player.bag.length : 0 }} 件</span>
        </div>
      </div>

      <div class="hud-bc">
        <span class="key-hint">WASD</span> 移动 ·
        <span class="key-hint">J</span> 攻击 ·
        <span class="key-hint">空格</span> 拾取 ·
        <span class="key-hint">I</span> 背包 ·
        <span class="key-hint">H</span> 帮助 ·
        <span class="key-hint">F5</span> 存档
      </div>

      <div class="msg-log">
        <div v-for="(m,i) in msgLog" :key="i" class="msg-line" :class="m.cls">{{ m.txt }}</div>
      </div>
    </div>

    <div class="inv-bg" v-if="showBag" @click.self="showBag=false">
      <div class="inv-box">
        <div class="inv-top">
          <span class="inv-title">物品栏</span>
          <span class="inv-close" @click="showBag=false">✕</span>
        </div>
        <div class="inv-weight">负重: {{ player.currentLoad || 0 }} / {{ player.maxCapacity || 50 }}</div>
        <div class="inv-list">
          <div v-if="!player.bag||player.bag.length===0" class="inv-empty">空空如也</div>
          <div v-for="item in player.bag" :key="item.name" class="inv-row" :class="{isWpn:item.range>0,isArm:item.weight>=1&&!item.range}">
            <span class="inv-icon">{{ item.range>0?'⚔':item.weight>=1?'🛡':'♨' }}</span>
            <div class="inv-mid">
              <div class="inv-name">{{ item.displayName || item.name }}</div>
              <div class="inv-desc" v-html="itemDesc(item)"></div>
            </div>
            <div class="inv-btns">
              <button v-if="item.range>0" class="sk-btn green" @click="equip(item)">装备</button>
              <button v-else-if="!item.consumable" class="sk-btn blue" @click="equip(item)">穿戴</button>
              <button v-else class="sk-btn cyan" @click="useItem(item)">使用</button>
              <button class="sk-btn red" @click="dropItem(item)">丢弃</button>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- Help Panel -->
    <div class="inv-bg" v-if="showHelp" @click.self="showHelp=false">
      <div class="inv-box" style="min-width:600px;max-width:700px;max-height:80vh;">
        <div class="inv-top">
          <span class="inv-title">游戏帮助</span>
          <span class="inv-close" @click="showHelp=false">✕</span>
        </div>
        <div style="display:flex;gap:12px;overflow:hidden;flex:1;">
          <div style="overflow-y:auto;flex:1;">
            <div style="color:#c0a060;font-size:13px;margin-bottom:8px;font-weight:700;">操作说明</div>
            <div v-for="row in HELP_CONTROLS" :key="row.k" style="display:flex;gap:10px;padding:2px 0;font-size:12px;">
              <span style="color:#ff8c00;min-width:70px;text-align:right;font-weight:700;">{{ row.k }}</span>
              <span style="color:#ccc;">{{ row.v }}</span>
            </div>
            <div style="color:#c0a060;font-size:13px;margin:14px 0 8px;font-weight:700;">战斗机制</div>
            <div style="font-size:11px;color:#aaa;line-height:1.6;">
              <div>· <b style="color:#ef5350">反击伤害</b>：攻击敌人时，目标会反击 (目标攻击×25% - 你的防御)。AOE武器命中多个敌人时叠加。</div>
              <div>· <b style="color:#64b5f6">荆棘铠甲</b>：被攻击时反弹30%伤害给攻击者。</div>
              <div>· <b style="color:#4caf50">击杀奖励</b>：击败玩家 +2攻击 +10最大生命。</div>
              <div>· <b>装备机制</b>：拾取物品放入背包 -> 按 I 打开背包 -> 装备/穿戴才生效。空手攻击只有10基础伤害。</div>
            </div>
          </div>
          <div style="overflow-y:auto;flex:1;border-left:1px solid #222;padding-left:12px;">
            <div style="color:#c0a060;font-size:13px;margin-bottom:8px;font-weight:700;">装备图鉴 (17件)</div>
            <div v-for="it in HELP_ITEMS" :key="it.n" style="padding:4px 0;border-bottom:1px solid rgba(255,255,255,.03);">
              <div style="display:flex;align-items:center;gap:6px;">
                <span style="font-size:16px;">{{ it.n.startsWith('Wpn')?'⚔':it.n.startsWith('Arm')?'🛡':'♨' }}</span>
                <span :style="{color:RCOLOR[it.r]||'#aaa',fontSize:'12px',fontWeight:'700'}">{{ it.name }}</span>
                <span style="font-size:10px;color:#888;">{{ it.tag }}</span>
              </div>
              <div style="color:#888;font-size:11px;padding-left:22px;">{{ it.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <!-- GM Panel -->
    <div class="inv-bg" v-if="showGm" @click.self="showGm=false">
      <div class="inv-box" style="min-width:300px;max-width:360px;">
        <div class="inv-top">
          <span class="inv-title">⚙ Game Master</span>
          <span class="inv-close" @click="showGm=false">✕</span>
        </div>
        <div v-if="!gmAuthed" style="padding:12px 0;">
          <el-input v-model="gmInput" placeholder="输入GM密钥" show-password style="margin-bottom:10px;" @keyup.enter="doGmAuth" />
          <el-button type="warning" size="small" style="width:100%;" @click="doGmAuth">验证</el-button>
          <div v-if="gmError" style="color:#f66;font-size:12px;margin-top:8px;text-align:center;">{{ gmError }}</div>
        </div>
        <div v-else>
          <div style="color:#c0a060;font-size:13px;margin-bottom:10px;">生成物品</div>
          <select v-model="gmItem" style="width:100%;padding:6px;background:#111;color:#ccc;border:1px solid #333;border-radius:4px;margin-bottom:8px;">
            <option v-for="it in HELP_ITEMS" :key="it.n" :value="it.in">{{ it.name }}</option>
          </select>
          <div style="display:flex;gap:6px;margin-bottom:12px;">
            <button class="sk-btn green" style="flex:1;" @click="gmSpawn('self')">给我</button>
            <button class="sk-btn cyan" style="flex:1;" @click="gmSpawn('room')">放房间</button>
          </div>
          <div style="color:#c0a060;font-size:13px;margin-bottom:8px;">快捷操作</div>
          <button class="sk-btn green" style="width:100%;" @click="gmHeal()">满血</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'

const T=48,W=15,H=10
const WS=(location.protocol==='https:'?'wss://':'ws://')+location.host+'/game/websocket'

const canvas=ref<HTMLCanvasElement|null>(null)
const player=reactive<any>({userId:0,playerName:'',attack:10,defense:5,currentHealth:100,maxHealth:100,maxCapacity:50,currentLoad:0,bag:[],posX:1,posY:1,currentRoomName:'',equippedWeapon:null,equippedArmor:null})
const room=reactive<any>({roomName:'',tiles:null,items:[],players:[],portal:false})
const msgLog=ref<{txt:string,cls:string,t:number}[]>([])
const disconnected=ref(false)
const connected=ref(false)
const showBag=ref(false)
const showHelp=ref(false)
const showGm=ref(false)
const gmAuthed=ref(false)
const gmInput=ref('')
const gmError=ref('')
const gmItem=ref('')
const rankings=ref<any[]>([])
const hpPct=computed(()=>Math.max(0,(player.currentHealth/(player.maxHealth||100))*100))
const roundTime=ref(600)
const serverRoundEnd=ref(0)
const tick=ref(0)
const roundDisplay=computed(()=>{
  void tick.value
  if (serverRoundEnd.value > 0) {
    roundTime.value = Math.max(0, Math.ceil((serverRoundEnd.value - Date.now()) / 1000))
  }
  const m=Math.floor(roundTime.value/60),s=roundTime.value%60;return `${m}:${s.toString().padStart(2,'0')}`
})

const RARITY:Record<string,number>={Sword:1,BloodVial:1,HealthPotion:1,MagicCookie:1,StonehideElixir:2,DragonscaleBulwark:2,SpeedBoots:2,StormCleaver:3,FrostBow:3,WarHammer:3,VampireFang:4,BloodDagger:4,ThornArmor:4,BerserkerTotem:5,ShadowbaneBallista:5,ImmortalCore:6,PhoenixFeather:6}
const RCOLOR:Record<number,string>={1:'#aaa',2:'#4fc3f7',3:'#a4e',4:'#ff8c00',5:'#f66',6:'#ff0'}

const HELP_CONTROLS=[
  {k:'W A S D',v:'移动 / 换方向'},
  {k:'J',v:'攻击当前面向目标'},
  {k:'空格',v:'拾取脚下物品'},
  {k:'I',v:'打开/关闭背包'},
  {k:'H',v:'打开/关闭帮助'},
  {k:'F5',v:'保存游戏'},
  {k:'F9',v:'读取存档'},
]

const HELP_CMDS=[
  {k:'go 方向',v:'移动到相邻房间'},
  {k:'back',v:'返回上一个房间'},
  {k:'look',v:'查看当前房间'},
  {k:'take 物品',v:'拾取物品'},
  {k:'drop 物品',v:'丢弃物品'},
  {k:'use 物品',v:'使用消耗品'},
  {k:'items',v:'查看背包'},
  {k:'attack 玩家',v:'攻击其他玩家'},
  {k:'save / load',v:'保存/读取存档'},
]

const HELP_ITEMS=[
  {n:'Wpn_Sword',in:'Sword',name:'铁剑',tag:'近战',r:1,desc:'+10攻击, 500ms冷却, 距离1'},
  {n:'Wpn_BloodDagger',in:'BloodDagger',name:'嗜血匕首',tag:'近战',r:4,desc:'击杀回复15HP, 400ms冷却'},
  {n:'Wpn_VampireFang',in:'VampireFang',name:'吸血獠牙',tag:'近战',r:4,desc:'+8攻击, 攻击回复33%伤害, 600ms冷却'},
  {n:'Wpn_WarHammer',in:'WarHammer',name:'战锤',tag:'范围',r:3,desc:'+20攻击, AOE范围伤害, 1200ms冷却'},
  {n:'Wpn_StormCleaver',in:'StormCleaver',name:'风暴斩刃',tag:'远程',r:3,desc:'+15攻击, 距离2, 1000ms冷却'},
  {n:'Wpn_FrostBow',in:'FrostBow',name:'寒冰弓',tag:'远程',r:3,desc:'+12攻击, 距离3, 1000ms冷却'},
  {n:'Wpn_ShadowbaneBallista',in:'ShadowbaneBallista',name:'暗影弩炮',tag:'远程',r:5,desc:'距离3, 每次攻击额外+5伤害, 1500ms冷却'},
  {n:'Arm_DragonscaleBulwark',in:'DragonscaleBulwark',name:'龙鳞壁垒',tag:'防具',r:2,desc:'+12防御'},
  {n:'Arm_ThornArmor',in:'ThornArmor',name:'荆棘铠甲',tag:'防具',r:4,desc:'+10防御, 反射30%所受伤害'},
  {n:'Arm_SpeedBoots',in:'SpeedBoots',name:'疾风靴',tag:'防具',r:2,desc:'+5攻击, +3防御'},
  {n:'Use_BloodVial',in:'BloodVial',name:'血瓶',tag:'消耗',r:1,desc:'恢复40HP'},
  {n:'Use_HealthPotion',in:'HealthPotion',name:'大血瓶',tag:'消耗',r:1,desc:'恢复50HP'},
  {n:'Use_MagicCookie',in:'MagicCookie',name:'魔法饼干',tag:'消耗',r:1,desc:'永久+20负重上限'},
  {n:'Use_StonehideElixir',in:'StonehideElixir',name:'石肤药剂',tag:'消耗',r:2,desc:'永久+15防御'},
  {n:'Pas_BerserkerTotem',in:'BerserkerTotem',name:'狂战士图腾',tag:'被动',r:5,desc:'受伤后+8攻击持续12秒'},
  {n:'Pas_ImmortalCore',in:'ImmortalCore',name:'不朽核心',tag:'被动',r:6,desc:'免疫一次死亡, 保留1HP'},
  {n:'Pas_PhoenixFeather',in:'PhoenixFeather',name:'凤凰羽毛',tag:'被动',r:6,desc:'死亡时复活, 恢复50%HP'},
]
function itemDesc(it:any){
  if (it.description) return it.description;
  const r=RARITY[it.name]||1
  const stars='★'.repeat(r)+'☆'.repeat(6-r)
  const tp=it.range>0?it.type+' rng '+it.range:it.consumable?'consumable':'armor'
  return `<span style="color:${RCOLOR[r]}">${stars}</span> ${tp} · ${it.weight}kg`
}

function pushMsg(txt:string,cls=''){
  const msg={txt,cls,t:Date.now()}
  msgLog.value.push(msg)
  if(msgLog.value.length>6) msgLog.value.shift()
  setTimeout(()=>{
    const idx=msgLog.value.indexOf(msg)
    if(idx>=0) msgLog.value.splice(idx,1)
  },5000)
}
function hasItemAt(x:number,y:number){
  if(!room.items) return false
  return room.items.some((it:any)=>it.x===x&&it.y===y)
}
function hasOtherPlayerAt(x:number,y:number){
  if(!room.players) return false
  return room.players.some((p:any)=>p.userId!==player.userId&&p.posX===x&&p.posY===y)
}

let ws:WebSocket|null=null,ctx:CanvasRenderingContext2D|null=null
let keys:Record<string,boolean>={},lastAtk=0,aid=0
let ldx=1,ldy=0,srvX=1,srvY=1,clX=1,clY=1,subX=0,subY=0
let hitFx:{x:number,y:number,t:number}[]=[]
let atkFx:{x:number,y:number,dx:number,dy:number,tp:string,rng:number,t:number}[]=[]
let prevHp:Record<number,number>={}
let dust:{x:number,y:number,s:number,a:number}[]=[]
let rmtP:{[uid:number]:{cx:number,cy:number,tx:number,ty:number}}={}
let hbTimer:ReturnType<typeof setInterval>|null=null
let reconTimer:ReturnType<typeof setTimeout>|null=null
let roundTimer:ReturnType<typeof setInterval>|null=null
let reconAttempts=0
const MAX_RECON=10
let dmgNums:{x:number,y:number,val:number,h:number,t:number}[]=[]
for(let i=0;i<30;i++)dust.push({x:Math.random()*W*T,y:Math.random()*H*T,s:.3+Math.random()*1.2,a:.02+Math.random()*.06})
const pcol:Record<number,string>={}
function gc(uid:number){if(!pcol[uid])pcol[uid]=['#4fc3f7','#f48fb1','#a5d6a7','#ffcc80','#ce93d8'][Object.keys(pcol).length%5];return pcol[uid]}

function render(){
  if(!ctx||!canvas.value){aid=requestAnimationFrame(render);return}
  const c=ctx,n=Date.now()
  c.clearRect(0,0,W*T,H*T)
  c.fillStyle='#050510';c.fillRect(0,0,W*T,H*T)
  if(!room.tiles||room.tiles.length===0){
    c.fillStyle='#a09070';c.font='20px monospace'
    c.textAlign='center';c.fillText('加载中...',W*T/2,H*T/2)
    aid=requestAnimationFrame(render);return
  }
  for(const d of dust){
    d.y-=d.s
    if(d.y<-10){d.y=H*T+10;d.x=Math.random()*W*T}
    c.fillStyle=`rgba(180,160,120,${d.a})`;c.beginPath();c.arc(d.x,d.y,0.5,0,Math.PI*2);c.fill()
  }
  const tiles=room.tiles
  for(let y=0;y<tiles.length;y++){
    for(let x=0;x<tiles[y].length;x++){
      const t=tiles[y][x],px=x*T,py=y*T
      if(t===1){
        c.fillStyle='#1a1008';c.fillRect(px,py,T,T)
        c.fillStyle='#2a1d12';c.fillRect(px+2,py+2,T-4,T-4)
      }else{
        c.fillStyle=(x+y)%2?'#1a1a2e':'#1e1e3a';c.fillRect(px,py,T,T)
        c.strokeStyle='rgba(255,255,255,.03)';c.lineWidth=0.5;c.strokeRect(px+.5,py+.5,T-1,T-1)
      }
      if(t>=2&&t<=5){
        c.fillStyle='rgba(100,255,100,.15)';c.fillRect(px,py,T,T)
        c.fillStyle='rgba(150,255,150,.8)';c.font='26px monospace'
        c.textAlign='center';c.textBaseline='middle'
        const arrows=['','','▲','▼','◀','▶'];c.fillText(arrows[t]||'◆',px+T/2,py+T/2)
      }
    }
  }
  if(room.items){
    for(const it of room.items){
      if(it.x!=null&&it.y!=null&&!isNaN(it.x)&&!isNaN(it.y)){
        const px=it.x*T+T/2,py=it.y*T+T/2,bob=Math.sin(n/300+it.x+it.y)*3
        const isWpn=it.range>0,isArm=it.weight>=1&&!it.range
        const glow=isWpn?'rgba(255,140,0,.2)':isArm?'rgba(80,180,255,.2)':'rgba(255,215,0,.2)'
        c.fillStyle=glow;c.beginPath();c.arc(px,py+bob,14,0,Math.PI*2);c.fill()
        c.fillStyle=isWpn?'#ff8c00':isArm?'#4fc3f7':'#ffd700'
        c.beginPath();c.arc(px,py+bob,8,0,Math.PI*2);c.fill()
        c.fillStyle='#000';c.font='bold 10px monospace'
        c.textAlign='center';c.textBaseline='middle'
        c.fillText(String(it.displayName||it.name||'?').substring(0,2),px,py+bob)
      }
    }
  }
  if(room.players){
    for(const p of room.players){
      if(p.userId===player.userId) continue
      let rp=rmtP[p.userId]
      if(!rp){rp={cx:(p.posX||1),cy:(p.posY||1),tx:(p.posX||1),ty:(p.posY||1)};rmtP[p.userId]=rp}
      if(rp.tx!==(p.posX||1)||rp.ty!==(p.posY||1)){rp.tx=p.posX||1;rp.ty=p.posY||1}
      rp.cx+=(rp.tx-rp.cx)*0.25;rp.cy+=(rp.ty-rp.cy)*0.25
      const rpx=rp.cx*T+T/2,rpy=rp.cy*T+T/2
      c.fillStyle=gc(p.userId);c.beginPath();c.arc(rpx,rpy,14,0,Math.PI*2);c.fill()
      c.strokeStyle='rgba(255,255,255,.3)';c.lineWidth=1;c.beginPath();c.arc(rpx,rpy,14,0,Math.PI*2);c.stroke()
      c.fillStyle='#fff';c.font='bold 14px monospace';c.textAlign='center';c.textBaseline='middle'
      c.fillText('@',rpx,rpy)
      const hbw=24,hbh=4,hbx=rpx-hbw/2,hby=rpy-18
      c.fillStyle='rgba(0,0,0,.6)';c.fillRect(hbx-1,hby-1,hbw+2,hbh+2)
      c.fillStyle='#333';c.fillRect(hbx,hby,hbw,hbh)
      const hpR=(p.currentHealth||0)/(p.maxHealth||100)
      c.fillStyle=hpR>.5?'#4caf50':hpR>.25?'#ff9800':'#f44336'
      c.fillRect(hbx,hby,hbw*hpR,hbh)
    }
  }
  const ppx=clX*T+T/2+subX,ppy=clY*T+T/2+subY
  c.shadowColor='rgba(76,175,80,.4)';c.shadowBlur=10
  c.fillStyle='#4caf50';c.beginPath();c.arc(ppx,ppy,14,0,Math.PI*2);c.fill()
  c.strokeStyle='#81c784';c.lineWidth=2;c.beginPath();c.arc(ppx,ppy,14,0,Math.PI*2);c.stroke()
  c.shadowBlur=0
  c.fillStyle='#fff';c.font='bold 16px monospace';c.textAlign='center';c.textBaseline='middle';c.fillText('@',ppx,ppy)
  hitFx=hitFx.filter(h=>n-h.t<400)
  for(const h of hitFx){
    const a=1-(n-h.t)/400
    c.strokeStyle=`rgba(255,50,50,${a})`;c.lineWidth=2
    c.beginPath();c.arc(h.x*T+T/2,h.y*T+T/2,8+(n-h.t)/20,0,Math.PI*2);c.stroke()
  }
  atkFx=atkFx.filter(a=>n-a.t<500)
  for(const a of atkFx){
    const age=n-a.t,al=1-age/500,sx=a.x*T+T/2+a.dx*T,sy=a.y*T+T/2+a.dy*T
    if(a.tp==='melee'){
      c.strokeStyle=`rgba(255,180,40,${al})`;c.lineWidth=3
      c.beginPath();c.arc(sx,sy,10+age/15,0,Math.PI*2);c.stroke()
    }else if(a.tp==='ranged'){
      c.strokeStyle=`rgba(80,180,255,${al})`;c.lineWidth=2;c.beginPath()
      let cx=a.x*T+T/2,cy=a.y*T+T/2;c.moveTo(cx,cy)
      const steps=(a.rng||3)
      for(let i=0;i<steps;i++){cx+=a.dx*T;cy+=a.dy*T;c.lineTo(cx,cy)};c.stroke()
    }else if(a.tp==='aoe'){
      const aoeR=(a.rng||1)
      for(let ox=-(aoeR);ox<=aoeR;ox++)
        for(let oy=-(aoeR);oy<=aoeR;oy++){
          if(ox===0&&oy===0) continue
          c.strokeStyle=`rgba(255,120,30,${al*0.6})`;c.lineWidth=2
          c.strokeRect((a.x+ox)*T+2,(a.y+oy)*T+2,T-4,T-4)
        }
      c.strokeStyle=`rgba(255,120,30,${al})`;c.lineWidth=3
      c.beginPath();c.arc(sx,sy,8+age/12,0,Math.PI*2);c.stroke()
    }
  }
  dmgNums=dmgNums.filter(d=>n-d.t<1200)
  for(const d of dmgNums){
    const prog=(n-d.t)/1200,dy=d.h-prog*40
    c.fillStyle=`rgba(255,${d.val>10?'50,50':d.val>5?'180,40':'220,220'},${1-prog})`
    c.font=`bold ${12+prog*4}px monospace`;c.textAlign='center'
    c.fillText('-'+String(d.val),d.x*T+T/2,d.y*T+dy)
  }
  {
    let dx=0,dy=0
    if(keys['w']||keys['arrowup'])dy-=1
    if(keys['s']||keys['arrowdown'])dy+=1
    if(keys['a']||keys['arrowleft'])dx-=1
    if(keys['d']||keys['arrowright'])dx+=1
    if(dx||dy){ldx=dx>0?1:dx<0?-1:0;ldy=dy>0?1:dy<0?-1:0}
    if(dx||dy){
      const mag=(Math.abs(dx)+Math.abs(dy))>1?0.707:1,sp=1.8*mag
      const nx=clX*T+T/2+subX+dx*sp,ny=clY*T+T/2+subY+dy*sp
      const tx=Math.floor(nx/T),ty=Math.floor(ny/T)
      let hit=false
      if(room.tiles&&ty>=0&&ty<room.tiles.length&&tx>=0&&tx<room.tiles[ty].length&&room.tiles[ty][tx]===1) hit=true
      if(!hit&&dx&&dy&&room.tiles){
        const tx2=Math.floor((clX*T+T/2+subX+dx*sp)/T),ty2=Math.floor((clY*T+T/2+subY)/T)
        const tx3=Math.floor((clX*T+T/2+subX)/T),ty3=Math.floor((clY*T+T/2+subY+dy*sp)/T)
        if(ty2>=0&&ty2<room.tiles.length&&tx2>=0&&tx2<room.tiles[ty2].length&&room.tiles[ty2][tx2]===1) hit=true
        if(ty3>=0&&ty3<room.tiles.length&&tx3>=0&&tx3<room.tiles[ty3].length&&room.tiles[ty3][tx3]===1) hit=true
      }
      if(!hit){
        subX+=dx*sp;subY+=dy*sp
        const ntX=Math.floor((clX*T+T/2+subX)/T),ntY=Math.floor((clY*T+T/2+subY)/T)
        const otX=Math.floor((clX*T+T/2)/T),otY=Math.floor((clY*T+T/2)/T)
        if(ntX!==otX||ntY!==otY){
          clX=ntX;clY=ntY
          subX-=(ntX-otX)*T;subY-=(ntY-otY)*T
          const m={dx:ntX-otX,dy:ntY-otY}
          ws.send(JSON.stringify({action:'move',data:JSON.stringify(m),token:sessionStorage.getItem('token')}))
        }
      }
    }
  }
  if(keys['j']&&ws&&ws.readyState===WebSocket.OPEN&&n-lastAtk>1200){
    const at=player.equippedWeapon?.type||'melee'
    const rng=player.equippedWeapon?.range||1
    ws.send(JSON.stringify({action:'attack',data:JSON.stringify({dx:ldx,dy:ldy}),token:sessionStorage.getItem('token')}))
    lastAtk=n;keys['j']=false
  }
  aid=requestAnimationFrame(render)
}

function useItem(it:any){ws?.send(JSON.stringify({action:'use',data:it.name,token:sessionStorage.getItem('token')}))}
function dropItem(it:any){ws?.send(JSON.stringify({action:'drop',data:it.name,token:sessionStorage.getItem('token')}))}
function equip(it:any){ws?.send(JSON.stringify({action:'equip',data:it.name,token:sessionStorage.getItem('token')}))}
function unequip(s:string){ws?.send(JSON.stringify({action:s==='w'?'unequipW':'unequipA',data:null,token:sessionStorage.getItem('token')}))}

function openGm(){showGm.value=!showGm.value;showBag.value=false}
function doGmAuth(){
  gmError.value=''
  if(!gmInput.value){gmError.value='请输入密钥';return}
  ws?.send(JSON.stringify({action:'gm_auth',data:gmInput.value,token:sessionStorage.getItem('token')}))
}
function gmSpawn(t:string){
  ws?.send(JSON.stringify({action:'gm_spawn',data:JSON.stringify({itemName:gmItem.value,target:t}),token:sessionStorage.getItem('token')}))
}
function gmHeal(){
  ws?.send(JSON.stringify({action:'gm_heal',data:null,token:sessionStorage.getItem('token')}))
}

function connect(){
  const tk=sessionStorage.getItem('token')
  if(!tk) return
  disconnected.value=false
  try{ws=new WebSocket(WS+'?token='+encodeURIComponent(tk))}catch{disconnected.value=true;return}
  ws.onopen=()=>{
    connected.value=true
    reconAttempts=0
    if(hbTimer) clearInterval(hbTimer)
    hbTimer=setInterval(()=>{
      if(ws?.readyState===WebSocket.OPEN)
        ws.send(JSON.stringify({action:'heartbeat',data:null,token:sessionStorage.getItem('token')}))
    },30000)
    if(roundTimer) clearInterval(roundTimer)
    roundTimer=setInterval(()=>{tick.value++},1000)
  }
  ws.onmessage=(e)=>{
    try{
      const p=JSON.parse(e.data)
      if(p.type==='playerPush'){
        const or=player.currentRoomName
        const wasDead=player.currentHealth<=0
        Object.assign(player,p.data)
        srvX=p.data.posX||1;srvY=p.data.posY||1
        if(p.data.currentRoomName!==or || wasDead){
          clX=srvX;clY=srvY;subX=0;subY=0;ldx=1;ldy=0
        }else if(Math.abs(clX-srvX)>2||Math.abs(clY-srvY)>2){
          clX=srvX;clY=srvY;subX=0;subY=0;ldx=1;ldy=0
        }
      }else if(p.type==='roomPush'){
        if(p.data&&p.data.players){
          for(const rp of p.data.players){
            const pv=prevHp[rp.userId]
            if(pv!==undefined&&rp.currentHealth<pv){
              hitFx.push({x:rp.posX||1,y:rp.posY||1,t:Date.now()})
              dmgNums.push({x:rp.posX||1,y:rp.posY||1,val:pv-rp.currentHealth,h:30,t:Date.now()})
            }
            prevHp[rp.userId]=rp.currentHealth
          }
        }
        Object.assign(room,p.data)
      }else if(p.type==='gmAuth'){
        if(p.data==='ok'){gmAuthed.value=true;gmError.value='';gmInput.value=''}
        else{gmError.value='密钥错误';gmAuthed.value=false}
      }else if(p.type==='roundTime'){
        serverRoundEnd.value=Date.now()+parseInt(p.data)*1000
      }else if(p.type==='roundReset'){
        serverRoundEnd.value=Date.now()+parseInt(p.data||'600')*1000
        pushMsg('新的一轮开始！所有玩家已重置','kill')
        setTimeout(()=>{location.reload()},3000)
      }else if(p.type==='rankings'){
        try{rankings.value=JSON.parse(p.data)}catch(e){}
      }else if(p.type==='messagePush'){
        const txt=p.data
        let cls=''
        if(typeof txt==='string'){
          if(txt.includes('defeated')){cls='kill';pushMsg(txt,cls)}
          else if(txt.includes('Hit ')){pushMsg(txt,'hit')}
          else if(txt.includes('countered')){pushMsg(txt,'counter')}
          else if(txt.includes('picked up')) pushMsg(txt,'pickup')
          else if(txt.includes('ATK:')){
            // broadcast: parse and render attack FX
            const parts=txt.substring(4).split(',')
            if(parts.length>=4){
          atkFx.push({x:parseInt(parts[0]),y:parseInt(parts[1]),dx:parseInt(parts[2]),dy:parseInt(parts[3]),tp:parts[4]||'melee',rng:parseInt(parts[5])||1,t:Date.now()})
            }
          } else pushMsg(txt)
        }
      }
    }catch{/*ignore*/}
  }
  ws.onclose=()=>{
    connected.value=false;disconnected.value=true
    if(reconTimer) clearTimeout(reconTimer)
    if(reconAttempts>=MAX_RECON) return
    reconAttempts++
    const delay=Math.min(30000,3000*Math.pow(2,reconAttempts-1))
    reconTimer=setTimeout(connect,delay)
  }
  ws.onerror=()=>ws?.close()
}

onMounted(()=>{
  nextTick(()=>{
    if(canvas.value){
      canvas.value.width=W*T;canvas.value.height=H*T
      ctx=canvas.value.getContext('2d');render()
    }
  })
  window.addEventListener('keydown',e=>{
    keys[e.key.toLowerCase()]=true
    if(e.key===' '){e.preventDefault();if(ws?.readyState===WebSocket.OPEN) ws.send(JSON.stringify({action:'interact',data:null,token:sessionStorage.getItem('token')}))}
    if(e.key==='i'){e.preventDefault();showBag.value=!showBag.value}
    if(e.key==='h'){e.preventDefault();showHelp.value=!showHelp.value;showBag.value=false}
    if(e.key==='F5'){e.preventDefault();ws?.send(JSON.stringify({action:'command',data:'save',token:sessionStorage.getItem('token')}))}
    if(e.key==='F9'){e.preventDefault();ws?.send(JSON.stringify({action:'command',data:'load',token:sessionStorage.getItem('token')}))}
  })
  window.addEventListener('keyup',e=>{keys[e.key.toLowerCase()]=false})
  window.addEventListener('blur',()=>{keys={}})
  connect()
})
onUnmounted(()=>{
  cancelAnimationFrame(aid)
  if(hbTimer) clearInterval(hbTimer)
  if(reconTimer) clearTimeout(reconTimer)
  if(roundTimer) clearInterval(roundTimer)
  ws?.close()
  keys={};hitFx=[];atkFx=[];dust=[];rmtP={};prevHp={}
})
</script>

<style scoped>
.game-root{width:100vw;height:100vh;background:#030308;display:flex;align-items:center;justify-content:center;position:relative;overflow:hidden;font-family:'VT323',monospace;image-rendering:pixelated;}

.loading{display:flex;flex-direction:column;align-items:center;gap:12px;z-index:100;}
.load-logo{font-family:'Press Start 2P',monospace;font-size:24px;color:#c0a060;text-shadow:0 0 20px rgba(192,160,96,.3),0 2px 0 #000;letter-spacing:4px;}
.load-logo2{font-family:'Press Start 2P',monospace;font-size:18px;color:#806030;text-shadow:0 0 15px rgba(128,96,48,.3);}
.load-logo3{font-family:'Press Start 2P',monospace;font-size:24px;color:#c0a060;text-shadow:0 0 20px rgba(192,160,96,.3),0 2px 0 #000;letter-spacing:4px;}
.load-bar-wrap{width:200px;height:6px;background:#111;border:2px solid #333;border-radius:3px;overflow:hidden;}
.load-bar{width:60%;height:100%;background:linear-gradient(90deg,#604020,#c0a060);border-radius:2px;animation:loadBar 1.5s ease-in-out infinite;}
@keyframes loadBar{0%,100%{transform:translateX(-30%)}50%{transform:translateX(130%)}}
.load-text{color:#666;font-size:11px;font-family:'VT323',monospace;letter-spacing:2px;}

.game-canvas{border:3px solid #1a1020;box-shadow:0 0 60px rgba(60,30,80,.2),inset 0 0 0 1px rgba(255,255,255,.02);}

.hud-layer{position:absolute;inset:0;pointer-events:none;z-index:10;}
.hud-layer>*{pointer-events:auto;}

/* Ranking Panel */
.rank-panel{position:absolute;left:220px;top:12px;width:150px;max-height:70vh;background:rgba(0,0,0,.75);border:2px solid #444;border-radius:6px;padding:8px;overflow-y:auto;}
.rank-title{color:#f0d060;font-size:12px;font-weight:700;letter-spacing:1px;text-align:center;margin-bottom:6px;text-shadow:0 0 6px rgba(240,208,96,.3);}
.rank-list{display:flex;flex-direction:column;gap:2px;}
.rank-row{display:flex;align-items:center;gap:6px;padding:3px 6px;border-radius:3px;font-size:11px;}
.rank-row.me{background:rgba(240,208,96,.1);}
.rank-idx{width:18px;text-align:center;color:#888;font-weight:700;}
.rank-idx.top1{color:#ffd700}.rank-idx.top2{color:#c0c0c0}.rank-idx.top3{color:#cd7f32}
.rank-name{flex:1;color:#ccc;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.rank-row.me .rank-name{color:#f0d060;}
.rank-kills{color:#ff7043;font-weight:700;}
.rank-empty{color:#555;text-align:center;font-size:11px;padding:8px;}

/* Top Left - HP & Stats */
.hud-tl{position:absolute;top:12px;left:12px;display:flex;flex-direction:column;gap:6px;}
.player-name-tag{color:#e0d0a0;font-size:14px;text-shadow:0 0 8px rgba(192,160,96,.3);letter-spacing:1px;text-transform:uppercase;}
.hp-bar-bg{width:200px;height:22px;background:rgba(0,0,0,.7);border:2px solid #333;border-radius:4px;position:relative;overflow:hidden;}
.hp-bar-fill{height:100%;background:linear-gradient(90deg,#f44336,#ff9800,#4caf50);border-radius:2px;transition:width .3s;}
.hp-bar-text{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;color:#fff;font-size:12px;font-weight:700;text-shadow:0 1px 2px #000;}
.stat-row{display:flex;gap:8px;}
.stat-badge{padding:3px 10px;border:1px solid #444;border-radius:4px;font-size:12px;letter-spacing:1px;text-shadow:0 1px 0 #000;background:rgba(0,0,0,.6);}
.stat-badge.atk{color:#ff8a65;border-color:#5a3020;}
.stat-badge.def{color:#64b5f6;border-color:#2a4060;}
.stat-icon{font-size:14px;}

/* Top Right - Minimap */
.hud-tr{position:absolute;top:12px;right:12px;display:flex;flex-direction:column;align-items:flex-end;gap:4px;}
.round-timer{color:#ffd54f;font-size:14px;font-weight:700;letter-spacing:1px;text-shadow:0 0 8px rgba(255,213,79,.3);}
.round-timer.warn{color:#f44336;text-shadow:0 0 12px rgba(244,67,54,.5);animation:pulse .5s infinite alternate;}
@keyframes pulse{from{opacity:1}to{opacity:.5}}
.minimap{width:75px;height:50px;border:2px solid #444;border-radius:3px;background:rgba(0,0,0,.8);overflow:hidden;display:flex;flex-direction:column;}
.mm-row{display:flex;height:5px;}
.mm-cell{flex:1;}
.mm-cell.wall{background:#333;}
.mm-cell.floor{background:#141420;}
.mm-cell.door{background:#2a5a2a;}
.mm-cell.player{background:#4caf50;box-shadow:0 0 4px #4caf50;}
.mm-cell.otherPlayer{background:#ff7043;box-shadow:0 0 3px #ff7043;}
.mm-cell.itemDot{background:#ffd700;box-shadow:0 0 2px #ffd700;}
.room-label{color:#c0a060;font-size:12px;text-shadow:0 0 8px rgba(192,160,96,.3);letter-spacing:1px;}
.player-count{color:#666;font-size:10px;letter-spacing:1px;}

/* Room Banner */
.room-banner{position:absolute;top:48px;left:50%;transform:translateX(-50%);color:#c0a060;font-size:14px;text-shadow:0 0 10px rgba(192,160,96,.3),0 1px 0 #000;text-transform:uppercase;letter-spacing:2px;}
.room-fade-enter-active{transition:opacity .2s}
.room-fade-leave-active{transition:opacity .5s}
.room-fade-enter-from,.room-fade-leave-to{opacity:0}

/* Bottom Right - Equipment Slots */
.hud-br{position:absolute;bottom:40px;right:12px;display:flex;gap:8px;}
.eq-slot{display:flex;flex-direction:column;align-items:center;justify-content:center;padding:6px 10px;border:2px solid #333;border-radius:4px;background:rgba(0,0,0,.7);cursor:pointer;min-width:70px;transition:border-color .2s;}
.eq-slot:hover{border-color:#666;}
.eq-slot.filled{border-color:#604020;}
.eq-slot.bag-btn{border-color:#444;}
.eq-slot.gm-btn{border-color:#6200ea;opacity:.8;}.eq-slot.gm-btn:hover{border-color:#a0f;opacity:1;}
.eq-icon{font-size:20px;margin-bottom:2px;}
.eq-label{color:#666;font-size:9px;letter-spacing:1px;text-transform:uppercase;}
.eq-name{color:#ccc;font-size:10px;text-align:center;max-width:80px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.eq-name.dim{color:#444;}

/* Bottom Center */
.hud-bc{position:absolute;bottom:14px;left:50%;transform:translateX(-50%);color:#555;font-size:11px;letter-spacing:1px;}
.key-hint{color:#888;font-weight:700;}

/* Messages */
.msg-log{position:absolute;bottom:90px;left:50%;transform:translateX(-50%);display:flex;flex-direction:column;align-items:center;gap:3px;max-width:400px;}
.msg-line{color:#ccc;font-size:13px;background:rgba(0,0,0,.85);padding:3px 14px;border-radius:4px;text-shadow:0 1px 0 #000;white-space:nowrap;}
.msg-line.kill{color:#f0d060;background:rgba(80,40,0,.85);font-weight:700;}
.msg-line.hit{color:#ff8a65;}
.msg-line.counter{color:#ef5350;}
.msg-line.pickup{color:#81c784;}

/* Inventory */
.inv-bg{position:fixed;inset:0;background:rgba(0,0,0,.75);display:flex;align-items:center;justify-content:center;z-index:50;}
.inv-box{background:#0d0d18;border:3px solid #2a2030;border-radius:8px;padding:16px;min-width:380px;max-width:440px;max-height:70vh;display:flex;flex-direction:column;box-shadow:0 0 40px rgba(60,30,80,.3);}
.inv-top{display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;}
.inv-title{color:#c0a060;font-size:14px;letter-spacing:2px;font-weight:700;}
.inv-close{color:#666;cursor:pointer;font-size:18px;}.inv-close:hover{color:#f66;}
.inv-weight{color:#888;font-size:12px;margin-bottom:10px;}
.inv-list{overflow-y:auto;display:flex;flex-direction:column;gap:4px;}
.inv-empty{color:#444;text-align:center;padding:24px;font-size:14px;}
.inv-row{display:flex;gap:8px;align-items:center;padding:6px 10px;border-radius:6px;background:rgba(255,255,255,.02);border:1px solid rgba(255,255,255,.05);}
.inv-row.isWpn{border-color:rgba(255,140,0,.15);}
.inv-row.isArm{border-color:rgba(100,181,246,.15);}
.inv-icon{font-size:20px;min-width:26px;text-align:center;}
.inv-mid{flex:1;min-width:0;}
.inv-name{color:#ddd;font-size:13px;font-weight:700;}
.inv-desc{color:#666;font-size:11px;}
.inv-btns{display:flex;gap:3px;flex-shrink:0;}
.sk-btn{border:1px solid #333;border-radius:3px;padding:3px 8px;cursor:pointer;font-size:10px;font-family:'VT323',monospace;text-transform:uppercase;letter-spacing:1px;background:rgba(0,0,0,.4);color:#aaa;transition:all .15s;}
.sk-btn:hover{color:#fff;}
.sk-btn.green{border-color:#2e5a2e;color:#8f8;}.sk-btn.green:hover{background:rgba(46,90,46,.3);}
.sk-btn.blue{border-color:#2a3a6a;color:#8af;}.sk-btn.blue:hover{background:rgba(42,58,106,.3);}
.sk-btn.cyan{border-color:#1a4a5a;color:#8dd;}.sk-btn.cyan:hover{background:rgba(26,74,90,.3);}
.sk-btn.red{border-color:#5a2a2a;color:#f88;}.sk-btn.red:hover{background:rgba(90,42,42,.3);}
</style>
