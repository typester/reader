package org.unknownplace.manga

import android.content.Context
import android.net.Uri
import uniffi.manga.Config
import uniffi.manga.Manga
import java.io.File

class Shared private constructor() {
    companion object {
        @Volatile
        private var obj: Manga? = null

        fun instance() : Manga {
            if (obj == null) {
                synchronized(this) {
                    if (obj == null) {
                        val databaseFile = File(SharedContext.context().filesDir, "database.db")
                        if (!databaseFile.exists()) {
                            databaseFile.createNewFile()
                        }
                        val databaseUri = Uri.fromFile(databaseFile)
                        val config = Config(
                            databaseUrl = "sqlite://" + databaseUri.path
                        )
                        obj = Manga(config)
                    }
                }
            }
            return obj!!
        }
    }
}

class SharedContext private constructor() {
    companion object {
        private var context: Context? = null

        fun context() : Context {
            return context!!
        }

        fun setContext(context: Context) {
            this.context = context
        }
    }
}