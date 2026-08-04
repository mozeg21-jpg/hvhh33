package com.news.kimo.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import com.news.kimo.databinding.FragmentMessagesBinding;
import com.news.kimo.models.Chat;
import com.news.kimo.models.User;
import com.news.kimo.ui.activities.ChatActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.ui.activities.SearchActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Messages fragment for the bottom navigation bar.
 * Shows chat list with tabs: المحادثات, المجموعات.
 * Each chat item shows avatar, name, last message, time, unread count,
 * online dot, and mute icon. Supports search, mute, delete, pin.
 */
public class MessagesFragment extends Fragment {

    private static final String TAG = "MessagesFragment";

    private FragmentMessagesBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private String currentUid;
    private User currentUser;

    private TabLayout tabLayout;
    private RecyclerView rvChats;
    private FloatingActionButton fabNewChat;
    private EditText etSearchChats;
    private View layoutEmpty;
    private TextView tvEmptyText;

    private ChatListAdapter adapter;
    private final List<Chat> chatList = new ArrayList<>();
    private final List<Chat> allChats = new ArrayList<>();
    private final Map<String, User> userCache = new HashMap<>();
    private final Map<String, Boolean> onlineStatusMap = new HashMap<>();
    private final List<String> pinnedChatIds = new ArrayList<>();

    private ChildEventListener chatsChildListener;
    private Query activeChatsQuery;
    private ValueEventListener userStatusListener;
    private int currentTab = 0;

    public static MessagesFragment getInstance() {
        return new MessagesFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(requireContext());
        currentUser = sessionManager.loadCurrentUser();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMessagesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        setupTabs();
        setupRecyclerView();
        setupSearch();
        setupFab();
        loadChats();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentUid != null && !currentUid.isEmpty()) {
            rootRef.child(Constants.USERS).child(currentUid).child("isOnline").setValue(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (currentUid != null && !currentUid.isEmpty()) {
            rootRef.child(Constants.USERS).child(currentUid).child("isOnline").setValue(false);
            rootRef.child(Constants.USERS).child(currentUid).child("lastSeen")
                    .setValue(System.currentTimeMillis());
        }
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        tabLayout = binding.tabLayout;
        rvChats = binding.rvChats;
        fabNewChat = binding.fabNewChat;
        etSearchChats = binding.etSearchChats;
        layoutEmpty = binding.layoutEmpty;
        tvEmptyText = binding.tvEmptyText;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_chats));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_groups));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                filterChats();
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
        rvChats.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ChatListAdapter();
        rvChats.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearchChats.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                filterChats();
            }
        });
    }

    private void setupFab() {
        fabNewChat.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), fabNewChat);
            popup.getMenu().add(0, 1, 0, R.string.new_chat);
            popup.getMenu().add(0, 2, 1, R.string.new_group);
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1) {
                    Intent intent = new Intent(requireContext(), SearchActivity.class);
                    intent.putExtra("search_mode", "new_chat");
                    startActivity(intent);
                } else if (id == 2) {
                    // Navigate to CreateGroupActivity
                    showMessageForTest("إنشاء مجموعة قريبًا");
                }
                return true;
            });
            popup.show();
        });
    }

    private void showMessageForTest(String msg) {
        if (getContext() != null) {
            android.widget.Toast.makeText(getContext(), msg, android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    // ==================================================================
    // Firebase
    // ==================================================================

    private void loadChats() {
        if (currentUid.isEmpty()) return;

        activeChatsQuery = rootRef.child(Constants.CHATS)
                .orderByChild("lastMessageTime")
                .limitToLast(100);

        chatsChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Chat chat = snapshot.getValue(Chat.class);
                if (chat != null) {
                    chat.setChatId(snapshot.getKey());
                    // Only show chats where current user is a participant
                    if (chat.getParticipants() != null && chat.getParticipants().contains(currentUid)) {
                        allChats.add(0, chat);
                        loadOtherUserForChat(chat);
                        checkOnlineStatus(chat);
                        filterChats();
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Chat chat = snapshot.getValue(Chat.class);
                if (chat != null) {
                    chat.setChatId(snapshot.getKey());
                    for (int i = 0; i < allChats.size(); i++) {
                        if (chat.getChatId().equals(allChats.get(i).getChatId())) {
                            allChats.set(i, chat);
                            filterChats();
                            break;
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                for (int i = 0; i < allChats.size(); i++) {
                    if (key.equals(allChats.get(i).getChatId())) {
                        allChats.remove(i);
                        filterChats();
                        break;
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadChats cancelled", error.toException());
            }
        };

        activeChatsQuery.addChildEventListener(chatsChildListener);
    }

    private void loadOtherUserForChat(Chat chat) {
        if ("group".equals(chat.getType())) return;
        // Find the other participant
        String otherUid = null;
        if (chat.getParticipants() != null) {
            for (String uid : chat.getParticipants()) {
                if (!uid.equals(currentUid)) {
                    otherUid = uid;
                    break;
                }
            }
        }
        if (otherUid == null) return;

        // Load from cache or network
        if (userCache.containsKey(otherUid)) return;

        rootRef.child(Constants.USERS).child(otherUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            user.setUid(snapshot.getKey());
                            userCache.put(otherUid, user);
                            adapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadOtherUserForChat cancelled", error.toException());
                    }
                });
    }

    private void checkOnlineStatus(Chat chat) {
        if ("group".equals(chat.getType())) return;
        String otherUid = getOtherUserId(chat);
        if (otherUid == null || onlineStatusMap.containsKey(otherUid)) return;

        rootRef.child(Constants.USERS).child(otherUid).child("isOnline")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean online = snapshot.getValue(Boolean.class);
                        onlineStatusMap.put(otherUid, Boolean.TRUE.equals(online));
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "checkOnlineStatus cancelled", error.toException());
                    }
                });
    }

    private String getOtherUserId(Chat chat) {
        if (chat.getParticipants() == null) return null;
        for (String uid : chat.getParticipants()) {
            if (!uid.equals(currentUid)) return uid;
        }
        return null;
    }

    // ==================================================================
    // Filter
    // ==================================================================

    private void filterChats() {
        chatList.clear();
        String searchQuery = etSearchChats.getText().toString().trim().toLowerCase(java.util.Locale.getDefault());

        // First add pinned chats
        List<Chat> unpinned = new ArrayList<>();
        for (Chat chat : allChats) {
            if (!passesTypeFilter(chat)) continue;
            if (!passesSearchFilter(chat, searchQuery)) continue;
            if (pinnedChatIds.contains(chat.getChatId())) {
                chatList.add(chat);
            } else {
                unpinned.add(chat);
            }
        }
        // Sort unpinned by lastMessageTime descending
        Collections.sort(unpinned, (c1, c2) -> Long.compare(c2.getLastMessageTime(), c1.getLastMessageTime()));
        chatList.addAll(unpinned);

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private boolean passesTypeFilter(Chat chat) {
        if (currentTab == 0) return !"group".equals(chat.getType()); // Chats only
        if (currentTab == 1) return "group".equals(chat.getType()); // Groups only
        return true;
    }

    private boolean passesSearchFilter(Chat chat, String query) {
        if (query.isEmpty()) return true;
        String name = chat.getName() != null ? chat.getName().toLowerCase() : "";
        String lastMsg = chat.getLastMessage() != null ? chat.getLastMessage().toLowerCase() : "";
        return name.contains(query) || lastMsg.contains(query);
    }

    // ==================================================================
    // Actions
    // ==================================================================

    private void openChat(Chat chat) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("chatId", chat.getChatId());

        String otherUid = getOtherUserId(chat);
        if (otherUid != null) {
            User otherUser = userCache.get(otherUid);
            if (otherUser != null) {
                intent.putExtra("otherUserId", otherUser.getUid());
                intent.putExtra("otherUserName", otherUser.getName());
                intent.putExtra("otherUserPhoto", otherUser.getPhotoUrl());
            }
        }

        if ("group".equals(chat.getType())) {
            intent.putExtra("isGroup", true);
        }

        startActivity(intent);
    }

    private void showChatOptions(Chat chat, View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, chat.isMuted() ? R.string.unmute : R.string.mute);
        popup.getMenu().add(0, 2, 1, R.string.pin_chat);
        popup.getMenu().add(0, 3, 2, R.string.delete);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                toggleMute(chat);
            } else if (id == 2) {
                togglePin(chat);
            } else if (id == 3) {
                confirmDeleteChat(chat);
            }
            return true;
        });
        popup.show();
    }

    private void toggleMute(Chat chat) {
        if (chat.getChatId() == null) return;
        boolean newMuted = !chat.isMuted();
        rootRef.child(Constants.CHATS).child(chat.getChatId())
                .child("isMuted").setValue(newMuted)
                .addOnSuccessListener(aVoid -> {
                    String msg = newMuted ? getString(R.string.chat_muted) : getString(R.string.chat_unmuted);
                    showMessageForTest(msg);
                });
    }

    private void togglePin(Chat chat) {
        if (chat.getChatId() == null) return;
        if (pinnedChatIds.contains(chat.getChatId())) {
            pinnedChatIds.remove(chat.getChatId());
            rootRef.child(Constants.CHATS).child(chat.getChatId())
                    .child("isPinned").setValue(false);
            showMessageForTest(getString(R.string.chat_unpinned));
        } else {
            pinnedChatIds.add(chat.getChatId());
            rootRef.child(Constants.CHATS).child(chat.getChatId())
                    .child("isPinned").setValue(true);
            showMessageForTest(getString(R.string.chat_pinned));
        }
        filterChats();
    }

    private void confirmDeleteChat(Chat chat) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_chat_title)
                .setMessage(R.string.delete_chat_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> deleteChat(chat))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteChat(Chat chat) {
        if (chat.getChatId() == null) return;
        rootRef.child(Constants.CHATS).child(chat.getChatId()).removeValue()
                .addOnSuccessListener(aVoid ->
                        showMessageForTest(getString(R.string.chat_deleted)))
                .addOnFailureListener(e ->
                        Log.e(TAG, "deleteChat failed", e.toException()));
    }

    // ==================================================================
    // Empty State
    // ==================================================================

    private void updateEmptyState() {
        if (chatList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvChats.setVisibility(View.GONE);
            tvEmptyText.setText(currentTab == 1 ? R.string.no_groups : R.string.no_chats);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvChats.setVisibility(View.VISIBLE);
        }
    }

    // ==================================================================
    // Chat List Adapter
    // ==================================================================

    private class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_chat, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            Chat chat = chatList.get(position);
            holder.bind(chat);
        }

        @Override
        public int getItemCount() {
            return chatList.size();
        }

        class ChatViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar, ivOnlineDot, ivMuteIcon, ivGroupIcon;
            TextView tvName, tvLastMessage, tvTime, tvUnreadCount;
            View pinnedIndicator;

            ChatViewHolder(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                ivOnlineDot = itemView.findViewById(R.id.ivOnlineDot);
                ivMuteIcon = itemView.findViewById(R.id.ivMuteIcon);
                ivGroupIcon = itemView.findViewById(R.id.ivGroupIcon);
                tvName = itemView.findViewById(R.id.tvName);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
                pinnedIndicator = itemView.findViewById(R.id.pinnedIndicator);
            }

            void bind(Chat chat) {
                // Name
                String displayName = chat.getName();
                String otherUid = getOtherUserId(chat);
                if (otherUid != null && userCache.containsKey(otherUid)) {
                    displayName = userCache.get(otherUid).getName();
                }
                tvName.setText(displayName != null ? displayName : "");

                // Last message
                String lastMsg = chat.getLastMessage();
                if (chat.getLastMessageSenderName() != null && !"group".equals(chat.getType())) {
                    // Don't prefix sender name for 1-on-1
                } else if (chat.getLastMessageSenderName() != null) {
                    lastMsg = chat.getLastMessageSenderName() + ": " + lastMsg;
                }
                tvLastMessage.setText(lastMsg != null ? lastMsg : "");

                // Time
                if (chat.getLastMessageTime() > 0) {
                    tvTime.setText(DateUtils.formatRelativeTimeArabic(chat.getLastMessageTime()));
                } else {
                    tvTime.setText("");
                }

                // Unread count
                long unread = chat.getUnreadCount();
                if (unread > 0) {
                    tvUnreadCount.setVisibility(View.VISIBLE);
                    tvUnreadCount.setText(String.valueOf(unread));
                    // Make name bold
                    tvName.setTextAppearance(R.style.TextAppearance_AppCompat_Medium);
                } else {
                    tvUnreadCount.setVisibility(View.GONE);
                    tvName.setTextAppearance(R.style.TextAppearance_AppCompat_Body1);
                }

                // Avatar
                if ("group".equals(chat.getType())) {
                    ivGroupIcon.setVisibility(View.VISIBLE);
                    if (chat.getPhotoUrl() != null && !chat.getPhotoUrl().isEmpty()) {
                        Glide.with(requireContext()).load(chat.getPhotoUrl())
                                .circleCrop().placeholder(R.drawable.ic_group_placeholder)
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_group_placeholder);
                    }
                } else {
                    ivGroupIcon.setVisibility(View.GONE);
                    String photoUrl = chat.getPhotoUrl();
                    if (otherUid != null && userCache.containsKey(otherUid)) {
                        photoUrl = userCache.get(otherUid).getPhotoUrl();
                    }
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(requireContext()).load(photoUrl)
                                .circleCrop().placeholder(R.drawable.ic_placeholder_avatar)
                                .error(R.drawable.ic_placeholder_avatar)
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.drawable.ic_placeholder_avatar);
                    }
                }

                // Online dot
                if (otherUid != null && onlineStatusMap.containsKey(otherUid)) {
                    ivOnlineDot.setVisibility(View.VISIBLE);
                    ivOnlineDot.setImageResource(onlineStatusMap.get(otherUid) ?
                            R.drawable.ic_online_dot : R.drawable.ic_offline_dot);
                } else {
                    ivOnlineDot.setVisibility(View.GONE);
                }

                // Mute icon
                ivMuteIcon.setVisibility(chat.isMuted() ? View.VISIBLE : View.GONE);

                // Pinned indicator
                pinnedIndicator.setVisibility(pinnedChatIds.contains(chat.getChatId()) ?
                        View.VISIBLE : View.GONE);

                // Click to open chat
                itemView.setOnClickListener(v -> openChat(chat));

                // Long press for options
                itemView.setOnLongClickListener(v -> {
                    showChatOptions(chat, v);
                    return true;
                });
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    public void onDestroyView() {
        if (chatsChildListener != null && activeChatsQuery != null) {
            activeChatsQuery.removeChildEventListener(chatsChildListener);
        }
        super.onDestroyView();
    }
}
