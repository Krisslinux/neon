package com.neonhub.widget;

import android.Manifest;
import android.app.*;
import android.appwidget.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.MediaMetadata;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.widget.RemoteViews;
import java.util.*;

public class WidgetReceiver extends AppWidgetProvider {
  static final String PKG="com.neonhub.widget";
  static final int LIME=0xffc6ff00;
  static String[] titles={"ChatGPT","Gemini","Grok","Perplexity"};
  static String[] packages={"com.openai.chatgpt","com.google.android.apps.bard","ai.x.grok","ai.perplexity.app.android"};
  static String[] urls={"https://chatgpt.com","https://gemini.google.com","https://grok.com","https://www.perplexity.ai"};
  static int[] ids={R.id.chatgpt,R.id.gemini,R.id.grok,R.id.perplexity};
  @Override public void onUpdate(Context c,AppWidgetManager m,int[] awids){for(int id:awids)render(c,m,id,this.getClass());}
  @Override public void onReceive(Context c,Intent i){super.onReceive(c,i); if(i!=null && !AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(i.getAction()))updateAll(c);}
  static void updateAll(Context c) {
    AppWidgetManager m=AppWidgetManager.getInstance(c);
    Class<?>[] classes={WidgetReceiver.class,HubAiReceiver.class,HubBatteryReceiver.class,HubBluetoothReceiver.class,HubMusicReceiver.class};
    for(Class<?> cl:classes){int[] ids=m.getAppWidgetIds(new ComponentName(c,cl)); for(int id:ids)render(c,m,id,cl);}
  }
  static PendingIntent pending(Context c,String action,int req){
    Intent i=new Intent(c,ActionReceiver.class).setAction("neonhub."+action).putExtra("cmd",action);
    return PendingIntent.getBroadcast(c,req,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  }
  static RemoteViews view(Context c,Class<?> widgetClass) {
    RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.widget);
    boolean hub=widgetClass==WidgetReceiver.class;
    boolean ai=widgetClass==HubAiReceiver.class;
    boolean bat=widgetClass==HubBatteryReceiver.class;
    boolean bt=widgetClass==HubBluetoothReceiver.class;
    boolean music=widgetClass==HubMusicReceiver.class;
    v.setTextViewText(R.id.title,"✦  "+(hub?"NEON HUB":ai?"AI LAUNCHER":bat?"BATTERY":bt?"BLUETOOTH":"MUSIC"));
    v.setViewVisibility(R.id.ai_section,(hub||ai)?0:8);
    v.setViewVisibility(R.id.status_section,(hub||bat||bt)?0:8);
    v.setViewVisibility(R.id.music_section,(hub||music)?0:8);
    for(int n=0;n<4;n++) v.setOnClickPendingIntent(ids[n],pending(c,"ai"+n,100+n));
    if(bat) v.setViewVisibility(R.id.bluetooth,8);
    if(bt) v.setViewVisibility(R.id.battery,8);
    Intent intent=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    int level=intent!=null?intent.getIntExtra(BatteryManager.EXTRA_LEVEL,-1):-1;
    int scale=intent!=null?intent.getIntExtra(BatteryManager.EXTRA_SCALE,-1):-1;
    int percent=scale>0&&level>=0?(level*100/scale):0;
    int status=intent!=null?intent.getIntExtra(BatteryManager.EXTRA_STATUS,0):0;
    v.setTextViewText(R.id.battery,"BATTERY\n"+percent+"%"+(status==BatteryManager.BATTERY_STATUS_CHARGING?" ⚡":""));
    v.setOnClickPendingIntent(R.id.battery,pending(c,"settings",210));
    String bluetooth="BLUETOOTH\n";
    try {
      if(Build.VERSION.SDK_INT>=31 && c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED) bluetooth+="Allow access";
      else {
        BluetoothManager bm=(BluetoothManager)c.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter ba=bm!=null?bm.getAdapter():null;
        bluetooth+=ba==null?"Unavailable":ba.isEnabled()?"ON":"OFF";
      }
    } catch(Exception e){bluetooth+="Unavailable";}
    v.setTextViewText(R.id.bluetooth,bluetooth);
    v.setOnClickPendingIntent(R.id.bluetooth,pending(c,"bluetooth",211));
    MediaController ctl=controller(c);
    String song="♫ Enable music access";
    if(ctl!=null){
      MediaMetadata md=ctl.getMetadata();
      String title=md!=null?md.getString(MediaMetadata.METADATA_KEY_TITLE):null;
      song="♫ "+(title!=null&&!title.isEmpty()?title:"Now playing");
    }
    v.setTextViewText(R.id.song,song);
    v.setOnClickPendingIntent(R.id.song,pending(c,"settings",212));
    v.setOnClickPendingIntent(R.id.prev,pending(c,"previous",213));
    v.setOnClickPendingIntent(R.id.play,pending(c,"toggle",214));
    v.setOnClickPendingIntent(R.id.next,pending(c,"next",215));
    return v;
  }
  static MediaController controller(Context c){
    try{
      String enabled=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");
      if(enabled==null||!enabled.contains(c.getPackageName()))return null;
      MediaSessionManager m=(MediaSessionManager)c.getSystemService(Context.MEDIA_SESSION_SERVICE);
      List<MediaController> ls=m.getActiveSessions(new ComponentName(c,MediaListener.class));
      for(MediaController item:ls)if(item.getPlaybackState()!=null&&item.getPlaybackState().getState()==PlaybackState.STATE_PLAYING)return item;
      return ls.isEmpty()?null:ls.get(0);
    }catch(Exception e){return null;}
  }
  static void render(Context c,AppWidgetManager m,int id,Class<?> cl){m.updateAppWidget(id,view(c,cl));}
}
class AiReceiver extends WidgetReceiver{}
class BatteryReceiver extends WidgetReceiver{}
class BluetoothReceiver extends WidgetReceiver{}
class MusicReceiver extends WidgetReceiver{}
class ActionReceiver extends android.content.BroadcastReceiver {
  @Override public void onReceive(Context c,Intent i) {
    String a=i.getStringExtra("cmd"); if(a==null)return;
    try {
      if(a.startsWith("ai")){
        int n=Integer.parseInt(a.substring(2));
        Intent launch=c.getPackageManager().getLaunchIntentForPackage(WidgetReceiver.packages[n]);
        if(launch==null)launch=new Intent(Intent.ACTION_VIEW,Uri.parse(WidgetReceiver.urls[n]));
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(launch);
      }else if(a.equals("bluetooth")){
        Intent open=new Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(open);
      }else if(a.equals("settings")){
        Intent open=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);c.startActivity(open);
      }else {
        MediaController ctl=WidgetReceiver.controller(c);
        if(ctl!=null) {
          MediaController.TransportControls tc=ctl.getTransportControls();
          if(a.equals("next"))tc.skipToNext();
          else if(a.equals("previous"))tc.skipToPrevious();
          else if(a.equals("toggle")){
            if(ctl.getPlaybackState()!=null&&ctl.getPlaybackState().getState()==PlaybackState.STATE_PLAYING)tc.pause();else tc.play();
          }
        }
      }
    } catch(Exception e){}
    WidgetReceiver.updateAll(c);
  }
}
class MediaListener extends NotificationListenerService {
  @Override public void onListenerConnected(){WidgetReceiver.updateAll(this);}
  @Override public void onNotificationPosted(android.service.notification.StatusBarNotification sbn){WidgetReceiver.updateAll(this);}
  @Override public void onNotificationRemoved(android.service.notification.StatusBarNotification sbn){WidgetReceiver.updateAll(this);}
}
