package com.pits.smbbrowse.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.pits.smbbrowse.R;
import com.pits.smbbrowse.tasks.BrowseDirectoryTask;
import com.pits.smbbrowse.utils.AppGlobals;
import com.pits.smbbrowse.utils.Helpers;
import com.pits.smbbrowse.utils.UiHelpers;

import java.net.MalformedURLException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

public class MainActivity extends AppCompatActivity implements ListView.OnItemClickListener {

    private ListView mListView;
    private NtlmPasswordAuthentication mAuth;
    private String mSambaHostAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (AppGlobals.isRunningForTheFirstTime()) {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
            return;
        }

        if (!Helpers.isWifiConnected(getApplicationContext())) {
            UiHelpers.showWifiNotConnectedDialog(MainActivity.this, true);
            return;
        }

        mSambaHostAddress = AppGlobals.getSambaHostAddress();
        mAuth = Helpers.getAuthenticationCredentials();

        mListView = (ListView) findViewById(R.id.content_list);
        mListView.setOnItemClickListener(this);

        new BrowseDirectoryTask(MainActivity.this, mSambaHostAddress, mAuth, mListView).execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (!Helpers.isWifiConnected(getApplicationContext())) {
            UiHelpers.showWifiNotConnectedDialog(MainActivity.this, false);
            return;
        }

        final SmbFile file = (SmbFile) parent.getItemAtPosition(position);
        try {
            if (file.isFile()) {
                UiHelpers.showLongToast(getApplicationContext(), "Cannot browse a file");
            } else {
                mListView.setAdapter(null);
                new BrowseDirectoryTask(
                        MainActivity.this, file.getCanonicalPath(), mAuth, mListView).execute();
            }
        } catch (SmbException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {

        if (!mSambaHostAddress.endsWith("/")) {
            mSambaHostAddress = mSambaHostAddress + "/";
        }

        if (!mSambaHostAddress.equals(AppGlobals.getCurrentBrowsedLocation())) {

            if (!Helpers.isWifiConnected(getApplicationContext())) {
                UiHelpers.showWifiNotConnectedDialog(MainActivity.this, false);
                return;
            }

            try {
                SmbFile file = new SmbFile(AppGlobals.getCurrentBrowsedLocation(), mAuth);
                String parent = file.getParent();
                new BrowseDirectoryTask(
                        MainActivity.this, parent, mAuth, mListView).execute();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else {
            super.onBackPressed();
        }
    }
}
