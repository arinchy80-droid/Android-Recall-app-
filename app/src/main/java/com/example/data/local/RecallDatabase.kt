package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.converter.RoomConverters
import com.example.data.local.dao.ActionItemDao
import com.example.data.local.dao.CommitmentDao
import com.example.data.local.dao.MediaDao
import com.example.data.local.dao.MemoryLinkDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.NoteRevisionDao
import com.example.data.local.dao.PersonDao
import com.example.data.local.dao.PlaceDao
import com.example.data.local.dao.ProjectDao
import com.example.data.local.dao.ReminderDao
import com.example.data.local.entity.ActionItemEntity
import com.example.data.local.entity.CommitmentEntity
import com.example.data.local.entity.MediaEntity
import com.example.data.local.entity.MemoryLinkEntity
import com.example.data.local.entity.NoteEntity
import com.example.data.local.entity.NotePersonCrossRef
import com.example.data.local.entity.NoteRevisionEntity
import com.example.data.local.entity.PersonEntity
import com.example.data.local.entity.PlaceEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ReminderEntity

@Database(
    entities = [
        NoteEntity::class,
        PersonEntity::class,
        NotePersonCrossRef::class,
        MediaEntity::class,
        ActionItemEntity::class,
        ReminderEntity::class,
        PlaceEntity::class,
        ProjectEntity::class,
        CommitmentEntity::class,
        MemoryLinkEntity::class,
        NoteRevisionEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class RecallDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun personDao(): PersonDao
    abstract fun mediaDao(): MediaDao
    abstract fun actionItemDao(): ActionItemDao
    abstract fun reminderDao(): ReminderDao
    abstract fun placeDao(): PlaceDao
    abstract fun projectDao(): ProjectDao
    abstract fun commitmentDao(): CommitmentDao
    abstract fun memoryLinkDao(): MemoryLinkDao
    abstract fun noteRevisionDao(): NoteRevisionDao

    companion object {
        @Volatile
        private var INSTANCE: RecallDatabase? = null

        /**
         * Non-destructive migration from Room v4 to v5:
         * - Adds Unified Memory System metadata columns to `notes`
         * - Creates `places`, `projects`, `commitments`, `memory_links`, and `note_revisions` tables
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Extend existing notes table with unified memory metadata columns
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `memoryType` TEXT NOT NULL DEFAULT 'PERSONAL_OBSERVATIONS'")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `occurredAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE `notes` SET `occurredAt` = `createdAt` WHERE `occurredAt` = 0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `sourceType` TEXT NOT NULL DEFAULT 'USER_EXPLICIT'")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `confidenceScore` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `verificationStatus` TEXT NOT NULL DEFAULT 'VERIFIED_FACT'")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `projectId` TEXT")
                db.execSQL("ALTER TABLE `notes` ADD COLUMN `placeId` TEXT")

                // 2. Create places table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `places` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `address` TEXT,
                        `latitude` REAL,
                        `longitude` REAL,
                        `notes` TEXT NOT NULL,
                        `visitCount` INTEGER NOT NULL,
                        `lastVisitedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_places_name` ON `places` (`name`)")

                // 3. Create projects table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `projects` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `tagsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_name` ON `projects` (`name`)")

                // 4. Create commitments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `commitments` (
                        `id` TEXT NOT NULL,
                        `noteId` TEXT,
                        `personId` TEXT,
                        `projectId` TEXT,
                        `description` TEXT NOT NULL,
                        `direction` TEXT NOT NULL,
                        `dueDate` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `confidenceScore` REAL NOT NULL,
                        `verificationStatus` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commitments_noteId` ON `commitments` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commitments_personId` ON `commitments` (`personId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_commitments_projectId` ON `commitments` (`projectId`)")

                // 5. Create memory_links table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `memory_links` (
                        `id` TEXT NOT NULL,
                        `sourceNoteId` TEXT NOT NULL,
                        `targetNoteId` TEXT NOT NULL,
                        `relationType` TEXT NOT NULL,
                        `strengthScore` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_links_sourceNoteId` ON `memory_links` (`sourceNoteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_links_targetNoteId` ON `memory_links` (`targetNoteId`)")

                // 6. Create note_revisions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `note_revisions` (
                        `id` TEXT NOT NULL,
                        `noteId` TEXT NOT NULL,
                        `previousTitle` TEXT NOT NULL,
                        `previousContent` TEXT NOT NULL,
                        `changeSummary` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_revisions_noteId` ON `note_revisions` (`noteId`)")
            }
        }

        fun getInstance(context: Context): RecallDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RecallDatabase::class.java,
                    "recall_memory.db"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
