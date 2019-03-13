package com.luck.picture.lib;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.luck.picture.lib.adapter.PictureAlbumDirectoryAdapter;
import com.luck.picture.lib.adapter.PictureImageGridAdapter;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.decoration.GridSpacingItemDecoration;
import com.luck.picture.lib.entity.EventEntity;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.entity.LocalMediaFolder;
import com.luck.picture.lib.model.LocalMediaLoader;
import com.luck.picture.lib.observable.ImagesObservable;
import com.luck.picture.lib.rxbus2.RxBus;
import com.luck.picture.lib.rxbus2.Subscribe;
import com.luck.picture.lib.rxbus2.ThreadMode;
import com.luck.picture.lib.tools.ScreenUtils;
import com.luck.picture.lib.tools.StringUtils;
import com.luck.picture.lib.tools.ToastManage;
import com.luck.picture.lib.widget.FolderPopWindow;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.trello.rxlifecycle2.android.ActivityEvent;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * data：2018/1/27 19:12
 * 描述: Media 选择页面
 *
 * @author luck
 */
public class PictureSelectorActivity extends PictureBaseActivity implements View.OnClickListener, PictureAlbumDirectoryAdapter.OnItemClickListener,
        PictureImageGridAdapter.OnPhotoSelectChangedListener {
    private static final int SHOW_DIALOG = 0;
    private static final int DISMISS_DIALOG = 1;

    private ImageView pictureLeftBackIv;
    private TextView pictureTitleTv, pictureRightTv, pictureTvOk, emptyTv, pictureImgNumTv, pictureIdPreviewTv;
    private RelativeLayout pictureTitleRl;
    private LinearLayout idOkLl;
    private RecyclerView pictureRv;
    private FolderPopWindow folderWindow;

    private PictureImageGridAdapter adapter;
    private List<LocalMedia> images = new ArrayList<>();
    private RxPermissions rxPermissions;
    private LocalMediaLoader mediaLoader;
    private Animation animation;
    private boolean anim = false;
    private boolean isPreview = false;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_DIALOG:
                    showPleaseDialog();
                    break;
                case DISMISS_DIALOG:
                    dismissDialog();
                    break;
                default:
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().register(this);
        }
        rxPermissions = new RxPermissions(this);
        if (config.camera) {
            if (savedInstanceState == null) {
                rxPermissions.requestEachCombined(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                        .compose(this.<Permission>bindUntilEvent(ActivityEvent.DESTROY))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Permission>() {
                            @Override
                            public void accept(Permission permission) throws Exception {
                                if (permission.granted) {
                                    //同意权限
                                    startActivityForResult(new Intent(mContext, PictureCameraActivity.class), PictureConfig.REQUEST_CAMERA);
                                } else {
                                    //拒绝权限
                                    ToastManage.s(mContext, getString(R.string.picture_camera));
                                    closeActivity();
                                }
                            }
                        });
            }
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.picture_empty);
        } else {
            setContentView(R.layout.picture_selector);
            initView(savedInstanceState);
        }
    }

    /**
     * init views
     */
    private void initView(Bundle savedInstanceState) {
        //获取控件
        pictureTitleRl = findViewById(R.id.rl_picture_title);
        pictureLeftBackIv = findViewById(R.id.picture_left_back);
        pictureTitleTv = findViewById(R.id.picture_title);
        pictureRightTv = findViewById(R.id.picture_right);
        pictureTvOk = findViewById(R.id.picture_tv_ok);
        pictureIdPreviewTv = findViewById(R.id.picture_id_preview);
        pictureImgNumTv = findViewById(R.id.picture_tv_img_num);
        pictureRv = findViewById(R.id.picture_recycler);
        idOkLl = findViewById(R.id.id_ll_ok);
        emptyTv = findViewById(R.id.tv_empty);
        pictureIdPreviewTv.setOnClickListener(this);
        pictureLeftBackIv.setOnClickListener(this);
        pictureRightTv.setOnClickListener(this);
        idOkLl.setOnClickListener(this);
        pictureTitleTv.setOnClickListener(this);
        //初始化控件
        pictureTitleTv.setText(R.string.picture_camera_roll);
        emptyTv.setText(R.string.picture_empty);
        StringUtils.tempTextFont(emptyTv);
        isNumComplete(numComplete);
        pictureRv.setHasFixedSize(true);
        pictureRv.addItemDecoration(new GridSpacingItemDecoration(config.imageSpanCount, ScreenUtils.dip2px(this, 2), false));
        pictureRv.setLayoutManager(new GridLayoutManager(this, config.imageSpanCount));
        // 解决调用 notifyItemChanged 闪烁问题,取消默认动画
        ((SimpleItemAnimator) pictureRv.getItemAnimator()).setSupportsChangeAnimations(false);
        adapter = new PictureImageGridAdapter(mContext, config);
        adapter.setOnPhotoSelectChangedListener(this);
        adapter.bindSelectImages(selectionMedias);
        pictureRv.setAdapter(adapter);
        folderWindow = new FolderPopWindow(this);
        folderWindow.setPictureTitleView(pictureTitleTv);
        folderWindow.setOnItemClickListener(this);
        //开始获取数据
        mediaLoader = new LocalMediaLoader(this, config.mimeType, config.isGif, config.videoMaxSecond, config.videoMinSecond);
        rxPermissions.request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .compose(this.<Boolean>bindUntilEvent(ActivityEvent.DESTROY))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if (aBoolean) {
                            mHandler.sendEmptyMessage(SHOW_DIALOG);
                            readLocalMedia();
                        } else {
                            ToastManage.s(mContext, getString(R.string.picture_jurisdiction));
                        }
                    }
                });
        if (savedInstanceState != null) {
            // 防止拍照内存不足时activity被回收，导致拍照后的图片未选中
            selectionMedias = PictureSelector.obtainSelectorList(savedInstanceState);
        }
    }

    /**
     * get LocalMedia s
     */
    protected void readLocalMedia() {
        mediaLoader.loadAllMedia(new LocalMediaLoader.LocalMediaLoadListener() {
            @Override
            public void loadComplete(List<LocalMediaFolder> folders) {
                if (folders.size() > 0 && !isPreview) {
                    LocalMediaFolder folder = folders.get(0);
                    folder.setChecked(true);
                    List<LocalMedia> localImg = folder.getImages();
                    // 这里解决有些机型会出现拍照完，相册列表不及时刷新问题
                    // 因为onActivityResult里手动添加拍照后的照片，
                    // 如果查询出来的图片大于或等于当前adapter集合的图片则取更新后的，否则就取本地的
                    if (localImg.size() >= images.size()) {
                        images = localImg;
                        folderWindow.bindFolder(folders);
                    }
                }
                if (adapter != null && !isPreview) {
                    if (images == null) {
                        images = new ArrayList<>();
                    }
                    adapter.bindImagesData(images);
                    emptyTv.setVisibility(images.size() > 0 ? View.INVISIBLE : View.VISIBLE);
                }
                mHandler.sendEmptyMessage(DISMISS_DIALOG);
                isPreview = false;
            }
        });
    }

    /**
     * none number style
     */
    private void isNumComplete(boolean numComplete) {
        pictureTvOk.setText(numComplete ?
                getString(R.string.picture_done_front_num, 0, config.selectionMode == PictureConfig.SINGLE ? 1 : config.maxSelectNum)
                : getString(R.string.picture_please_select));
        if (!numComplete) {
            animation = AnimationUtils.loadAnimation(this, R.anim.modal_in);
        }
        animation = numComplete ? null : AnimationUtils.loadAnimation(this, R.anim.modal_in);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null) {
            List<LocalMedia> selectedImages = adapter.getSelectedImages();
            outState.putSerializable(PictureConfig.EXTRA_SELECT_LIST, (Serializable) selectedImages);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.picture_left_back || id == R.id.picture_right) {
            //返回按钮
            if (folderWindow.isShowing()) {
                folderWindow.dismiss();
            } else {
                closeActivity();
            }
        } else if (id == R.id.picture_title) {
            //title，切换相册
            if (folderWindow.isShowing()) {
                folderWindow.dismiss();
            } else {
                if (images != null && images.size() > 0) {
                    folderWindow.showAsDropDown(pictureTitleRl);
                    List<LocalMedia> selectedImages = adapter.getSelectedImages();
                    folderWindow.notifyDataCheckedStatus(selectedImages);
                }
            }
        } else if (id == R.id.picture_id_preview) {
            //预览按钮
            isPreview = true;
            List<LocalMedia> selectedImages = adapter.getSelectedImages();
            List<LocalMedia> medias = new ArrayList<>(selectedImages);
            Bundle bundle = new Bundle();
            bundle.putSerializable(PictureConfig.EXTRA_PREVIEW_SELECT_LIST, (Serializable) medias);
            bundle.putSerializable(PictureConfig.EXTRA_SELECT_LIST, (Serializable) selectedImages);
            bundle.putBoolean(PictureConfig.EXTRA_BOTTOM_PREVIEW, true);
            startActivity(PicturePreviewActivity.class, bundle, UCrop.REQUEST_CROP);
            overridePendingTransition(R.anim.a5, 0);
        } else if (id == R.id.id_ll_ok) {
            //确定按钮
            List<LocalMedia> images = adapter.getSelectedImages();
            LocalMedia image = images.size() > 0 ? images.get(0) : null;
            // 如果设置了图片最小选择数量，则判断是否满足条件
            if (config.minSelectNum > 0 && config.selectionMode == PictureConfig.MULTIPLE) {
                if (images.size() < config.minSelectNum) {
                    String str = getString(R.string.picture_min_img_num, config.minSelectNum);
                    ToastManage.s(mContext, str);
                    return;
                }
            }
            if (config.enableCrop && images.size() == 1 && image.getPictureType().startsWith(PictureConfig.IMAGE)) {
                //只能对1张图片进行截图操作
                originalPath = image.getPath();
                startCrop(originalPath);
            } else if (config.isCompress) {
                boolean isAllPhoto = true;
                for (LocalMedia media : images) {
                    if (!media.getPictureType().startsWith(PictureConfig.IMAGE)) {
                        isAllPhoto = false;
                        break;
                    }
                }
                if (isAllPhoto) {
                    //图片才压缩，视频不管
                    compressImage(images);
                } else {
                    onResult(images);
                }
            } else {
                onResult(images);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        closeActivity();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (RxBus.getDefault().isRegistered(this)) {
            RxBus.getDefault().unregister(this);
        }
        ImagesObservable.getInstance().clearLocalMedia();
        if (animation != null) {
            animation.cancel();
            animation = null;
        }
    }

    @Override
    public void onItemClick(String folderName, List<LocalMedia> images) {
        pictureTitleTv.setText(folderName);
        adapter.bindImagesData(images);
        folderWindow.dismiss();
    }

    @Override
    public void onChange(List<LocalMedia> selectImages) {
        changeImageNumber(selectImages);
    }

    @Override
    public void onPictureClick(LocalMedia media, int position) {
        List<LocalMedia> images = adapter.getImages();
        startPreview(images, position);
    }

    /**
     * change image selector state
     *
     * @param selectImages
     */
    public void changeImageNumber(List<LocalMedia> selectImages) {
        boolean enable = selectImages.size() != 0;
        if (enable) {
            idOkLl.setEnabled(true);
            pictureIdPreviewTv.setEnabled(true);
            pictureIdPreviewTv.setSelected(true);
            pictureTvOk.setSelected(true);
            if (numComplete) {
                pictureTvOk.setText(getString(R.string.picture_done_front_num, selectImages.size(),
                        config.selectionMode == PictureConfig.SINGLE ? 1 : config.maxSelectNum));
            } else {
                if (!anim) {
                    pictureImgNumTv.startAnimation(animation);
                }
                pictureImgNumTv.setVisibility(View.VISIBLE);
                pictureImgNumTv.setText(String.valueOf(selectImages.size()));
                pictureTvOk.setText(R.string.picture_completed);
                anim = false;
            }
        } else {
            idOkLl.setEnabled(false);
            pictureIdPreviewTv.setEnabled(false);
            pictureIdPreviewTv.setSelected(false);
            pictureTvOk.setSelected(false);
            if (numComplete) {
                pictureTvOk.setText(getString(R.string.picture_done_front_num, 0,
                        config.selectionMode == PictureConfig.SINGLE ? 1 : config.maxSelectNum));
            } else {
                pictureImgNumTv.setVisibility(View.INVISIBLE);
                pictureTvOk.setText(R.string.picture_please_select);
            }
        }
    }

    /**
     * preview image and video
     *
     * @param previewImages
     * @param position
     */
    public void startPreview(List<LocalMedia> previewImages, int position) {
        isPreview = true;
        Bundle bundle = new Bundle();
        List<LocalMedia> selectedImages = adapter.getSelectedImages();
        ImagesObservable.getInstance().saveLocalMedia(previewImages);
        bundle.putSerializable(PictureConfig.EXTRA_SELECT_LIST, (Serializable) selectedImages);
        bundle.putInt(PictureConfig.EXTRA_POSITION, position);
        startActivity(PicturePreviewActivity.class, bundle, UCrop.REQUEST_CROP);
        overridePendingTransition(R.anim.a5, 0);
    }

    /**
     * rxBus 回调
     *
     * @param obj
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void rxBusCallback(EventEntity obj) {
        switch (obj.what) {
            case PictureConfig.UPDATE_FLAG:
                // 预览时勾选图片更新回调
                List<LocalMedia> selectImages = obj.medias;
                anim = selectImages.size() > 0;
                int position = obj.position;
                Log.i("刷新下标:", String.valueOf(position));
                adapter.bindSelectImages(selectImages);
                adapter.notifyItemChanged(position);
                break;

            case PictureConfig.PREVIEW_DATA_FLAG:
                List<LocalMedia> medias = obj.medias;
                if (config.isCompress) {
                    boolean isAllPhoto = true;
                    for (LocalMedia media : medias) {
                        if (!media.getPictureType().startsWith(PictureConfig.IMAGE)) {
                            isAllPhoto = false;
                            break;
                        }
                    }

                    if (isAllPhoto) {
                        //图片才压缩，视频不管
                        compressImage(medias);
                    } else {
                        onResult(medias);
                    }
                } else {
                    onResult(medias);
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            List<LocalMedia> medias = new ArrayList<>();
            LocalMedia media;
            String imageType;
            switch (requestCode) {
                case UCrop.REQUEST_CROP:
                    Uri resultUri = UCrop.getOutput(data);
                    String cutPath = resultUri.getPath();
                    if (adapter != null) {
                        // 取单张裁剪已选中图片的path作为原图
                        List<LocalMedia> mediaList = adapter.getSelectedImages();
                        media = mediaList != null && mediaList.size() > 0 ? mediaList.get(0) : null;
                        if (media != null) {
                            originalPath = media.getPath();
                            media = new LocalMedia(originalPath, media.getDuration(), false,
                                    media.getPosition(), media.getNum(), config.mimeType);
                            media.setCutPath(cutPath);
                            media.setCut(true);
                            imageType = PictureMimeType.createImageType(cutPath);
                            media.setPictureType(imageType);
                            medias.add(media);
                            handlerResult(medias);
                        }
                    } else if (config.camera) {
                        // 单独拍照
                        media = new LocalMedia(cameraPath, 0, false, 0, 0, config.mimeType);
                        media.setCut(true);
                        media.setCutPath(cutPath);
                        imageType = PictureMimeType.createImageType(cutPath);
                        media.setPictureType(imageType);
                        medias.add(media);
                        handlerResult(medias);
                    }
                    break;
                case PictureConfig.REQUEST_CAMERA:
                    // on take photo success
                    String imagePath = data.getStringExtra("imagePath");
                    String videoPath = data.getStringExtra("videoPath");
                    if (!TextUtils.isEmpty(imagePath)) {
                        cameraPath = imagePath;
                    } else {
                        cameraPath = videoPath;
                    }
                    final File file = new File(cameraPath);
                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    // 生成新拍照片或视频对象
                    media = new LocalMedia();
                    media.setPath(cameraPath);
                    String toType = PictureMimeType.fileToType(file);
                    boolean eqVideo = toType.startsWith(PictureConfig.VIDEO);
                    int duration = eqVideo ? PictureMimeType.getLocalVideoDuration(cameraPath) : 0;
                    String pictureType = eqVideo ? PictureMimeType.createVideoType(cameraPath) : PictureMimeType.createImageType(cameraPath);
                    media.setPictureType(pictureType);
                    media.setDuration(duration);
                    media.setMimeType(config.mimeType);
                    // 因为加入了单独拍照功能，所有如果是单独拍照的话也默认为单选状态
                    boolean eqImg = toType.startsWith(PictureConfig.IMAGE);
                    if (config.enableCrop && eqImg) {
                        // 去裁剪
                        originalPath = cameraPath;
                        startCrop(cameraPath);
                    } else if (config.isCompress && eqImg) {
                        // 去压缩
                        medias.add(media);
                        compressImage(medias);
                    } else {
                        // 不裁剪 不压缩 直接返回结果
                        medias.add(media);
                        onResult(medias);
                    }
                    break;
                default:
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (config.camera) {
                closeActivity();
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable throwable = (Throwable) data.getSerializableExtra(UCrop.EXTRA_ERROR);
            ToastManage.s(mContext, throwable.getMessage());
        } else if (requestCode == PictureConfig.CAMERA_ERROR) {
            //相机开启失败
            ToastManage.s(mContext, getString(R.string.picture_camera));
            closeActivity();
        }
    }
}
