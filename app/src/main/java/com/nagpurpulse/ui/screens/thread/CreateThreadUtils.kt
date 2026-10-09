//java/com/nagpurpulse/ui/screens/thread/CreateThreadUtils.kt

package com.nagpurpulse.ui.screens.thread



import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector


val postCategories = listOf(
    "community",
    "local_news",
    "traffic",
    "weather",
    "food",
    "events",
    "nightlife",
    "jobs",
    "education",
    "sports",
    "shopping",
    "health",
    "safety",
    "civic_issues",
    "entertainment",
    "real_estate",
    "technology",
    "beta"
)

// ── Category metadata ─────────────────────────────────────────────────────────
fun categoryChipLabel(cat: String): String = when (cat) {
    "community"     -> "Community"
    "local_news"    -> "Local News"
    "traffic"       -> "Traffic"
    "weather"       -> "Weather"
    "food"          -> "Food"
    "events"        -> "Events"
    "nightlife"     -> "Nightlife"
    "jobs"          -> "Jobs"
    "education"     -> "Education"
    "sports"        -> "Sports"
    "shopping"      -> "Shopping"
    "health"        -> "Health"
    "safety"        -> "Safety"
    "civic_issues"  -> "Civic Issues"
    "entertainment" -> "Entertainment"
    "real_estate"   -> "Real Estate"
    "technology"    -> "Technology"
    "beta"          -> "Beta"
    else -> cat.replaceFirstChar { it.uppercase() }
}

fun categoryChipIcon(cat: String): ImageVector = when (cat) {
    "community"     -> Icons.Filled.Groups
    "local_news"    -> Icons.Filled.Newspaper
    "traffic"       -> Icons.Filled.DirectionsCar
    "weather"       -> Icons.Filled.Cloud
    "food"          -> Icons.Filled.Restaurant
    "events"        -> Icons.Filled.Event
    "nightlife"     -> Icons.Filled.Nightlife
    "jobs"          -> Icons.Filled.Work
    "education"     -> Icons.Filled.School
    "sports"        -> Icons.Filled.SportsCricket
    "shopping"      -> Icons.Filled.ShoppingBag
    "health"        -> Icons.Filled.LocalHospital
    "safety"        -> Icons.Filled.Campaign
    "civic_issues"  -> Icons.Filled.AccountBalance
    "entertainment" -> Icons.Filled.Movie
    "real_estate"   -> Icons.Filled.Home
    "technology"    -> Icons.Filled.Computer
    "beta"          -> Icons.Filled.BugReport
    else            -> Icons.Filled.Label
}
val categoryKeywords = mapOf(

    "community" to emptyList(),



    "traffic" to listOf(
        "traffic", "jam", "accident", "road", "roads",
        "highway", "signal", "flyover", "vehicle",
        "car", "bike", "truck", "bus", "auto",
        "congestion", "blocked", "diversion",
        "route", "parking", "rush", "commute",
        "collision", "roadblock"
    ),

    "food" to listOf(
        "food", "restaurant", "cafe", "café",
        "biryani", "pizza", "burger", "coffee",
        "tea", "breakfast", "lunch", "dinner",
        "snacks", "street food", "eat", "eating",
        "meal", "thali", "bakery", "dessert",
        "ice cream", "juice", "hotel", "menu",
        "cook", "cooking", "taste", "tasty"
    ),

    "jobs" to listOf(
        "job", "jobs", "hiring", "vacancy",
        "vacancies", "opening", "openings",
        "recruitment", "recruiter", "interview",
        "resume", "cv", "career", "careers",
        "employee", "employer", "salary",
        "internship", "intern", "walk-in",
        "work from home", "wfh", "fresher",
        "experienced", "apply"
    ),

    "events" to listOf(
        "event", "events", "festival",
        "concert", "meetup", "celebration",
        "show", "program", "programme",
        "gathering", "exhibition", "fair",
        "marathon", "competition",
        "seminar", "workshop", "launch",
        "fest", "cultural", "community event"
    ),

    "weather" to listOf(
        "rain", "raining", "weather",
        "storm", "heat", "heatwave",
        "flood", "waterlogging", "summer",
        "winter", "temperature", "humid",
        "humidity", "wind", "windy",
        "cloud", "cloudy", "sunny",
        "monsoon", "cold", "hot"
    ),

    "sports" to listOf(
        "sports", "cricket", "football",
        "soccer", "kabaddi", "badminton",
        "tennis", "volleyball", "basketball",
        "match", "tournament", "league",
        "team", "player", "stadium",
        "ground", "ipl", "run", "runs",
        "wicket", "goal", "fitness race"
    ),

    "education" to listOf(
        "education", "college", "school",
        "exam", "exams", "admission",
        "student", "students", "university",
        "vnit", "nit", "iit",
        "course", "courses", "training",
        "class", "classes", "teacher",
        "faculty", "campus", "study",
        "studies", "result", "syllabus"
    ),

    "health" to listOf(
        "health", "hospital", "doctor",
        "medical", "medicine", "clinic",
        "treatment", "patient", "patients",
        "surgery", "blood", "ambulance",
        "healthcare", "fever", "injury",
        "mental health", "fitness",
        "diet", "nutrition", "disease"
    ),

    "shopping" to listOf(
        "shopping", "mall", "shop",
        "store", "market", "discount",
        "offer", "offers", "sale",
        "buy", "purchase", "deal",
        "deals", "product", "products",
        "fashion", "clothes", "mobile",
        "electronics", "grocery"
    ),

    "nightlife" to listOf(
        "nightlife", "club", "bar",
        "pub", "party", "dj",
        "dance", "music night",
        "cocktail", "weekend",
        "late night", "night out"
    ),

    "real_estate" to listOf(
        "flat", "house", "home",
        "rent", "rental", "pg",
        "hostel", "property",
        "real estate", "apartment",
        "room", "rooms", "broker",
        "plot", "villa", "tenant",
        "owner", "accommodation"
    ),

    "local_news" to listOf(
        "news", "update", "updates",
        "announcement", "announcements",
        "breaking", "headline",
        "report", "reported",
        "latest", "important",
        "official", "press release"
    ),

    "safety" to listOf(
        "crime", "theft", "stolen",
        "robbery", "police", "fraud",
        "scam", "missing", "unsafe",
        "security", "attack",
        "harassment", "warning",
        "alert", "suspicious"
    ),

    "civic_issues" to listOf(
        "power cut", "electricity",
        "water issue", "water supply",
        "garbage", "waste",
        "pothole", "drainage",
        "sewage", "street light",
        "municipal", "road damage",
        "water leakage", "water problem",
        "maintenance", "corporation"
    ),


    "technology" to listOf(
        "technology",
        "tech",
        "software",
        "developer",
        "development",
        "android",
        "ios",
        "app",
        "application",
        "programming",
        "coding",
        "code",
        "java",
        "kotlin",
        "python",
        "react",
        "flutter",
        "ai",
        "chatgpt",
        "openai",
        "laptop",
        "computer",
        "internet",
        "wifi",
        "network",
        "server",
        "sap",
        "aws",
        "cloud",
        "database"
    ),

    "entertainment" to listOf(
        "movie", "movies", "cinema",
        "music", "actor", "actress",
        "song", "songs", "web series",
        "series", "show", "shows",
        "netflix", "amazon prime",
        "youtube", "theatre",
        "comedy", "performance"
    )
)

// Whole-word matchers, built once. Plain substring matching used to turn "care" into "car"
// (traffic) and "brunch" into "run" (sports). Lookarounds (not word-boundary anchors) are used so words with
// accents ("café") and hyphens ("walk-in") still match at their edges.
private val categoryMatchers: Map<String, List<Pair<Regex, Int>>> by lazy {
    categoryKeywords.mapValues { (_, keywords) ->
        keywords.map { keyword ->
            val pattern = "(?<![\\p{L}\\p{N}])${Regex.escape(keyword.lowercase())}(?![\\p{L}\\p{N}])"
            Regex(pattern) to (if (keyword.contains(" ")) 3 else 1)
        }
    }
}

fun suggestCategory(
    title: String,
    body: String
): String {

    val content = "$title $body".lowercase()

    if (content.isBlank()) return "community"

    var bestCategory = "community"
    var bestScore = 0

    categoryMatchers.forEach { (category, matchers) ->
        var score = 0
        matchers.forEach { (regex, weight) ->
            score += regex.findAll(content).count() * weight
        }
        if (score > bestScore) {
            bestScore = score
            bestCategory = category
        }
    }

    return bestCategory
}

/*
val nagpurAreasWithDist = listOf(
    "Sitabuldi"    to "2.2 km",
    "VNIT Area"    to "3.1 km",
    "Wardha Road"  to "4.8 km",
    "Manish Nagar" to "5.6 km",
    "Dharampeth"   to "3.4 km",
    "Sadar"        to "1.8 km",
    "Bajaj Nagar"  to "4.2 km",
    "Ramdaspeth"   to "2.7 km",
    "Civil Lines"  to "3.9 km",
    "Hingna"       to "7.1 km"
)
*/









// ── Colors (matching Image 3 exactly) ────────────────────────────────────────
 val OrangeGradient = Brush.horizontalGradient(listOf(Color(0xFFFFA726), Color(0xFFFF7A00)))
 val DarkSurface    = Color(0xFF1C1C1E)   // card background
 val DarkerBg       = Color(0xFF111111)   // page background
 val BorderGray     = Color(0xFF2C2C2E)   // subtle borders
 val TextWhite      = Color(0xFFFFFFFF)
 val TextGray       = Color(0xFF8E8E93)   // secondary text
 val TextDimGray    = Color(0xFF48484A)   // tertiary / placeholder
 val OrangeMain     = Color(0xFFFF8C00)
 val OrangeSubtleC  = Color(0xFF2A1A00)   // dark orange tint bg
 val PurpleAccent   = Color(0xFF9B59B6)   // AI Assis

// ── Helper: get recent images from MediaStore (runs on the IO dispatcher) ──────
suspend fun loadRecentImages(context: Context, limit: Int = 12): List<Uri> =
    withContext(Dispatchers.IO) {
        val images = mutableListOf<Uri>()
        try {
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sortOrder  = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext() && images.size < limit) {
                    images.add(
                        Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            cursor.getLong(idCol).toString()
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission revoked while the screen was open — show nothing.
        }
        images
    }

// ── Composer content helpers ──────────────────────────────────────────────────

/** Categories shown directly in the chip row; everything else lives behind "More". */
val topPostCategories = listOf("community", "traffic", "food", "events", "local_news", "safety")

/** One-tap starters shown under the title field while it is empty. */
data class PostStarter(
    val label: String,
    val emoji: String,
    val category: String,
    val template: String
)

val postStarters = listOf(
    PostStarter("Traffic update", "🚦", "traffic",   "Traffic update: "),
    PostStarter("Food find",      "🍜", "food",      "Found a great place to eat: "),
    PostStarter("Ask Nagpur",     "🙋", "community", "Can anyone suggest "),
    PostStarter("Lost & found",   "🔍", "community", "Lost / found: "),
    PostStarter("Event nearby",   "🎉", "events",    "Happening near me: ")
)

/** Time-of-day prompts, optionally personalised with the user's detected area. */
fun composerPrompts(area: String?, hour: Int): List<String> {
    val base = when (hour) {
        in 5..11  -> listOf("Good morning Nagpur", "Any updates from your area?", "What's happening today?")
        in 12..17 -> listOf("What's happening in Nagpur?", "Any traffic updates?", "Share a local update...")
        in 18..22 -> listOf("Any events tonight?", "Recommend a place to eat", "What's trending this evening?")
        else      -> listOf("Late night thoughts?", "Anything happening nearby?", "Share something interesting...")
    }
    val common = listOf(
        "Any road closures today?",
        "Power cut in your area?",
        "Any hidden food gems?",
        "Recommend a good cafe...",
        "What should Nagpur know?"
    )
    val local = area?.takeIf { it.isNotBlank() }?.let {
        listOf("What's happening in $it?", "Anything new around $it?")
    } ?: emptyList()
    return local + base + common
}

/** 0f..1f — how "complete" the post is. Drives the progress ring on the Post button. */
fun postQualityProgress(
    title: String,
    body: String,
    hasImage: Boolean,
    hasArea: Boolean
): Float {
    var score = 0f
    if (title.trim().length >= 10) score += 0.35f else if (title.isNotBlank()) score += 0.2f
    if (body.trim().length >= 40) score += 0.25f else if (body.isNotBlank()) score += 0.12f
    if (hasImage) score += 0.25f
    if (hasArea) score += 0.15f
    return score.coerceIn(0f, 1f)
}

/** One short, actionable hint at a time. Returns null when the post is already in good shape. */
fun postQualityNudge(
    title: String,
    body: String,
    hasImage: Boolean,
    hasArea: Boolean
): String? = when {
    title.isBlank()              -> null
    title.trim().length < 10     -> "A little more detail helps neighbours understand"
    body.isBlank()               -> "Add details so people can reply faster"
    !hasImage                    -> "Posts with a photo get noticed more"
    !hasArea                     -> "Pick an area so nearby people see it first"
    else                         -> null
}

