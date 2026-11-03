"use client";

import { useEffect, useMemo, useState } from "react";
import { Bell, Check, Compass, Eye, Fingerprint, Globe2, LockKeyhole, LogOut, Menu, MessageCircle, MoreHorizontal, Plus, Search, Settings, ShieldCheck, Sparkles, Users, Video, Phone, Mic, Paperclip, Camera, Send, X, UserRound, Wifi, WifiOff } from "lucide-react";

type View = "messages" | "discover" | "stories" | "groups" | "saved";
type SettingsState = { showOnline: boolean; lastSeen: boolean; readReceipts: boolean; typing: boolean; fingerprint: boolean };

const DEFAULT_SETTINGS: SettingsState = { showOnline: true, lastSeen: true, readReceipts: true, typing: true, fingerprint: false };

export default function Home() {
  const [view, setView] = useState<View>("messages");
  const [search, setSearch] = useState("");
  const [composer, setComposer] = useState("");
  const [messages, setMessages] = useState<string[]>([]);
  const [notice, setNotice] = useState("");
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [authOpen, setAuthOpen] = useState<"login" | "register" | null>(null);
  const [settings, setSettings] = useState<SettingsState>(DEFAULT_SETTINGS);
  const [mobileNav, setMobileNav] = useState(false);
  const [activeChat, setActiveChat] = useState<string | null>(null);

  useEffect(() => {
    try {
      const raw = localStorage.getItem("olunaa-settings");
      if (raw) setSettings({ ...DEFAULT_SETTINGS, ...JSON.parse(raw) });
    } catch {}
  }, []);

  const updateSetting = (key: keyof SettingsState, value: boolean) => {
    const next = { ...settings, [key]: value };
    setSettings(next);
    localStorage.setItem("olunaa-settings", JSON.stringify(next));
    notify("Setting saved on this device");
  };

  const notify = (text: string) => {
    setNotice(text);
    window.setTimeout(() => setNotice(""), 2200);
  };

  const send = () => {
    const value = composer.trim();
    if (!value) return;
    setMessages((items) => [...items, value]);
    setComposer("");
  };

  const title = useMemo(() => ({ messages: "Messages", discover: "Discover", stories: "Stories", groups: "Communities", saved: "Saved" }[view]), [view]);

  return (
    <main className="olunaa">
      <aside className={`sidebar ${mobileNav ? "open" : ""}`}>
        <div className="brand"><div className="logo">O</div><strong>OLUNAA</strong><button className="close-mobile" onClick={() => setMobileNav(false)}><X size={18}/></button></div>
        <div className="account-card"><div className="avatar">?</div><div><b>Not signed in</b><span>Sign in to start</span></div></div>
        <nav>
          <Nav active={view === "messages"} icon={<MessageCircle/>} label="Messages" onClick={() => {setView("messages");setMobileNav(false)}}/>
          <Nav active={view === "discover"} icon={<Compass/>} label="Discover" onClick={() => {setView("discover");setMobileNav(false)}}/>
          <Nav active={view === "stories"} icon={<Eye/>} label="Stories" onClick={() => {setView("stories");setMobileNav(false)}}/>
          <Nav active={view === "groups"} icon={<Users/>} label="Communities" onClick={() => {setView("groups");setMobileNav(false)}}/>
          <Nav active={view === "saved"} icon={<LockKeyhole/>} label="Saved" onClick={() => {setView("saved");setMobileNav(false)}}/>
        </nav>
        <div className="security"><div><ShieldCheck/> Privacy first</div><div><Wifi/> Realtime-ready</div><div><Globe2/> Global design</div></div>
        <div className="sidebar-bottom"><button onClick={() => setSettingsOpen(true)}><Settings/> Settings</button><button onClick={() => setAuthOpen("login")}><LogOut/> Sign in</button></div>
      </aside>

      <section className="main-column">
        <header className="topbar"><button className="mobile-menu" onClick={() => setMobileNav(true)}><Menu/></button><div><span>OLUNAA</span><h1>{title}</h1></div><div className="top-actions"><button onClick={() => setAuthOpen("register")} className="primary">Create account</button><button onClick={() => setSettingsOpen(true)} className="icon"><Settings/></button></div></header>
        {(view === "messages" || view === "discover" || view === "groups") && <div className="searchbar"><Search size={17}/><input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search @username, people, groups…"/><kbd>/</kbd></div>}

        {view === "messages" && <EmptyMessages search={search} onCreate={() => setAuthOpen("register")} />}
        {view === "discover" && <EmptySection icon={<Compass/>} title="Discover real people" text="Search will show verified accounts once you sign in and the live database is connected." />}
        {view === "stories" && <EmptySection icon={<Eye/>} title="Stories" text="Your stories and stories from accounts you follow will appear here." />}
        {view === "groups" && <EmptySection icon={<Users/>} title="Communities" text="Create or join communities after you sign in." />}
        {view === "saved" && <EmptySection icon={<LockKeyhole/>} title="Saved" text="Saved messages and files are private to your account." />}
      </section>

      <section className="conversation">
        {!activeChat ? <div className="conversation-empty"><div className="empty-orb"><Sparkles/></div><h2>Your conversations belong here.</h2><p>No demo users, fake online indicators or fake messages are shown. Sign in and connect a real account to begin.</p><button className="primary big" onClick={() => setAuthOpen("register")}>Create your OLUNAA account</button></div> : <>
          <header className="conversation-head"><div className="avatar">{activeChat[0]}</div><div><b>{activeChat}</b><span>{settings.showOnline ? "Online status available" : "Online status hidden"}</span></div><div className="call-actions"><button onClick={() => notify("Voice call requires a signed-in account")}><Phone/></button><button onClick={() => notify("Video call requires a signed-in account")}><Video/></button><button onClick={() => notify("Conversation options")}><MoreHorizontal/></button></div></header>
          <div className="conversation-body"><div className="secure-note"><LockKeyhole size={13}/> Messages are protected by account privacy settings.</div>{messages.map((m, i) => <div className="message-row mine" key={`${m}-${i}`}><div className="bubble">{m}<small>{settings.readReceipts ? <><Check/><Check/></> : <Check/>}</small></div></div>)}{messages.length === 0 && <div className="empty-chat">No messages yet.</div>}</div>
          <div className="composer"><button onClick={() => notify("Attachments require a signed-in account")}><Paperclip/></button><input value={composer} onChange={e => setComposer(e.target.value)} onKeyDown={e => e.key === "Enter" && send()} placeholder="Write a message…"/><button onClick={() => notify("Camera requires a signed-in account")}><Camera/></button>{composer ? <button className="send" onClick={send}><Send/></button> : <button onClick={() => notify("Voice messages require a signed-in account")}><Mic/></button>}</div>
        </>}
      </section>

      {settingsOpen && <SettingsPanel settings={settings} update={updateSetting} close={() => setSettingsOpen(false)} notify={notify}/>} 
      {authOpen && <AuthModal mode={authOpen} switchMode={() => setAuthOpen(authOpen === "login" ? "register" : "login")} close={() => setAuthOpen(null)} notify={notify}/>} 
      {notice && <div className="toast"><Check size={15}/>{notice}</div>}

      <style jsx global>{`
        *{box-sizing:border-box}body{margin:0;background:#07080d;color:#f7f7fb;font-family:Inter,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif}button,input{font:inherit}.olunaa{min-height:100vh;display:grid;grid-template-columns:250px minmax(300px,430px) minmax(0,1fr);background:radial-gradient(circle at 25% 0%,rgba(103,68,240,.12),transparent 32%),#07080d}.sidebar{border-right:1px solid #171a25;padding:18px 13px;display:flex;flex-direction:column;min-height:100vh;background:rgba(9,10,16,.82);backdrop-filter:blur(18px)}.brand{display:flex;align-items:center;gap:10px;padding:7px 8px 19px;font-size:16px;letter-spacing:.08em}.logo{width:34px;height:34px;border-radius:11px;display:grid;place-items:center;background:linear-gradient(135deg,#8b5cf6,#22d3ee);font-weight:900;box-shadow:0 8px 30px rgba(139,92,246,.3)}.close-mobile{display:none}.account-card{display:flex;align-items:center;gap:10px;border:1px solid #1b1f2c;background:#0d0f16;border-radius:15px;padding:10px;margin-bottom:15px}.avatar{width:39px;height:39px;border-radius:13px;background:linear-gradient(135deg,#252a3a,#161925);display:grid;place-items:center;font-weight:800}.account-card b,.account-card span{display:block}.account-card span{font-size:11px;color:#73798b;margin-top:2px}.sidebar nav{display:grid;gap:4px}.nav{border:0;background:transparent;color:#858b9d;display:flex;align-items:center;gap:11px;padding:11px 12px;border-radius:12px;cursor:pointer;text-align:left}.nav svg{width:18px}.nav.active{color:white;background:linear-gradient(90deg,#21163c,#13131d)}.security{margin-top:20px;padding:13px;border:1px solid #171b28;border-radius:15px;color:#707789;font-size:10px;display:grid;gap:10px}.security div{display:flex;align-items:center;gap:8px}.security svg{width:14px;color:#8b5cf6}.sidebar-bottom{margin-top:auto;display:grid;gap:5px}.sidebar-bottom button{border:0;background:transparent;color:#777d8e;padding:11px 12px;display:flex;align-items:center;gap:10px;border-radius:11px;cursor:pointer}.sidebar-bottom button:hover{background:#10121a;color:white}.main-column{border-right:1px solid #171a25;min-width:0;background:#090a10}.topbar{height:77px;padding:14px 16px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #171a25}.topbar span{font-size:8px;color:#8b5cf6;font-weight:800;letter-spacing:.18em}.topbar h1{font-size:21px;margin:3px 0 0}.top-actions{display:flex;gap:6px;align-items:center}.primary{border:0;border-radius:11px;padding:10px 14px;background:linear-gradient(135deg,#8b5cf6,#5b45df);color:#fff;font-weight:700;cursor:pointer}.icon,.mobile-menu{border:1px solid #1b1e2a;background:#0f1119;color:#aab0c0;width:38px;height:38px;border-radius:11px;display:grid;place-items:center;cursor:pointer}.searchbar{margin:12px;border:1px solid #1b1e2a;background:#0d0f16;border-radius:13px;display:flex;align-items:center;gap:9px;padding:0 12px;color:#6f7587}.searchbar input{width:100%;border:0;outline:0;background:transparent;color:#fff;padding:11px 0;font-size:12px}.searchbar kbd{font-size:10px;border:1px solid #292d3b;border-radius:6px;padding:2px 5px}.empty-section{padding:50px 22px;text-align:center;color:#747a8b}.empty-icon{width:58px;height:58px;margin:0 auto 14px;border-radius:18px;display:grid;place-items:center;background:#11131d;color:#8b5cf6}.empty-section h2{color:#f2f3f7;font-size:18px;margin:0 0 8px}.empty-section p{font-size:11px;line-height:1.7;max-width:320px;margin:auto}.conversation{min-width:0;display:flex;flex-direction:column;min-height:100vh;background:radial-gradient(circle at 50% 0%,rgba(95,65,180,.08),transparent 40%),#08090e}.conversation-empty{margin:auto;text-align:center;max-width:460px;padding:30px}.empty-orb{width:72px;height:72px;margin:auto;border-radius:24px;display:grid;place-items:center;background:linear-gradient(135deg,#21163d,#101d2c);color:#a78bfa}.conversation-empty h2{font-size:24px;margin:17px 0 8px}.conversation-empty p{font-size:12px;color:#73798a;line-height:1.7}.big{margin-top:10px;padding:12px 18px}.conversation-head{height:73px;padding:12px 18px;border-bottom:1px solid #171a25;display:flex;align-items:center;gap:10px}.conversation-head>div:nth-child(2){display:grid}.conversation-head span{font-size:10px;color:#717789;margin-top:3px}.call-actions{margin-left:auto;display:flex;gap:4px}.call-actions button,.composer button{border:0;background:transparent;color:#8e94a5;width:36px;height:36px;display:grid;place-items:center;border-radius:10px;cursor:pointer}.call-actions button:hover,.composer button:hover{background:#11131b;color:white}.conversation-body{flex:1;padding:18px;overflow:auto}.secure-note{font-size:9px;color:#707789;text-align:center;margin-bottom:30px;display:flex;justify-content:center;align-items:center;gap:5px}.message-row{display:flex;margin:8px 0}.message-row.mine{justify-content:flex-end}.bubble{max-width:min(70%,420px);padding:10px 12px;border-radius:15px;background:#151822;font-size:12px;line-height:1.5}.message-row.mine .bubble{background:linear-gradient(135deg,#7449ee,#4d3cc8)}.bubble small{display:flex;justify-content:flex-end;gap:-2px;color:#c4bbff;margin-top:3px}.bubble small svg{width:10px}.empty-chat{text-align:center;color:#5f6575;font-size:11px;margin-top:25vh}.composer{margin:14px;border:1px solid #1d202c;background:#0e1017;border-radius:16px;display:flex;align-items:center;padding:4px 6px}.composer input{flex:1;border:0;outline:0;background:transparent;color:#fff;padding:10px;font-size:12px}.composer .send{background:#7449ee;color:#fff}.toast{position:fixed;z-index:100;bottom:20px;left:50%;transform:translateX(-50%);padding:10px 14px;border:1px solid #292d3b;background:#11131b;border-radius:12px;display:flex;gap:7px;align-items:center;font-size:10px;box-shadow:0 20px 70px #000}.overlay{position:fixed;inset:0;z-index:90;background:rgba(0,0,0,.68);backdrop-filter:blur(10px);display:grid;place-items:center;padding:18px}.panel{width:min(500px,100%);background:#0c0e15;border:1px solid #202431;border-radius:22px;box-shadow:0 30px 100px #000;padding:20px}.panel-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px}.panel-head h2{font-size:18px;margin:0}.panel-head button{border:0;background:#141720;color:#8d93a5;width:32px;height:32px;border-radius:9px}.setting{display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid #181b26}.setting:last-child{border-bottom:0}.setting svg{width:18px;color:#8b5cf6}.setting div{flex:1}.setting b{display:block;font-size:12px}.setting span{display:block;font-size:9px;color:#6f7586;margin-top:3px;line-height:1.5}.switch{width:39px;height:23px;border:0;border-radius:20px;background:#252938;padding:3px;cursor:pointer}.switch i{display:block;width:17px;height:17px;border-radius:50%;background:#777d8c;transition:.2s}.switch.on{background:#6844df}.switch.on i{transform:translateX(16px);background:#fff}.auth input{width:100%;margin:6px 0;padding:12px;border:1px solid #202432;background:#10121a;border-radius:11px;color:#fff;outline:0}.auth small{color:#6f7586;font-size:9px;line-height:1.5}.auth .primary{width:100%;margin-top:9px}.auth .switch-auth{border:0;background:transparent;color:#a78bfa;font-size:10px;margin-top:12px;cursor:pointer}.status-note{display:flex;gap:8px;align-items:center;padding:11px;border-radius:12px;background:#11131c;color:#73798a;font-size:9px;margin-top:12px}.status-note svg{color:#22c55e}
        @media(max-width:1100px){.olunaa{grid-template-columns:220px minmax(300px,430px) minmax(0,1fr)}.sidebar{padding:14px 9px}.security{display:none}}
        @media(max-width:820px){.olunaa{display:block}.sidebar{position:fixed;z-index:80;inset:0 auto 0 0;width:min(290px,88vw);transform:translateX(-105%);transition:.22s;box-shadow:30px 0 80px rgba(0,0,0,.5)}.sidebar.open{transform:translateX(0)}.close-mobile{display:grid;margin-left:auto;border:0;background:#151722;color:#aaa;width:32px;height:32px;border-radius:9px;place-items:center}.main-column{min-height:100vh;border:0}.conversation{display:none}.topbar{padding-left:10px}.mobile-menu{display:grid;margin-right:8px}.topbar>div:first-of-type{margin-right:auto}.top-actions .primary{display:none}.searchbar{margin:10px}.empty-section{padding-top:42px}}
        @media(min-width:821px){.mobile-menu{display:none}}
        @media(min-width:1500px){.olunaa{grid-template-columns:270px minmax(400px,520px) minmax(500px,1fr)}.conversation-empty h2{font-size:28px}}
      `}</style>
    </main>
  );
}

function Nav({ active, icon, label, onClick }: { active: boolean; icon: React.ReactNode; label: string; onClick: () => void }) {
  return <button className={`nav ${active ? "active" : ""}`} onClick={onClick}>{icon}<span>{label}</span></button>;
}

function EmptyMessages({ search, onCreate }: { search: string; onCreate: () => void }) {
  return <div className="empty-section"><div className="empty-icon"><MessageCircle/></div><h2>{search ? `No real results for “${search}”` : "No conversations yet"}</h2><p>OLUNAA no longer displays invented users, fake online states or sample messages. Create an account to load real conversations.</p><button className="primary" onClick={onCreate}>Create account</button><div className="status-note"><WifiOff size={14}/> Live account data will appear after the production database is connected.</div></div>;
}

function EmptySection({ icon, title, text }: { icon: React.ReactNode; title: string; text: string }) {
  return <div className="empty-section"><div className="empty-icon">{icon}</div><h2>{title}</h2><p>{text}</p></div>;
}

function SettingsPanel({ settings, update, close, notify }: { settings: SettingsState; update: (k:keyof SettingsState,v:boolean)=>void; close:()=>void; notify:(s:string)=>void }) {
  return <div className="overlay" onMouseDown={close}><div className="panel" onMouseDown={e=>e.stopPropagation()}><div className="panel-head"><h2>OLUNAA Settings</h2><button onClick={close}><X size={17}/></button></div>
    <Setting icon={<Wifi/>} title="Show online status" text="Let people you chat with see when you are online." value={settings.showOnline} onChange={v=>update("showOnline",v)}/>
    <Setting icon={<Eye/>} title="Last seen" text="Control whether your last active time is visible." value={settings.lastSeen} onChange={v=>update("lastSeen",v)}/>
    <Setting icon={<Check/>} title="Read receipts" text="Show the second check when a message has been read." value={settings.readReceipts} onChange={v=>update("readReceipts",v)}/>
    <Setting icon={<MessageCircle/>} title="Typing indicators" text="Show when you are typing in a conversation." value={settings.typing} onChange={v=>update("typing",v)}/>
    <Setting icon={<Fingerprint/>} title="Biometric app lock" text="The Android app will use the system biometric prompt when enabled." value={settings.fingerprint} onChange={v=>{update("fingerprint",v);notify(v?"Biometric preference enabled":"Biometric preference disabled")}}/>
    <div className="status-note"><ShieldCheck size={14}/> Fingerprint/face authentication is handled by Android's secure biometric system in the native app.</div>
  </div></div>;
}

function Setting({icon,title,text,value,onChange}:{icon:React.ReactNode;title:string;text:string;value:boolean;onChange:(v:boolean)=>void}){return <div className="setting">{icon}<div><b>{title}</b><span>{text}</span></div><button className={`switch ${value?"on":""}`} onClick={()=>onChange(!value)} aria-label={title}><i/></button></div>}

function AuthModal({ mode, switchMode, close, notify }: { mode: "login"|"register"; switchMode:()=>void; close:()=>void; notify:(s:string)=>void }) {
  return <div className="overlay" onMouseDown={close}><div className="panel auth" onMouseDown={e=>e.stopPropagation()}><div className="panel-head"><div><span style={{fontSize:8,color:"#8b5cf6",letterSpacing:".15em"}}>WELCOME TO OLUNAA</span><h2>{mode === "register" ? "Create your account" : "Sign in"}</h2></div><button onClick={close}><X size={17}/></button></div>{mode === "register" && <input placeholder="Display name" autoComplete="name"/>}<input placeholder="Email" type="email" autoComplete="email"/><input placeholder={mode === "register" ? "Choose @username" : "Email or @username"} autoComplete="username"/><input placeholder="Password" type="password" autoComplete={mode === "register" ? "new-password" : "current-password"}/><small>Account creation is intentionally shown as a real flow; server-side registration, username reservation and session storage require the production database to be connected.</small><button className="primary" onClick={()=>notify("Connect the production database to create a real account")}>{mode === "register" ? "Create OLUNAA account" : "Continue"}</button><button className="switch-auth" onClick={switchMode}>{mode === "register" ? "Already have an account? Sign in" : "New to OLUNAA? Create account"}</button></div></div>;
}
