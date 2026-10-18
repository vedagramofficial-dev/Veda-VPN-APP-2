package com.veda.vpn;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;

public class MainActivity extends Activity {
    private static final String SUBSCRIPTION_URL = "https://veda.beer/free/ph7-MihMDNSw8ta7";
    private static final String CHANNEL_URL = "https://t.me/vedavpn";
    private static final String FORUM_URL = "https://t.me/vedavpnforum";
    private static final String BOT_URL = "https://t.me/vedavpn_bot";
    // Change this to the official support URL if the provider gives a separate one.
    private static final String SUPPORT_URL = "https://t.me/vedavpn_bot";
    private final int bg = Color.rgb(20, 13, 30), panel = Color.rgb(38, 25, 52), purple = Color.rgb(132, 72, 190);
    private LinearLayout list, root;
    private TextView status, lastUpdated, subscriptionInfo;
    private final ExecutorService pool = Executors.newFixedThreadPool(8);
    private final List<Node> nodes = new ArrayList<>();
    private boolean loading = false;

    static class Node {
        String name, host, raw, protocol, transport, security; int port; long ping = -1;
        Node(String n,String h,int p,String r,String proto,String trans,String sec){
            name=n;host=h;port=p;raw=r;protocol=proto;transport=trans;security=sec;
        }
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(bg); getWindow().setNavigationBarColor(bg);
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,26,20,28); root.setBackgroundColor(bg); scroll.addView(root);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logoImage = new ImageView(this);
        logoImage.setImageResource(com.veda.vpn.R.drawable.veda_vpn_logo);
        logoImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(52,52);
        logoParams.rightMargin = 10;
        header.addView(logoImage, logoParams);
        TextView logo = text("VEDA VPN", 23, Color.rgb(232,213,255), true);
        header.addView(logo,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(iconButton("↻", "Обновить подписку", v -> loadSubscription()));
        header.addView(iconButton("◉", "Проверить Ping", v -> checkPings()));
        header.addView(iconButton("⋯", "Открыть меню", v -> showMenu()));
        root.addView(header);
        root.addView(gap(10));
        LinearLayout infoCard = new LinearLayout(this); infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(15,12,15,12); infoCard.setBackground(round(panel,18));
        root.addView(infoCard);
        infoCard.addView(linkText("📢 Канал: t.me/vedavpn", CHANNEL_URL));
        infoCard.addView(linkText("💬 Форум: t.me/vedavpnforum", FORUM_URL));
        infoCard.addView(linkText("🤖 Бот: t.me/vedavpn_bot", BOT_URL));
        subscriptionInfo = text("🔄 Обновите подписку, если пропало соединение",14,Color.rgb(205,194,219),false);
        infoCard.addView(subscriptionInfo);
        infoCard.addView(text("♻️ Автообновление серверов: каждые 30 мин (пока приложение открыто)",13,Color.rgb(205,194,219),false));
        lastUpdated = text("🕒 Обновлено: ещё не обновлялось",13,Color.rgb(205,194,219),false);
        infoCard.addView(lastUpdated);
        TextView support = linkText("ⓘ Поддержка", SUPPORT_URL);
        support.setTextColor(Color.rgb(221,200,255)); support.setPadding(0,12,0,4); infoCard.addView(support);

        TextView section = text("Серверы",21,Color.WHITE,true);
        LinearLayout.LayoutParams secp=new LinearLayout.LayoutParams(-1,-2); secp.topMargin=22; root.addView(section,secp);
        status = text("Загрузка подписки…",13,Color.LTGRAY,false); root.addView(status);
        Button ping = button("◉  Проверить Ping всех серверов");
        ping.setTextSize(16);
        ping.setMinHeight((int)(54 * getResources().getDisplayMetrics().density));
        ping.setCompoundDrawablePadding(10);
        ping.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(111, 43, 190)));
        root.addView(ping);
        ping.setOnClickListener(v -> checkPings());
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2); lp.topMargin=14; root.addView(list,lp);
        root.addView(text("Информация о серверах загружается из вашей подписки.",12,Color.rgb(160,143,177),false));
        setContentView(scroll);
        loadSubscription();
        // Auto-refresh while the screen is active.
        root.postDelayed(autoRefresh, 30L * 60L * 1000L);
    }
    private final Runnable autoRefresh = new Runnable() {
        @Override public void run() {
            if (!isFinishing()) { loadSubscription(); root.postDelayed(this, 30L*60L*1000L); }
        }
    };
    TextView text(String s,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,5,0,5);return t;
    }
    GradientDrawable round(int color,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius);return d;
    }
    View gap(int h){ View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,h));return v; }
    Button iconButton(String s,String desc,View.OnClickListener listener) {
        Button b=new Button(this);b.setText(s);b.setTextSize(19);b.setContentDescription(desc);b.setAllCaps(false);
        b.setTextColor(Color.rgb(232,213,255));b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel));
        b.setOnClickListener(listener);return b;
    }
    TextView linkText(String s,String url) {
        TextView t=text(s,14,Color.rgb(205,194,219),false);
        t.setOnClickListener(v -> {try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));}catch(Exception ignored){}});
        return t;
    }
    Button button(String s) {
        Button b=new Button(this);b.setText(s);b.setTextColor(Color.WHITE);b.setAllCaps(false);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(purple));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=12;b.setLayoutParams(p);return b;
    }
    void loadSubscription() {
        if(loading)return; loading=true; status.setText("Загрузка подписки…");
        pool.execute(() -> {
            HttpURLConnection c=null;
            try {
                c=(HttpURLConnection)new URL(SUBSCRIPTION_URL).openConnection();
                c.setConnectTimeout(15000);c.setReadTimeout(15000);c.setInstanceFollowRedirects(true);
                c.setRequestProperty("User-Agent","VEDA-VPN-Android/0.2");
                StringBuilder body=new StringBuilder();
                try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream(),StandardCharsets.UTF_8))){
                    String line;while((line=r.readLine())!=null)body.append(line.trim()).append('\n');
                }
                String rawBody=body.toString();
                List<Node> parsed=parse(rawBody);
                String description=extractDescription(rawBody);
                runOnUiThread(() -> {
                    nodes.clear();nodes.addAll(parsed);render();
                    status.setText(nodes.size()+" серверов загружено");
                    lastUpdated.setText("🕒 Обновлено: "+new SimpleDateFormat("dd.MM.yy HH:mm",Locale.getDefault()).format(new Date()));
                    if(!description.isEmpty()) subscriptionInfo.setText(description);
                    else subscriptionInfo.setText("🔄 Обновите подписку, если пропало соединение");
                    loading=false;
                });
            } catch(Exception e) {
                runOnUiThread(() -> {status.setText("Не удалось загрузить подписку. Проверьте интернет или ссылку.");loading=false;});
            } finally {if(c!=null)c.disconnect();}
        });
    }
    String extractDescription(String body) {
        // If the subscription is plain text, display non-VLESS descriptive lines from it.
        for(String line:body.split("\\n")) {
            String s=line.trim();
            if(s.isEmpty() || s.startsWith("vless://") || s.startsWith("vmess://") || s.startsWith("trojan://") || s.startsWith("ss://")) continue;
            if(s.startsWith("#") || s.startsWith("//")) continue;
            if(s.length()>4 && s.length()<500 && !s.matches("^[A-Za-z0-9+/=]+$")) return s;
        }
        return "";
    }
    List<Node> parse(String body) {
        List<Node> out=new ArrayList<>();
        String decoded=body;
        // Some subscription services return Base64-encoded lines.
        if(!body.contains("://")) {
            try { decoded=new String(android.util.Base64.decode(body.trim(),android.util.Base64.DEFAULT),StandardCharsets.UTF_8); }
            catch(Exception ignored) {}
        }
        for(String line:decoded.split("[\\r\\n\\s]+")) {
            if(!line.startsWith("vless://"))continue;
            try {
                URI u=URI.create(line);String host=u.getHost();int port=u.getPort();
                String name=u.getFragment()==null?host:URLDecoder.decode(u.getFragment(),"UTF-8");
                String query=u.getRawQuery()==null?"":u.getRawQuery().toLowerCase(Locale.ROOT);
                String trans=query.contains("type=xhttp")?"XHTTP":query.contains("type=ws")?"WS":query.contains("type=grpc")?"gRPC":query.contains("type=tcp")?"TCP":"VLESS";
                String sec=query.contains("security=reality")?"REALITY":query.contains("security=tls")?"TLS":"";
                if(host!=null&&port>0)out.add(new Node(name,host,port,line,"VLESS",trans,sec));
            }catch(Exception ignored){}
        }
        return out;
    }
    void checkPings() {
        if(nodes.isEmpty()){status.setText("Серверы не найдены. Обновите подписку.");return;}
        status.setText("Проверяем Ping всех серверов…");
        // Reset old results so the UI clearly reflects the current test.
        for (Node n : nodes) n.ping = -1;
        render();

        List<Node> snapshot = new ArrayList<>(nodes);
        CountDownLatch latch = new CountDownLatch(snapshot.size());
        for(Node n : snapshot) {
            pool.execute(() -> {
                long start = System.nanoTime();
                try(Socket s = new Socket()) {
                    s.connect(new InetSocketAddress(n.host,n.port),3500);
                    n.ping = (System.nanoTime()-start)/1_000_000;
                } catch(Exception e) {
                    n.ping = -1;
                } finally {
                    latch.countDown();
                }
            });
        }
        pool.execute(() -> {
            try { latch.await(12,TimeUnit.SECONDS); }
            catch(InterruptedException ignored) { Thread.currentThread().interrupt(); }
            // Sort successful measurements first, ascending by ping; unreachable servers go last.
            synchronized(nodes) {
                Collections.sort(nodes,(a,b)->{
                    if(a.ping < 0 && b.ping < 0) return 0;
                    if(a.ping < 0) return 1;
                    if(b.ping < 0) return -1;
                    return Long.compare(a.ping,b.ping);
                });
            }
            runOnUiThread(() -> {
                render();
                long reachable = nodes.stream().filter(n -> n.ping >= 0).count();
                status.setText("Серверы отсортированы по Ping • доступно: " + reachable + " / " + nodes.size());
            });
        });
    }
    void render() {
        list.removeAllViews();
        if(nodes.isEmpty()){list.addView(text("В подписке не найдено ссылок VLESS. Возможно, формат требует дополнительной обработки.",14,Color.LTGRAY,false));return;}
        for(Node n:nodes) {
            LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(14,10,14,10);card.setBackground(round(panel,16));
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=9;list.addView(card,cp);
            TextView flag=text(flagFor(n.name),25,Color.WHITE,false);card.addView(flag);
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(10,0,8,0);
            TextView name=text(n.name,15,Color.WHITE,true);name.setMaxLines(2);info.addView(name);
            String detail=n.protocol+" · "+n.transport+(n.security.isEmpty()?"":" · "+n.security);
            info.addView(text(detail,12,Color.rgb(170,196,224),false));
            card.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            TextView value=text(n.ping<0?"— ms":n.ping+" ms",15,n.ping<0?Color.LTGRAY:Color.rgb(125,230,176),true);card.addView(value);
        }
    }
    String flagFor(String name) {
        String s=name.toLowerCase(Locale.ROOT);
        if(s.contains("эстони")||s.contains("estonia"))return "🇪🇪";
        if(s.contains("герман")||s.contains("german"))return "🇩🇪";
        if(s.contains("гонконг")||s.contains("hong"))return "🇭🇰";
        if(s.contains("итал")||s.contains("ital"))return "🇮🇹";
        if(s.contains("швец")||s.contains("swed"))return "🇸🇪";
        if(s.contains("росси")||s.contains("russia"))return "🇷🇺";
        if(s.contains("литв")||s.contains("lith"))return "🇱🇹";
        if(s.contains("финлянд")||s.contains("finland"))return "🇫🇮";
        if(s.contains("нидер")||s.contains("nether"))return "🇳🇱";
        if(s.contains("франц")||s.contains("france"))return "🇫🇷";
        return "🌐";
    }
    void showMenu() {
        String[] items={"Обновить подписку","Проверить Ping","Канал","Форум","Бот","Поддержка"};
        new AlertDialog.Builder(this).setTitle("VEDA VPN").setItems(items,(d,w)->{
            if(w==0)loadSubscription(); else if(w==1)checkPings();
            else if(w==2)open(CHANNEL_URL);else if(w==3)open(FORUM_URL);else if(w==4)open(BOT_URL);else open(SUPPORT_URL);
        }).show();
    }
    void open(String url){try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception ignored){}}
    @Override protected void onDestroy(){super.onDestroy();root.removeCallbacks(autoRefresh);pool.shutdownNow();}
}
