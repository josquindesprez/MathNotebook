package it.lectio.bibbia.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import it.lectio.bibbia.data.repository.TranslationRepository
import it.lectio.bibbia.data.source.DefaultTranslationSourceFactory
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class RealAssetInstallTest {
    @Test
    fun `installa il testo reale dagli assets`() = runBlocking {
        val db = inMemoryDatabase()
        val repo = TranslationRepository(db, DefaultTranslationSourceFactory(ApplicationProvider.getApplicationContext<Context>()))
        val result = repo.install("riveduta")
        result.exceptionOrNull()?.printStackTrace()
        assertThat(result.getOrNull()).isEqualTo(31_102)
        db.close()
    }
}
