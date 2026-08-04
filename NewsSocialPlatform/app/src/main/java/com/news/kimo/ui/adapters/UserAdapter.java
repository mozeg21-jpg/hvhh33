package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.news.kimo.R;
import com.news.kimo.databinding.ItemUserBinding;
import com.news.kimo.models.User;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private final Context context;
    private final List<User> userList;
    private final String currentUid;
    private final FirebaseFirestore db;
    private final Map<String, Boolean> followCache;
    private String searchQuery = "";

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnFollowClickListener {
        void onFollowClick(User user, int position, boolean isFollowing);
    }

    private OnUserClickListener onUserClickListener;
    private OnFollowClickListener onFollowClickListener;

    public UserAdapter(Context context, List<User> userList, String currentUid) {
        this.context = context;
        this.userList = userList != null ? userList : new ArrayList<>();
        this.currentUid = currentUid;
        this.db = FirebaseFirestore.getInstance();
        this.followCache = new HashMap<>();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemUserBinding binding = ItemUserBinding.inflate(inflater, parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(userList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < userList.size() && userList.get(position).getUid() != null) {
            return userList.get(position).getUid().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user, int position) {
            // Avatar
            Glide.with(context)
                    .load(user.getPhotoUrl())
                    .apply(RequestOptions.circleCropTransform()
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar))
                    .into(binding.ivUserAvatar);

            // Verified badge
            binding.ivVerifiedBadge.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);

            // Name
            binding.tvUserName.setText(user.getName());

            // Username (@)
            binding.tvUsername.setText("@" + (user.getUid() != null ? user.getUid().substring(0, Math.min(8, user.getUid().length())) : ""));

            // Bio (1 line)
            if (user.getBio() != null && !user.getBio().isEmpty()) {
                binding.tvUserBio.setVisibility(View.VISIBLE);
                binding.tvUserBio.setText(user.getBio());
                binding.tvUserBio.setMaxLines(1);
            } else {
                binding.tvUserBio.setVisibility(View.GONE);
            }

            // Follow/Unfollow button - check followers/{user.uid}/{currentUid}
            if (user.getUid().equals(currentUid)) {
                // Own profile - hide follow button
                binding.btnFollow.setVisibility(View.GONE);
            } else {
                binding.btnFollow.setVisibility(View.VISIBLE);
                // Check cache first
                if (followCache.containsKey(user.getUid())) {
                    setFollowButtonState(binding.btnFollow, followCache.get(user.getUid()), user);
                } else {
                    binding.btnFollow.setText(R.string.loading);
                    binding.btnFollow.setEnabled(false);
                    checkFollowStatus(user.getUid(), binding.btnFollow, user, position);
                }

                binding.btnFollow.setOnClickListener(v -> {
                    boolean isCurrentlyFollowing = followCache.containsKey(user.getUid()) && followCache.get(user.getUid());
                    // Handle private account
                    if (!isCurrentlyFollowing && user.isPrivate()) {
                        // Send follow request instead of direct follow
                        sendFollowRequest(user, position);
                    } else {
                        toggleFollow(user, position, isCurrentlyFollowing);
                    }
                });
            }

            // Click -> ProfileActivity
            itemView.setOnClickListener(v -> {
                if (onUserClickListener != null) {
                    onUserClickListener.onUserClick(user);
                } else {
                    context.startActivity(new Intent(context, ProfileActivity.class)
                            .putExtra(Constants.EXTRA_USER_ID, user.getUid()));
                }
            });
        }
    }

    // ---- Follow Status Check ----

    private void checkFollowStatus(String targetUid, View btnFollow, User user, int position) {
        db.collection(Constants.FOLLOWERS)
                .document(targetUid)
                .collection("items")
                .document(currentUid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean isFollowing = snapshot.exists();
                    followCache.put(targetUid, isFollowing);
                    setFollowButtonState(btnFollow, isFollowing, user);
                })
                .addOnFailureListener(e -> {
                    btnFollow.setEnabled(false);
                });
    }

    private void setFollowButtonState(View btnFollow, boolean isFollowing, User user) {
        android.widget.Button btn = (android.widget.Button) btnFollow;
        btn.setEnabled(true);
        if (isFollowing) {
            btn.setText(R.string.unfollow);
            btn.setBackgroundColor(context.getColor(R.color.surfaceColor));
            btn.setTextColor(context.getColor(R.color.colorPrimary));
        } else {
            if (user.isPrivate()) {
                btn.setText(R.string.follow_request);
            } else {
                btn.setText(R.string.follow);
            }
            btn.setBackgroundColor(context.getColor(R.color.colorPrimary));
            btn.setTextColor(context.getColor(android.R.color.white));
        }
    }

    // ---- Toggle Follow ----

    private void toggleFollow(User user, int position, boolean isCurrentlyFollowing) {
        if (isCurrentlyFollowing) {
            // Unfollow
            db.collection(Constants.FOLLOWERS)
                    .document(user.getUid())
                    .collection("items")
                    .document(currentUid)
                    .delete();
            db.collection(Constants.FOLLOWING)
                    .document(currentUid)
                    .collection("items")
                    .document(user.getUid())
                    .delete();
            // Update counts
            db.collection(Constants.USERS).document(user.getUid())
                    .update("followersCount", com.google.firebase.firestore.FieldValue.increment(-1));
            db.collection(Constants.USERS).document(currentUid)
                    .update("followingCount", com.google.firebase.firestore.FieldValue.increment(-1));
            followCache.put(user.getUid(), false);
            user.setFollowersCount(Math.max(0, user.getFollowersCount() - 1));
        } else {
            // Follow
            Map<String, Object> followData = new HashMap<>();
            followData.put("uid", currentUid);
            followData.put("timestamp", System.currentTimeMillis());
            db.collection(Constants.FOLLOWERS)
                    .document(user.getUid())
                    .collection("items")
                    .document(currentUid)
                    .set(followData);
            db.collection(Constants.FOLLOWING)
                    .document(currentUid)
                    .collection("items")
                    .document(user.getUid())
                    .set(followData);
            // Update counts
            db.collection(Constants.USERS).document(user.getUid())
                    .update("followersCount", com.google.firebase.firestore.FieldValue.increment(1));
            db.collection(Constants.USERS).document(currentUid)
                    .update("followingCount", com.google.firebase.firestore.FieldValue.increment(1));
            followCache.put(user.getUid(), true);
            user.setFollowersCount(user.getFollowersCount() + 1);
        }
        notifyItemChanged(position);
        if (onFollowClickListener != null) {
            onFollowClickListener.onFollowClick(user, position, !isCurrentlyFollowing);
        }
    }

    // ---- Send Follow Request (Private Account) ----

    private void sendFollowRequest(User user, int position) {
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("fromUid", currentUid);
        requestData.put("timestamp", System.currentTimeMillis());
        requestData.put("status", "pending");
        db.collection("follow_requests")
                .document(user.getUid())
                .collection("items")
                .document(currentUid)
                .set(requestData);
        followCache.put(user.getUid(), false);
        notifyItemChanged(position);
        android.widget.Toast.makeText(context, R.string.follow_request_sent, android.widget.Toast.LENGTH_SHORT).show();
    }

    // ---- Search Filter ----

    public void filter(String query) {
        this.searchQuery = query != null ? query.toLowerCase().trim() : "";
        notifyDataSetChanged();
    }

    // ---- Data Operations ----

    public void addUser(User user) {
        userList.add(0, user);
        notifyItemInserted(0);
    }

    public void addUsers(List<User> newUsers) {
        int startPos = userList.size();
        userList.addAll(newUsers);
        notifyItemRangeInserted(startPos, newUsers.size());
    }

    public void updateUser(int position, User user) {
        if (position >= 0 && position < userList.size()) {
            userList.set(position, user);
            notifyItemChanged(position);
        }
    }

    public void removeUser(int position) {
        if (position >= 0 && position < userList.size()) {
            userList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public User getUser(int position) {
        if (position >= 0 && position < userList.size()) {
            return userList.get(position);
        }
        return null;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setFollowStatus(String uid, boolean isFollowing) {
        followCache.put(uid, isFollowing);
    }

    // ---- Setters ----

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.onUserClickListener = listener;
    }

    public void setOnFollowClickListener(OnFollowClickListener listener) {
        this.onFollowClickListener = listener;
    }
}
