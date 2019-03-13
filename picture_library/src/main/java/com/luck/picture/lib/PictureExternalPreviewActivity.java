package com.luck.picture.lib;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.TextView;

import com.luck.picture.lib.adapter.PicturePreviewPageAdapter;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.widget.PreviewViewPager;

import java.util.ArrayList;
import java.util.List;

/**
 * 提供外部图片预览的页面
 * <p>
 * project：PictureSelector
 * package：com.luck.picture.ui
 * email：邮箱->893855882@qq.com
 * data：17/01/18
 *
 * @author luck
 */
public class PictureExternalPreviewActivity extends PictureBaseActivity implements View.OnClickListener {
    private TextView pictureTitleTv;
    private PreviewViewPager viewPager;
    private List<LocalMedia> images = new ArrayList<>();
    private int position = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_activity_external_preview);
        pictureTitleTv = findViewById(R.id.picture_title_tv);
        viewPager = findViewById(R.id.preview_pager);
        position = getIntent().getIntExtra(PictureConfig.EXTRA_POSITION, 0);
        images = (List<LocalMedia>) getIntent().getSerializableExtra(PictureConfig.EXTRA_PREVIEW_SELECT_LIST);
        findViewById(R.id.left_back_ib).setOnClickListener(this);
        initViewPageAdapterData();
    }

    private void initViewPageAdapterData() {
        updateTitleStr(position);
        PicturePreviewPageAdapter adapter = new PicturePreviewPageAdapter(images, this, new PicturePreviewPageAdapter.OnCallBackListener() {
            @Override
            public void onActivityBackPressed() {
                onBackPressed();
            }

            @Override
            public void startLoading() {
                showPleaseDialog();
            }

            @Override
            public void endLoading() {
                dismissDialog();
            }
        });
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateTitleStr(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    /**
     * 更新title
     *
     * @param index 当前索引值
     */
    private void updateTitleStr(int index) {
        pictureTitleTv.setText(index + 1 + "/" + images.size());
    }

    @Override
    public void onClick(View v) {
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(0, R.anim.a3);
    }
}
