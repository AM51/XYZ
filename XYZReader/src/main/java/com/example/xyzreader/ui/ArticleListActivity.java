package com.example.xyzreader.ui;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.transition.Explode;
import android.transition.Fade;
import android.transition.Slide;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private Toolbar mToolbar;
    //private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_article_list);

       // setUpAnimations();

        setContentView(R.layout.activity_list_cordinator_layout);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);


        final View toolbarContainerView = findViewById(R.id.toolbar_container);

        //mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        getLoaderManager().initLoader(0, null, this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CollapsingToolbarLayout collapsingToolbar =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        collapsingToolbar.setTitle("XYZ Reader");
        collapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT);
        collapsingToolbar.setContentScrimColor(Color.BLUE);
        if (savedInstanceState == null) {
            refresh();
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setUpAnimations() {
        Explode explode = new Explode();
        explode.setDuration(1000);

        getWindow().setExitTransition(explode);


        Slide slide = new Slide();
        slide.setDuration(1000);
        getWindow().setReturnTransition(slide);
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }

    private boolean mIsRefreshing = false;

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                //updateRefreshingUI();
            }
        }
    };
//
//    private void updateRefreshingUI() {
//        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String transitionName = getString(R.string.image_transition);
                    ImageView imageView = (ImageView)view.findViewById(R.id.thumbnail);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        Log.v("archit","Making scene transition");
                        Bundle bundle  = ActivityOptions.makeSceneTransitionAnimation(ArticleListActivity.this,imageView,transitionName).toBundle();
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))),bundle);
                    } else {
                        Log.v("archit","Not making scene transition");
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))));
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            holder.thumbnailView.setImageUrl(
                    mCursor.getString(ArticleLoader.Query.THUMB_URL),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO));
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
