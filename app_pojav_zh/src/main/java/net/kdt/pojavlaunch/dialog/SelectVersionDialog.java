package net.kdt.pojavlaunch.dialog;

import android.app.Dialog;
import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.material.tabs.TabLayout;
import com.movtery.versionlist.VersionListView;
import com.movtery.versionlist.VersionSelectedListener;
import com.movtery.versionlist.VersionType;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.File;

public class SelectVersionDialog extends Dialog {
    private TabLayout mTabLayout;
    private TabLayout.Tab installedTab, releaseTab, snapshotTab, betaTab, alphaTab, returnTab;
    private VersionType versionType;
    private VersionListView versionListView;

    public SelectVersionDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
        setContentView(R.layout.dialog_select_version);
        init(context);
    }

    private void init(Context context) {
        mTabLayout = findViewById(R.id.zh_version_tab);
        bindTab(context);
        versionListView = findViewById(R.id.zh_version);

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                refresh(tab);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        refresh(mTabLayout.getTabAt(mTabLayout.getSelectedTabPosition()));
    }

    public void setOnVersionSelectedListener(VersionSelectedListener versionSelectedListener) {
        this.versionListView.setVersionSelectedListener(versionSelectedListener);
    }

    private void refresh(TabLayout.Tab tab) {
        setVersionType(tab);

        String[] installedVersionsList = new File(Tools.DIR_GAME_NEW + "/versions").list();
        //如果安装的版本列表为空，那么隐藏 已安装 按钮
        boolean hasInstalled = !(installedVersionsList == null || installedVersionsList.length == 0);
        if (hasInstalled) {
            if (mTabLayout.getTabAt(0) != installedTab) mTabLayout.addTab(installedTab, 0);
        } else {
            if (mTabLayout.getTabAt(0) == installedTab) mTabLayout.removeTab(installedTab);
        }

        versionListView.setVersionType(versionType);
    }

    private void setVersionType(TabLayout.Tab tab) {
        if (tab == installedTab) {
            versionType = VersionType.INSTALLED;
        } else if (tab == releaseTab) {
            versionType = VersionType.RELEASE;
        } else if (tab == snapshotTab) {
            versionType = VersionType.SNAPSHOT;
        } else if (tab == betaTab) {
            versionType = VersionType.BETA;
        } else if (tab == alphaTab) {
            versionType = VersionType.ALPHA;
        } else if (tab == returnTab) {
            this.dismiss();
        }
    }

    private void bindTab(Context context) {
        installedTab = mTabLayout.newTab();
        releaseTab = mTabLayout.newTab();
        snapshotTab = mTabLayout.newTab();
        betaTab = mTabLayout.newTab();
        alphaTab = mTabLayout.newTab();
        returnTab = mTabLayout.newTab();

        installedTab.setText(context.getString(R.string.mcl_setting_veroption_installed));
        releaseTab.setText(context.getString(R.string.mcl_setting_veroption_release));
        snapshotTab.setText(context.getString(R.string.mcl_setting_veroption_snapshot));
        betaTab.setText(context.getString(R.string.mcl_setting_veroption_oldbeta));
        alphaTab.setText(context.getString(R.string.mcl_setting_veroption_oldalpha));
        returnTab.setText(context.getString(R.string.zh_return));

        mTabLayout.addTab(installedTab);
        mTabLayout.addTab(releaseTab);
        mTabLayout.addTab(snapshotTab);
        mTabLayout.addTab(betaTab);
        mTabLayout.addTab(alphaTab);
        mTabLayout.addTab(returnTab);

        mTabLayout.selectTab(releaseTab);
    }
}
