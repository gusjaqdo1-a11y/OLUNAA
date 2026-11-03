"use client";

import { useEffect, useMemo, useState } from "react";
import { Check, Compass, Eye, Fingerprint, Globe2, LockKeyhole, LogIn, LogOut, Menu, MessageCircle, MoreHorizontal, Phone, Video, Mic, Paperclip, Camera, Send, Search, Settings, ShieldCheck, Sparkles, Users, Wifi, WifiOff, X } from "lucide-react";

type View = "messages" | "discover" | "stories" | "groups" | "saved";
type AuthMode = "login" | "register" | "forgot";
type SettingsState = { showOnline: boolean; lastSeen: boolean; readReceipts: boolean; typing: boolean; fingerprint: boolean };
type User = { id: string; email: string; username: string; displayName: string; settings?: { showOnlineStatus: boolean; showLastSeen: boolean; readReceipts: boolean; typingIndicators: boolean } | null };
type Conversation = { id: string; other: { id: string; username: string; displayName: string; avatarUrl?: string | null; lastActiveAt: string; online: boolean } | null; lastMessage: { body: string; senderId: string; createdAt: string; deliveredAt?: string | null; seenAt?: string | null } | null };
type ChatMessage = { id: string; senderId: string; body: string; createdAt: string; deliveredAt?: string | null; seenAt?: string | null };

const DEFAULT_SETTINGS: SettingsState = { showOnline: true, lastSeen: true, readReceipts: true, typing: true, fingerprint: false };

export default function Home() {
  const [view, setView] = useState<View>("messages");
  const [search, setSearch] = useState("");
  const [composer, setComposer] = useState("");
  const [notice, setNotice] = useState("");
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState<AuthMode | null>(null);
  const [settings, setSettings] = useState(DEFAULT_SETTINGS);
  const [mobileNav, setMobileNav] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeChat, setActiveChat] = useState<Conversation | null>(null);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [results, setResults] = useState<Array<{ id: string; username: string; displayName: string; online: boolean }>>([]);
  const [loading, setLoading] = useState(true);

  const notify = (text: string) => { setNotice(text); window.setTimeout(() => setNotice(""), 2600); };

  const loadMe = async () => {
    try {
      const r = await fetch("/api/auth/me", { cache: "no-store" });
      if (!r.ok) { setUser(null); setLoading(false); return; }
      const data = await r.json();
      setUser(data.user);
      if (data.user?.settings) setSettings({ ...DEFAULT_SETTINGS, showOnline: data.user.settings.showOnlineStatus, lastSeen: data.user.settings.showLastSeen, readReceipts: data.user.settings.readReceipts, typing: data.user.settings.typingIndicators });
    } catch { setUser(null); }
    finally { setLoading(false); }
  };

  const loadConversations = async () => {
    if (!user) return;
    const r = await fetch("/api/conversations", { cache: "no-store" });
    if (r.ok) setConversations((await r.json()).conversations ?? []);
  };

  useEffect(() => { loadMe(); }, []);
  useEffect(() => { if (user) loadConversations(); }, [user]);
  useEffect(() => {
    if (!user) return;
    const heartbeat = async () => { try { await fetch("/api/presence", { method: "POST", cache: "no-store" }); } catch {} };
    heartbeat(); const timer = window.setInterval(heartbeat, 20000); return () => window.clearInterval(timer);
  }, [user]);
  useEffect(() => {
    if (!user || !search.trim()) { setResults([]); return; }
    const timer = window.setTimeout(async () => {
      const r = await fetch(`/api/search?q=${encodeURIComponent(search.trim())}`, { cache: "no-store" });
      if (r.ok) setResults((await r.json()).users ?? []);
    }, 250);
    return () => window.clearTimeout(timer);
  }, [search, user]);
  useEffect(() => {
    if (!activeChat || !user) return;
    const load = async () => { const r = await fetch(`/api/messages?conversationId=${activeChat.id}`, { cache: "no-store" }); if (r.ok) setChatMessages((await r.json()).messages ?? []); };
    load(); const timer = window.setInterval(load, 3500); return () => window.clearInterval(timer);
  }, [activeChat, user]);

  const selectUser = async (recipientId: string) => {
    const r = await fetch("/api/conversations", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ userId: recipientId }) });
    if (!r.ok) { notify((await r.json()).error ?? "Could not open conversation"); return; }
    const id = (await r.json()).conversationId;
    await loadConversations();
    const existing = conversations.find(c => c.id === id);
    if (existing) setActiveChat(existing); else { const fresh = { id, other: results.find(x => x.id === recipientId) ? { ...results.find(x => x.id === recipientId)!, avatarUrl: null, lastActiveAt: new Date().toISOString() } : null, lastMessage: null } as Conversation; setActiveChat(fresh); }
    setSearch(""); setResults([]); setView("messages");
  };

  const send = async () => {
    const body = composer.trim(); if (!body || !activeChat || !user) return;
    const r = await fetch("/api/messages", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ conversationId: activeChat.id, body }) });
    if (!r.ok) { notify((await r.json()).error ?? "Message failed"); return; }
    const data = await r.json(); setChatMessages(items => [...items, data.message]); setComposer(""); loadConversations();
  };

  const logout = async () => { await fetch("/api/auth/logout", { method: "POST" }); setUser(null); setConversations([]); setActiveChat(null); setChatMessages([]); notify("Signed out"); };
  const updateSetting = async (key: keyof SettingsState, value: boolean) => {
    const next = { ...settings, [key]: value }; setSettings(next); localStorage.setItem("olunaa-settings", JSON.stringify(next));
    if (!user) { notify("Sign in to save account settings"); return; }
    const map: Record<string,string> = { showOnline: "showOnlineStatus", lastSeen: "showLastSeen", readReceipts: "readReceipts", typing: "typingIndicators" };
    if (map[key]) { const r = await fetch("/api/settings", { method: "PATCH", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ [map[key]]: value }) }); if (!r.ok) notify("Could not save setting"); else notify("Setting saved"); }
    else notify(value ? "Biometric preference enabled" : "Biometric preference disabled");
  };

  const title = useMemo(() => ({ messages: "Messages", discover: "Discover", stories: "Stories", groups: "Communities", saved: "Saved" }[view]), [view]);

  return <main className="olunaa">
    <aside className={`sidebar ${mobileNav ? "open" : ""}`}>
      <div className="brand"><div className="logo">O</div><strong>OLUNAA</strong><button className="close-mobile" onClick={() => setMobileNav(false)}><X size={18}/></button></div>
      <div className="account-card"><div className="avatar">{user ? user.displayName[0]?.toUpperCase() : "?"}</div><div><b>{user ? user.displayName : "Not signed in"}</b><span>{user ? `@${user.username}` : "Sign in to start"}</span></div></div>
      <nav>
        <Nav active={view === "messages"} icon={<MessageCircle/>} label="Messages" onClick={() => {setView("messages");setMobileNav(false)}}/>
        <Nav active={view === "discover"} icon={<Compass/>} label="Discover" onClick={() => {setView("discover");setMobileNav(false)}}/>
        <Nav active={view === "stories"} icon={<Eye/>} label="Stories" onClick={() => {setView("stories");setMobileNav(false)}}/>
        <Nav active={view === "groups"} icon={<Users/>} label="Communities" onClick={() => {setView("groups");setMobileNav(false)}}/>
        <Nav active={view === "saved"} icon={<LockKeyhole/>} label="Saved" onClick={() => {setView("saved");setMobileNav(false)}}/>
      </nav>
      <div className="security"><div><ShieldCheck/> Privacy first</div><div><Wifi/> Real online status</div><div><Globe2/> Global design</div></div>
      <div className="sidebar-bottom"><button onClick={() => setSettingsOpen(true)}><Settings/> Settings</button>{user ? <button onClick={logout}><LogOut/> Sign out</button> : <button onClick={() => setAuthOpen("login")}><LogIn/> Sign in</button>}</div>
    </aside>

    <section className="main-column">
      <header className="topbar"><button className="mobile-menu" onClick={() => setMobileNav(true)}><Menu/></button><div><span>OLUNAA</span><h1>{title}</h1></div><div className="top-actions">{!user && <button onClick={() => setAuthOpen("register")} className="primary">Create account</button>}<button onClick={() => setSettingsOpen(true)} className="icon"><Settings/></button></div></header>
      {(view === "messages" || view === "discover" || view === "groups") && <div className="searchbar"><Search size={17}/><input value={search} onChange={e => setSearch(e.target.value)} placeholder={user ? "Search @username or people…" : "Sign in to search people…"}/><kbd>/</kbd></div>}
      {user && search && results.length > 0 && <div className="results">{results.map(r => <button key={r.id} onClick={() => selectUser(r.id)}><span className="result-avatar">{r.displayName[0]?.toUpperCase()}</span><span><b>{r.displayName}</b><small>@{r.username} {r.online && <i>● Active</i>}</small></span></button>)}</div>}

      {view === "messages" && <div className="conversation-list">{loading ? <Empty title="Loading your account…" text=""/> : !user ? <Empty icon={<MessageCircle/>} title="No conversations yet" text="Sign in or create an account to use real conversations." action={() => setAuthOpen("login")} actionText="Sign in"/> : conversations.length === 0 ? <Empty icon={<MessageCircle/>} title="No conversations yet" text="Search an @username above to start a real conversation."/> : conversations.map(c => c.other && <button className="conversation-item" key={c.id} onClick={() => setActiveChat(c)}><span className="result-avatar">{c.other.displayName[0]?.toUpperCase()}</span><span><b>{c.other.displayName}</b><small>@{c.other.username} {c.other.online && <i>● Active</i>}</small>{c.lastMessage && <em>{c.lastMessage.body}</em>}</span><span className="check">{c.lastMessage?.senderId === user.id ? (c.lastMessage.seenAt ? <><Check/><Check/></> : <Check/>) : ""}</span></button>)}</div>}
      {view === "discover" && <Empty icon={<Compass/>} title="Discover real people" text={user ? "Search for a username or display name above." : "Sign in to search real accounts."} action={!user ? () => setAuthOpen("login") : undefined} actionText="Sign in"/>}
      {view === "stories" && <Empty icon={<Eye/>} title="Stories" text="Real stories from accounts you follow will appear here."/>}
      {view === "groups" && <Empty icon={<Users/>} title="Communities" text="Real communities will appear here when you join or create them."/>}
      {view === "saved" && <Empty icon={<LockKeyhole/>} title="Saved" text="Saved messages and files are private to your account."/>}
    </section>

    <section className="conversation">
      {!activeChat || !user ? <div className="conversation-empty"><div className="empty-orb"><Sparkles/></div><h2>{user ? "Your conversations belong here." : "Welcome to OLUNAA."}</h2><p>{user ? "Choose a real conversation from the list or search an @username to start one." : "No demo users, fake messages or fake online states. Sign in to use the real app."}</p>{!user && <button className="primary big" onClick={() => setAuthOpen("login")}>Sign in</button>}</div> : <>
        <header className="conversation-head"><div className="avatar">{activeChat.other?.displayName[0]?.toUpperCase()}</div><div><b>{activeChat.other?.displayName}</b><span>{settings.showOnline && activeChat.other?.online ? <><i className="online-dot"/> Active now</> : settings.lastSeen ? "Last seen recently" : "Status hidden"}</span></div><div className="call-actions"><button onClick={() => notify("Voice calls are next on the realtime service layer")}><Phone/></button><button onClick={() => notify("Video calls are next on the realtime service layer")}><Video/></button><button onClick={() => notify("Conversation options")}><MoreHorizontal/></button></div></header>
        <div className="conversation-body"><div className="secure-note"><LockKeyhole size={13}/> Private conversation</div>{chatMessages.map(m => <div className={`message-row ${m.senderId === user.id ? "mine" : ""}`} key={m.id}><div className="bubble">{m.body}<small>{m.senderId === user.id && (m.seenAt ? <><Check/><Check/></> : m.deliveredAt ? <><Check/><Check/></> : <Check/>)}</small></div></div>)}{chatMessages.length === 0 && <div className="empty-chat">No messages yet. Say hello.</div>}</div>
        <div className="composer"><button onClick={() => notify("Media attachments are not enabled yet")}><Paperclip/></button><input value={composer} onChange={e => setComposer(e.target.value)} onKeyDown={e => e.key === "Enter" && send()} placeholder="Write a message…"/><button onClick={() => notify("Camera is not enabled yet")}><Camera/></button>{composer ? <button className="send" onClick={send}><Send/></button> : <button onClick={() => notify("Voice messages are not enabled yet")}><Mic/></button>}</div>
      </>}
    </section>

    {settingsOpen && <SettingsPanel settings={settings} update={updateSetting} close={() => setSettingsOpen(false)} user={!!user}/>} 
    {authOpen && <AuthModal mode={authOpen} switchMode={() => setAuthOpen(authOpen === "login" ? "register" : "login")} close={() => setAuthOpen(null)} notify={notify} onSuccess={async () => { setAuthOpen(null); await loadMe(); }}/>} 
    {notice && <div className="toast"><Check size={15}/>{notice}</div>}

    <style jsx global>{`
      *{box-sizing:border-box}body{margin:0;background:#07080d;color:#f7f7fb;font-family:Inter,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}button,input{font:inherit}.olunaa{min-height:100vh;display:grid;grid-template-columns:250px minmax(300px,430px) minmax(0,1fr);background:radial-gradient(circle at 25% 0%,rgba(103,68,240,.12),transparent 32%),#07080d}.sidebar{border-right:1px solid #171a25;padding:18px 13px;display:flex;flex-direction:column;min-height:100vh;background:rgba(9,10,16,.82);backdrop-filter:blur(18px)}.brand{display:flex;align-items:center;gap:10px;padding:7px 8px 19px;font-size:16px;letter-spacing:.08em}.logo{width:34px;height:34px;border-radius:11px;display:grid;place-items:center;background:linear-gradient(135deg,#8b5cf6,#22d3ee);font-weight:900;box-shadow:0 8px 30px rgba(139,92,246,.3)}.close-mobile{display:none}.account-card{display:flex;align-items:center;gap:10px;border:1px solid #1b1f2c;background:#0d0f16;border-radius:15px;padding:10px;margin-bottom:15px}.avatar,.result-avatar{width:39px;height:39px;border-radius:13px;background:linear-gradient(135deg,#252a3a,#161925);display:grid;place-items:center;font-weight:800;flex:none}.account-card b,.account-card span{display:block}.account-card span{font-size:11px;color:#73798b;margin-top:2px}.sidebar nav{display:grid;gap:4px}.nav{border:0;background:transparent;color:#858b9d;display:flex;align-items:center;gap:11px;padding:11px 12px;border-radius:12px;cursor:pointer;text-align:left}.nav svg{width:18px}.nav.active{color:white;background:linear-gradient(90deg,#21163c,#13131d)}.security{margin-top:20px;padding:13px;border:1px solid #171b28;border-radius:15px;color:#707789;font-size:10px;display:grid;gap:10px}.security div{display:flex;align-items:center;gap:8px}.security svg{width:14px;color:#8b5cf6}.sidebar-bottom{margin-top:auto;display:grid;gap:5px}.sidebar-bottom button{border:0;background:transparent;color:#777d8e;padding:11px 12px;display:flex;align-items:center;gap:10px;border-radius:11px;cursor:pointer}.sidebar-bottom button:hover{background:#10121a;color:white}.main-column{border-right:1px solid #171a25;min-width:0;background:#090a10;position:relative}.topbar{height:77px;padding:14px 16px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #171a25}.topbar span{font-size:8px;color:#8b5cf6;font-weight:800;letter-spacing:.18em}.topbar h1{font-size:21px;margin:3px 0 0}.top-actions{display:flex;gap:6px;align-items:center}.primary{border:0;border-radius:11px;padding:10px 14px;background:linear-gradient(135deg,#8b5cf6,#5b45df);color:#fff;font-weight:700;cursor:pointer}.icon,.mobile-menu{border:1px solid #1b1e2a;background:#0f1119;color:#aab0c0;width:38px;height:38px;border-radius:11px;display:grid;place-items:center;cursor:pointer}.searchbar{margin:12px;border:1px solid #1b1e2a;background:#0d0f16;border-radius:13px;display:flex;align-items:center;gap:9px;padding:0 12px;color:#6f7587}.searchbar input{width:100%;border:0;outline:0;background:transparent;color:#fff;padding:11px 0;font-size:12px}.searchbar kbd{font-size:10px;border:1px solid #292d3b;border-radius:6px;padding:2px 5px}.results{position:absolute;left:12px;right:12px;top:130px;z-index:30;border:1px solid #242837;background:#0c0e15;border-radius:15px;box-shadow:0 25px 80px #000;overflow:hidden}.results button,.conversation-item{width:100%;border:0;border-bottom:1px solid #181b26;background:transparent;color:white;text-align:left;display:flex;align-items:center;gap:10px;padding:11px;cursor:pointer}.results button:hover,.conversation-item:hover{background:#12141d}.results button:last-child,.conversation-item:last-child{border-bottom:0}.results b,.results small,.conversation-item b,.conversation-item small,.conversation-item em{display:block}.results small,.conversation-item small{color:#6f7586;font-size:9px;margin-top:2px}.results i,.conversation-item i{color:#22c55e;font-style:normal}.conversation-list{padding:0 12px}.conversation-item{padding:13px 8px}.conversation-item>span:nth-child(2){min-width:0;flex:1}.conversation-item em{font-size:10px;color:#777d8d;font-style:normal;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:280px;margin-top:3px}.check{color:#a78bfa;display:flex}.check svg{width:11px}.empty-section{padding:50px 22px;text-align:center;color:#747a8b}.empty-icon{width:58px;height:58px;margin:0 auto 14px;border-radius:18px;display:grid;place-items:center;background:#11131d;color:#8b5cf6}.empty-section h2{color:#f2f3f7;font-size:18px;margin:0 0 8px}.empty-section p{font-size:11px;line-height:1.7;max-width:320px;margin:0 auto 14px}.conversation{min-width:0;display:flex;flex-direction:column;min-height:100vh;background:radial-gradient(circle at 50% 0%,rgba(95,65,180,.08),transparent 40%),#08090e}.conversation-empty{margin:auto;text-align:center;max-width:460px;padding:30px}.empty-orb{width:72px;height:72px;margin:auto;border-radius:24px;display:grid;place-items:center;background:linear-gradient(135deg,#21163d,#101d2c);color:#a78bfa}.conversation-empty h2{font-size:24px;margin:17px 0 8px}.conversation-empty p{font-size:12px;color:#73798a;line-height:1.7}.big{margin-top:10px;padding:12px 18px}.conversation-head{height:73px;padding:12px 18px;border-bottom:1px solid #171a25;display:flex;align-items:center;gap:10px}.conversation-head>div:nth-child(2){display:grid}.conversation-head span{font-size:10px;color:#717789;margin-top:3px}.online-dot{display:inline-block;width:7px;height:7px;border-radius:50%;background:#22c55e;margin-right:5px}.call-actions{margin-left:auto;display:flex;gap:4px}.call-actions button,.composer button{border:0;background:transparent;color:#8e94a5;width:36px;height:36px;display:grid;place-items:center;border-radius:10px;cursor:pointer}.call-actions button:hover,.composer button:hover{background:#11131b;color:white}.conversation-body{flex:1;padding:18px;overflow:auto}.secure-note{font-size:9px;color:#707789;text-align:center;margin-bottom:30px;display:flex;justify-content:center;align-items:center;gap:5px}.message-row{display:flex;margin:8px 0}.message-row.mine{justify-content:flex-end}.bubble{max-width:min(70%,420px);padding:10px 12px;border-radius:15px;background:#151822;font-size:12px;line-height:1.5}.message-row.mine .bubble{background:linear-gradient(135deg,#7449ee,#4d3cc8)}.bubble small{display:flex;justify-content:flex-end;gap:1px;color:#c4bbff;margin-top:3px}.bubble small svg{width:10px}.empty-chat{text-align:center;color:#5f6575;font-size:11px;margin-top:25vh}.composer{margin:14px;border:1px solid #1d202c;background:#0e1017;border-radius:16px;display:flex;align-items:center;padding:4px 6px}.composer input{flex:1;border:0;outline:0;background:transparent;color:#fff;padding:10px;font-size:12px}.composer .send{background:#7449ee;color:#fff}.toast{position:fixed;z-index:100;bottom:20px;left:50%;transform:translateX(-50%);padding:10px 14px;border:1px solid #292d3b;background:#11131b;border-radius:12px;display:flex;gap:7px;align-items:center;font-size:10px;box-shadow:0 20px 70px #000}.overlay{position:fixed;inset:0;z-index:90;background:rgba(0,0,0,.68);backdrop-filter:blur(10px);display:grid;place-items:center;padding:18px}.panel{width:min(500px,100%);max-height:92vh;overflow:auto;background:#0c0e15;border:1px solid #202431;border-radius:22px;box-shadow:0 30px 100px #000;padding:20px}.panel-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}.panel-head h2{font-size:18px;margin:3px 0 0}.panel-head button{border:0;background:#141720;color:#8d93a5;width:32px;height:32px;border-radius:9px}.setting{display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid #181b26}.setting:last-child{border-bottom:0}.setting svg{width:18px;color:#8b5cf6}.setting div{flex:1}.setting b{display:block;font-size:12px}.setting span{display:block;font-size:9px;color:#6f7586;margin-top:3px;line-height:1.5}.switch{width:39px;height:23px;border:0;border-radius:20px;background:#252938;padding:3px;cursor:pointer}.switch i{display:block;width:17px;height:17px;border-radius:50%;background:#777d8c;transition:.2s}.switch.on{background:#6844df}.switch.on i{transform:translateX(16px);background:#fff}.auth label{display:block;font-size:10px;color:#a4a9b8;margin:9px 0 5px}.auth input{width:100%;padding:12px;border:1px solid #202432;background:#10121a;border-radius:11px;color:#fff;outline:0}.auth input:focus{border-color:#6844df}.auth small{color:#6f7586;font-size:9px;line-height:1.5;display:block;margin-top:10px}.auth .primary{width:100%;margin-top:10px}.auth .switch-auth,.forgot-link{border:0;background:transparent;color:#a78bfa;font-size:10px;margin-top:12px;cursor:pointer}.error{color:#fb7185;font-size:10px;margin-top:9px}.success{color:#4ade80;font-size:10px;margin-top:9px}.status-note{display:flex;gap:8px;align-items:center;padding:11px;border-radius:12px;background:#11131c;color:#73798a;font-size:9px;margin-top:12px}.status-note svg{color:#22c55e}
      @media(max-width:1100px){.olunaa{grid-template-columns:220px minmax(300px,430px) minmax(0,1fr)}.sidebar{padding:14px 9px}.security{display:none}}
      @media(max-width:820px){.olunaa{display:block}.sidebar{position:fixed;z-index:80;inset:0 auto 0 0;width:min(290px,88vw);transform:translateX(-105%);transition:.22s;box-shadow:30px 0 80px rgba(0,0,0,.5)}.sidebar.open{transform:translateX(0)}.close-mobile{display:grid;margin-left:auto;border:0;background:#151722;color:#aaa;width:32px;height:32px;border-radius:9px;place-items:center}.main-column{min-height:100vh;border:0}.conversation{display:none}.topbar{padding-left:10px}.mobile-menu{display:grid;margin-right:8px}.topbar>div:first-of-type{margin-right:auto}.top-actions .primary{display:none}.searchbar{margin:10px}.results{top:124px}.conversation-empty{min-height:70vh;display:grid;place-items:center;align-content:center}.panel{padding:17px}.empty-section{padding-top:42px}}
      @media(min-width:821px){.mobile-menu{display:none}}
      @media(min-width:1500px){.olunaa{grid-template-columns:270px minmax(400px,520px) minmax(500px,1fr)}.conversation-empty h2{font-size:28px}}
    `}</style>
  </main>;
}

function Nav({ active, icon, label, onClick }: { active: boolean; icon: React.ReactNode; label: string; onClick: () => void }) { return <button className={`nav ${active ? "active" : ""}`} onClick={onClick}>{icon}<span>{label}</span></button>; }
function Empty({ icon = <WifiOff/>, title, text, action, actionText }: { icon?: React.ReactNode; title: string; text: string; action?: () => void; actionText?: string }) { return <div className="empty-section"><div className="empty-icon">{icon}</div><h2>{title}</h2>{text && <p>{text}</p>}{action && <button className="primary" onClick={action}>{actionText}</button>}</div>; }
function Setting({icon,title,text,value,onChange}:{icon:React.ReactNode;title:string;text:string;value:boolean;onChange:(v:boolean)=>void}){return <div className="setting">{icon}<div><b>{title}</b><span>{text}</span></div><button className={`switch ${value?"on":""}`} onClick={()=>onChange(!value)} aria-label={title}><i/></button></div>}
function SettingsPanel({ settings, update, close, user }: { settings: SettingsState; update: (k:keyof SettingsState,v:boolean)=>void; close:()=>void; user:boolean }) { return <div className="overlay" onMouseDown={close}><div className="panel" onMouseDown={e=>e.stopPropagation()}><div className="panel-head"><h2>OLUNAA Settings</h2><button onClick={close}><X size={17}/></button></div><Setting icon={<Wifi/>} title="Show online status" text="Let people see when you are active." value={settings.showOnline} onChange={v=>update("showOnline",v)}/><Setting icon={<Eye/>} title="Last seen" text="Control whether your last active time is visible." value={settings.lastSeen} onChange={v=>update("lastSeen",v)}/><Setting icon={<Check/>} title="Read receipts" text="Show when your messages have been read." value={settings.readReceipts} onChange={v=>update("readReceipts",v)}/><Setting icon={<MessageCircle/>} title="Typing indicators" text="Show when you are typing." value={settings.typing} onChange={v=>update("typing",v)}/><Setting icon={<Fingerprint/>} title="Biometric app lock" text="Uses the Android system biometric prompt in the native app." value={settings.fingerprint} onChange={v=>update("fingerprint",v)}/><div className="status-note"><ShieldCheck size={14}/>{user ? "Account settings are saved to your OLUNAA profile." : "Sign in to save account settings across devices."}</div></div></div>; }

function AuthModal({ mode, switchMode, close, notify, onSuccess }: { mode: AuthMode; switchMode:()=>void; close:()=>void; notify:(s:string)=>void; onSuccess:()=>Promise<void> }) {
  const [busy,setBusy]=useState(false); const [error,setError]=useState(""); const [success,setSuccess]=useState("");
  const submit = async (e:React.FormEvent<HTMLFormElement>) => { e.preventDefault(); setError(""); setSuccess(""); setBusy(true); const form=new FormData(e.currentTarget); let endpoint=""; let body:any={};
    if(mode==="register"){ endpoint="/api/auth/register"; body={displayName:String(form.get("displayName")||""),email:String(form.get("email")||""),username:String(form.get("username")||""),password:String(form.get("password")||"")}; }
    else if(mode==="login"){ endpoint="/api/auth/login"; body={identifier:String(form.get("identifier")||""),password:String(form.get("password")||"")}; }
    else { endpoint="/api/auth/forgot"; body={identifier:String(form.get("identifier")||"")}; }
    try { const r=await fetch(endpoint,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)}); const data=await r.json(); if(!r.ok) throw new Error(data.error||"Request failed"); if(mode==="forgot"){setSuccess(data.message); notify("If the account exists, a reset email was requested");} else { notify(mode==="login"?"Signed in":"Account created"); await onSuccess(); } } catch(err:any){setError(err.message||"Something went wrong");} finally{setBusy(false);} };
  return <div className="overlay" onMouseDown={close}><div className="panel auth" onMouseDown={e=>e.stopPropagation()}><div className="panel-head"><div><span style={{fontSize:8,color:"#8b5cf6",letterSpacing:".15em"}}>WELCOME TO OLUNAA</span><h2>{mode==="register"?"Create your account":mode==="forgot"?"Forgot password":"Sign in"}</h2></div><button onClick={close}><X size={17}/></button></div>
    <form onSubmit={submit}>{mode==="register" && <><label>Display name</label><input name="displayName" required autoComplete="name" placeholder="Your name"/><label>Email address</label><input name="email" type="email" required autoComplete="email" placeholder="you@example.com"/><label>Username</label><input name="username" required autoComplete="username" placeholder="username" pattern="[A-Za-z0-9_]{3,24}"/><label>Password</label><input name="password" type="password" required minLength={8} autoComplete="new-password" placeholder="At least 8 characters"/><small>Your username becomes your unique @username. Email and username are both required only when creating the account.</small></>}
    {mode==="login" && <><label>Email or @username</label><input name="identifier" required autoComplete="username" placeholder="email@example.com or @username"/><label>Password</label><input name="password" type="password" required autoComplete="current-password" placeholder="Your password"/><button type="button" className="forgot-link" onClick={()=>{setError("");setSuccess("");(document.activeElement as HTMLElement)?.blur();window.setTimeout(()=>{const el=document.querySelector('[data-forgot]') as HTMLElement|null;el?.click();},0)}}>Forgot password?</button></>}
    {mode==="forgot" && <><label>Email or @username</label><input name="identifier" required autoComplete="username" placeholder="email@example.com or @username"/><small>Enter either the email address or @username on your account. The reset link will be sent to the account's email.</small></>}
    {error && <div className="error">{error}</div>}{success && <div className="success">{success}</div>}<button className="primary" type="submit" disabled={busy}>{busy?"Please wait…":mode==="register"?"Create OLUNAA account":mode==="forgot"?"Send reset link":"Sign in"}</button></form>
    {mode==="login" && <button data-forgot className="switch-auth" style={{display:"none"}} onClick={()=>{}}>hidden</button>}
    {mode==="login" ? <button className="switch-auth" onClick={()=>{const el=document.querySelector('[data-forgot]') as HTMLButtonElement|null; if(el){el.setAttribute("data-active","1");} close(); window.setTimeout(()=>notify("Open Sign in again to use Forgot password"),0)}}>New to OLUNAA? Create account</button> : mode==="register" ? <button className="switch-auth" onClick={switchMode}>Already have an account? Sign in</button> : <button className="switch-auth" onClick={switchMode}>Back to sign in</button>}
    {mode==="login" && <button className="forgot-link" style={{display:"block",width:"100%"}} onClick={()=>{close();window.setTimeout(()=>notify("Forgot password is available from the sign-in form"),0)}}>Forgot password recovery</button>}
  </div></div>;
}
