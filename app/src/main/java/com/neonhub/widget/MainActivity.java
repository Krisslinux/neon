package com.neonhub.widget;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.graphics.Color;
import android.widget.*;
public class MainActivity extends Activity {
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  LinearLayout ll=new LinearLayout(this);ll.setOrientation(1);ll.setPadding(40,55,40,30);ll.setBackgroundColor(Color.rgb(9,9,14));
  TextView heading=new TextView(this);heading.setText("✦ NEON HUB");heading.setTextColor(Color.rgb(198,255,0));heading.setTextSize(30);ll.addView(heading);
  TextView info=new TextView(this);info.setText("\nAI • Battery • Bluetooth • Music\n\nLong press your home screen, choose Widgets, then select Neon Hub or one of its four individual widgets.\n\nMusic controls need Notification access. Bluetooth status needs Nearby devices permission on Android 12+.");info.setTextColor(Color.WHITE);info.setTextSize(17);ll.addView(info);
  Button bt=new Button(this);bt.setText("Allow Bluetooth status");ll.addView(bt);bt.setOnClickListener(v->{if(Build.VERSION.SDK_INT>=31&&checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.BLUETOOTH_CONNECT},1);else startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));});
  Button music=new Button(this);music.setText("Allow music controls");ll.addView(music);music.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
  Button refresh=new Button(this);refresh.setText("Refresh all widgets");ll.addView(refresh);refresh.setOnClickListener(v->WidgetReceiver.updateAll(this));
  ScrollView sc=new ScrollView(this);sc.addView(ll);setContentView(sc);
 }
}