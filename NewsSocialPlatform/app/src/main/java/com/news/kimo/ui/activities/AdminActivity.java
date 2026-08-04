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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityAdminBinding;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin dashboard activity. Verifies the current user has admin role,
 * then displays a grid of admin management cards.
 * Each card navigates to the corresponding admin sub-activity.
 * A red badge shows the pending report count on the reports card.
 */
public class AdminActivity extends BaseActivity {

    private static final String TAG = "AdminActivity";
    private static final int GRID_SPAN = 2;

    private ActivityAdminBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private String currentUid;

    private AdminCardAdapter cardAdapter;
    private final List<AdminCard> cardList = new ArrayList<>();
    private long pendingReportCount = 0;

    private ValueEventListener reportCountListener;

    // ==================================================================
    // Admin Card Model
    // ==================================================================

    private static class AdminCard {
        int iconRes;
        String title;
        Class<?> targetActivity;
        boolean hasBadge;
        int badgeCount;

        AdminCard(int iconRes, String title, Class<?> targetActivity) {
            this.iconRes = iconRes;
            this.title = title;
            this.targetActivity = targetActivity;
            this.hasBadge = false;
            this.badgeCount = 0;
        }

        AdminCard(int iconRes, String title, Class<?> targetActivity,
                   boolean hasBadge, int badgeCount) {
            this.iconRes = iconRes;
            this.title = title;
            this.targetActivity = targetActivity;
            this.hasBadge = hasBadge;
            this.badgeCount = badgeCount;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        initViews();
        checkAdminRole();
        loadReportCount();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        setupRecyclerView();
        buildCards();
    }

    private void setupRecyclerView() {
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, GRID_SPAN);
        binding.rvAdminCards.setLayoutManager(gridLayoutManager);
        cardAdapter = new AdminCardAdapter();
        binding.rvAdminCards.setAdapter(cardAdapter);
    }

    // ==================================================================
    // Admin Role Check
    // ==================================================================

    private void checkAdminRole() {
        if (currentUid.isEmpty()) {
            finishWithError();
            return;
        }

        rootRef.child(Constants.USERS).child(currentUid).child("role")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String role = snapshot.getValue(String.class);
                        if (!Constants.ROLE_ADMIN.equals(role)
                                && !Constants.ROLE_SUPER_ADMIN.equals(role)) {
                            finishWithError();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "checkAdminRole cancelled", error.toException());
                        finishWithError();
                    }
                });
    }

    private void finishWithError() {
        showError(getString(R.string.access_denied));
        finish();
    }

    // ==================================================================
    // Build Cards
    // ==================================================================

    private void buildCards() {
        cardList.clear();
        cardList.add(new AdminCard(R.drawable.ic_users, getString(R.string.admin_users),
                AdminUsersActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_posts, getString(R.string.admin_posts),
                AdminPostsActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_comments, getString(R.string.admin_comments),
                AdminCommentsActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_reports, getString(R.string.admin_reports),
                AdminReportsActivity.class, true, (int) pendingReportCount));
        cardList.add(new AdminCard(R.drawable.ic_media, getString(R.string.admin_media),
                AdminMediaActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_ads, getString(R.string.admin_ads),
                AdminAdsActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_notifications, getString(R.string.admin_notifications),
                AdminNotificationsActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_permissions, getString(R.string.admin_permissions),
                AdminPermissionsActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_verify, getString(R.string.admin_verification),
                AdminVerificationActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_stats, getString(R.string.admin_statistics),
                AdminStatisticsActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_logs, getString(R.string.admin_logs),
                AdminLogsActivity.class));
        cardList.add(new AdminCard(R.drawable.ic_settings_admin, getString(R.string.admin_settings),
                AdminSettingsActivity.class));
        cardAdapter.notifyDataSetChanged();
    }

    // ==================================================================
    // Report Count Badge
    // ==================================================================

    private void loadReportCount() {
        reportCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String status = snap.child("status").getValue(String.class);
                    if ("pending".equals(status)) {
                        count++;
                    }
                }
                pendingReportCount = count;
                updateReportBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadReportCount cancelled", error.toException());
            }
        };
        rootRef.child(Constants.REPORTS).addValueEventListener(reportCountListener);
    }

    private void updateReportBadge() {
        // Update the reports card (index 3)
        for (int i = 0; i < cardList.size(); i++) {
            if (cardList.get(i).targetActivity == AdminReportsActivity.class) {
                cardList.get(i).badgeCount = (int) pendingReportCount;
                cardList.get(i).hasBadge = pendingReportCount > 0;
                cardAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    // ==================================================================
    // Admin Card Adapter
    // ==================================================================

    private class AdminCardAdapter extends RecyclerView.Adapter<AdminCardAdapter.CardViewHolder> {

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_card, parent, false);
            return new CardViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
            AdminCard card = cardList.get(position);
            holder.bind(card);
        }

        @Override
        public int getItemCount() {
            return cardList.size();
        }

        class CardViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvTitle;
            View cardRoot;
            TextView tvBadge;
            View badgeDot;

            CardViewHolder(View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.ivIcon);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                cardRoot = itemView.findViewById(R.id.cardRoot);
                tvBadge = itemView.findViewById(R.id.tvBadge);
                badgeDot = itemView.findViewById(R.id.badgeDot);
            }

            void bind(AdminCard card) {
                if (ivIcon != null) {
                    ivIcon.setImageResource(card.iconRes);
                }
                if (tvTitle != null) {
                    tvTitle.setText(card.title);
                }

                // Badge
                if (card.hasBadge && card.badgeCount > 0) {
                    if (tvBadge != null) {
                        tvBadge.setVisibility(View.VISIBLE);
                        tvBadge.setText(String.valueOf(card.badgeCount));
                    } else if (badgeDot != null) {
                        badgeDot.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (tvBadge != null) {
                        tvBadge.setVisibility(View.GONE);
                    }
                    if (badgeDot != null) {
                        badgeDot.setVisibility(View.GONE);
                    }
                }

                cardRoot.setOnClickListener(v -> {
                    if (card.targetActivity != null) {
                        openActivity(card.targetActivity);
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
        if (reportCountListener != null) {
            rootRef.child(Constants.REPORTS).removeEventListener(reportCountListener);
        }
        super.onDestroy();
    }
}
