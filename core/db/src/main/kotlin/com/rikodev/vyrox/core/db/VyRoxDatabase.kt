package com.rikodev.vyrox.core.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE project_id = :projectId LIMIT 1")
    suspend fun getById(projectId: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE project_id = :projectId")
    suspend fun deleteById(projectId: String)
}

@Dao
interface MediaAssetDao {
    @Query("SELECT * FROM media_assets WHERE asset_id = :assetId LIMIT 1")
    suspend fun getById(assetId: String): MediaAssetEntity?

    @Query("SELECT * FROM media_assets WHERE content_uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): MediaAssetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MediaAssetEntity)
}

@Dao
interface ColorPresetDao {
    @Query("SELECT * FROM color_presets ORDER BY is_builtin DESC, updated_at DESC")
    fun observeAll(): Flow<List<ColorPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ColorPresetEntity)

    @Query("DELETE FROM color_presets WHERE preset_id = :presetId AND is_builtin = 0")
    suspend fun deleteCustom(presetId: String)
}

@Database(
    entities = [
        ProjectEntity::class,
        MediaAssetEntity::class,
        ColorPresetEntity::class,
        CacheEntryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VyRoxDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun colorPresetDao(): ColorPresetDao
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VyRoxDatabase =
        Room.databaseBuilder(context, VyRoxDatabase::class.java, "vyrox.db")
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideProjectDao(db: VyRoxDatabase): ProjectDao = db.projectDao()

    @Provides
    fun provideMediaAssetDao(db: VyRoxDatabase): MediaAssetDao = db.mediaAssetDao()

    @Provides
    fun provideColorPresetDao(db: VyRoxDatabase): ColorPresetDao = db.colorPresetDao()
}
