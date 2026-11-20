package com.goner.app;

import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;

import com.goner.app.databinding.DialogConfirmBinding;

public class ConfirmDialog {
    public static void show(Context context, int title, int message, int okText, View.OnClickListener okListener) {
        DialogConfirmBinding binding = DialogConfirmBinding.inflate(LayoutInflater.from(context));

        binding.dialogtitle.setText(title);
        binding.dialogsub.setText(message);
        binding.dialogok.setText(okText);

        AlertDialog dialog = new AlertDialog.Builder(context).setView(binding.getRoot()).create();

        binding.dialogcancel.setOnClickListener(v -> dialog.dismiss());
        binding.dialogok.setOnClickListener(v -> {
            dialog.dismiss();
            okListener.onClick(v);
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
}