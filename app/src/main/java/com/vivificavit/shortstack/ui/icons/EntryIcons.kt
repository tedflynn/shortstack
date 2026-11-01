package com.vivificavit.shortstack.ui.icons

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.vivificavit.shortstack.R
import com.vivificavit.shortstack.data.model.Transaction
import com.vivificavit.shortstack.data.model.TransactionType
import com.vivificavit.shortstack.util.IconMatcher

/** The groups in the icon picker, in the order it shows them. */
enum class IconTheme(@StringRes val labelRes: Int) {
    MONEY(R.string.icon_theme_money),
    CHORES(R.string.icon_theme_chores),
    SCHOOL(R.string.icon_theme_school),
    FUN(R.string.icon_theme_fun),
    SPORTS(R.string.icon_theme_sports),
    FOOD(R.string.icon_theme_food),
    SHOPPING(R.string.icon_theme_shopping),
    OUTINGS(R.string.icon_theme_outings),
    OTHER(R.string.icon_theme_other)
}

/**
 * A Material icon an entry or category can show. [key] is what's stored and backed up.
 * Icons sharing a [family] are variations of one thing (three kinds of book), so the
 * picker suggests them together.
 */
data class EntryIcon(
    val key: String,
    val vector: ImageVector,
    @StringRes val labelRes: Int,
    val theme: IconTheme,
    val family: String = key
)

object EntryIcons {
    /** Every icon on offer, grouped by theme in picker order. */
    val all: List<EntryIcon> = listOf(
        EntryIcon("money", Icons.Filled.AttachMoney, R.string.icon_money, IconTheme.MONEY),
        EntryIcon("payments", Icons.Filled.Payments, R.string.icon_payments, IconTheme.MONEY, family = "money"),
        EntryIcon("wallet", Icons.Filled.AccountBalanceWallet, R.string.icon_wallet, IconTheme.MONEY, family = "money"),
        EntryIcon("card", Icons.Filled.CreditCard, R.string.icon_card, IconTheme.MONEY, family = "money"),
        EntryIcon("savings", Icons.Filled.Savings, R.string.icon_savings, IconTheme.MONEY),
        EntryIcon("work", Icons.Filled.Work, R.string.icon_work, IconTheme.MONEY),
        EntryIcon("gift", Icons.Filled.CardGiftcard, R.string.icon_gift, IconTheme.MONEY),
        EntryIcon("cake", Icons.Filled.Cake, R.string.icon_cake, IconTheme.MONEY),
        EntryIcon("celebration", Icons.Filled.Celebration, R.string.icon_celebration, IconTheme.MONEY),
        EntryIcon("star", Icons.Filled.Star, R.string.icon_star, IconTheme.MONEY),

        EntryIcon("cleaning", Icons.Filled.CleaningServices, R.string.icon_cleaning, IconTheme.CHORES),
        EntryIcon("yard", Icons.Filled.Grass, R.string.icon_yard, IconTheme.CHORES),
        EntryIcon("pets", Icons.Filled.Pets, R.string.icon_pets, IconTheme.CHORES),
        EntryIcon("laundry", Icons.Filled.LocalLaundryService, R.string.icon_laundry, IconTheme.CHORES),
        EntryIcon("handyman", Icons.Filled.Handyman, R.string.icon_handyman, IconTheme.CHORES),
        EntryIcon("babysitting", Icons.Filled.ChildCare, R.string.icon_babysitting, IconTheme.CHORES),

        EntryIcon("book", Icons.AutoMirrored.Filled.MenuBook, R.string.icon_book, IconTheme.SCHOOL),
        EntryIcon("book_open", Icons.Filled.AutoStories, R.string.icon_book_open, IconTheme.SCHOOL, family = "book"),
        EntryIcon("books", Icons.AutoMirrored.Filled.LibraryBooks, R.string.icon_books, IconTheme.SCHOOL, family = "book"),
        EntryIcon("school", Icons.Filled.School, R.string.icon_school, IconTheme.SCHOOL),
        EntryIcon("art", Icons.Filled.Palette, R.string.icon_art, IconTheme.SCHOOL),
        EntryIcon("science", Icons.Filled.Science, R.string.icon_science, IconTheme.SCHOOL),
        EntryIcon("music", Icons.Filled.MusicNote, R.string.icon_music, IconTheme.SCHOOL),

        EntryIcon("game", Icons.Filled.SportsEsports, R.string.icon_game, IconTheme.FUN),
        EntryIcon("gamepad", Icons.Filled.VideogameAsset, R.string.icon_gamepad, IconTheme.FUN, family = "game"),
        EntryIcon("toy", Icons.Filled.Toys, R.string.icon_toy, IconTheme.FUN),
        EntryIcon("puzzle", Icons.Filled.Extension, R.string.icon_puzzle, IconTheme.FUN),
        EntryIcon("dice", Icons.Filled.Casino, R.string.icon_dice, IconTheme.FUN),
        EntryIcon("movie", Icons.Filled.Movie, R.string.icon_movie, IconTheme.FUN),
        EntryIcon("tv", Icons.Filled.Tv, R.string.icon_tv, IconTheme.FUN),
        EntryIcon("headphones", Icons.Filled.Headphones, R.string.icon_headphones, IconTheme.FUN),
        EntryIcon("camera", Icons.Filled.PhotoCamera, R.string.icon_camera, IconTheme.FUN),
        EntryIcon("phone", Icons.Filled.Smartphone, R.string.icon_phone, IconTheme.FUN),
        EntryIcon("computer", Icons.Filled.Computer, R.string.icon_computer, IconTheme.FUN),

        EntryIcon("soccer", Icons.Filled.SportsSoccer, R.string.icon_soccer, IconTheme.SPORTS),
        EntryIcon("basketball", Icons.Filled.SportsBasketball, R.string.icon_basketball, IconTheme.SPORTS),
        EntryIcon("baseball", Icons.Filled.SportsBaseball, R.string.icon_baseball, IconTheme.SPORTS),
        EntryIcon("football", Icons.Filled.SportsFootball, R.string.icon_football, IconTheme.SPORTS),
        EntryIcon("tennis", Icons.Filled.SportsTennis, R.string.icon_tennis, IconTheme.SPORTS),
        EntryIcon("swimming", Icons.Filled.Pool, R.string.icon_swimming, IconTheme.SPORTS),
        EntryIcon("bike", Icons.AutoMirrored.Filled.DirectionsBike, R.string.icon_bike, IconTheme.SPORTS),
        EntryIcon("skateboard", Icons.Filled.Skateboarding, R.string.icon_skateboard, IconTheme.SPORTS),
        EntryIcon("hiking", Icons.Filled.Hiking, R.string.icon_hiking, IconTheme.SPORTS),

        EntryIcon("food", Icons.Filled.Fastfood, R.string.icon_food, IconTheme.FOOD),
        EntryIcon("lunch", Icons.Filled.LunchDining, R.string.icon_lunch, IconTheme.FOOD, family = "food"),
        EntryIcon("pizza", Icons.Filled.LocalPizza, R.string.icon_pizza, IconTheme.FOOD),
        EntryIcon("bakery", Icons.Filled.BakeryDining, R.string.icon_bakery, IconTheme.FOOD),
        EntryIcon("icecream", Icons.Filled.Icecream, R.string.icon_icecream, IconTheme.FOOD),
        EntryIcon("candy", Icons.Filled.Cookie, R.string.icon_candy, IconTheme.FOOD),
        EntryIcon("cafe", Icons.Filled.LocalCafe, R.string.icon_cafe, IconTheme.FOOD),
        EntryIcon("restaurant", Icons.Filled.Restaurant, R.string.icon_restaurant, IconTheme.FOOD),
        EntryIcon("groceries", Icons.Filled.LocalGroceryStore, R.string.icon_groceries, IconTheme.FOOD),

        EntryIcon("shopping", Icons.Filled.ShoppingBag, R.string.icon_shopping, IconTheme.SHOPPING),
        EntryIcon("clothes", Icons.Filled.Checkroom, R.string.icon_clothes, IconTheme.SHOPPING),
        EntryIcon("jewelry", Icons.Filled.Diamond, R.string.icon_jewelry, IconTheme.SHOPPING),

        EntryIcon("car", Icons.Filled.DirectionsCar, R.string.icon_car, IconTheme.OUTINGS),
        EntryIcon("bus", Icons.Filled.DirectionsBus, R.string.icon_bus, IconTheme.OUTINGS),
        EntryIcon("travel", Icons.Filled.Flight, R.string.icon_travel, IconTheme.OUTINGS),
        EntryIcon("beach", Icons.Filled.BeachAccess, R.string.icon_beach, IconTheme.OUTINGS),
        EntryIcon("park", Icons.Filled.Park, R.string.icon_park, IconTheme.OUTINGS),
        EntryIcon("ticket", Icons.Filled.ConfirmationNumber, R.string.icon_ticket, IconTheme.OUTINGS),

        EntryIcon("charity", Icons.Filled.VolunteerActivism, R.string.icon_charity, IconTheme.OTHER),
        EntryIcon("heart", Icons.Filled.Favorite, R.string.icon_heart, IconTheme.OTHER),
        EntryIcon("health", Icons.Filled.MedicalServices, R.string.icon_health, IconTheme.OTHER),
        EntryIcon("haircut", Icons.Filled.ContentCut, R.string.icon_haircut, IconTheme.OTHER),
    )

    private val byKey = all.associateBy { it.key }

    private val incomeFallback = byKey.getValue("money")
    private val expenseFallback = byKey.getValue("shopping")

    /** Shown in Manage categories for a category with no chosen icon and no matching word. */
    val noMatch: ImageVector = Icons.Filled.Category

    fun forKey(key: String?): EntryIcon? = key?.let(byKey::get)

    /**
     * The icon for an entry: the icon the user chose for its category, then a match for
     * words in its note, then for its category's name, then a generic one for its type.
     * [chosen] maps lower-cased category names to icon keys.
     */
    fun forEntry(entry: Transaction, chosen: Map<String, String>): EntryIcon =
        forKey(chosen[entry.category.lowercase()])
            ?: forKey(IconMatcher.match(entry.title))
            ?: forKey(IconMatcher.match(entry.category))
            ?: if (entry.type == TransactionType.CREDIT) incomeFallback else expenseFallback

    /** The icon a category picks automatically from its name, if any word matches. */
    fun automaticForCategory(category: String): EntryIcon? = forKey(IconMatcher.match(category))

    /**
     * Icons to offer first when picking [category]'s icon: the one its name matches, then
     * the ones its entries' [notes] match (most common first), then related icons from the
     * same themes, variations of the same thing before the rest.
     */
    fun suggestedFor(category: String, notes: List<String>, limit: Int = 10): List<EntryIcon> {
        val noteMatches = notes.mapNotNull { forKey(IconMatcher.match(it)) }
            .groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value } // Stable, so ties keep first-seen order.
            .map { it.key }
        val matched = (listOfNotNull(automaticForCategory(category)) + noteMatches).distinct()
        val related = matched.flatMap { icon ->
            all.filter { it.theme == icon.theme }.sortedBy { if (it.family == icon.family) 0 else 1 }
        }
        return (matched + related).distinct().take(limit)
    }
}
