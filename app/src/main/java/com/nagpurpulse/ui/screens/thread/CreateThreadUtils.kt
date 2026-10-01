//java/com/nagpurpulse/ui/screens/thread/CreateThreadUtils.kt

package com.nagpurpulse.ui.screens.thread



import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
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

fun suggestCategory(
    title: String,
    body: String
): String {

    val content = "$title $body".lowercase()

    val scores = mutableMapOf<String, Int>()

    categoryKeywords.forEach { (category, keywords) ->

        var score = 0

        keywords.forEach { keyword ->

            val count = content.split(keyword.lowercase()).size - 1

            score += when {
                keyword.contains(" ") -> count * 3
                else -> count
            }
        }

        scores[category] = score
    }

    val bestMatch = scores.maxByOrNull { it.value }

    return if (bestMatch == null || bestMatch.value <= 0)
        "community"
    else
        bestMatch.key
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

// ── Helper: get recent images from MediaStore ─────────────────────────────────
 fun getRecentImages(context: Context): List<Uri> {
    val images    = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder  = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection, null, null, sortOrder
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        var count = 0
        while (cursor.moveToNext() && count < 30) {
            images.add(
                Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(idCol).toString()
                )
            )
            count++
        }
    }
    Log.d("PHOTO_TEST", "Images Found = ${images.size}")
    return images
}
