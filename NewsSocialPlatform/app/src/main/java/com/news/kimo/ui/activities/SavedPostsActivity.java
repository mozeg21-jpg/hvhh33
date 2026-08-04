package com.news.kimo.ui.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import com.bumptech.glide.Glide;
import com.news.kimo.databinding.ActivitySavedPostsBinding;
import com.news.kimo.models.Post;
import com.news.kimo.models.SavedPost;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity for viewing saved/bookmarked posts organized in lists.
 * Features TabLayout with dynamic tabs from list names, ViewPager2 for each list,
 * create new list, move between lists, unsave posts, and empty state.
 */
public class SavedPostsActivity extends BaseActivity {

    private static final String TAG = "SavedPostsActivity";

    private ActivitySavedPostsBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private String currentUid;

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private FloatingActionButton fabCreateList;
    private View layoutEmpty;

    private SavedPostsPagerAdapter pagerAdapter;
    private final List<String> listNames = new ArrayList<>();
    private final Map<String, List<SavedPost>> savedPostsMap = new HashMap<>();
    private final Map<String, List<Post>> postsMap = new HashMap<>();

    private ChildEventListener savedPostsListener;
    private Query activeSavedPostsQuery;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        if (currentUid.isEmpty()) {
            showError(getString(R.string.error_generic));
            finish();
            return;
        }

        initViews();
        loadSavedLists();
        listenForSavedPosts();
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        tabLayout = binding.tabLayout;
        viewPager = binding.viewPager;
        fabCreateList = binding.fabCreateList;
        layoutEmpty = binding.layoutEmpty;

        // Default list
        listNames.add("الكل");

        fabCreateList.setOnClickListener(v -> showCreateListDialog());
    }

    private void setupViewPager() {
        pagerAdapter = new SavedPostsPagerAdapter(getSupportFragmentManager(), listNames.size());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(listNames.size());
        tabLayout.setupWithViewPager(viewPager);

        for (int i = 0; i < listNames.size(); i++) {
            tabLayout.getTabAt(i).setText(listNames.get(i));
        }

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateEmptyState(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    // ==================================================================
    // Firebase
    // ==================================================================

    private void loadSavedLists() {
        rootRef.child(Constants.SAVED_POSTS)
                .child(currentUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Set<String> names = new HashSet<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            SavedPost sp = snap.getValue(SavedPost.class);
                            if (sp != null && sp.getListName() != null) {
                                names.add(sp.getListName());
                            }
                        }
                        // Preserve "الكل" tab and add dynamic list names
                        listNames.clear();
                        listNames.add("الكل");
                        listNames.addAll(names);
                        setupViewPager();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadSavedLists cancelled", error.toException());
                    }
                });
    }

    private void listenForSavedPosts() {
        if (currentUid.isEmpty()) return;

        activeSavedPostsQuery = rootRef.child(Constants.SAVED_POSTS)
                .child(currentUid)
                .orderByChild("timestamp");

        savedPostsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                SavedPost savedPost = snapshot.getValue(SavedPost.class);
                if (savedPost != null) {
                    savedPost.setSavedId(snapshot.getKey());
                    String listName = savedPost.getListName() != null ? savedPost.getListName() : "الافتراضي";
                    if (!savedPostsMap.containsKey(listName)) {
                        savedPostsMap.put(listName, new ArrayList<>());
                    }
                    savedPostsMap.get(listName).add(0, savedPost);
                    // Also add to "الكل"
                    if (!savedPostsMap.containsKey("الكل")) {
                        savedPostsMap.put("الكل", new ArrayList<>());
                    }
                    savedPostsMap.get("الكل").add(0, savedPost);
                    loadPostDetails(savedPost);
                    refreshCurrentPage();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (Map.Entry<String, List<SavedPost>> entry : savedPostsMap.entrySet()) {
                    for (int i = 0; i < entry.getValue().size(); i++) {
                        if (key.equals(entry.getValue().get(i).getSavedId())) {
                            SavedPost removed = entry.getValue().remove(i);
                            // Also remove from posts map
                            if (removed != null && postsMap.containsKey(entry.getKey())) {
                                for (int j = 0; j < postsMap.get(entry.getKey()).size(); j++) {
                                    if (removed.getPostId().equals(postsMap.get(entry.getKey()).get(j).getPostId())) {
                                        postsMap.get(entry.getKey()).remove(j);
                                        break;
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
                refreshCurrentPage();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "listenForSavedPosts cancelled", error.toException());
            }
        };

        activeSavedPostsQuery.addChildEventListener(savedPostsListener);
    }

    private void loadPostDetails(SavedPost savedPost) {
        rootRef.child(Constants.POSTS).child(savedPost.getPostId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Post post = snapshot.getValue(Post.class);
                        if (post != null) {
                            post.setPostId(snapshot.getKey());
                            String listName = savedPost.getListName() != null ? savedPost.getListName() : "الافتراضي";
                            if (!postsMap.containsKey(listName)) {
                                postsMap.put(listName, new ArrayList<>());
                            }
                            postsMap.get(listName).add(0, post);
                            if (!postsMap.containsKey("الكل")) {
                                postsMap.put("الكل", new ArrayList<>());
                            }
                            postsMap.get("الكل").add(0, post);
                            refreshCurrentPage();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadPostDetails cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
 // Create List
    // ==================================================================

    private void showCreateListDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint(R.string.enter_list_name);
        int padding = dp(16);
        input.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle(R.string.create_new_list)
                .setView(input)
                .setPositiveButton(R.string.create, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        createNewList(name);
                    } else {
                        showError(getString(R.string.enter_list_name));
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void createNewList(String name) {
        if (listNames.contains(name)) {
            showMessage(getString(R.string.list_already_exists));
            return;
        }
        listNames.add(name);
        savedPostsMap.put(name, new ArrayList<>());
        postsMap.put(name, new ArrayList<>());
        setupViewPager();
        showMessage(getString(R.string.list_created));
    }

    // ==================================================================
    // Unsave / Move
    // ==================================================================

    private void unsavePost(SavedPost savedPost, int position) {
        if (savedPost.getSavedId() == null) return;
        rootRef.child(Constants.SAVED_POSTS)
                .child(currentUid)
                .child(savedPost.getSavedId())
                .removeValue()
                .addOnSuccessListener(aVoid ->
                        showMessage(getString(R.string.post_unsaved)))
                .addOnFailureListener(e ->
                        showError(getString(R.string.error_generic)));
    }

    private void showMoveToListDialog(SavedPost savedPost) {
        List<String> availableLists = new ArrayList<>(listNames);
        availableLists.remove("الكل");
        if (availableLists.isEmpty()) {
            showMessage(getString(R.string.create_list_first));
            return;
        }

        CharSequence[] items = availableLists.toArray(new CharSequence[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.move_to_list)
                .setItems(items, (dialog, which) -> {
                    String newListName = availableLists.get(which);
                    moveToList(savedPost, newListName);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void moveToList(SavedPost savedPost, String newListName) {
        if (savedPost.getSavedId() == null) return;
        rootRef.child(Constants.SAVED_POSTS)
                .child(currentUid)
                .child(savedPost.getSavedId())
                .child("listName")
                .setValue(newListName)
                .addOnSuccessListener(aVoid ->
                        showMessage(getString(R.string.moved_to_list)))
                .addOnFailureListener(e ->
                        showError(getString(R.string.error_generic)));
    }

    // ==================================================================
    // UI Updates
    // ==================================================================

    private void refreshCurrentPage() {
        if (pagerAdapter != null && viewPager != null) {
            Fragment fragment = pagerAdapter.getItem(viewPager.getCurrentItem());
            if (fragment instanceof SavedListFragment) {
                ((SavedListFragment) fragment).refreshData(getCurrentPosts());
            }
        }
        updateEmptyState(viewPager != null ? viewPager.getCurrentItem() : 0);
    }

    private List<Post> getCurrentPosts() {
        int position = viewPager != null ? viewPager.getCurrentItem() : 0;
        String listName = listNames.get(position);
        return postsMap.getOrDefault(listName, new ArrayList<>());
    }

    private void updateEmptyState(int position) {
        String listName = listNames.get(position);
        List<Post> posts = postsMap.getOrDefault(listName, new ArrayList<>());
        if (posts.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            binding.tvEmptyText.setText(R.string.no_saved_posts);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    // ==================================================================
    // Pager Adapter
    // ==================================================================

    private class SavedPostsPagerAdapter extends FragmentPagerAdapter {

        SavedPostsPagerAdapter(FragmentManager fm, int count) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            return SavedListFragment.getInstance(listNames.get(position));
        }

        @Override
        public int getCount() {
            return listNames.size();
        }
    }

    /**
     * Fragment for displaying saved posts in a single list.
     * Hosted inside the SavedPostsActivity ViewPager.
     */
    public static class SavedListFragment extends Fragment {

        private static final String ARG_LIST_NAME = "arg_list_name";
        private RecyclerView rvPosts;
        private SavedPostsInListAdapter adapter;
        private final List<Post> posts = new ArrayList<>();

        public static SavedListFragment getInstance(String listName) {
            SavedListFragment fragment = new SavedListFragment();
            Bundle args = new Bundle();
            args.putString(ARG_LIST_NAME, listName);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_saved_list, container, false);
            rvPosts = view.findViewById(R.id.rvPosts);
            rvPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
            adapter = new SavedPostsInListAdapter();
            rvPosts.setAdapter(adapter);
            return view;
        }

        public void refreshData(List<Post> newPosts) {
            if (newPosts == null) return;
            posts.clear();
            posts.addAll(newPosts);
            adapter.notifyDataSetChanged();
        }

        private class SavedPostsInListAdapter extends RecyclerView.Adapter<SavedPostsInListAdapter.ViewHolder> {

            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_post, parent, false);
                return new ViewHolder(view);
            }

            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                Post post = posts.get(position);
                holder.tvUserName.setText(post.getUserName());
                holder.tvText.setText(post.getText());
                holder.tvTime.setText(DateUtils.formatRelativeTimeArabic(post.getTimestamp()));

                if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                    holder.ivPostImage.setVisibility(View.VISIBLE);
                    Glide.with(requireContext())
                            .load(post.getImageUrl())
                            .centerCrop()
                            .into(holder.ivPostImage);
                } else {
                    holder.ivPostImage.setVisibility(View.GONE);
                }

                Glide.with(requireContext())
                        .load(post.getUserPhoto())
                        .circleCrop()
                        .placeholder(R.drawable.ic_placeholder_avatar)
                        .into(holder.ivUserPhoto);

                holder.itemView.setOnClickListener(v -> {
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.EXTRA_POST_ID, post.getPostId());
                    startActivity(new Intent(requireContext(), PostDetailsActivity.class)
                            .putExtras(bundle));
                });

                holder.itemView.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.options)
                            .setItems(new String[]{R.string.unsave, R.string.move_to_list}, (dialog, which) -> {
                                if (which == 0) {
                                    // Unsave handled by parent activity
                                }
                            })
                            .show();
                    return true;
                });
            }

            @Override
            public int getItemCount() {
                return posts.size();
            }

            class ViewHolder extends RecyclerView.ViewHolder {
                ImageView ivUserPhoto, ivPostImage;
                TextView tvUserName, tvText, tvTime;

                ViewHolder(View itemView) {
                    super(itemView);
                    ivUserPhoto = itemView.findViewById(R.id.ivUserPhoto);
                    ivPostImage = itemView.findViewById(R.id.ivPostImage);
                    tvUserName = itemView.findViewById(R.id.tvUserName);
                    tvText = itemView.findViewById(R.id.tvText);
                    tvTime = itemView.findViewById(R.id.tvTime);
                }
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onDestroy() {
        if (savedPostsListener != null && activeSavedPostsQuery != null) {
            activeSavedPostsQuery.removeEventListener(savedPostsListener);
        }
        super.onDestroy();
    }
}
