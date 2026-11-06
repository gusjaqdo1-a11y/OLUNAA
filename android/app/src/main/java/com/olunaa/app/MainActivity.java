package com.olunaa.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.MediaStore;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.Param;

public class MainActivity extends AppCompatActivity {
    private static final String API="https://olunaa.vercel.app/api";
    private static final int REQ_MEDIA=701, REQ_NOTIF=702, REQ_MEDIA_PERMISSION=703, REQ_CAMERA=704, REQ_DOCUMENT=705, REQ_LOCATION=706;
    private static final int GREEN=Color.rgb(72,166,255), GREEN2=Color.rgb(32,104,235), BG_D=Color.rgb(5,11,24), SURFACE_D=Color.rgb(12,23,43), CARD_D=Color.rgb(17,34,59), FIELD_D=Color.rgb(18,35,59), TEXT_D=Color.rgb(244,249,255), MUTED_D=Color.rgb(151,177,207);
    private static final int BG_L=Color.rgb(244,248,255), SURFACE_L=Color.WHITE, CARD_L=Color.rgb(234,241,252), FIELD_L=Color.rgb(226,235,250), TEXT_L=Color.rgb(17,29,52), MUTED_L=Color.rgb(99,116,148);
    private final ExecutorService io=Executors.newCachedThreadPool();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<String,Bitmap> imageCache=new ConcurrentHashMap<>();
    private String lastRenderedMessagesKey="";
    private String activeHomeTab="";
    private long lastHomeNavigationAt=0L;
    private int homeGeneration=0;
    private android.content.SharedPreferences prefs;
    private LinearLayout root, body;
    private String meId="", meUsername="", meName="", meAvatar="", activeConversation="", activeOtherName="", activeOtherAvatar="", activeGroupId="";
    private boolean dark=true;
    private int mediaMode=0;
    private Runnable poller,typingDebounce,presencePoller;
    private TextView chatPresence,chatReplyHint;
    private AblyRealtime realtimeClient,chatRealtimeClient;
    private Channel incomingCallChannel,chatRealtimeChannel;
    private String pendingReplyId="",pendingReplyBody="",pendingReplySender="";

    private int bg(){return dark?BG_D:BG_L;} private int surface(){return dark?SURFACE_D:SURFACE_L;}
    private int card(){return dark?CARD_D:CARD_L;} private int field(){return dark?FIELD_D:FIELD_L;}
    private int text(){return dark?TEXT_D:TEXT_L;} private int muted(){return dark?MUTED_D:MUTED_L;}
    private GradientDrawable rounded(int c,float r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(r);return g;}
    private GradientDrawable gradient(int a,int b,float r){GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{a,b});g.setCornerRadius(r);return g;}
    private TextView tv(String s,float z,int c){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(c);return v;}
    private String safeString(JSONObject object,String key,String fallback){if(object==null||object.isNull(key))return fallback;String value=object.optString(key,fallback);return "null".equalsIgnoreCase(value)?fallback:value;}
    private LinearLayout.LayoutParams lp(int h,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,h);p.setMargins(0,0,0,b);return p;}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void rootBase(){
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackground(dark?gradient(Color.rgb(5,11,24),Color.rgb(10,27,49),dp(0)):rounded(bg(),0));root.setPadding(dp(12),0,dp(12),0);setContentView(root);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,in)->{androidx.core.graphics.Insets x=in.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());v.setPadding(dp(12)+x.left,x.top+dp(8),dp(12)+x.right,x.bottom+dp(6));return in;});
        ViewCompat.requestApplyInsets(root);
    }
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setHintTextColor(muted());e.setTextColor(text());e.setTextSize(15);e.setSingleLine(true);e.setPadding(dp(18),0,dp(18),0);e.setBackground(rounded(field(),dp(18)));return e;}
    private Button btn(String s,boolean primary){Button b=new Button(this);b.setText(s);b.setTextColor(primary?Color.WHITE:text());b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(dp(14),0,dp(14),0);GradientDrawable shape=primary?gradient(GREEN,GREEN2,dp(18)):rounded(dark?Color.rgb(22,39,63):card(),dp(18));shape.setStroke(dp(1),primary?Color.argb(95,164,220,255):Color.argb(42,160,195,235));b.setBackground(shape);b.setElevation(primary?dp(5):dp(1));return b;}
    private ImageView logo(int size){ImageView im=new ImageView(this);im.setImageResource(R.drawable.ic_olunaa);im.setScaleType(ImageView.ScaleType.CENTER_INSIDE);return im;}
    private ImageView avatar(String url,int size){ImageView im=new ImageView(this);im.setScaleType(ImageView.ScaleType.CENTER_CROP);im.setBackground(rounded(field(),dp(999)));im.setClipToOutline(true);im.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);im.setContentDescription("Profile photo");im.setPadding(0,0,0,0);if(url!=null&&!url.isEmpty())loadImage(im,url);return im;}
    private Bitmap decodeScaledImage(byte[] bytes, int maxDimension) {
        if (bytes == null || bytes.length == 0) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null;
        int sample = 1;
        int limit = Math.max(256, Math.min(960, maxDimension));
        while (bounds.outWidth / sample > limit || bounds.outHeight / sample > limit) sample *= 2;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sample;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
    }

    private String imageCacheKey(String url) {
        if (url != null && url.startsWith("data:image")) {
            return "data:image:" + url.length() + ":" + Integer.toHexString(url.hashCode());
        }
        return url == null ? "" : url;
    }

    private void cacheImage(String key, Bitmap bitmap) {
        if (bitmap == null) return;
        imageCache.put(key, bitmap);
        if (imageCache.size() > 100) {
            Iterator<String> keys = imageCache.keySet().iterator();
            if (keys.hasNext()) imageCache.remove(keys.next());
        }
    }

    private void loadImage(ImageView im, String url) {
        if (url == null || url.isEmpty()) return;
        String cacheKey = imageCacheKey(url);
        Bitmap cached = imageCache.get(cacheKey);
        if (cached != null) { im.setImageBitmap(cached); return; }
        if (url.startsWith("data:image")) {
            io.execute(() -> {
                try {
                    byte[] raw = android.util.Base64.decode(url.substring(url.indexOf(',') + 1), android.util.Base64.DEFAULT);
                    Bitmap bitmap = decodeScaledImage(raw, 720);
                    if (bitmap != null) {
                        cacheImage(cacheKey, bitmap);
                        main.post(() -> im.setImageBitmap(bitmap));
                    }
                } catch (Exception ignored) { }
            });
            return;
        }
        io.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(6000);
                connection.setReadTimeout(8000);
                connection.setUseCaches(true);
                connection.setRequestProperty("Accept", "image/avif,image/webp,image/*");
                try (InputStream input = connection.getInputStream();
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int count;
                    int total = 0;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > 8 * 1024 * 1024) throw new IOException("Image exceeds 8 MB");
                        output.write(buffer, 0, count);
                    }
                    Bitmap bitmap = decodeScaledImage(output.toByteArray(), 720);
                    if (bitmap != null) {
                        cacheImage(cacheKey, bitmap);
                        main.post(() -> im.setImageBitmap(bitmap));
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);WindowCompat.enableEdgeToEdge(getWindow());getWindow().setStatusBarColor(Color.TRANSPARENT);getWindow().setNavigationBarColor(Color.TRANSPARENT);prefs=getSharedPreferences("olunaa_session",MODE_PRIVATE);
        String savedTheme=prefs.getString("theme","dark");dark=savedTheme.equals("system")?((getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES):!savedTheme.equals("light");
        WindowInsetsControllerCompat bars=WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());bars.setAppearanceLightStatusBars(!dark);bars.setAppearanceLightNavigationBars(!dark);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);requestNotifications();
        if(prefs.getBoolean("lock",false)&&hasUser())biometric();else if(hasUser()){restoreUser();showHome("Chats");handleIncomingCallIntent(getIntent());handleMessageNotificationIntent(getIntent());}else showAuth(false);
    }
    private void requestNotifications(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIF);}
    private boolean hasUser(){return !prefs.getString("id","").isEmpty()&&!prefs.getString("username","").isEmpty();}
    private void restoreUser(){meId=prefs.getString("id","");meUsername=prefs.getString("username","");meName=prefs.getString("name","");meAvatar=prefs.getString("avatar","");}
    private void saveUser(){prefs.edit().putString("id",meId).putString("username",meUsername).putString("name",meName).putString("avatar",meAvatar).apply();}
    private void setUser(JSONObject u){if(u==null)return;meId=u.optString("id",meId);meUsername=u.optString("username",meUsername);meName=u.optString("displayName",meName);meAvatar=u.optString("avatarUrl",meAvatar);prefs.edit().putBoolean("verified",u.optBoolean("verified",prefs.getBoolean("verified",false))).apply();saveUser();}
    private void message(String s){main.post(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());}

    private void showAuth(boolean register){
        stopPoll();rootBase();ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER_HORIZONTAL);box.setPadding(dp(8),dp(24),dp(8),dp(30));scroll.addView(box);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        box.addView(logo(1),new LinearLayout.LayoutParams(dp(112),dp(112)));TextView brand=tv("OLUNAA",32,text());brand.setTypeface(null,1);brand.setGravity(Gravity.CENTER);box.addView(brand,lp(dp(44),0));TextView tagline=tv(register?"Your identity. Your people. Your space.":"Private conversations, made beautifully.",14,muted());tagline.setGravity(Gravity.CENTER);box.addView(tagline,lp(dp(38),dp(18)));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(20),dp(18),dp(20));card.setBackground(rounded(surface(),dp(28)));box.addView(card,lp(-2,dp(14)));TextView title=tv(register?"Create your OLUNAA account":"Welcome back",25,text());title.setTypeface(null,1);card.addView(title,lp(-2,dp(4)));card.addView(tv(register?"Set your name, email and @username.":"Sign in with email or @username.",13,muted()),lp(-2,dp(18)));
        EditText name=null,email=null;if(register){name=input("Display name");email=input("Email address");card.addView(name,lp(dp(54),dp(9)));card.addView(email,lp(dp(54),dp(9)));}
        EditText ident=input(register?"Username":"Email or @username"),pass=input("Password");pass.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);card.addView(ident,lp(dp(54),dp(9)));card.addView(pass,lp(dp(54),dp(14)));
        Button submit=btn(register?"Create account":"Sign in",true);card.addView(submit,new LinearLayout.LayoutParams(-1,dp(54)));TextView switcher=tv(register?"Already have an account?  Sign in":"New here?  Create an account",14,GREEN);switcher.setGravity(Gravity.CENTER);box.addView(switcher,lp(dp(54),0));
        if(!register){TextView forgot=tv("Forgot password?",13,muted());forgot.setGravity(Gravity.CENTER);box.addView(forgot,lp(dp(38),0));forgot.setOnClickListener(v->showForgot());}
        EditText fName=name,fEmail=email,fIdent=ident,fPass=pass;submit.setOnClickListener(v->{String id=fIdent.getText().toString().trim().replaceFirst("^@","");if(fPass.getText().toString().length()<8||(register&&(fName.getText().toString().trim().isEmpty()||fEmail.getText().toString().trim().isEmpty()||id.isEmpty()))){message("Complete all fields. Password must be 8+ characters.");return;}JSONObject j=new JSONObject();try{j.put("password",fPass.getText().toString());if(register){j.put("displayName",fName.getText().toString().trim());j.put("email",fEmail.getText().toString().trim());j.put("username",id);}else j.put("identifier",id);}catch(Exception ignored){}submit.setEnabled(false);api(register?"/auth/register":"/auth/login","POST",j,r->{submit.setEnabled(true);if(!r.ok){message(r.error);return;}setUser(r.data.optJSONObject("user"));if(register)profilePhotoPrompt();else showHome("Chats");});});switcher.setOnClickListener(v->showAuth(!register));
    }
    private void showForgot(){rootBase();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(35),dp(12),0);root.addView(box,new LinearLayout.LayoutParams(-1,-1));TextView back=tv("‹  Sign in",15,GREEN);box.addView(back,lp(-2,dp(18)));back.setOnClickListener(v->showAuth(false));TextView h=tv("Reset password",30,text());h.setTypeface(null,1);box.addView(h,lp(-2,dp(6)));box.addView(tv("We'll send a reset request if the account exists.",14,muted()),lp(-2,dp(20)));EditText id=input("Email or @username");box.addView(id,lp(dp(54),dp(12)));Button go=btn("Send reset request",true);box.addView(go,new LinearLayout.LayoutParams(-1,dp(54)));go.setOnClickListener(v->{JSONObject j=new JSONObject();try{j.put("identifier",id.getText().toString().trim());}catch(Exception ignored){}api("/auth/forgot","POST",j,r->message(r.ok?"Request sent if the account exists.":r.error));});}
    private void profilePhotoPrompt(){rootBase();LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setGravity(Gravity.CENTER_HORIZONTAL);page.setPadding(dp(20),dp(24),dp(20),dp(24));root.addView(page,new LinearLayout.LayoutParams(-1,-1));ImageView mark=logo(1);page.addView(mark,new LinearLayout.LayoutParams(dp(72),dp(72)));TextView title=tv("Make your profile yours",25,text());title.setTypeface(null,1);title.setGravity(Gravity.CENTER);page.addView(title,lp(-2,dp(8)));TextView subtitle=tv("Add a photo so friends can recognize you. You can change it any time.",14,muted());subtitle.setGravity(Gravity.CENTER);page.addView(subtitle,lp(-2,dp(24)));FrameLayout photo=new FrameLayout(this);photo.setBackground(rounded(card(),dp(30)));photo.setForeground(rounded(Color.TRANSPARENT,dp(30)));ImageView preview=avatar("",1);FrameLayout.LayoutParams pp=new FrameLayout.LayoutParams(dp(150),dp(150),Gravity.CENTER);photo.addView(preview,pp);TextView plus=tv("+",26,Color.WHITE);plus.setGravity(Gravity.CENTER);plus.setBackground(rounded(GREEN,dp(30)));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(46),dp(46),Gravity.BOTTOM|Gravity.RIGHT);photo.addView(plus,bp);page.addView(photo,new LinearLayout.LayoutParams(dp(220),dp(220)));TextView hint=tv("PROFILE PHOTO",11,GREEN);hint.setTypeface(null,1);page.addView(hint,lp(dp(38),dp(20)));View spacer=new View(this);page.addView(spacer,new LinearLayout.LayoutParams(1,0,1));Button choose=btn("Choose photo or take a picture",true);page.addView(choose,new LinearLayout.LayoutParams(-1,dp(54)));choose.setOnClickListener(v->{mediaMode=1;pickImage();});Button skip=btn("Skip for now",false);page.addView(skip,lp(dp(52),0));skip.setOnClickListener(v->showHome("Chats"));}

    private void showHome(String tab){
        long now=SystemClock.elapsedRealtime();if(tab.equals(activeHomeTab)&&now-lastHomeNavigationAt<650L)return;
        activeHomeTab=tab;lastHomeNavigationAt=now;homeGeneration++;
        stopPoll();rootBase();
        if (hasUser() && realtimeClient == null) startIncomingCallListener();
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(10),dp(8),dp(10),dp(8));top.setBackground(dark?gradient(Color.rgb(17,34,68),Color.rgb(10,18,38),dp(22)):rounded(surface(),dp(22)));
        top.addView(logo(1),new LinearLayout.LayoutParams(dp(42),dp(46)));
        LinearLayout titleBox=new LinearLayout(this);titleBox.setOrientation(LinearLayout.VERTICAL);titleBox.setPadding(dp(7),0,0,0);
        TextView title=tv(tab.equals("Chats")?"OLUNAA":tab,21,text());title.setTypeface(null,1);
        TextView sub=tv("@"+meUsername,11,muted());titleBox.addView(title);titleBox.addView(sub);
        top.addView(titleBox,new LinearLayout.LayoutParams(0,dp(48),1));
        TextView newChat=tv("New Chat",11,text());newChat.setGravity(Gravity.CENTER);newChat.setTypeface(null,1);newChat.setPadding(dp(9),0,dp(9),0);newChat.setBackground(rounded(card(),dp(14)));
        TextView newGroup=tv("New Group",11,Color.WHITE);newGroup.setGravity(Gravity.CENTER);newGroup.setTypeface(null,1);newGroup.setPadding(dp(9),0,dp(9),0);newGroup.setBackground(gradient(GREEN,GREEN2,dp(14)));
        ImageView menu=new ImageView(this);menu.setImageResource(R.drawable.ic_action_more);menu.setImageTintList(android.content.res.ColorStateList.valueOf(text()));menu.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        if(tab.equals("Chats")){top.addView(newChat,new LinearLayout.LayoutParams(dp(96),dp(42)));LinearLayout.LayoutParams ngp=new LinearLayout.LayoutParams(dp(104),dp(42));ngp.setMargins(dp(6),0,0,0);top.addView(newGroup,ngp);}else if(tab.equals("Communities")){TextView newChannel=tv("＋ Channel",11,Color.WHITE);newChannel.setGravity(Gravity.CENTER);newChannel.setTypeface(null,1);newChannel.setPadding(dp(9),0,dp(9),0);newChannel.setBackground(gradient(GREEN,GREEN2,dp(14)));top.addView(newChannel,new LinearLayout.LayoutParams(dp(92),dp(42)));newChannel.setOnClickListener(v->createChannelDialog());}else{ImageView searchIcon=new ImageView(this);searchIcon.setImageResource(R.drawable.ic_action_search);searchIcon.setImageTintList(android.content.res.ColorStateList.valueOf(text()));searchIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);top.addView(searchIcon,new LinearLayout.LayoutParams(dp(40),dp(42)));searchIcon.setOnClickListener(v->showSearch());}top.addView(menu,new LinearLayout.LayoutParams(dp(28),dp(42)));root.addView(top,lp(dp(66),dp(8)));
        if(tab.equals("Chats")){LinearLayout searchWrap=new LinearLayout(this);searchWrap.setGravity(Gravity.CENTER_VERTICAL);searchWrap.setPadding(dp(14),0,dp(12),0);searchWrap.setBackground(rounded(field(),dp(16)));ImageView glyph=new ImageView(this);glyph.setImageResource(R.drawable.ic_action_search);glyph.setImageTintList(android.content.res.ColorStateList.valueOf(muted()));searchWrap.addView(glyph,new LinearLayout.LayoutParams(dp(24),dp(48)));EditText query=input("Search chats, people or groups");query.setBackgroundColor(Color.TRANSPARENT);query.setPadding(0,0,0,0);searchWrap.addView(query,new LinearLayout.LayoutParams(0,dp(48),1));root.addView(searchWrap,lp(dp(50),dp(10)));searchWrap.setOnClickListener(v->showSearch());query.setOnClickListener(v->showSearch());}
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.addView(body);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout nav=new LinearLayout(this);GradientDrawable navBackground=rounded(surface(),dp(22));navBackground.setStroke(dp(1),Color.argb(dark?45:28,110,145,210));nav.setBackground(navBackground);nav.setPadding(dp(4),dp(5),dp(4),dp(5));
        String[] labels={"Chats","Updates","Communities","Calls","Settings"};int[] navIcons={R.drawable.ic_nav_chats,R.drawable.ic_nav_updates,R.drawable.ic_nav_communities,R.drawable.ic_nav_calls,R.drawable.ic_nav_settings};
        for(int i=0;i<labels.length;i++){final String n=labels[i];boolean selected=n.equals(tab);LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setGravity(Gravity.CENTER);item.setBackground(selected?rounded(Color.argb(dark?38:22,67,139,255),dp(15)):rounded(Color.TRANSPARENT,dp(15)));ImageView icon=new ImageView(this);icon.setImageResource(navIcons[i]);icon.setImageTintList(android.content.res.ColorStateList.valueOf(selected?GREEN:muted()));icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);TextView t=tv(n,10,selected?GREEN:muted());t.setTypeface(null,selected?1:0);t.setGravity(Gravity.CENTER);item.addView(icon,new LinearLayout.LayoutParams(-1,dp(27)));item.addView(t,new LinearLayout.LayoutParams(-1,dp(19)));item.setOnClickListener(v->{if("Settings".equals(n))showSettings();else showHome(n);});nav.addView(item,new LinearLayout.LayoutParams(0,dp(52),1));}
        root.addView(nav,lp(dp(58),0));newChat.setOnClickListener(v->showSearch());newGroup.setOnClickListener(v->createGroupDialog());menu.setOnClickListener(v->showMenu());
        if(tab.equals("Chats"))loadChats();else if(tab.equals("Updates"))loadStatus();else if(tab.equals("Communities"))loadChannels();else if(tab.equals("Calls"))loadCalls();else if(tab.equals("Alerts"))loadNotifications();else if(tab.equals("Profile"))loadProfile();else showSettings();
    }
    private void showCreateHub(){stopPoll();rootBase();LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);root.addView(page,new LinearLayout.LayoutParams(-1,-1));LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView back=tv("‹",35,text());back.setGravity(Gravity.CENTER);head.addView(back,new LinearLayout.LayoutParams(dp(48),dp(56)));LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(tv("New conversation",21,text()));titles.addView(tv("Find people and create spaces",12,muted()));head.addView(titles);page.addView(head,lp(dp(62),dp(8)));back.setOnClickListener(v->showHome("Chats"));LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);actions.setPadding(dp(12),dp(12),dp(12),dp(12));actions.setBackground(gradient(Color.rgb(27,17,78),Color.rgb(10,24,65),dp(24)));addHubAction(actions,"User","New User","Find an OLUNAA member and start a private chat",()->showSearch());addHubAction(actions,"Group","New Group","Create a group conversation",()->createGroupDialog());addHubAction(actions,"Channel","New Channel","Create a public or private broadcast channel",()->createChannelDialog());addHubAction(actions,"Space","New Community","Bring groups and channels together",()->showCreateInfo("New Community","Community creation needs its own backend and membership controls."));page.addView(actions,lp(-2,dp(12)));TextView label=tv("YOUR OLUNAA CONTACTS",11,GREEN);label.setTypeface(null,1);page.addView(label,lp(dp(28),dp(6)));EditText query=input("Search name, @username or email");page.addView(query,lp(dp(52),dp(8)));ScrollView sc=new ScrollView(this);LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);sc.addView(results);page.addView(sc,new LinearLayout.LayoutParams(-1,0,1));TextView hint=tv("Loading OLUNAA contacts…",13,muted());hint.setGravity(Gravity.CENTER);results.addView(hint,lp(dp(90),0));api("/search?q=","GET",null,r->{if(!r.ok)return;results.removeAllViews();JSONArray users=r.data.optJSONArray("users");if(users==null||users.length()==0){TextView none=tv("No contacts found yet.",13,muted());none.setGravity(Gravity.CENTER);results.addView(none,lp(dp(70),0));return;}for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u!=null)contactRow(results,u);}});query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int count,int after){}public void onTextChanged(CharSequence x,int st,int before,int count){String q=x.toString().trim();if(q.length()<2)return;api("/search?q="+URLEncoder.encode(q,StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok)return;results.removeAllViews();JSONArray users=r.data.optJSONArray("users");if(users==null||users.length()==0){TextView none=tv("No matching contacts found.",13,muted());none.setGravity(Gravity.CENTER);results.addView(none,lp(dp(70),0));return;}for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u!=null)contactRow(results,u);}});}public void afterTextChanged(android.text.Editable e){}});}
    private void addHubAction(LinearLayout parent,String glyph,String title,String sub,Runnable action){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(8),dp(8),dp(8));TextView icon=tv(glyph,24,GREEN);icon.setGravity(Gravity.CENTER);icon.setBackground(rounded(Color.rgb(25,67,49),dp(15)));row.addView(icon,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);TextView t=tv(title,15,text());t.setTypeface(null,1);info.addView(t);info.addView(tv(sub,11,muted()));row.addView(info,new LinearLayout.LayoutParams(0,dp(54),1));row.addView(tv("›",25,muted()));parent.addView(row,lp(dp(64),dp(3)));row.setOnClickListener(v->action.run());}
    private void contactRow(LinearLayout parent,JSONObject u){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(8),dp(10),dp(8));row.setBackground(rounded(card(),dp(18)));ImageView av=avatar(u.optString("avatarUrl",""),1);row.addView(av,new LinearLayout.LayoutParams(dp(48),dp(48)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(11),0,0,0);TextView name=tv(u.optString("displayName","OLUNAA user"),15,text());name.setTypeface(null,1);info.addView(name);info.addView(tv("@"+u.optString("username",""),12,muted()));row.addView(info,new LinearLayout.LayoutParams(0,dp(54),1));row.addView(tv("＋",23,GREEN));parent.addView(row,lp(dp(68),dp(6)));row.setOnClickListener(v->{JSONObject j=new JSONObject();try{j.put("userId",u.optString("id"));}catch(Exception ignored){}api("/conversations","POST",j,r->{if(!r.ok){message(r.error);return;}String id=r.data.optString("conversationId");if(id.isEmpty()){message("Could not open conversation.");return;}openChat(id,u.optString("displayName","User"),u.optString("avatarUrl",""),false,u.optBoolean("verified"));});});}
    private void createChannelDialog(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(8),dp(8),dp(8),0);
        EditText name=input("Channel name");EditText handle=input("Channel username (e.g. news2026)");EditText desc=input("What is this channel about?");
        form.addView(name,lp(dp(52),dp(7)));form.addView(handle,lp(dp(52),dp(7)));form.addView(desc,lp(dp(52),dp(7)));
        TextView privacyLabel=tv("Channel privacy",12,muted());form.addView(privacyLabel,lp(dp(26),0));
        Spinner privacy=new Spinner(this);String[] privacyOptions={"Public — anyone can find and follow","Private — invite-only"};privacy.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,privacyOptions));form.addView(privacy,lp(dp(48),dp(6)));
        new AlertDialog.Builder(this).setTitle("Create a channel").setMessage("Choose its identity and who can discover it.").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Create channel",(d,w)->{
            String channelName=name.getText().toString().trim(),channelHandle=handle.getText().toString().trim().replaceFirst("^@","");
            if(channelName.length()<2||channelHandle.length()<3){message("Enter a channel name and a username with at least 3 characters.");return;}
            JSONObject payload=new JSONObject();try{payload.put("name",channelName);payload.put("handle",channelHandle);payload.put("description",desc.getText().toString().trim());payload.put("privacy",privacy.getSelectedItemPosition()==1?"private":"public");}catch(Exception ignored){}
            api("/channels","POST",payload,r->{if(!r.ok){message(r.error);return;}message("Channel created successfully.");showHome("Communities");});
        }).show();
    }
    private void showCreateInfo(String title,String detail){new AlertDialog.Builder(this).setTitle(title).setMessage(detail).setPositiveButton("OK",null).show();}
    private void heading(String h,String s){TextView a=tv(h,24,text());a.setTypeface(null,1);body.addView(a,lp(-2,dp(3)));body.addView(tv(s,13,muted()),lp(-2,dp(14)));}
    private void cardRow(LinearLayout row){row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setBackground(rounded(card(),dp(20)));body.addView(row,lp(dp(78),dp(7)));}
    private String chatCacheKey(){return "chats_cache_"+meId;}
    private String groupCacheKey(){return "groups_cache_"+meId;}
    private String messageCacheKey(String conversationId){return "messages_cache_"+meId+"_"+conversationId;}
    private void renderChatRows(LinearLayout target,JSONArray rows){target.removeAllViews();for(int i=0;i<rows.length();i++){JSONObject conversation=rows.optJSONObject(i),other=conversation==null?null:conversation.optJSONObject("other");if(other!=null)chatRow(conversation,other);}}
    private void renderGroupRows(LinearLayout target,JSONArray rows){target.removeAllViews();if(rows.length()==0){TextView hint=tv("Create a group to start a conversation with several people.",12,muted());target.addView(hint,lp(dp(40),0));return;}for(int i=0;i<rows.length();i++){JSONObject group=rows.optJSONObject(i);if(group!=null)groupRow(group);}}
    private void loadChats(){
        final LinearLayout target=body;final int generation=homeGeneration;
        heading("Your conversations","Your chats are saved on this device and appear instantly.");
        LinearLayout chatRows=new LinearLayout(this);chatRows.setOrientation(LinearLayout.VERTICAL);target.addView(chatRows);
        String cached=prefs.getString(chatCacheKey(),"");
        if(!cached.isEmpty())try{renderChatRows(chatRows,new JSONArray(cached));}catch(Exception ignored){}
        api("/conversations","GET",null,r->{if(generation!=homeGeneration||target!=body)return;if(!r.ok){if(chatRows.getChildCount()==0)message(r.error);return;}JSONArray rows=r.data.optJSONArray("conversations");if(rows!=null){prefs.edit().putString(chatCacheKey(),rows.toString()).apply();renderChatRows(chatRows,rows);prefetchRecentMessages(rows);}});
        TextView groupTitle=tv("YOUR GROUPS",11,GREEN);groupTitle.setTypeface(null,1);target.addView(groupTitle,lp(dp(30),dp(15)));
        LinearLayout groupRows=new LinearLayout(this);groupRows.setOrientation(LinearLayout.VERTICAL);target.addView(groupRows);
        String cachedGroups=prefs.getString(groupCacheKey(),"");
        if(!cachedGroups.isEmpty())try{renderGroupRows(groupRows,new JSONArray(cachedGroups));}catch(Exception ignored){}
        api("/groups","GET",null,r->{if(generation!=homeGeneration||target!=body)return;if(!r.ok)return;JSONArray rows=r.data.optJSONArray("groups");if(rows!=null){prefs.edit().putString(groupCacheKey(),rows.toString()).apply();renderGroupRows(groupRows,rows);}});
    }
    private void prefetchRecentMessages(JSONArray conversations){
        int limit=Math.min(conversations.length(),12);
        for(int i=0;i<limit;i++){
            JSONObject item=conversations.optJSONObject(i);if(item==null)continue;
            String conversationId=item.optString("id","");if(conversationId.isEmpty())continue;
            String cacheKey=messageCacheKey(conversationId);
            io.execute(()->{
                HttpURLConnection connection=null;
                try{
                    String cookie=prefs.getString("cookies","");
                    URL url=new URL(API+"/messages?conversationId="+URLEncoder.encode(conversationId,StandardCharsets.UTF_8));
                    connection=(HttpURLConnection)url.openConnection();connection.setRequestMethod("GET");connection.setConnectTimeout(7000);connection.setReadTimeout(10000);connection.setRequestProperty("Accept","application/json");if(!cookie.isEmpty())connection.setRequestProperty("Cookie",cookie);
                    int code=connection.getResponseCode();if(code<200||code>=300)return;
                    String raw=read(connection.getInputStream());JSONObject payload=new JSONObject(raw);JSONArray messages=payload.optJSONArray("messages");if(messages==null)return;
                    String snapshot=messages.toString();if(snapshot.length()<=180000)prefs.edit().putString(cacheKey,snapshot).apply();
                }catch(Exception ignored){}finally{if(connection!=null)connection.disconnect();}
            });
        }
    }
    private void createGroupDialog(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(8),dp(8),dp(8),0);
        EditText name=input("Group name");EditText handle=input("Group username (e.g. friends2026)");EditText desc=input("Group description");
        form.addView(name,lp(dp(52),dp(7)));form.addView(handle,lp(dp(52),dp(7)));form.addView(desc,lp(dp(52),dp(7)));
        ArrayList<String> selectedIds=new ArrayList<>();TextView selected=tv("＋  Add members  ·  No members selected",13,GREEN);selected.setGravity(Gravity.CENTER_VERTICAL);selected.setPadding(dp(12),0,dp(12),0);selected.setBackground(rounded(card(),dp(14)));form.addView(selected,lp(dp(48),dp(7)));
        TextView privacyTitle=tv("Group privacy",12,muted());form.addView(privacyTitle,lp(dp(26),0));Spinner privacy=new Spinner(this);String[] privacyOptions={"Private — invite-only","Public — discoverable by everyone"};privacy.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,privacyOptions));form.addView(privacy,lp(dp(48),dp(5)));
        selected.setOnClickListener(v->showGroupInvitePicker(selectedIds,selected));
        new AlertDialog.Builder(this).setTitle("Create your group").setMessage("Set up your group, then choose people to add.").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Create group",(d,w)->{
            String groupName=name.getText().toString().trim(),groupHandle=handle.getText().toString().trim();
            if(groupName.length()<2||groupHandle.replaceFirst("^@","").length()<3){message("Enter a group name and a username with at least 3 characters.");return;}
            JSONObject payload=new JSONObject();try{payload.put("name",groupName);payload.put("handle",groupHandle.replaceFirst("^@",""));payload.put("description",desc.getText().toString().trim());payload.put("memberIds",new JSONArray(selectedIds));payload.put("privacy",privacy.getSelectedItemPosition()==1?"public":"private");}catch(Exception ignored){}
            api("/groups","POST",payload,r->{if(!r.ok){message(r.error);return;}JSONObject g=r.data.optJSONObject("group");if(g==null){message("Group created, but could not open it.");return;}activeGroupId=g.optString("id","");String cid=g.optString("conversationId","");if(cid.isEmpty()){message("Group created. Reopen Messages to join.");showHome("Chats");return;}openChat(cid,g.optString("name","Group"),g.optString("avatarUrl",""),true);});
        }).show();
    }
    private void showGroupInvitePicker(ArrayList<String> selectedIds,TextView summary){
        LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);EditText query=input("Search people by name or @username");wrap.addView(query,lp(dp(50),dp(8)));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.addView(results);wrap.addView(sc,new LinearLayout.LayoutParams(-1,dp(300)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Add group members").setView(wrap).setNegativeButton("Done",(d,w)->summary.setText("✓  "+selectedIds.size()+" member(s) selected")).create();
        query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int c,int a){}public void onTextChanged(CharSequence x,int st,int before,int count){String q=x.toString().trim();if(q.length()<2)return;api("/search?q="+URLEncoder.encode(q,StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok)return;results.removeAllViews();JSONArray users=r.data.optJSONArray("users");if(users==null)return;for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u==null||meId.equals(u.optString("id")))continue;String id=u.optString("id");CheckBox item=new CheckBox(MainActivity.this);item.setText(u.optString("displayName","User")+"  @"+u.optString("username",""));item.setTextColor(text());item.setButtonTintList(android.content.res.ColorStateList.valueOf(GREEN));item.setChecked(selectedIds.contains(id));item.setPadding(dp(8),dp(8),dp(8),dp(8));item.setOnCheckedChangeListener((button,checked)->{if(checked&&!selectedIds.contains(id))selectedIds.add(id);else if(!checked)selectedIds.remove(id);summary.setText("＋  Add members  ·  "+selectedIds.size()+" selected");});results.addView(item,lp(dp(52),dp(2)));}});}public void afterTextChanged(android.text.Editable e){}});
        dialog.show();dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    private void showGroupMembers(String groupId){api("/groups/"+groupId+"/members","GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray members=r.data.optJSONArray("members");StringBuilder text=new StringBuilder();if(members!=null)for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i),u=m==null?null:m.optJSONObject("user");if(u!=null)text.append(u.optString("displayName","User")).append("  @").append(u.optString("username","")).append(m.optString("role","member").equals("owner")?"  · Owner":"").append("\n");}new AlertDialog.Builder(this).setTitle("Group members").setMessage(text.length()==0?"No members yet.":text.toString()).setPositiveButton("Invite people",(d,w)->showInviteGroupMember(groupId)).setNeutralButton("Manage admins",(d,w)->showGroupAdminPicker(groupId)).setNegativeButton("Close",null).show();});}
    private void showGroupAdminPicker(String groupId){
        api("/groups/"+groupId+"/members","GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray members=r.data.optJSONArray("members");if(members==null||members.length()==0){message("No members to manage.");return;}
            ArrayList<JSONObject> candidates=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();
            for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i),u=m==null?null:m.optJSONObject("user");if(u==null||"owner".equals(m.optString("role")))continue;candidates.add(m);labels.add(u.optString("displayName","Member")+"  ·  "+m.optString("role","member"));}
            if(candidates.isEmpty()){message("Invite members before assigning admins.");return;}
            new AlertDialog.Builder(this).setTitle("Choose a member").setItems(labels.toArray(new String[0]),(dialog,which)->{JSONObject member=candidates.get(which),u=member.optJSONObject("user");String id=u.optString("id",""),current=member.optString("role","member");String next=current.equals("admin")?"member":"admin";new AlertDialog.Builder(this).setTitle(next.equals("admin")?"Make group admin?":"Remove admin role?").setMessage(u.optString("displayName","This member")+" will "+(next.equals("admin")?"be able to invite members and manage the group.":"return to normal member permissions.")).setNegativeButton("Cancel",null).setPositiveButton("Confirm",(d,w)->{JSONObject payload=new JSONObject();try{payload.put("userId",id);payload.put("role",next);}catch(Exception ignored){}api("/groups/"+groupId+"/members","PATCH",payload,res->{if(!res.ok){message(res.error);return;}message(next.equals("admin")?"Admin role assigned.":"Admin role removed.");showGroupMembers(groupId);});}).show();}).setNegativeButton("Close",null).show();
        });
    }
    private void showInviteGroupMember(String groupId){EditText query=input("Search by name, @username or email");LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);ScrollView sc=new ScrollView(this);sc.addView(results);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.addView(query,lp(dp(52),dp(6)));wrap.addView(sc,new LinearLayout.LayoutParams(-1,dp(260)));query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){String q=s.toString().trim();if(q.length()<2)return;api("/search?q="+URLEncoder.encode(q,StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok)return;results.removeAllViews();JSONArray users=r.data.optJSONArray("users");if(users==null)return;for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u==null)continue;Button item=btn(u.optString("displayName","User")+"  @"+u.optString("username",""),false);results.addView(item,lp(dp(48),dp(4)));item.setOnClickListener(v->{JSONObject payload=new JSONObject();try{JSONArray ids=new JSONArray();ids.put(u.optString("id"));payload.put("userIds",ids);}catch(Exception ignored){}api("/groups/"+groupId+"/members","POST",payload,res->{if(!res.ok)message(res.error);else{message("Member invited.");showGroupMembers(groupId);}});});}});}public void afterTextChanged(android.text.Editable e){}});new AlertDialog.Builder(this).setTitle("Invite to group").setView(wrap).setNegativeButton("Close",null).show();}
    private void groupRow(JSONObject g){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setBackground(rounded(card(),dp(18)));ImageView av=avatar(g.optString("avatarUrl",""),1);row.addView(av,new LinearLayout.LayoutParams(dp(52),dp(52)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);TextView title=tv(g.optString("name","Group"),15,text());title.setTypeface(null,1);info.addView(title);info.addView(tv("@"+g.optString("handle",""),12,muted()));JSONArray members=g.optJSONArray("members");info.addView(tv((members==null?0:members.length())+" members",11,GREEN));row.addView(info,new LinearLayout.LayoutParams(0,dp(66),1));body.addView(row,lp(dp(76),dp(6)));row.setOnClickListener(v->{String cid=g.optString("conversationId","");if(!cid.isEmpty()){activeGroupId=g.optString("id","");openChat(cid,g.optString("name","Group"),g.optString("avatarUrl",""),true);}else api("/groups/"+g.optString("id")+"/join","POST",new JSONObject(),r->{if(!r.ok){message(r.error);return;}activeGroupId=g.optString("id","");openChat(r.data.optString("conversationId"),g.optString("name","Group"),g.optString("avatarUrl",""),true);});});row.setOnLongClickListener(v->{showGroupMembers(g.optString("id",""));return true;});}
    private void chatRow(JSONObject c, JSONObject o) {
        // WhatsApp-inspired conversation row: edge-to-edge, clear hierarchy,
        // generous touch target and a subtle separator instead of stacked cards.
        boolean online = o.optBoolean("online", false);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(4), dp(9), dp(5), dp(9));
        row.setBackgroundColor(Color.TRANSPARENT);
        row.setMinimumHeight(dp(78));
        row.setClickable(true);
        row.setFocusable(true);

        FrameLayout avatarWrap = new FrameLayout(this);
        ImageView photo = avatar(o.optString("avatarUrl", ""), 1);
        avatarWrap.addView(photo, new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER));
        if (online) {
            View presence = new View(this);
            GradientDrawable dot = new GradientDrawable();
            dot.setShape(GradientDrawable.OVAL);
            dot.setColor(Color.rgb(48, 209, 88));
            dot.setStroke(dp(2), bg());
            presence.setBackground(dot);
            FrameLayout.LayoutParams dotParams = new FrameLayout.LayoutParams(dp(14), dp(14), Gravity.BOTTOM | Gravity.RIGHT);
            dotParams.setMargins(0, 0, dp(1), dp(1));
            avatarWrap.addView(presence, dotParams);
        }
        row.addView(avatarWrap, new LinearLayout.LayoutParams(dp(64), dp(64)));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(9), 0, dp(7), 0);

        JSONObject last = c.optJSONObject("lastMessage");
        String displayName = safeString(o, "displayName", "OLUNAA user");
        String preview = "Tap to start a conversation";
        String time = "";
        boolean unread = false;
        boolean mine = false;
        boolean seen = false;
        if (last != null) {
            String bodyText = safeString(last, "body", "").trim();
            String mediaType = safeString(last, "mediaType", "");
            if (!bodyText.isEmpty()) preview = bodyText;
            else if (mediaType.startsWith("image")) preview = "▧  Photo";
            else if (mediaType.startsWith("video")) preview = "▶  Video";
            else if (mediaType.startsWith("audio")) preview = "♫  Voice message";
            else if (!safeString(last, "mediaUrl", "").isEmpty()) preview = "📎  Attachment";
            else preview = "Message";
            mine = meId.equals(last.optString("senderId", ""));
            seen = !safeString(last, "seenAt", "").isEmpty();
            if (mine) preview = "You: " + preview;
            String created = safeString(last, "createdAt", "");
            if (created.length() >= 16) time = created.substring(11, 16);
            unread = !mine && !seen;
        }

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);
        titleLine.setOrientation(LinearLayout.HORIZONTAL);
        TextView name = tv(displayName, 16, text());
        name.setTypeface(null, 1);
        name.setSingleLine(true);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleLine.addView(name, new LinearLayout.LayoutParams(0, dp(25), 1));
        if (o.optBoolean("verified")) {
            ImageView verified = new ImageView(this);
            verified.setImageResource(R.drawable.ic_verified_blue);
            verified.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            LinearLayout.LayoutParams verifiedLp = new LinearLayout.LayoutParams(dp(17), dp(17));
            verifiedLp.leftMargin = dp(4);
            titleLine.addView(verified, verifiedLp);
        }
        if (!time.isEmpty()) {
            TextView timeView = tv(time, 11, unread ? GREEN : muted());
            timeView.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
            LinearLayout.LayoutParams timeLp = new LinearLayout.LayoutParams(-2, dp(22));
            timeLp.leftMargin = dp(7);
            titleLine.addView(timeView, timeLp);
        }
        info.addView(titleLine, new LinearLayout.LayoutParams(-1, dp(27)));

        LinearLayout previewLine = new LinearLayout(this);
        previewLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView messagePreview = tv(preview, 13, unread ? text() : muted());
        messagePreview.setSingleLine(true);
        messagePreview.setEllipsize(android.text.TextUtils.TruncateAt.END);
        messagePreview.setTypeface(null, unread ? 1 : 0);
        previewLine.addView(messagePreview, new LinearLayout.LayoutParams(0, dp(24), 1));
        if (mine && last != null) {
            TextView receipt = tv(seen ? "✓✓" : "✓", 12, seen ? Color.rgb(69, 170, 255) : muted());
            receipt.setPadding(0, 0, dp(5), 0);
            previewLine.addView(receipt);
        }
        if (unread) {
            int unreadCount = Math.max(1, c.optInt("unreadCount", 1));
            TextView badge = tv(unreadCount > 99 ? "99+" : String.valueOf(unreadCount), 10, Color.WHITE);
            badge.setGravity(Gravity.CENTER);
            badge.setTypeface(null, 1);
            badge.setMinWidth(dp(20));
            badge.setPadding(dp(5), 0, dp(5), 0);
            badge.setBackground(rounded(Color.rgb(37, 190, 105), dp(20)));
            LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(-2, dp(20));
            badgeLp.leftMargin = dp(7);
            previewLine.addView(badge, badgeLp);
        } else if (online) {
            TextView active = tv("online", 10, GREEN);
            active.setPadding(dp(5), 0, 0, 0);
            previewLine.addView(active);
        }
        info.addView(previewLine, new LinearLayout.LayoutParams(-1, dp(25)));
        row.addView(info, new LinearLayout.LayoutParams(0, dp(62), 1));

        View divider = new View(this);
        divider.setBackgroundColor(Color.argb(dark ? 34 : 24, 145, 165, 195));
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.addView(row, new LinearLayout.LayoutParams(-1, dp(82)));
        LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(-1, dp(1));
        dividerLp.leftMargin = dp(73);
        item.addView(divider, dividerLp);
        body.addView(item, lp(-2, 0));
        row.setOnClickListener(v -> openChat(c.optString("id"), displayName, o.optString("avatarUrl", ""), false, o.optBoolean("verified")));
    }

    private void showSearch(){rootBase();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,dp(8),0,0);root.addView(box,new LinearLayout.LayoutParams(-1,-1));LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);TextView back=tv("‹",34,text());back.setGravity(Gravity.CENTER);bar.addView(back,new LinearLayout.LayoutParams(dp(48),dp(54)));EditText q=input("Search name, @username or email");bar.addView(q,new LinearLayout.LayoutParams(0,dp(54),1));box.addView(bar,lp(dp(62),dp(5)));back.setOnClickListener(v->showHome("Chats"));LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);ScrollView sc=new ScrollView(this);sc.addView(results);box.addView(sc,new LinearLayout.LayoutParams(-1,0,1));q.setOnEditorActionListener((v,a,e)->{searchUsers(q.getText().toString(),results);return true;});q.requestFocus();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(q,InputMethodManager.SHOW_IMPLICIT);}
    private void searchUsers(String q,LinearLayout results){results.removeAllViews();if(q.trim().isEmpty())return;api("/search?q="+URLEncoder.encode(q.trim(),StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray users=r.data.optJSONArray("users");if(users==null||users.length()==0){TextView x=tv("No people found.",15,muted());x.setPadding(dp(16),dp(25),dp(16),0);results.addView(x);return;}for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u!=null)searchRow(results,u);}});}
    private void searchRow(LinearLayout results,JSONObject u){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setBackground(rounded(card(),dp(20)));ImageView av=avatar(u.optString("avatarUrl",""),1);row.addView(av,new LinearLayout.LayoutParams(dp(52),dp(52)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);LinearLayout nameLine=new LinearLayout(this);nameLine.setGravity(Gravity.CENTER_VERTICAL);TextView n=tv(u.optString("displayName","User"),16,text());n.setTypeface(null,1);nameLine.addView(n);if(u.optBoolean("verified")){TextView tick=tv("✓",14,Color.rgb(40,132,255));tick.setTypeface(null,1);tick.setPadding(dp(5),0,0,0);nameLine.addView(tick);}info.addView(nameLine);info.addView(tv("@"+u.optString("username",""),12,muted()));info.addView(tv(u.optBoolean("online")?"Online":"Offline",11,u.optBoolean("online")?GREEN:muted()));row.addView(info,new LinearLayout.LayoutParams(0,dp(70),1));results.addView(row,lp(dp(72),dp(7)));row.setOnClickListener(v->createConversation(u));}
    private void createConversation(JSONObject u){JSONObject j=new JSONObject();try{j.put("userId",u.optString("id"));}catch(Exception ignored){}api("/conversations","POST",j,r->{if(!r.ok){message(r.error);return;}openChat(r.data.optString("conversationId"),u.optString("displayName","User"),u.optString("avatarUrl",""),false,u.optBoolean("verified"));});}

    private void openChat(String id,String name,String av){openChat(id,name,av,false,false);}
    private void openChat(String id,String name,String av,boolean isGroup){openChat(id,name,av,isGroup,false);}
    private void openChat(String id,String name,String av,boolean isGroup,boolean verified){
        if(id==null||id.trim().isEmpty())return;if(id.equals(activeConversation)&&chatReplyHint!=null)return;
        activeHomeTab="";activeConversation=id;activeOtherName=name;activeOtherAvatar=av;lastRenderedMessagesKey="";stopPoll();stopChatRealtime();rootBase();
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(dp(2),dp(5),dp(2),dp(8));top.setBackground(gradient(Color.rgb(16,43,91),Color.rgb(8,19,43),dp(22)));
        TextView back=tv("‹",34,text());back.setGravity(Gravity.CENTER);top.addView(back,new LinearLayout.LayoutParams(dp(40),dp(54)));
        ImageView img=avatar(av,1);top.addView(img,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout title=new LinearLayout(this);title.setOrientation(LinearLayout.VERTICAL);title.setPadding(dp(9),0,0,0);
        TextView n=tv(name,16,text());n.setTypeface(null,1);title.addView(n);if(verified){ImageView badge=new ImageView(this);badge.setImageResource(R.drawable.ic_verified_blue);title.addView(badge,new LinearLayout.LayoutParams(dp(18),dp(18)));}chatPresence=tv(isGroup?"Group conversation":"Checking status…",11,GREEN);title.addView(chatPresence);
        top.addView(title,new LinearLayout.LayoutParams(0,dp(54),1));
        ImageView voice=new ImageView(this),video=new ImageView(this);voice.setImageResource(R.drawable.ic_call_phone);video.setImageResource(R.drawable.ic_call_video);voice.setImageTintList(android.content.res.ColorStateList.valueOf(GREEN));video.setImageTintList(android.content.res.ColorStateList.valueOf(GREEN));voice.setBackground(rounded(Color.argb(dark?48:22,67,139,255),dp(15)));video.setBackground(rounded(Color.argb(dark?48:22,67,139,255),dp(15)));voice.setPadding(dp(10),dp(10),dp(10),dp(10));video.setPadding(dp(10),dp(10),dp(10),dp(10));voice.setContentDescription("Start voice call");video.setContentDescription("Start video call");
        top.addView(voice,new LinearLayout.LayoutParams(dp(42),dp(54)));top.addView(video,new LinearLayout.LayoutParams(dp(42),dp(54)));if(isGroup){TextView members=tv("☷",19,GREEN);members.setGravity(Gravity.CENTER);top.addView(members,new LinearLayout.LayoutParams(dp(58),dp(54)));members.setOnClickListener(v->showGroupMembers(activeGroupId));}root.addView(top,lp(dp(64),dp(7)));
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setSmoothScrollingEnabled(true);sc.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout messages=new LinearLayout(this);messages.setOrientation(LinearLayout.VERTICAL);messages.setPadding(dp(5),dp(12),dp(5),dp(14));sc.addView(messages);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        chatReplyHint=tv("",11,GREEN);chatReplyHint.setPadding(dp(12),dp(6),dp(12),dp(6));chatReplyHint.setVisibility(View.GONE);root.addView(chatReplyHint,lp(-2,0));
        LinearLayout composer=new LinearLayout(this);composer.setGravity(Gravity.CENTER_VERTICAL);composer.setPadding(dp(5),dp(6),dp(5),dp(6));composer.setBackground(gradient(Color.rgb(18,38,78),Color.rgb(9,20,48),dp(25)));
        ImageView attach=new ImageView(this);attach.setImageResource(R.drawable.ic_action_attach);attach.setImageTintList(android.content.res.ColorStateList.valueOf(GREEN));attach.setPadding(dp(7),dp(7),dp(7),dp(7));composer.addView(attach,new LinearLayout.LayoutParams(dp(38),dp(50)));
        EditText textBox=input("Message OLUNAA…");textBox.setSingleLine(false);textBox.setMinLines(1);textBox.setMaxLines(4);textBox.setMaxHeight(dp(116));textBox.setHorizontallyScrolling(false);textBox.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|InputType.TYPE_TEXT_FLAG_MULTI_LINE);textBox.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND|android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI);textBox.setTextColor(dark?Color.rgb(250,252,255):Color.rgb(16,28,48));textBox.setHintTextColor(dark?Color.rgb(168,184,207):Color.rgb(100,116,139));textBox.setTextSize(16);textBox.setGravity(Gravity.CENTER_VERTICAL);textBox.setPadding(dp(14),dp(10),dp(14),dp(10));textBox.setMinHeight(dp(48));textBox.setFocusable(true);textBox.setFocusableInTouchMode(true);textBox.setSelectAllOnFocus(false);
        composer.addView(textBox,new LinearLayout.LayoutParams(0,-2,1));
        textBox.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int c,int a){}public void onTextChanged(CharSequence x,int st,int before,int count){if(typingDebounce!=null)main.removeCallbacks(typingDebounce);String value=x.toString().trim();typingDebounce=()->setTyping(!value.isEmpty());main.postDelayed(typingDebounce,450);}public void afterTextChanged(android.text.Editable e){}});
        ImageView camera=new ImageView(this);camera.setImageResource(R.drawable.ic_action_camera);camera.setImageTintList(android.content.res.ColorStateList.valueOf(GREEN));camera.setPadding(dp(7),dp(7),dp(7),dp(7));composer.addView(camera,new LinearLayout.LayoutParams(dp(38),dp(50)));
        ImageView send=new ImageView(this);send.setImageResource(R.drawable.ic_action_send);send.setImageTintList(android.content.res.ColorStateList.valueOf(Color.WHITE));send.setPadding(dp(12),dp(12),dp(12),dp(12));send.setBackground(gradient(GREEN2,GREEN,dp(50)));composer.addView(send,new LinearLayout.LayoutParams(dp(46),dp(46)));root.addView(composer,lp(-2,dp(4)));
        back.setOnClickListener(v->showHome("Chats"));attach.setOnClickListener(v->{mediaMode=3;pickImage();});camera.setOnClickListener(v->{mediaMode=3;pickImage();});
        send.setOnClickListener(v->sendMessage(textBox,messages));textBox.setOnEditorActionListener((v,a,e)->{if(a==android.view.inputmethod.EditorInfo.IME_ACTION_SEND){sendMessage(textBox,messages);return true;}return false;});
        voice.setOnClickListener(v->callNotice(false));video.setOnClickListener(v->callNotice(true));
        loadMessages(messages);startChatRealtime(id,messages);poller=()->{loadMessages(messages);main.postDelayed(poller,8000);};main.postDelayed(poller,8000);presencePoller=()->{if(id.equals(activeConversation)){refreshChatPresence();main.postDelayed(presencePoller,1400);}};main.postDelayed(presencePoller,500);
    }
    private void stopChatRealtime(){try{if(chatRealtimeClient!=null)chatRealtimeClient.close();}catch(Exception ignored){}chatRealtimeClient=null;chatRealtimeChannel=null;}
    private void startChatRealtime(String conversationId,LinearLayout messageList){try{stopChatRealtime();ClientOptions options=new ClientOptions();options.authUrl=API+"/realtime/token?conversationId="+URLEncoder.encode(conversationId,StandardCharsets.UTF_8);options.authMethod="GET";String cookies=prefs.getString("cookies","");if(!cookies.isEmpty())options.authHeaders=new Param[]{new Param("Cookie",cookies)};options.clientId=meId;chatRealtimeClient=new AblyRealtime(options);chatRealtimeChannel=chatRealtimeClient.channels.get("conversation:"+conversationId);chatRealtimeChannel.subscribe(event->{if("message:new".equals(event.name))main.post(()->{if(conversationId.equals(activeConversation))loadMessages(messageList);});});}catch(Exception e){android.util.Log.w("OLUNAA","Realtime chat unavailable; polling fallback remains active",e);}}
    private void setTyping(boolean typing){if(activeConversation.isEmpty())return;JSONObject j=new JSONObject();try{j.put("conversationId",activeConversation);j.put("typing",typing);}catch(Exception ignored){}api("/typing","POST",j,r->{});}
    private void refreshChatPresence(){if(activeConversation.isEmpty())return;api("/typing?conversationId="+URLEncoder.encode(activeConversation,StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok||chatPresence==null)return;JSONArray a=r.data.optJSONArray("typing");if(a!=null&&a.length()>0){JSONObject u=a.optJSONObject(0);chatPresence.setText((u==null?"Someone":u.optString("displayName","Someone"))+" is typing…");}else api("/conversations","GET",null,c->{if(!c.ok||chatPresence==null)return;JSONArray rows=c.data.optJSONArray("conversations");if(rows==null)return;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row!=null&&activeConversation.equals(row.optString("id"))){JSONObject other=row.optJSONObject("other");if(other!=null){chatPresence.setText(other.optBoolean("online",false)?"Online":(other.optString("lastSeen","").isEmpty()?"Offline":"Last seen recently"));return;}}}chatPresence.setText("OLUNAA chat");});});}
    private void loadMessages(LinearLayout list){
        final String conversationId=activeConversation;
        if(lastRenderedMessagesKey.isEmpty()){
            String cached=prefs.getString(messageCacheKey(conversationId),"");
            if(!cached.isEmpty())try{JSONArray saved=new JSONArray(cached);lastRenderedMessagesKey=saved.toString();for(int i=0;i<saved.length();i++){JSONObject m=saved.optJSONObject(i);if(m!=null)bubble(list,m);}}catch(Exception ignored){}
        }
        api("/messages?conversationId="+URLEncoder.encode(conversationId,StandardCharsets.UTF_8),"GET",null,r->{
            if(!r.ok||!conversationId.equals(activeConversation))return;JSONArray a=r.data.optJSONArray("messages");if(a==null)return;String snapshot=a.toString();
            if(snapshot.equals(lastRenderedMessagesKey)){refreshChatPresence();return;}
            lastRenderedMessagesKey=snapshot;if(snapshot.length()<350000)prefs.edit().putString(messageCacheKey(conversationId),snapshot).apply();
            list.removeAllViews();for(int i=0;i<a.length();i++){JSONObject m=a.optJSONObject(i);if(m!=null)bubble(list,m);}refreshChatPresence();list.postDelayed(()->{ScrollView parent=findScroll(list);if(parent!=null)parent.fullScroll(View.FOCUS_DOWN);},80);
        });
    }
    private ScrollView findScroll(View v){View p=v;while(p.getParent() instanceof View){p=(View)p.getParent();if(p instanceof ScrollView)return (ScrollView)p;}return null;}
    private void bubble(LinearLayout list,JSONObject m){
        boolean mine=meId.equals(m.optString("senderId"));LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setGravity(mine?Gravity.RIGHT:Gravity.LEFT);
        String media=safeString(m,"mediaUrl","");String mediaType=safeString(m,"mediaType","");if(!media.isEmpty()){if(mediaType.startsWith("video")||media.startsWith("data:video/")){wrap.addView(videoPreview(media),new LinearLayout.LayoutParams(dp(250),dp(210)));}else if(media.startsWith("data:image/")||media.startsWith("https://")||media.startsWith("http://")){ImageView photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setBackground(rounded(card(),dp(18)));loadImage(photo,media);wrap.addView(photo,new LinearLayout.LayoutParams(dp(250),dp(210)));}else{TextView attachment=tv("📎  Attachment",14,GREEN);attachment.setPadding(dp(14),dp(12),dp(14),dp(12));attachment.setBackground(rounded(card(),dp(16)));wrap.addView(attachment);}}
        if(m.optBoolean("forwarded",false)){TextView fw=tv("↗ Forwarded",10,muted());fw.setPadding(dp(8),dp(3),dp(8),dp(3));wrap.addView(fw);}if(!safeString(m,"replyToId","").isEmpty()){LinearLayout quote=new LinearLayout(this);quote.setOrientation(LinearLayout.VERTICAL);quote.setPadding(dp(9),dp(6),dp(9),dp(6));quote.setBackground(rounded(dark?Color.rgb(28,49,43):Color.rgb(220,235,226),dp(10)));quote.addView(tv(safeString(m,"replyToSender","User"),10,GREEN));quote.addView(tv(safeString(m,"replyToBody","Message"),11,muted()));wrap.addView(quote,lp(-2,dp(4)));}String bodyText=safeString(m,"body","");if(!bodyText.isEmpty()){TextView b=tv(bodyText,15,mine?Color.WHITE:text());b.setPadding(dp(15),dp(11),dp(15),dp(11));b.setMaxWidth(dp(310));b.setBackground(mine?gradient(Color.rgb(22,111,226),Color.rgb(25,190,213),dp(20)):rounded(dark?Color.rgb(17,31,53):Color.rgb(235,242,253),dp(20)));b.setElevation(dp(2));wrap.addView(b,lp(-2,dp(3)));}
        JSONArray reactions=m.optJSONArray("reactions");if(reactions!=null&&reactions.length()>0){LinkedHashMap<String,Integer> counts=new LinkedHashMap<>();for(int i=0;i<reactions.length();i++){JSONObject rx=reactions.optJSONObject(i);if(rx!=null){String emoji=rx.optString("emoji","");counts.put(emoji,counts.getOrDefault(emoji,0)+1);}}StringBuilder rb=new StringBuilder();for(Map.Entry<String,Integer> e:counts.entrySet())rb.append(e.getKey()).append(" ").append(e.getValue()).append("  ");TextView reactionText=tv(rb.toString(),11,GREEN);reactionText.setPadding(dp(8),dp(4),dp(8),dp(4));wrap.addView(reactionText);}wrap.setOnLongClickListener(v->{showMessageActions(m,list);return true;});
        String stamp=m.optString("createdAt","");String receipt=mine?(m.optString("seenAt","").isEmpty()?(m.optString("deliveredAt","").isEmpty()?"":"  ✓"):"  ✓✓"):"";TextView meta=tv((stamp.length()>=16?stamp.substring(11,16):"")+receipt,10,mine&&!m.optString("seenAt","").isEmpty()?Color.rgb(61,177,255):muted());meta.setPadding(dp(5),dp(1),dp(5),0);wrap.addView(meta);list.addView(wrap,lp(-2,dp(9)));
    }
    private void sendMessage(EditText box,LinearLayout list){String s=box.getText().toString().trim();if(s.isEmpty()||activeConversation.isEmpty())return;String conversationId=activeConversation,replyId=pendingReplyId,replyBody=pendingReplyBody,replySender=pendingReplySender;JSONObject payload=new JSONObject(),optimistic=new JSONObject();try{payload.put("conversationId",conversationId);payload.put("body",s);if(!replyId.isEmpty())payload.put("replyToId",replyId);optimistic.put("id","local-"+UUID.randomUUID());optimistic.put("senderId",meId);optimistic.put("body",s);optimistic.put("createdAt",new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",java.util.Locale.US).format(new java.util.Date()));optimistic.put("deliveredAt","");optimistic.put("seenAt","");if(!replyId.isEmpty()){optimistic.put("replyToId",replyId);optimistic.put("replyToBody",replyBody);optimistic.put("replyToSender",replySender);}}catch(Exception ignored){}box.setText("");setTyping(false);clearReply();bubble(list,optimistic);ScrollView scroll=findScroll(list);if(scroll!=null)scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));api("/messages","POST",payload,r->{if(!conversationId.equals(activeConversation))return;if(!r.ok){message(r.error);loadMessages(list);}else loadMessages(list);});}
    private void clearReply(){pendingReplyId=pendingReplyBody=pendingReplySender="";if(chatReplyHint!=null)chatReplyHint.setVisibility(View.GONE);}
    private void setReply(JSONObject m){pendingReplyId=m.optString("id","");pendingReplyBody=m.optString("body",m.optString("mediaType","Media"));JSONObject sender=m.optJSONObject("sender");pendingReplySender=sender==null?"User":sender.optString("displayName","User");if(chatReplyHint!=null){chatReplyHint.setText("Replying to "+pendingReplySender+": "+pendingReplyBody);chatReplyHint.setVisibility(View.VISIBLE);}}
    private void showMessageActions(JSONObject m,LinearLayout list){String[] options={"Reply","Forward","React","Copy text"};new AlertDialog.Builder(this).setTitle("Message options").setItems(options,(d,w)->{if(w==0)setReply(m);else if(w==1)forwardMessage(m);else if(w==2)showReactions(m,list);else{String body=m.optString("body","");if(!body.isEmpty()){android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);cm.setPrimaryClip(ClipData.newPlainText("OLUNAA message",body));message("Message copied.");}}}).show();}
    private void showReactions(JSONObject m,LinearLayout list){String[] choices={"👍 Like","❤️ Love","😂 Laugh","😮 Wow","😢 Sad","🙏 Thanks","🔥 Fire","🎉 Celebrate"};String[] emojis={"👍","❤️","😂","😮","😢","🙏","🔥","🎉"};new AlertDialog.Builder(this).setTitle("React to message").setItems(choices,(d,w)->{JSONObject payload=new JSONObject();try{payload.put("messageId",m.optString("id"));payload.put("emoji",emojis[w]);}catch(Exception ignored){}api("/reactions","POST",payload,res->{if(!res.ok)message(res.error);else loadMessages(list);});}).show();}
    private void forwardMessage(JSONObject m){api("/conversations","GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray a=r.data.optJSONArray("conversations");if(a==null||a.length()==0){message("No conversation to forward to.");return;}ArrayList<JSONObject> choices=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i),o=c==null?null:c.optJSONObject("other");if(c!=null){choices.add(c);labels.add(o==null?"Group chat":o.optString("displayName","User")+"  @"+o.optString("username",""));}}new AlertDialog.Builder(this).setTitle("Forward to…").setItems(labels.toArray(new String[0]),(d,w)->{JSONObject target=choices.get(w),payload=new JSONObject();try{payload.put("conversationId",target.optString("id"));payload.put("body",m.optString("body",""));String media=m.optString("mediaUrl","");if(!media.isEmpty())payload.put("mediaUrl",media);String type=m.optString("mediaType","");if(!type.isEmpty())payload.put("mediaType",type);payload.put("forwarded",true);}catch(Exception ignored){}api("/messages","POST",payload,res->{if(!res.ok)message(res.error);else message("Message forwarded.");});}).setNegativeButton("Cancel",null).show();});}
    private void startLiveKitCall(String conversationId,String mode){JSONObject payload=new JSONObject();try{payload.put("conversationId",conversationId);payload.put("mode",mode);}catch(Exception ignored){}api("/calls/token","POST",payload,token->{if(!token.ok){message(token.error);return;}String server=token.data.optString("serverUrl",""),participant=token.data.optString("participantToken","");if(server.isEmpty()||participant.isEmpty()){message("Calling service is not configured. Please try again later.");return;}Intent call=new Intent(this,LiveKitCallActivity.class);call.putExtra("mode",mode);call.putExtra("serverUrl",server);call.putExtra("participantToken",participant);call.putExtra("otherName",activeOtherName);startActivity(call);});}
    private void callNotice(boolean video){if(activeConversation.isEmpty()){message("Open a conversation first.");return;}String mode=video?"video":"audio";JSONObject payload=new JSONObject();try{payload.put("conversationId",activeConversation);payload.put("mode",mode);}catch(Exception ignored){}api("/calls/invite","POST",payload,invite->{if(!invite.ok){message(invite.error);return;}startLiveKitCall(activeConversation,mode);});}
    private void registerFcmToken(){try{if(FirebaseApp.getApps(this).isEmpty())return;FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->{if(token==null||token.isEmpty())return;prefs.edit().putString("fcm_token",token).apply();JSONObject body=new JSONObject();try{body.put("token",token);body.put("platform","android");}catch(Exception ignored){}api("/devices/token","POST",body,r->{if(!r.ok)android.util.Log.w("OLUNAA","FCM token registration failed: "+r.error);});});}catch(Exception e){android.util.Log.w("OLUNAA","Firebase is not configured in this build",e);}}
    private void showIncomingCallDialog(JSONObject data){
        String conversationId=data.optString("conversationId",""),mode=data.optString("mode","audio"),caller=data.optString("callerName","OLUNAA user"),callId=data.optString("callId","");
        if(conversationId.isEmpty()||isFinishing())return;
        android.media.Ringtone ringtone=null;
        try{android.net.Uri sound=android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE);ringtone=android.media.RingtoneManager.getRingtone(this,sound);if(ringtone!=null){if(Build.VERSION.SDK_INT>=28)ringtone.setLooping(true);ringtone.play();}}catch(Exception ignored){}
        final android.media.Ringtone incomingTone=ringtone;
        TextView title=tv(caller+" is calling",21,text());title.setTypeface(null,1);title.setGravity(Gravity.CENTER);title.setPadding(dp(12),dp(16),dp(12),dp(8));
        TextView subtitle=tv("Incoming "+("video".equals(mode)?"video":"voice")+" call",14,muted());subtitle.setGravity(Gravity.CENTER);subtitle.setPadding(dp(12),0,dp(12),dp(18));
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(dp(8),dp(8),dp(8),dp(4));panel.addView(title);panel.addView(subtitle);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(panel).setNegativeButton("Decline",(d,w)->{if(incomingTone!=null)incomingTone.stop();if(!callId.isEmpty()){JSONObject state=new JSONObject();try{state.put("status","declined");}catch(Exception ignored){}api("/calls/"+callId+"/status","POST",state,r->{if(!r.ok)android.util.Log.w("OLUNAA","Call decline status failed: "+r.error);});}}).setPositiveButton("Answer",(d,w)->{if(incomingTone!=null)incomingTone.stop();activeOtherName=caller;if(!callId.isEmpty()){JSONObject state=new JSONObject();try{state.put("status","accepted");}catch(Exception ignored){}api("/calls/"+callId+"/status","POST",state,r->{if(!r.ok){message(r.error);return;}startLiveKitCall(conversationId,mode);});}else startLiveKitCall(conversationId,mode);}).create();
        dialog.setOnCancelListener(d->{if(incomingTone!=null)incomingTone.stop();});
        dialog.setOnDismissListener(d->{if(incomingTone!=null&&incomingTone.isPlaying())incomingTone.stop();});
        dialog.show();
    }
    private void handleIncomingCallIntent(Intent intent){if(intent==null||!"incoming_call".equals(intent.getStringExtra("type")))return;String conversationId=intent.getStringExtra("conversationId"),mode=intent.getStringExtra("mode"),action=intent.getStringExtra("call_action"),callId=intent.getStringExtra("callId");if(conversationId==null||conversationId.isEmpty())return;intent.removeExtra("type");intent.removeExtra("call_action");String caller=intent.getStringExtra("callerName");if("decline".equals(action)){if(callId!=null&&!callId.isEmpty()){JSONObject state=new JSONObject();try{state.put("status","declined");}catch(Exception ignored){}api("/calls/"+callId+"/status","POST",state,r->{if(!r.ok)android.util.Log.w("OLUNAA","Call decline status failed: "+r.error);});}return;}if("accept".equals(action)){activeOtherName=caller==null?"OLUNAA user":caller;if(callId!=null&&!callId.isEmpty()){JSONObject state=new JSONObject();try{state.put("status","accepted");}catch(Exception ignored){}api("/calls/"+callId+"/status","POST",state,r->{if(!r.ok){message(r.error);return;}startLiveKitCall(conversationId,mode==null?"audio":mode);});}else startLiveKitCall(conversationId,mode==null?"audio":mode);return;}JSONObject data=new JSONObject();try{data.put("callId",callId);data.put("conversationId",conversationId);data.put("mode",mode==null?"audio":mode);data.put("callerName",caller);}catch(Exception ignored){}showIncomingCallDialog(data);}
    private void handleMessageNotificationIntent(Intent intent){
        if(intent==null||!"message".equals(intent.getStringExtra("type")))return;
        String conversationId=intent.getStringExtra("conversationId");
        if(conversationId==null||conversationId.isEmpty())return;
        String sender=intent.getStringExtra("title");
        String avatar=intent.getStringExtra("senderAvatarUrl");
        intent.removeExtra("type");
        openChat(conversationId,sender==null||sender.isEmpty()?"OLUNAA contact":sender,avatar==null?"":avatar,false,false);
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);handleIncomingCallIntent(intent);handleMessageNotificationIntent(intent);}
    private void startIncomingCallListener(){if(meId.isEmpty())return;try{if(realtimeClient!=null)realtimeClient.close();ClientOptions options=new ClientOptions();options.authUrl=API+"/realtime/token";options.authMethod="GET";String cookies=prefs.getString("cookies","");if(!cookies.isEmpty())options.authHeaders=new Param[]{new Param("Cookie",cookies)};options.clientId=meId;realtimeClient=new AblyRealtime(options);incomingCallChannel=realtimeClient.channels.get("user:"+meId);incomingCallChannel.subscribe(msg->{if(!"call:invite".equals(msg.name))return;try{JSONObject data;if(msg.data instanceof JSONObject)data=(JSONObject)msg.data;else if(msg.data instanceof Map)data=new JSONObject((Map)msg.data);else data=new JSONObject(String.valueOf(msg.data));main.post(()->showIncomingCallDialog(data));}catch(Exception ignored){}});}catch(Exception e){android.util.Log.w("OLUNAA","Incoming call listener unavailable",e);}}

    private void loadNotifications(){heading("Notifications","Your messages, mentions and OLUNAA activity.");Button readAll=btn("Mark all as read",false);body.addView(readAll,lp(dp(46),dp(10)));readAll.setOnClickListener(v->{JSONObject j=new JSONObject();try{j.put("all",true);}catch(Exception ignored){}api("/notifications","PATCH",j,r->{if(r.ok){message("All notifications marked as read.");loadNotifications();}else message(r.error);});});api("/notifications","GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray rows=r.data.optJSONArray("notifications");if(rows==null||rows.length()==0){empty("All caught up","New activity will appear here.");return;}for(int i=0;i<rows.length();i++){JSONObject n=rows.optJSONObject(i);if(n==null)continue;JSONObject actor=n.optJSONObject("actor");LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(13),dp(10),dp(13),dp(10));card.setBackground(rounded(card(),dp(17)));ImageView av=avatar(actor==null?"":actor.optString("avatarUrl",""),1);card.addView(av,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(11),0,0,0);String who=actor==null?"OLUNAA":actor.optString("displayName","OLUNAA");TextView title=tv(who,14,text());title.setTypeface(null,1);info.addView(title);info.addView(tv(n.optString("body","New activity"),12,muted()));info.addView(tv(n.optString("createdAt","").replace("T"," ").substring(0,Math.min(16,n.optString("createdAt","").replace("T"," ").length())),10,muted()));card.addView(info,new LinearLayout.LayoutParams(0,dp(62),1));if(n.optString("readAt","").isEmpty()){View dot=new View(this);dot.setBackground(rounded(GREEN,dp(20)));card.addView(dot,new LinearLayout.LayoutParams(dp(9),dp(9)));}body.addView(card,lp(dp(82),dp(7)));card.setOnClickListener(v->{JSONObject j=new JSONObject();try{j.put("id",n.optString("id"));}catch(Exception ignored){}api("/notifications","PATCH",j,res->{if(res.ok)card.setAlpha(.65f);});});}});}
    private void loadStatus(){heading("Updates","Share photos and videos. Your updates expire automatically.");Button add=btn("＋  Add status",true);body.addView(add,lp(dp(50),dp(12)));add.setOnClickListener(v->{mediaMode=2;pickImage();});api("/status","GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray all=r.data.optJSONArray("stories");if(all==null||all.length()==0){empty("No updates yet","Post your first photo or video status.");return;}LinkedHashMap<String,JSONArray> grouped=new LinkedHashMap<>();for(int i=0;i<all.length();i++){JSONObject item=all.optJSONObject(i);if(item==null)continue;JSONObject owner=item.optJSONObject("user");String key=owner==null?"unknown":owner.optString("id","unknown");if(!grouped.containsKey(key))grouped.put(key,new JSONArray());grouped.get(key).put(item);}for(JSONArray items:grouped.values())statusRow(items);});}
    private void statusRow(JSONArray stories){JSONObject first=stories.optJSONObject(0),u=first==null?null:first.optJSONObject("user");if(first==null)return;boolean mine=meId.equals(first.optString("userId"));int totalViews=0;for(int i=0;i<stories.length();i++){JSONArray v=stories.optJSONObject(i).optJSONArray("views");if(v!=null)totalViews+=v.length();}LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(8),dp(10),dp(8));row.setBackground(rounded(card(),dp(18)));ImageView av=avatar(u==null?"":u.optString("avatarUrl",""),1);row.addView(av,new LinearLayout.LayoutParams(dp(58),dp(58)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);TextView name=tv(mine?"Your status":u==null?"OLUNAA user":u.optString("displayName","User"),15,text());name.setTypeface(null,1);info.addView(name);info.addView(tv(u==null?"":("@"+u.optString("username","")),12,muted()));info.addView(tv(stories.length()+" update(s)  •  "+totalViews+" views",12,GREEN));row.addView(info,new LinearLayout.LayoutParams(0,dp(66),1));TextView arrow=tv("›",26,muted());row.addView(arrow);body.addView(row,lp(dp(82),dp(8)));row.setOnClickListener(v->{if(mine)showMyStatuses(stories);else viewStatus(stories.optJSONObject(stories.length()-1));});}
    private void showMyStatuses(JSONArray stories){LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);for(int i=0;i<stories.length();i++){JSONObject st=stories.optJSONObject(i);if(st==null)continue;JSONArray viewers=st.optJSONArray("views");StringBuilder names=new StringBuilder((viewers==null?0:viewers.length())+" views");if(viewers!=null)for(int k=0;k<viewers.length();k++){JSONObject view=viewers.optJSONObject(k),person=view==null?null:view.optJSONObject("viewer");if(person!=null)names.append("\n").append(person.optString("displayName","User")).append("  @").append(person.optString("username",""));}LinearLayout item=new LinearLayout(this);item.setGravity(Gravity.CENTER_VERTICAL);item.setPadding(dp(10),dp(8),dp(10),dp(8));item.setBackground(rounded(card(),dp(16)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(tv(st.optString("caption","Photo/video status"),14,text()));info.addView(tv(names.toString(),11,muted()));item.addView(info,new LinearLayout.LayoutParams(0,-2,1));Button del=btn("Delete",false);item.addView(del,new LinearLayout.LayoutParams(dp(86),dp(42)));del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Delete status?").setMessage("This status and its view history will be deleted.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(d,w)->{JSONObject j=new JSONObject();try{j.put("storyId",st.optString("id"));}catch(Exception ignored){}api("/status","DELETE",j,r->{if(!r.ok)message(r.error);else{message("Status deleted.");showHome("Updates");}});}).show());list.addView(item,lp(-2,dp(7)));}new AlertDialog.Builder(this).setTitle("Your status • Viewers").setView(list).setPositiveButton("Close",null).show();}
    private void viewStatus(JSONObject st){if(st==null)return;JSONObject j=new JSONObject();try{j.put("storyId",st.optString("id"));}catch(Exception ignored){}api("/status","PUT",j,r->{if(!r.ok)message(r.error);});LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);String media=st.optString("mediaUrl","");if(media.startsWith("data:video/"))content.addView(videoPreview(media),new LinearLayout.LayoutParams(-1,dp(300)));else{ImageView preview=avatar("",1);if(!media.isEmpty())loadImage(preview,media);content.addView(preview,new LinearLayout.LayoutParams(-1,dp(300)));}content.addView(tv(st.optString("caption",""),14,text()));new AlertDialog.Builder(this).setTitle("Status").setView(content).setPositiveButton("Close",null).show();}

    private void loadChannels(){heading("Communities","Public channels and announcements.");api("/channels","GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray a=r.data.optJSONArray("channels");if(a==null||a.length()==0){empty("No communities yet","Public communities will appear here.");return;}for(int i=0;i<a.length();i++){JSONObject c=a.optJSONObject(i);if(c!=null)channelRow(c);}});}
    private void channelRow(JSONObject c){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);ImageView av=avatar(c.optString("avatarUrl",""),1);row.addView(av,new LinearLayout.LayoutParams(dp(54),dp(54)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);TextView n=tv(c.optString("name","Community"),16,text());n.setTypeface(null,1);info.addView(n);if(c.optBoolean("verified")){ImageView tick=new ImageView(this);tick.setImageResource(R.drawable.ic_verified_blue);info.addView(tick,new LinearLayout.LayoutParams(dp(18),dp(18)));}String publicId=c.optString("channelNumber","");info.addView(tv(publicId.isEmpty()?"@"+c.optString("handle",""):publicId,12,muted()));info.addView(tv(c.optString("description","Public channel"),12,muted()));row.addView(info,new LinearLayout.LayoutParams(0,dp(70),1));body.addView(row,lp(dp(76),dp(7)));row.setOnClickListener(v->openChannel(c.optString("channelNumber","").isEmpty()?c.optString("handle",""):c.optString("channelNumber","")));row.setOnLongClickListener(v->{showChannelSettings(c);return true;});}
    private void openChannel(String h){
        String key=h.startsWith("+")?"number":"handle";
        api("/channels?"+key+"="+URLEncoder.encode(h,StandardCharsets.UTF_8),"GET",null,r->{
            if(!r.ok){message(r.error);return;}
            JSONObject c=r.data.optJSONObject("channel");if(c==null)return;
            ScrollView scroll=new ScrollView(this);
            LinearLayout feed=new LinearLayout(this);feed.setOrientation(LinearLayout.VERTICAL);feed.setPadding(dp(12),dp(10),dp(12),dp(12));scroll.addView(feed);
            TextView description=tv(c.optString("description","Official OLUNAA channel"),13,muted());feed.addView(description,lp(-2,dp(10)));
            JSONArray posts=c.optJSONArray("messages");
            if(posts!=null)for(int i=posts.length()-1;i>=0;i--){
                JSONObject post=posts.optJSONObject(i);if(post==null)continue;
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(12),dp(12),dp(12));card.setBackground(rounded(card(),dp(18)));
                String body=post.optString("body","");if(!body.isEmpty())card.addView(tv(body,14,text()),lp(-2,dp(8)));
                String media=post.optString("mediaUrl",""),type=post.optString("mediaType","");
                if(!media.isEmpty()&&type.startsWith("image")){
                    ImageView photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);photo.setBackground(rounded(field(),dp(12)));photo.setClipToOutline(true);card.addView(photo,lp(dp(220),dp(8)));loadImage(photo,media);
                }else if(!media.isEmpty()&&type.startsWith("video")&&media.startsWith("http")){
                    android.widget.VideoView video=new android.widget.VideoView(this);video.setVideoURI(android.net.Uri.parse(media));card.addView(video,lp(dp(220),dp(8)));video.setOnPreparedListener(player->{player.setLooping(false);});video.setOnClickListener(v->video.start());
                }
                String created=post.optString("createdAt","");if(created.length()>=16)card.addView(tv(created.substring(0,16).replace("T"," "),10,muted()));
                feed.addView(card,lp(-2,dp(9)));
            }
            new AlertDialog.Builder(this).setTitle(c.optString("name","Community")+"  ✓").setView(scroll).setPositiveButton("Close",null).show();
        });
    }

    private void showChannelSettings(JSONObject channel){
        String id=channel.optString("id","");if(id.isEmpty()){message("Channel details are unavailable.");return;}
        String privacy=channel.optString("privacy","public").equals("private")?"Private · invite-only":"Public · discoverable";
        new AlertDialog.Builder(this).setTitle(channel.optString("name","Channel")).setMessage("@"+channel.optString("handle","")+"\n"+privacy+"\n\nLong-press a channel to manage members and admins.").setPositiveButton("Invite people",(d,w)->showChannelInvite(id)).setNeutralButton("Manage admins",(d,w)->showChannelAdminPicker(id)).setNegativeButton("Close",null).show();
    }
    private void showChannelInvite(String channelId){
        EditText query=input("Search people by name or @username");LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);ScrollView sc=new ScrollView(this);sc.addView(results);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.addView(query,lp(dp(50),dp(6)));wrap.addView(sc,new LinearLayout.LayoutParams(-1,dp(260)));
        query.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence x,int st,int c,int a){}public void onTextChanged(CharSequence x,int st,int before,int count){String q=x.toString().trim();if(q.length()<2)return;api("/search?q="+URLEncoder.encode(q,StandardCharsets.UTF_8),"GET",null,r->{if(!r.ok)return;results.removeAllViews();JSONArray users=r.data.optJSONArray("users");if(users==null)return;for(int i=0;i<users.length();i++){JSONObject u=users.optJSONObject(i);if(u==null||meId.equals(u.optString("id")))continue;Button item=btn(u.optString("displayName","User")+"  @"+u.optString("username",""),false);results.addView(item,lp(dp(48),dp(4)));item.setOnClickListener(v->{JSONObject payload=new JSONObject();try{JSONArray ids=new JSONArray();ids.put(u.optString("id"));payload.put("userIds",ids);}catch(Exception ignored){}api("/channels/"+channelId+"/members","POST",payload,res->{if(!res.ok)message(res.error);else message("Member added to channel.");});});}});}public void afterTextChanged(android.text.Editable e){}});
        new AlertDialog.Builder(this).setTitle("Invite to channel").setView(wrap).setNegativeButton("Close",null).show();
    }
    private void showChannelAdminPicker(String channelId){
        api("/channels/"+channelId+"/members","GET",null,r->{if(!r.ok){message(r.error);return;}JSONArray members=r.data.optJSONArray("members");if(members==null)return;ArrayList<JSONObject> candidates=new ArrayList<>();ArrayList<String> labels=new ArrayList<>();for(int i=0;i<members.length();i++){JSONObject m=members.optJSONObject(i),u=m==null?null:m.optJSONObject("user");if(u==null||"owner".equals(m.optString("role")))continue;candidates.add(m);labels.add(u.optString("displayName","Member")+" · "+m.optString("role","member"));}if(candidates.isEmpty()){message("Invite members before assigning admins.");return;}new AlertDialog.Builder(this).setTitle("Choose a channel member").setItems(labels.toArray(new String[0]),(d,which)->{JSONObject m=candidates.get(which),u=m.optJSONObject("user");String next=m.optString("role").equals("admin")?"member":"admin";new AlertDialog.Builder(this).setTitle(next.equals("admin")?"Make channel admin?":"Remove admin role?").setMessage(u.optString("displayName","This member")).setNegativeButton("Cancel",null).setPositiveButton("Confirm",(x,y)->{JSONObject payload=new JSONObject();try{payload.put("userId",u.optString("id"));payload.put("role",next);}catch(Exception ignored){}api("/channels/"+channelId+"/members","PATCH",payload,res->message(res.ok?(next.equals("admin")?"Admin role assigned.":"Admin role removed."):res.error));}).show();}).setNegativeButton("Close",null).show();});
    }
    private void loadCalls(){heading("Calls","Voice and video conversations.");LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(18),dp(18),dp(18));box.setBackground(rounded(card(),dp(22)));ImageView callIcon=new ImageView(this);callIcon.setImageResource(R.drawable.ic_call_phone);callIcon.setImageTintList(android.content.res.ColorStateList.valueOf(GREEN));box.addView(callIcon,new LinearLayout.LayoutParams(dp(42),dp(42)));TextView h=tv("Your calls, one tap away",20,text());h.setTypeface(null,1);box.addView(h,lp(-2,dp(8)));box.addView(tv("Open a chat and use the real voice or video call controls. Calls connect through secure LiveKit rooms.",13,muted()));body.addView(box,lp(-2,dp(12)));Button b=btn("Call settings",false);body.addView(b,new LinearLayout.LayoutParams(-1,dp(50)));b.setOnClickListener(v->showSettings());}

    private void loadProfile(){heading("Profile","Your OLUNAA identity");LinearLayout hero=new LinearLayout(this);hero.setGravity(Gravity.CENTER_VERTICAL);hero.setPadding(dp(16),dp(16),dp(16),dp(16));hero.setBackground(rounded(card(),dp(24)));ImageView av=avatar(meAvatar,1);hero.addView(av,new LinearLayout.LayoutParams(dp(82),dp(82)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(15),0,0,0);TextView n=tv(meName,22,text());n.setTypeface(null,1);info.addView(n);if(prefs.getBoolean("verified",false)){ImageView badge=new ImageView(this);badge.setImageResource(R.drawable.ic_verified_blue);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(20),dp(20));bp.topMargin=dp(3);info.addView(badge,bp);}info.addView(tv("@"+meUsername,13,GREEN));info.addView(tv("OLUNAA member",12,muted()));hero.addView(info,new LinearLayout.LayoutParams(0,dp(100),1));body.addView(hero,lp(-2,dp(12)));Button edit=btn("Edit profile",true);body.addView(edit,lp(dp(50),dp(8)));edit.setOnClickListener(v->editProfile());Button settings=btn("Settings & privacy",false);body.addView(settings,lp(dp(50),dp(8)));settings.setOnClickListener(v->showSettings());Button lock=btn(prefs.getBoolean("lock",false)?"Disable app lock":"Enable fingerprint / device lock",false);body.addView(lock,lp(dp(50),dp(8)));lock.setOnClickListener(v->{prefs.edit().putBoolean("lock",!prefs.getBoolean("lock",false)).apply();loadProfile();});Button logout=btn("Sign out",false);body.addView(logout,lp(dp(50),0));logout.setOnClickListener(v->logout());api("/auth/me","GET",null,r->{if(r.ok){JSONObject current=r.data.optJSONObject("user");if(current!=null)setUser(current);}});}
    private void editProfile(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(5),0,dp(5),0);EditText name=input("Display name");name.setText(meName);EditText user=input("Username");user.setText("@"+meUsername);EditText bio=input("Bio");box.addView(name,lp(dp(52),dp(8)));box.addView(user,lp(dp(52),dp(8)));box.addView(bio,lp(dp(52),0));new AlertDialog.Builder(this).setTitle("Edit profile").setView(box).setPositiveButton("Save",(d,w)->{JSONObject j=new JSONObject();try{j.put("displayName",name.getText().toString().trim());j.put("username",user.getText().toString().trim());j.put("bio",bio.getText().toString().trim());}catch(Exception ignored){}api("/profile","PATCH",j,r->{if(!r.ok){message(r.error);return;}setUser(r.data.optJSONObject("user"));showHome("Profile");});}).setNegativeButton("Cancel",null).show();}
    private void showSettings(){rootBase();LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);root.addView(shell,new LinearLayout.LayoutParams(-1,-1));LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView back=tv("‹",35,text());back.setGravity(Gravity.CENTER);head.addView(back,new LinearLayout.LayoutParams(dp(48),dp(55)));LinearLayout ht=new LinearLayout(this);ht.setOrientation(LinearLayout.VERTICAL);ht.addView(tv("Settings & Privacy",22,text()));ht.addView(tv("Make OLUNAA work your way",12,muted()));head.addView(ht);shell.addView(head,lp(dp(65),dp(4)));back.setOnClickListener(v->showHome("Profile"));ScrollView scroll=new ScrollView(this);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(0,dp(4),0,dp(22));scroll.addView(box);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));box.addView(settingTitle("PERSONALIZE"));addThemeRow(box,"Dark mode","OLED-friendly midnight appearance",0);addThemeRow(box,"Light mode","Bright and clean appearance",1);addThemeRow(box,"System default","Follow your Android theme",2);box.addView(settingTitle("PRIVACY & SECURITY"));addServerSwitch(box,"Show online status","Let contacts see when you are online","showOnlineStatus",true);addServerSwitch(box,"Show last seen","Share your last active time","showLastSeen",true);addServerSwitch(box,"Read receipts","Show read confirmation to others","readReceipts",true);addServerSwitch(box,"Typing indicators","Show when you are typing","typingIndicators",true);addServerSwitch(box,"Message requests","Allow messages from people you do not know","messageRequests",true);addLocalSwitch(box,"Fingerprint / device lock","Require device authentication to open OLUNAA","lock",false);addActionRow(box,"Notifications","Manage Android notification permission",this::requestNotifications);box.addView(settingTitle("CHATS & MEDIA"));addLocalSwitch(box,"Send with Enter","Use keyboard Enter to send messages","enterToSend",false);addLocalSwitch(box,"Save received media","Allow media to be saved to this device","saveMedia",false);addLocalSwitch(box,"Data saver","Reduce image and media data usage","dataSaver",false);addLocalSwitch(box,"Message sounds","Play message interaction sounds","messageSounds",true);addActionRow(box,"Chat text size","Choose comfortable message text size",()->{String[] sizes={"Small","Medium","Large","Extra large"};new AlertDialog.Builder(this).setTitle("Message text size").setSingleChoiceItems(sizes,prefs.getInt("chatTextSize",1),(d,w)->{prefs.edit().putInt("chatTextSize",w).apply();d.dismiss();message("Text size saved.");}).setNegativeButton("Close",null).show();});addActionRow(box,"Chat wallpaper","Choose the conversation background",()->{String[] names={"Midnight gradient","Electric blue","Ocean blue","Plain dark"};new AlertDialog.Builder(this).setTitle("Chat wallpaper").setSingleChoiceItems(names,prefs.getInt("wallpaper",0),(d,w)->{prefs.edit().putInt("wallpaper",w).apply();d.dismiss();message("Wallpaper preference saved.");}).setNegativeButton("Close",null).show();});box.addView(settingTitle("ACCOUNT"));addActionRow(box,"My profile","View and edit your OLUNAA profile",()->showHome("Profile"));addActionRow(box,"Notifications","View messages and account activity",()->showHome("Alerts"));addActionRow(box,"Edit profile","Change your name, username and profile details",this::editProfile);addActionRow(box,"Sign out","Sign out from this device",this::logout);box.addView(settingTitle("ABOUT OLUNAA"));LinearLayout about=new LinearLayout(this);about.setOrientation(LinearLayout.VERTICAL);about.setPadding(dp(16),dp(15),dp(16),dp(15));about.setBackground(rounded(card(),dp(20)));about.addView(tv("OLUNAA",19,GREEN));about.addView(tv("Native Android • Version 1.4.2\nPrivate chats • Updates • Channels • Communities • Voice & video calls",12,muted()));box.addView(about,lp(-2,dp(10)));}
    private void addServerSwitch(LinearLayout box,String title,String sub,String key,boolean def){LinearLayout row=preferenceRow(box,title,sub);Switch sw=new Switch(this);sw.setChecked(prefs.getBoolean("srv_"+key,def));row.addView(sw);final android.widget.CompoundButton.OnCheckedChangeListener[] listener=new android.widget.CompoundButton.OnCheckedChangeListener[1];listener[0]=(button,on)->{boolean previous=prefs.getBoolean("srv_"+key,def);sw.setEnabled(false);JSONObject j=new JSONObject();try{j.put(key,on);}catch(Exception ignored){}api("/settings","PATCH",j,r->{sw.setEnabled(true);if(!r.ok){sw.setOnCheckedChangeListener(null);sw.setChecked(previous);sw.setOnCheckedChangeListener(listener[0]);message("Could not save "+title+". Your previous setting was restored.");return;}prefs.edit().putBoolean("srv_"+key,on).apply();});};sw.setOnCheckedChangeListener(listener[0]);row.setOnClickListener(v->sw.setChecked(!sw.isChecked()));}
    private void addLocalSwitch(LinearLayout box,String title,String sub,String key,boolean def){LinearLayout row=preferenceRow(box,title,sub);Switch sw=new Switch(this);sw.setChecked(key.equals("lock")?prefs.getBoolean("lock",false):prefs.getBoolean("pref_"+key,def));row.addView(sw);sw.setOnCheckedChangeListener((v,on)->{if(key.equals("lock"))prefs.edit().putBoolean("lock",on).apply();else prefs.edit().putBoolean("pref_"+key,on).apply();if(key.equals("lock"))message(on?"App lock enabled":"App lock disabled");});row.setOnClickListener(v->sw.setChecked(!sw.isChecked()));}
    private LinearLayout preferenceRow(LinearLayout box,String title,String sub){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(8),dp(14),dp(8));row.setBackground(rounded(card(),dp(18)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);TextView t=tv(title,14,text());t.setTypeface(null,1);info.addView(t);info.addView(tv(sub,11,muted()));row.addView(info,new LinearLayout.LayoutParams(0,dp(56),1));box.addView(row,lp(dp(68),dp(6)));return row;}
    private TextView settingTitle(String s){TextView t=tv(s,12,GREEN);t.setTypeface(null,1);t.setPadding(dp(10),dp(18),0,dp(8));return t;}
    private void addThemeRow(LinearLayout box,String title,String sub,int mode){String current=prefs.getString("theme","dark");LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(10),dp(14),dp(10));row.setBackground(rounded(card(),dp(18)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(tv(title,16,text()));info.addView(tv(sub,12,muted()));row.addView(info,new LinearLayout.LayoutParams(0,dp(62),1));RadioButton radio=new RadioButton(this);radio.setChecked((mode==0&&current.equals("dark"))||(mode==1&&current.equals("light"))||(mode==2&&current.equals("system")));row.addView(radio);box.addView(row,lp(dp(70),dp(6)));row.setOnClickListener(v->{if(mode==0){prefs.edit().putString("theme","dark").apply();dark=true;}else if(mode==1){prefs.edit().putString("theme","light").apply();dark=false;}else{prefs.edit().putString("theme","system").apply();dark=(getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_NIGHT_MASK)==android.content.res.Configuration.UI_MODE_NIGHT_YES;}showSettings();});}
    private void addActionRow(LinearLayout box,String title,String sub,Runnable action){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(10),dp(14),dp(10));row.setBackground(rounded(card(),dp(18)));LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.addView(tv(title,16,text()));info.addView(tv(sub,12,muted()));row.addView(info,new LinearLayout.LayoutParams(0,dp(62),1));TextView arrow=tv("›",26,muted());arrow.setGravity(Gravity.CENTER);row.addView(arrow,new LinearLayout.LayoutParams(dp(35),dp(62)));box.addView(row,lp(dp(70),dp(6)));row.setOnClickListener(v->action.run());}
    private void showMenu(){String[] items={"Profile","Settings","Toggle dark/light","Sign out"};new AlertDialog.Builder(this).setTitle("OLUNAA").setItems(items,(d,w)->{if(w==0)showHome("Profile");else if(w==1)showSettings();else if(w==2){dark=!dark;prefs.edit().putString("theme",dark?"dark":"light").apply();showHome("Chats");}else logout();}).show();}
    private void empty(String h,String s){LinearLayout e=new LinearLayout(this);e.setOrientation(LinearLayout.VERTICAL);e.setGravity(Gravity.CENTER);e.setPadding(dp(25),dp(45),dp(25),dp(45));TextView i=tv("◉",50,GREEN);i.setGravity(Gravity.CENTER);e.addView(i,new LinearLayout.LayoutParams(-1,dp(65)));TextView a=tv(h,20,text());a.setTypeface(null,1);a.setGravity(Gravity.CENTER);e.addView(a);TextView b=tv(s,13,muted());b.setGravity(Gravity.CENTER);b.setPadding(0,dp(8),0,0);e.addView(b);body.addView(e);}

    private static class MediaEntry{Uri uri;boolean video;long date;MediaEntry(Uri u,boolean v,long d){uri=u;video=v;date=d;}}
    private void pickImage(){
        boolean chatMode=mediaMode==3;
        String[] actions=chatMode?new String[]{"⌖  Location","◎  Camera","▧  Gallery","▤  Document","☷  Poll"}:new String[]{"◎  Camera","▧  Gallery"};
        new AlertDialog.Builder(this).setTitle(chatMode?"Share with OLUNAA":"Choose a photo").setItems(actions,(dialog,which)->{
            if(!chatMode){if(which==0)openCamera();else pickFromDevice();return;}
            if(which==0)shareCurrentLocation();else if(which==1)openCamera();else if(which==2)pickFromDevice();else if(which==3)openDocumentPicker();else createPollMessage();
        }).setNegativeButton("Cancel",null).show();
    }
    private void openDocumentPicker(){try{Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("*/*");startActivityForResult(intent,REQ_DOCUMENT);}catch(Exception e){message("Could not open documents.");}}
    private void createPollMessage(){
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(8),dp(8),dp(8),0);
        EditText question=input("Ask a question"),first=input("Option 1"),second=input("Option 2");
        form.addView(question,lp(dp(52),dp(8)));form.addView(first,lp(dp(52),dp(8)));form.addView(second,lp(dp(52),dp(8)));
        new AlertDialog.Builder(this).setTitle("Create a poll").setView(form).setNegativeButton("Cancel",null).setPositiveButton("Send poll",(d,w)->{
            String q=question.getText().toString().trim(),a=first.getText().toString().trim(),b=second.getText().toString().trim();
            if(q.isEmpty()||a.isEmpty()||b.isEmpty()){message("Enter a question and at least two options.");return;}
            sendPollText("POLL: "+q+" | 1. "+a+" | 2. "+b);
        }).show();
    }
    private void sendPollText(String content){JSONObject payload=new JSONObject();try{payload.put("conversationId",activeConversation);payload.put("body",content);}catch(Exception ignored){}api("/messages","POST",payload,r->{if(!r.ok)message(r.error);else message("Shared in chat.");});}
    private void shareCurrentLocation(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ_LOCATION);return;}
        try{android.location.LocationManager manager=(android.location.LocationManager)getSystemService(LOCATION_SERVICE);android.location.Location location=null;try{location=manager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);}catch(Exception ignored){}if(location==null)try{location=manager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);}catch(Exception ignored){}
            if(location==null){message("Location is not available yet. Turn on Location and try again.");return;}
            sendPollText("Location: https://maps.google.com/?q="+location.getLatitude()+","+location.getLongitude());
        }catch(SecurityException e){message("Allow OLUNAA to access your location.");}
    }
    private String documentDataUrl(Uri uri){try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>2400000)return null;out.write(buffer,0,count);}String type=getContentResolver().getType(uri);if(type==null)type="application/octet-stream";return "data:"+type+";base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);}catch(Exception e){return null;}}
    private void sendDocument(String data,String name){JSONObject payload=new JSONObject();try{payload.put("conversationId",activeConversation);payload.put("body","Document: "+name);payload.put("mediaUrl",data);payload.put("mediaType","document");}catch(Exception ignored){}api("/messages","POST",payload,r->{if(!r.ok)message(r.error);else message("Document sent.");});}
    private void pickFromDevice(){String[] permissions=Build.VERSION.SDK_INT>=34?new String[]{"android.permission.READ_MEDIA_IMAGES","android.permission.READ_MEDIA_VIDEO","android.permission.READ_MEDIA_VISUAL_USER_SELECTED"}:Build.VERSION.SDK_INT>=33?new String[]{"android.permission.READ_MEDIA_IMAGES","android.permission.READ_MEDIA_VIDEO"}:new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};boolean granted=false;ArrayList<String> need=new ArrayList<>();for(String p:permissions){if(checkSelfPermission(p)==PackageManager.PERMISSION_GRANTED)granted=true;else need.add(p);}if(!granted&&!need.isEmpty()){requestPermissions(need.toArray(new String[0]),REQ_MEDIA_PERMISSION);return;}showMediaPicker();}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==REQ_MEDIA_PERMISSION){boolean ok=false;for(int g:grantResults)if(g==PackageManager.PERMISSION_GRANTED)ok=true;if(ok)showMediaPicker();else message("Allow Photos and videos access to choose media.");}else if(requestCode==REQ_LOCATION){boolean ok=false;for(int g:grantResults)if(g==PackageManager.PERMISSION_GRANTED)ok=true;if(ok)shareCurrentLocation();else message("Location permission was not granted.");}}
    private void showMediaPicker(){rootBase();LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);root.addView(page,new LinearLayout.LayoutParams(-1,-1));LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView back=tv("‹",34,text());back.setGravity(Gravity.CENTER);head.addView(back,new LinearLayout.LayoutParams(dp(48),dp(54)));LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.addView(tv(mediaMode==2?"Choose status media":"Choose a photo or video",20,text()));titles.addView(tv("Your recent photos and videos on this device",12,muted()));head.addView(titles);page.addView(head,lp(dp(62),dp(6)));back.setOnClickListener(v->showHome(mediaMode==2?"Updates":"Chats"));ScrollView scroll=new ScrollView(this);LinearLayout grid=new LinearLayout(this);grid.setOrientation(LinearLayout.VERTICAL);scroll.addView(grid);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));ArrayList<MediaEntry> entries=new ArrayList<>();loadMediaEntries(entries);if(mediaMode==1){for(int i=entries.size()-1;i>=0;i--)if(entries.get(i).video)entries.remove(i);}if(entries.isEmpty()){emptyPicker(grid);return;}for(int i=0;i<entries.size();i+=3){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);for(int k=i;k<Math.min(i+3,entries.size());k++){MediaEntry entry=entries.get(k);FrameLayout tile=new FrameLayout(this);ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);try{if(Build.VERSION.SDK_INT>=29)image.setImageBitmap(getContentResolver().loadThumbnail(entry.uri,new android.util.Size(dp(120),dp(140)),null));else image.setImageURI(entry.uri);}catch(Exception ignored){}tile.addView(image,new FrameLayout.LayoutParams(-1,-1));if(entry.video){TextView badge=tv("▶ VIDEO",10,Color.WHITE);badge.setPadding(dp(7),dp(4),dp(7),dp(4));badge.setBackground(rounded(Color.argb(190,0,0,0),dp(8)));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.LEFT);bp.setMargins(dp(6),0,0,dp(6));tile.addView(badge,bp);}LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(132),1);tp.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(tile,tp);tile.setOnClickListener(v->selectMedia(entry));}while(row.getChildCount()<3){View spacer=new View(this);row.addView(spacer,new LinearLayout.LayoutParams(0,dp(132),1));}grid.addView(row);} }
    private void emptyPicker(LinearLayout grid){TextView t=tv("No media found. Add photos or videos to your device, then try again.",14,muted());t.setGravity(Gravity.CENTER);grid.addView(t,lp(dp(130),dp(12)));}
    private void loadMediaEntries(ArrayList<MediaEntry> entries){loadMediaType(entries,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,false);loadMediaType(entries,MediaStore.Video.Media.EXTERNAL_CONTENT_URI,true);Collections.sort(entries,(a,b)->Long.compare(b.date,a.date));if(entries.size()>90)entries.subList(90,entries.size()).clear();}
    private void loadMediaType(ArrayList<MediaEntry> out,Uri base,boolean video){String[] cols={MediaStore.MediaColumns._ID,MediaStore.MediaColumns.DATE_ADDED};try(Cursor c=getContentResolver().query(base,cols,null,null,MediaStore.MediaColumns.DATE_ADDED+" DESC")){if(c==null)return;int id=c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID),date=c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED);while(c.moveToNext()&&out.size()<180){long n=c.getLong(id),d=date>=0?c.getLong(date):0;Uri u=ContentUris.withAppendedId(base,n);out.add(new MediaEntry(u,video,d));}}catch(Exception ignored){}}
    private void selectMedia(MediaEntry entry){io.execute(()->{String dataUrl=entry.video?toVideoDataUrl(entry.uri):toDataUrl(entry.uri);main.post(()->{if(dataUrl==null){message(entry.video?"Video is too large. Choose a short clip under 2.5 MB.":"Could not read this media.");return;}if(mediaMode==1){JSONObject j=new JSONObject();try{j.put("avatarUrl",dataUrl);}catch(Exception ignored){}api("/profile","PATCH",j,r->{if(r.ok){setUser(r.data.optJSONObject("user"));showHome("Profile");}else message(r.error);});}else if(mediaMode==2)postStatus(dataUrl,entry.video);else if(mediaMode==3)sendMedia(dataUrl,entry.video);});});}
    private void openCamera(){try{Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);if(i.resolveActivity(getPackageManager())==null){message("No camera app is available.");return;}startActivityForResult(i,REQ_CAMERA);}catch(Exception e){message("Could not open camera.");}}
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);if(requestCode==REQ_DOCUMENT&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();String name=uri.getLastPathSegment()==null?"Document":uri.getLastPathSegment();io.execute(()->{String encoded=documentDataUrl(uri);main.post(()->{if(encoded==null)message("Document is too large. Choose a file under 2.4 MB.");else sendDocument(encoded,name);});});return;}if(requestCode==REQ_CAMERA&&resultCode==RESULT_OK&&data!=null){Object extra=data.getExtras()==null?null:data.getExtras().get("data");if(!(extra instanceof Bitmap)){message("Camera did not return a photo.");return;}Bitmap bitmap=(Bitmap)extra;ByteArrayOutputStream out=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,88,out);String image="data:image/jpeg;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);if(mediaMode==1){JSONObject j=new JSONObject();try{j.put("avatarUrl",image);}catch(Exception ignored){}api("/profile","PATCH",j,r->{if(r.ok){setUser(r.data.optJSONObject("user"));showHome("Profile");}else message(r.error);});}else if(mediaMode==2)postStatus(image,false);else if(mediaMode==3)sendMedia(image,false);}}
    private String toDataUrl(Uri uri){try{Bitmap b=BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));if(b==null)return null;int max=1280;float scale=Math.min(1f,Math.min((float)max/b.getWidth(),(float)max/b.getHeight()));if(scale<1f)b=Bitmap.createScaledBitmap(b,Math.max(1,(int)(b.getWidth()*scale)),Math.max(1,(int)(b.getHeight()*scale)),true);ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.JPEG,82,out);return "data:image/jpeg;base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);}catch(Exception e){return null;}}
    private VideoView videoPreview(String data){VideoView player=new VideoView(this);player.setBackground(rounded(Color.BLACK,dp(18)));io.execute(()->{try{String b64=data.substring(data.indexOf(",")+1);byte[] bytes=android.util.Base64.decode(b64,android.util.Base64.DEFAULT);File file=File.createTempFile("olunaa-video-",".mp4",getCacheDir());try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);}main.post(()->{player.setVideoURI(Uri.fromFile(file));player.setOnPreparedListener(mp->{mp.setLooping(true);player.start();});});}catch(Exception e){message("Could not play this video.");}});return player;}
    private String toVideoDataUrl(Uri uri){try(InputStream in=getContentResolver().openInputStream(uri);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>2400000)return null;out.write(buf,0,n);}String type=getContentResolver().getType(uri);if(type==null||!type.startsWith("video/"))type="video/mp4";return "data:"+type+";base64,"+android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);}catch(Exception e){return null;}}
    private void postStatus(String media,boolean video){EditText cap=input("Caption (optional)");String[] expiry={"24 hours (Normal)","1 hour","6 hours","12 hours","48 hours","3 days","7 days"};int[] hours={24,1,6,12,48,72,168};final int[] selected={0};LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.addView(cap);Spinner duration=new Spinner(this);duration.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,expiry));duration.setSelection(0);panel.addView(tv("How long should this status stay visible?",12,muted()));panel.addView(duration);new AlertDialog.Builder(this).setTitle("New "+(video?"video":"photo")+" status").setView(panel).setPositiveButton("Post",(d,w)->{JSONObject j=new JSONObject();try{j.put("mediaUrl",media);j.put("caption",cap.getText().toString().trim());j.put("expiresHours",hours[duration.getSelectedItemPosition()]);}catch(Exception ignored){}api("/status","POST",j,r->{if(!r.ok)message(r.error);else{message("Status posted.");showHome("Updates");}});}).setNegativeButton("Cancel",null).show();}
    private void sendMedia(String media,boolean video){JSONObject j=new JSONObject();try{j.put("conversationId",activeConversation);j.put("body","");j.put("mediaUrl",media);j.put("mediaType",video?"video":"image");}catch(Exception ignored){}api("/messages","POST",j,r->{if(!r.ok)message(r.error);else message(video?"Video sent.":"Photo sent.");});}
    private void logout(){stopPoll();stopChatRealtime();try{if(realtimeClient!=null)realtimeClient.close();}catch(Exception ignored){}realtimeClient=null;api("/auth/logout","POST",new JSONObject(),r->{prefs.edit().clear().apply();meId=meUsername=meName=meAvatar="";showAuth(false);});}
    private void biometric(){BiometricManager bm=BiometricManager.from(this);if(bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL)==BiometricManager.BIOMETRIC_SUCCESS){BiometricPrompt p=new BiometricPrompt(this,ContextCompat.getMainExecutor(this),new BiometricPrompt.AuthenticationCallback(){@Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r){restoreUser();showHome("Chats");}@Override public void onAuthenticationFailed(){message("Authentication failed.");}});p.authenticate(new BiometricPrompt.PromptInfo.Builder().setTitle("Unlock OLUNAA").setSubtitle("Use fingerprint or device credentials").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG|BiometricManager.Authenticators.DEVICE_CREDENTIAL).build());}else{restoreUser();showHome("Chats");}}
    private void stopPoll(){if(poller!=null){main.removeCallbacks(poller);poller=null;}if(presencePoller!=null){main.removeCallbacks(presencePoller);presencePoller=null;}if(typingDebounce!=null){main.removeCallbacks(typingDebounce);typingDebounce=null;}}
    private static class Result{boolean ok;String error;JSONObject data;Result(boolean o,String e,JSONObject d){ok=o;error=e;data=d;}}
    private interface Callback{void done(Result r);}
    private void api(String path,String method,JSONObject body,Callback cb){io.execute(()->{Result res;try{HttpURLConnection c=(HttpURLConnection)new URL(API+path).openConnection();c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setDoInput(true);c.setRequestProperty("Accept","application/json");String cookies=prefs.getString("cookies","");if(!cookies.isEmpty())c.setRequestProperty("Cookie",cookies);if(body!=null&&!method.equals("GET")){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}int code=c.getResponseCode();String set=c.getHeaderField("Set-Cookie");if(set!=null)saveCookie(set);InputStream in=code>=400?c.getErrorStream():c.getInputStream();String raw=read(in);c.disconnect();JSONObject data=raw.isEmpty()?new JSONObject():new JSONObject(raw);res=new Result(code>=200&&code<300,data.optString("error","Request failed ("+code+")"),data);}catch(Exception e){res=new Result(false,"Connection error. Check your internet and try again.",new JSONObject());}Result rr=res;main.post(()->cb.done(rr));});}
    private String read(InputStream in)throws Exception{if(in==null)return "";BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();String x;while((x=r.readLine())!=null)s.append(x);r.close();return s.toString();}
    private void saveCookie(String set){String cookie=set.split(";",2)[0];String old=prefs.getString("cookies","");Map<String,String> m=new LinkedHashMap<>();for(String x:old.split(";")){if(x.trim().isEmpty()||!x.contains("="))continue;String[] p=x.split("=",2);m.put(p[0].trim(),p[1]);}if(cookie.contains("=")){String[] p=cookie.split("=",2);m.put(p[0].trim(),p[1]);}StringBuilder out=new StringBuilder();for(Map.Entry<String,String> e:m.entrySet()){if(out.length()>0)out.append("; ");out.append(e.getKey()).append("=").append(e.getValue());}prefs.edit().putString("cookies",out.toString()).apply();}
}
