//java/com/nagpurpulse/data/remote/SupabaseClient.kt

package com.nagpurpulse.data.remote


import okhttp3.logging.HttpLoggingInterceptor
import com.nagpurpulse.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp

object SupabaseClientProvider {

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {

        httpEngine = OkHttp.create {
            // Avoid logging private request/response bodies in production.
            // Basic request/response metadata is useful only during debug builds.
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }

        install(Auth) {
            autoLoadFromStorage = true
            autoSaveToStorage = true
            // Used by Supabase OTP/recovery callbacks on Android.
            scheme = "nagpurpulse"
            host = "auth"
        }

        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}