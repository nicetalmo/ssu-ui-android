package com.example.ssuui.map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ssuui.R;

import java.util.ArrayList;
import java.util.List;

public class StoreListAdapter extends RecyclerView.Adapter<StoreListAdapter.Holder> {

    static final int BUSY_FROM_TEAMS = 6; // 이 팀 수부터 주황 태그

    public static final class Row {
        public final MapStore store;
        public final int walkMinutes;

        public Row(@NonNull MapStore store, int walkMinutes) {
            this.store = store;
            this.walkMinutes = walkMinutes;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void submit(@NonNull List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_map_store, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView meta;
        private final TextView tag;

        Holder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.row_name);
            meta = itemView.findViewById(R.id.row_meta);
            tag = itemView.findViewById(R.id.row_tag);
        }

        void bind(@NonNull Row row) {
            Context context = itemView.getContext();
            MapStore store = row.store;

            name.setText(store.getName());
            meta.setText(context.getString(R.string.map_row_meta,
                    row.walkMinutes, store.getMinutesPerTeam()));

            if (!store.acceptsQueue()) {
                tag.setText(R.string.map_tag_paused);
                paintTag(R.color.surface_container, R.color.outline, R.color.on_surface_variant);
                return;
            }

            int teams = store.getWaitingCount();
            tag.setText(context.getString(R.string.map_tag_waiting, teams));
            if (teams == 0) {
                paintTag(R.color.success_container, R.color.success, R.color.on_success_container);
            } else if (teams < BUSY_FROM_TEAMS) {
                paintTag(R.color.primary_container, R.color.primary, R.color.on_primary_container);
            } else {
                paintTag(R.color.accent_container, R.color.accent, R.color.on_accent_container);
            }
        }

        private void paintTag(@ColorRes int background, @ColorRes int dot, @ColorRes int text) {
            Context context = itemView.getContext();
            tag.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, background)));
            tag.setCompoundDrawableTintList(ColorStateList.valueOf(ContextCompat.getColor(context, dot)));
            tag.setTextColor(ContextCompat.getColor(context, text));
        }
    }
}
