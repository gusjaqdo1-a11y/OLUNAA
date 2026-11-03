"use client";

import { useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";

export default function ResetPasswordPage() {
  const params = useSearchParams();
  const token = useMemo(() => params.get("token") ?? "", [params]);
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(""); setMessage("");
    if (!token) return setError("This reset link is missing its token.");
    if (password.length < 8) return setError("Password must be at least 8 characters.");
    if (password !== confirm) return setError("Passwords do not match.");
    setBusy(true);
    try {
      const r = await fetch("/api/auth/reset", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ token, password }) });
      const data = await r.json();
      if (!r.ok) throw new Error(data.error || "Password reset failed.");
      setMessage("Password changed successfully. You can now use OLUNAA.");
      setTimeout(() => { window.location.href = "/"; }, 1200);
    } catch (e: any) { setError(e.message || "Password reset failed."); }
    finally { setBusy(false); }
  };

  return <main style={{minHeight:"100vh",display:"grid",placeItems:"center",background:"#07080d",color:"#fff",fontFamily:"Inter,system-ui,sans-serif",padding:18}}><form onSubmit={submit} style={{width:"min(430px,100%)",background:"#0c0e15",border:"1px solid #202431",borderRadius:22,padding:24,boxShadow:"0 30px 100px #000"}}><div style={{fontSize:10,color:"#8b5cf6",fontWeight:800,letterSpacing:".16em"}}>OLUNAA</div><h1 style={{fontSize:24,margin:"8px 0"}}>Reset password</h1><p style={{fontSize:12,color:"#73798a",lineHeight:1.6}}>Choose a new password for your account.</p><label style={{display:"block",fontSize:11,color:"#a4a9b8",margin:"16px 0 6px"}}>New password</label><input type="password" value={password} onChange={e=>setPassword(e.target.value)} minLength={8} autoComplete="new-password" required style={{width:"100%",padding:12,border:"1px solid #202432",background:"#10121a",borderRadius:11,color:"#fff",outline:0}}/><label style={{display:"block",fontSize:11,color:"#a4a9b8",margin:"12px 0 6px"}}>Confirm password</label><input type="password" value={confirm} onChange={e=>setConfirm(e.target.value)} minLength={8} autoComplete="new-password" required style={{width:"100%",padding:12,border:"1px solid #202432",background:"#10121a",borderRadius:11,color:"#fff",outline:0}}/>{error&&<p style={{color:"#fb7185",fontSize:11}}>{error}</p>}{message&&<p style={{color:"#4ade80",fontSize:11}}>{message}</p>}<button disabled={busy} style={{width:"100%",marginTop:14,padding:12,border:0,borderRadius:11,background:"linear-gradient(135deg,#8b5cf6,#5b45df)",color:"#fff",fontWeight:700}}>{busy?"Saving…":"Set new password"}</button></form></main>;
}
