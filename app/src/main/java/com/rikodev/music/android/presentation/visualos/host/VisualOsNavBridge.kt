package com.rikodev.music.android.presentation.visualos.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.rikodev.music.android.presentation.navigation.RikoDestination
import com.rikodev.music.android.presentation.visualos.contract.RikoSheetType
import com.rikodev.music.android.presentation.visualos.contract.RikoSceneType
import com.rikodev.music.android.presentation.visualos.contract.SceneSpec
import com.rikodev.music.android.presentation.visualos.runtime.VisualOsEvent
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import androidx.compose.material.icons.filled.Pause

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — NAVIGATION BRIDGE
//
// NavController (বিদ্যমান) এবং VisualOsCoordinator-এর মধ্যে দ্বিমুখী সেতু।
//
// Nav → Visual OS: NavController ব্যাক স্ট্যাক পরিবর্তন পর্যবেক্ষণ করে,
//     SceneNavigated ইভেন্ট ডিসপ্যাচ করে কোঅর্ডিনেটরে।
// Visual OS → Nav: কোঅর্ডিনেটর কলব্যাক NavController নেভিগেট করে।
//
// এই ব্রিজ নিশ্চিত করে যে নেভিগেশন এক জায়গায় থাকে (NavGraph),
// অথচ Visual OS লেয়ার সিন ট্রানজিশনের জন্য সচেতন থাকে।
//
// Rule #70: Architecture Freeze — বিদ্যমান NavController ব্যবহার করে, প্রতিস্থাপন করে না।
// Constitution: "Navigation = choreography, not replacement"
// Rule #83: Coordinator Executes — কলব্যাক শুধু নেভিগেশন করে, স্টেট পরিবর্তন নয়।
// ─────────────────────────────────────────────────────────────────────────────

// ═════════════════════════════════════════════════════════════════════════════════
// ১. প্রধান ব্রিজ কম্পোজেবল
// ═════════════════════════════════════════════════════════════════════════════════

/**
 * NavController এবং VisualOsCoordinator-এর মধ্যে দ্বিমুখী সেতু।
 *
 * এই কম্পোজেবল দুটি দিক থেকে কাজ করে:
 *
 * **Nav → Visual OS:**
 * NavController ব্যাক স্ট্যাক পরিবর্তন পর্যবেক্ষণ করে।
 * রুট পরিবর্তন হলে [VisualOsSceneMap] ব্যবহার করে সিন টাইপ
 * নির্ধারণ করে এবং [VisualOsEvent.SceneNavigated] ডিসপ্যাচ করে।
 *
 * **Visual OS→Nav:** retired as a direction — the coordinator→world
 * callbacks are owned by exactly one wiring point (`VisualOsCoordinatorWiring`,
 * invoked from MainActivity, G-4). This bridge is one-way: Nav → Coordinator scene sync.
 *
 * @param navController বিদ্যমান NavController (NavGraph থেকে)।
 * @param coordinator Visual OS কোঅর্ডিনেটর।
 */
@Composable
fun VisualOsNavBridge(
    navController: NavController,
    coordinator: VisualOsCoordinator
) {
    // ── বর্তমান রুট পর্যবেক্ষণ ─────────────────────────────────────

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // আগের রুট মনে রাখি — পরিবর্তন সনাক্ত করতে
    var previousRoute by remember { mutableStateOf<String?>(null) }


    // ── রুট পরিবর্তন → কোঅর্ডিনেটরে ডিসপ্যাচ ──────────────────────

    // snapshotFlow ব্যবহার করে রুট পরিবর্তন পর্যবেক্ষণ করি।
    // LaunchedEffect(key=currentRoute) নয়, কারণ snapshotFlow আরো
    // নির্ভরযোগ্যভাবে কনসেকিউটিভ পরিবর্তন ধরে।
    LaunchedEffect(navController) {
        snapshotFlow {
            navController.currentBackStackEntry?.destination?.route
        }
            .map { it } // রুট স্ট্রিং
            .distinctUntilChanged()
            .collect { route ->
                if (route == previousRoute) return@collect

                // নতুন সিন টাইপ নির্ধারণ করি
                val sceneType = VisualOsSceneMap.sceneTypeForRoute(route)
                val layerType = VisualOsSceneMap.layerTypeForRoute(route)

                // কনটেক্সট ডেটা তৈরি করি (রুট আর্গুমেন্ট সহ)
                val contextData = mutableMapOf<String, Any>()
                contextData["route"] = route ?: RikoDestination.AllSongs.route

                // প্লেলিস্ট ডিটেইল আর্গুমেন্ট বের করি
                if (route != null && route.startsWith("playlist_detail/")) {
                    val playlistId = route.removePrefix("playlist_detail/").toLongOrNull()
                    if (playlistId != null) {
                        contextData[RikoDestination.PlaylistDetail.ARG_PLAYLIST_ID] = playlistId
                    }
                }

                // কোঅর্ডিনেটরে সিন নেভিগেশন ইভেন্ট ডিসপ্যাচ করি
                coordinator.dispatch(
                    VisualOsEvent.SceneNavigated(
                        target = sceneType,
                        context = contextData
                    )
                )

                previousRoute = route
            }
    }

    // ── শিট সিঙ্ক হ্যান্ডলার — Visual OS প্যানেল ↔ NavGraph শিট ──

    // Visual OS প্যানেল খোলার সময় NavGraph-এর শিটও খুলতে হবে,
    // কারণ শিট কনটেন্ট এখনো NavGraph-এ রয়েছে।
    // ভবিষ্যতে শিট কনটেন্ট Visual OS প্যানেলে সরে যাবে,
    // তখন এই সিঙ্ক অপ্রয়োজনীয় হবে।

    // শিট খোলার পর্যবেক্ষণ করা হয় VisualOsHost থেকে কল করে।
    // এখানে শুধু স্টেট ট্র্যাক করা হয়।
}

// ═════════════════════════════════════════════════════════════════════════════════
// ২. নেভিগেশন হ্যান্ডলার — কোঅর্ডিনেটর থেকে ব্যবহৃত
// ═════════════════════════════════════════════════════════════════════════════════

/**
 * Visual OS কোঅর্ডিনেটর থেকে নেভিগেশন কলব্যাক হ্যান্ডেল করে।
 *
 * এই অবজেক্ট কোঅর্ডিনেটরের কলব্যাক প্রপার্টি সেট করে
 * যাতে কোঅর্ডিনেটর NavController-এ নেভিগেট করতে পারে।
 *
 * Rule #83: কোঅর্ডিনেটর এক্সিকিউট করে, রিডিউসার কখনো না।
 * এই হ্যান্ডলার কোঅর্ডিনেটরের পক্ষ থেকে নেভিগেশন
 * সাইড এফেক্ট হিসেবে কাজ করে।
 */
object VisualOsNavHandler {

    /**
     * একটি সিন টাইপ থেকে NavController রুট নির্ধারণ করে।
     *
     * Visual OS কোঅর্ডিনেটর যখন একটি সিন টাইপে নেভিগেট
     * করতে চায়, এই মেথড সঠিক NavController রুট খুঁজে বের করে।
     *
     * @param sceneType লক্ষ্য সিন টাইপ।
     * @param context সিন কনটেক্সট ডেটা (যেমন playlistId)।
     * @return NavController রুট স্ট্রিং, বা null যদি রুট না পাওয়া যায়।
     */
    fun resolveRoute(sceneType: RikoSceneType, context: Map<String, Any> = emptyMap()): String? {
        return when (sceneType) {
            is RikoSceneType.Library -> RikoDestination.AllSongs.route
            is RikoSceneType.Playlists -> RikoDestination.Playlists.route
            is RikoSceneType.NowPlaying -> RikoDestination.NowPlaying.route
            is RikoSceneType.Search -> RikoDestination.Search.route
            is RikoSceneType.Settings -> RikoDestination.Settings.route
            is RikoSceneType.PlaylistDetail -> {
                val playlistId = context[RikoDestination.PlaylistDetail.ARG_PLAYLIST_ID] as? Long
                    ?: sceneType.playlistId
                RikoDestination.PlaylistDetail.createRoute(playlistId)
            }
            // প্যানেল এবং ওভারলে সিনগুলোর NavController রুট নেই
            // এরা বটম শিট / ফ্লোটিং লেয়ার হিসেবে রেন্ডার হয়
            is RikoSceneType.EqualizerPanel -> null
            is RikoSceneType.SleepTimerPanel -> null
            is RikoSceneType.SongInfoPanel -> null
            is RikoSceneType.QueuePanel -> null
            is RikoSceneType.ContextMenu -> null
            is RikoSceneType.QuickInfo -> null
            is RikoSceneType.MiniPlayerDock -> null
        }
    }

    /**
     * একটি শিট টাইপ থেকে সিন টাইপ নির্ধারণ করে।
     *
     * [VisualOsSceneMap.sceneTypeForSheet]-এ ডেলিগেট করে।
     *
     * @param sheetType বটম শিটের ধরন।
     * @return সংশ্লিষ্ট প্যানেল সিন টাইপ।
     */
    fun sceneTypeForSheet(sheetType: RikoSheetType): RikoSceneType {
        return VisualOsSceneMap.sceneTypeForSheet(sheetType)
    }
}

// ═════════════════════════════════════════════════════════════════════════════════
// ৩. রুট সিঙ্ক্রোনাইজেশন হেল্পার
// ═════════════════════════════════════════════════════════════════════════════════

/**
 * রুট সিঙ্ক্রোনাইজেশনের জন্য ডেটা ক্লাস।
 *
 * ব্রিজ এবং হোস্ট মধ্যে রুট সিঙ্ক স্টেট শেয়ার করতে ব্যবহৃত।
 */
data class RouteSyncState(
    /** বর্তমান NavController রুট। */
    val currentRoute: String? = null,

    /** আগের NavController রুট (পরিবর্তন সনাক্তকরণের জন্য)। */
    val previousRoute: String? = null,

    /** বর্তমানে সক্রিয় শিট (যদি থাকে)। */
    val activeSheet: RikoSheetType? = null,

    /** রুট পরিবর্তন হয়েছে কিনা। */
    val hasRouteChanged: Boolean = false
) {
    companion object {
        /** প্রাথমিক সিঙ্ক স্টেট তৈরি করে। */
        fun initial(route: String?): RouteSyncState = RouteSyncState(
            currentRoute = route,
            previousRoute = null,
            activeSheet = null,
            hasRouteChanged = false
        )
    }
}
