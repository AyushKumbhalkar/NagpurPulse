

//java/com/nagpurpulse/di/AppModule.kt

package com.nagpurpulse.di


import com.nagpurpulse.data.repository.AdminRepository
import com.nagpurpulse.data.location.LocationHelper
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.nagpurpulse.data.remote.weather.*
import android.content.Context
import com.nagpurpulse.data.local.SessionManager
import com.nagpurpulse.data.remote.SupabaseClientProvider
import com.nagpurpulse.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseClientProvider.client

    @Provides @Singleton
    fun provideSessionManager(@ApplicationContext ctx: Context): SessionManager =
        SessionManager(ctx)

    @Provides
    @Singleton
    fun provideLocationHelper(
        @ApplicationContext context: Context
    ): LocationHelper {
        return LocationHelper(context)
    }

    @Provides @Singleton
    fun provideAuthRepository(client: SupabaseClient): AuthRepository =
        AuthRepository(client)

    @Provides
    @Singleton
    fun providePostRepository(
        client: SupabaseClient,
        notificationRepository: NotificationRepository,
        authRepository: AuthRepository
    ): PostRepository =
        PostRepository(
            client,
            notificationRepository,
            authRepository
        )

    @Provides @Singleton
    fun provideAlertRepository(client: SupabaseClient): AlertRepository =
        AlertRepository(client)

    @Provides @Singleton
    fun provideProfileRepository(client: SupabaseClient): ProfileRepository =
        ProfileRepository(client)

    @Provides @Singleton
    fun provideNotificationRepository(
        client: SupabaseClient,
        authRepository: AuthRepository
    ): NotificationRepository = NotificationRepository(client, authRepository)

    @Provides @Singleton
    fun provideSavedPostsRepository(client: SupabaseClient): SavedPostsRepository =
        SavedPostsRepository(client)

    @Provides
    @Singleton
    fun provideAdminRepository(
        client: SupabaseClient
    ): AdminRepository {
        return AdminRepository(client)
    }

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideWeatherApi(
        retrofit: Retrofit
    ): WeatherApi {
        return retrofit.create(WeatherApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(
        api: WeatherApi
    ): WeatherRepository {
        return WeatherRepository(api)
    }

    @Provides @Singleton
    fun provideMessageRepository(
        client: SupabaseClient,
        authRepository: AuthRepository,
        profileRepository: ProfileRepository,
        notificationRepository: NotificationRepository
    ): MessageRepository = MessageRepository(
        client,
        authRepository,
        profileRepository,
        notificationRepository
    )

}
