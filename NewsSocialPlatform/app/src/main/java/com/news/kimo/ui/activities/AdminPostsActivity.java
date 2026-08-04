package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityAdminPostsBinding;
import com.news.kimo.models.AdminLog;
import com.news.kimo.models.Post;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Admin activity for managing posts. Supports search, filter dropdown
 * (All, Reported, Pinned, Archived), and per-post actions: delete,
 * pin/unpin, archive/unarchive, and view details. Includes bulk actions.
 */
public class AdminPostsActivity extends BaseActivity {

    private static final String TAG = "AdminPostsActivity";

    private ActivityAdminPostsBinding binding;
    private DatabaseReference rootRef;
    private String adminUid;
    private String adminName;

    private final List<Post> allPosts = new ArrayList<>();
    private final List<Post> filteredPosts = new ArrayList<>();
    private final Set<String> reportedPostIds = new HashSet<>();
    private AdminPostAdapter postAdapter;
    private ChildEventListener childEventListener;
    private Query activeQuery;

    private String searchQuery = "";
    private int filterIndex = 0;
    private final Set<String> selectedPostIds = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        adminUid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser() != null ?
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        initViews();
        setupFilterSpinner();
        setupRecyclerView();
        setupSearch();
        setupBulkActions();
        loadPosts();
        loadReportedPostIds();
        loadAdminName();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        updateEmptyState();
    }

    private void setupFilterSpinner() {
        String[] filters = {getString(R.string.filter_all),
                getString(R.string.filter_reported),
                getString(R.string.filter_pinned),
                getString(R.string.filter_archived)};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filters);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFilter.setAdapter(adapter);

        binding.spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterIndex = position;
                filterPosts();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvPosts.setLayoutManager(layoutManager);
        postAdapter = new AdminPostAdapter();
        binding.rvPosts.setAdapter(postAdapter);
    }

    private void setupSearch() {
        if (binding.etSearch != null) {
            binding.etSearch.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                    filterPosts();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private void setupBulkActions() {
        binding.chipSelectAll.setOnClickListener(v -> {
            if (selectedPostIds.size() == filteredPosts.size()) {
                selectedPostIds.clear();
            } else {
                selectedPostIds.clear();
                for (Post p : filteredPosts) {
                    if (p.getPostId() != null) {
                        selectedPostIds.add(p.getPostId());
                    }
                }
            }
            postAdapter.notifyDataSetChanged();
            updateBulkBar();
        });

        binding.btnBulkDelete.setOnClickListener(v -> bulkDelete());
        binding.btnBulkArchive.setOnClickListener(v -> bulkArchive());
        binding.btnBulkPin.setOnClickListener(v -> bulkPin());
    }

    private void updateBulkBar() {
        if (selectedPostIds.isEmpty()) {
            binding.layoutBulkBar.setVisibility(View.GONE);
        } else {
            binding.layoutBulkBar.setVisibility(View.VISIBLE);
            binding.tvSelectedCount.setText(selectedPostIds.size() + " " + getString(R.string.selected));
        }
    }

    // ==================================================================
    // Load Posts
    // ==================================================================

    private void loadPosts() {
        if (childEventListener != null && activeQuery != null) {
            activeQuery.removeEventListener(childEventListener);
        }

        allPosts.clear();
        filteredPosts.clear();
        postAdapter.notifyDataSetChanged();

        activeQuery = rootRef.child(Constants.POSTS)
                .orderByChild("timestamp")
                .limitToLast(100);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Post post = snapshot.getValue(Post.class);
                if (post != null) {
                    post.setPostId(snapshot.getKey());
                    allPosts.add(0, post);
                    filterPosts();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Post post = snapshot.getValue(Post.class);
                if (post != null) {
                    post.setPostId(snapshot.getKey());
                    for (int i = 0; i < allPosts.size(); i++) {
                        if (snapshot.getKey().equals(allPosts.get(i).getPostId())) {
                            allPosts.set(i, post);
                            break;
                        }
                    }
                    filterPosts();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < allPosts.size(); i++) {
                    if (key.equals(allPosts.get(i).getPostId())) {
                        allPosts.remove(i);
                        break;
                    }
                }
                filterPosts();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadPosts cancelled", error.toException());
            }
        };

        activeQuery.addChildEventListener(childEventListener);
    }

    private void loadReportedPostIds() {
        rootRef.child(Constants.REPORTS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        reportedPostIds.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String type = snap.child("type").getValue(String.class);
                            String reportedId = snap.child("reportedId").getValue(String.class);
                            String status = snap.child("status").getValue(String.class);
                            if ("post".equals(type) && reportedId != null
                                    && !"resolved".equals(status)
                                    && !"dismissed".equals(status)) {
                                reportedPostIds.add(reportedId);
                            }
                        }
                        filterPosts();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadReportedPostIds cancelled", error.toException());
                    }
                });
    }

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
    // Filter
    // ==================================================================

    private void filterPosts() {
        filteredPosts.clear();

        for (Post post : allPosts) {
            // Filter by type
            switch (filterIndex) {
                case 1: // Reported
                    if (!reportedPostIds.contains(post.getPostId())) continue;
                    break;
                case 2: // Pinned
                    if (!post.isPinned()) continue;
                    break;
                case 3: // Archived
                    if (!post.isArchived()) continue;
                    break;
                default: // All
                    break;
            }

            // Search filter
            if (!searchQuery.isEmpty()) {
                String text = post.getText() != null ?
                        post.getText().toLowerCase(Locale.getDefault()) : "";
                String name = post.getUserName() != null ?
                        post.getUserName().toLowerCase(Locale.getDefault()) : "";
                if (!text.contains(searchQuery) && !name.contains(searchQuery)) {
                    continue;
                }
            }

            filteredPosts.add(post);
        }

        postAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredPosts.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvPosts.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvPosts.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Post Actions
    // ==================================================================

    private void deletePost(Post post) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_post)
                .setMessage(R.string.delete_post_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    rootRef.child(Constants.POSTS).child(post.getPostId()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                showMessage(getString(R.string.post_deleted));
                                writeAdminLog("delete_post", "post", post.getPostId(),
                                        "Deleted post by " + post.getUserName());
                                // Also remove related data
                                rootRef.child(Constants.COMMENTS).child(post.getPostId()).removeValue();
                                rootRef.child(Constants.LIKES).child(post.getPostId()).removeValue();
                                rootRef.child(Constants.REACTIONS).child(post.getPostId()).removeValue();
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void togglePin(Post post) {
        boolean newValue = !post.isPinned();
        rootRef.child(Constants.POSTS).child(post.getPostId())
                .child("isPinned").setValue(newValue)
                .addOnSuccessListener(aVoid -> {
                    showMessage(newValue ?
                            getString(R.string.post_pinned) :
                            getString(R.string.post_unpinned));
                    writeAdminLog(newValue ? "pin_post" : "unpin_post",
                            "post", post.getPostId(),
                            (newValue ? "Pinned" : "Unpinned") +
                                    " post by " + post.getUserName());
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void toggleArchive(Post post) {
        boolean newValue = !post.isArchived();
        rootRef.child(Constants.POSTS).child(post.getPostId())
                .child("isArchived").setValue(newValue)
                .addOnSuccessListener(aVoid -> {
                    showMessage(newValue ?
                            getString(R.string.post_archived) :
                            getString(R.string.post_unarchived));
                    writeAdminLog(newValue ? "archive_post" : "unarchive_post",
                            "post", post.getPostId(),
                            (newValue ? "Archived" : "Unarchived") +
                                    " post by " + post.getUserName());
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    // ==================================================================
    // Bulk Actions
    // ==================================================================

    private void bulkDelete() {
        if (selectedPostIds.isEmpty()) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_posts)
                .setMessage(getString(R.string.delete_posts_confirm) + " (" + selectedPostIds.size() + ")")
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    showLoading();
                    final int[] remaining = {selectedPostIds.size()};
                    for (String postId : selectedPostIds) {
                        rootRef.child(Constants.POSTS).child(postId).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    remaining[0]--;
                                    if (remaining[0] <= 0) {
                                        hideLoading();
                                        selectedPostIds.clear();
                                        postAdapter.notifyDataSetChanged();
                                        updateBulkBar();
                                        showMessage(getString(R.string.posts_deleted));
                                        writeAdminLog("bulk_delete_posts", "post", null,
                                                "Bulk deleted " + selectedPostIds.size() + " posts");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    remaining[0]--;
                                    if (remaining[0] <= 0) {
                                        hideLoading();
                                        selectedPostIds.clear();
                                        postAdapter.notifyDataSetChanged();
                                        updateBulkBar();
                                    }
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void bulkArchive() {
        if (selectedPostIds.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        for (String postId : selectedPostIds) {
            updates.put(postId + "/isArchived", true);
        }
        rootRef.child(Constants.POSTS).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    showMessage(getString(R.string.posts_archived));
                    selectedPostIds.clear();
                    postAdapter.notifyDataSetChanged();
                    updateBulkBar();
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void bulkPin() {
        if (selectedPostIds.isEmpty()) return;
        Map<String, Object> updates = new HashMap<>();
        for (String postId : selectedPostIds) {
            updates.put(postId + "/isPinned", true);
        }
        rootRef.child(Constants.POSTS).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    showMessage(getString(R.string.posts_pinned));
                    selectedPostIds.clear();
                    postAdapter.notifyDataSetChanged();
                    updateBulkBar();
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    // ==================================================================
    // Admin Log
    // ==================================================================

    private void writeAdminLog(String action, String targetType, String targetId, String details) {
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
    // Admin Post Adapter
    // ==================================================================

    private class AdminPostAdapter extends RecyclerView.Adapter<AdminPostAdapter.PostViewHolder> {

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Post post = filteredPosts.get(position);
            holder.bind(post);
        }

        @Override
        public int getItemCount() {
            return filteredPosts.size();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView ivUserPhoto, ivThumb;
            TextView tvUserName, tvText, tvTime, tvLikes, tvComments, tvShares;
            View layoutStats;
            View cbSelect;
            View chipReported, chipPinned, chipArchived;

            PostViewHolder(View itemView) {
                super(itemView);
                ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
                ivThumb = itemView.findViewById(R.id.ivThumb);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvText = itemView.findViewById(R.id.tvText);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvLikes = itemView.findViewById(R.id.tvLikes);
                tvComments = itemView.findViewById(R.id.tvComments);
                tvShares = itemView.findViewById(R.id.tvShares);
                layoutStats = itemView.findViewById(R.id.layoutStats);
                cbSelect = itemView.findViewById(R.id.cbSelect);
                chipReported = itemView.findViewById(R.id.chipReported);
                chipPinned = itemView.findViewById(R.id.chipPinned);
                chipArchived = itemView.findViewById(R.id.chipArchived);
            }

            void bind(Post post) {
                loadCircularImage(post.getUserPhoto(), ivUserPhoto);
                tvUserName.setText(post.getUserName() != null ? post.getUserName() : "");
                tvTime.setText(getRelativeTime(post.getTimestamp()));

                // Content preview
                if (post.getText() != null && !post.getText().isEmpty()) {
                    tvText.setVisibility(View.VISIBLE);
                    String preview = post.getText();
                    if (preview.length() > 100) {
                        preview = preview.substring(0, 100) + "...";
                    }
                    tvText.setText(preview);
                } else {
                    tvText.setVisibility(View.GONE);
                }

                // Image thumbnail
                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    ivThumb.setVisibility(View.VISIBLE);
                    loadImage(post.getImageUrl(), ivThumb);
                } else if (post.getImages() != null && !post.getImages().isEmpty()) {
                    ivThumb.setVisibility(View.VISIBLE);
                    loadImage(post.getImages().get(0), ivThumb);
                } else {
                    ivThumb.setVisibility(View.GONE);
                }

                // Stats
                if (layoutStats != null) {
                    tvLikes.setText(String.valueOf(post.getLikesCount()));
                    tvComments.setText(String.valueOf(post.getCommentsCount()));
                    tvShares.setText(String.valueOf(post.getSharesCount()));
                }

                // Chips
                if (chipReported != null) {
                    chipReported.setVisibility(
                            reportedPostIds.contains(post.getPostId()) ?
                                    View.VISIBLE : View.GONE);
                }
                if (chipPinned != null) {
                    chipPinned.setVisibility(post.isPinned() ? View.VISIBLE : View.GONE);
                }
                if (chipArchived != null) {
                    chipArchived.setVisibility(post.isArchived() ? View.VISIBLE : View.GONE);
                }

                // Checkbox
                if (cbSelect != null) {
                    cbSelect.setSelected(selectedPostIds.contains(post.getPostId()));
                    cbSelect.setOnClickListener(v -> {
                        if (selectedPostIds.contains(post.getPostId())) {
                            selectedPostIds.remove(post.getPostId());
                        } else {
                            selectedPostIds.add(post.getPostId());
                        }
                        postAdapter.notifyItemChanged(getAdapterPosition());
                        updateBulkBar();
                    });
                }

                // Click to view details
                itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_POST_ID, post.getPostId());
                    openActivity(PostDetailsActivity.class, bundle);
                });

                // Long press for options
                itemView.setOnLongClickListener(v -> {
                    showPostOptions(post);
                    return true;
                });
            }

            private void showPostOptions(Post post) {
                PopupMenu popup = new PopupMenu(itemView.getContext(), itemView);
                popup.getMenu().add(0, 1, 0, R.string.view_details);
                popup.getMenu().add(0, 2, 1, post.isPinned() ?
                        R.string.unpin_post : R.string.pin_post);
                popup.getMenu().add(0, 3, 2, post.isArchived() ?
                        R.string.unarchive_post : R.string.archive_post);
                popup.getMenu().add(0, 4, 3, R.string.delete_post);

                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1:
                            Bundle b = new Bundle();
                            b.putString(Constants.EXTRA_POST_ID, post.getPostId());
                            openActivity(PostDetailsActivity.class, b);
                            break;
                        case 2: togglePin(post); break;
                        case 3: toggleArchive(post); break;
                        case 4: deletePost(post); break;
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
        super.onDestroy();
    }
}