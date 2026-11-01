package com.vivificavit.shortstack.util

/**
 * Picks an icon key for a piece of text (an entry's note or a category name)
 * from a built-in word list. Runs entirely on the device; nothing is sent anywhere.
 *
 * Words are checked in the order they appear, so "Baseball game" gets the
 * baseball icon rather than the video game one. Keys name icons in EntryIcons.
 */
object IconMatcher {

    /** Two-word phrases, checked before single words. */
    private val phrases: Map<String, String> = mapOf(
        "ice cream" to "icecream",
        "frozen yogurt" to "icecream",
        "board game" to "dice",
        "card game" to "dice",
        "gift card" to "gift",
        "pocket money" to "payments",
        "tooth fairy" to "star",
        "field trip" to "school",
        "dog walking" to "pets",
        "car wash" to "car",
        "credit card" to "card",
        "debit card" to "card"
    )

    private val words: Map<String, String> = buildMap {
        fun icon(key: String, vararg terms: String) = terms.forEach { put(it, key) }

        // Earning and saving
        icon("payments", "allowance", "pay", "paycheck", "salary", "wage", "wages", "earning", "income", "payment")
        icon("money", "money", "cash", "dollar", "coin", "cents", "refund", "interest", "loan", "debt")
        icon("wallet", "wallet", "purse")
        icon("savings", "saving", "savings", "save", "bank", "piggy", "deposit", "invest", "investment")
        icon("work", "job", "work", "business", "gig", "shift", "odd")
        icon("gift", "gift", "present", "christmas", "hanukkah", "holiday", "stocking")
        icon("cake", "birthday", "bday", "cake")
        icon("celebration", "party", "celebration", "graduation", "halloween", "easter", "valentine")
        icon("star", "reward", "prize", "award", "grade", "grades", "tooth", "bonus", "report")

        // Chores
        icon("cleaning", "chore", "clean", "cleaning", "cleaned", "dish", "dishes", "vacuum", "sweep", "dust", "tidy",
            "trash", "garbage", "mop", "bathroom")
        icon("yard", "lawn", "mow", "mowed", "mowing", "yard", "garden", "gardening", "weed", "weeding", "leaf", "leaves",
            "rake", "raking", "shovel", "shoveling", "snow", "grass")
        icon("pets", "pet", "dog", "cat", "puppy", "kitten", "hamster", "fish", "bird")
        icon("laundry", "laundry", "fold", "folding", "ironing")
        icon("handyman", "fix", "repair", "tool", "tools", "build")
        icon("babysitting", "babysit", "babysitting", "babysitter", "nanny", "childcare")

        // School and hobbies
        icon("school", "school", "class", "supply", "supplies", "tuition", "homework", "backpack", "pencil",
            "notebook", "tutor", "tutoring", "lesson", "lessons")
        icon("books", "library", "bookstore")
        icon("book", "book", "novel", "comic", "magazine", "reading", "kindle", "manga")
        icon("art", "art", "paint", "painting", "craft", "crafts", "drawing", "crayon", "marker", "sketchbook")
        icon("science", "science", "lab", "experiment", "microscope", "telescope")
        icon("music", "music", "song", "guitar", "piano", "violin", "drum", "drums", "band", "concert",
            "instrument", "spotify", "itunes", "album")

        // Games, toys and gadgets
        icon("game", "game", "games", "gaming", "videogame", "nintendo", "switch", "xbox", "playstation", "ps5",
            "steam", "fortnite", "roblox", "minecraft", "robux", "vbucks", "console", "controller", "arcade")
        icon("toy", "toy", "lego", "doll", "figure", "figurine", "plush", "squishmallow", "pokemon", "nerf",
            "slime", "kite", "yoyo", "fidget", "stuffed")
        icon("puzzle", "puzzle", "jigsaw", "rubik")
        icon("dice", "boardgame", "dice", "chess", "checkers", "monopoly", "uno")
        icon("movie", "movie", "film", "cinema", "theater", "theatre", "popcorn")
        icon("tv", "tv", "television", "netflix", "disney", "hulu", "youtube", "streaming")
        icon("headphones", "headphones", "earbuds", "airpods", "headset", "speaker")
        icon("camera", "camera", "photo", "photos", "picture")
        icon("phone", "phone", "iphone", "smartphone", "android")
        icon("computer", "computer", "laptop", "pc", "keyboard", "tablet", "ipad", "chromebook")

        // Sports and outdoors
        icon("soccer", "soccer")
        icon("basketball", "basketball", "hoops")
        icon("baseball", "baseball", "softball", "tball")
        icon("football", "football")
        icon("tennis", "tennis", "racket", "pickleball")
        icon("swimming", "swim", "swimming", "pool", "swimsuit")
        icon("bike", "bike", "bicycle", "cycling", "scooter")
        icon("skateboard", "skate", "skating", "skateboard", "skatepark", "rollerblades")
        icon("hiking", "hike", "hiking", "camp", "camping", "scout", "scouts", "trail")

        // Food and treats
        icon("food", "food", "burger", "fries", "snack", "snacks", "mcdonalds", "chips", "taco", "sandwich",
            "dinner", "breakfast")
        icon("lunch", "lunch", "lunchbox")
        icon("bakery", "bakery", "bread", "bagel", "muffin", "croissant", "pastry")
        icon("pizza", "pizza")
        icon("icecream", "icecream", "gelato", "froyo", "sundae", "popsicle")
        icon("cafe", "drink", "soda", "coffee", "tea", "boba", "juice", "smoothie", "lemonade", "starbucks",
            "slushie", "milkshake")
        icon("restaurant", "restaurant", "dining", "diner", "takeout")
        icon("groceries", "grocery", "groceries", "supermarket", "market")
        icon("candy", "candy", "chocolate", "cookie", "gum", "sweets", "treat", "treats", "donut", "dessert")

        // Shopping
        icon("shopping", "shopping", "shop", "store", "mall", "purchase", "amazon", "target", "walmart")
        icon("clothes", "clothes", "clothing", "shirt", "tshirt", "pants", "jeans", "dress", "shoe", "shoes",
            "sneakers", "jacket", "hoodie", "hat", "socks", "outfit", "uniform")
        icon("jewelry", "jewelry", "necklace", "bracelet", "earring", "earrings")

        // Getting around and going out
        icon("car", "car", "gas", "fuel", "uber", "lyft", "parking")
        icon("bus", "bus", "train", "subway", "metro", "transit", "fare")
        icon("travel", "trip", "travel", "vacation", "flight", "plane", "airport", "hotel")
        icon("beach", "beach", "ocean", "sunscreen")
        icon("park", "park", "zoo", "playground", "outing")
        icon("ticket", "ticket", "admission", "fair", "carnival", "amusement", "museum")

        // Giving and everything else
        icon("charity", "charity", "donate", "donation", "donated", "church", "tithe", "giving", "fundraiser", "offering")
        icon("heart", "love", "kindness")
        icon("health", "doctor", "dentist", "medicine", "glasses", "pharmacy", "bandaid", "health")
        icon("haircut", "haircut", "hair", "salon", "barber", "nails")
    }

    /** Every key the matcher can return, so tests can check each one has an icon. */
    val allKeys: Set<String> get() = (words.values + phrases.values).toSet()

    /** The icon key for the first recognized word or phrase in [text], or null if none match. */
    fun match(text: String): String? {
        val tokens = text.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
        tokens.forEachIndexed { i, token ->
            tokens.getOrNull(i + 1)?.let { next -> phrases["$token $next"]?.let { return it } }
            (words[token] ?: words[singular(token)])?.let { return it }
        }
        return null
    }

    /** A rough English singular, enough for "toys", "puppies" and "boxes". */
    private fun singular(word: String): String = when {
        word.length > 4 && word.endsWith("ies") -> word.dropLast(3) + "y"
        word.endsWith("ches") || word.endsWith("shes") || word.endsWith("xes") -> word.dropLast(2)
        word.length > 3 && word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
        else -> word
    }
}
