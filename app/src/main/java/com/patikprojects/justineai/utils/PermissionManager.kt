package com.patikprojects.justineai.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

object PermissionManager {
    private const val TAG = "PermissionManager"
    const val RECORD_AUDIO_PERMISSION_CODE = 1001

    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun checkAndRequestPermissions(
        activity: Activity,
        onPermissionsGranted: () -> Unit
    ) {
        if (!hasAudioPermission(activity)) {
            showPermissionDialog(
                activity,
                "Microphone Permission Required",
                "JustineAI needs microphone access to listen for voice commands.",
                "Grant Permission"
            ) {
                requestAudioPermission(activity)
            }
        } else {
            Log.i(TAG, "Audio permission granted")

//            promptToSetDefaultAssistant(activity)

            onPermissionsGranted()
        }
    }


    private fun showPermissionDialog(
        activity: Activity,
        title: String,
        message: String,
        positiveButton: String,
        onPositiveClick: () -> Unit
    ) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButton) { _, _ -> onPositiveClick() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }

    private fun requestAudioPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_CODE
        )
    }

    fun handlePermissionResult(
        activity: Activity,
        requestCode: Int,
        grantResults: IntArray,
        onAllPermissionsGranted: () -> Unit
    ) {
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Audio permission granted")
                onAllPermissionsGranted()
            } else {
                showPermissionDeniedDialog(
                    activity,
                    "Microphone permission is required for Justine to function."
                )
            }
        }
    }

    private fun showPermissionDeniedDialog(activity: Activity, message: String) {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage(message)
            .setPositiveButton("Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            }
            .setNegativeButton("Continue") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    fun promptToSetDefaultAssistant(activity: Activity) {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activity.startActivity(intent)
    }
}

