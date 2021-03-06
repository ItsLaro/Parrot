package com.codepath.apps.restclienttemplate.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.codepath.apps.restclienttemplate.R;
import com.codepath.apps.restclienttemplate.TwitterApp;
import com.codepath.apps.restclienttemplate.TwitterClient;
import com.codepath.apps.restclienttemplate.adapters.TweetsAdapter;
import com.codepath.apps.restclienttemplate.databinding.ActivityUserProfileBinding;
import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.apps.restclienttemplate.models.User;
import com.codepath.apps.restclienttemplate.utilities.EndlessRecyclerViewScrollListener;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

import static android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN;

public class UserProfileActivity extends AppCompatActivity {

    private static String TAG = "UserProfileActivity";

    private ActivityUserProfileBinding binding;

    private TwitterClient client;
    private TweetsAdapter tweetsAdapter;
    private EndlessRecyclerViewScrollListener scrollListener;
    private LinearLayoutManager layoutManager;

    private List<Tweet> profileTweets = new ArrayList<>();
    private int lastVisitedTweetPosition = 0;

    User selectedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        View userProfileView = binding.getRoot();
        setContentView(userProfileView);

        //Client
        client = TwitterApp.getRestClient(this);

        //Getting parcel from last Activity
        selectedUser = Parcels.unwrap(getIntent().getParcelableExtra("user_object"));
        Log.d(TAG, "Loaded details for tweet: " + selectedUser.getUsername());

        //Setting views to passed Tweet data
        binding.displayName.setText(selectedUser.getName());
        binding.userHandle.setText(selectedUser.getUsername());
        binding.userBio.setText(selectedUser.getBio());
        binding.followingCount.setText(Integer.toString(selectedUser.getFollowingCount()));
        binding.followerCount.setText(Integer.toString(selectedUser.getFollowersCount()));

        //Profile Picture
        Glide.with(this)
                .load(selectedUser.getProfileImageUrl())
                .placeholder(R.drawable.default_profilepic)
                .transform(new CircleCrop())
                .into(binding.profileImage);
        //Banner
        Glide.with(this)
                .load(selectedUser.getProfileBanner())
                .placeholder(R.drawable.default_banner)
                .centerCrop()
                .into(binding.profileBanner);

        //Adapter
        TweetsAdapter.OnClickListener onClickListener = new TweetsAdapter.OnClickListener() {
            @Override
            public void onItemClick(int position) {

                Intent tweetDetailsIntent = new Intent(UserProfileActivity.this,
                        TweetDetailsActivity.class);

                /* Storing position so we can refresh recycle view
                and smooth scroll back to where we were */
                lastVisitedTweetPosition = position;

                //Passing data to the intent
                tweetDetailsIntent.putExtra("tweet_object", Parcels.wrap(profileTweets.get(position)));

                startActivity(tweetDetailsIntent);
            }
        };

        tweetsAdapter = new TweetsAdapter(this, profileTweets, onClickListener);
        layoutManager = new LinearLayoutManager(this);
        binding.profileTimeline.setLayoutManager(layoutManager);
        binding.profileTimeline.setAdapter(tweetsAdapter);

        populatePrfoileTimeline();

        //LISTENERS

        //Click listener on Following field
        binding.followingContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Clicked following for: " + selectedUser.getUsername());

                Intent followingListIntent = new Intent(UserProfileActivity.this, UserFollowingListActivity.class);

                //Passing data to the intent
                followingListIntent.putExtra("user_object", Parcels.wrap(selectedUser));

                UserProfileActivity.this.startActivity(followingListIntent);

                Log.d(TAG, "Following activity initiated.");
            }
        });

        //Click listener on Follower field
        binding.followersContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Clicked followers for: " + selectedUser.getUsername());

                Intent followerListIntent = new Intent(UserProfileActivity.this, UserFollowersListActivity.class);

                //Passing data to the intent
                followerListIntent.putExtra("user_object", Parcels.wrap(selectedUser));

                UserProfileActivity.this.startActivity(followerListIntent);

                Log.d(TAG, "Followers activity initiated.");
            }
        });
    }

    private void populatePrfoileTimeline() {
        client.getProfileTimeline(selectedUser.getUsername(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {

                Log.d(TAG, statusCode + ", Success: " + json.toString());
                JSONArray jsonArray = json.jsonArray;
                try {
                    tweetsAdapter.clear();
                    tweetsAdapter.addAll(Tweet.fromJSONArray(jsonArray));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable e) {
                Log.d(TAG, statusCode + ", Failure:" + response + "; Error: " + e);
            }
        });
    }
}