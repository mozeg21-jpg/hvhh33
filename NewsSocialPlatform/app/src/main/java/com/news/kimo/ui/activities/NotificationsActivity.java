package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
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
import com.news.kimo.databinding.ActivityNotificationsBinding;
import com.news.kimo.models.NotificationItem;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Notifications activity displaying real-time notifications from Firebase.
 * Supports TabLayout filtering: الكل, إعجابات, تعليقات, متابعات, رسائل.
 * Features swipe-to-delete, mark all as read, and navigation to relevant screens.
 */
public class NotificationsActivity extends BaseActivity {

    private static final String TAG = "NotificationsActivity";

    private ActivityNotificationsBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private String currentUid;

    private TabLayout tabLayout;
    private RecyclerView rvNotifications;
    private View layoutEmpty;
    private TextView tvEmptyText;

    private NotificationAdapter notificationAdapter;
    private final List<NotificationItem> notificationList = new ArrayList<>();
    private final List<NotificationItem> allNotifications = new ArrayList<>();

    private ChildEventListener notificationChildListener;
    private Query activeQuery;
    private int currentTab = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        initViews();
        setupTabs();
        setupRecyclerView();
        setupSwipeToDelete();
        markAllAsRead();
        listenForNotifications();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        tabLayout = binding.tabLayout;
        rvNotifications = binding.rvNotifications;
        layoutEmpty = binding.layoutEmpty;
        tvEmptyText = binding.tvEmptyText;

        binding.tvMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_all));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_likes));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_comments));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_follows));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_messages));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                filterNotifications();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void setupRecyclerView() {
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter();
        rvNotifications.setAdapter(notificationAdapter);
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
    // Firebase Listener
    // ==================================================================

    private void listenForNotifications() {
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
                    allNotifications.add(0, notification);
                    Collections.sort(allNotifications, (n1, n2) ->
                            Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                    filterNotifications();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                NotificationItem notification = snapshot.getValue(NotificationItem.class);
                if (notification != null) {
                    notification.setNotificationId(snapshot.getKey());
                    updateNotificationInList(notification);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < allNotifications.size(); i++) {
                    if (key.equals(allNotifications.get(i).getNotificationId())) {
                        allNotifications.remove(i);
                        break;
                    }
                }
                filterNotifications();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "listenForNotifications cancelled", error.toException());
            }
        };

        activeQuery.addChildEventListener(notificationChildListener);
    }

    private void updateNotificationInList(NotificationItem updated) {
        for (int i = 0; i < allNotifications.size(); i++) {
            if (updated.getNotificationId().equals(allNotifications.get(i).getNotificationId())) {
                allNotifications.set(i, updated);
                break;
            }
        }
        filterNotifications();
    }

    // ==================================================================
    // Mark as Read
    // ==================================================================

    private void markAllAsRead() {
        if (currentUid.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        for (NotificationItem notification : allNotifications) {
            if (!notification.isRead()) {
                updates.put(notification.getNotificationId() + "/isRead", true);
            }
        }
        if (!updates.isEmpty()) {
            rootRef.child(Constants.NOTIFICATIONS).child(currentUid)
                    .updateChildren(updates)
                    .addOnFailureListener(e ->
                            Log.e(TAG, "markAllAsRead failed", e.toException()));
        }
    }

    // ==================================================================
    // Filter
    // ==================================================================

    private void filterNotifications() {
        notificationList.clear();
        String filterType = getFilterType();

        if (filterType == null) {
            // All notifications
            notificationList.addAll(allNotifications);
        } else {
            for (NotificationItem item : allNotifications) {
                if (filterType.equals(item.getType())) {
                    notificationList.add(item);
                }
            }
        }

        notificationAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private String getFilterType() {
        switch (currentTab) {
            case 1: return Constants.NOTIFICATION_LIKE;
            case 2: return Constants.NOTIFICATION_COMMENT;
            case 3: return Constants.NOTIFICATION_FOLLOW;
            case 4: return Constants.NOTIFICATION_MESSAGE;
            default: return null;
        }
    }

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            tvEmptyText.setText(getEmptyMessage());
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
    }

    private String getEmptyMessage() {
        switch (currentTab) {
            case 1: return getString(R.string.no_likes_notifications);
            case 2: return getString(R.string.no_comments_notifications);
            case 3: return getString(R.string.no_follows_notifications);
            case 4: return getString(R.string.no_messages_notifications);
            default: return getString(R.string.no_notifications);
        }
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
                    allNotifications.remove(item);
                    notificationAdapter.notifyItemRemoved(position);
                    updateEmptyState();
                    showMessage(getString(R.string.notification_deleted));
                })
                .addOnFailureListener(e -> {
                    notificationAdapter.notifyItemChanged(position);
                    showError(getString(R.string.error_generic));
                });
    }

    // ==================================================================
    // Navigation
    // ==================================================================

    private void navigateToNotification(NotificationItem item) {
        // Mark as read
        if (!item.isRead()) {
            rootRef.child(Constants.NOTIFICATIONS)
                    .child(currentUid)
                    .child(item.getNotificationId())
                    .child("isRead").setValue(true);
        }

        String type = item.getType();
        Bundle bundle = new Bundle();

        if (Constants.NOTIFICATION_LIKE.equals(type) || Constants.NOTIFICATION_COMMENT.equals(type)
                || Constants.NOTIFICATION_REPLY.equals(type) || Constants.NOTIFICATION_MENTION.equals(type)) {
            if (item.getPostId() != null) {
                bundle.putString(Constants.EXTRA_POST_ID, item.getPostId());
                openActivity(PostDetailsActivity.class, bundle);
            }
        } else if (Constants.NOTIFICATION_FOLLOW.equals(type)) {
            if (item.getFromUid() != null) {
                bundle.putString(Constants.EXTRA_USER_ID, item.getFromUid());
                openActivity(ProfileActivity.class, bundle);
            }
        } else if (Constants.NOTIFICATION_MESSAGE.equals(type)
                || Constants.NOTIFICATION_GROUP_INVITE.equals(type)) {
            if (item.getMessageId() != null) {
                bundle.putString("chatId", item.getMessageId());
                openActivity(ChatActivity.class, bundle);
            }
        }
    }

    // ==================================================================
    // Notification Adapter
    // ==================================================================

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

        @NonNull
        @Override
        public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_notification, parent, false);
            return new NotificationViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
            NotificationItem item = notificationList.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return notificationList.size();
        }

        class NotificationViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto;
            TextView tvTitle, tvBody, tvTime;
            View unreadIndicator;

            NotificationViewHolder(View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivPhoto);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvBody = itemView.findViewById(R.id.tvBody);
                tvTime = itemView.findViewById(R.id.tvTime);
                unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            }

            void bind(NotificationItem item) {
                tvTitle.setText(item.getTitle() != null ? item.getTitle() : item.getFromName());
                tvBody.setText(item.getBody());
                tvTime.setText(getRelativeTime(item.getTimestamp()));

                // Unread indicator
                unreadIndicator.setVisibility(item.isRead() ? View.GONE : View.VISIBLE);

                // Background tint for unread
                itemView.setAlpha(item.isRead() ? 0.7f : 1.0f);

                // Load photo
                loadCircularImage(item.getFromPhoto(), ivPhoto);

                // Click to navigate
                itemView.setOnClickListener(v -> navigateToNotification(item));
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        if (notificationChildListener != null && activeQuery != null) {
            activeQuery.removeEventListener(notificationChildListener);
        }
        super.onDestroy();
    }
}
