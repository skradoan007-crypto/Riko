package com.rikodev.sync.nexmesh.core.vdfs.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.rikodev.sync.nexmesh.core.vdfs.dao.FileChunkDao
import com.rikodev.sync.nexmesh.core.vdfs.dao.SwarmNodeDao
import com.rikodev.sync.nexmesh.core.vdfs.dao.VirtualFileDao
import com.rikodev.sync.nexmesh.core.vdfs.entity.FileChunkEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.SwarmNodeEntity
import com.rikodev.sync.nexmesh.core.vdfs.entity.VirtualFileEntity

@Database(
    entities = [
        SwarmNodeEntity::class,
        VirtualFileEntity::class,
        FileChunkEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VdfsDatabase : RoomDatabase() {
    abstract fun swarmNodeDao(): SwarmNodeDao
    abstract fun virtualFileDao(): VirtualFileDao
    abstract fun fileChunkDao(): FileChunkDao

    companion object {
        @Volatile
        private var instance: VdfsDatabase? = null

        fun getInstance(context: Context): VdfsDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    VdfsDatabase::class.java,
                    "nexmesh_vdfs.db"
                ).build().also { instance = it }
            }
        }
    }
}
