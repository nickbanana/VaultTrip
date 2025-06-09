package com.nickchi.vaulttrip.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.core.content.edit

object VaultPrefs {
    private const val PREF_NAME = "vault_prefs"
    private const val KEY_ROOT_URI = "vault_root_uri"
    private const val KEY_TEMPLATE_URI = "vault_template_uri"

    fun saveRootUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_ROOT_URI, uri.toString())
        }
    }

    fun getRootUri(context: Context): Uri? {
        val str = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ROOT_URI, null)
        return str?.toUri()
    }

    fun hasRootUri(context: Context): Boolean {
        return getRootUri(context) != null
    }

    fun clearRootUri(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_ROOT_URI)
        }
    }

    fun saveTemplateUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_TEMPLATE_URI, uri.toString())
        }
    }

    fun getTemplateUri(context: Context): Uri? {
        val str = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TEMPLATE_URI, null)
        return str?.toUri()
    }

    fun hasTemplateUri(context: Context): Boolean {
        return getTemplateUri(context) != null
    }

    fun clearTemplateUri(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_TEMPLATE_URI)
        }
    }
}