"use client";

import { useMemo, useState } from "react";
import {
  Bell, Camera, Check, ChevronRight, Compass, Heart, Image as ImageIcon,
  LockKeyhole, MessageCircle, Mic, MoreHorizontal, Paperclip, Phone,
  Play, Plus, Search, Send, Settings, Sparkles, UserPlus, Users, Video,
  WandSparkles, X, Zap, Globe2, ShieldCheck, Headphones, Radio, Bookmark,
  CircleUserRound, LogOut, Menu, ArrowLeft
} from "lucide-react";

type Chat = { name:string; handle:string; avatar:string; message:string; time:string; online?:boolean; unread?:number; color:string };

const chats: Chat[] = [
  {name:"Amina",handle:"@amina",avatar:"A",message:"That launch looks incredible ✨",time:"9:18",online:true,color:"#9b5cff",unread:2},
  {name:"OLUNAA Community",handle:"@olunaa",avatar:"O",message:"New community update",time:"9:04",unread:8,color:"#5c7cff"},
  {name:"Ahmed",handle:"@ahmedx",avatar:"A",message:"Voice note · 0:18",time:"8:51",online:true,color:"#14b8a6"},
  {name:"Creators Hub",handle:"@creators",avatar:"C",message:"Maya shared a reel",time:"Yesterday",color:"#ff7a59"},
  {name:"Family",handle:"@family",avatar:"F",message:"See you tonight ❤️",time:"Yesterday",color:"#ec4899",unread:3},
];

const discover = [
  {name:"Maya Hassan",handle:"@mayah",followers:"248K",tag:"Creator",color:"#e879f9"},
  {name:"Tech World",handle:"@techworld",followers:"92K",tag:"Community",color:"#38bdf8"},
  {name:"Luna Studio",handle:"@lunastudio",followers:"61K",tag:"Design",color:"#a78bfa"},
  {name:"Omar Ali",handle:"@omarali",followers:"18K",tag:"Photographer",color:"#f59e0b"},
];

export default function Home() {
  const [view,setView] = useState<"chats"|"discover"|"stories"|"profile">("chats");
  const [active,setActive] = useState(chats[0]);
  const [composer,setComposer] = useState("");
  const [messages,setMessages] = useState<string[]>(["Hello! 👋", "How are you today?", "I’m great — OLUNAA is looking amazing ✨"]);
  const [search,setSearch] = useState("");
  const [showNew,setShowNew] = useState(false);
  const [showLogin,setShowLogin] = useState(false);
  const [followed,setFollowed] = useState<Record<string,boolean>>({});
  const [menuOpen,setMenuOpen] = useState(false);

  const filtered = useMemo(()=>chats.filter(c => `${c.name} ${c.handle}`.toLowerCase().includes(search.toLowerCase())),[search]);
  const send = () => { const m=composer.trim(); if(!m) return; setMessages(v=>[...v,m]); setComposer(""); };

  return <main className="app-shell">
    <aside className="sidebar">
      <div className="brand-row"><div className="brand-mark"><span>O</span></div><span className="brand-name">OLUNAA</span><button className="icon-btn desktop-hide" onClick={()=>setMenuOpen(false)}><X size={18}/></button></div>
      <div className="profile-mini"><div className="avatar me">N</div><div><b>New account</b><span>@olunaa_user</span></div><button className="icon-btn"><MoreHorizontal size={18}/></button></div>
      <nav className="nav">
        <button className={view==="chats"?"active":""} onClick={()=>setView("chats")}><MessageCircle size={19}/><span>Messages</span><em>12</em></button>
        <button className={view==="discover"?"active":""} onClick={()=>setView("discover")}><Compass size={19}/><span>Discover</span></button>
        <button className={view==="stories"?"active":""} onClick={()=>setView("stories")}><Radio size={19}/><span>Stories</span><i className="live-dot"/></button>
        <button><Users size={19}/><span>Communities</span></button>
        <button><Bookmark size={19}/><span>Saved</span></button>
      </nav>
      <div className="side-features"><div><Zap size={16}/><span>Instant sync</span></div><div><ShieldCheck size={16}/><span>Privacy first</span></div><div><Globe2 size={16}/><span>Global by design</span></div></div>
      <div className="sidebar-bottom"><button><Settings size={18}/> Settings</button><button onClick={()=>setShowLogin(true)}><LogOut size={18}/> Sign in / switch</button></div>
    </aside>

    <section className="inbox">
      <header className="inbox-head"><div><span className="eyebrow">YOUR SPACE</span><h1>{view==="discover"?"Discover":view==="stories"?"Stories":"Messages"}</h1></div><div className="head-actions"><button className="round-btn" onClick={()=>setShowNew(true)}><Plus size={20}/></button><button className="round-btn"><Bell size={18}/><i/></button></div></header>
      {view==="chats" && <><div className="search"><Search size={17}/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search people, chats, @usernames"/></div><div className="filter-row"><button className="pill active">All</button><button className="pill">Unread</button><button className="pill">Groups</button><button className="pill">Channels</button></div><div className="chat-list">{filtered.map(c=><button className={`chat-item ${active.handle===c.handle?"selected":""}`} key={c.handle} onClick={()=>setActive(c)}><div className="avatar" style={{background:c.color}}>{c.avatar}</div><div className="chat-copy"><div><b>{c.name}</b>{c.online&&<span className="online">●</span>}<time>{c.time}</time></div><p>{c.message}</p></div>{c.unread&&<span className="unread">{c.unread}</span>}</button>)}</div></>}
      {view==="discover" && <div className="discover-list">{discover.map(d=><div className="person-card" key={d.handle}><div className="avatar big" style={{background:d.color}}>{d.name[0]}</div><div className="person-copy"><b>{d.name}</b><span>{d.handle} · {d.followers} followers</span><small>{d.tag}</small></div><button className={followed[d.handle]?"follow following":"follow"} onClick={()=>setFollowed(v=>({...v,[d.handle]:!v[d.handle]}))}>{followed[d.handle]?<><Check size={15}/> Following</>:<><UserPlus size={15}/> Follow</>}</button></div>)}</div>}
      {view==="stories" && <div className="story-grid">{["A","M","O","L","C","F"].map((x,i)=><button className="story-card" key={i}><div className="story-ring"><div className="avatar big">{x}</div></div><span>{["Amina","Maya","Omar","Luna","Creators","Family"][i]}</span><small>{i<2?"2m ago":"Today"}</small></button>)}</div>}
    </section>

    <section className="conversation">
      <header className="conversation-head"><button className="mobile-back" onClick={()=>setMenuOpen(true)}><ArrowLeft size={20}/></button><div className="avatar" style={{background:active.color}}>{active.avatar}</div><div className="contact"><b>{active.name}</b><span><i className="online-dot"/> Active now · {active.handle}</span></div><div className="conversation-actions"><button className="icon-btn"><Phone size={18}/></button><button className="icon-btn"><Video size={19}/></button><button className="icon-btn"><Search size={18}/></button><button className="icon-btn"><MoreHorizontal size={19}/></button></div></header>
      <div className="conversation-body"><div className="secure-note"><LockKeyhole size={13}/> End-to-end encrypted · You control this conversation</div><div className="date-chip">Today</div>{messages.map((m,i)=><div className={`bubble-row ${i===messages.length-1?"mine":""}`} key={i}><div className="bubble">{m}<span>{i+9}:1{i}<Check size={12}/></span></div></div>)}<div className="assistant-card"><div className="ai-icon"><Sparkles size={17}/></div><div><b>OLUNAA AI</b><p>Need a quick reply, translation, summary or creative idea? I’m here.</p></div><button><WandSparkles size={16}/></button></div></div>
      <div className="composer-wrap"><div className="composer"><button className="icon-btn"><Plus size={19}/></button><input value={composer} onChange={e=>setComposer(e.target.value)} onKeyDown={e=>e.key==='Enter'&&send()} placeholder="Write a message…"/><button className="icon-btn"><Paperclip size={18}/></button><button className="icon-btn"><Camera size={18}/></button>{composer?<button className="send-btn" onClick={send}><Send size={17}/></button>:<button className="voice-btn"><Mic size={18}/></button>}</div><div className="composer-hint"><span><ShieldCheck size={12}/> Private by default</span><span>Enter to send · Shift + Enter for new line</span></div></div>
    </section>

    <aside className="right-rail"><div className="rail-card hero-card"><span className="spark"><Sparkles size={16}/></span><span className="eyebrow">BUILT FOR WHAT'S NEXT</span><h2>More than a messenger.</h2><p>Chat, follow, create, discover and connect — in one smooth space.</p><div className="feature-tags"><span>AI native</span><span>Live translation</span><span>Cloud sync</span><span>Creator tools</span></div></div><div className="rail-card"><div className="rail-title"><b>People you may know</b><button>See all</button></div>{discover.slice(0,3).map(d=><div className="suggestion" key={d.handle}><div className="avatar small" style={{background:d.color}}>{d.name[0]}</div><div><b>{d.name}</b><span>{d.handle}</span></div><button onClick={()=>setFollowed(v=>({...v,[d.handle]:!v[d.handle]}))}>{followed[d.handle]?<Check size={15}/>:<Plus size={16}/>}</button></div>)}</div><div className="rail-card stats"><div><span>Latency</span><b>&lt; 120ms</b></div><div><span>Privacy</span><b>Private</b></div><div><span>Availability</span><b>Global</b></div></div></aside>

    <button className="mobile-menu" onClick={()=>setMenuOpen(true)}><Menu size={21}/></button>
    {showNew&&<Modal title="Start something new" onClose={()=>setShowNew(false)}><div className="modal-grid"><button><MessageCircle/><b>New chat</b><span>Message anyone by @username</span></button><button><Users/><b>Create community</b><span>Bring people together</span></button><button><Radio/><b>Post a story</b><span>Share a moment</span></button><button><Headphones/><b>Start a room</b><span>Talk live with your people</span></button></div></Modal>}
    {showLogin&&<Modal title="Welcome to OLUNAA" onClose={()=>setShowLogin(false)}><div className="auth-form"><div className="auth-logo"><div className="brand-mark"><span>O</span></div><b>One identity. Your username everywhere.</b></div><label>Email or @username<input placeholder="you@example.com or @username"/></label><label>Password<input type="password" placeholder="••••••••"/></label><button className="primary">Continue <ChevronRight size={17}/></button><div className="or"><span/>or<span/></div><button className="secondary"><Sparkles size={17}/> Continue with Google</button><p>New here? <button onClick={()=>setShowLogin(false)}>Create your OLUNAA identity</button></p></div></Modal>}
  </main>
}

function Modal({title,onClose,children}:{title:string;onClose:()=>void;children:React.ReactNode}){return <div className="modal-backdrop" onMouseDown={e=>e.currentTarget===e.target&&onClose()}><div className="modal"><div className="modal-head"><div><span className="eyebrow">OLUNAA</span><h2>{title}</h2></div><button className="icon-btn" onClick={onClose}><X size={19}/></button></div>{children}</div></div>}
