package com.olunaa.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
    private static final String API="https://olunaa.vercel.app/api";
    private static final int BG=Color.rgb(7,11,15), SURFACE=Color.rgb(14,20,25), CARD=Color.rgb(20,27,32), FIELD=Color.rgb(27,35,41), TEXT=Color.rgb(244,247,248), MUTED=Color.rgb(145,157,164), GREEN=Color.rgb(34,197,94), GREEN_DARK=Color.rgb(9,104,73), BLUE=Color.rgb(73,142,255), RED=Color.rgb(214,67,80);
    private final ExecutorService io=Executors.newCachedThreadPool();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
    private android.content.SharedPreferences prefs;
    private LinearLayout root,content;
    private TextView pageTitle,subtitle;
    private String meId="",meUsername="",meName="",meAvatar="",currentConversation="";
    private int pickerMode=0;
    private String pendingProfileImage="";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        CookieHandler.setDefault(cookies);
        prefs=getSharedPreferences("olunaa_session",MODE_PRIVATE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);
        restoreCookies();
        if(prefs.getBoolean("lock",false)) biometric();
        else if(hasSavedUser()) { restoreUser(); showHome(); }
        else showAuth(false);
    }

    private boolean hasSavedUser(){return !prefs.getString("id","").isEmpty() && !prefs.getString("username","").isEmpty();}
    private void restoreUser(){meId=prefs.getString("id","");meUsername=prefs.getString("username","");meName=prefs.getString("name","");meAvatar=prefs.getString("avatar","");}
    private void saveUser(){prefs.edit().putString("id",meId).putString("username",meUsername).putString("name",meName).putString("avatar",meAvatar).apply();}
    private TextView tv(String s,float size,int color){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("kern");return v;}
    private GradientDrawable shape(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
    private Button action(String text){Button b=new Button(this);b.setText(text);b.setTextColor(TEXT);b.setTextSize(14);b.setAllCaps(false);b.setGravity(Gravity.CENTER);b.setPadding(10,0,10,0);b.setMinHeight(0);b.setMinimumHeight(0);b.setBackground(shape(GREEN_DARK,28));return b;}
    private Button iconButton(String text){Button b=action(text);b.setTextSize(20);b.setTextColor(TEXT);b.setBackgroundColor(Color.TRANSPARENT);return b;}
    private EditText field(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(MUTED);e.setTextColor(TEXT);e.setTextSize(15);e.setSingleLine(true);e.setPadding(18,0,18,0);e.setBackground(shape(FIELD,24));return e;}
    private LinearLayout.LayoutParams lp(int h,int bottom){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h);p.setMargins(0,0,0,bottom);return p;}
    private void base(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);root.setPadding(10,8,10,8);setContentView(root);}
    private void toast(String s){main.post(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());}
    private void setMe(JSONObject u){if(u==null)return;meId=u.optString("id",meId);meUsername=u.optString("username",meUsername);meName=u.optString("displayName",meName);meAvatar=u.optString("avatarUrl",meAvatar);saveUser();}

    private ImageView logoView(int size){
        ImageView im=new ImageView(this);im.setImageResource(com.olunaa.app.R.drawable.ic_olunaa);im.setScaleType(ImageView.ScaleType.CENTER_INSIDE);return im;
    }
    private ImageView avatar(String url,String fallback){
        ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setBackground(shape(FIELD,100));im.setPadding(2,2,2,2);
        if(url!=null&&!url.isEmpty()) setImage(im,url); else {im.setImageDrawable(new android.graphics.drawable.ColorDrawable(FIELD));im.setContentDescription(fallback);}
        return im;
    }

    private void showAuth(boolean register){
        base();
        ScrollView sc=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);box.setPadding(18,30,18,30);sc.addView(box);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        ImageView logo=logoView(100);box.addView(logo,new LinearLayout.LayoutParams(108,108));
        TextView brand=tv("OLUNAA",32,TEXT);brand.setTypeface(null,1);brand.setGravity(Gravity.CENTER);box.addView(brand,lp(44,0));
        TextView tag=tv(register?"Create your account":"Private. Fast. Connected.",14,MUTED);tag.setGravity(Gravity.CENTER);box.addView(tag,lp(34,22));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(16,18,16,18);card.setBackground(shape(SURFACE,26));box.addView(card,lp(-2,14));
        TextView title=tv(register?"Create account":"Welcome back",23,TEXT);title.setTypeface(null,1);card.addView(title,lp(-2,4));
        TextView desc=tv(register?"Choose your @username and start chatting.":"Sign in with your email or @username.",13,MUTED);card.addView(desc,lp(-2,16));
        EditText name=null,email=null,identifier=field(register?"Username":"Email or @username"),pass=field("Password");
        if(register){name=field("Your name");email=field("Email address");card.addView(name,lp(56,9));card.addView(email,lp(56,9));}
        card.addView(identifier,lp(56,9));card.addView(pass,lp(56,14));
        Button go=action(register?"Create account":"Sign in");card.addView(go,new LinearLayout.LayoutParams(-1,56));
        TextView toggle=tv(register?"Already have an account?  Sign in":"New to OLUNAA?  Create account",14,GREEN);toggle.setGravity(Gravity.CENTER);box.addView(toggle,lp(50,0));
        TextView forgot=tv("Forgot password?",13,MUTED);forgot.setGravity(Gravity.CENTER);if(!register)box.addView(forgot,lp(38,0));
        EditText fn=name,fe=email,fi=identifier,fp=pass;
        go.setOnClickListener(v->{
            String user=fi.getText().toString().trim();if(register)user=user.replace("@","");
            if((register&&(fn.getText().toString().trim().isEmpty()||fe.getText().toString().trim().isEmpty()||user.isEmpty()))||fp.getText().toString().isEmpty()){toast("Please complete all required fields");return;}
            JSONObject j=new JSONObject();try{if(register){j.put("displayName",fn.getText().toString().trim());j.put("email",fe.getText().toString().trim());j.put("username",user);}else j.put("identifier",user);j.put("password",fp.getText().toString());}catch(Exception ignored){}
            go.setEnabled(false);api(register?"/auth/register":"/auth/login","POST",j,r->{go.setEnabled(true);if(!r.ok){toast(r.error);return;}JSONObject u=r.data.optJSONObject("user");if(u==null&&r.data.optJSONObject("data")!=null)u=r.data.optJSONObject("data").optJSONObject("user");if(u==null){toast("Account response was incomplete");return;}setMe(u);if(register)showFirstPhotoPrompt();else showHome();});
        });
        toggle.setOnClickListener(v->showAuth(!register));if(!register)forgot.setOnClickListener(v->showForgot());
    }

    private void showFirstPhotoPrompt(){
        new AlertDialog.Builder(this).setTitle("Add your profile photo").setMessage("Use a real profile photo so people can recognize you. You can also skip and add it later.").setPositiveButton("Choose photo",(d,w)->{pickerMode=2;pickImage();}).setNegativeButton("Skip",(d,w)->showHome()).setCancelable(false).show();
    }
    private void showForgot(){
        base();LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);b.setPadding(12,35,12,10);root.addView(b,new LinearLayout.LayoutParams(-1,-1));
        b.addView(tv("Reset password",28,TEXT),lp(-2,10));b.addView(tv("Enter your email or @username. If it exists, a reset request will be sent.",14,MUTED),lp(-2,18));EditText id=field("Email or @username");b.addView(id,lp(56,10));Button go=action("Send reset link");b.addView(go,new LinearLayout.LayoutParams(-1,56));go.setOnClickListener(v->{JSONObject j=new JSONObject();try{j.put("identifier",id.getText().toString().trim());}catch(Exception ignored){}api("/auth/forgot","POST",j,r->toast(r.ok?"Reset request sent if the account exists.":r.error));});TextView back=tv("‹  Back to sign in",14,GREEN);back.setPadding(0,22,0,0);b.addView(back);back.setOnClickListener(v->showAuth(false));
    }

    private void showHome(){
        base();
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(2,0,0,4);
        ImageView mini=logoView(40);top.addView(mini,new LinearLayout.LayoutParams(42,52));
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.setPadding(8,0,0,0);pageTitle=tv("OLUNAA",23,TEXT);pageTitle.setTypeface(null,1);subtitle=tv("@"+meUsername,12,MUTED);titles.addView(pageTitle);titles.addView(subtitle);top.addView(titles,new LinearLayout.LayoutParams(0,58,1));
        Button camera=iconButton("⌾"),searchBtn=iconButton("⌕"),more=iconButton("⋮");top.addView(camera,new LinearLayout.LayoutParams(42,52));top.addView(searchBtn,new LinearLayout.LayoutParams(42,52));top.addView(more,new LinearLayout.LayoutParams(42,52));root.addView(top);
        EditText search=field("Search messages, people, @username…");root.addView(search,lp(50,7));
        LinearLayout filters=new LinearLayout(this);filters.setGravity(Gravity.CENTER_VERTICAL);String[] fs={"All","Unread","Favorites","Groups"};for(String f:fs){TextView chip=tv(f,12,f.equals("All")?TEXT:MUTED);chip.setGravity(Gravity.CENTER);chip.setPadding(17,0,17,0);chip.setBackground(shape(f.equals("All")?GREEN_DARK:SURFACE,24));filters.addView(chip,new LinearLayout.LayoutParams(-2,38));LinearLayout.LayoutParams cp=(LinearLayout.LayoutParams)chip.getLayoutParams();cp.setMargins(0,0,7,0);chip.setLayoutParams(cp);chip.setOnClickListener(v->{for(int i=0;i<filters.getChildCount();i++){TextView c=(TextView)filters.getChildAt(i);c.setTextColor(MUTED);c.setBackground(shape(SURFACE,24));}chip.setTextColor(TEXT);chip.setBackground(shape(GREEN_DARK,24));});}root.addView(filters,lp(42,7));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setBackground(shape(SURFACE,24));String[] names={"Chats","Updates","Communities","Calls"};String[] icons={"◉","◌","▣","☎"};for(int i=0;i<names.length;i++){final String n=names[i];LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);TextView ic=tv(icons[i],18,n.equals("Chats")?GREEN:MUTED);ic.setGravity(Gravity.CENTER);TextView tx=tv(n,11,n.equals("Chats")?TEXT:MUTED);tx.setGravity(Gravity.CENTER);item.addView(ic,new LinearLayout.LayoutParams(-1,27));item.addView(tx,new LinearLayout.LayoutParams(-1,22));item.setOnClickListener(v->switchTab(n));nav.addView(item,new LinearLayout.LayoutParams(0,58,1));}root.addView(nav,lp(62,0));
        camera.setOnClickListener(v->{pickerMode=3;pickImage();});searchBtn.setOnClickListener(v->search.requestFocus());more.setOnClickListener(v->showMenu());search.setOnEditorActionListener((v,a,e)->{discover(search.getText().toString().trim());return true;});loadChats();
    }

    private void switchTab(String n){pageTitle.setText(n.equals("Chats")?"OLUNAA":n);subtitle.setText(n.equals("Chats")?"@"+meUsername:"Explore OLUNAA");content.removeAllViews();if(n.equals("Chats"))loadChats();else if(n.equals("Updates"))loadStatus();else if(n.equals("Communities"))loadCommunity();else loadCalls();}
    private void section(String title,String sub){TextView t=tv(title,23,TEXT);t.setTypeface(null,1);content.addView(t,lp(-2,2));TextView s=tv(sub,13,MUTED);content.addView(s,lp(-2,12));}
    private void empty(String title,String text){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(20,55,20,55);TextView icon=tv("◉",46,GREEN);icon.setGravity(Gravity.CENTER);box.addView(icon,new LinearLayout.LayoutParams(-1,60));TextView t=tv(title,19,TEXT);t.setTypeface(null,1);t.setGravity(Gravity.CENTER);box.addView(t);TextView s=tv(text,13,MUTED);s.setGravity(Gravity.CENTER);s.setPadding(0,8,0,0);box.addView(s);content.addView(box);}

    private void loadChats(){
        section("Chats","Private conversations with your people");
        Button newChat=action("＋  New chat");content.addView(newChat,lp(50,9));newChat.setOnClickListener(v->newChat());
        api("/conversations","GET",null,r->{if(!r.ok){toast(r.error);return;}JSONArray a=r.data.optJSONArray("conversations");if(a==null||a.length()==0){empty("No chats yet","Tap New chat and search an @username.");return;}for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i),o=c==null?null:c.optJSONObject("other");if(o!=null)addChatRow(c,o);}});
    }
    private void addChatRow(JSONObject c,JSONObject o){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(8,10,8,10);row.setBackground(shape(CARD,22));
        row.addView(avatar(o.optString("avatarUrl",""),o.optString("displayName","User")),new LinearLayout.LayoutParams(56,56));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(12,0,5,0);
        TextView n=tv(o.optString("displayName")+(o.optBoolean("verified")?"  ✓":""),16,TEXT);n.setTypeface(null,1);TextView u=tv("@"+o.optString("username")+(o.optBoolean("online")?"  • online":""),11,o.optBoolean("online")?GREEN:MUTED);JSONObject lm=c.optJSONObject("lastMessage");TextView last=tv(lm==null?"Start a conversation":lm.optString("body","").isEmpty()?"Photo":""+lm.optString("body"),12,MUTED);info.addView(n);info.addView(u);info.addView(last);row.addView(info,new LinearLayout.LayoutParams(0,70,1));
        TextView badge=tv("",11,TEXT);badge.setGravity(Gravity.CENTER);badge.setBackground(shape(GREEN,20));row.addView(badge,new LinearLayout.LayoutParams(24,24));content.addView(row,lp(78,6));String cid=c.optString("id");row.setOnClickListener(v->openChat(cid,o));
    }
    private void newChat(){EditText q=field("Search @username or name");new AlertDialog.Builder(this).setTitle("New chat").setView(q).setPositiveButton("Search",(d,w)->discover(q.getText().toString().trim())).setNegativeButton("Cancel",null).show();}
    private void discover(String q){if(q==null||q.trim().isEmpty())return;content.removeAllViews();section("People","Search results for "+q);api("/search?q="+URLEncoder.encode(q,StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok){toast(r.error);return;}JSONArray a=r.data.optJSONArray("users");if(a==null||a.length()==0){empty("No user found","Try the exact @username.");return;}for(int i=0;i<a.length();i++){JSONObject u=a.optJSONObject(i);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(8,10,8,10);row.setBackground(shape(CARD,22));row.addView(avatar(u.optString("avatarUrl",""),u.optString("displayName","User")),new LinearLayout.LayoutParams(54,54));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(12,0,0,0);TextView n=tv(u.optString("displayName")+(u.optBoolean("verified")?"  ✓":""),16,TEXT);n.setTypeface(null,1);info.addView(n);info.addView(tv("@"+u.optString("username")+(u.optBoolean("online")?"  • online":""),12,u.optBoolean("online")?GREEN:MUTED));row.addView(info,new LinearLayout.LayoutParams(0,68,1));content.addView(row,lp(72,7));row.setOnClickListener(v->createChat(u.optString("id"),u));}});}
    private void createChat(String uid,JSONObject u){JSONObject j=new JSONObject();try{j.put("userId",uid);}catch(Exception ignored){}api("/conversations","POST",j,r->{if(!r.ok){toast(r.error);return;}openChat(r.data.optString("conversationId"),u);});}

    private void openChat(String cid,JSONObject other){
        currentConversation=cid;content.removeAllViews();
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);head.setPadding(0,0,0,4);Button back=iconButton("‹");head.addView(back,new LinearLayout.LayoutParams(42,54));head.addView(avatar(other.optString("avatarUrl",""),other.optString("displayName","User")),new LinearLayout.LayoutParams(46,46));LinearLayout hn=new LinearLayout(this);hn.setOrientation(LinearLayout.VERTICAL);hn.setPadding(10,0,0,0);TextView n=tv(other.optString("displayName")+(other.optBoolean("verified")?"  ✓":""),17,TEXT);n.setTypeface(null,1);hn.addView(n);hn.addView(tv(other.optBoolean("online")?"online":"last seen recently",11,other.optBoolean("online")?GREEN:MUTED));head.addView(hn,new LinearLayout.LayoutParams(0,55,1));Button call=iconButton("☎"),video=iconButton("▣"),menu=iconButton("⋮");head.addView(call,new LinearLayout.LayoutParams(42,52));head.addView(video,new LinearLayout.LayoutParams(42,52));head.addView(menu,new LinearLayout.LayoutParams(42,52));content.addView(head);
        ScrollView sc=new ScrollView(this);LinearLayout msgs=new LinearLayout(this);msgs.setOrientation(LinearLayout.VERTICAL);msgs.setPadding(6,10,6,10);sc.addView(msgs);content.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout composer=new LinearLayout(this);composer.setGravity(Gravity.CENTER_VERTICAL);composer.setPadding(0,5,0,0);Button plus=iconButton("＋"),emoji=iconButton("☺"),send=action("➤");EditText text=field("Message");composer.addView(plus,new LinearLayout.LayoutParams(42,54));composer.addView(emoji,new LinearLayout.LayoutParams(42,54));composer.addView(text,new LinearLayout.LayoutParams(0,54,1));composer.addView(send,new LinearLayout.LayoutParams(58,54));content.addView(composer);
        Runnable refresh=()->api("/messages?conversationId="+cid,"GET",null,r->{if(!r.ok){toast(r.error);return;}msgs.removeAllViews();JSONArray a=r.data.optJSONArray("messages");if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);boolean mine=meId.equals(m.optString("senderId"));LinearLayout line=new LinearLayout(this);line.setGravity(mine?Gravity.RIGHT:Gravity.LEFT);LinearLayout bubble=new LinearLayout(this);bubble.setOrientation(LinearLayout.VERTICAL);bubble.setPadding(13,9,13,7);bubble.setBackground(shape(mine?Color.rgb(0,112,78):FIELD,18));String body=m.optString("body","");if(!body.isEmpty())bubble.addView(tv(body,15,TEXT));String media=m.optString("mediaUrl","");if(!media.isEmpty()){ImageView im=new ImageView(this);im.setAdjustViewBounds(true);im.setScaleType(ImageView.ScaleType.CENTER_CROP);setImage(im,media);bubble.addView(im,new LinearLayout.LayoutParams(250,220));}if(mine){TextView ticks=tv(m.optString("seenAt").isEmpty()?"✓":"✓✓",10,BLUE);ticks.setGravity(Gravity.RIGHT);bubble.addView(ticks);}line.addView(bubble,new LinearLayout.LayoutParams(-2,-2));msgs.addView(line,lp(-2,5));}sc.post(()->sc.fullScroll(View.FOCUS_DOWN));});refresh.run();
        back.setOnClickListener(v->loadChats());emoji.setOnClickListener(v->text.append(" 😊"));plus.setOnClickListener(v->{pickerMode=1;pickImage();});send.setOnClickListener(v->sendText(text,cid,refresh));call.setOnClickListener(v->showCallDialog(other,false));video.setOnClickListener(v->showCallDialog(other,true));menu.setOnClickListener(v->toast("Chat options are ready for future message controls"));
    }
    private void sendText(EditText e,String cid,Runnable refresh){String s=e.getText().toString().trim();if(s.isEmpty())return;JSONObject j=new JSONObject();try{j.put("conversationId",cid);j.put("body",s);}catch(Exception ignored){}e.setText("");api("/messages","POST",j,r->{if(!r.ok)toast(r.error);else refresh.run();});}
    private void showCallDialog(JSONObject other,boolean video){new AlertDialog.Builder(this).setTitle(video?"Video call":"Voice call").setMessage("Calling "+other.optString("displayName","User")+" requires live WebRTC signaling. The chat, contacts and call history UI are already native.").setPositiveButton("OK",null).show();}

    private void loadStatus(){
        section("Updates","Share photos and moments that disappear after 24 hours");
        LinearLayout mine=new LinearLayout(this);mine.setGravity(Gravity.CENTER_VERTICAL);mine.setPadding(8,10,8,10);mine.setBackground(shape(CARD,22));mine.addView(avatar(meAvatar,meName),new LinearLayout.LayoutParams(58,58));LinearLayout mi=new LinearLayout(this);mi.setOrientation(LinearLayout.VERTICAL);mi.setPadding(12,0,0,0);TextView mn=tv("My status",16,TEXT);mn.setTypeface(null,1);mi.addView(mn);mi.addView(tv("Tap to add a photo",12,MUTED));mine.addView(mi,new LinearLayout.LayoutParams(0,70,1));Button add=action("＋");mine.addView(add,new LinearLayout.LayoutParams(52,52));content.addView(mine,lp(78,12));mine.setOnClickListener(v->{pickerMode=3;pickImage();});add.setOnClickListener(v->{pickerMode=3;pickImage();});
        TextView recent=tv("Recent updates",15,TEXT);recent.setTypeface(null,1);content.addView(recent,lp(-2,8));
        api("/status","GET",null,r->{if(!r.ok){toast(r.error);return;}JSONArray a=r.data.optJSONArray("stories");if(a==null||a.length()==0){empty("No recent updates","Your contacts' new stories will appear here.");return;}for(int i=0;i<a.length();i++){JSONObject st=a.optJSONObject(i),u=st.optJSONObject("user");String who=u==null?"You":u.optString("displayName","User");LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(8,10,8,10);row.setBackground(shape(CARD,22));row.addView(avatar(u==null?meAvatar:u.optString("avatarUrl",""),who),new LinearLayout.LayoutParams(56,56));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(12,0,0,0);TextView wn=tv(who+(u!=null&&u.optBoolean("verified")?"  ✓":""),16,TEXT);wn.setTypeface(null,1);info.addView(wn);info.addView(tv(st.optString("caption","").isEmpty()?"Photo update":st.optString("caption"),12,MUTED));row.addView(info,new LinearLayout.LayoutParams(0,70,1));content.addView(row,lp(78,7));row.setOnClickListener(v->viewStory(st));}});
    }
    private void viewStory(JSONObject st){JSONObject j=new JSONObject();try{j.put("storyId",st.optString("id"));}catch(Exception ignored){}api("/status","PUT",j,r->{LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);ImageView im=new ImageView(this);im.setAdjustViewBounds(true);setImage(im,st.optString("mediaUrl",""));box.addView(im,new LinearLayout.LayoutParams(-1,430));box.addView(tv(st.optString("caption",""),15,TEXT),lp(-2,6));JSONArray v=st.optJSONArray("views");box.addView(tv("Viewed by "+(v==null?0:v.length())+" people",12,MUTED));new AlertDialog.Builder(this).setTitle("Status").setView(box).setPositiveButton("Close",null).show();});}

    private void loadCommunity(){section("Communities","Official channels and community spaces");api("/channels","GET",null,r->{if(!r.ok){toast(r.error);return;}JSONArray a=r.data.optJSONArray("channels");if(a==null||a.length()==0){empty("No communities yet","Official OLUNAA communities will appear here.");return;}for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(9,10,9,10);row.setBackground(shape(CARD,22));row.addView(avatar(c.optString("avatarUrl",""),c.optString("name","Community")),new LinearLayout.LayoutParams(56,56));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(12,0,0,0);TextView n=tv(c.optString("name")+(c.optBoolean("verified",true)?"  ✓":""),16,TEXT);n.setTypeface(null,1);info.addView(n);info.addView(tv("@"+c.optString("handle")+"  •  "+c.optString("description","Official community"),12,MUTED));row.addView(info,new LinearLayout.LayoutParams(0,70,1));content.addView(row,lp(78,7));row.setOnClickListener(v->openChannel(c.optString("handle")));}});}
    private void openChannel(String handle){content.removeAllViews();Button back=iconButton("‹");content.addView(back,new LinearLayout.LayoutParams(44,50));back.setOnClickListener(v->loadCommunity());api("/channels?handle="+URLEncoder.encode(handle,StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok){toast(r.error);return;}JSONObject c=r.data.optJSONObject("channel");if(c==null)return;TextView t=tv(c.optString("name")+"  ✓",25,TEXT);t.setTypeface(null,1);content.addView(t);content.addView(tv("@"+c.optString("handle")+"\n"+c.optString("description",""),13,MUTED),lp(-2,12));JSONArray a=c.optJSONArray("messages");if(a!=null)for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);LinearLayout p=new LinearLayout(this);p.setOrientation(LinearLayout.VERTICAL);p.setPadding(13,11,13,11);p.setBackground(shape(CARD,20));JSONObject au=m.optJSONObject("author");p.addView(tv((au==null?"OLUNAA":au.optString("displayName","Admin"))+"  ✓",15,TEXT));p.addView(tv(m.optString("body",""),14,MUTED));content.addView(p,lp(-2,8));}});}
    private void loadCalls(){section("Calls","Voice and video call history");empty("No calls yet","Calls will appear here after live calling is connected.");}

    private void showMenu(){String[] items={"My profile","Fingerprint / device lock","Sign out"};new AlertDialog.Builder(this).setTitle("OLUNAA").setItems(items,(d,w)->{if(w==0)loadProfile();else if(w==1)enableLock();else signOut();}).show();}
    private void loadProfile(){
        content.removeAllViews();section("Profile","Your public OLUNAA identity");LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(10,12,10,12);card.setBackground(shape(CARD,24));card.addView(avatar(meAvatar,meName),new LinearLayout.LayoutParams(76,76));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(14,0,0,0);TextView n=tv(meName,20,TEXT);n.setTypeface(null,1);info.addView(n);info.addView(tv("@"+meUsername,13,MUTED));card.addView(info);content.addView(card,lp(100,10));Button edit=action("Edit profile");content.addView(edit,lp(54,8));edit.setOnClickListener(v->editProfile());Button lock=action("Fingerprint / device lock");lock.setBackground(shape(SURFACE,24));content.addView(lock,lp(54,8));lock.setOnClickListener(v->enableLock());Button logout=action("Sign out");logout.setBackground(shape(RED,24));content.addView(logout,new LinearLayout.LayoutParams(-1,54));logout.setOnClickListener(v->signOut());
    }
    private void enableLock(){prefs.edit().putBoolean("lock",true).apply();toast("App lock enabled");}
    private void signOut(){api("/auth/logout","POST",new JSONObject(),r->{prefs.edit().clear().apply();meId="";meUsername="";meName="";meAvatar="";showAuth(false);});}
    private void editProfile(){LinearLayout b=new LinearLayout(this);b.setOrientation(LinearLayout.VERTICAL);EditText name=field("Name");name.setText(meName);EditText username=field("Username");username.setText(meUsername);EditText bio=field("Bio");b.addView(name,lp(56,8));b.addView(username,lp(56,8));b.addView(bio,new LinearLayout.LayoutParams(-1,56));new AlertDialog.Builder(this).setTitle("Edit profile").setView(b).setPositiveButton("Save",(d,w)->{JSONObject j=new JSONObject();try{j.put("displayName",name.getText().toString().trim());j.put("username",username.getText().toString().trim());j.put("bio",bio.getText().toString().trim());if(!pendingProfileImage.isEmpty())j.put("avatarUrl",pendingProfileImage);}catch(Exception ignored){}api("/profile","PATCH",j,r->{if(!r.ok){toast(r.error);return;}setMe(r.data.optJSONObject("user"));pendingProfileImage="";showHome();});}).setNeutralButton("Choose photo",(d,w)->{pickerMode=2;pickImage();}).setNegativeButton("Cancel",null).show();}

    private void pickImage(){Intent i=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);startActivityForResult(i,90);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req!=90||result!=RESULT_OK||data==null)return;Uri uri=data.getData();if(uri==null)return;try{Bitmap b=MediaStore.Images.Media.getBitmap(getContentResolver(),uri);int max=900;float scale=Math.min(1f,max/(float)Math.max(b.getWidth(),b.getHeight()));if(scale<1f)b=Bitmap.createScaledBitmap(b,(int)(b.getWidth()*scale),(int)(b.getHeight()*scale),true);ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,58,out);String encoded="data:image/jpeg;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);if(pickerMode==1&&!currentConversation.isEmpty()){JSONObject j=new JSONObject();j.put("conversationId",currentConversation);j.put("body","");j.put("mediaUrl",encoded);j.put("mediaType","image/jpeg");api("/messages","POST",j,r->toast(r.ok?"Photo sent":"Photo failed: "+r.error));}else if(pickerMode==2){pendingProfileImage=encoded;toast("Photo selected. Open Edit profile and tap Save.");}else if(pickerMode==3)createStory(encoded);}catch(Exception e){toast("Could not read photo");}}
    private void createStory(String media){EditText cap=field("Caption (optional)");new AlertDialog.Builder(this).setTitle("Post status").setMessage("This photo will expire after 24 hours.").setView(cap).setPositiveButton("Post",(d,w)->{JSONObject j=new JSONObject();try{j.put("mediaUrl",media);j.put("caption",cap.getText().toString());j.put("expiresHours",24);}catch(Exception ignored){}api("/status","POST",j,r->{toast(r.ok?"Status posted":"Status failed: "+r.error);loadStatus();});}).setNegativeButton("Cancel",null).show();}

    private void biometric(){BiometricManager bm=BiometricManager.from(this);int can=bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL);if(can!=BiometricManager.BIOMETRIC_SUCCESS){restoreUser();showHome();return;}BiometricPrompt p=new BiometricPrompt(this,ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult r){restoreUser();showHome();}@Override public void onAuthenticationError(int e,CharSequence s){toast("Unlock required");}});p.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("Unlock OLUNAA").setSubtitle("Use fingerprint or device lock").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL).build());}

    private void restoreCookies(){String raw=prefs.getString("cookies","");if(!raw.isEmpty()){try{for(String c:raw.split("\\|")){if(!c.trim().isEmpty())cookies.getCookieStore().add(new URI(API),HttpCookie.parse(c).get(0));}}catch(Exception ignored){}}}
    private void saveCookies(){try{List<HttpCookie> list=cookies.getCookieStore().getCookies();StringBuilder s=new StringBuilder();for(HttpCookie c:list){if(s.length()>0)s.append('|');s.append(c.toString());}prefs.edit().putString("cookies",s.toString()).apply();}catch(Exception ignored){}}

    private static class Result{boolean ok;String error;JSONObject data;Result(boolean o,String e,JSONObject d){ok=o;error=e;data=d==null?new JSONObject():d;}}
    private interface Callback{void done(Result r);}
    private void api(String path,String method,JSONObject body,Callback cb){io.execute(()->{HttpURLConnection c=null;try{URL u=new URL(API+path);c=(HttpURLConnection)u.openConnection();c.setRequestMethod(method);c.setConnectTimeout(15000);c.setReadTimeout(25000);c.setDoInput(true);c.setRequestProperty("Accept","application/json");String cookie=cookies.getCookieStore().getCookies().isEmpty()?"":TextUtils.join("; ",cookies.getCookieStore().getCookies());if(!cookie.isEmpty())c.setRequestProperty("Cookie",cookie);if(body!=null&&(method.equals("POST")||method.equals("PATCH")||method.equals("PUT"))){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);try(OutputStream os=c.getOutputStream()){os.write(bytes);}}int code=c.getResponseCode();String set=c.getHeaderField("Set-Cookie");if(set!=null&&!set.isEmpty()){try{cookies.getCookieStore().add(new URI(API),HttpCookie.parse(set).get(0));saveCookies();}catch(Exception ignored){}}InputStream in=code>=400?c.getErrorStream():c.getInputStream();String s="";if(in!=null){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);s=out.toString("UTF-8");in.close();}JSONObject d=new JSONObject(s.isEmpty()?"{}":s);String err=d.optString("error",code+" error");main.post(()->cb.done(new Result(code>=200&&code<300,err,d)));}catch(Exception e){main.post(()->cb.done(new Result(false,e.getMessage()==null?"Network error":e.getMessage(),new JSONObject())));}finally{if(c!=null)c.disconnect();}});}
}
