package com.ttt.ktv.bean;

import android.os.Parcel;
import android.os.Parcelable;

import com.wushuangtech.expansion.bean.LocalAudioStats;
import com.wushuangtech.expansion.bean.LocalVideoStats;
import com.wushuangtech.expansion.bean.RemoteAudioStats;
import com.wushuangtech.expansion.bean.RemoteVideoStats;

/**
 * Created by wangzhiguo on 17/10/13.
 */

public class JniObjs implements Parcelable {

    public int mJniType;
    public long mUid;
    public int mIdentity;
    public int mReason;
    public int mAudioLevel;
    public String mChannelName;
    public String mSEI;
    public int mErrorType;
    public int mScreenRecordTime;
    public int mAudioRoute;
    public String mDevID;

    public RemoteVideoStats mRemoteVideoStats;
    public RemoteAudioStats mRemoteAudioStats;
    public LocalVideoStats mLocalVideoStats;
    public LocalAudioStats mLocalAudioStats;

    public JniObjs() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mJniType);
        dest.writeLong(this.mUid);
        dest.writeInt(this.mIdentity);
        dest.writeInt(this.mReason);
        dest.writeInt(this.mAudioLevel);
        dest.writeString(this.mChannelName);
        dest.writeString(this.mSEI);
        dest.writeInt(this.mErrorType);
        dest.writeInt(this.mScreenRecordTime);
        dest.writeInt(this.mAudioRoute);
        dest.writeString(this.mDevID);
        dest.writeParcelable(this.mRemoteVideoStats, flags);
        dest.writeParcelable(this.mRemoteAudioStats, flags);
        dest.writeParcelable(this.mLocalVideoStats, flags);
        dest.writeParcelable(this.mLocalAudioStats, flags);
    }

    protected JniObjs(Parcel in) {
        this.mJniType = in.readInt();
        this.mUid = in.readLong();
        this.mIdentity = in.readInt();
        this.mReason = in.readInt();
        this.mAudioLevel = in.readInt();
        this.mChannelName = in.readString();
        this.mSEI = in.readString();
        this.mErrorType = in.readInt();
        this.mScreenRecordTime = in.readInt();
        this.mAudioRoute = in.readInt();
        this.mDevID = in.readString();
        this.mRemoteVideoStats = in.readParcelable(RemoteVideoStats.class.getClassLoader());
        this.mRemoteAudioStats = in.readParcelable(RemoteAudioStats.class.getClassLoader());
        this.mLocalVideoStats = in.readParcelable(LocalVideoStats.class.getClassLoader());
        this.mLocalAudioStats = in.readParcelable(LocalAudioStats.class.getClassLoader());
    }

    public static final Creator<JniObjs> CREATOR = new Creator<JniObjs>() {
        @Override
        public JniObjs createFromParcel(Parcel source) {
            return new JniObjs(source);
        }

        @Override
        public JniObjs[] newArray(int size) {
            return new JniObjs[size];
        }
    };
}
