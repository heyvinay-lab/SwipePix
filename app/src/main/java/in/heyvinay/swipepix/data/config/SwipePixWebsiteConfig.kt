package `in`.heyvinay.swipepix.data.config

/**
 * Centralized website routes for SwipePix.
 *
 * All external support actions (Updates, Donate, Feedback) are routed to the official website.
 * The Android app does not perform any network requests or API integration for these actions.
 */
object SwipePixWebsiteConfig {
    const val BASE_URL: String = "https://swipepix.heyvinay.in/"
    const val UPDATES_URL: String = "https://swipepix.heyvinay.in/updates"
    const val DONATE_URL: String = "https://swipepix.heyvinay.in/donate"
    const val FEEDBACK_URL: String = "https://swipepix.heyvinay.in/feedback"
}
