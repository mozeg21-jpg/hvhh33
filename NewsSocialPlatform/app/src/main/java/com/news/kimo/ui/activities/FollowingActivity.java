package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityFollowingBinding;
import com.news.kimo.models.Follower;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity showing followers, following, and follow requests for a given user.
 * Tabs: المتابعون, المتابَعون, طلبات المتابعة (only visible for current user).
 * Supports follow/unfollow, accept/reject requests, and remove follower.
 */
public class FollowingActivity extends BaseActivity {

    private static final String TAG = "FollowingActivity";

    private ActivityFollowingBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private String currentUid;
    private String targetUid;
    private int initialTab;

    private TabLayout tabLayout;
    private RecyclerView rvUsers;
    private View layoutEmpty;
    private TextView tvEmptyText;

    private FollowingAdapter adapter;
    private final List<User> userList = new ArrayList<>();
    private final List<String> loadedUids = new ArrayList<>();

    private ChildEventListener followersListener;
    private ChildEventListener followingListener;
    private ChildEventListener requestsListener;
    private Query activeFollowersQuery;
    private Query activeFollowingQuery;
    private Query activeRequestsQuery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFollowingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        if (getIntent() != null) {
            targetUid = getIntent().getStringExtra(Constants.EXTRA_USER_ID);
            initialTab = getIntent().getIntExtra("tab", 0);
        }
        if (targetUid == null || targetUid.isEmpty()) {
            targetUid = currentUid;
        }

        initViews();
        setupTabs();
        setupRecyclerView();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        tabLayout = binding.tabLayout;
        rvUsers = binding.rvUsers;
        layoutEmpty = binding.layoutEmpty;
        tvEmptyText = binding.tvEmptyText;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_followers));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_following));

        // Show follow requests tab only for current user
        if (currentUid.equals(targetUid)) {
            tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_follow_requests));
        }

        // Set initial tab
        TabLayout.Tab tab = tabLayout.getTabAt(initialTab);
        if (tab != null) {
            tab.select();
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                detachAllListeners();
                userList.clear();
                loadedUids.clear();
                adapter.notifyDataSetChanged();

                switch (position) {
                    case 0:
                        loadFollowers();
                        break;
                    case 1:
                        loadFollowing();
                        break;
                    case 2:
                        loadFollowRequests();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Load initial tab
        if (initialTab == 0) {
            loadFollowers();
        } else if (initialTab == 1) {
            loadFollowing();
        } else if (initialTab == 2 && currentUid.equals(targetUid)) {
            loadFollowRequests();
        }
    }

    private void setupRecyclerView() {
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FollowingAdapter();
        rvUsers.setAdapter(adapter);
    }

    // ==================================================================
    // Load Followers
    // ==================================================================

    private void loadFollowers() {
        activeFollowersQuery = rootRef.child(Constants.FOLLOWERS)
                .child(targetUid)
                .orderByChild("timestamp");

        followersListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Follower follower = snapshot.getValue(Follower.class);
                if (follower != null && !"pending".equals(follower.getStatus())) {
                    String uid = snapshot.getKey();
                    if (!loadedUids.contains(uid)) {
                        loadedUids.add(uid);
                        loadUserDetails(uid, follower.getTimestamp());
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String uid = snapshot.getKey();
                int index = loadedUids.indexOf(uid);
                if (index >= 0) {
                    loadedUids.remove(index);
                    userList.remove(index);
                    adapter.notifyItemRemoved(index);
                    updateEmptyState(R.string.no_followers);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadFollowers cancelled", error.toException());
            }
        };

        activeFollowersQuery.addChildEventListener(followersListener);
    }

    // ==================================================================
    // Load Following
    // ==================================================================

    private void loadFollowing() {
        activeFollowingQuery = rootRef.child(Constants.FOLLOWING)
                .child(targetUid)
                .orderByChild("timestamp");

        followingListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String uid = snapshot.getKey();
                if (!loadedUids.contains(uid)) {
                    loadedUids.add(uid);
                    Follower f = snapshot.getValue(Follower.class);
                    long ts = f != null ? f.getTimestamp() : System.currentTimeMillis();
                    loadUserDetails(uid, ts);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String uid = snapshot.getKey();
                int index = loadedUids.indexOf(uid);
                if (index >= 0) {
                    loadedUids.remove(index);
                    userList.remove(index);
                    adapter.notifyItemRemoved(index);
                    updateEmptyState(R.string.no_following);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadFollowing cancelled", error.toException());
            }
        };

        activeFollowingQuery.addChildEventListener(followingListener);
    }

    // ==================================================================
    // Load Follow Requests
    // ==================================================================

    private void loadFollowRequests() {
        activeRequestsQuery = rootRef.child(Constants.FOLLOWERS)
                .child(targetUid)
                .orderByChild("status")
                .equalTo("pending");

        requestsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String uid = snapshot.getKey();
                if (!loadedUids.contains(uid)) {
                    loadedUids.add(uid);
                    Follower f = snapshot.getValue(Follower.class);
                    long ts = f != null ? f.getTimestamp() : System.currentTimeMillis();
                    loadUserDetails(uid, ts);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Follower f = snapshot.getValue(Follower.class);
                if (f != null && !"pending".equals(f.getStatus())) {
                    String uid = snapshot.getKey();
                    int index = loadedUids.indexOf(uid);
                    if (index >= 0) {
                        loadedUids.remove(index);
                        userList.remove(index);
                        adapter.notifyItemRemoved(index);
                        updateEmptyState(R.string.no_follow_requests);
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String uid = snapshot.getKey();
                int index = loadedUids.indexOf(uid);
                if (index >= 0) {
                    loadedUids.remove(index);
                    userList.remove(index);
                    adapter.notifyItemRemoved(index);
                    updateEmptyState(R.string.no_follow_requests);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadFollowRequests cancelled", error.toException());
            }
        };

        activeRequestsQuery.addChildEventListener(requestsListener);
    }

    // ==================================================================
    // Load User Details
    // ==================================================================

    private void loadUserDetails(String uid, long timestamp) {
        rootRef.child(Constants.USERS).child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            user.setUid(snapshot.getKey());
                            userList.add(user);
                            Collections.sort(userList, (u1, u2) ->
                                    Long.compare(u2.getCreatedAt(), u1.getCreatedAt()));
                            adapter.notifyDataSetChanged();
                            updateEmptyState(R.string.no_followers);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadUserDetails cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Follow / Unfollow / Accept / Reject / Remove
    // ==================================================================

    private void toggleFollow(User user) {
        if (currentUid.isEmpty()) return;
        String targetUserUid = user.getUid();

        // Check if already following
        rootRef.child(Constants.FOLLOWING).child(currentUid).child(targetUserUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            unfollowUser(targetUserUid, user);
                        } else {
                            followUser(targetUserUid, user);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "toggleFollow cancelled", error.toException());
                    }
                });
    }

    private void followUser(String targetUserUid, User user) {
        long now = System.currentTimeMillis();

        // Add to following
        Map<String, Object> followingData = new HashMap<>();
        followingData.put("followerUid", currentUid);
        followingData.put("followingUid", targetUserUid);
        followingData.put("timestamp", now);
        followingData.put("status", "accepted");

        rootRef.child(Constants.FOLLOWING).child(currentUid).child(targetUserUid)
                .setValue(followingData);

        // Add to their followers
        Map<String, Object> followerData = new HashMap<>();
        followerData.put("followerUid", currentUid);
        followerData.put("followingUid", targetUserUid);
        followerData.put("timestamp", now);
        followerData.put("status", "accepted");

        rootRef.child(Constants.FOLLOWERS).child(targetUserUid).child(currentUid)
                .setValue(followerData)
                .addOnSuccessListener(aVoid -> {
                    // Update follower counts
                    rootRef.child(Constants.USERS).child(currentUid)
                            .child("followingCount").child(currentUid).setValue(true);
                    rootRef.child(Constants.USERS).child(targetUserUid)
                            .child("followersCount").child(targetUserUid).setValue(true);
                });

        showMessage(getString(R.string.followed_successfully));
    }

    private void unfollowUser(String targetUserUid, User user) {
        rootRef.child(Constants.FOLLOWING).child(currentUid).child(targetUserUid).removeValue();
        rootRef.child(Constants.FOLLOWERS).child(targetUserUid).child(currentUid).removeValue()
                .addOnSuccessListener(aVoid -> {
                    rootRef.child(Constants.USERS).child(currentUid)
                            .child("followingCount").child(targetUserUid).removeValue();
                    rootRef.child(Constants.USERS).child(targetUserUid)
                            .child("followersCount").child(currentUid).removeValue();
                });
        showMessage(getString(R.string.unfollowed_successfully));
    }

    private void acceptFollowRequest(String requesterUid) {
        rootRef.child(Constants.FOLLOWERS).child(currentUid).child(requesterUid)
                .child("status").setValue("accepted")
                .addOnSuccessListener(aVoid -> {
                    // Also add to requester's following
                    Map<String, Object> followingData = new HashMap<>();
                    followingData.put("followerUid", requesterUid);
                    followingData.put("followingUid", currentUid);
                    followingData.put("timestamp", System.currentTimeMillis());
                    followingData.put("status", "accepted");
                    rootRef.child(Constants.FOLLOWING).child(requesterUid).child(currentUid)
                            .setValue(followingData);

                    showMessage(getString(R.string.request_accepted));
                });
    }

    private void rejectFollowRequest(String requesterUid) {
        rootRef.child(Constants.FOLLOWERS).child(currentUid).child(requesterUid)
                .removeValue()
                .addOnSuccessListener(aVoid ->
                        showMessage(getString(R.string.request_rejected)));
    }

    private void removeFollower(String followerUid) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_follower_title)
                .setMessage(R.string.remove_follower_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    rootRef.child(Constants.FOLLOWERS).child(targetUid).child(followerUid).removeValue();
                    rootRef.child(Constants.FOLLOWING).child(followerUid).child(targetUid).removeValue()
                            .addOnSuccessListener(aVoid ->
                                    showMessage(getString(R.string.follower_removed)));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ==================================================================
    // Empty State
    // ==================================================================

    private void updateEmptyState(int emptyMessageRes) {
        if (userList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvUsers.setVisibility(View.GONE);
            tvEmptyText.setText(emptyMessageRes);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvUsers.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Detach Listeners
    // ==================================================================

    private void detachAllListeners() {
        if (followersListener != null && activeFollowersQuery != null) {
            activeFollowersQuery.removeChildEventListener(followersListener);
        }
        if (followingListener != null && activeFollowingQuery != null) {
            activeFollowingQuery.removeChildEventListener(followingListener);
        }
        if (requestsListener != null && activeRequestsQuery != null) {
            activeRequestsQuery.removeChildEventListener(requestsListener);
        }
    }

    // ==================================================================
    // Adapter
    // ==================================================================

    private class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.UserViewHolder> {

        private boolean isRequestsTab() {
            return tabLayout.getSelectedTabPosition() == 2 && currentUid.equals(targetUid);
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_follow, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = userList.get(position);
            holder.bind(user);
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            TextView tvName, tvBio;
            Button btnAction;
            View btnAccept, btnReject;

            UserViewHolder(View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivPhoto);
                tvName = itemView.findViewById(R.id.tvName);
                tvBio = itemView.findViewById(R.id.tvBio);
                btnAction = itemView.findViewById(R.id.btnAction);
                btnAccept = itemView.findViewById(R.id.btnAccept);
                btnReject = itemView.findViewById(R.id.btnReject);
            }

            void bind(User user) {
                tvName.setText(user.getName());
                tvBio.setText(user.getBio() != null ? user.getBio() : "");
                loadCircularImage(user.getPhotoUrl(), ivPhoto);

                // Click to profile
                itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_USER_ID, user.getUid());
                    openActivity(ProfileActivity.class, bundle);
                });

                if (isRequestsTab()) {
                    // Show accept/reject for follow requests
                    btnAction.setVisibility(View.GONE);
                    btnAccept.setVisibility(View.VISIBLE);
                    btnReject.setVisibility(View.VISIBLE);

                    btnAccept.setOnClickListener(v -> acceptFollowRequest(user.getUid()));
                    btnReject.setOnClickListener(v -> rejectFollowRequest(user.getUid()));
                } else if (tabLayout.getSelectedTabPosition() == 0
                        && !currentUid.equals(user.getUid())) {
                    // Followers tab: show remove button for own followers
                    btnAction.setVisibility(View.VISIBLE);
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    btnAction.setText(R.string.remove);
                    btnAction.setOnClickListener(v -> removeFollower(user.getUid()));
                } else {
                    // Following tab or other user's followers: show follow/unfollow
                    btnAction.setVisibility(View.VISIBLE);
                    btnAccept.setVisibility(View.GONE);
                    btnReject.setVisibility(View.GONE);
                    checkFollowStatus(user, btnAction);
                    btnAction.setOnClickListener(v -> toggleFollow(user));
                }
            }

            private void checkFollowStatus(User user, Button btnAction) {
                rootRef.child(Constants.FOLLOWING).child(currentUid).child(user.getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    btnAction.setText(R.string.unfollow);
                                } else {
                                    btnAction.setText(R.string.follow);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                btnAction.setText(R.string.follow);
                            }
                        });
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        detachAllListeners();
        super.onDestroy();
    }
}
