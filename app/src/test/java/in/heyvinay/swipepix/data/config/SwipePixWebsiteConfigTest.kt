package `in`.heyvinay.swipepix.data.config

import `in`.heyvinay.swipepix.ui.util.WebIntentUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.net.URI

@RunWith(JUnit4::class)
class SwipePixWebsiteConfigTest {

    @Test
    fun testBaseUrl_isCorrectAndHttps() {
        assertEquals("https://swipepix.heyvinay.in/", SwipePixWebsiteConfig.BASE_URL)
        assertTrue(SwipePixWebsiteConfig.BASE_URL.startsWith("https://"))
    }

    @Test
    fun testUpdatesUrl_isCorrectAndHttps() {
        assertEquals("https://swipepix.heyvinay.in/updates", SwipePixWebsiteConfig.UPDATES_URL)
        assertTrue(SwipePixWebsiteConfig.UPDATES_URL.startsWith("https://"))
    }

    @Test
    fun testDonateUrl_isCorrectAndHttps() {
        assertEquals("https://swipepix.heyvinay.in/donate", SwipePixWebsiteConfig.DONATE_URL)
        assertTrue(SwipePixWebsiteConfig.DONATE_URL.startsWith("https://"))
    }

    @Test
    fun testFeedbackUrl_isCorrectAndHttps() {
        assertEquals("https://swipepix.heyvinay.in/feedback", SwipePixWebsiteConfig.FEEDBACK_URL)
        assertTrue(SwipePixWebsiteConfig.FEEDBACK_URL.startsWith("https://"))
    }

    @Test
    fun testAllUrls_belongToOfficialDomainAndUseHttps() {
        val allUrls = listOf(
            SwipePixWebsiteConfig.BASE_URL,
            SwipePixWebsiteConfig.UPDATES_URL,
            SwipePixWebsiteConfig.DONATE_URL,
            SwipePixWebsiteConfig.FEEDBACK_URL,
        )

        for (urlString in allUrls) {
            val uri = URI.create(urlString)
            assertEquals("https", uri.scheme)
            assertEquals("swipepix.heyvinay.in", uri.host)
        }
    }

    @Test
    fun testWebIntentUtils_isValidHttpsUrl_validatesHttpsCorrectly() {
        assertTrue(WebIntentUtils.isValidHttpsUrl("https://swipepix.heyvinay.in/"))
        assertTrue(WebIntentUtils.isValidHttpsUrl("https://swipepix.heyvinay.in/updates"))
        assertTrue(WebIntentUtils.isValidHttpsUrl("https://swipepix.heyvinay.in/donate"))
        assertTrue(WebIntentUtils.isValidHttpsUrl("https://swipepix.heyvinay.in/feedback"))
        assertTrue(WebIntentUtils.isValidHttpsUrl("HTTPS://SWIPEPIX.HEYVINAY.IN/"))

        assertFalse(WebIntentUtils.isValidHttpsUrl("http://swipepix.heyvinay.in/"))
        assertFalse(WebIntentUtils.isValidHttpsUrl("ftp://swipepix.heyvinay.in/"))
        assertFalse(WebIntentUtils.isValidHttpsUrl("content://swipepix/media"))
        assertFalse(WebIntentUtils.isValidHttpsUrl(null))
        assertFalse(WebIntentUtils.isValidHttpsUrl(""))
        assertFalse(WebIntentUtils.isValidHttpsUrl("   "))
    }

    @Test
    fun testWebIntentUtils_createViewIntent_rejectsInsecureOrInvalidUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            WebIntentUtils.createViewIntent("http://swipepix.heyvinay.in/updates")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WebIntentUtils.createViewIntent("ftp://swipepix.heyvinay.in/updates")
        }
        assertThrows(IllegalArgumentException::class.java) {
            WebIntentUtils.createViewIntent("")
        }
    }
}
