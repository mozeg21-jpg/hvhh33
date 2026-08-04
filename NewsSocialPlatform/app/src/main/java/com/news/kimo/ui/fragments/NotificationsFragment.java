package com.news.kimo.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.FragmentNotificationsBinding;
import com.news.kimo.models.NotificationItem;
import com.news.kimo.ui.activities.ChatActivity;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Notifications fragment for the bottom navigation bar.
 * Provides real-time notification display with unread badge,
 * mark all as read, swipe-to-delete, and navigation to relevant screens.
 */
public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    private static final String ARG_BADGE_LISTENER = "badge_listener";

    private FragmentNotificationsBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private String currentUid;

    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private TextView tvEmptyText;
    private TextView tvMarkAllRead;

    private NotificationAdapter adapter;
    private final List<NotificationItem> notificationList = new ArrayList<>();

    private ChildEventListener notificationChildListener;
    private Query activeQuery;
    private int unreadCount = 0;

    // Badge update callback for the activity
    private Runnable badgeUpdateCallback;

    public static NotificationsFragment getInstance() {
        return new NotificationsFragment();
    }

    /**
     * Set a callback to update the badge count on the bottom nav.
     */
    public void setBadgeUpdateCallback(Runnable callback) {
        this.badgeUpdateCallback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        setupRecyclerView();
        setupSwipeToDelete();
        loadNotifications();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        rvNotifications = binding.rvNotifications;
        layoutEmpty = binding.layoutEmpty;
        tvEmptyText = binding.tvEmptyText;
        tvMarkAllRead = binding.tvMarkAllRead;

        tvMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new NotificationAdapter();
        rvNotifications.setAdapter(adapter);
    }

    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                NotificationItem item = notificationList.get(position);
                deleteNotification(item, position);
            }
        }).attachToRecyclerView(rvNotifications);
    }

    // ==================================================================
    // Firebase Real-time Notifications
    // ==================================================================

    private void loadNotifications() {
        if (currentUid.isEmpty()) return;

        activeQuery = rootRef.child(Constants.NOTIFICATIONS)
                .child(currentUid)
                .orderByChild("timestamp")
                .limitToLast(100);

        notificationChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                NotificationItem notification = snapshot.getValue(NotificationItem.class);
                if (notification != null) {
                    notification.setNotificationId(snapshot.getKey());
                    notificationList.add(0, notification);
                    Collections.sort(notificationList, (n1, n2) ->
                            Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                    adapter.notifyDataSetChanged();
                    updateUnreadCount();
                    updateEmptyState();

                    // Auto mark new notifications as read after a short delay
                    if (!notification.isRead()) {
                        markSingleAsRead(notification.getNotificationId());
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                NotificationItem notification = snapshot.getValue(NotificationItem.class);
                if (notification != null) {
                    notification.setNotificationId(snapshot.getKey());
                    for (int i = 0; i < notificationList.size(); i++) {
                        if (notification.getNotificationId().equals(notificationList.get(i).getNotificationId())) {
                            notificationList.set(i, notification);
                            adapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    updateUnreadCount();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < notificationList.size(); i++) {
                    if (key.equals(notificationList.get(i).getNotificationId())) {
                        notificationList.remove(i);
                        adapter.notifyItemRemoved(i);
                        updateUnreadCount();
                        updateEmptyState();
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadNotifications cancelled", error.toException());
            }
        };

        activeQuery.addChildEventListener(notificationChildListener);
    }

    // ==================================================================
    // Mark as Read
    // ==================================================================

    private void markAllAsRead() {
        if (currentUid.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        boolean hasUnread = false;
        for (NotificationItem notification : notificationList) {
            if (!notification.isRead()) {
                hasUnread = true;
                notification.setRead(true);
                updates.put(notification.getNotificationId() + "/isRead", true);
            }
        }
        if (hasUnread) {
            rootRef.child(Constants.NOTIFICATIONS).child(currentUid)
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        adapter.notifyDataSetChanged();
                        unreadCount = 0;
                        updateBadge();
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "markAllAsRead failed", e.toException()));
        }
    }

    private void markSingleAsRead(String notificationId) {
        if (notificationId == null || currentUid.isEmpty()) return;
        rootRef.child(Constants.NOTIFICATIONS)
                .child(currentUid)
                .child(notificationId)
                .child("isRead")
                .setValue(true);
    }

    // ==================================================================
    // Unread Count & Badge
    // ==================================================================

    private void updateUnreadCount() {
        unreadCount = 0;
        for (NotificationItem item : notificationList) {
            if (!item.isRead()) unreadCount++;
        }
        updateBadge();
    }

    private void updateBadge() {
        if (badgeUpdateCallback != null) {
            badgeUpdateCallback.run();
        }
    }

    /**
     * Get the current unread notification count.
     */
    public int getUnreadCount() {
        return unreadCount;
    }

    // ==================================================================
    // Delete Notification
    // ==================================================================

    private void deleteNotification(NotificationItem item, int position) {
        if (currentUid.isEmpty() || item.getNotificationId() == null) return;
        rootRef.child(Constants.NOTIFICATIONS)
                .child(currentUid)
                .child(item.getNotificationId())
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    notificationList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateUnreadCount();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    adapter.notifyItemChanged(position);
                    Log.e(TAG, "deleteNotification failed", e.toException());
                });
    }

    // ==================================================================
    // Empty State
    // ==================================================================

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            tvEmptyText.setText(R.string.no_notifications);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Navigation
    // ==================================================================

    private void navigateToNotification(NotificationItem item) {
        // Mark as read
        if (!item.isRead()) {
            markSingleAsRead(item.getNotificationId());
        }

        String type = item.getType();
        Intent intent = null;
        Bundle bundle = new Bundle();

        if (Constants.NOTIFICATION_LIKE.equals(type)
                || Constants.NOTIFICATION_COMMENT.equals(type)
                || Constants.NOTIFICATION_REPLY.equals(type)
                || Constants.NOTIFICATION_MENTION.equals(type)
                || Constants.NOTIFICATION_REACTION.equals(type)) {
            if (item.getPostId() != null) {
                bundle.putString(Constants.EXTRA_POST_ID, item.getPostId());
                intent = new Intent(requireContext(), PostDetailsActivity.class);
            }
        } else if (Constants.NOTIFICATION_FOLLOW.equals(type)) {
            if (item.getFromUid() != null) {
                bundle.putString(Constants.EXTRA_USER_ID, item.getFromUid());
                intent = new Intent(requireContext(), ProfileActivity.class);
            }
        } else if (Constants.NOTIFICATION_MESSAGE.equals(type)
                || Constants.NOTIFICATION_GROUP_INVITE.equals(type)) {
            if (item.getMessageId() != null) {
                bundle.putString("chatId", item.getMessageId());
                intent = new Intent(requireContext(), ChatActivity.class);
            }
        }

        if (intent != null) {
            intent.putExtras(bundle);
            startActivity(intent);
        }
    }

    // ==================================================================
    // Notification Adapter
    // ==================================================================

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationItem item = notificationList.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return notificationList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto, ivTypeIcon;
            TextView tvTitle, tvBody, tvTime;
            View unreadDot;

            ViewHolder(View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivPhoto);
                ivTypeIcon = itemView.findViewById(R.id.ivTypeIcon);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvBody = itemView.findViewById(R.id.tvBody);
                tvTime = itemView.findViewById(R.id.tvTime);
                unreadDot = itemView.findViewById(R.id.unreadIndicator);
            }

            void bind(NotificationItem item) {
                tvTitle.setText(item.getTitle() != null ? item.getTitle() : item.getFromName());
                tvBody.setText(item.getBody());
                tvTime.setText(com.news.kimo.utils.DateUtils.formatRelativeTimeArabic(item.getTimestamp()));

                // Unread visual state
                unreadDot.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);
                itemView.setAlpha(item.isRead() ? 0.65f : 1.0f);
                itemView.setBackgroundColor(item.isRead() ? 0 : requireContext().getColor(R.color.colorNotificationUnreadBg));

                // Photo
                com.bumptech.glide.Glide.with(requireContext())
                        .load(item.getFromPhoto())
                        .circleCrop()
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .error(R.drawable.ic_placeholder_avatar)
                        .into(ivPhoto);

                // Type icon
                setTypeIcon(item.getType(), ivTypeIcon);

                itemView.setOnClickListener(v -> navigateToNotification(item));
            }

            private void setTypeIcon(String type, ImageView iv) {
                if (Constants.NOTIFICATION_LIKE.equals(type) || Constants.NOTIFICATION_REACTION.equals(type)) {
                    iv.setImageResource(R.drawable.ic_like);
                } else if (Constants.NOTIFICATION_COMMENT.equals(type)
                        || Constants.NOTIFICATION_REPLY.equals(type)) {
                    iv.setImageResource(R.drawable.ic_comment);
                } else if (Constants.NOTIFICATION_FOLLOW.equals(type)) {
                    iv.setImageResource(R.drawable.ic_follow);
                } else if (Constants.NOTIFICATION_MESSAGE.equals(type)) {
                    iv.setImageResource(R.drawable.ic_message);
                } else {
                    iv.setImageResource(R.drawable.ic_notification);
                }
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    public void onDestroyView() {
        if (notificationChildListener != null && activeQuery != null) {
            activeQuery.removeEventListener(notificationChildListener);
        }
        badgeUpdateCallback = null;
        super.onDestroyView();
    }
}
