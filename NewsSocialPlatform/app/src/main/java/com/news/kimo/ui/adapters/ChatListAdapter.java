package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.news.kimo.R;
import com.news.kimo.databinding.ItemChatBinding;
import com.news.kimo.models.Chat;
import com.news.kimo.ui.activities.ChatActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private final Context context;
    private final List<Chat> chatList;
    private final String currentUid;
    private final FirebaseDatabase realtimeDb;
    private String searchQuery = "";

    public interface OnChatLongClickListener {
        void onMuteClick(Chat chat, int position);
        void onPinClick(Chat chat, int position);
        void onDeleteClick(Chat chat, int position);
        void onClearChat(Chat chat, int position);
    }

    private OnChatLongClickListener onChatLongClickListener;

    public ChatListAdapter(Context context, List<Chat> chatList, String currentUid) {
        this.context = context;
        this.chatList = chatList != null ? chatList : new ArrayList<>();
        this.currentUid = currentUid;
        this.realtimeDb = FirebaseDatabase.getInstance();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemChatBinding binding = ItemChatBinding.inflate(inflater, parent, false);
        return new ChatViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(chatList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < chatList.size() && chatList.get(position).getChatId() != null) {
            return chatList.get(position).getChatId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ItemChatBinding binding;

        ChatViewHolder(ItemChatBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Chat chat, int position) {
            // Avatar
            if (chat.getPhotoUrl() != null && !chat.getPhotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(chat.getPhotoUrl())
                        .apply(RequestOptions.circleCropTransform()
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar))
                        .into(binding.ivChatAvatar);
            }

            // Name
            binding.tvChatName.setText(chat.getName());

            // Last message with sender prefix for groups
            if ("group".equals(chat.getType()) && chat.getLastMessageSenderName() != null) {
                binding.tvLastMessage.setText(chat.getLastMessageSenderName() + ": " +
                        StringUtils.truncateText(chat.getLastMessage(), 40));
            } else {
                binding.tvLastMessage.setText(StringUtils.truncateText(chat.getLastMessage(), 50));
            }

            // Time (relative)
            binding.tvChatTime.setText(DateUtils.formatRelativeTimeArabic(chat.getLastMessageTime()));

            // Unread badge (red circle with count)
            if (chat.getUnreadCount() > 0) {
                binding.tvUnreadBadge.setVisibility(View.VISIBLE);
                String countStr = chat.getUnreadCount() > 99 ? "99+" : String.valueOf(chat.getUnreadCount());
                binding.tvUnreadBadge.setText(countStr);
            } else {
                binding.tvUnreadBadge.setVisibility(View.GONE);
            }

            // Mute icon
            binding.ivMuteIcon.setVisibility(chat.isMuted() ? View.VISIBLE : View.GONE);

            // Online dot - listen to isOnline for the other user
            if ("private".equals(chat.getType()) && chat.getParticipants() != null) {
                for (String uid : chat.getParticipants()) {
                    if (!uid.equals(currentUid)) {
                        listenToOnlineStatus(uid, binding.ivOnlineDot);
                        break;
                    }
                }
            } else {
                binding.ivOnlineDot.setVisibility(View.GONE);
            }

            // Typing indicator (hidden by default, updated by activity)
            binding.tvTypingIndicator.setVisibility(View.GONE);

            // Click -> ChatActivity
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class)
                        .putExtra("chatId", chat.getChatId())
                        .putExtra("chatName", chat.getName())
                        .putExtra("isGroup", "group".equals(chat.getType()));
                context.startActivity(intent);
            });

            // Long press: mute, pin, delete, clear chat
            itemView.setOnLongClickListener(v -> {
                showPopupMenu(v, chat, position);
                return true;
            });
        }

        private void listenToOnlineStatus(String uid, View onlineDot) {
            onlineDot.setVisibility(View.INVISIBLE);
            realtimeDb.getReference(Constants.USERS).child(uid).child("isOnline")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Boolean isOnline = snapshot.getValue(Boolean.class);
                            if (Boolean.TRUE.equals(isOnline)) {
                                onlineDot.setVisibility(View.VISIBLE);
                            } else {
                                onlineDot.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            onlineDot.setVisibility(View.GONE);
                        }
                    });
        }
    }

    // ---- Popup Menu ----

    private void showPopupMenu(View anchor, Chat chat, int position) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, chat.isMuted() ? R.string.unmute_chat : R.string.mute_chat);
        popup.getMenu().add(0, 2, 1, R.string.pin_chat);
        popup.getMenu().add(0, 3, 2, R.string.clear_chat);
        popup.getMenu().add(0, 4, 3, R.string.delete_chat);
        popup.setOnMenuItemClickListener(item -> {
            if (onChatLongClickListener == null) {
                Toast.makeText(context, R.string.action_not_available, Toast.LENGTH_SHORT).show();
                return false;
            }
            switch (item.getItemId()) {
                case 1:
                    onChatLongClickListener.onMuteClick(chat, position);
                    break;
                case 2:
                    onChatLongClickListener.onPinClick(chat, position);
                    break;
                case 3:
                    onChatLongClickListener.onClearChat(chat, position);
                    break;
                case 4:
                    onChatLongClickListener.onDeleteClick(chat, position);
                    break;
            }
            return true;
        });
        popup.show();
    }

    // ---- Search Filter ----

    public void filter(String query) {
        this.searchQuery = query != null ? query.toLowerCase().trim() : "";
        notifyDataSetChanged();
    }

    // ---- Sort with Pins First ----

    public void sortChats() {
        // Pinned chats sorted first by lastMessageTime, then non-pinned
        Collections.sort(chatList, (a, b) -> {
            boolean aPinned = a.isMuted(); // reuse muted for pinned or add isPinned
            boolean bPinned = b.isMuted();
            if (aPinned && !bPinned) return -1;
            if (!aPinned && bPinned) return 1;
            return Long.compare(b.getLastMessageTime(), a.getLastMessageTime());
        });
        notifyDataSetChanged();
    }

    // ---- Update Typing ----

    public void setTyping(String chatId, boolean isTyping) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatList.get(i).getChatId().equals(chatId)) {
                // notify activity to handle typing display
                notifyItemChanged(i);
                break;
            }
        }
    }

    // ---- Data Operations ----

    public void addChat(Chat chat) {
        chatList.add(0, chat);
        notifyItemInserted(0);
    }

    public void addChats(List<Chat> newChats) {
        int startPos = chatList.size();
        chatList.addAll(newChats);
        notifyItemRangeInserted(startPos, newChats.size());
    }

    public void updateChat(int position, Chat chat) {
        if (position >= 0 && position < chatList.size()) {
            chatList.set(position, chat);
            notifyItemChanged(position);
        }
    }

    public void removeChat(int position) {
        if (position >= 0 && position < chatList.size()) {
            chatList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Chat getChat(int position) {
        if (position >= 0 && position < chatList.size()) {
            return chatList.get(position);
        }
        return null;
    }

    public List<Chat> getChatList() {
        return chatList;
    }

    // ---- Setters ----

    public void setOnChatLongClickListener(OnChatLongClickListener listener) {
        this.onChatLongClickListener = listener;
    }
}