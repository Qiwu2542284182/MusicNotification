package me.qiwu.MusicNotification;

import android.app.AndroidAppHelper;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.app.NotificationCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.graphics.Palette;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;

import org.michaelevans.colorart.library.ColorArt;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static android.R.attr.selectableItemBackground;


/**
 * Created by Deng on 2019/2/18.
 */

public class NotificationHook {

    public void init(){
        XposedBridge.hookAllMethods(Notification.Builder.class, "build", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Notification notification = (Notification)param.getResult();
                if (isMediaNotification(notification)){
                    Bundle extras = NotificationCompat.getExtras(notification);
                    String title = extras.getString(NotificationCompat.EXTRA_TITLE, "未知音乐");
                    String subtitle = extras.getString(NotificationCompat.EXTRA_TEXT, "未知艺术家");
                    RemoteViews remoteViews = getContentView(title,subtitle,notification);
                    int resId = getIconId(notification)!= -1 ? getIconId(notification) : android.R.drawable.ic_dialog_info;
                    NotificationCompat.Builder builder= new NotificationCompat.Builder(getContext())
                            .setSmallIcon(resId)
                            .setContentTitle(title)
                            .setContentText(subtitle)
                            .setCategory(NotificationCompat.CATEGORY_STATUS)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setOngoing(true)
                            .setContent(remoteViews)
                            .setCustomBigContentView(remoteViews)
                            .setCustomContentView(remoteViews)
                            .setContentIntent(notification.contentIntent)
                            .setDeleteIntent(notification.deleteIntent);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                        builder.setPriority(NotificationManager.IMPORTANCE_MAX);
                    } else {
                        builder.setPriority(Notification.PRIORITY_MAX);
                    }
                    Notification newNotification = builder.build();
                    param.setResult(newNotification);


                }
            }
        });

    }

    private RemoteViews getContentView(String title,String subtitle,Notification notification){
        int backgroundColor = Color.BLACK;
        int textColor = Color.WHITE;
        if (notification.getLargeIcon()!=null){
            Bitmap bitmap = getLargeIcon(notification);
            int[] colors = ColorUtil.getColor(bitmap);
            backgroundColor = colors[0];
            textColor = colors[1];
        }
        RemoteViews remoteViews = new RemoteViews(getMoudleContext(getContext()).getPackageName(),R.layout.notifition_layout);

        remoteViews.setTextViewText(R.id.appName,getContext().getPackageManager().getApplicationLabel(AndroidAppHelper.currentApplicationInfo()));
        remoteViews.setTextViewText(R.id.title,title);
        remoteViews.setTextViewText(R.id.subtitle,subtitle);
        remoteViews.setImageViewIcon(R.id.smallIcon,notification.getSmallIcon());
        remoteViews.setTextColor(R.id.appName,textColor);
        remoteViews.setTextColor(R.id.title,textColor);
        remoteViews.setTextColor(R.id.subtitle,textColor);
        remoteViews.setImageViewIcon(R.id.largeIcon,notification.getLargeIcon());
        remoteViews.setInt(R.id.smallIcon,"setColorFilter",textColor);
        remoteViews.setInt(R.id.foregroundImage,"setColorFilter", backgroundColor);
        remoteViews.setInt(R.id.background, "setBackgroundColor", backgroundColor);
        TypedArray typedArray = getContext().obtainStyledAttributes(new int[]{android.R.attr.selectableItemBackground});
        int selectableItemBackground = typedArray.getResourceId(0, 0);
        typedArray.recycle();
        if (NotificationCompat.getActionCount(notification)>0){
            for (int i = 0;i<NotificationCompat.getActionCount(notification);i++){
                int id = getMoudleContext(getContext()).getResources().getIdentifier("ic_"+String.valueOf(i),"id",BuildConfig.APPLICATION_ID);
                NotificationCompat.Action action = NotificationCompat.getAction(notification,i);
                remoteViews.setViewVisibility(id, View.VISIBLE);
                remoteViews.setImageViewBitmap(id, BitmapFactory.decodeResource(getContext().getResources(),action.getIcon()));
                remoteViews.setOnClickPendingIntent(id, action.getActionIntent());
                remoteViews.setInt(id,"setColorFilter", textColor);
                remoteViews.setInt(id, "setBackgroundResource", selectableItemBackground);
            }
        } else {
            XposedBridge.log("没有Action");
        }
        return remoteViews;
    }

    private Context getMoudleContext(Context context){
        Context moudleContext = null;
        try {
            moudleContext = context.createPackageContext(BuildConfig.APPLICATION_ID, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return moudleContext;
    }

    private Context getContext(){
        return AndroidAppHelper.currentApplication().getApplicationContext();
    }

    private int getIconId(Notification notification){
        int id = -1;
        if (notification.getSmallIcon()!=null){
            try {
                id = (int)XposedHelpers.callMethod(notification.getSmallIcon(),"getResId");
            } catch (Exception e){
                XposedBridge.log(e);
            }
        }
        return id;
    }

    private Bitmap getLargeIcon(Notification notification){
        Bitmap bitmap = null;
        if (notification.getLargeIcon()!=null){
            try {
                bitmap = (Bitmap) XposedHelpers.callMethod(notification.getLargeIcon(),"getBitmap");
            } catch (Exception e){
                bitmap = BitmapFactory.decodeResource(getContext().getResources(),getIconId(notification));
            }
        }
        return bitmap;
    }


    private boolean isMediaNotification(Notification notification){

        if (notification.extras.containsKey(NotificationCompat.EXTRA_MEDIA_SESSION)){
            return true;
        } else if (!TextUtils.isEmpty(notification.extras.getString(Notification.EXTRA_TEMPLATE))) {
            return Notification.MediaStyle.class.getName().equals(notification.extras.getString(Notification.EXTRA_TEMPLATE));
        }
        return false;
    }


}
