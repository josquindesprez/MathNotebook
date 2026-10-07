package it.lectio.bibbia.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TranslationEntity::class,
        BookEntity::class,
        VerseEntity::class,
        VerseFtsEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BibleDatabase : RoomDatabase() {
    abstract fun translationDao(): TranslationDao
    abstract fun bookDao(): BookDao
    abstract fun verseDao(): VerseDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao

    companion object {
        const val NAME = "bibbia.db"

        fun create(context: Context): BibleDatabase =
            Room.databaseBuilder(context.applicationContext, BibleDatabase::class.java, NAME)
                .build()
    }
}
