package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.news.kimo.R;
import com.news.kimo.databinding.ItemNotificationBinding;
import com.news.kimo.models.NotificationItem;
import com.news.kimo.ui.activities.ChatActivity;
import com.news.kimo.ui.activities.HashtagActivity;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<NotificationItem> notificationList;
    private final FirebaseFirestore db;
    private OnNotificationClickListener onNotificationClickListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(NotificationItem notification);
        void onUserClick(String uid);
    }

    public NotificationAdapter(Context context, List<NotificationItem> notificationList) {
        this.context = context;
        this.notificationList = notificationList != null ? notificationList : new ArrayList<>();
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemNotificationBinding binding = ItemNotificationBinding.inflate(inflater, parent, false);
        return new NotificationViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        holder.bind(notificationList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < notificationList.size() && notificationList.get(position).getNotificationId() != null) {
            return notificationList.get(position).getNotificationId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ItemNotificationBinding binding;

        NotificationViewHolder(ItemNotificationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(NotificationItem notification, int position) {
            // Load avatar from users/{fromUid}
            String cachedPhoto = com.news.kimo.utils.CacheHelper.getInstance().getUserPhoto(notification.getFromUid());
            if (cachedPhoto != null) {
                Glide.with(context)
                        .load(cachedPhoto)
                        .apply(RequestOptions.circleCropTransform()
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar))
                        .into(binding.ivNotificationAvatar);
            } else {
                db.collection(Constants.USERS).document(notification.getFromUid())
                        .get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                String photoUrl = snapshot.getString("photoUrl");
                                String name = snapshot.getString("name");
                                if (photoUrl != null) {
                                    com.news.kimo.utils.CacheHelper.getInstance().cacheUserPhoto(notification.getFromUid(), photoUrl);
                                    Glide.with(context)
                                            .load(photoUrl)
                                            .apply(RequestOptions.circleCropTransform()
                                                    .placeholder(R.drawable.ic_default_avatar)
                                                    .error(R.drawable.ic_default_avatar))
                                            .into(binding.ivNotificationAvatar);
                                }
                                if (name != null) {
                                    notification.setFromName(name);
                                }
                            }
                        });
                Glide.with(context)
                        .load(notification.getFromPhoto())
                        .apply(RequestOptions.circleCropTransform()
                                .placeholder(R.drawable.ic_default_avatar)
                                .error(R.drawable.ic_default_avatar))
                        .into(binding.ivNotificationAvatar);
            }

            // Rich text: name (bold) + action text + time
            String name = notification.getFromName() != null ? notification.getFromName() : "مستخدم";
            String actionText = getActionText(notification.getType());
            String timeStr = DateUtils.formatRelativeTimeArabic(notification.getTimestamp());

            SpannableStringBuilder builder = new SpannableStringBuilder();
            int nameStart = builder.length();
            builder.append(name);
            builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), nameStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(" ");
            builder.append(actionText);
            builder.append(" \u2022 ");
            int timeStart = builder.length();
            builder.append(timeStr);
            builder.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.textSecondary)),
                    timeStart, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            binding.tvNotificationText.setText(builder);

            // Preview text
            if (notification.getBody() != null && !notification.getBody().isEmpty()) {
                binding.tvPreviewText.setVisibility(View.VISIBLE);
                binding.tvPreviewText.setText(notification.getBody());
            } else {
                binding.tvPreviewText.setVisibility(View.GONE);
            }

            // Unread dot (blue circle)
            binding.ivUnreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Different icons by type
            setNotificationIcon(notification.getType());

            // Click: navigate based on type + mark as read
            itemView.setOnClickListener(v -> {
                markAsRead(notification, position);
                navigateToTarget(notification);
                if (onNotificationClickListener != null) {
                    onNotificationClickListener.onNotificationClick(notification);
                }
            });

            // Avatar click
            binding.ivNotificationAvatar.setOnClickListener(v -> {
                context.startActivity(new Intent(context, ProfileActivity.class)
                        .putExtra(Constants.EXTRA_USER_ID, notification.getFromUid()));
                if (onNotificationClickListener != null) {
                    onNotificationClickListener.onUserClick(notification.getFromUid());
                }
            });
        }

        private void setNotificationIcon(String type) {
            if (type == null) {
                binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_general);
                return;
            }
            switch (type) {
                case Constants.NOTIFICATION_LIKE:
                case Constants.NOTIFICATION_REACTION:
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_like);
                    binding.ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.reaction_like));
                    break;
                case Constants.NOTIFICATION_COMMENT:
                case Constants.NOTIFICATION_REPLY:
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_comment);
                    binding.ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.reaction_comment));
                    break;
                case Constants.NOTIFICATION_FOLLOW:
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_follow);
                    binding.ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimary));
                    break;
                case Constants.NOTIFICATION_MESSAGE:
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_message);
                    binding.ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.reaction_message));
                    break;
                case Constants.NOTIFICATION_MENTION:
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_mention);
                    binding.ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.colorAccent));
                    break;
                case Constants.NOTIFICATION_VERIFICATION:
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_verification);
                    binding.ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.verification_color));
                    break;
                case Constants.NOTIFICATION_SYSTEM:
                default:
                    binding.ivNotificationIcon.setImageResource(R.drawable.ic_notification_general);
                    binding.ivNotificationIcon.setColorFilter(ContextCompat.getColor(context, R.color.textSecondary));
                    break;
            }
        }

        private String getActionText(String type) {
            if (type == null) return "";
            switch (type) {
                case Constants.NOTIFICATION_LIKE: return "أعجب بمنشورك";
                case Constants.NOTIFICATION_COMMENT: return "علّق على منشورك";
                case Constants.NOTIFICATION_REPLY: return "ردّ على تعليقك";
                case Constants.NOTIFICATION_FOLLOW: return "بدأ بمتابعتك";
                case Constants.NOTIFICATION_MENTION: return "أشار إليك";
                case Constants.NOTIFICATION_REPOST: return "أعاد نشر منشورك";
                case Constants.NOTIFICATION_MESSAGE: return "أرسل لك رسالة";
                case Constants.NOTIFICATION_REACTION: return "تفاعل مع منشورك";
                case Constants.NOTIFICATION_VERIFICATION: return "تم تحديث حالة التحقق";
                case Constants.NOTIFICATION_SYSTEM: return "إشعار جديد من النظام";
                default: return "";
            }
        }
    }

    // ---- Mark as Read ----

    private void markAsRead(NotificationItem notification, int position) {
        if (!notification.isRead()) {
            db.collection(Constants.NOTIFICATIONS)
                    .document(notification.getToUid())
                    .collection("items")
                    .document(notification.getNotificationId())
                    .update("isRead", true);
            notification.setRead(true);
            notifyItemChanged(position);
        }
    }

    // ---- Navigate Based on Type ----

    private void navigateToTarget(NotificationItem notification) {
        Intent intent = null;
        String type = notification.getType();
        if (type == null) return;

        switch (type) {
            case Constants.NOTIFICATION_LIKE:
            case Constants.NOTIFICATION_COMMENT:
            case Constants.NOTIFICATION_REPLY:
            case Constants.NOTIFICATION_REPOST:
            case Constants.NOTIFICATION_REACTION:
            case Constants.NOTIFICATION_MENTION:
                if (notification.getPostId() != null) {
                    intent = new Intent(context, PostDetailsActivity.class)
                            .putExtra(Constants.EXTRA_POST_ID, notification.getPostId());
                }
                break;
            case Constants.NOTIFICATION_FOLLOW:
                intent = new Intent(context, ProfileActivity.class)
                        .putExtra(Constants.EXTRA_USER_ID, notification.getFromUid());
                break;
            case Constants.NOTIFICATION_MESSAGE:
                if (notification.getMessageId() != null) {
                    intent = new Intent(context, ChatActivity.class)
                            .putExtra("chatId", notification.getMessageId());
                }
                break;
            case Constants.NOTIFICATION_VERIFICATION:
            case Constants.NOTIFICATION_SYSTEM:
            default:
                // No specific navigation for system notifications
                break;
        }
        if (intent != null) {
            context.startActivity(intent);
        }
    }

    // ---- Data Operations ----

    public void addNotification(NotificationItem notification) {
        notificationList.add(0, notification);
        notifyItemInserted(0);
    }

    public void addNotifications(List<NotificationItem> newNotifications) {
        int startPos = notificationList.size();
        notificationList.addAll(newNotifications);
        notifyItemRangeInserted(startPos, newNotifications.size());
    }

    public void markAllAsRead() {
        for (int i = 0; i < notificationList.size(); i++) {
            notificationList.get(i).setRead(true);
        }
        notifyDataSetChanged();
    }

    public int getUnreadCount() {
        int count = 0;
        for (NotificationItem item : notificationList) {
            if (!item.isRead()) count++;
        }
        return count;
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.onNotificationClickListener = listener;
    }
}