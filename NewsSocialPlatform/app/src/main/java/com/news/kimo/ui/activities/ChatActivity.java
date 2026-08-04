package com.news.kimo.ui.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.news.kimo.R;
import com.news.kimo.databinding.ActivityChatBinding;
import com.news.kimo.models.Chat;
import com.news.kimo.models.Message;
import com.news.kimo.models.User;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;
import com.news.kimo.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chat activity for real-time messaging between users or in groups.
 * Supports text, image, video, file, audio, location, and GIF messages.
 * Features: reply, edit, delete messages, typing indicator, online status,
 * attachment bottom sheet, read receipts, and scroll-based pagination.
 */
public class ChatActivity extends BaseActivity {

    private static final String TAG = "ChatActivity";
    private static final int PAGE_SIZE = 30;
    private static final int REQUEST_IMAGE = 2001;
    private static final int REQUEST_VIDEO = 2002;
    private static final int REQUEST_FILE = 2003;
    private static final int REQUEST_CONTACT = 2004;
    private static final int REQUEST_LOCATION_PERMISSION = 2005;
    private static final int REQUEST_AUDIO_PERMISSION = 2006;

    private ActivityChatBinding binding;
    private DatabaseReference rootRef;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private StorageReference storageRef;
    private FusedLocationProviderClient fusedLocationClient;

    private String chatId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhoto;
    private boolean isGroup;
    private String currentUid;
    private User currentUser;

    private MessageAdapter messageAdapter;
    private final List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager layoutManager;

    private ChildEventListener messageChildListener;
    private ValueEventListener onlineStatusListener;
    private ValueEventListener typingListener;
    private Query activeMessagesQuery;

    private Message replyToMessage;
    private Message editingMessage;
    private boolean isLoadingMore = false;
    private long oldestMessageTimestamp = Long.MAX_VALUE;
    private boolean allMessagesLoaded = false;

    private MediaRecorder mediaRecorder;
    private File audioFile;
    private boolean isRecording = false;
    private long recordingStartTime = 0;
    private final Handler recordingHandler = new Handler(Looper.getMainLooper());
    private Runnable recordingTimerRunnable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rootRef = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        sessionManager = SessionManager.getInstance(this);
        storageRef = FirebaseStorage.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        currentUser = sessionManager.loadCurrentUser();
        currentUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        getIntentExtras();
        if (chatId == null && otherUserId == null) {
            showError(getString(R.string.error_generic));
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupToolbar();
        setupMessageInput();
        setupAttachmentButton();

        if (chatId != null) {
            loadChatInfo();
            listenForMessages();
            listenForOnlineStatus();
            listenForTyping();
            markMessagesAsRead();
        } else {
            findOrCreateChat();
        }
    }

    // ==================================================================
    // Intent Extras
    // ==================================================================

    private void getIntentExtras() {
        if (getIntent() != null) {
            chatId = getIntent().getStringExtra("chatId");
            otherUserId = getIntent().getStringExtra("otherUserId");
            otherUserName = getIntent().getStringExtra("otherUserName");
            otherUserPhoto = getIntent().getStringExtra("otherUserPhoto");
            isGroup = getIntent().getBooleanExtra("isGroup", false);
        }
    }

    // ==================================================================
    // Initialisation
    // ==================================================================

    private void initViews() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        binding.ivSend.setOnClickListener(v -> sendTextMessage());
        binding.ivAttachment.setOnClickListener(v -> showAttachmentBottomSheet());

        // Reply cancel
        binding.layoutReply.ivCancelReply.setOnClickListener(v -> clearReply());

        // Edit cancel
        if (binding.layoutEdit != null) {
            binding.layoutEdit.ivCancelEdit.setOnClickListener(v -> clearEdit());
            if (binding.layoutEdit.ivSendEdit != null) {
                binding.layoutEdit.ivSendEdit.setOnClickListener(v -> sendEditedMessage());
            }
        }

        // Audio recording
        binding.ivRecordAudio.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    startRecording();
                    return true;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    stopRecording();
                    return true;
            }
            return false;
        });

        updateToolbarInfo();
    }

    private void setupToolbar() {
        Toolbar toolbar = binding.toolbar;
        toolbar.inflateMenu(R.menu.menu_chat);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_block_user) {
                showBlockDialog();
                return true;
            } else if (id == R.id.action_mute_chat) {
                toggleMuteChat();
                return true;
            } else if (id == R.id.action_clear_chat) {
                showClearChatDialog();
                return true;
            }
            return false;
        });
    }

    private void updateToolbarInfo() {
        binding.tvChatName.setText(otherUserName != null ? otherUserName : "");
        if (!isGroup && otherUserPhoto != null) {
            loadCircularImage(otherUserPhoto, binding.ivChatPhoto);
        }
    }

    private void setupRecyclerView() {
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        binding.rvMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter();
        binding.rvMessages.setAdapter(messageAdapter);

        // Pagination: load more when scrolling to top
        binding.rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!layoutManager.canScrollVertically(1) && !isLoadingMore && !allMessagesLoaded) {
                    loadMoreMessages();
                }
            }
        });
    }

    private void setupMessageInput() {
        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Toggle send/record button visibility
                if (s.toString().trim().isEmpty()) {
                    binding.ivSend.setVisibility(View.GONE);
                    binding.ivRecordAudio.setVisibility(View.VISIBLE);
                } else {
                    binding.ivSend.setVisibility(View.VISIBLE);
                    binding.ivRecordAudio.setVisibility(View.GONE);
                }
                // Typing indicator
                setTypingStatus(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Reset typing timer
                typingHandler.removeCallbacks(typingRunnable);
                typingRunnable = () -> setTypingStatus(false);
                typingHandler.postDelayed(typingRunnable, 2000);
            }
        });
    }

    private final Handler typingHandler = new Handler(Looper.getMainLooper());
    private Runnable typingRunnable;

    private void setupAttachmentButton() {
        // Attachment bottom sheet handled in showAttachmentBottomSheet()
    }

    // ==================================================================
    // Chat Setup
    // ==================================================================

    private void findOrCreateChat() {
        // Try to find an existing chat between current user and other user
        rootRef.child(Constants.CHATS)
                .orderByChild("participants/" + currentUid)
                .equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot chatSnap : snapshot.getChildren()) {
                            Chat chat = chatSnap.getValue(Chat.class);
                            if (chat != null && !"group".equals(chat.getType())) {
                                // Check if other user is also a participant
                                if (chat.getParticipants() != null &&
                                        chat.getParticipants().contains(otherUserId)) {
                                    chatId = chatSnap.getKey();
                                    loadChatInfo();
                                    listenForMessages();
                                    markMessagesAsRead();
                                    return;
                                }
                            }
                        }
                        // No existing chat found, create new one
                        createNewChat();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "findOrCreateChat cancelled", error.toException());
                        createNewChat();
                    }
                });
    }

    private void createNewChat() {
        if (chatId != null) return;
        chatId = rootRef.child(Constants.CHATS).push().getKey();
        if (chatId == null) return;

        Map<String, Object> chatData = new HashMap<>();
        chatData.put("type", "private");
        chatData.put("name", otherUserName);
        chatData.put("photoUrl", otherUserPhoto);
        chatData.put("lastMessage", "");
        chatData.put("lastMessageTime", System.currentTimeMillis());
        chatData.put("lastMessageSenderName", currentUser != null ? currentUser.getName() : "");
        chatData.put("unreadCount", 0);
        chatData.put("isMuted", false);
        chatData.put("createdAt", System.currentTimeMillis());

        Map<String, Boolean> participantsMap = new HashMap<>();
        participantsMap.put(currentUid, true);
        participantsMap.put(otherUserId, true);
        chatData.put("participants", participantsMap);

        rootRef.child(Constants.CHATS).child(chatId).setValue(chatData)
                .addOnSuccessListener(aVoid -> {
                    loadChatInfo();
                    listenForMessages();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "createNewChat failed", e);
                    showError(getString(R.string.error_generic));
                });
    }

    private void loadChatInfo() {
        if (chatId == null) return;
        rootRef.child(Constants.CHATS).child(chatId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Chat chat = snapshot.getValue(Chat.class);
                        if (chat != null) {
                            binding.tvChatName.setText(chat.getName());
                            if (chat.getPhotoUrl() != null && !chat.getPhotoUrl().isEmpty()) {
                                loadCircularImage(chat.getPhotoUrl(), binding.ivChatPhoto);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "loadChatInfo cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Messages Listener (Real-time)
    // ==================================================================

    private void listenForMessages() {
        if (chatId == null) return;

        activeMessagesQuery = rootRef.child(Constants.MESSAGES)
                .child(chatId)
                .orderByChild("timestamp")
                .limitToLast(PAGE_SIZE);

        messageChildListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    message.setMessageId(snapshot.getKey());
                    if (message.getTimestamp() < oldestMessageTimestamp) {
                        oldestMessageTimestamp = message.getTimestamp();
                    }
                    // Avoid duplicates
                    int existingIndex = findMessageIndex(message.getMessageId());
                    if (existingIndex >= 0) {
                        messageList.set(existingIndex, message);
                    } else {
                        messageList.add(message);
                    }
                    messageList.sort((m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                    messageAdapter.notifyDataSetChanged();
                    // Auto-scroll to bottom (newest since reverse)
                    if (previousChildName == null) {
                        binding.rvMessages.scrollToPosition(0);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                if (message != null) {
                    message.setMessageId(snapshot.getKey());
                    int index = findMessageIndex(message.getMessageId());
                    if (index >= 0) {
                        messageList.set(index, message);
                        messageAdapter.notifyItemChanged(index);
                    }
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String key = snapshot.getKey();
                int index = findMessageIndex(key);
                if (index >= 0) {
                    messageList.remove(index);
                    messageAdapter.notifyItemRemoved(index);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "listenForMessages cancelled", error.toException());
            }
        };

        activeMessagesQuery.addChildEventListener(messageChildListener);
    }

    private int findMessageIndex(String messageId) {
        for (int i = 0; i < messageList.size(); i++) {
            if (messageList.get(i).getMessageId() != null &&
                    messageList.get(i).getMessageId().equals(messageId)) {
                return i;
            }
        }
        return -1;
    }

    // ==================================================================
    // Pagination
    // ==================================================================

    private void loadMoreMessages() {
        if (chatId == null || isLoadingMore || allMessagesLoaded) return;
        isLoadingMore = true;

        Query olderMessagesQuery = rootRef.child(Constants.MESSAGES)
                .child(chatId)
                .orderByChild("timestamp")
                .endAt(oldestMessageTimestamp - 1)
                .limitToLast(PAGE_SIZE);

        olderMessagesQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                int oldSize = messageList.size();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Message message = snap.getValue(Message.class);
                    if (message != null) {
                        message.setMessageId(snap.getKey());
                        if (findMessageIndex(message.getMessageId()) < 0) {
                            messageList.add(message);
                            if (message.getTimestamp() < oldestMessageTimestamp) {
                                oldestMessageTimestamp = message.getTimestamp();
                            }
                            count++;
                        }
                    }
                }
                if (count > 0) {
                    messageList.sort((m1, m2) -> Long.compare(m2.getTimestamp(), m1.getTimestamp()));
                    messageAdapter.notifyDataSetChanged();
                    // Scroll to the first old item loaded
                    binding.rvMessages.scrollToPosition(count - 1);
                } else {
                    allMessagesLoaded = true;
                }
                isLoadingMore = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                isLoadingMore = false;
                Log.e(TAG, "loadMoreMessages cancelled", error.toException());
            }
        });
    }

    // ==================================================================
    // Send Messages
    // ==================================================================

    private void sendTextMessage() {
        if (editingMessage != null) {
            sendEditedMessage();
            return;
        }

        String text = binding.etMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        if (!isNetworkAvailable()) {
            showError(getString(R.string.error_no_internet));
            return;
        }

        binding.etMessage.setText("");
        setTypingStatus(false);

        String pushId = rootRef.child(Constants.MESSAGES).child(chatId).push().getKey();
        if (pushId == null) return;

        Message message = new Message();
        message.setMessageId(pushId);
        message.setChatId(chatId);
        message.setSenderId(currentUid);
        message.setSenderName(currentUser != null ? currentUser.getName() : "");
        message.setSenderPhoto(currentUser != null ? currentUser.getPhotoUrl() : "");
        message.setText(text);
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setEdited(false);
        message.setDeleted(false);

        if (replyToMessage != null) {
            message.setReplyToMessageId(replyToMessage.getMessageId());
            message.setReplyToText(replyToMessage.getText());
            clearReply();
        }

        rootRef.child(Constants.MESSAGES).child(chatId).child(pushId).setValue(message)
                .addOnSuccessListener(aVoid -> updateChatLastMessage(text))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "sendTextMessage failed", e);
                    showError(getString(R.string.error_sending_message));
                });
    }

    private void sendImageMessage(Uri imageUri) {
        if (imageUri == null || chatId == null) return;
        showLoading();

        String fileName = "chat_images/" + chatId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageRef.child(fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    sendMessageWithMedia("imageUrl", downloadUrl, null);
                    hideLoading();
                }))
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError(getString(R.string.error_uploading_image));
                });
    }

    private void sendVideoMessage(Uri videoUri) {
        if (videoUri == null || chatId == null) return;
        showLoading();

        String fileName = "chat_videos/" + chatId + "/" + System.currentTimeMillis() + ".mp4";
        StorageReference videoRef = storageRef.child(fileName);

        videoRef.putFile(videoUri)
                .addOnSuccessListener(taskSnapshot -> videoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    sendMessageWithMedia("videoUrl", downloadUrl, null);
                    hideLoading();
                }))
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError(getString(R.string.error_uploading_video));
                });
    }

    private void sendFileMessage(Uri fileUri, String fileName) {
        if (fileUri == null || chatId == null) return;
        showLoading();

        String storageName = "chat_files/" + chatId + "/" + System.currentTimeMillis() + "_" + fileName;
        StorageReference fileRef = storageRef.child(storageName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    sendMessageWithFile(downloadUrl, fileName);
                    hideLoading();
                }))
                .addOnFailureListener(e -> {
                    hideLoading();
                    showError(getString(R.string.error_uploading_file));
                });
    }

    private void sendLocationMessage(double latitude, double longitude) {
        String pushId = rootRef.child(Constants.MESSAGES).child(chatId).push().getKey();
        if (pushId == null) return;

        Map<String, Double> locationMap = new HashMap<>();
        locationMap.put("latitude", latitude);
        locationMap.put("longitude", longitude);

        Message message = new Message();
        message.setMessageId(pushId);
        message.setChatId(chatId);
        message.setSenderId(currentUid);
        message.setSenderName(currentUser != null ? currentUser.getName() : "");
        message.setSenderPhoto(currentUser != null ? currentUser.getPhotoUrl() : "");
        message.setText(getString(R.string.shared_location));
        message.setLocation(locationMap);
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setEdited(false);
        message.setDeleted(false);

        rootRef.child(Constants.MESSAGES).child(chatId).child(pushId).setValue(message)
                .addOnSuccessListener(aVoid -> updateChatLastMessage(getString(R.string.shared_location)));
    }

    private void sendMessageWithMedia(String mediaField, String url, String fileName) {
        String pushId = rootRef.child(Constants.MESSAGES).child(chatId).push().getKey();
        if (pushId == null) return;

        Message message = new Message();
        message.setMessageId(pushId);
        message.setChatId(chatId);
        message.setSenderId(currentUid);
        message.setSenderName(currentUser != null ? currentUser.getName() : "");
        message.setSenderPhoto(currentUser != null ? currentUser.getPhotoUrl() : "");
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setEdited(false);
        message.setDeleted(false);

        if ("imageUrl".equals(mediaField)) {
            message.setImageUrl(url);
            message.setText("");
            updateChatLastMessage(getString(R.string.sent_photo));
        } else if ("videoUrl".equals(mediaField)) {
            message.setVideoUrl(url);
            message.setText("");
            updateChatLastMessage(getString(R.string.sent_video));
        }

        if (replyToMessage != null) {
            message.setReplyToMessageId(replyToMessage.getMessageId());
            message.setReplyToText(replyToMessage.getText());
            clearReply();
        }

        rootRef.child(Constants.MESSAGES).child(chatId).child(pushId).setValue(message);
    }

    private void sendMessageWithFile(String url, String fileName) {
        String pushId = rootRef.child(Constants.MESSAGES).child(chatId).push().getKey();
        if (pushId == null) return;

        Message message = new Message();
        message.setMessageId(pushId);
        message.setChatId(chatId);
        message.setSenderId(currentUid);
        message.setSenderName(currentUser != null ? currentUser.getName() : "");
        message.setSenderPhoto(currentUser != null ? currentUser.getPhotoUrl() : "");
        message.setFileUrl(url);
        message.setFileName(fileName);
        message.setText("");
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setEdited(false);
        message.setDeleted(false);

        rootRef.child(Constants.MESSAGES).child(chatId).child(pushId).setValue(message)
                .addOnSuccessListener(aVoid -> updateChatLastMessage(getString(R.string.sent_file)));
    }

    // ==================================================================
    // Audio Recording
    // ==================================================================

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION);
            return;
        }
        try {
            audioFile = File.createTempFile("audio_", ".3gp", getCacheDir());
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordingStartTime = System.currentTimeMillis();
            binding.tvRecordingTime.setVisibility(View.VISIBLE);
            updateRecordingTimer();
        } catch (IOException e) {
            Log.e(TAG, "startRecording failed", e);
            isRecording = false;
        }
    }

    private void updateRecordingTimer() {
        recordingTimerRunnable = () -> {
            if (isRecording) {
                long elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000;
                binding.tvRecordingTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                        elapsed / 60, elapsed % 60));
                recordingHandler.postDelayed(recordingTimerRunnable, 1000);
            }
        };
        recordingHandler.postDelayed(recordingTimerRunnable, 1000);
    }

    private void stopRecording() {
        if (!isRecording || mediaRecorder == null) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
        } catch (Exception e) {
            Log.e(TAG, "stopRecording error", e);
        }
        isRecording = false;
        recordingHandler.removeCallbacks(recordingTimerRunnable);
        binding.tvRecordingTime.setVisibility(View.GONE);

        // Upload and send
        if (audioFile != null && audioFile.exists()) {
            showLoading();
            String storageName = "chat_audio/" + chatId + "/" + System.currentTimeMillis() + ".3gp";
            StorageReference audioRef = storageRef.child(storageName);

            Uri audioUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", audioFile);
            audioRef.putFile(audioUri)
                    .addOnSuccessListener(taskSnapshot -> audioRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        sendAudioMessage(uri.toString());
                        hideLoading();
                    }))
                    .addOnFailureListener(e -> {
                        hideLoading();
                        showError(getString(R.string.error_sending_audio));
                    });
        }
    }

    private void sendAudioMessage(String audioUrl) {
        String pushId = rootRef.child(Constants.MESSAGES).child(chatId).push().getKey();
        if (pushId == null) return;

        long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;

        Message message = new Message();
        message.setMessageId(pushId);
        message.setChatId(chatId);
        message.setSenderId(currentUid);
        message.setSenderName(currentUser != null ? currentUser.getName() : "");
        message.setSenderPhoto(currentUser != null ? currentUser.getPhotoUrl() : "");
        message.setAudioUrl(audioUrl);
        message.setDuration(duration);
        message.setText("");
        message.setTimestamp(System.currentTimeMillis());
        message.setRead(false);
        message.setEdited(false);
        message.setDeleted(false);

        rootRef.child(Constants.MESSAGES).child(chatId).child(pushId).setValue(message)
                .addOnSuccessListener(aVoid -> updateChatLastMessage(getString(R.string.sent_audio)));
    }

    // ==================================================================
    // Reply & Edit & Delete
    // ==================================================================

    private void setReplyTo(Message message) {
        replyToMessage = message;
        binding.layoutReply.layoutReply.setVisibility(View.VISIBLE);
        binding.layoutReply.tvReplyText.setText(
                message.getText() != null ? message.getText() :
                        (message.getImageUrl() != null ? getString(R.string.photo) :
                                (message.getVideoUrl() != null ? getString(R.string.video) : "")));
        binding.layoutReply.tvReplySender.setText(message.getSenderName());
        binding.etMessage.requestFocus();
    }

    private void clearReply() {
        replyToMessage = null;
        binding.layoutReply.layoutReply.setVisibility(View.GONE);
    }

    private void setEditMode(Message message) {
        if (!currentUid.equals(message.getSenderId())) return;
        editingMessage = message;
        binding.etMessage.setText(message.getText());
        binding.etMessage.setSelection(message.getText().length());
        if (binding.layoutEdit != null) {
            binding.layoutEdit.layoutEdit.setVisibility(View.VISIBLE);
        }
        binding.etMessage.requestFocus();
    }

    private void clearEdit() {
        editingMessage = null;
        binding.etMessage.setText("");
        if (binding.layoutEdit != null) {
            binding.layoutEdit.layoutEdit.setVisibility(View.GONE);
        }
    }

    private void sendEditedMessage() {
        if (editingMessage == null || chatId == null) return;
        String newText = binding.etMessage.getText().toString().trim();
        if (newText.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("text", newText);
        updates.put("isEdited", true);

        rootRef.child(Constants.MESSAGES).child(chatId)
                .child(editingMessage.getMessageId())
                .updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    clearEdit();
                    showMessage(getString(R.string.message_edited));
                })
                .addOnFailureListener(e -> showError(getString(R.string.error_generic)));
    }

    private void deleteMessage(Message message) {
        if (!currentUid.equals(message.getSenderId())) return;
        if (chatId == null || message.getMessageId() == null) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_message_title)
                .setMessage(R.string.delete_message_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("isDeleted", true);
                    updates.put("text", "");
                    updates.put("imageUrl", "");
                    updates.put("videoUrl", "");
                    updates.put("fileUrl", "");
                    updates.put("audioUrl", "");
                    updates.put("gifUrl", "");

                    rootRef.child(Constants.MESSAGES).child(chatId)
                            .child(message.getMessageId())
                            .updateChildren(updates)
                            .addOnSuccessListener(aVoid ->
                                    showMessage(getString(R.string.message_deleted)))
                            .addOnFailureListener(e ->
                                    showError(getString(R.string.error_generic)));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ==================================================================
    // Update Chat
    // ==================================================================

    private void updateChatLastMessage(String lastMessage) {
        if (chatId == null) return;
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastMessage", lastMessage);
        updates.put("lastMessageTime", System.currentTimeMillis());
        updates.put("lastMessageSenderName", currentUser != null ? currentUser.getName() : "");

        rootRef.child(Constants.CHATS).child(chatId).updateChildren(updates)
                .addOnFailureListener(e -> Log.e(TAG, "updateChatLastMessage failed", e));
    }

    // ==================================================================
    // Read Receipts
    // ==================================================================

    private void markMessagesAsRead() {
        if (chatId == null) return;
        rootRef.child(Constants.MESSAGES).child(chatId)
                .orderByChild("senderId")
                .equalTo(otherUserId != null ? otherUserId : "")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Object> updates = new HashMap<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Message msg = snap.getValue(Message.class);
                            if (msg != null && !msg.isRead()) {
                                updates.put(snap.getKey() + "/isRead", true);
                            }
                        }
                        if (!updates.isEmpty()) {
                            rootRef.child(Constants.MESSAGES).child(chatId)
                                    .updateChildren(updates);
                        }
                        // Reset unread count
                        rootRef.child(Constants.CHATS).child(chatId)
                                .child("unreadCount").setValue(0);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "markMessagesAsRead cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Typing Indicator
    // ==================================================================

    private void setTypingStatus(boolean isTyping) {
        if (chatId == null || currentUid == null) return;
        rootRef.child(Constants.CHATS).child(chatId)
                .child("typing").child(currentUid)
                .setValue(isTyping);
    }

    private void listenForTyping() {
        if (chatId == null || (isGroup && otherUserId == null)) return;
        String listenUid = isGroup ? null : otherUserId;
        if (listenUid == null) return;

        typingListener = rootRef.child(Constants.CHATS).child(chatId)
                .child("typing").child(listenUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isTyping = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        binding.tvTypingStatus.setVisibility(isTyping ? View.VISIBLE : View.GONE);
                        if (isTyping) {
                            binding.tvTypingStatus.setText(R.string.typing);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "listenForTyping cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Online Status
    // ==================================================================

    private void listenForOnlineStatus() {
        if (isGroup || otherUserId == null) return;

        onlineStatusListener = rootRef.child(Constants.USERS).child(otherUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                        Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);

                        if (Boolean.TRUE.equals(isOnline)) {
                            binding.tvOnlineStatus.setText(R.string.online);
                            binding.tvOnlineStatus.setTextColor(getColor(R.color.colorOnline));
                            binding.ivOnlineDot.setVisibility(View.VISIBLE);
                        } else {
                            binding.tvOnlineStatus.setText(
                                    lastSeen != null ? DateUtils.formatRelativeTimeArabic(lastSeen) : "");
                            binding.tvOnlineStatus.setTextColor(getColor(R.color.colorTextSecondary));
                            binding.ivOnlineDot.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "listenForOnlineStatus cancelled", error.toException());
                    }
                });
    }

    // ==================================================================
    // Attachment Bottom Sheet
    // ==================================================================

    private void showAttachmentBottomSheet() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_attachment, null);
        bottomSheet.setContentView(view);

        view.findViewById(R.id.layoutImage).setOnClickListener(v -> {
            bottomSheet.dismiss();
            pickImage();
        });

        view.findViewById(R.id.layoutVideo).setOnClickListener(v -> {
            bottomSheet.dismiss();
            pickVideo();
        });

        view.findViewById(R.id.layoutFile).setOnClickListener(v -> {
            bottomSheet.dismiss();
            pickFile();
        });

        view.findViewById(R.id.layoutLocation).setOnClickListener(v -> {
            bottomSheet.dismiss();
            requestLocationAndSend();
        });

        view.findViewById(R.id.layoutContact).setOnClickListener(v -> {
            bottomSheet.dismiss();
            pickContact();
        });

        view.findViewById(R.id.layoutGif).setOnClickListener(v -> {
            bottomSheet.dismiss();
            showMessage(getString(R.string.gif_coming_soon));
        });

        bottomSheet.show();
    }

    // ==================================================================
    // Pickers
    // ==================================================================

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    private void pickVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, REQUEST_VIDEO);
    }

    private void pickFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST_FILE);
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(android.provider.ContactsContract.Contacts.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    private void requestLocationAndSend() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        sendLocationMessage(location.getLatitude(), location.getLongitude());
                    } else {
                        showError(getString(R.string.error_getting_location));
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                showMessage(getString(R.string.audio_permission_denied));
            }
        } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestLocationAndSend();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_IMAGE:
                    sendImageMessage(data.getData());
                    break;
                case REQUEST_VIDEO:
                    sendVideoMessage(data.getData());
                    break;
                case REQUEST_FILE:
                    String fileName = "file";
                    if (data.getData() != null) {
                        String path = data.getData().getPath();
                        if (path != null) {
                            int lastSlash = path.lastIndexOf('/');
                            if (lastSlash >= 0) fileName = path.substring(lastSlash + 1);
                        }
                    }
                    sendFileMessage(data.getData(), fileName);
                    break;
            }
        }
    }

    // ==================================================================
    // Menu Actions
    // ==================================================================

    private void showBlockDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.block_user_title)
                .setMessage(R.string.block_user_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> blockUser())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void blockUser() {
        if (otherUserId == null) return;
        Map<String, Object> blockData = new HashMap<>();
        blockData.put("blockedUid", otherUserId);
        blockData.put("blockedBy", currentUid);
        blockData.put("timestamp", System.currentTimeMillis());

        rootRef.child(Constants.BLOCKS).child(currentUid).child(otherUserId).setValue(blockData)
                .addOnSuccessListener(aVoid -> {
                    showMessage(getString(R.string.user_blocked));
                    finish();
                })
                .addOnFailureListener(e -> showError(getString(R.string.error_generic)));
    }

    private void toggleMuteChat() {
        if (chatId == null) return;
        rootRef.child(Constants.CHATS).child(chatId).child("isMuted")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean currentMuted = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        rootRef.child(Constants.CHATS).child(chatId)
                                .child("isMuted").setValue(!currentMuted)
                                .addOnSuccessListener(aVoid ->
                                        showMessage(!currentMuted ?
                                                getString(R.string.chat_muted) :
                                                getString(R.string.chat_unmuted)));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "toggleMuteChat cancelled", error.toException());
                    }
                });
    }

    private void showClearChatDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_chat_title)
                .setMessage(R.string.clear_chat_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    if (chatId != null) {
                        rootRef.child(Constants.MESSAGES).child(chatId).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    messageList.clear();
                                    messageAdapter.notifyDataSetChanged();
                                    showMessage(getString(R.string.chat_cleared));
                                });
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // ==================================================================
    // Message Adapter
    // ==================================================================

    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        private static final int TYPE_SENT = 1;
        private static final int TYPE_RECEIVED = 2;

        @Override
        public int getItemViewType(int position) {
            Message message = messageList.get(position);
            return currentUid.equals(message.getSenderId()) ? TYPE_SENT : TYPE_RECEIVED;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            int layout = viewType == TYPE_SENT ? R.layout.item_message_sent : R.layout.item_message_received;
            View view = inflater.inflate(layout, parent, false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = messageList.get(position);
            holder.bind(message);
        }

        @Override
        public int getItemCount() {
            return messageList.size();
        }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPhoto, ivMessageImage, ivMessageVideo, ivMessageFile, ivMessagePlay;
            TextView tvMessage, tvTime, tvSenderName, tvReadStatus;
            TextView tvFileName, tvReplyText, tvReplySender;
            View layoutReply, layoutImage, layoutVideo, layoutFile, layoutLocation, layoutAudio;
            TextView tvLocation, tvAudioDuration;

            MessageViewHolder(View itemView) {
                super(itemView);
                ivPhoto = itemView.findViewById(R.id.ivPhoto);
                ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
                ivMessageVideo = itemView.findViewById(R.id.ivMessageVideo);
                ivMessageFile = itemView.findViewById(R.id.ivMessageFile);
                ivMessagePlay = itemView.findViewById(R.id.ivMessagePlay);
                tvMessage = itemView.findViewById(R.id.tvMessage);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvSenderName = itemView.findViewById(R.id.tvSenderName);
                tvReadStatus = itemView.findViewById(R.id.tvReadStatus);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvReplyText = itemView.findViewById(R.id.tvReplyText);
                tvReplySender = itemView.findViewById(R.id.tvReplySender);
                layoutReply = itemView.findViewById(R.id.layoutReply);
                layoutImage = itemView.findViewById(R.id.layoutImage);
                layoutVideo = itemView.findViewById(R.id.layoutVideo);
                layoutFile = itemView.findViewById(R.id.layoutFile);
                layoutLocation = itemView.findViewById(R.id.layoutLocation);
                layoutAudio = itemView.findViewById(R.id.layoutAudio);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                tvAudioDuration = itemView.findViewById(R.id.tvAudioDuration);
            }

            void bind(Message message) {
                // Deleted message
                if (message.isDeleted()) {
                    tvMessage.setText(R.string.message_deleted);
                    tvMessage.setAlpha(0.5f);
                } else {
                    tvMessage.setText(message.getText());
                    tvMessage.setAlpha(1.0f);
                }

                // Time
                tvTime.setText(getRelativeTime(message.getTimestamp()));

                // Sender name for groups
                if (isGroup) {
                    tvSenderName.setVisibility(View.VISIBLE);
                    tvSenderName.setText(message.getSenderName());
                } else {
                    tvSenderName.setVisibility(View.GONE);
                }

                // Read status
                if (currentUid.equals(message.getSenderId())) {
                    if (tvReadStatus != null) {
                        tvReadStatus.setVisibility(View.VISIBLE);
                        tvReadStatus.setText(message.isRead() ? R.string.read : R.string.sent);
                    }
                }

                // Edited indicator
                if (message.isEdited() && tvMessage != null) {
                    // Could append edited indicator
                }

                // Photo
                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    layoutImage.setVisibility(View.VISIBLE);
                    loadImage(message.getImageUrl(), ivMessageImage);
                    ivMessageImage.setOnClickListener(v -> {
                        // Open image viewer
                    });
                } else {
                    layoutImage.setVisibility(View.GONE);
                }

                // Video
                if (message.getVideoUrl() != null && !message.getVideoUrl().isEmpty()) {
                    layoutVideo.setVisibility(View.VISIBLE);
                    // Load video thumbnail
                } else {
                    layoutVideo.setVisibility(View.GONE);
                }

                // File
                if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
                    layoutFile.setVisibility(View.VISIBLE);
                    tvFileName.setText(message.getFileName() != null ? message.getFileName() : "file");
                } else {
                    layoutFile.setVisibility(View.GONE);
                }

                // Audio
                if (message.getAudioUrl() != null && !message.getAudioUrl().isEmpty()) {
                    layoutAudio.setVisibility(View.VISIBLE);
                    tvAudioDuration.setText(String.format(Locale.getDefault(), "%02d:%02d",
                            message.getDuration() / 60, message.getDuration() % 60));
                } else {
                    layoutAudio.setVisibility(View.GONE);
                }

                // Location
                if (message.getLocation() != null && !message.getLocation().isEmpty()) {
                    layoutLocation.setVisibility(View.VISIBLE);
                    Double lat = message.getLocation().get("latitude");
                    Double lng = message.getLocation().get("longitude");
                    if (lat != null && lng != null) {
                        tvLocation.setText(String.format(Locale.getDefault(),
                                "%.4f, %.4f", lat, lng));
                    }
                } else {
                    layoutLocation.setVisibility(View.GONE);
                }

                // Reply preview
                if (message.getReplyToText() != null && !message.getReplyToText().isEmpty()) {
                    layoutReply.setVisibility(View.VISIBLE);
                    tvReplyText.setText(message.getReplyToText());
                    tvReplySender.setText(message.getReplyToMessageId());
                } else {
                    layoutReply.setVisibility(View.GONE);
                }

                // Long press for options
                itemView.setOnLongClickListener(v -> {
                    if (currentUid.equals(message.getSenderId()) && !message.isDeleted()) {
                        PopupMenu popup = new PopupMenu(ChatActivity.this, itemView);
                        popup.getMenu().add(0, 1, 0, R.string.reply);
                        popup.getMenu().add(0, 2, 1, R.string.edit);
                        popup.getMenu().add(0, 3, 2, R.string.delete);
                        popup.setOnMenuItemClickListener(item -> {
                            int id = item.getItemId();
                            if (id == 1) {
                                setReplyTo(message);
                            } else if (id == 2) {
                                setEditMode(message);
                            } else if (id == 3) {
                                deleteMessage(message);
                            }
                            return true;
                        });
                        popup.show();
                    } else {
                        setReplyTo(message);
                    }
                    return true;
                });

                // Click to reply
                itemView.setOnClickListener(v -> {
                    if (replyToMessage == null) {
                        setReplyTo(message);
                    }
                });

                // Load sender avatar for received messages
                if (!currentUid.equals(message.getSenderId())) {
                    loadCircularImage(message.getSenderPhoto(), ivPhoto);
                }
            }
        }
    }

    // ==================================================================
    // Lifecycle
    // ==================================================================

    @Override
    protected void onResume() {
        super.onResume();
        // Set user online
        if (currentUid != null) {
            rootRef.child(Constants.USERS).child(currentUid).child("isOnline").setValue(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Set user offline
        if (currentUid != null) {
            rootRef.child(Constants.USERS).child(currentUid).child("isOnline").setValue(false);
            rootRef.child(Constants.USERS).child(currentUid).child("lastSeen")
                    .setValue(System.currentTimeMillis());
        }
        // Clear typing
        setTypingStatus(false);
        // Stop recording if active
        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        if (messageChildListener != null && activeMessagesQuery != null) {
            activeMessagesQuery.removeEventListener(messageChildListener);
        }
        if (onlineStatusListener != null && otherUserId != null) {
            rootRef.child(Constants.USERS).child(otherUserId).removeEventListener(onlineStatusListener);
        }
        if (typingListener != null && chatId != null && otherUserId != null) {
            rootRef.child(Constants.CHATS).child(chatId).child("typing").child(otherUserId)
                    .removeEventListener(typingListener);
        }
        recordingHandler.removeCallbacks(recordingTimerRunnable);
        typingHandler.removeCallbacks(typingRunnable);
        super.onDestroy();
    }
}
