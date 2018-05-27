/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.greenthumb;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.udacity.greenthumb.data.DbContract.PlantEntry;
import com.google.firebase.udacity.greenthumb.data.Plant;
import com.google.firebase.udacity.greenthumb.data.PlantCartHelper;

/**
 * {@link PlantDetailActivity} displays a plant's name and description
 * and allows the user to check out the plant to the shopping cart.
 */
public class PlantDetailActivity extends AppCompatActivity
        implements View.OnClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = PlantDetailActivity.class.getSimpleName();

    private static final String INTENT_EXTRA_ITEM = "item_id";
    private static final int PLANT_DETAIL_LOADER = 2;

    private int mItemId;
    private Plant mPlant;

    private Cursor mCursor;

    private Toolbar mToolbar;
    private TextView mItemDescription;
    private TextView mItemPrice;

    public static void startActivity(Context context, int itemPosition) {
        Intent i = new Intent(context, PlantDetailActivity.class);
        i.putExtra(INTENT_EXTRA_ITEM, itemPosition);
        context.startActivity(i);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("");
        mItemDescription = (TextView) findViewById(R.id.text_view_item_description);
        mItemPrice = (TextView) findViewById(R.id.text_view_item_price);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        setSupportActionBar(mToolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        fab.setOnClickListener(this);

        mItemId = getIntent().getIntExtra(INTENT_EXTRA_ITEM, 0);

        getSupportLoaderManager().initLoader(PLANT_DETAIL_LOADER, null, this);

        // Call the getDynamicLink() method to receive the deep link
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri uri = null;
                        if (pendingDynamicLinkData != null) {
                            // An example of a dynamic link in Green Thumb is
                            // http://greenthumb.example.com/1
                            // Display the correct plant description based on the link
                            uri = pendingDynamicLinkData.getLink();
                            String plantId = uri.getLastPathSegment(); // extract plantId from the link
                            mItemId = Integer.parseInt(plantId);
                            getSupportLoaderManager().restartLoader(
                                    PLANT_DETAIL_LOADER, null, PlantDetailActivity.this);
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "getDynamicLink:onFailure", e);
                    }
                });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab:
                if (mPlant == null)
                    return;

                int quantity = 1;
                PlantCartHelper.addCartQuantity(this, mPlant.id, quantity);
                Snackbar.make(v, R.string.shopping_cart_item_added, Snackbar.LENGTH_SHORT).show();

                Analytics.logEventAddToCart(this, mPlant, quantity);
                break;
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                PlantEntry._ID,
                PlantEntry.COLUMN_NAME,
                PlantEntry.COLUMN_DESCRIPTION,
                PlantEntry.COLUMN_PRICE
        };
        String selection = PlantEntry._ID + " = " + mItemId;
        return new CursorLoader(this,
                PlantEntry.CONTENT_URI,
                projection,
                selection,
                null,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursor = data;
        mCursor.moveToFirst();

        mPlant = new Plant(data);
        mToolbar.setTitle(mPlant.name);
        mItemDescription.setText(mPlant.description);
        mItemPrice.setText(getString(R.string.plant_credits, mPlant.price));

        // Called in onLoadFinished in PlantDetailActivity, once the Plant object is queried and initialized
        Analytics.logEventViewItem(this, mPlant);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursor = null;
    }
}
