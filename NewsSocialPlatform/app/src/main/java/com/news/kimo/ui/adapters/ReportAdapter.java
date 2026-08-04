package com.news.kimo.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.news.kimo.R;
import com.news.kimo.databinding.ItemReportBinding;
import com.news.kimo.models.Report;
import com.news.kimo.ui.activities.PostDetailsActivity;
import com.news.kimo.ui.activities.ProfileActivity;
import com.news.kimo.utils.Constants;
import com.news.kimo.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private final Context context;
    private final List<Report> reportList;

    public interface OnReportActionListener {
        void onReviewClick(Report report, int position);
        void onDismissClick(Report report, int position);
        void onReportClick(Report report);
    }

    private OnReportActionListener onReportActionListener;

    public ReportAdapter(Context context, List<Report> reportList) {
        this.context = context;
        this.reportList = reportList != null ? reportList : new ArrayList<>();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemReportBinding binding = ItemReportBinding.inflate(inflater, parent, false);
        return new ReportViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reportList.get(position), position);
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    @Override
    public long getItemId(int position) {
        if (position < reportList.size() && reportList.get(position).getReportId() != null) {
            return reportList.get(position).getReportId().hashCode();
        }
        return RecyclerView.NO_ID;
    }

    // ---- ViewHolder ----

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ItemReportBinding binding;

        ReportViewHolder(ItemReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Report report, int position) {
            // Type badge
            String typeLabel = getTypeLabel(report.getType());
            binding.tvTypeBadge.setText(typeLabel);
            binding.tvTypeBadge.setBackgroundTintList(
                    ContextCompat.getColorStateList(context, getTypeColor(report.getType())));

            // Reporter
            binding.tvReporterName.setText(report.getReporterName() != null ? report.getReporterName() : "مجهول");

            // Reason
            String reasonLabel = getReasonLabel(report.getReason());
            binding.tvReason.setText(reasonLabel);

            // Description
            if (report.getDescription() != null && !report.getDescription().isEmpty()) {
                binding.tvDescription.setVisibility(View.VISIBLE);
                binding.tvDescription.setText(report.getDescription());
                binding.tvDescription.setMaxLines(3);
            } else {
                binding.tvDescription.setVisibility(View.GONE);
            }

            // Date
            binding.tvReportDate.setText(DateUtils.formatDateTime(report.getTimestamp()));

            // Status badge (color coded)
            setStatusBadge(report.getStatus());

            // Review button (only for pending reports)
            if ("pending".equals(report.getStatus())) {
                binding.btnReview.setVisibility(View.VISIBLE);
                binding.btnReview.setOnClickListener(v -> {
                    if (onReportActionListener != null) {
                        onReportActionListener.onReviewClick(report, position);
                    }
                });
            } else {
                binding.btnReview.setVisibility(View.GONE);
            }

            // Dismiss button
            if ("pending".equals(report.getStatus())) {
                binding.btnDismiss.setVisibility(View.VISIBLE);
                binding.btnDismiss.setOnClickListener(v -> {
                    if (onReportActionListener != null) {
                        onReportActionListener.onDismissClick(report, position);
                    }
                });
            } else {
                binding.btnDismiss.setVisibility(View.GONE);
            }

            // Click -> navigate to reported content
            itemView.setOnClickListener(v -> {
                navigateToReportedContent(report);
                if (onReportActionListener != null) {
                    onReportActionListener.onReportClick(report);
                }
            });
        }

        private String getTypeLabel(String type) {
            if (type == null) return context.getString(R.string.report_type_other);
            switch (type) {
                case "post": return context.getString(R.string.report_type_post);
                case "user": return context.getString(R.string.report_type_user);
                case "comment": return context.getString(R.string.report_type_comment);
                case "message": return context.getString(R.string.report_type_message);
                default: return context.getString(R.string.report_type_other);
            }
        }

        private int getTypeColor(String type) {
            if (type == null) return R.color.report_other;
            switch (type) {
                case "post": return R.color.report_post;
                case "user": return R.color.report_user;
                case "comment": return R.color.report_comment;
                case "message": return R.color.report_message;
                default: return R.color.report_other;
            }
        }

        private String getReasonLabel(String reason) {
            if (reason == null) return "";
            switch (reason) {
                case Constants.REPORT_SPAM: return context.getString(R.string.report_reason_spam);
                case Constants.REPORT_HARASSMENT: return context.getString(R.string.report_reason_harassment);
                case Constants.REPORT_HATE_SPEECH: return context.getString(R.string.report_reason_hate_speech);
                case Constants.REPORT_VIOLENCE: return context.getString(R.string.report_reason_violence);
                case Constants.REPORT_NUDITY: return context.getString(R.string.report_reason_nudity);
                case Constants.REPORT_FALSE_INFO: return context.getString(R.string.report_reason_false_info);
                case Constants.REPORT_COPYRIGHT: return context.getString(R.string.report_reason_copyright);
                case Constants.REPORT_OTHER:
                default: return context.getString(R.string.report_reason_other);
            }
        }

        private void setStatusBadge(String status) {
            if (status == null) {
                binding.tvStatusBadge.setText(R.string.status_pending);
                binding.tvStatusBadge.setBackgroundColor(context.getColor(R.color.status_pending));
                return;
            }
            switch (status) {
                case "pending":
                    binding.tvStatusBadge.setText(R.string.status_pending);
                    binding.tvStatusBadge.setBackgroundColor(context.getColor(R.color.status_pending));
                    break;
                case "reviewed":
                case "resolved":
                    binding.tvStatusBadge.setText(R.string.status_resolved);
                    binding.tvStatusBadge.setBackgroundColor(context.getColor(R.color.status_resolved));
                    break;
                case "dismissed":
                    binding.tvStatusBadge.setText(R.string.status_dismissed);
                    binding.tvStatusBadge.setBackgroundColor(context.getColor(R.color.status_dismissed));
                    break;
                case "action_taken":
                    binding.tvStatusBadge.setText(R.string.status_action_taken);
                    binding.tvStatusBadge.setBackgroundColor(context.getColor(R.color.status_action_taken));
                    break;
                default:
                    binding.tvStatusBadge.setText(R.string.status_pending);
                    binding.tvStatusBadge.setBackgroundColor(context.getColor(R.color.status_pending));
                    break;
            }
        }
    }

    // ---- Navigate to Reported Content ----

    private void navigateToReportedContent(Report report) {
        Intent intent = null;
        String type = report.getType();
        if (type == null) return;

        switch (type) {
            case "post":
                if (report.getReportedId() != null) {
                    intent = new Intent(context, PostDetailsActivity.class)
                            .putExtra(Constants.EXTRA_POST_ID, report.getReportedId());
                }
                break;
            case "user":
                if (report.getReportedId() != null) {
                    intent = new Intent(context, ProfileActivity.class)
                            .putExtra(Constants.EXTRA_USER_ID, report.getReportedId());
                }
                break;
            default:
                break;
        }
        if (intent != null) {
            context.startActivity(intent);
        }
    }

    // ---- Data Operations ----

    public void addReport(Report report) {
        reportList.add(0, report);
        notifyItemInserted(0);
    }

    public void addReports(List<Report> newReports) {
        int startPos = reportList.size();
        reportList.addAll(newReports);
        notifyItemRangeInserted(startPos, newReports.size());
    }

    public void updateReport(int position, Report report) {
        if (position >= 0 && position < reportList.size()) {
            reportList.set(position, report);
            notifyItemChanged(position);
        }
    }

    public void removeReport(int position) {
        if (position >= 0 && position < reportList.size()) {
            reportList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public Report getReport(int position) {
        if (position >= 0 && position < reportList.size()) {
            return reportList.get(position);
        }
        return null;
    }

    public List<Report> getReportList() {
        return reportList;
    }

    // ---- Setters ----

    public void setOnReportActionListener(OnReportActionListener listener) {
        this.onReportActionListener = listener;
    }
}