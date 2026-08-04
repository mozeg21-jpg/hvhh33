package com.news.kimo.ui.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.news.kimo.databinding.ActivityAdminUsersBinding;
import com.news.kimo.models.AdminLog;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Admin activity for managing users. Supports search, tab filtering
 * (All, Active, Disabled, Verified), and per-user actions: verify,
 * ban/unban, change role, view profile, and delete (with cascade).
 */
public class AdminUsersActivity extends BaseActivity {

    private static final String TAG = "AdminUsersActivity";

    private ActivityAdminUsersBinding binding;
    private DatabaseReference rootRef;
    private String adminUid;
    private String adminName;

    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();
    private AdminUserAdapter userAdapter;
    private ChildEventListener childEventListener;
    private Query activeQuery;

    private int currentTab = 0;
    private String searchQuery = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        adminUid = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser() != null ?
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        initViews();
        setupTabs();
        setupRecyclerView();
        setupSearch();
        loadUsers();
        loadAdminName();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        updateEmptyState();
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_all));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_active));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_disabled));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.tab_verified));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                filterUsers();
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
        binding.rvUsers.setLayoutManager(layoutManager);
        userAdapter = new AdminUserAdapter();
        binding.rvUsers.setAdapter(userAdapter);
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
                    filterUsers();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    // ==================================================================
    // Load Users
    // ==================================================================

    private void loadUsers() {
        if (childEventListener != null && activeQuery != null) {
            activeQuery.removeEventListener(childEventListener);
        }

        allUsers.clear();
        filteredUsers.clear();
        userAdapter.notifyDataSetChanged();

        activeQuery = rootRef.child(Constants.USERS);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setUid(snapshot.getKey());
                    allUsers.add(user);
                    filterUsers();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    user.setUid(snapshot.getKey());
                    for (int i = 0; i < allUsers.size(); i++) {
                        if (snapshot.getKey().equals(allUsers.get(i).getUid())) {
                            allUsers.set(i, user);
                            break;
                        }
                    }
                    filterUsers();
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < allUsers.size(); i++) {
                    if (key.equals(allUsers.get(i).getUid())) {
                        allUsers.remove(i);
                        break;
                    }
                }
                filterUsers();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadUsers cancelled", error.toException());
            }
        };

        activeQuery.addChildEventListener(childEventListener);
    }

    // ==================================================================
    // Filter
    // ==================================================================

    private void filterUsers() {
        filteredUsers.clear();

        for (User user : allUsers) {
            // Tab filter
            switch (currentTab) {
                case 1: // Active
                    if (user.isDisabled()) continue;
                    break;
                case 2: // Disabled
                    if (!user.isDisabled()) continue;
                    break;
                case 3: // Verified
                    if (!user.isVerified()) continue;
                    break;
                default: // All
                    break;
            }

            // Search filter
            if (!searchQuery.isEmpty()) {
                String name = user.getName() != null ?
                        user.getName().toLowerCase(Locale.getDefault()) : "";
                String email = user.getEmail() != null ?
                        user.getEmail().toLowerCase(Locale.getDefault()) : "";
                if (!name.contains(searchQuery) && !email.contains(searchQuery)) {
                    continue;
                }
            }

            filteredUsers.add(user);
        }

        userAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredUsers.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.rvUsers.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.rvUsers.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Load Admin Name
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
    // User Actions
    // ==================================================================

    private void verifyUser(User user) {
        boolean newValue = !user.isVerified();
        rootRef.child(Constants.USERS).child(user.getUid())
                .child("isVerified").setValue(newValue)
                .addOnSuccessListener(aVoid -> {
                    showMessage(newValue ?
                            getString(R.string.user_verified) :
                            getString(R.string.user_unverified));
                    writeAdminLog("verify_user",
                            "user", user.getUid(),
                            newValue ? "Verified user: " + user.getName() :
                                    "Unverified user: " + user.getName());
                })
                .addOnFailureListener(e -> showError(e.getMessage()));
    }

    private void banOrUnbanUser(User user) {
        boolean isDisabled = user.isDisabled();
        String action = isDisabled ? "unban_user" : "ban_user";
        String confirmMsg = isDisabled ?
                getString(R.string.confirm_unban_user) :
                getString(R.string.confirm_ban_user);

        new MaterialAlertDialogBuilder(this)
                .setTitle(isDisabled ? R.string.unban_user : R.string.ban_user)
                .setMessage(confirmMsg)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    rootRef.child(Constants.USERS).child(user.getUid())
                            .child("isDisabled").setValue(!isDisabled)
                            .addOnSuccessListener(aVoid -> {
                                showMessage(isDisabled ?
                                        getString(R.string.user_unbanned) :
                                        getString(R.string.user_banned));
                                writeAdminLog(action, "user", user.getUid(),
                                        (isDisabled ? "Unbanned" : "Banned") +
                                                " user: " + user.getName());
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void changeUserRole(User user) {
        String[] roles = {Constants.ROLE_USER, Constants.ROLE_MODERATOR,
                Constants.ROLE_ADMIN, Constants.ROLE_SUPER_ADMIN};
        String[] roleLabels = {getString(R.string.role_user),
                getString(R.string.role_moderator),
                getString(R.string.role_admin),
                getString(R.string.role_super_admin)};

        int currentRoleIndex = 0;
        String currentRole = user.getRole();
        if (currentRole != null) {
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].equals(currentRole)) {
                    currentRoleIndex = i;
                    break;
                }
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.change_role)
                .setSingleChoiceItems(roleLabels, currentRoleIndex, (dialog, which) -> {
                    String newRole = roles[which];
                    dialog.dismiss();
                    rootRef.child(Constants.USERS).child(user.getUid())
                            .child("role").setValue(newRole)
                            .addOnSuccessListener(aVoid -> {
                                showMessage(getString(R.string.role_changed));
                                writeAdminLog("change_role", "user", user.getUid(),
                                        "Changed role of " + user.getName() +
                                                " to " + newRole);
                            })
                            .addOnFailureListener(e -> showError(e.getMessage()));
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void viewUserProfile(User user) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.EXTRA_USER_ID, user.getUid());
        openActivity(ProfileActivity.class, bundle);
    }

    private void deleteUser(User user) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_user)
                .setMessage(getString(R.string.delete_user_confirm) + " " + user.getName())
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    cascadeDeleteUser(user.getUid());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void cascadeDeleteUser(String uid) {
        showLoading();
        // Remove all user data from all nodes
        String[] paths = {Constants.USERS, Constants.POSTS, Constants.COMMENTS,
                Constants.LIKES, Constants.FOLLOWERS, Constants.FOLLOWING,
                Constants.NOTIFICATIONS, Constants.SAVED_POSTS, Constants.SETTINGS,
                Constants.BLOCKS, Constants.MUTES, Constants.MESSAGES, Constants.CHATS,
                Constants.REPORTS, Constants.ANALYTICS};

        List<DatabaseReference> refs = new ArrayList<>();
        for (String path : paths) {
            refs.add(rootRef.child(path).child(uid));
        }

        // Count remaining tasks
        final int[] remaining = {refs.size()};

        for (DatabaseReference ref : refs) {
            ref.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        remaining[0]--;
                        if (remaining[0] <= 0) {
                            hideLoading();
                            showMessage(getString(R.string.user_deleted));
                            writeAdminLog("delete_user", "user", uid,
                                    "Deleted user and all associated data");
                        }
                    })
                    .addOnFailureListener(e -> {
                        remaining[0]--;
                        if (remaining[0] <= 0) {
                            hideLoading();
                            showMessage(getString(R.string.user_deleted));
                            writeAdminLog("delete_user", "user", uid,
                                    "Deleted user (partial): " + e.getMessage());
                        }
                    });
        }

        // Safety: if refs is empty
        if (refs.isEmpty()) {
            hideLoading();
        }
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
    // Admin User Adapter
    // ==================================================================

    private class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = filteredUsers.get(position);
            holder.bind(user);
        }

        @Override
        public int getItemCount() {
            return filteredUsers.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvName, tvEmail, tvRole, tvJoinDate, tvPostCount;
            View btnVerify, btnBan, btnRole;
            View layoutDisabled;
            View layoutVerified;

            UserViewHolder(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                tvRole = itemView.findViewById(R.id.tvRole);
                tvJoinDate = itemView.findViewById(R.id.tvJoinDate);
                tvPostCount = itemView.findViewById(R.id.tvPostCount);
                btnVerify = itemView.findViewById(R.id.btnVerify);
                btnBan = itemView.findViewById(R.id.btnBan);
                btnRole = itemView.findViewById(R.id.btnRole);
                layoutDisabled = itemView.findViewById(R.id.layoutDisabled);
                layoutVerified = itemView.findViewById(R.id.layoutVerified);
            }

            void bind(User user) {
                loadCircularImage(user.getPhotoUrl(), ivAvatar);
                tvName.setText(user.getName() != null ? user.getName() : "");
                tvEmail.setText(user.getEmail() != null ? user.getEmail() : "");

                // Role badge
                String role = user.getRole();
                if (role == null) role = Constants.ROLE_USER;
                tvRole.setText(getRoleLabel(role));

                // Join date
                if (user.getCreatedAt() > 0) {
                    tvJoinDate.setText(getRelativeTime(user.getCreatedAt()));
                } else {
                    tvJoinDate.setText("");
                }

                // Post count
                tvPostCount.setText(String.valueOf(user.getPostCount()));

                // Disabled badge
                if (layoutDisabled != null) {
                    layoutDisabled.setVisibility(user.isDisabled() ? View.VISIBLE : View.GONE);
                }

                // Verified badge
                if (layoutVerified != null) {
                    layoutVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
                }

                // Verify button
                if (btnVerify != null) {
                    btnVerify.setOnClickListener(v -> verifyUser(user));
                }

                // Ban/unban button
                if (btnBan != null) {
                    btnBan.setOnClickListener(v -> banOrUnbanUser(user));
                }

                // Role button
                if (btnRole != null) {
                    btnRole.setOnClickListener(v -> changeUserRole(user));
                }

                // Click to view profile
                itemView.setOnClickListener(v -> viewUserProfile(user));

                // Long press for more options
                itemView.setOnLongClickListener(v -> {
                    showMoreOptions(user);
                    return true;
                });
            }

            private String getRoleLabel(String role) {
                switch (role) {
                    case Constants.ROLE_ADMIN: return getString(R.string.role_admin);
                    case Constants.ROLE_MODERATOR: return getString(R.string.role_moderator);
                    case Constants.ROLE_SUPER_ADMIN: return getString(R.string.role_super_admin);
                    default: return getString(R.string.role_user);
                }
            }

            private void showMoreOptions(User user) {
                PopupMenu popup = new PopupMenu(itemView.getContext(), itemView);
                popup.getMenu().add(0, 1, 0, R.string.view_profile);
                popup.getMenu().add(0, 2, 1, user.isVerified() ?
                        R.string.remove_verification : R.string.verify);
                popup.getMenu().add(0, 3, 2, user.isDisabled() ?
                        R.string.unban_user : R.string.ban_user);
                popup.getMenu().add(0, 4, 3, R.string.change_role);
                popup.getMenu().add(0, 5, 4, R.string.delete_user);

                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case 1: viewUserProfile(user); break;
                        case 2: verifyUser(user); break;
                        case 3: banOrUnbanUser(user); break;
                        case 4: changeUserRole(user); break;
                        case 5: deleteUser(user); break;
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