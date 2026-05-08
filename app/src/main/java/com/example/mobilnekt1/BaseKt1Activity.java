package com.example.mobilnekt1;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;

public abstract class BaseKt1Activity extends Activity {
    protected boolean isBlank(EditText editText) {
        return editText.getText().toString().trim().isEmpty();
    }

    protected void showToast(int messageResId) {
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    protected void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    protected void showFinishDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                .setOnCancelListener(dialog -> finish())
                .show();
    }
}
