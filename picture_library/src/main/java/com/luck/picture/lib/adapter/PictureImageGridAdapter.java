package com.luck.picture.lib.adapter;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.luck.picture.lib.R;
import com.luck.picture.lib.anim.OptAnimationLoader;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.config.PictureSelectionConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.DateUtils;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.StringUtils;
import com.luck.picture.lib.tools.ToastManage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * project：PictureSelector
 * package：com.luck.picture.lib.adapter
 * email：893855882@qq.com
 * data：2016/12/30
 *
 * @author luck
 */
public class PictureImageGridAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static int DURATION = 450;
    private Context context;
    private OnPhotoSelectChangedListener imageSelectChangedListener;
    private int maxSelectNum;
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<>();
    private boolean enablePreview;
    private int selectMode;
    private boolean isCheckedNum;
    private int overrideWidth, overrideHeight;
    private float sizeMultiplier;
    private Animation animation;
    private boolean zoomAnim;

    public PictureImageGridAdapter(Context context, PictureSelectionConfig config) {
        this.context = context;
        this.selectMode = config.selectionMode;
        this.maxSelectNum = config.maxSelectNum;
        this.enablePreview = config.enablePreview;
        this.isCheckedNum = config.checkNumMode;
        this.overrideWidth = config.overrideWidth;
        this.overrideHeight = config.overrideHeight;
        this.sizeMultiplier = config.sizeMultiplier;
        this.zoomAnim = config.zoomAnim;
        animation = OptAnimationLoader.loadAnimation(context, R.anim.modal_in);
    }

    public void bindImagesData(List<LocalMedia> images) {
        this.images = images;
        notifyDataSetChanged();
    }

    public void bindSelectImages(List<LocalMedia> images) {
        // 这里重新构构造一个新集合，不然会产生已选集合一变，结果集合也会添加的问题
        selectImages.clear();
        selectImages.addAll(images);
        subSelectPosition();
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener.onChange(selectImages);
        }
    }

    public List<LocalMedia> getSelectedImages() {
        if (selectImages == null) {
            selectImages = new ArrayList<>();
        }
        return selectImages;
    }

    public List<LocalMedia> getImages() {
        if (images == null) {
            images = new ArrayList<>();
        }
        return images;
    }

    @Override
    public int getItemViewType(int position) {
        return PictureConfig.TYPE_PICTURE;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.picture_image_grid_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        final ViewHolder contentHolder = (ViewHolder) holder;
        int index = contentHolder.getAdapterPosition();
        final LocalMedia image = images.get(index);
        image.position = index;
        final String path = image.getPath();
        final String pictureType = image.getPictureType();
        if (isCheckedNum) {
            notifyCheckChanged(contentHolder, image);
        }
        selectImage(contentHolder, isSelected(image), false);

        final int mediaMimeType = PictureMimeType.isPictureType(pictureType);
        boolean gif = PictureMimeType.isGif(pictureType);
        contentHolder.isGifTv.setVisibility(gif ? View.VISIBLE : View.GONE);

        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.video_icon);
        StringUtils.modifyTextViewDrawable(contentHolder.durationTv, drawable, 0);
        contentHolder.durationTv.setVisibility(mediaMimeType == PictureConfig.TYPE_VIDEO ? View.VISIBLE : View.GONE);

        boolean eqLongImg = PictureMimeType.isLongImg(image);
        contentHolder.longChartTv.setVisibility(eqLongImg ? View.VISIBLE : View.GONE);

        long duration = image.getDuration();
        contentHolder.durationTv.setText(DateUtils.timeParse(duration));

        RequestOptions options = new RequestOptions();
        if (overrideWidth <= 0 && overrideHeight <= 0) {
            options.sizeMultiplier(sizeMultiplier);
        } else {
            options.override(overrideWidth, overrideHeight);
        }
        options.diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.image_placeholder);
        Glide.with(context)
                .asBitmap()
                .load(path)
                .apply(options)
                .into(contentHolder.pictureIv);
        if (enablePreview) {
            contentHolder.checkLl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 如原图路径不存在或者路径存在但文件不存在
                    if (!new File(path).exists() || PictureFileUtils.isDamage(path, mediaMimeType)) {
                        ToastManage.s(context, context.getString(R.string.picture_source_error));
                        return;
                    }
                    changeCheckboxState(contentHolder, image);
                }
            });
        }
        contentHolder.contentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 如原图路径不存在或者路径存在但文件不存在
                if (!new File(path).exists() || PictureFileUtils.isDamage(path, mediaMimeType)) {
                    ToastManage.s(context, context.getString(R.string.picture_source_error));
                    return;
                }
                boolean eqResult = mediaMimeType == PictureConfig.TYPE_IMAGE && enablePreview
                        || mediaMimeType == PictureConfig.TYPE_VIDEO && (enablePreview || selectMode == PictureConfig.SINGLE);
                if (eqResult) {
                    imageSelectChangedListener.onPictureClick(image, image.position);
                } else {
                    changeCheckboxState(contentHolder, image);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView pictureIv;
        TextView checkTv;
        TextView durationTv, isGifTv, longChartTv;
        View contentView;
        LinearLayout checkLl;

        ViewHolder(View itemView) {
            super(itemView);
            contentView = itemView;
            pictureIv = itemView.findViewById(R.id.iv_picture);
            checkTv = itemView.findViewById(R.id.check);
            checkLl = itemView.findViewById(R.id.ll_check);
            durationTv = itemView.findViewById(R.id.tv_duration);
            isGifTv = itemView.findViewById(R.id.tv_is_gif);
            longChartTv = itemView.findViewById(R.id.tv_long_chart);
        }
    }

    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(ViewHolder viewHolder, LocalMedia imageBean) {
        viewHolder.checkTv.setText("");
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(imageBean.getPath())) {
                imageBean.setNum(media.getNum());
                media.setPosition(imageBean.getPosition());
                viewHolder.checkTv.setText(String.valueOf(imageBean.getNum()));
            }
        }
    }

    /**
     * 改变图片选中状态
     *
     * @param contentHolder
     * @param image
     */
    private void changeCheckboxState(ViewHolder contentHolder, LocalMedia image) {
        boolean isChecked = contentHolder.checkTv.isSelected();
        if (selectImages.size() >= maxSelectNum && !isChecked) {
            String str = context.getString(R.string.picture_message_max_num, maxSelectNum);
            ToastManage.s(context, str);
            return;
        }

        if (isChecked) {
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(image.getPath())) {
                    selectImages.remove(media);
                    subSelectPosition();
                    disZoom(contentHolder.pictureIv);
                    break;
                }
            }
        } else {
            // 如果是单选，则清空已选中的并刷新列表(作单一选择)
            if (selectMode == PictureConfig.SINGLE) {
                singleRadioMediaImage();
            }
            selectImages.add(image);
            image.setNum(selectImages.size());
            zoom(contentHolder.pictureIv);
        }
        //通知点击项发生了改变
        notifyItemChanged(contentHolder.getAdapterPosition());
        selectImage(contentHolder, !isChecked, true);
        if (imageSelectChangedListener != null) {
            imageSelectChangedListener.onChange(selectImages);
        }
    }

    /**
     * 单选模式
     */
    private void singleRadioMediaImage() {
        if (selectImages != null && selectImages.size() > 0) {
            LocalMedia media = selectImages.get(0);
            notifyItemChanged(media.position);
            selectImages.clear();
        }
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        if (isCheckedNum) {
            for (int index = 0; index < selectImages.size(); index++) {
                LocalMedia media = selectImages.get(index);
                media.setNum(index + 1);
                notifyItemChanged(media.position);
            }
        }
    }

    /**
     * 选中的图片并执行动画
     *
     * @param holder
     * @param isChecked
     * @param isAnim
     */
    private void selectImage(ViewHolder holder, boolean isChecked, boolean isAnim) {
        holder.checkTv.setSelected(isChecked);
        if (isChecked) {
            if (isAnim) {
                if (animation != null) {
                    holder.checkTv.startAnimation(animation);
                }
            }
            holder.pictureIv.setColorFilter(ContextCompat.getColor(context, R.color.image_overlay_true), PorterDuff.Mode.SRC_ATOP);
        } else {
            holder.pictureIv.setColorFilter(ContextCompat.getColor(context, R.color.image_overlay_false), PorterDuff.Mode.SRC_ATOP);
        }
    }

    public interface OnPhotoSelectChangedListener {
        /**
         * 已选Media回调
         *
         * @param selectImages
         */
        void onChange(List<LocalMedia> selectImages);

        /**
         * 图片预览回调
         *
         * @param media
         * @param position
         */
        void onPictureClick(LocalMedia media, int position);
    }

    public void setOnPhotoSelectChangedListener(OnPhotoSelectChangedListener imageSelectChangedListener) {
        this.imageSelectChangedListener = imageSelectChangedListener;
    }

    private void zoom(ImageView imgIv) {
        if (zoomAnim) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(ObjectAnimator.ofFloat(imgIv, "scaleX", 1f, 1.12f),
                    ObjectAnimator.ofFloat(imgIv, "scaleY", 1f, 1.12f));
            set.setDuration(DURATION);
            set.start();
        }
    }

    private void disZoom(ImageView imgIv) {
        if (zoomAnim) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(ObjectAnimator.ofFloat(imgIv, "scaleX", 1.12f, 1f),
                    ObjectAnimator.ofFloat(imgIv, "scaleY", 1.12f, 1f));
            set.setDuration(DURATION);
            set.start();
        }
    }
}
