package com.ttt.ktv;

import com.ttt.ktv.callback.MyTTTRtcEngineEventHandler;

import java.util.ArrayList;

/**
 * Created by wangzhiguo on 17/6/15.
 */

public class LocalConfig {

    public static ArrayList<Long> mUserEnterOrder = new ArrayList<>();

    public static long mUid;

    public static String mRoomID;

    public static int mRole;

    public static MyTTTRtcEngineEventHandler mMyTTTRtcEngineEventHandler;
}
