package com.luck.picture.lib;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.luck.picture.lib.adapter.PicturePreviewPageAdapter;
import com.luck.picture.lib.anim.OptAnimationLoader;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.EventEntity;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.observable.ImagesObservable;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.rxbus2.Subscribe;
import com.luck.picture.lib.rxbus2.ThreadMode;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.luck.picture.lib.tools.ScreenUtils;
import com.luck.picture.lib.tools.ToastManage;
import com.luck.picture.lib.widget.PreviewViewPager;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 相册内部预览的页面
 * <p>
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：893855882@qq.com
 * data：16/12/31
 *
 * @author luck
 */
public class PicturePreviewActivity extends PictureBaseActivity implements View.OnClickListener, Animation.AnimationListener, PicturePreviewPageAdapter.OnCallBackListener {
    private ImageView pictureLeftBackIv;
    private TextView imgNumTv, titleTv, okTv;
    private PreviewViewPager viewPager;
    private LinearLayout idOkLl;
    private LinearLayout checkLl;
    private TextView checkTv;

    private int position;
    private List<LocalMedia> images = new ArrayList<>();
    private List<LocalMedia> selectImages = new ArrayList<>();
    private PicturePreviewPageAdapter adapter;
    private int index;
    private boolean refresh;
    private Animation animation;
    private int screenWidth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_preview);
        if (!RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().register(this);
        }
        //获取控件
        pictureLeftBackIv = findViewById(R.id.picture_left_back);
        viewPager = findViewById(R.id.preview_pager);
        checkLl = findViewById(R.id.ll_check);
        idOkLl = findViewById(R.id.id_ll_ok);
        checkTv = findViewById(R.id.check);
        okTv = findViewById(R.id.tv_ok);
        imgNumTv = findViewById(R.id.tv_img_num);
        titleTv = findViewById(R.id.picture_title);
        pictureLeftBackIv.setOnClickListener(this);
        idOkLl.setOnClickListener(this);
        checkLl.setOnClickListener(this);
        //初始化数据
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        okTv.setText(numComplete ? getString(R.string.picture_done_front_num, 0, config.selectionMode == PictureConfig.SINGLE ? 1 : config.maxSelectNum) :
                getString(R.string.picture_please_select));
        imgNumTv.setSelected(config.checkNumMode);
        selectImages = (List<LocalMedia>) getIntent().getSerializableExtra(PictureConfig.EXTRA_SELECT_LIST);
        boolean isBottomPreview = getIntent().getBooleanExtra(PictureConfig.EXTRA_BOTTOM_PREVIEW, false);
        if (isBottomPreview) {
            // 底部预览按钮过来
            images = (List<LocalMedia>) getIntent().getSerializableExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);
        } else {
            images = ImagesObservable.getInstance().readLocalMedias();
        }
        animation = OptAnimationLoader.loadAnimation(this, R.anim.modal_in);
        animation.setAnimationListener(this);
        screenWidth = ScreenUtils.getScreenWidth(this);
        initViewPageAdapterData();
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                isPreviewEggs(config.previewEggs, position, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int i) {
                position = i;
                titleTv.setText(position + 1 + "/" + images.size());
                LocalMedia media = images.get(position);
                index = media.getPosition();
                if (!config.previewEggs) {
                    if (config.checkNumMode) {
                        checkTv.setText(media.getNum() + "");
                        notifyCheckChanged(media);
                    }
                    onImageChecked(position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * 这里没实际意义，好处是预览图片时 滑动到屏幕一半以上可看到下一张图片是否选中了
     *
     * @param previewEggs          是否显示预览友好体验
     * @param positionOffsetPixels 滑动偏移量
     */
    private void isPreviewEggs(boolean previewEggs, int position, int positionOffsetPixels) {
        if (previewEggs) {
            if (images != null && images.size() > 0) {
                LocalMedia media;
                int num;
                if (positionOffsetPixels < screenWidth / 2) {
                    media = images.get(position);
                    checkTv.setSelected(isSelected(media));
                    if (config.checkNumMode) {
                        num = media.getNum();
                        checkTv.setText(num + "");
                        notifyCheckChanged(media);
                        onImageChecked(position);
                    }
                } else {
                    media = images.get(position + 1);
                    checkTv.setSelected(isSelected(media));
                    if (config.checkNumMode) {
                        num = media.getNum();
                        checkTv.setText(num + "");
                        notifyCheckChanged(media);
                        onImageChecked(position + 1);
                    }
                }
            }
        }
    }

    /**
     * 单选图片
     */
    private void singleRadioMediaImage() {
        if (selectImages != null && selectImages.size() > 0) {
            LocalMedia media = selectImages.get(0);
            RxBus.getDefault().post(new EventEntity(PictureConfig.UPDATE_FLAG, selectImages, media.getPosition()));
            selectImages.clear();
        }
    }

    /**
     * 初始化ViewPage数据
     */
    private void initViewPageAdapterData() {
        titleTv.setText(position + 1 + "/" + images.size());
        adapter = new PicturePreviewPageAdapter(images, this, this);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        onSelectNumChange(false);
        onImageChecked(position);
        if (images.size() > 0) {
            LocalMedia media = images.get(position);
            index = media.getPosition();
            if (config.checkNumMode) {
                imgNumTv.setSelected(true);
                checkTv.setText(media.getNum() + "");
                notifyCheckChanged(media);
            }
        }
    }

    /**
     * 选择按钮更新
     */
    private void notifyCheckChanged(LocalMedia imageBean) {
        if (config.checkNumMode) {
            checkTv.setText("");
            for (LocalMedia media : selectImages) {
                if (media.getPath().equals(imageBean.getPath())) {
                    imageBean.setNum(media.getNum());
                    checkTv.setText(String.valueOf(imageBean.getNum()));
                }
            }
        }
    }

    /**
     * 更新选择的顺序
     */
    private void subSelectPosition() {
        for (int index = 0, len = selectImages.size(); index < len; index++) {
            LocalMedia media = selectImages.get(index);
            media.setNum(index + 1);
        }
    }

    /**
     * 判断当前图片是否选中
     *
     * @param position
     */
    public void onImageChecked(int position) {
        if (images != null && images.size() > 0) {
            LocalMedia media = images.get(position);
            checkTv.setSelected(isSelected(media));
        } else {
            checkTv.setSelected(false);
        }
    }

    /**
     * 当前图片是否选中
     *
     * @param image
     * @return
     */
    public boolean isSelected(LocalMedia image) {
        for (LocalMedia media : selectImages) {
            if (media.getPath().equals(image.getPath())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 更新图片选择数量
     */
    public void onSelectNumChange(boolean isRefresh) {
        refresh = isRefresh;
        boolean enable = selectImages.size() != 0;
        if (enable) {
            okTv.setSelected(true);
            idOkLl.setEnabled(true);
            if (numComplete) {
                okTv.setText(getString(R.string.picture_done_front_num, selectImages.size(),
                        config.selectionMode == PictureConfig.SINGLE ? 1 : config.maxSelectNum));
            } else {
                if (refresh) {
                    imgNumTv.startAnimation(animation);
                }
                imgNumTv.setVisibility(View.VISIBLE);
                imgNumTv.setText(String.valueOf(selectImages.size()));
                okTv.setText(R.string.picture_completed);
            }
        } else {
            idOkLl.setEnabled(false);
            okTv.setSelected(false);
            if (numComplete) {
                okTv.setText(getString(R.string.picture_done_front_num, 0,
                        config.selectionMode == PictureConfig.SINGLE ? 1 : config.maxSelectNum));
            } else {
                imgNumTv.setVisibility(View.INVISIBLE);
                okTv.setText(R.string.picture_please_select);
            }
        }
        updateSelector(refresh);
    }

    /**
     * 更新图片列表选中效果
     *
     * @param isRefresh
     */
    private void updateSelector(boolean isRefresh) {
        if (isRefresh) {
            EventEntity obj = new EventEntity(PictureConfig.UPDATE_FLAG, selectImages, index);
            RxBus.getDefault().post(obj);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.picture_left_back) {
            //返回
            onBackPressed();
        } else if (id == R.id.id_ll_ok) {
            //确定按钮
            // 如果设置了图片最小选择数量，则判断是否满足条件
            if (config.minSelectNum > 0 && config.selectionMode == PictureConfig.MULTIPLE) {
                if (selectImages.size() < config.minSelectNum) {
                    String str = getString(R.string.picture_min_img_num, config.minSelectNum);
                    ToastManage.s(mContext, str);
                    return;
                }
            }
            LocalMedia image = selectImages.size() > 0 ? selectImages.get(0) : null;
            if (config.enableCrop && selectImages.size() == 1 && image.getPictureType().startsWith(PictureConfig.IMAGE)) {
                //只能对1张图片进行截图操作
                originalPath = image.getPath();
                startCrop(originalPath);
            } else {
                onResult(selectImages);
            }
        } else if (id == R.id.ll_check) {
            //选中或者取消选中
            if (images != null && images.size() > 0) {
                LocalMedia image = images.get(viewPager.getCurrentItem());
                String path = image.getPath();
                final String pictureType = image.getPictureType();
                final int mediaMimeType = PictureMimeType.isPictureType(pictureType);
                if (!new File(path).exists() || PictureFileUtils.isDamage(path, mediaMimeType)) {
                    ToastManage.s(this, getString(R.string.picture_source_error));
                    return;
                }
                // 刷新图片列表中图片状态
                boolean isChecked;
                if (!checkTv.isSelected()) {
                    isChecked = true;
                    checkTv.setSelected(true);
                    checkTv.startAnimation(animation);
                } else {
                    isChecked = false;
                    checkTv.setSelected(false);
                }
                if (selectImages.size() >= config.maxSelectNum && isChecked) {
                    ToastManage.s(mContext, getString(R.string.picture_message_max_num, config.maxSelectNum));
                    checkTv.setSelected(false);
                    return;
                }
                if (isChecked) {
                    // 如果是单选，则清空已选中的并刷新列表(作单一选择)
                    if (config.selectionMode == PictureConfig.SINGLE) {
                        singleRadioMediaImage();
                    }
                    selectImages.add(image);
                    image.setNum(selectImages.size());
                    if (config.checkNumMode) {
                        checkTv.setText(String.valueOf(image.getNum()));
                    }
                } else {
                    for (LocalMedia media : selectImages) {
                        if (media.getPath().equals(image.getPath())) {
                            selectImages.remove(media);
                            subSelectPosition();
                            notifyCheckChanged(media);
                            break;
                        }
                    }
                }
                onSelectNumChange(true);
            }
        }
    }

    @Override
    public void onResult(List<LocalMedia> images) {
        RxBus.getDefault().post(new EventEntity(PictureConfig.PREVIEW_DATA_FLAG, images));
        // 如果开启了压缩，先不关闭此页面，PictureImageGridActivity压缩完在通知关闭
        if (!config.isCompress) {
            onBackPressed();
        } else {
            showPleaseDialog();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case UCrop.REQUEST_CROP:
                    if (data != null) {
                        setResult(RESULT_OK, data);
                    }
                    finish();
                    break;

                default:
                    break;
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable throwable = (Throwable) data.getSerializableExtra(UCrop.EXTRA_ERROR);
            ToastManage.s(mContext, throwable.getMessage());
        }
    }


    @Override
    public void onBackPressed() {
        closeActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().unregister(this);
        }
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {
        updateSelector(refresh);
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    @Override
    public void onActivityBackPressed() {
        onBackPressed();
    }

    @Override
    public void startLoading() {

    }

    @Override
    public void endLoading() {

    }

    /**
     * rxbus 回调
     *
     * @param obj
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void rxBusCallback(EventEntity obj) {
        switch (obj.what) {
            case PictureConfig.CLOSE_PREVIEW_FLAG:
                // 压缩完后关闭预览界面
                dismissDialog();
                viewPager.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                }, 150);
                break;

            default:
                break;
        }
    }
}
