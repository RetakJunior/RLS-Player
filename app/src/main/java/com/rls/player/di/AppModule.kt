package com.rls.player.di

import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.rls.player.core.data.local.*
import com.rls.player.core.data.repository.LocalCollectionRepository
import com.rls.player.core.data.repository.LocalMusicRepository
import com.rls.player.core.domain.repository.CollectionRepository
import com.rls.player.core.domain.repository.MusicRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.rlsDataStore: DataStore<Preferences> by preferencesDataStore(name = "rls_settings")
@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindMusicRepository(value: LocalMusicRepository): MusicRepository
    @Binds @Singleton abstract fun bindCollectionRepository(value: LocalCollectionRepository): CollectionRepository
}
@Module @InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context): RlsDatabase = Room.databaseBuilder(context, RlsDatabase::class.java, "rls.db").fallbackToDestructiveMigration().build()
    @Provides fun songDao(db: RlsDatabase) = db.songDao()
    @Provides fun favoriteDao(db: RlsDatabase) = db.favoriteDao()
    @Provides fun playlistDao(db: RlsDatabase) = db.playlistDao()
    @Provides fun historyDao(db: RlsDatabase) = db.historyDao()
    @Provides fun resolver(@ApplicationContext context: Context): ContentResolver = context.contentResolver
    @Provides @Singleton fun preferences(@ApplicationContext context: Context): DataStore<Preferences> = context.rlsDataStore
}
