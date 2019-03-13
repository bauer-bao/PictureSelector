package com.luck.picture.lib.config;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StyleRes;

import com.cjt2325.cameralibrary.JCameraView;
import com.luck.picture.lib.R;
import com.luck.picture.lib.entity.LocalMedia;

import java.util.ArrayList;
import java.util.List;

/**
 * author：luck
 * project：PictureSelector
 * package：com.luck.picture.lib.config
 * email：893855882@qq.com
 * data：2017/5/24
 */

public final class PictureSelectionConfig implements Parcelable {
    /**
     * 文件类型，1.图片和视频，2.图片，3.视频 三种
     */
    public int mimeType;
    /**
     * 拍照，还是打开相册
     */
    public boolean camera;
    /**
     * 拍照保存地址
     */
    public String outputCameraPath;
    /**
     * 样式
     */
    @StyleRes
    public int themeStyleId;
    /**
     * 单选或者多选
     */
    public int selectionMode;
    /**
     * 选择范围
     */
    public int maxSelectNum;
    public int minSelectNum;
    /**
     * 图片显示列数
     */
    public int imageSpanCount;
    /**
     * 是否支持预览
     */
    public boolean enablePreview;
    /**
     * 缩放效果
     */
    public boolean zoomAnim;
    /**
     * 图片加载的宽高
     */
    public int overrideWidth;
    public int overrideHeight;
    /**
     * 显示gif
     */
    public boolean isGif;
    /**
     * 选中的时候以数字模式展示
     */
    public boolean checkNumMode;
    /**
     * 预览图片时 是否增强左右滑动图片体验(图片滑动一半即可看到上一张是否选中)
     */
    public boolean previewEggs;
    /**
     * glide 加载图片大小 0~1之间 如设置 .glideOverride()无效
     */
    public float sizeMultiplier;
    /**
     * 视频录制质量
     */
    public int videoQuality;
    /**
     * 视频录制时长
     */
    public int recordVideoSecond;
    /**
     * 查询视频的最大长度
     */
    public int videoMaxSecond;
    /**
     * 查询视频的最小长度
     */
    public int videoMinSecond;
    /**
     * 是否压缩
     */
    public boolean isCompress;
    /**
     * 最小开始压缩的尺寸
     */
    public int minimumCompressSize;
    /**
     * 是否裁剪
     */
    public boolean enableCrop;
    /**
     * 裁剪比例
     */
    public int aspectRatioX;
    public int aspectRatioY;
    /**
     * 显示底部操作栏
     */
    public boolean hideBottomControls;
    /**
     * 裁剪压缩质量
     */
    public int cropCompressQuality;
    /**
     * 裁剪宽高比
     */
    public int cropWidth;
    public int cropHeight;
    /**
     * 是否显示裁剪矩形框
     */
    public boolean showCropFrame;
    /**
     * 是否显示裁剪网格
     */
    public boolean showCropGrid;
    /**
     * 裁剪框是否可拖拽，整个拖拽
     */
    public boolean freeStyleCropEnabled;
    /**
     * 圆形裁剪框
     */
    public boolean circleDimmedLayer;
    /**
     * 裁剪框是否可拖拽，调整比例
     */
    public boolean isDragFrame;
    /**
     * 支持旋转
     */
    public boolean rotateEnabled;
    /**
     * 支持缩放
     */
    public boolean scaleEnabled;
    /**
     * 是否传入已选图片
     */
    public List<LocalMedia> selectionMedias;

    private void reset() {
        mimeType = PictureConfig.TYPE_IMAGE;
        camera = false;
        themeStyleId = R.style.picture_default_style;
        selectionMode = PictureConfig.MULTIPLE;
        maxSelectNum = 9;
        minSelectNum = 0;
        videoQuality = JCameraView.MEDIA_QUALITY_MIDDLE;
        cropCompressQuality = 90;
        videoMaxSecond = 0;
        videoMinSecond = 0;
        recordVideoSecond = 10;
        minimumCompressSize = PictureConfig.MAX_COMPRESS_SIZE;
        imageSpanCount = 4;
        overrideWidth = 0;
        overrideHeight = 0;
        isCompress = false;
        aspectRatioX = 0;
        aspectRatioY = 0;
        cropWidth = 0;
        cropHeight = 0;
        isGif = false;
        enablePreview = true;
        checkNumMode = false;
        enableCrop = false;
        freeStyleCropEnabled = false;
        circleDimmedLayer = false;
        showCropFrame = true;
        showCropGrid = true;
        hideBottomControls = true;
        rotateEnabled = true;
        scaleEnabled = true;
        previewEggs = false;
        zoomAnim = true;
        isDragFrame = true;
        outputCameraPath = "";
        sizeMultiplier = 0.5f;
        selectionMedias = new ArrayList<>();
    }

    public static PictureSelectionConfig getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static PictureSelectionConfig getCleanInstance() {
        PictureSelectionConfig selectionSpec = getInstance();
        selectionSpec.reset();
        return selectionSpec;
    }

    private static final class InstanceHolder {
        private static final PictureSelectionConfig INSTANCE = new PictureSelectionConfig();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mimeType);
        dest.writeByte(this.camera ? (byte) 1 : (byte) 0);
        dest.writeString(this.outputCameraPath);
        dest.writeInt(this.themeStyleId);
        dest.writeInt(this.selectionMode);
        dest.writeInt(this.maxSelectNum);
        dest.writeInt(this.minSelectNum);
        dest.writeInt(this.videoQuality);
        dest.writeInt(this.cropCompressQuality);
        dest.writeInt(this.videoMaxSecond);
        dest.writeInt(this.videoMinSecond);
        dest.writeInt(this.recordVideoSecond);
        dest.writeInt(this.minimumCompressSize);
        dest.writeInt(this.imageSpanCount);
        dest.writeInt(this.overrideWidth);
        dest.writeInt(this.overrideHeight);
        dest.writeInt(this.aspectRatioX);
        dest.writeInt(this.aspectRatioY);
        dest.writeFloat(this.sizeMultiplier);
        dest.writeInt(this.cropWidth);
        dest.writeInt(this.cropHeight);
        dest.writeByte(this.zoomAnim ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isCompress ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isGif ? (byte) 1 : (byte) 0);
        dest.writeByte(this.enablePreview ? (byte) 1 : (byte) 0);
        dest.writeByte(this.checkNumMode ? (byte) 1 : (byte) 0);
        dest.writeByte(this.enableCrop ? (byte) 1 : (byte) 0);
        dest.writeByte(this.freeStyleCropEnabled ? (byte) 1 : (byte) 0);
        dest.writeByte(this.circleDimmedLayer ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showCropFrame ? (byte) 1 : (byte) 0);
        dest.writeByte(this.showCropGrid ? (byte) 1 : (byte) 0);
        dest.writeByte(this.hideBottomControls ? (byte) 1 : (byte) 0);
        dest.writeByte(this.rotateEnabled ? (byte) 1 : (byte) 0);
        dest.writeByte(this.scaleEnabled ? (byte) 1 : (byte) 0);
        dest.writeByte(this.previewEggs ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isDragFrame ? (byte) 1 : (byte) 0);
        dest.writeTypedList(this.selectionMedias);
    }

    public PictureSelectionConfig() {
    }

    protected PictureSelectionConfig(Parcel in) {
        this.mimeType = in.readInt();
        this.camera = in.readByte() != 0;
        this.outputCameraPath = in.readString();
        this.themeStyleId = in.readInt();
        this.selectionMode = in.readInt();
        this.maxSelectNum = in.readInt();
        this.minSelectNum = in.readInt();
        this.videoQuality = in.readInt();
        this.cropCompressQuality = in.readInt();
        this.videoMaxSecond = in.readInt();
        this.videoMinSecond = in.readInt();
        this.recordVideoSecond = in.readInt();
        this.minimumCompressSize = in.readInt();
        this.imageSpanCount = in.readInt();
        this.overrideWidth = in.readInt();
        this.overrideHeight = in.readInt();
        this.aspectRatioX = in.readInt();
        this.aspectRatioY = in.readInt();
        this.sizeMultiplier = in.readFloat();
        this.cropWidth = in.readInt();
        this.cropHeight = in.readInt();
        this.zoomAnim = in.readByte() != 0;
        this.isCompress = in.readByte() != 0;
        this.isGif = in.readByte() != 0;
        this.enablePreview = in.readByte() != 0;
        this.checkNumMode = in.readByte() != 0;
        this.enableCrop = in.readByte() != 0;
        this.freeStyleCropEnabled = in.readByte() != 0;
        this.circleDimmedLayer = in.readByte() != 0;
        this.showCropFrame = in.readByte() != 0;
        this.showCropGrid = in.readByte() != 0;
        this.hideBottomControls = in.readByte() != 0;
        this.rotateEnabled = in.readByte() != 0;
        this.scaleEnabled = in.readByte() != 0;
        this.previewEggs = in.readByte() != 0;
        this.isDragFrame = in.readByte() != 0;
        this.selectionMedias = in.createTypedArrayList(LocalMedia.CREATOR);
    }

    public static final Parcelable.Creator<PictureSelectionConfig> CREATOR = new Parcelable.Creator<PictureSelectionConfig>() {
        @Override
        public PictureSelectionConfig createFromParcel(Parcel source) {
            return new PictureSelectionConfig(source);
        }

        @Override
        public PictureSelectionConfig[] newArray(int size) {
            return new PictureSelectionConfig[size];
        }
    };
}
