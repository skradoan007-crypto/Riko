package com.rikodev.music.android.presentation.visualos.host

import com.rikodev.music.android.presentation.navigation.RikoDestination
import com.rikodev.music.android.presentation.visualos.contract.RikoSheetType
import com.rikodev.music.android.presentation.visualos.contract.RikoLayerType
import com.rikodev.music.android.presentation.visualos.contract.RikoSceneType
import com.rikodev.music.android.presentation.visualos.contract.SceneSpec

// ─────────────────────────────────────────────────────────────────────────────
// RIKO VISUAL OS — SCENE MAP
//
// Route থেকে SceneType-এ ম্যাপিং — একক সত্যের উৎস।
// NavGraph যখন নেভিগেট করে, এই ম্যাপ রুট স্ট্রিং-কে scene model-এ রূপান্তর করে।
//
// Architecture Freeze (Rule #70): নতুন আর্কিটেকচার উদ্ভাবন নেই।
// Constitution: "Navigation = choreography, not replacement"
// ─────────────────────────────────────────────────────────────────────────────

// ═════════════════════════════════════════════════════════════════════════════════
// ১. বটম শিট টাইপ — প্যানেল সিস্টেমে ব্যবহৃত
// ═════════════════════════════════════════════════════════════════════════════════


// ═════════════════════════════════════════════════════════════════════════════════
// ২. SCENE MAP — রুট → সিন টাইপ ম্যাপিং
// ═════════════════════════════════════════════════════════════════════════════════

/**
 * RIKO Visual OS — Scene Map
 *
 * নেভিগেশন রুট থেকে Visual OS সিন টাইপে ম্যাপ করে।
 * একক সত্যের উৎস: route string → RikoSceneType + RikoLayerType।
 * NavGraph যখন নেভিগেট করে, এই ম্যাপ রুটগুলোকে সিন মডেলে অনুবাদ করে।
 *
 * Rule #60: Screen State Is Already Renderable — এই ম্যাপ রেন্ডারিং-এর
 * আগেই সিন টাইপ নির্ধারণ করে, তাই composable গুলো শুধু পড়ে।
 */
object VisualOsSceneMap {

    // ── টপ-লেভেল বেস সিন রুট — বটম নেভ আইটেম ──────────────────────

    /** সকল টপ-লেভেল বেস সিন রুট। */
    private val baseRouteMap: Map<String, RikoSceneType> = mapOf(
        RikoDestination.AllSongs.route to RikoSceneType.Library,
        RikoDestination.Playlists.route to RikoSceneType.Playlists,
        RikoDestination.NowPlaying.route to RikoSceneType.NowPlaying,
        RikoDestination.Search.route to RikoSceneType.Search,
        RikoDestination.Settings.route to RikoSceneType.Settings
    )

    // ── ফোকাস সিন রুট — সাব-ডেস্টিনেশন ─────────────────────────────

    /** ফোকাস সিন রুটের প্রিফিক্স থেকে সিন টাইপ নির্ধারণ। */
    private val focusRoutePrefixes: List<Pair<String, (Map<String, String>) -> RikoSceneType>> = listOf(
        RikoDestination.PlaylistDetail.route to { args ->
            val playlistId = args[RikoDestination.PlaylistDetail.ARG_PLAYLIST_ID]?.toLongOrNull() ?: 0L
            RikoSceneType.PlaylistDetail(playlistId)
        }
    )

    // ── রুট থেকে আর্গুমেন্ট বের করা ──────────────────────────────────

    /**
     * একটি রুট স্ট্রিং থেকে আর্গুমেন্ট ম্যাপ তৈরি করে।
     * যেমন: "playlist_detail/42" → {"playlistId": "42"}
     */
    private fun extractRouteArguments(route: String, template: String): Map<String, String> {
        val args = mutableMapOf<String, String>()
        val routeParts = route.split("/")
        val templateParts = template.split("/")

        for (i in templateParts.indices) {
            val part = templateParts[i]
            if (part.startsWith("{") && part.endsWith("}")) {
                val argName = part.removeSurrounding("{", "}")
                if (i < routeParts.size) {
                    args[argName] = routeParts[i]
                }
            }
        }

        return args
    }

    // ═════════════════════════════════════════════════════════════════════
    // পাবলিক API
    // ═════════════════════════════════════════════════════════════════════

    /**
     * একটি ডেস্টিনেশন রুট থেকে সিন টাইপ ম্যাপ করে।
     *
     * যদি রুট কোনো পরিচিত সিনের সাথে মিলে না যায়, তাহলে
     * ডিফল্টভাবে [RikoSceneType.Library] রিটার্ন করে।
     *
     * @param route NavController থেকে প্রাপ্ত রুট স্ট্রিং (null হতে পারে)।
     * @return সংশ্লিষ্ট [RikoSceneType]।
     */
    fun sceneTypeForRoute(route: String?): RikoSceneType {
        if (route == null) return RikoSceneType.Library

        // প্রথমে সরাসরি বেস রুট ম্যাচ করি
        baseRouteMap[route]?.let { return it }

        // তারপর প্যারামিটারাইজড ফোকাস রুট ম্যাচ করি
        for ((template, sceneFactory) in focusRoutePrefixes) {
            if (routeMatchesTemplate(route, template)) {
                val args = extractRouteArguments(route, template)
                return sceneFactory(args)
            }
        }

        // কোনো ম্যাচ না পাওয়া গেলে ডিফল্ট
        return RikoSceneType.Library
    }

    /**
     * একটি ডেস্টিনেশন রুট থেকে লেয়ার টাইপ নির্ধারণ করে।
     *
     * @param route NavController থেকে প্রাপ্ত রুট স্ট্রিং।
     * @return সংশ্লিষ্ট [RikoLayerType]।
     */
    fun layerTypeForRoute(route: String?): RikoLayerType {
        val sceneType = sceneTypeForRoute(route)
        return SceneSpec.resolveLayerType(sceneType)
    }

    /**
     * রুটটি কি টপ-লেভেল বেস সিন কিনা পরীক্ষা করে।
     *
     * বেস সিন হলো সেই সিন যা বটম নেভ ট্যাবে দেখায় এবং
     * সিন স্ট্যাকের নিচে থাকে (index 0)।
     *
     * @param route NavController থেকে প্রাপ্ত রুট স্ট্রিং।
     * @return true যদি বেস সিন হয়।
     */
    fun isBaseScene(route: String?): Boolean {
        if (route == null) return false
        return route in baseRouteMap
    }

    /**
     * রুটটি কি ফোকাস সিন (সাব-ডেস্টিনেশন) কিনা পরীক্ষা করে।
     *
     * ফোকাস সিন হলো যে সিন বেস সিনের উপরে ভাসে, যেমন
     * NowPlaying, PlaylistDetail। এরা [RikoLayerType.FOCUS] লেয়ারে।
     *
     * @param route NavController থেকে প্রাপ্ত রুট স্ট্রিং।
     * @return true যদি ফোকাস সিন হয়।
     */
    fun isFocusScene(route: String?): Boolean {
        if (route == null) return false
        if (route == RikoDestination.NowPlaying.route) return true

        // প্যারামিটারাইজড ফোকাস রুট পরীক্ষা
        for ((template, _) in focusRoutePrefixes) {
            if (routeMatchesTemplate(route, template)) {
                return true
            }
        }

        return false
    }

    /**
     * রুটটি কি প্যানেল সিন (বটম শিট) কিনা পরীক্ষা করে।
     *
     * প্যানেল সিন হলো ভাসমান যন্ত্র — ইকুয়ালাইজার, টাইমার,
     * কিউ, গানের তথ্য। এরা [RikoLayerType.PANEL] লেয়ারে।
     *
     * বর্তমানে প্যানেল সিনগুলো NavGraph-এ রুট হিসেবে নেই —
     * এরা ModalBottomSheet হিসেবে রেন্ডার হয়। তাই এই মেথড
     * সর্বদা false রিটার্ন করে, কিন্তু [sceneTypeForSheet]
     * ব্যবহার করে প্যানেল সিন টাইপ পাওয়া যায়।
     *
     * @param route NavController থেকে প্রাপ্ত রুট স্ট্রিং।
     * @return বর্তমানে সর্বদা false (প্যানেল রুট-ভিত্তিক নয়)।
     */
    fun isPanelScene(route: String?): Boolean {
        // প্যানেল সিনগুলো NavGraph রুট নয় — এরা বটম শিট।
        // Visual OS প্যানেল সিস্টেম এদের sceneTypeForSheet দিয়ে হ্যান্ডেল করে।
        return false
    }

    /**
     * সকল টপ-লেভেল বেস সিন রুট প্রদান করে।
     *
     * @return বেস সিন রুটের সেট।
     */
    fun topLevelRoutes(): Set<String> {
        return baseRouteMap.keys.toSet()
    }

    /**
     * একটি বটম শিট টাইপ থেকে সিন টাইপ নির্ধারণ করে।
     *
     * NavGraph-এর ModalBottomSheet থেকে Visual OS প্যানেল
     * সিস্টেমে রূপান্তরের জন্য ব্যবহৃত।
     *
     * @param sheetType বটম শিটের ধরন।
     * @return সংশ্লিষ্ট [RikoSceneType] প্যানেল সিন।
     */
    fun sceneTypeForSheet(sheetType: RikoSheetType): RikoSceneType = when (sheetType) {
        RikoSheetType.EQUALIZER -> RikoSceneType.EqualizerPanel
        RikoSheetType.QUEUE -> RikoSceneType.QueuePanel
        RikoSheetType.SONG_INFO -> RikoSceneType.SongInfoPanel
        RikoSheetType.SLEEP_TIMER -> RikoSceneType.SleepTimerPanel
    }

    /**
     * একটি প্যানেল সিন টাইপ থেকে শিট টাইপ নির্ধারণ করে (বিপরীত ম্যাপিং)।
     *
     * @param sceneType প্যানেল সিন টাইপ।
     * @return সংশ্লিষ্ট [RikoSheetType], অথবা null যদি প্যানেল না হয়।
     */
    fun sheetTypeForScene(sceneType: RikoSceneType): RikoSheetType? = when (sceneType) {
        is RikoSceneType.EqualizerPanel -> RikoSheetType.EQUALIZER
        is RikoSceneType.QueuePanel -> RikoSheetType.QUEUE
        is RikoSceneType.SongInfoPanel -> RikoSheetType.SONG_INFO
        is RikoSceneType.SleepTimerPanel -> RikoSheetType.SLEEP_TIMER
        else -> null
    }

    /**
     * একটি রুট থেকে SceneSpec তৈরি করে (Constitution অনুসারে)।
     *
     * [SceneSpec.create] ফ্যাক্টরি ব্যবহার করে সিন স্পেসিফিকেশন তৈরি করে,
     * যা Constitution-এ সংজ্ঞায়িত সকল ডিফল্ট মান প্রয়োগ করে।
     *
     * @param route NavController রুট।
     * @param extraContext অতিরিক্ত কনটেক্সট ডেটা (যেমন playlistId)।
     * @return নতুন [SceneSpec]।
     */
    fun createSceneSpecForRoute(
        route: String?,
        extraContext: Map<String, Any> = emptyMap()
    ): SceneSpec {
        val sceneType = sceneTypeForRoute(route)
        val contextData = buildMap {
            putAll(extraContext)
            // রুট আর্গুমেন্ট থেকে কনটেক্সট বের করি
            if (route != null) {
                for ((template, _) in focusRoutePrefixes) {
                    if (routeMatchesTemplate(route, template)) {
                        putAll(extractRouteArguments(route, template))
                        break
                    }
                }
            }
        }
        return SceneSpec.create(
            type = sceneType,
            contextData = contextData
        )
    }

    // ═════════════════════════════════════════════════════════════════════
    // অভ্যন্তরীয় সহায়ক
    // ═════════════════════════════════════════════════════════════════════

    /**
     * একটি রুট কি টেমপ্লেটের সাথে মেলে পরীক্ষা করে।
     * যেমন: route="playlist_detail/42", template="playlist_detail/{playlistId}" → true
     */
    private fun routeMatchesTemplate(route: String, template: String): Boolean {
        val routeParts = route.split("/")
        val templateParts = template.split("/")

        if (routeParts.size != templateParts.size) return false

        for (i in templateParts.indices) {
            val part = templateParts[i]
            // {arg} প্যারামিটার যেকোনো মান গ্রহণ করে
            if (part.startsWith("{") && part.endsWith("}")) continue
            // স্থির সেগমেন্ট অবশ্যই মিলতে হবে
            if (part != routeParts[i]) return false
        }

        return true
    }
}
