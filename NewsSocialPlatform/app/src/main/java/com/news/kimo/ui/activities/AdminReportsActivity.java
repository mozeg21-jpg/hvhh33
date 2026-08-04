package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityAdminReportsBinding;
import com.news.kimo.models.AdminLog;
import com.news.kimo.models.Report;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Admin activity for managing reports. Shows badge count of pending reports
 * in the toolbar. Supports TabLayout filtering: Pending, Reviewed, Resolved, Dismissed.
 * Each report shows type badge, reporter info, reason, description, date, status.
 * Actions: review (navigate to content), resolve, dismiss, delete.
 * Every action is written to admin_logs.
 */
public class AdminReportsActivity extends BaseActivity {

    private static final String TAG = "AdminReportsActivity";

    private ActivityAdminReportsBinding binding;
    private DatabaseReference rootRef;
    private String adminUid;
    private String adminName;

    private final List<Report> allReports = new ArrayList<>();
    private final List<Report> filteredReports = new ArrayList<>();
    private ReportAdapter reportAdapter;
    private ChildEventListener childEventListener;
    private Query activeQuery;
    private ValueEventListener pendingCountListener;

    private int currentTab = 0;
    private long pendingCount = 0;

    // Status constants
    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_REVIEWED = "reviewed";
    private static final String STATUS_RESOLVED = "resolved";
    private static final String STATUS_DISMISSED = "dismissed";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReportsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        adminUid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser() != null ?
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        initViews();
        setupTabs();
        setupRecyclerView();
        loadReports();
        loadPendingCount();
        loadAdminName();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        binding.tvBadgeCount.setVisibility(View.GONE);
        updateEmptyState();
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_pending));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_reviewed));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_resolved));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_dismissed));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                filterReports();
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvReports.setLayoutManager(layoutManager);
        reportAdapter = new ReportAdapter();
        binding.rvReports.setAdapter(reportAdapter);
    }

    // ==================================================================
    // Load Reports
    // ==================================================================

    private void loadReports() {
        if (childEventListener != null && activeQuery != null) {
            activeQuery.removeEventListener(childEventListener);
        }

        allReports.clear();
        filteredReports.clear();
        reportAdapter.notifyDataSetChanged();

        activeQuery = rootRef.child(Constants.REPORTS)
                .orderByChild("timestamp");

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Report report = snapshot.getValue(Report.class);
                if (report != null) {
                    report.setReportId(snapshot.getKey());
                    allReports.add(0, report);
                    Collections.sort(allReports, (a, b) ->
                            Long.compare(b.getTimestamp(), a.getTimestamp()));
                    filterReports();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Report report = snapshot.getValue(Report.class);
                if (report != null) {
                    report.setReportId(snapshot.getKey());
                    for (int i = 0; i < allReports.size(); i++) {
                        if (snapshot.getKey().equals(allReports.get(i).getReportId())) {
                            allReports.set(i, report);
                            break;
                        }
                    }
                    filterReports();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < allReports.size(); i++) {
                    if (key.equals(allReports.get(i).getReportId())) {
                        allReports.remove(i);
                        break;
                    }
                }
                filterReports();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadReports cancelled", error.toException());
            }
        };

        activeQuery.addChildEventListener(childEventListener);
    }

    // ==================================================================
    // Pending Count
    // ==================================================================

    private void loadPendingCount() {
        pendingCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    String status = snap.child("status").getValue(String.class);
                    if (STATUS_PENDING.equals(status)) {
                        count++;
                    }
                }
                pendingCount = count;
                updateBadge();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadPendingCount cancelled", error.toException());
            }
        };
        rootRef.child(Constants.REPORTS).addValueEventListener(pendingCountListener);
    }

    private void updateBadge() {
        if (pendingCount > 0) {
            binding.tvBadgeCount.setVisibility(View.VISIBLE);
            binding.tvBadgeCount.setText(String.valueOf(pendingCount));
        } else {
            binding.tvBadgeCount.setVisibility(View.GONE);
        }
    }

    // ==================================================================
    // Filter
    // ==================================================================

    private void filterReports() {
        filteredReports.clear();

        String statusFilter = getStatusFilter();

        for (Report report : allReports) {
            String reportStatus = report.getStatus();
            if (reportStatus == null) reportStatus = STATUS_PENDING;

            if (statusFilter == null || statusFilter.equals(reportStatus)) {
                filteredReports.add(report);
            }
        }

        reportAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private String getStatusFilter() {
        switch (currentTab) {
            case 0: return STATUS_PENDING;
            case 1: return STATUS_REVIEWED;
            case 2: return STATUS_RESOLVED;
            case 3: return STATUS_DISMISSED;
            default: return null;
        }
    }

    private void updateEmptyState() {
        if (filteredReports.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvReports.setVisibility(View.GONE);
            binding.tvEmptyText.setText(getEmptyMessage());
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvReports.setVisibility(View.VISIBLE);
        }
    }

    private String getEmptyMessage() {
        switch (currentTab) {
            case 0: return getString(R.string.no_pending_reports);
            case 1: return getString(R.string.no_reviewed_reports);
            case 2: return getString(R.string.no_resolved_reports);
            case 3: return getString(R.string.no_dismissed_reports);
            default: return getString(R.string.no_reports);
        }
    }

    // ==================================================================
    // Admin Name
    // ==================================================================

    private void loadAdminName() {
        rootRef.child(Constants.USERS).child(adminUid).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        adminName = snapshot.getValue(String.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadAdminName cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Report Actions
    // ==================================================================

    /**
     * Navigate to the reported content (post, user, or comment).
     */
    private void reviewReport(Report report) {
        // Mark as reviewed first
        rootRef.child(Constants.REPORTS).child(report.getReportId())
                .child("status").setValue(STATUS_REVIEWED)
                .addOnSuccessListener(aVoid -> {
                    rootRef.child(Constants.REPORTS).child(report.getReportId())
                            .child("reviewedBy").setValue(adminUid)
                            .addOnSuccessListener(v -> {
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("reviewedAt").setValue(System.currentTimeMillis());
                            });
                    writeAdminLog("review_report", "report", report.getReportId(),
                            "Reviewed report: " + report.getReason());
                });

        // Navigate to reported content
        String type = report.getType();
        String reportedId = report.getReportedId();
        if (reportedId == null) return;

        Bundle bundle = new Bundle();
        if ("post".equals(type)) {
            bundle.putString(Constants.EXTRA_POST_ID, reportedId);
            openActivity(PostDetailsActivity.class, bundle);
        } else if ("user".equals(type)) {
            bundle.putString(Constants.EXTRA_USER_ID, reportedId);
            openActivity(ProfileActivity.class, bundle);
        } else if ("comment".equals(type)) {
            // Open the comment's post
            bundle.putString(Constants.EXTRA_COMMENT_ID, reportedId);
            openActivity(PostDetailsActivity.class, bundle);
        }
    }

    /**
     * Resolve the report: mark as resolved, optionally take action on content.
     */
    private void resolveReport(Report report) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.resolve_report)
                .setMessage(R.string.resolve_report_confirm)
                .setPositiveButton(R.string.resolve, (dialog, which) -> {
                    rootRef.child(Constants.REPORTS).child(report.getReportId())
                            .child("status").setValue(STATUS_RESOLVED)
                            .addOnSuccessListener(aVoid -> {
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("reviewedBy").setValue(adminUid);
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("reviewedAt").setValue(System.currentTimeMillis());
                                showMessage(getString(R.string.report_resolved));
                                writeAdminLog("resolve_report", "report", report.getReportId(),
                                        "Resolved report on " + report.getType() +
                                                " Reason: " + report.getReason());
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Dismiss the report: mark as dismissed, no action taken.
     */
    private void dismissReport(Report report) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.dismiss_report)
                .setMessage(R.string.dismiss_report_confirm)
                .setPositiveButton(R.string.dismiss, (dialog, which) -> {
                    rootRef.child(Constants.REPORTS).child(report.getReportId())
                            .child("status").setValue(STATUS_DISMISSED)
                            .addOnSuccessListener(aVoid -> {
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("reviewedBy").setValue(adminUid);
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("reviewedAt").setValue(System.currentTimeMillis());
                                showMessage(getString(R.string.report_dismissed));
                                writeAdminLog("dismiss_report", "report", report.getReportId(),
                                        "Dismissed report on " + report.getType() +
                                                " Reason: " + report.getReason());
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Delete the report entirely from the database.
     */
    private void deleteReport(Report report) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_report)
                .setMessage(R.string.delete_report_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    rootRef.child(Constants.REPORTS).child(report.getReportId())
                            .removeValue()
                            .addOnSuccessListener(aVoid -> {
                                showMessage(getString(R.string.report_deleted));
                                writeAdminLog("delete_report", "report", report.getReportId(),
                                        "Deleted report: " + report.getReason());
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /**
     * Resolve and delete the reported content.
     */
    private void resolveAndDeleteContent(Report report) {
        String type = report.getType();
        String reportedId = report.getReportedId();
        if (type == null || reportedId == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_reported_content)
                .setMessage(R.string.delete_reported_content_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    DatabaseReference contentRef;
                    if ("post".equals(type)) {
                        contentRef = rootRef.child(Constants.POSTS).child(reportedId);
                    } else if ("comment".equals(type)) {
                        contentRef = rootRef.child(Constants.COMMENTS).child(reportedId);
                    } else {
                        showError(getString(R.string.error_generic));
                        return;
                    }

                    contentRef.removeValue()
                            .addOnSuccessListener(aVoid -> {
                                // Also resolve the report
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("status").setValue(STATUS_RESOLVED);
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("reviewedBy").setValue(adminUid);
                                rootRef.child(Constants.REPORTS).child(report.getReportId())
                                        .child("reviewedAt").setValue(System.currentTimeMillis());

                                showMessage(getString(R.string.content_deleted));
                                writeAdminLog("resolve_delete_content",
                                        type, reportedId,
                                        "Resolved and deleted reported " + type +
                                                ": " + reportedId);
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ==================================================================
    // Admin Log
    // ==================================================================

    private void writeAdminLog(String action, String targetType, String targetId,
                               String details) {
        AdminLog log = new AdminLog(
                rootRef.child(Constants.ADMIN_LOGS).push().getKey(),
                adminUid, adminName, action, targetType, targetId,
                details, System.currentTimeMillis()
        );
        if (log.getLogId() != null) {
            rootRef.child(Constants.ADMIN_LOGS).child(log.getLogId()).setValue(log);
        }
    }

    // ==================================================================
    // Report Adapter
    // ==================================================================

    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

        @NonNull
        @Override
        public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report, parent, false);
            return new ReportViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
            Report report = filteredReports.get(position);
            holder.bind(report);
        }

        @Override
        public int getItemCount() {
            return filteredReports.size();
        }

        class ReportViewHolder extends RecyclerView.ViewHolder {
            TextView tvTypeBadge, tvReporter, tvReason, tvDescription;
            TextView tvDate, tvStatusBadge;
            ImageView ivReporterPhoto;
            View btnReview, btnResolve, btnDismiss, btnDelete;

            ReportViewHolder(View itemView) {
                super(itemView);
                tvTypeBadge = itemView.findViewById(R.id.tvTypeBadge);
                tvReporter = itemView.findViewById(R.id.tvReporter);
                tvReason = itemView.findViewById(R.id.tvReason);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvDate = itemView.findViewById(R.id.tvDate);
                tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
                ivReporterPhoto = itemView.findViewById(R.id.ivReporterPhoto);
                btnReview = itemView.findViewById(R.id.btnReview);
                btnResolve = itemView.findViewById(R.id.btnResolve);
                btnDismiss = itemView.findViewById(R.id.btnDismiss);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }

            void bind(Report report) {
                // Type badge
                if (tvTypeBadge != null) {
                    String type = report.getType() != null ? report.getType() : "";
                    tvTypeBadge.setText(getTypeLabel(type));
                    int color = getTypeColor(type);
                    tvTypeBadge.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(color));
                }

                // Reporter
                tvReporter.setText(report.getReporterName() != null ?
                        report.getReporterName() : "");

                // Reason
                tvReason.setText(report.getReason() != null ? report.getReason() : "");

                // Description
                if (report.getDescription() != null && !report.getDescription().isEmpty()) {
                    tvDescription.setVisibility(View.VISIBLE);
                    tvDescription.setText(report.getDescription());
                } else {
                    tvDescription.setVisibility(View.GONE);
                }

                // Date
                tvDate.setText(getRelativeTime(report.getTimestamp()));

                // Status badge
                String status = report.getStatus();
                if (status == null) status = STATUS_PENDING;
                tvStatusBadge.setText(getStatusLabel(status));
                int statusColor = getStatusColor(status);
                tvStatusBadge.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(statusColor));

                // Load reporter photo
                if (ivReporterPhoto != null) {
                    rootRef.child(Constants.USERS).child(report.getReporterUid())
                            .child("photoUrl")
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String url = snapshot.getValue(String.class);
                                    loadCircularImage(url, ivReporterPhoto);
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });
                }

                // Action buttons
                boolean isPending = STATUS_PENDING.equals(status);

                if (btnReview != null) {
                    btnReview.setVisibility(isPending ? View.VISIBLE : View.GONE);
                    btnReview.setOnClickListener(v -> reviewReport(report));
                }

                if (btnResolve != null) {
                    btnResolve.setVisibility(!STATUS_RESOLVED.equals(status) ?
                            View.VISIBLE : View.GONE);
                    btnResolve.setOnClickListener(v -> resolveReport(report));
                }

                if (btnDismiss != null) {
                    btnDismiss.setVisibility(!STATUS_DISMISSED.equals(status) ?
                            View.VISIBLE : View.GONE);
                    btnDismiss.setOnClickListener(v -> dismissReport(report));
                }

                if (btnDelete != null) {
                    btnDelete.setOnClickListener(v -> deleteReport(report));
                }

                // Long press for more options
                itemView.setOnLongClickListener(v -> {
                    showMoreOptions(report);
                    return true;
                });
            }

            private String getTypeLabel(String type) {
                if (type == null) return "";
                switch (type) {
                    case "post": return getString(R.string.type_post);
                    case "user": return getString(R.string.type_user);
                    case "comment": return getString(R.string.type_comment);
                    default: return type;
                }
            }

            private int getTypeColor(String type) {
                if ("post".equals(type)) return ContextCompat.getColor(itemView.getContext(),
                        R.color.colorPostType);
                if ("user".equals(type)) return ContextCompat.getColor(itemView.getContext(),
                        R.color.colorUserType);
                if ("comment".equals(type)) return ContextCompat.getColor(itemView.getContext(),
                        R.color.colorCommentType);
                return ContextCompat.getColor(itemView.getContext(), R.color.colorGray);
            }

            private String getStatusLabel(String status) {
                if (status == null) return getString(R.string.status_pending);
                switch (status) {
                    case STATUS_REVIEWED: return getString(R.string.status_reviewed);
                    case STATUS_RESOLVED: return getString(R.string.status_resolved);
                    case STATUS_DISMISSED: return getString(R.string.status_dismissed);
                    default: return getString(R.string.status_pending);
                }
            }

            private int getStatusColor(String status) {
                if (STATUS_PENDING.equals(status))
                    return ContextCompat.getColor(itemView.getContext(), R.color.colorPending);
                if (STATUS_REVIEWED.equals(status))
                    return ContextCompat.getColor(itemView.getContext(), R.color.colorReviewed);
                if (STATUS_RESOLVED.equals(status))
                    return ContextCompat.getColor(itemView.getContext(), R.color.colorResolved);
                if (STATUS_DISMISSED.equals(status))
                    return ContextCompat.getColor(itemView.getContext(), R.color.colorDismissed);
                return ContextCompat.getColor(itemView.getContext(), R.color.colorGray);
            }

            private void showMoreOptions(Report report) {
                PopupMenu popup = new PopupMenu(itemView.getContext(), itemView);
                popup.getMenu().add(0, 1, 0, R.string.review);
                popup.getMenu().add(0, 2, 1, R.string.resolve);
                popup.getMenu().add(0, 3, 2, R.string.dismiss);
                popup.getMenu().add(0, 4, 3, R.string.delete_reported_content);
                popup.getMenu().add(0, 5, 4, R.string.delete);

                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1: reviewReport(report); break;
                        case 2: resolveReport(report); break;
                        case 3: dismissReport(report); break;
                        case 4: resolveAndDeleteContent(report); break;
                        case 5: deleteReport(report); break;
                    }
                    return true;
                });
                popup.show();
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        if (childEventListener != null && activeQuery != null) {
            activeQuery.removeEventListener(childEventListener);
        }
        if (pendingCountListener != null) {
            rootRef.child(Constants.REPORTS).removeEventListener(pendingCountListener);
        }
        super.onDestroy();
    }
}
