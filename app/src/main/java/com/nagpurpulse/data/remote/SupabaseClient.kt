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

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        httpEngine = OkHttp.create {
            addInterceptor(loggingInterceptor)
        }

        install(Auth) {
            autoLoadFromStorage = true
            autoSaveToStorage = true
        }

        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}