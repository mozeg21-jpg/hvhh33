package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.news.kimo.R;
import com.news.kimo.databinding.ItemMessageReceivedBinding;
import com.news.kimo.databinding.ItemMessageSentBinding;
import com.news.kimo.models.Message;
import com.news.kimo.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_SENT = 0;
    public static final int TYPE_RECEIVED = 1;

    private final Context context;
    private final List<Message> messageList;
    private final String chatId;
    private final String currentUid;
    private final boolean isGroup;

    private OnMessageClickListener onMessageClickListener;
    private OnMessageLongClickListener onMessageLongClickListener;

    public interface OnMessageClickListener {
        void onImageClick(String imageUrl);
        void onVideoClick(String videoUrl);
        void onFileClick(String fileUrl, String fileName);
    }

    public interface OnMessageLongClickListener {
        void onEditClick(Message message, int position);
        void onDeleteClick(Message message, int position);
        void onReplyClick(Message message, int position);
        void onCopyClick(Message message, int position);
    }

    public MessageAdapter(Context context, List<Message> messageList, String chatId,
                          String currentUid, boolean isGroup) {
        this.context = context;
        this.messageList = messageList != null ? messageList : new ArrayList<>();
        this.chatId = chatId;
        this.currentUid = currentUid;
        this.isGroup = isGroup;
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId().equals(currentUid)) {
            return TYPE_SENT;
        }
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_SENT) {
            ItemMessageSentBinding binding = ItemMessageSentBinding.inflate(inflater, parent, false);
            return new SentViewHolder(binding);
        } else {
            ItemMessageReceivedBinding binding = ItemMessageReceivedBinding.inflate(inflater, parent, false);
            return new ReceivedViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(message, position);
        } else if (holder instanceof ReceivedViewHolder) {
            ((ReceivedViewHolder) holder).bind(message, position);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < messageList.size() && messageList.get(position).getMessageId() != null) {
            return messageList.get(position).getMessageId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ============================================================
    // Sent ViewHolder
    // ============================================================

    class SentViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageSentBinding binding;

        SentViewHolder(ItemMessageSentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Message message, int position) {
            // Reply preview
            if (message.getReplyToText() != null && !message.getReplyToText().isEmpty()) {
                binding.replyPreviewContainer.setVisibility(View.VISIBLE);
                binding.tvReplyText.setText(message.getReplyToText());
            } else {
                binding.replyPreviewContainer.setVisibility(View.GONE);
            }

            // Text message
            if (message.getText() != null && !message.getText().isEmpty()) {
                binding.tvMessageText.setVisibility(View.VISIBLE);
                binding.tvMessageText.setText(message.getText());
            } else {
                binding.tvMessageText.setVisibility(View.GONE);
            }

            // Image
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                binding.ivMessageImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(binding.ivMessageImage);
                binding.ivMessageImage.setOnClickListener(v -> {
                    if (onMessageClickListener != null) {
                        onMessageClickListener.onImageClick(message.getImageUrl());
                    }
                });
            } else {
                binding.ivMessageImage.setVisibility(View.GONE);
            }

            // Video
            if (message.getVideoUrl() != null && !message.getVideoUrl().isEmpty()) {
                binding.videoContainer.setVisibility(View.VISIBLE);
                binding.ivPlayIcon.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_video_placeholder)
                        .into(binding.ivVideoThumbnail);
                binding.videoContainer.setOnClickListener(v -> {
                    if (onMessageClickListener != null) {
                        onMessageClickListener.onVideoClick(message.getVideoUrl());
                    }
                });
            } else {
                binding.videoContainer.setVisibility(View.GONE);
            }

            // File
            if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                binding.fileContainer.setVisibility(View.VISIBLE);
                binding.tvFileName.setText(message.getFileName() != null ? message.getFileName() : "ملف");
                binding.fileContainer.setOnClickListener(v -> {
                    if (onMessageClickListener != null) {
                        onMessageClickListener.onFileClick(message.getFileUrl(), message.getFileName());
                    }
                });
            } else {
                binding.fileContainer.setVisibility(View.GONE);
            }

            // Time
            binding.tvMessageTime.setText(DateUtils.formatTime(message.getTimestamp()));

            // Edit indicator
            binding.ivEditedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);

            // Long press for options: edit, delete, reply, copy
            binding.messageBubble.setOnLongClickListener(v -> {
                showPopupMenu(v, message, position, true);
                return true;
            });
        }
    }

    // ============================================================
    // Received ViewHolder
    // ============================================================

    class ReceivedViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageReceivedBinding binding;

        ReceivedViewHolder(ItemMessageReceivedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Message message, int position) {
            // Sender name (groups)
            if (isGroup) {
                binding.tvSenderName.setVisibility(View.VISIBLE);
                binding.tvSenderName.setText(message.getSenderName() != null ? message.getSenderName() : "مستخدم");
                // Load sender avatar for group messages
                Glide.with(context)
                        .load(message.getSenderPhoto())
                        .apply(RequestOptions.circleCropTransform()
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar))
                        .into(binding.ivSenderAvatar);
                binding.ivSenderAvatar.setVisibility(View.VISIBLE);
            } else {
                binding.tvSenderName.setVisibility(View.GONE);
                binding.ivSenderAvatar.setVisibility(View.GONE);
            }

            // Reply preview
            if (message.getReplyToText() != null && !message.getReplyToText().isEmpty()) {
                binding.replyPreviewContainer.setVisibility(View.VISIBLE);
                binding.tvReplyText.setText(message.getReplyToText());
            } else {
                binding.replyPreviewContainer.setVisibility(View.GONE);
            }

            // Text message
            if (message.getText() != null && !message.getText().isEmpty()) {
                binding.tvMessageText.setVisibility(View.VISIBLE);
                binding.tvMessageText.setText(message.getText());
            } else {
                binding.tvMessageText.setVisibility(View.GONE);
            }

            // Image
            if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                binding.ivMessageImage.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(binding.ivMessageImage);
                binding.ivMessageImage.setOnClickListener(v -> {
                    if (onMessageClickListener != null) {
                        onMessageClickListener.onImageClick(message.getImageUrl());
                    }
                });
            } else {
                binding.ivMessageImage.setVisibility(View.GONE);
            }

            // Video
            if (message.getVideoUrl() != null && !message.getVideoUrl().isEmpty()) {
                binding.videoContainer.setVisibility(View.VISIBLE);
                binding.ivPlayIcon.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(message.getImageUrl())
                        .placeholder(R.drawable.ic_video_placeholder)
                        .into(binding.ivVideoThumbnail);
                binding.videoContainer.setOnClickListener(v -> {
                    if (onMessageClickListener != null) {
                        onMessageClickListener.onVideoClick(message.getVideoUrl());
                    }
                });
            } else {
                binding.videoContainer.setVisibility(View.GONE);
            }

            // File
            if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                binding.fileContainer.setVisibility(View.VISIBLE);
                binding.tvFileName.setText(message.getFileName() != null ? message.getFileName() : "ملف");
                binding.fileContainer.setOnClickListener(v -> {
                    if (onMessageClickListener != null) {
                        onMessageClickListener.onFileClick(message.getFileUrl(), message.getFileName());
                    }
                });
            } else {
                binding.fileContainer.setVisibility(View.GONE);
            }

            // Time
            binding.tvMessageTime.setText(DateUtils.formatTime(message.getTimestamp()));

            // Edit indicator
            binding.ivEditedIndicator.setVisibility(message.isEdited() ? View.VISIBLE : View.GONE);

            // Long press: reply, copy (no edit/delete for received)
            binding.messageBubble.setOnLongClickListener(v -> {
                showPopupMenu(v, message, position, false);
                return true;
            });
        }
    }

    // ============================================================
    // Popup Menu
    // ============================================================

    private void showPopupMenu(View anchor, Message message, int position, boolean isOwnMessage) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, R.string.menu_reply);
        popup.getMenu().add(0, 2, 1, R.string.menu_copy);
        if (isOwnMessage) {
            if (!message.isDeleted()) {
                popup.getMenu().add(0, 3, 2, R.string.menu_edit);
            }
            popup.getMenu().add(0, 4, 3, R.string.menu_delete);
        }
        popup.setOnMenuItemClickListener(item -> {
            if (onMessageLongClickListener == null) {
                Toast.makeText(context, R.string.action_not_available, Toast.LENGTH_SHORT).show();
                return false;
            }
            switch (item.getItemId()) {
                case 1: // Reply
                    onMessageLongClickListener.onReplyClick(message, position);
                    break;
                case 2: // Copy
                    onMessageLongClickListener.onCopyClick(message, position);
                    break;
                case 3: // Edit
                    onMessageLongClickListener.onEditClick(message, position);
                    break;
                case 4: // Delete
                    onMessageLongClickListener.onDeleteClick(message, position);
                    break;
            }
            return true;
        });
        popup.show();
    }

    // ============================================================
    // Data Operations
    // ============================================================

    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    public void addMessageAtStart(Message message) {
        messageList.add(0, message);
        notifyItemInserted(0);
    }

    public void addMessages(List<Message> newMessages) {
        int startPos = messageList.size();
        messageList.addAll(newMessages);
        notifyItemRangeInserted(startPos, newMessages.size());
    }

    public void updateMessage(int position, Message message) {
        if (position >= 0 && position < messageList.size()) {
            messageList.set(position, message);
            notifyItemChanged(position);
        }
    }

    public void removeMessage(int position) {
        if (position >= 0 && position < messageList.size()) {
            messageList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Message getMessage(int position) {
        if (position >= 0 && position < messageList.size()) {
            return messageList.get(position);
        }
        return null;
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    // ---- Setters ----

    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.onMessageClickListener = listener;
    }

    public void setOnMessageLongClickListener(OnMessageLongClickListener listener) {
        this.onMessageLongClickListener = listener;
    }
}