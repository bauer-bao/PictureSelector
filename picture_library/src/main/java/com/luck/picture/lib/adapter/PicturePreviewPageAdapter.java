package com.luck.picture.lib.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.luck.picture.lib.PictureVideoPlayActivity;
import com.luck.picture.lib.R;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.photoview.OnViewTapListener;
import com.luck.picture.lib.photoview.PhotoView;
import com.luck.picture.lib.widget.longimage.ImageSource;
import com.luck.picture.lib.widget.longimage.ImageViewState;
import com.luck.picture.lib.widget.longimage.SubsamplingScaleImageView;

import java.util.List;

/**
 * date 2018/1/27 下午7:50
 * 描述 图片预览
 *
 * @author luck
 */
public class PicturePreviewPageAdapter extends PagerAdapter {
    private List<LocalMedia> images;
    private Context mContext;
    private OnCallBackListener onCallBackListener;

    public interface OnCallBackListener {
        /**
         * 关闭预览Activity
         */
        void onActivityBackPressed();

        /**
         * 开始loading
         */
        void startLoading();

        /**
         * 结束loading
         */
        void endLoading();
    }

    public PicturePreviewPageAdapter(List<LocalMedia> images, Context context, OnCallBackListener onCallBackListener) {
        super();
        this.images = images;
        this.mContext = context;
        this.onCallBackListener = onCallBackListener;
    }

    @Override
    public int getCount() {
        if (images != null) {
            return images.size();
        }
        return 0;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        (container).removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        final View contentView = LayoutInflater.from(container.getContext()).inflate(R.layout.picture_image_preview, container, false);
        // 常规图控件
        final PhotoView imageView = contentView.findViewById(R.id.preview_image);
        // 长图控件
        final SubsamplingScaleImageView longImg = contentView.findViewById(R.id.long_img_iv);
        ImageView playIv = contentView.findViewById(R.id.iv_play);
        LocalMedia media = images.get(position);
        if (media != null) {
            final String pictureType = media.getPictureType();
            boolean eqVideo = pictureType.startsWith(PictureConfig.VIDEO);
            playIv.setVisibility(eqVideo ? View.VISIBLE : View.GONE);
            final String path;
            if (media.isCut() && !media.isCompressed()) {
                // 裁剪过
                path = media.getCutPath();
            } else if (media.isCompressed()) {
                // 压缩过,或者裁剪同时压缩过,以最终压缩过图片为准
                path = media.getCompressPath();
            } else {
                path = media.getPath();
            }
            boolean isHttp = PictureMimeType.isHttp(path);
            // 可以长按保存并且是网络图片显示一个对话框
            if (isHttp) {
                if (onCallBackListener != null) {
                    onCallBackListener.startLoading();
                }
            }
            boolean isGif = PictureMimeType.isGif(pictureType);
            final boolean eqLongImg = PictureMimeType.isLongImg(media);
            imageView.setVisibility(eqLongImg && !isGif ? View.GONE : View.VISIBLE);
            longImg.setVisibility(eqLongImg && !isGif ? View.VISIBLE : View.GONE);
            // 压缩过的gif就不是gif了
            if (isGif && !media.isCompressed()) {
                RequestOptions gifOptions = new RequestOptions()
                        .override(480, 800)
                        .priority(Priority.HIGH)
                        .diskCacheStrategy(DiskCacheStrategy.NONE);
                Glide.with(mContext)
                        .asGif()
                        .load(path)
                        .apply(gifOptions)
                        .listener(new RequestListener<GifDrawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                                if (onCallBackListener != null) {
                                    onCallBackListener.endLoading();
                                }
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                                if (onCallBackListener != null) {
                                    onCallBackListener.endLoading();
                                }
                                return false;
                            }
                        })
                        .into(imageView);
            } else {
                RequestOptions options = new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL);
                Glide.with(mContext)
                        .asBitmap()
                        .load(path)
                        .apply(options)
                        .into(new SimpleTarget<Bitmap>(480, 800) {

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                super.onLoadFailed(errorDrawable);
                                if (onCallBackListener != null) {
                                    onCallBackListener.endLoading();
                                }
                            }

                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                                if (onCallBackListener != null) {
                                    onCallBackListener.endLoading();
                                }
                                if (eqLongImg) {
                                    displayLongPic(resource, longImg);
                                } else {
                                    imageView.setImageBitmap(resource);
                                }
                            }
                        });
            }
            imageView.setOnViewTapListener(new OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    if (onCallBackListener != null) {
                        onCallBackListener.onActivityBackPressed();
                    }
                }
            });
            longImg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (onCallBackListener != null) {
                        onCallBackListener.onActivityBackPressed();
                    }
                }
            });
            playIv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("video_path", path);
                    intent.putExtras(bundle);
                    intent.setClass(mContext, PictureVideoPlayActivity.class);
                    mContext.startActivity(intent);
                }
            });
        }
        (container).addView(contentView, 0);
        return contentView;
    }

    /**
     * 加载长图
     *
     * @param bmp     图片
     * @param longImg 控件
     */
    private void displayLongPic(Bitmap bmp, SubsamplingScaleImageView longImg) {
        longImg.setQuickScaleEnabled(true);
        longImg.setZoomEnabled(true);
        longImg.setPanEnabled(true);
        longImg.setDoubleTapZoomDuration(100);
        longImg.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP);
        longImg.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER);
        longImg.setImage(ImageSource.cachedBitmap(bmp), new ImageViewState(0, new PointF(0, 0), 0));
    }
}
