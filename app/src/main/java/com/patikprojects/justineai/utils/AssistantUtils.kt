package com.patikprojects.justineai.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AssistantUtils {
    fun isJustineDefaultAssistant(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
            roleManager?.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT) == true &&
                    getDefaultAssistantPackage(context) == context.packageName
        } else {
            getDefaultAssistantPackage(context) == context.packageName
        }
    }

    private fun getDefaultAssistantPackage(context: Context): String? {
        val intent = android.content.Intent(android.content.Intent.ACTION_ASSIST)
        val resolveInfo = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }
}