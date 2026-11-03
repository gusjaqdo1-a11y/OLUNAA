"use client";

import { useMemo, useState } from "react";
import {
  Bell, Camera, Check, ChevronRight, Compass, Heart, Image as ImageIcon,
  LockKeyhole, MessageCircle, Mic, MoreHorizontal, Paperclip, Phone,
  Play, Plus, Search, Send, Settings, Sparkles, UserPlus, Users, Video,
  WandSparkles, X, Zap, Globe2, ShieldCheck, Headphones, Radio, Bookmark,
  CircleUserRound, LogOut, Menu, ArrowLeft, Hash, UserRoundSearch, Smile,
  Pin, FileText, Link2, UserCheck, Clock3, Volume2, AtSign, SlidersHorizontal,
  CameraOff, PhoneCall, Shield, Languages, Bot, UserRoundPlus
} from "lucide-react";

type Chat = { name:string; handle:string; avatar:string; message:string; time:string; online?:boolean; unread?:number; color:string; group?:boolean };
type Story = { name:string; handle:string; avatar:string; color:string; seen?:boolean; live?:boolean };
type Group = { name:string; handle:string; members:string; avatar:string; color:string; description:string };

const chats: Chat[] = [
  {name:"Amina",handle:"@amina",avatar:"A",message:"That launch looks incredible ✨",time:"9:18",online:true,color:"#9b5cff",unread:2},
  {name:"OLUNAA Community",handle:"@olunaa",avatar:"O",message:"New community update",time:"9:04",unread:8,color:"#5c7cff",group:true},
  {name:"Ahmed",handle:"@ahmedx",avatar:"A",message:"Voice note · 0:18",time:"8:51",online:true,color:"#14b8a6"},
  {name:"Creators Hub",handle:"@creators",avatar:"C",message:"Maya shared a reel",time:"Yesterday",color:"#ff7a59",group:true},
  {name:"Family",handle:"@family",avatar:"F",message:"See you tonight ❤️",time:"Yesterday",color:"#ec4899",unread:3,group:true},
];

const discover = [
  {name:"Maya Hassan",handle:"@mayah",followers:"248K",tag:"Creator",color:"#e879f9"},
  {name:"Tech World",handle:"@techworld",followers:"92K",tag:"Community",color:"#38bdf8"},
  {name:"Luna Studio",handle:"@lunastudio",followers:"61K",tag:"Design",color:"#a78bfa"},
  {name:"Omar Ali",handle:"@omarali",followers:"18K",tag:"Photographer",color:"#f59e0b"},
  {name:"Nora Labs",handle:"@noralabs",followers:"31K",tag:"AI & Future",color:"#22c55e"},
];

const stories: Story[] = [
  {name:"Amina",handle:"@amina",avatar:"A",color:"#d946ef",live:true},
  {name:"Maya",handle:"@mayah",avatar:"M",color:"#8b5cf6"},
  {name:"Omar",handle:"@omarali",avatar:"O",color:"#f59e0b"},
  {name:"Luna",handle:"@lunastudio",avatar:"L",color:"#06b6d4"},
  {name:"Creators",handle:"@creators",avatar:"C",color:"#ef4444"},
  {name:"Family",handle:"@family",avatar:"F",color:"#ec4899"},
];

const groups: Group[] = [
  {name:"OLUNAA Community",handle:"@olunaa",members:"24.8K members",avatar:"O",color:"#6956ff",description:"Official community for product news and creators."},
  {name:"Creators Hub",handle:"@creators",members:"8.4K members",avatar:"C",color:"#ff7a59",description:"Share work, ideas, feedback and opportunities."},
  {name:"Tech World",handle:"@techworld",members:"92K members",avatar:"T",color:"#22c7ff",description:"The future of technology, AI and startups."},
  {name:"Travel Lovers",handle:"@travel",members:"16K members",avatar:"✈",color:"#2dd4bf",description:"Beautiful places, stories and travel tips."},
];

type View = "chats"|"discover"|"stories"|"groups"|"saved"|"profile";

export default function Home() {
  const [view,setView] = useState<View>("chats");
  const [active,setActive] = useState(chats[0]);
  const [composer,setComposer] = useState("");
  const [messages,setMessages] = useState<string[]>(["Hello! 👋", "How are you today?", "I’m great — OLUNAA is looking amazing ✨"]);
  const [search,setSearch] = useState("");
  const [showNew,setShowNew] = useState(false);
  const [authMode,setAuthMode] = useState<"login"|"register"|null>(null);
  const [followed,setFollowed] = useState<Record<string,boolean>>({});
  const [joined,setJoined] = useState<Record<string,boolean>>({});
  const [mobileRail,setMobileRail] = useState(false);
  const [showCall,setShowCall] = useState(false);
  const [showProfile,setShowProfile] = useState(false);
  const [toast,setToast] = useState("");

  const filtered = useMemo(()=>chats.filter(c => `${c.name} ${c.handle}`.toLowerCase().includes(search.toLowerCase())),[search]);
  const globalResults = useMemo(()=>[...discover.map(d=>({kind:"person",name:d.name,handle:d.handle,meta:`${d.followers} followers`,color:d.color,avatar:d.name[0]})),...groups.map(g=>({kind:"group",name:g.name,handle:g.handle,meta:g.members,color:g.color,avatar:g.avatar}))].filter(x=>`${x.name} ${x.handle}`.toLowerCase().includes(search.toLowerCase())),[search]);

  const notify = (text:string) => { setToast(text); window.setTimeout(()=>setToast(""),2200); };
  const send = () => { const m=composer.trim(); if(!m) return; setMessages(v=>[...v,m]); setComposer(""); };
  const openChat = (chat:Chat) => { setActive(chat); setView("chats"); };

  return <main className="app-shell">
    <aside className={`sidebar ${mobileRail?"mobile-open":""}`}>
      <div className="brand-row"><div className="brand-mark"><span>O</span></div><span className="brand-name">OLUNAA</span><button className="icon-btn desktop-hide" onClick={()=>setMobileRail(false)}><X size={18}/></button></div>
      <button className="profile-mini profile-button" onClick={()=>setShowProfile(true)}><div className="avatar me">N</div><div><b>New account</b><span>@olunaa_user</span></div><MoreHorizontal size={18}/></button>
      <nav className="nav">
        <NavButton active={view==="chats"} onClick={()=>setView("chats")} icon={<MessageCircle size={19}/>} label="Messages" badge="12"/>
        <NavButton active={view==="discover"} onClick={()=>setView("discover")} icon={<Compass size={19}/>} label="Discover"/>
        <NavButton active={view==="stories"} onClick={()=>setView("stories")} icon={<Radio size={19}/>} label="Stories" live/>
        <NavButton active={view==="groups"} onClick={()=>setView("groups")} icon={<Users size={19}/>} label="Communities"/>
        <NavButton active={view==="saved"} onClick={()=>setView("saved")} icon={<Bookmark size={19}/>} label="Saved"/>
      </nav>
      <div className="side-features"><div><Zap size={16}/><span>Instant sync</span></div><div><ShieldCheck size={16}/><span>Privacy first</span></div><div><Globe2 size={16}/><span>Global by design</span></div><div><Bot size={16}/><span>AI native</span></div></div>
      <div className="sidebar-bottom"><button onClick={()=>notify("Settings are ready for your account.")}><Settings size={18}/> Settings</button><button onClick={()=>setAuthMode("login")}><LogOut size={18}/> Sign in / switch</button></div>
    </aside>

    <section className="inbox">
      <header className="inbox-head"><div><span className="eyebrow">YOUR SPACE</span><h1>{view==="discover"?"Discover":view==="stories"?"Stories":view==="groups"?"Communities":view==="saved"?"Saved":"Messages"}</h1></div><div className="head-actions"><button className="round-btn" onClick={()=>setShowNew(true)}><Plus size={20}/></button><button className="round-btn" onClick={()=>notify("No new notifications")}><Bell size={18}/><i/></button></div></header>
      {(view==="chats"||view==="discover"||view==="groups") && <div className="search"><Search size={17}/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search people, chats, groups or @usernames"/><SlidersHorizontal size={15}/></div>}
      {view==="chats" && <><div className="filter-row"><button className="pill active">All</button><button className="pill">Unread</button><button className="pill">Groups</button><button className="pill">Mentions</button></div>{search && <SearchResults results={globalResults} onPerson={(r)=>notify(`Opening ${r.handle}`)} onGroup={(r)=>notify(`Opening ${r.name}`)}/>}<div className="chat-list">{filtered.map(c=><button className={`chat-item ${active.handle===c.handle?"selected":""}`} key={c.handle} onClick={()=>openChat(c)}><div className="avatar" style={{background:c.color}}>{c.avatar}</div><div className="chat-copy"><div><b>{c.name}</b>{c.online&&<span className="online">●</span>}<time>{c.time}</time></div><p>{c.message}</p></div>{c.unread&&<span className="unread">{c.unread}</span>}</button>)}</div></>}
      {view==="discover" && <Discover followed={followed} setFollowed={setFollowed} notify={notify}/>} 
      {view==="stories" && <Stories stories={stories} notify={notify}/>} 
      {view==="groups" && <Groups joined={joined} setJoined={setJoined} notify={notify}/>} 
      {view==="saved" && <Saved/>}
    </section>

    <section className="conversation">
      <header className="conversation-head"><button className="mobile-back" onClick={()=>setMobileRail(true)}><ArrowLeft size={20}/></button><div className="avatar" style={{background:active.color}}>{active.avatar}</div><div className="contact"><b>{active.name}</b><span><i className="online-dot"/> Active now · {active.handle}</span></div><div className="conversation-actions"><button className="icon-btn" onClick={()=>setShowCall(true)}><Phone size={18}/></button><button className="icon-btn" onClick={()=>setShowCall(true)}><Video size={19}/></button><button className="icon-btn" onClick={()=>notify("Search inside conversation") }><Search size={18}/></button><button className="icon-btn" onClick={()=>notify("Conversation menu") }><MoreHorizontal size={19}/></button></div></header>
      <div className="conversation-body"><div className="secure-note"><LockKeyhole size={13}/> End-to-end encrypted · You control this conversation</div><div className="date-chip">Today</div>{messages.map((m,i)=><div className={`bubble-row ${i===messages.length-1?"mine":""}`} key={`${m}-${i}`}><div className="bubble">{m}<span>{i+9}:1{i}<Check size={12}/></span></div></div>)}<div className="assistant-card"><div className="ai-icon"><Sparkles size={17}/></div><div><b>OLUNAA AI</b><p>Reply, translate, summarize, create or find information without leaving your chat.</p></div><button onClick={()=>notify("OLUNAA AI is ready")}><WandSparkles size={16}/></button></div></div>
      <div className="composer-wrap"><div className="composer"><button className="icon-btn" onClick={()=>setShowNew(true)}><Plus size={19}/></button><input value={composer} onChange={e=>setComposer(e.target.value)} onKeyDown={e=>e.key==='Enter'&&send()} placeholder="Write a message…"/><button className="icon-btn" onClick={()=>notify("Attachments ready")}><Paperclip size={18}/></button><button className="icon-btn" onClick={()=>notify("Camera opened") }><Camera size={18}/></button>{composer?<button className="send-btn" onClick={send}><Send size={17}/></button>:<button className="voice-btn" onClick={()=>notify("Recording voice message…")}><Mic size={18}/></button>}</div><div className="composer-hint"><span><ShieldCheck size={12}/> Private by default</span><span>Enter to send · <Smile size={10}/> Reactions</span></div></div>
    </section>

    <aside className="right-rail"><div className="rail-card hero-card"><span className="spark"><Sparkles size={16}/></span><span className="eyebrow">BUILT FOR WHAT'S NEXT</span><h2>More than a messenger.</h2><p>Chat, follow, create, discover and connect — in one smooth space.</p><div className="feature-tags"><span>AI native</span><span>Live translation</span><span>Cloud sync</span><span>Creator tools</span><span>Stories</span><span>Communities</span></div></div><div className="rail-card"><div className="rail-title"><b>People you may know</b><button onClick={()=>setView("discover")}>See all</button></div>{discover.slice(0,3).map(d=><div className="suggestion" key={d.handle}><div className="avatar small" style={{background:d.color}}>{d.name[0]}</div><div><b>{d.name}</b><span>{d.handle}</span></div><button onClick={()=>setFollowed(v=>({...v,[d.handle]:!v[d.handle]}))}>{followed[d.handle]?<Check size={15}/>:<Plus size={16}/>}</button></div>)}</div><div className="rail-card stats"><div><span>Latency</span><b>&lt; 120ms</b></div><div><span>Privacy</span><b>Private</b></div><div><span>Availability</span><b>Global</b></div></div></aside>

    <button className="mobile-menu" onClick={()=>setMobileRail(true)}><Menu size={21}/></button>
    {showNew&&<Modal title="Create or connect" onClose={()=>setShowNew(false)}><div className="modal-grid"><Action icon={<MessageCircle/>} title="New chat" text="Message anyone by @username" onClick={()=>{setShowNew(false);setView("chats")}}/><Action icon={<Users/>} title="Create community" text="Build a space around a topic" onClick={()=>{setShowNew(false);setView("groups")}}/><Action icon={<Radio/>} title="Post a story" text="Share a moment for 24 hours" onClick={()=>{setShowNew(false);setView("stories");notify("Story composer opened")}}/><Action icon={<Headphones/>} title="Start a room" text="Talk live with your people" onClick={()=>{setShowNew(false);notify("Live room created")}}/><Action icon={<UserRoundPlus/>} title="Invite friends" text="Bring your people to OLUNAA" onClick={()=>notify("Invite link copied")}/><Action icon={<Bot/>} title="Ask OLUNAA AI" text="Create, translate or summarize" onClick={()=>notify("AI assistant ready")}/></div></Modal>}
    {authMode&&<AuthModal mode={authMode} onModeChange={setAuthMode} onClose={()=>setAuthMode(null)} notify={notify}/>} 
    {showCall&&<CallModal contact={active} onClose={()=>setShowCall(false)}/>} 
    {showProfile&&<ProfileModal onClose={()=>setShowProfile(false)} onAuth={()=>{setShowProfile(false);setAuthMode("register")}}/>}
    {toast&&<div className="toast"><Check size={15}/>{toast}</div>}

    <style jsx global>{`
      .profile-button{width:100%;cursor:pointer;border:1px solid var(--line)}
      .profile-button>svg{margin-left:auto;color:#777d90}
      .search svg:last-child{margin-left:auto;color:#555b6d}
      .search-results{margin:0 12px 8px;border:1px solid var(--line);border-radius:14px;background:#10121a;overflow:hidden}
      .result{width:100%;display:flex;align-items:center;gap:9px;padding:9px;text-align:left;border-bottom:1px solid var(--line)}
      .result:last-child{border-bottom:0}.result:hover{background:#171925}.result-copy{min-width:0;flex:1}.result-copy b,.result-copy span{display:block}.result-copy b{font-size:10px}.result-copy span{font-size:8px;color:#707688;margin-top:3px}.result-kind{font-size:7px;color:#9d82ff;padding:4px 6px;border-radius:7px;background:#1b1730}
      .story-toolbar{display:flex;gap:8px;padding:8px 12px 2px}.story-toolbar button{padding:8px 10px;border-radius:10px;background:#151721;border:1px solid var(--line);font-size:9px}.story-toolbar .primary-mini{background:#7047e8;border-color:transparent}
      .group-list{padding:5px 12px}.group-card{display:flex;align-items:center;gap:10px;padding:13px 7px;border-bottom:1px solid var(--line)}.group-copy{min-width:0;flex:1}.group-copy b,.group-copy span,.group-copy small{display:block}.group-copy b{font-size:11px}.group-copy span{font-size:8px;color:#9c7eff;margin-top:3px}.group-copy small{font-size:8px;color:#777d90;margin-top:4px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.join-btn{padding:7px 10px;border-radius:10px;background:#7549ef;font-size:9px}.join-btn.joined{background:#1b1d27;color:#aeb2c1}.saved-list{padding:12px}.saved-card{display:flex;gap:10px;padding:12px;border:1px solid var(--line);border-radius:14px;margin-bottom:8px;background:#10121a}.saved-icon{width:35px;height:35px;border-radius:11px;background:#211a3c;color:#ad91ff;display:grid;place-items:center;flex:none}.saved-card b{font-size:10px}.saved-card p{font-size:9px;color:#777d90;margin:4px 0 0;line-height:1.5}.story-grid{grid-template-columns:repeat(2,1fr)}
      .toast{position:fixed;z-index:100;left:50%;bottom:22px;transform:translateX(-50%);display:flex;align-items:center;gap:7px;padding:10px 14px;border:1px solid rgba(255,255,255,.12);background:#151722;border-radius:13px;box-shadow:0 20px 60px rgba(0,0,0,.5);font-size:10px;color:#e7e8ee}.toast svg{color:#8d6aff}
      .call-modal{width:min(700px,100%);background:#090a10;border:1px solid var(--line);border-radius:25px;overflow:hidden}.call-stage{height:360px;display:grid;place-items:center;background:radial-gradient(circle at 50% 30%,rgba(121,76,255,.22),transparent 35%),linear-gradient(145deg,#10111a,#08090e);position:relative}.call-avatar{width:105px;height:105px;border-radius:32px;display:grid;place-items:center;font:800 42px Manrope;box-shadow:0 0 0 8px rgba(139,92,246,.08),0 0 70px rgba(139,92,246,.22)}.call-name{position:absolute;bottom:35px;text-align:center}.call-name b,.call-name span{display:block}.call-name b{font-size:15px}.call-name span{font-size:9px;color:#7e8393;margin-top:5px}.call-actions{display:flex;justify-content:center;gap:10px;padding:17px}.call-actions button{width:44px;height:44px;border-radius:14px;background:#171925;display:grid;place-items:center}.call-actions .hangup{background:#ef4444}.auth-tabs{display:flex;gap:4px;padding:4px;background:#11131c;border-radius:12px;margin-bottom:4px}.auth-tabs button{flex:1;padding:8px;border-radius:9px;font-size:9px;color:#747a8a}.auth-tabs button.active{background:#24202f;color:white}.username-check{font-size:8px;margin-top:5px;display:flex;align-items:center;gap:4px}.username-check.ok{color:#34d36a}.username-check.bad{color:#ff6b6b}.password-meter{height:3px;background:#1c1f2a;border-radius:99px;margin-top:6px;overflow:hidden}.password-meter i{display:block;height:100%;width:65%;background:#8b5cf6}.profile-modal{display:grid;gap:14px;text-align:center}.profile-cover{height:80px;border-radius:16px;background:linear-gradient(120deg,#2c1b5a,#1a355e,#102f39)}.profile-big{width:76px;height:76px;margin:-38px auto 0;border-radius:24px;background:linear-gradient(145deg,#8b5cf6,#4f46e5);display:grid;place-items:center;font:800 30px Manrope;border:5px solid #0d0f17}.profile-modal h3{margin:0;font:800 17px Manrope}.profile-modal p{margin:0;color:#777d90;font-size:9px}.profile-stats{display:grid;grid-template-columns:repeat(3,1fr);gap:7px}.profile-stats div{background:#11131c;padding:9px;border-radius:11px}.profile-stats b,.profile-stats span{display:block}.profile-stats b{font-size:11px}.profile-stats span{font-size:7px;color:#73788a;margin-top:3px}.profile-actions{display:flex;gap:7px}.profile-actions button{flex:1;height:38px;border-radius:11px;font-size:9px}.profile-actions .primary{background:#7449ee}.profile-actions .secondary{border:1px solid var(--line)}
      @media(max-width:820px){.sidebar{position:fixed;z-index:80;left:-280px;top:0;bottom:0;width:270px;transition:left .22s}.sidebar.mobile-open{left:0}.inbox{grid-column:1/-1;width:100%;border-right:0}.conversation{display:none}.mobile-menu,.mobile-back{display:grid}.mobile-menu{position:fixed;z-index:70;left:12px;bottom:15px;width:43px;height:43px;border-radius:14px;background:#7449ee;place-items:center;box-shadow:0 12px 35px rgba(116,73,238,.35)}.mobile-back{place-items:center}.desktop-hide{display:grid}.right-rail{display:none}.app-shell{display:block}.inbox{min-height:100vh}.chat-item{padding:13px 9px}.inbox-head{padding-left:65px}.story-grid{grid-template-columns:repeat(3,1fr)}.modal-grid{grid-template-columns:1fr 1fr}.call-stage{height:300px}}
      @media(max-width:480px){.story-grid{grid-template-columns:repeat(2,1fr)}.modal-grid{grid-template-columns:1fr}.inbox-head h1{font-size:22px}.filter-row{overflow:auto}.composer-hint span:last-child{display:none}}
    `}</style>
  </main>;
}

function NavButton({active,onClick,icon,label,badge,live}:{active:boolean;onClick:()=>void;icon:React.ReactNode;label:string;badge?:string;live?:boolean}){return <button className={active?"active":""} onClick={onClick}>{icon}<span>{label}</span>{badge&&<em>{badge}</em>}{live&&<i className="live-dot"/>}</button>}

function Action({icon,title,text,onClick}:{icon:React.ReactNode;title:string;text:string;onClick:()=>void}){return <button onClick={onClick}>{icon}<b>{title}</b><span>{text}</span></button>}

function SearchResults({results,onPerson,onGroup}:{results:any[];onPerson:(r:any)=>void;onGroup:(r:any)=>void}){if(!results.length)return null;return <div className="search-results">{results.slice(0,5).map((r:any)=><button className="result" key={r.handle} onClick={()=>r.kind==="person"?onPerson(r):onGroup(r)}><div className="avatar small" style={{background:r.color}}>{r.avatar}</div><div className="result-copy"><b>{r.name}</b><span>{r.handle} · {r.meta}</span></div><span className="result-kind">{r.kind}</span></button>)}</div>}

function Discover({followed,setFollowed,notify}:{followed:Record<string,boolean>;setFollowed:React.Dispatch<React.SetStateAction<Record<string,boolean>>>;notify:(s:string)=>void}){return <div className="discover-list">{discover.map(d=><div className="person-card" key={d.handle}><div className="avatar big" style={{background:d.color}}>{d.name[0]}</div><div className="person-copy"><b>{d.name}</b><span>{d.handle} · {d.followers} followers</span><small>{d.tag}</small></div><button className={followed[d.handle]?"follow following":"follow"} onClick={()=>{setFollowed(v=>({...v,[d.handle]:!v[d.handle]}));notify(followed[d.handle]?`Unfollowed ${d.handle}`:`Following ${d.handle}`)}}>{followed[d.handle]?<><Check size={15}/> Following</>:<><UserPlus size={15}/> Follow</>}</button></div>)}</div>}

function Stories({stories,notify}:{stories:Story[];notify:(s:string)=>void}){return <><div className="story-toolbar"><button className="primary-mini" onClick={()=>notify("Story composer ready")}> <Plus size={13}/> Your story</button><button onClick={()=>notify("Story privacy controls opened")}><Shield size={13}/> Privacy</button></div><div className="story-grid">{stories.map(s=><button className="story-card" key={s.handle} onClick={()=>notify(`Opening ${s.name}'s story`)}><div className="story-ring" style={{background:s.live?"linear-gradient(135deg,#ff3040,#ff9f0a,#8b5cf6)":"linear-gradient(135deg,#d946ef,#6d5dfc,#28d7ff)"}}><div className="avatar big" style={{background:s.color}}>{s.avatar}</div></div><span>{s.name} {s.live&&<small style={{display:"inline",color:"#ff5f56"}}>LIVE</small>}</span><small>{s.live?"Live now":"Today"}</small></button>)}</div></>}

function Groups({joined,setJoined,notify}:{joined:Record<string,boolean>;setJoined:React.Dispatch<React.SetStateAction<Record<string,boolean>>>;notify:(s:string)=>void}){return <div className="group-list">{groups.map(g=><div className="group-card" key={g.handle}><div className="avatar big" style={{background:g.color}}>{g.avatar}</div><div className="group-copy"><b>{g.name}</b><span>{g.handle} · {g.members}</span><small>{g.description}</small></div><button className={joined[g.handle]?"join-btn joined":"join-btn"} onClick={()=>{setJoined(v=>({...v,[g.handle]:!v[g.handle]}));notify(joined[g.handle]?`Left ${g.name}`:`Joined ${g.name}`)}}>{joined[g.handle]?<><UserCheck size={13}/> Joined</>:<><UserPlus size={13}/> Join</>}</button></div>)}</div>}

function Saved(){return <div className="saved-list"><div className="saved-card"><div className="saved-icon"><Bookmark size={16}/></div><div><b>Launch notes</b><p>Saved messages, links, media and files live here for quick access.</p></div></div><div className="saved-card"><div className="saved-icon"><Link2 size={16}/></div><div><b>Useful links</b><p>Keep important URLs and shared resources organized in one private place.</p></div></div><div className="saved-card"><div className="saved-icon"><FileText size={16}/></div><div><b>Private files</b><p>Documents and media can be pinned here when the storage layer is connected.</p></div></div></div>}

function AuthModal({mode,onModeChange,onClose,notify}:{mode:"login"|"register";onModeChange:(m:"login"|"register")=>void;onClose:()=>void;notify:(s:string)=>void}){const [email,setEmail]=useState("");const [username,setUsername]=useState("");const [name,setName]=useState("");const [password,setPassword]=useState("");const [loading,setLoading]=useState(false);const [error,setError]=useState("");const validUsername=/^[a-z0-9_]{3,24}$/.test(username.toLowerCase());
 const submit=async()=>{setError("");if(mode==="register"&&!validUsername){setError("Username must be 3–24 characters: letters, numbers or _.");return}if(!email||!password||(mode==="register"&&!name)){setError("Please complete all required fields.");return}setLoading(true);try{const endpoint=mode==="register"?"/api/auth/register":"/api/auth/login";const body=mode==="register"?{email,username:username.toLowerCase(),displayName:name,password}:{identifier:email,password};const res=await fetch(endpoint,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)});const data=await res.json().catch(()=>({}));if(!res.ok)throw new Error(data.error||"Could not complete this request");notify(mode==="register"?`Welcome to OLUNAA, @${username.toLowerCase()}`:`Welcome back, ${data.user?.displayName||"friend"}`);onClose()}catch(e){setError(e instanceof Error?e.message:"Something went wrong. Connect the database and try again.")}finally{setLoading(false)}};
 return <Modal title={mode==="register"?"Create your OLUNAA identity":"Welcome back to OLUNAA"} onClose={onClose}><div className="auth-form"><div className="auth-tabs"><button className={mode==="login"?"active":""} onClick={()=>{onModeChange("login");setError("")}}>Sign in</button><button className={mode==="register"?"active":""} onClick={()=>{onModeChange("register");setError("")}}>Create account</button></div><div className="auth-logo"><div className="brand-mark"><span>O</span></div><b>One identity. Your @username everywhere.</b></div>{mode==="register"&&<><label>Display name<input value={name} onChange={e=>setName(e.target.value)} placeholder="Your name" autoComplete="name"/></label><label>Choose your @username<input value={username} onChange={e=>setUsername(e.target.value.replace(/^@/,""))} placeholder="olunaa_user" autoComplete="username"/><span className={`username-check ${validUsername?"ok":"bad"}`}>{username&&validUsername?<><Check size={11}/> Username format is available</>:username?<><X size={11}/> Use 3–24 letters, numbers or _</>:<><AtSign size={11}/> Your unique OLUNAA identity</>}</span></label></>}<label>Email<input value={email} onChange={e=>setEmail(e.target.value)} type="email" placeholder="you@example.com" autoComplete="email"/></label><label>Password<input value={password} onChange={e=>setPassword(e.target.value)} type="password" placeholder="••••••••" autoComplete={mode==="register"?"new-password":"current-password"/>{mode==="register"&&password&&<span className="password-meter"><i/></span>}</label>{error&&<div style={{color:"#ff6b6b",fontSize:9}}>{error}</div>}<button className="primary" onClick={submit} disabled={loading}>{loading?"Working…":mode==="register"?"Create OLUNAA account":"Continue"} {!loading&&<ChevronRight size={17}/>}</button><div className="or"><span/>or<span/></div><button className="secondary" onClick={()=>notify("Google sign-in can be connected next") }><Sparkles size={17}/> Continue with Google</button><p>{mode==="register"?"Already have an account?":"New here?"} <button onClick={()=>onModeChange(mode==="register"?"login":"register")}>{mode==="register"?"Sign in":"Create your OLUNAA identity"}</button></p></div></Modal>}

function CallModal({contact,onClose}:{contact:Chat;onClose:()=>void}){return <div className="modal-backdrop" onMouseDown={e=>e.currentTarget===e.target&&onClose()}><div className="call-modal"><div className="call-stage"><div className="call-avatar" style={{background:contact.color}}>{contact.avatar}</div><div className="call-name"><b>{contact.name}</b><span>Connecting securely · OLUNAA call</span></div></div><div className="call-actions"><button><Mic size={18}/></button><button><CameraOff size={18}/></button><button><Volume2 size={18}/></button><button className="hangup" onClick={onClose}><PhoneCall size={18}/></button></div></div></div>}

function ProfileModal({onClose,onAuth}:{onClose:()=>void;onAuth:()=>void}){return <div className="modal-backdrop" onMouseDown={e=>e.currentTarget===e.target&&onClose()}><div className="modal"><div className="modal-head"><div><span className="eyebrow">YOUR IDENTITY</span><h2>OLUNAA Profile</h2></div><button className="icon-btn" onClick={onClose}><X size={19}/></button></div><div className="profile-modal"><div className="profile-cover"/><div className="profile-big">N</div><div><h3>New account</h3><p>@olunaa_user · Your public identity</p></div><div className="profile-stats"><div><b>0</b><span>Following</span></div><div><b>0</b><span>Followers</span></div><div><b>0</b><span>Stories</span></div></div><div className="profile-actions"><button className="primary" onClick={onAuth}>Create account</button><button className="secondary" onClick={onClose}>Edit profile</button></div></div></div></div>}

function Modal({title,onClose,children}:{title:string;onClose:()=>void;children:React.ReactNode}){return <div className="modal-backdrop" onMouseDown={e=>e.currentTarget===e.target&&onClose()}><div className="modal"><div className="modal-head"><div><span className="eyebrow">OLUNAA</span><h2>{title}</h2></div><button className="icon-btn" onClick={onClose}><X size={19}/></button></div>{children}</div></div>}
