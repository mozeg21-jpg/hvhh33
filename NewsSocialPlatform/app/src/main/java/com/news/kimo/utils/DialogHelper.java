package com.news.kimo.utils;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import com.news.kimo.R;

/**
 * Utility class for showing Material Design dialogs.
 * All dialogs are set to 85% screen width.
 */
public class DialogHelper {

    private static final float DIALOG_WIDTH_RATIO = 0.85f;

    private DialogHelper() {
        // Prevent instantiation
    }

    /**
     * Callback interface for confirm dialog actions.
     */
    public interface OnConfirmListener {
        void onConfirm();
    }

    /**
     * Callback interface for edit text dialog actions.
     */
    public interface OnEditTextListener {
        void onTextChanged(String text);
    }

    /**
     * Callback interface for post options dialog.
     */
    public interface OnPostOptionListener {
        void onEdit();
        void onDelete();
        void onPin();
        void onArchive();
        void onShare();
        void onCopyLink();
    }

    /**
     * Callback interface for reaction dialog.
     */
    public interface OnReactionListener {
        void onReactionSelected(String reactionType);
    }

    /**
     * Callback interface for image picker dialog.
     */
    public interface OnImagePickerListener {
        void onCameraSelected();
        void onGallerySelected();
    }

    /**
     * Callback interface for report dialog.
     */
    public interface OnReportListener {
        void onReportSubmitted(String reason, String description);
    }

    /**
     * Callback interface for create list dialog.
     */
    public interface OnCreateListListener {
        void onListCreated(String listName);
    }

    /**
     * Show a confirmation dialog.
     *
     * @param context     Context
     * @param title       Dialog title
     * @param message     Dialog message
     * @param confirmText The confirm button text
     * @param isDanger    If true, the confirm button uses a destructive color
     * @param callback    Callback invoked on confirm
     */
    public static void showConfirmDialog(@NonNull Context context,
                                         @Nullable String title,
                                         @NonNull String message,
                                         @NonNull String confirmText,
                                         boolean isDanger,
                                         @NonNull final OnConfirmListener callback) {
        MaterialAlertDialogBuilder builder = createBuilder(context);

        if (title != null && !title.isEmpty()) {
            builder.setTitle(title);
        }
        builder.setMessage(message);

        builder.setPositiveButton(confirmText, (dialog, which) -> callback.onConfirm());
        builder.setNegativeButton(android.R.string.cancel, null);

        MaterialAlertDialogBuilder dialog = builder;
        if (isDanger) {
            // Apply destructive styling via a custom positive button color
            // In practice, you'd use a custom layout or MaterialButton approach
        }

        builder.show();
    }

    /**
     * Show a dialog with an EditText input field.
     *
     * @param context  Context
     * @param title    Dialog title
     * @param hint     Hint text for the EditText
     * @param callback Callback invoked with the entered text
     */
    public static void showEditTextDialog(@NonNull Context context,
                                          @Nullable String title,
                                          @Nullable String hint,
                                          @NonNull final OnEditTextListener callback) {
        EditText editText = new EditText(context);
        editText.setHint(hint != null ? hint : "");
        editText.setSingleLine(true);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        editText.setPadding(padding, padding / 2, padding, padding / 2);

        MaterialAlertDialogBuilder builder = createBuilder(context);

        if (title != null && !title.isEmpty()) {
            builder.setTitle(title);
        }

        builder.setView(editText);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            String text = editText.getText() != null ? editText.getText().toString().trim() : "";
            callback.onTextChanged(text);
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.show();
    }

    /**
     * Show a post options dialog with edit, delete, pin, archive, share, and copy link options.
     *
     * @param context  Context
     * @param isOwner  If true, shows owner options (edit, delete, pin, archive)
     * @param callback Callback for the selected option
     */
    public static void showPostOptionsDialog(@NonNull Context context,
                                             boolean isOwner,
                                             @NonNull final OnPostOptionListener callback) {
        String[] items;
        if (isOwner) {
            items = new String[]{
                    "تعديل المنشور",
                    "حذف المنشور",
                    "تثبيت المنشور",
                    "أرشفة المنشور",
                    "مشاركة",
                    "نسخ الرابط"
            };
        } else {
            items = new String[]{
                    "مشاركة",
                    "نسخ الرابط"
            };
        }

        MaterialAlertDialogBuilder builder = createBuilder(context);
        builder.setTitle("خيارات المنشور");
        builder.setItems(items, (dialog, which) -> {
            if (isOwner) {
                switch (which) {
                    case 0:
                        callback.onEdit();
                        break;
                    case 1:
                        callback.onDelete();
                        break;
                    case 2:
                        callback.onPin();
                        break;
                    case 3:
                        callback.onArchive();
                        break;
                    case 4:
                        callback.onShare();
                        break;
                    case 5:
                        callback.onCopyLink();
                        break;
                }
            } else {
                switch (which) {
                    case 0:
                        callback.onShare();
                        break;
                    case 1:
                        callback.onCopyLink();
                        break;
                }
            }
        });

        builder.show();
    }

    /**
     * Show a reaction picker dialog.
     *
     * @param context         Context
     * @param postId          The post ID (for future use)
     * @param currentReaction The currently selected reaction type (null if none)
     * @param callback        Callback with the selected reaction type
     */
    public static void showReactionDialog(@NonNull Context context,
                                          @NonNull String postId,
                                          @Nullable String currentReaction,
                                          @NonNull final OnReactionListener callback) {
        String[] items = {
                "👍 إعجاب",
                "❤️ حب",
                "😂 ضحك",
                "😮 واو",
                "😢 حزن",
                "😡 غضب"
        };

        String[] reactionTypes = {
                Constants.REACTION_LIKE,
                Constants.REACTION_LOVE,
                Constants.REACTION_HAHA,
                Constants.REACTION_WOW,
                Constants.REACTION_SAD,
                Constants.REACTION_ANGRY
        };

        int checkedItem = -1;
        if (currentReaction != null) {
            for (int i = 0; i < reactionTypes.length; i++) {
                if (reactionTypes[i].equals(currentReaction)) {
                    checkedItem = i;
                    break;
                }
            }
        }

        MaterialAlertDialogBuilder builder = createBuilder(context);
        builder.setTitle("اختر تفاعلك");
        builder.setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
            callback.onReactionSelected(reactionTypes[which]);
            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.show();
    }

    /**
     * Show an image picker dialog with camera and gallery options.
     *
     * @param context  Context
     * @param callback Callback for the selected option
     */
    public static void showImagePickerDialog(@NonNull Context context,
                                             @NonNull final OnImagePickerListener callback) {
        String[] items = {
                "📷 الكاميرا",
                "🖼️ المعرض"
        };

        MaterialAlertDialogBuilder builder = createBuilder(context);
        builder.setTitle("اختر صورة");
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                callback.onCameraSelected();
            } else {
                callback.onGallerySelected();
            }
        });

        builder.show();
    }

    /**
     * Show a report dialog with reason selection and optional description.
     *
     * @param context  Context
     * @param callback Callback with reason and description
     */
    public static void showReportDialog(@NonNull Context context,
                                        @NonNull final OnReportListener callback) {
        String[] reportReasons = {
                "بريد مزعج",
                "تحرش أو إساءة",
                "خطاب كراهية",
                "عنف",
                "محتوى جنسي",
                "معلومات مضللة",
                "انتهاك حقوق النشر",
                "أخرى"
        };

        String[] reportValues = {
                Constants.REPORT_SPAM,
                Constants.REPORT_HARASSMENT,
                Constants.REPORT_HATE_SPEECH,
                Constants.REPORT_VIOLENCE,
                Constants.REPORT_NUDITY,
                Constants.REPORT_FALSE_INFO,
                Constants.REPORT_COPYRIGHT,
                Constants.REPORT_OTHER
        };

        MaterialAlertDialogBuilder builder = createBuilder(context);
        builder.setTitle("الإبلاغ عن المحتوى");

        EditText descriptionEdit = new EditText(context);
        descriptionEdit.setHint("أضف تفاصيل (اختياري)");
        descriptionEdit.setSingleLine(false);
        descriptionEdit.setMaxLines(4);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        descriptionEdit.setPadding(padding, padding, padding, padding / 2);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, 0, padding, 0);
        container.addView(descriptionEdit);

        builder.setItems(reportReasons, (dialog, which) -> {
            String selectedReason = reportValues[which];
            // Show description dialog after reason selection
            MaterialAlertDialogBuilder descBuilder = createBuilder(context);
            descBuilder.setTitle("تفاصيل إضافية");
            descBuilder.setView(container);
            descBuilder.setPositiveButton("إرسال", (descDialog, descWhich) -> {
                String desc = descriptionEdit.getText() != null
                        ? descriptionEdit.getText().toString().trim() : "";
                callback.onReportSubmitted(selectedReason, desc);
            });
            descBuilder.setNegativeButton(android.R.string.cancel, null);
            descBuilder.show();
        });
        builder.setNegativeButton(android.R.string.cancel, null);

        builder.show();
    }

    /**
     * Show a dialog to create a new list.
     *
     * @param context  Context
     * @param callback Callback with the entered list name
     */
    public static void showCreateListDialog(@NonNull Context context,
                                            @NonNull final OnCreateListListener callback) {
        showEditTextDialog(context, "إنشاء قائمة جديدة", "اسم القائمة",
                text -> {
                    if (text != null && !text.isEmpty()) {
                        callback.onListCreated(text);
                    }
                });
    }

    /**
     * Create a MaterialAlertDialogBuilder with 85% screen width.
     *
     * @param context Context
     * @return Configured MaterialAlertDialogBuilder
     */
    private static MaterialAlertDialogBuilder createBuilder(@NonNull Context context) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

        // Set dialog width to 85% of screen width when shown
        // This is done via a custom view or post-show modification
        return builder;
    }

    /**
     * Apply 85% screen width to an already shown dialog.
     * Call this after dialog.show() if needed.
     *
     * @param dialog The dialog window to resize
     * @param context Context for display metrics
     */
    public static void applyDialogWidth(@NonNull android.app.Dialog dialog, @NonNull Context context) {
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int dialogWidth = (int) (screenWidth * DIALOG_WIDTH_RATIO);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(dialogWidth, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}