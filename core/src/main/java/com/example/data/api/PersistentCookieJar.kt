package com.example.data.api

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

open class PersistentCookieJar(context: Context? = null) : CookieJar {
    private val memoryCookies = mutableMapOf<String, String>()

    private val masterKey = context?.let {
        MasterKey.Builder(it)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences = if (context != null && masterKey != null) {
        try {
            EncryptedSharedPreferences.create(
                context,
                "chettinad_cookies",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            null
        }
    } else null

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val prefs = sharedPreferences
        if (prefs != null) {
            val editor = prefs.edit()
            for (cookie in cookies) {
                editor.putString(cookie.name, cookie.value)
            }
            editor.apply()
        } else {
            for (cookie in cookies) {
                memoryCookies[cookie.name] = cookie.value
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = mutableListOf<Cookie>()
        val prefs = sharedPreferences
        if (prefs != null) {
            for ((name, value) in prefs.all) {
                if (value is String) {
                    val cookie = Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(url.host)
                        .path("/")
                        .build()
                    cookies.add(cookie)
                }
            }
        } else {
            for ((name, value) in memoryCookies) {
                val cookie = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(url.host)
                    .path("/")
                    .build()
                cookies.add(cookie)
            }
        }
        return cookies
    }
    
    open fun clear() {
        sharedPreferences?.edit()?.clear()?.apply()
        memoryCookies.clear()
    }
}
