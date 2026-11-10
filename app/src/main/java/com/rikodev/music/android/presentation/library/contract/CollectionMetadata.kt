/*
 * RIKO EXPERIENCE OS — LIBRARY EXPERIENCE
 * Build B1 — Collection Contract Pack v2 (Final)
 *
 * File 10 / 15 — CollectionMetadata.kt  ⭐ NEW in v2
 * --------------------------------------------------------------------
 *
 * PURPOSE
 * -------
 * Every collection has an *Identity Card*. This file is it.
 *
 * Metadata is NOT computed at runtime. Metadata is declared at the
 * Constitution level — it is what makes a contract self-explanatory.
 *
 * RULE #38 — Every Collection Can Explain Itself
 * RULE #40 — Metadata Lives Forever (v2)
 *
 * Metadata drives:
 *   • Empty-state copy (subtitle, description, userExplanation)
 *   • Search indexing (searchKeywords, aliases)
 *   • Developer docs (developerNotes, futureNotes)
 *   • Onboarding hints (userExplanation shown in tutorial overlay)
 *   • Audit / regression (version, createdFor)
 *
 * A new developer opening this file should be able to understand
 * each collection WITHOUT reading the engine. This is the file
 * that turns RIKO from "code" into "documentation that runs".
 *
 * No logic. Pure declaration.
 */

package com.rikodev.music.android.presentation.library.contract

/**
 * The Identity Card of a RIKO collection.
 *
 * Every field is a string — no logic, no derivation. The runtime
 * is free to *render* these (e.g. localize, truncate, wrap), but
 * it MUST NOT generate them.
 */
data class CollectionMetadata(
    /** Short user-facing name. Same as [CollectionType.displayName]. */
    val title: String,
    /** Single-line subtitle — appears below the title in the header. */
    val subtitle: String,
    /** 2–3 sentence description shown in onboarding / long-press. */
    val description: String,
    /** Internal note for developers — never shown to users. */
    val developerNotes: String,
    /** User-facing explanation in plain language — first-person voice. */
    val userExplanation: String,
    /** Searchable keywords — used by Library search index. */
    val searchKeywords: List<String>,
    /** Alternative names — used for fuzzy search and voice queries. */
    val aliases: List<String>,
    /** Constitution version this collection was introduced in. */
    val version: String,
    /** Which build introduced this collection (e.g. "B1", "B3"). */
    val createdFor: String,
    /** Notes for future engineers — context for future modifications. */
    val futureNotes: String,
) {

    companion object {

        // ─────────────────────────────────────────────────────────
        // LISTENING CONTINUITY
        // ─────────────────────────────────────────────────────────

        val RECENTLY_PLAYED = CollectionMetadata(
            title = "Recently Played",
            subtitle = "Where you left off",
            description = "A short list of the tracks you played most " +
                "recently. Tap any to resume from where you stopped.",
            developerNotes = "Engine: ContinueStrategy. Source: " +
                "PlaybackHistory (last 14 days). Capped at 50.",
            userExplanation = "We keep track of what you played lately " +
                "so you can jump straight back in.",
            searchKeywords = listOf("recent", "latest", "played", "history"),
            aliases = listOf("recent", "last played", "just played"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Consider adding 'resume from exact position' " +
                "once per-song position persistence is in (Post-v1.0).",
        )

        val CONTINUE_LISTENING = CollectionMetadata(
            title = "Continue Listening",
            subtitle = "Pick up where you left off",
            description = "Tracks you started but didn't finish. " +
                "RIKO noticed — and saved them here for you.",
            developerNotes = "Engine: ContinueStrategy. Source: " +
                "PlaybackHistory filtered by completion < 0.6.",
            userExplanation = "We noticed you started a few tracks but " +
                "didn't finish them. Want to continue?",
            searchKeywords = listOf("continue", "resume", "incomplete", "left off"),
            aliases = listOf("continue", "resume", "finish"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Show exact pause position once WaveformRepository " +
                "exposes per-song pause timestamp.",
        )

        val RECENTLY_FINISHED = CollectionMetadata(
            title = "Recently Finished",
            subtitle = "Tracks you completed",
            description = "Tracks you listened to all the way through " +
                "in the last 72 hours.",
            developerNotes = "Engine: ContinueStrategy. Filter: completion ≥ 0.95.",
            userExplanation = "You finished these recently — that means " +
                "they held your attention.",
            searchKeywords = listOf("finished", "completed", "done", "full"),
            aliases = listOf("finished", "completed"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Could be split into 'finished today' and " +
                "'finished this week' if feed grows noisy.",
        )

        // ─────────────────────────────────────────────────────────
        // PLAY FREQUENCY
        // ─────────────────────────────────────────────────────────

        val MOST_PLAYED = CollectionMetadata(
            title = "Most Played",
            subtitle = "Your top tracks",
            description = "The tracks you've played the most across " +
                "your entire library.",
            developerNotes = "Engine: FrequencyStrategy. Source: " +
                "PlaybackHistory aggregated by play count.",
            userExplanation = "These are the tracks you keep coming " +
                "back to. We thought you'd like to see them together.",
            searchKeywords = listOf("top", "most", "played", "frequent", "favorite"),
            aliases = listOf("top tracks", "most played", "frequent"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Consider windowed variants: 'most played this month'.",
        )

        val LEAST_PLAYED = CollectionMetadata(
            title = "Least Played",
            subtitle = "Worth another try",
            description = "Tracks you've played but rarely. Maybe it's " +
                "time to give them another chance.",
            developerNotes = "Engine: FrequencyStrategy. Filter: " +
                "playCount between 1 and 3.",
            userExplanation = "You've played these, but not often. " +
                "Maybe they deserve another listen.",
            searchKeywords = listOf("least", "rare", "underplayed"),
            aliases = listOf("least played", "rarely played"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Pair with HiddenGems to avoid overlap.",
        )

        val NEVER_PLAYED = CollectionMetadata(
            title = "Never Played",
            subtitle = "Still waiting for you",
            description = "Tracks in your library you've never played " +
                "even once.",
            developerNotes = "Engine: DiscoveryStrategy. Source: " +
                "Library minus PlaybackHistory.",
            userExplanation = "These have been in your library but you've " +
                "never pressed play on them. Surprise yourself.",
            searchKeywords = listOf("never", "unplayed", "new"),
            aliases = listOf("never played", "unplayed"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Could surface a 'random unplayed' button.",
        )

        // ─────────────────────────────────────────────────────────
        // LIBRARY INTAKE
        // ─────────────────────────────────────────────────────────

        val RECENTLY_ADDED = CollectionMetadata(
            title = "Recently Added",
            subtitle = "Fresh in your library",
            description = "Tracks that have joined your library in the " +
                "last 30 days.",
            developerNotes = "Engine: LibraryStrategy. Source: " +
                "Song.dateAdded.",
            userExplanation = "These are the newest members of your " +
                "library. Welcome them.",
            searchKeywords = listOf("recent", "new", "added", "imported"),
            aliases = listOf("new", "recently added", "fresh"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Add 'just imported' badge for items added in last hour.",
        )

        // ─────────────────────────────────────────────────────────
        // HIDDEN GEMS
        // ─────────────────────────────────────────────────────────

        val HIDDEN_GEMS = CollectionMetadata(
            title = "Hidden Gems",
            subtitle = "Songs worth rediscovering",
            description = "Tracks you rarely play but usually finish " +
                "when you do. RIKO's quiet love letter to you.",
            developerNotes = "Engine: DiscoveryStrategy. Filter: " +
                "playCount low, completion ratio high.",
            userExplanation = "We noticed you usually listen to these " +
                "until the end — even though you don't play them often.",
            searchKeywords = listOf("hidden", "gems", "rare", "forgotten", "classic", "underrated"),
            aliases = listOf("hidden gems", "underrated", "rediscover"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Signature collection — protect from algorithm " +
                "drift. Hand-tune thresholds yearly.",
        )

        val FORGOTTEN_CLASSICS = CollectionMetadata(
            title = "Forgotten Classics",
            subtitle = "Remember these?",
            description = "Tracks you once loved but haven't played in " +
                "90+ days.",
            developerNotes = "Engine: ForgottenStrategy. Filter: " +
                "playCount > 5 AND lastPlayed > 90 days ago.",
            userExplanation = "It's been a while since you played these. " +
                "Maybe it's time to revisit.",
            searchKeywords = listOf("forgotten", "old", "classic", "remember", "nostalgia"),
            aliases = listOf("forgotten", "nostalgia", "remember"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Add 'forgotten this year' variant for year-end recap.",
        )

        // ─────────────────────────────────────────────────────────
        // MOOD / TIME OF DAY
        // ─────────────────────────────────────────────────────────

        val NIGHT_CALM = CollectionMetadata(
            title = "Night Calm",
            subtitle = "For the quiet hours",
            description = "Slow, soft, low-tempo tracks for the wind-down " +
                "before sleep.",
            developerNotes = "Engine: MoodStrategy. Filter: BPM < 90, " +
                "energy low, vocal presence moderate.",
            userExplanation = "Calm sounds for the end of your day.",
            searchKeywords = listOf("night", "calm", "sleep", "quiet", "relax"),
            aliases = listOf("night", "calm", "sleep", "wind down"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Auto-surface after 9pm local time.",
        )

        val MORNING_ENERGY = CollectionMetadata(
            title = "Morning Energy",
            subtitle = "Start the day right",
            description = "Bright, energetic tracks to kick off your morning.",
            developerNotes = "Engine: MoodStrategy. Filter: BPM > 110, " +
                "energy high, brightness high.",
            userExplanation = "Energy for the start of your day.",
            searchKeywords = listOf("morning", "energy", "wake", "start", "fresh"),
            aliases = listOf("morning", "wake up", "energetic"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Auto-surface between 6am and 10am local time.",
        )

        val EVENING_UNWIND = CollectionMetadata(
            title = "Evening Unwind",
            subtitle = "Slow the day down",
            description = "Mellow tracks to help you transition from day " +
                "to night.",
            developerNotes = "Engine: MoodStrategy. Filter: BPM 80–110, " +
                "energy medium-low.",
            userExplanation = "Mellow sounds to ease you into the evening.",
            searchKeywords = listOf("evening", "unwind", "sunset", "transition"),
            aliases = listOf("evening", "unwind", "sunset"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Auto-surface between 6pm and 9pm local time.",
        )

        val COMMUTE_MUSIC = CollectionMetadata(
            title = "Commute",
            subtitle = "For the road",
            description = "Steady, present tracks for the daily commute.",
            developerNotes = "Engine: MoodStrategy. Filter: BPM 100–120, " +
                "energy medium, instrumental-friendly.",
            userExplanation = "Music for being on the move.",
            searchKeywords = listOf("commute", "travel", "road", "drive", "transit"),
            aliases = listOf("commute", "travel", "driving"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Detect motion activity to auto-surface.",
        )

        val STUDY_FOCUS = CollectionMetadata(
            title = "Study Focus",
            subtitle = "Stay in the zone",
            description = "Instrumental and low-vocal tracks for sustained " +
                "concentration.",
            developerNotes = "Engine: MoodStrategy. Filter: instrumental " +
                "or low-vocal, BPM 70–100.",
            userExplanation = "Music that helps you focus without " +
                "distracting you.",
            searchKeywords = listOf("study", "focus", "work", "concentration", "instrumental"),
            aliases = listOf("study", "focus", "work", "concentration"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Pair with a 25-minute focus timer.",
        )

        val SLEEP_DRIFT = CollectionMetadata(
            title = "Sleep Drift",
            subtitle = "Drift away",
            description = "Ambient, near-silent tracks designed to fade " +
                "out as you fall asleep.",
            developerNotes = "Engine: MoodStrategy. Filter: BPM < 70, " +
                "energy very low, ambient.",
            userExplanation = "Sounds to drift off to.",
            searchKeywords = listOf("sleep", "drift", "ambient", "fade", "night"),
            aliases = listOf("sleep", "ambient", "drift"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Auto-fade out over 30 minutes.",
        )

        val WORKOUT_PUMP = CollectionMetadata(
            title = "Workout Pump",
            subtitle = "Push harder",
            description = "High-energy tracks to power your workout.",
            developerNotes = "Engine: MoodStrategy. Filter: BPM > 130, " +
                "energy very high.",
            userExplanation = "High energy for when you need to push.",
            searchKeywords = listOf("workout", "pump", "exercise", "gym", "energy"),
            aliases = listOf("workout", "gym", "exercise"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Detect activity to auto-surface.",
        )

        // ─────────────────────────────────────────────────────────
        // EMOTIONAL DEEP CUTS
        // ─────────────────────────────────────────────────────────

        val ROMANTIC_MOOD = CollectionMetadata(
            title = "Romantic",
            subtitle = "For the heart",
            description = "Warm, intimate tracks for moments with someone " +
                "special — or for missing them.",
            developerNotes = "Engine: EmotionStrategy. Filter: warmth " +
                "high, intensity gentle, vocal presence high.",
            userExplanation = "Warm sounds for soft moments.",
            searchKeywords = listOf("romantic", "love", "heart", "warm", "intimate"),
            aliases = listOf("romantic", "love", "intimate"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Pair with calendar proximity to anniversaries.",
        )

        val MELANCHOLY_JOURNEY = CollectionMetadata(
            title = "Melancholy",
            subtitle = "For the rainy afternoons",
            description = "Reflective, bittersweet tracks for when you " +
                "want to feel the feeling fully.",
            developerNotes = "Engine: EmotionStrategy. Filter: warmth " +
                "low, intensity medium, BPM slow.",
            userExplanation = "Music for the days when you need to feel " +
                "something fully.",
            searchKeywords = listOf("melancholy", "sad", "reflective", "rainy", "blue"),
            aliases = listOf("melancholy", "sad", "reflective"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Detect weather to auto-surface on rainy days.",
        )

        val INSTRUMENTAL_ESCAPE = CollectionMetadata(
            title = "Instrumental Escape",
            subtitle = "No words, just music",
            description = "Pure instrumental tracks for when vocals would " +
                "be too much.",
            developerNotes = "Engine: EmotionStrategy. Filter: " +
                "vocalPresence < 0.1.",
            userExplanation = "Just instruments — no words to distract.",
            searchKeywords = listOf("instrumental", "no vocals", "ambient", "music only"),
            aliases = listOf("instrumental", "no vocals"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Sub-categorize by primary instrument once " +
                "instrument classifier ships.",
        )

        val VOCAL_ONLY = CollectionMetadata(
            title = "Vocals Only",
            subtitle = "Just the voice",
            description = "Tracks where the voice carries everything.",
            developerNotes = "Engine: EmotionStrategy. Filter: " +
                "vocalPresence > 0.7.",
            userExplanation = "These tracks are all about the voice.",
            searchKeywords = listOf("vocal", "voice", "singing", "acapella"),
            aliases = listOf("vocals", "voice"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Sub-categorize by language once language " +
                "detection ships.",
        )

        // ─────────────────────────────────────────────────────────
        // DISCOVERY
        // ─────────────────────────────────────────────────────────

        val DISCOVERY = CollectionMetadata(
            title = "Discovery",
            subtitle = "What you haven't heard yet",
            description = "Curated tracks from your library that you " +
                "haven't fully explored.",
            developerNotes = "Engine: DiscoveryStrategy. Blend of " +
                "NeverPlayed, LeastPlayed, and SimilarTo.",
            userExplanation = "Tracks from your library we think you " +
                "haven't fully explored yet.",
            searchKeywords = listOf("discovery", "explore", "new", "find"),
            aliases = listOf("discovery", "explore", "find new"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Add 'why this is here' tooltip once engine " +
                "exposes reason codes.",
        )

        val SIMILAR_TO = CollectionMetadata(
            title = "Similar To",
            subtitle = "If you liked that…",
            description = "Tracks similar to what you've been playing " +
                "recently.",
            developerNotes = "Engine: SimilarStrategy. Source: last " +
                "played track → cosine similarity on audio features.",
            userExplanation = "Because you played X, you might like these.",
            searchKeywords = listOf("similar", "related", "like", "recommend"),
            aliases = listOf("similar", "related", "recommend"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Surface 'because you played X' label explicitly.",
        )

        val SURPRISE_ME = CollectionMetadata(
            title = "Surprise Me",
            subtitle = "Roll the dice",
            description = "A random assortment from your library — " +
                "refresh for a new surprise.",
            developerNotes = "Engine: RandomStrategy. Allow duplicates. " +
                "Cache: never.",
            userExplanation = "A random mix from your library. Tap " +
                "refresh for another surprise.",
            searchKeywords = listOf("surprise", "random", "shuffle", "dice"),
            aliases = listOf("surprise", "random", "shuffle me"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Allow weighting by mood once mood detection " +
                "is reliable.",
        )

        // ─────────────────────────────────────────────────────────
        // CONTEXT-AWARE (Future-ready)
        // ─────────────────────────────────────────────────────────

        val MOOD_BASED = CollectionMetadata(
            title = "Mood",
            subtitle = "Match your mood",
            description = "Collections generated based on what your " +
                "current mood seems to be.",
            developerNotes = "Engine: AdaptiveStrategy. Source: " +
                "inferred from recent playback patterns.",
            userExplanation = "We picked up on your mood from what " +
                "you've been playing. Here's what fits.",
            searchKeywords = listOf("mood", "adaptive", "feeling", "vibe"),
            aliases = listOf("mood", "vibe", "adaptive"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Will require explicit opt-in for mood inference.",
        )

        val WEATHER_BASED = CollectionMetadata(
            title = "Weather",
            subtitle = "Sounds for the sky",
            description = "Collections that match the current weather " +
                "outside your window.",
            developerNotes = "Engine: WeatherStrategy. Source: system " +
                "weather API.",
            userExplanation = "Music that fits the weather outside.",
            searchKeywords = listOf("weather", "rain", "sun", "snow", "sky"),
            aliases = listOf("weather", "rainy day", "sunny"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Needs location permission flow.",
        )

        val TIME_BASED = CollectionMetadata(
            title = "Time of Day",
            subtitle = "Right for right now",
            description = "Collections that match the current time of " +
                "day — morning, afternoon, evening, night.",
            developerNotes = "Engine: TimeStrategy. Source: system clock.",
            userExplanation = "Music that fits this time of day.",
            searchKeywords = listOf("time", "morning", "afternoon", "evening", "night"),
            aliases = listOf("time", "time of day"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Allow user override for shift workers.",
        )

        // ─────────────────────────────────────────────────────────
        // USER-OWNED
        // ─────────────────────────────────────────────────────────

        val FAVORITES = CollectionMetadata(
            title = "Favorites",
            subtitle = "Your loved tracks",
            description = "Every track you've ever marked as favorite.",
            developerNotes = "Source: Song.isFavorite flag. " +
                "Engine: none — direct read.",
            userExplanation = "These are the tracks you've loved. " +
                "They live here forever.",
            searchKeywords = listOf("favorite", "loved", "starred", "liked"),
            aliases = listOf("favorites", "loved", "starred"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Allow folders within favorites in future.",
        )

        val PINNED = CollectionMetadata(
            title = "Pinned",
            subtitle = "Always at the top",
            description = "Collections or songs you've pinned for quick " +
                "access.",
            developerNotes = "Source: PinDao. Engine: none.",
            userExplanation = "You've pinned these so they're always " +
                "easy to find.",
            searchKeywords = listOf("pinned", "fixed", "top", "quick"),
            aliases = listOf("pinned", "fixed"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Allow reordering within pinned.",
        )

        val PLAYLIST_USER = CollectionMetadata(
            title = "Your Playlists",
            subtitle = "Made by you",
            description = "Playlists you've created and curated yourself.",
            developerNotes = "Source: PlaylistDao where source = USER.",
            userExplanation = "Playlists you've made. They're yours — " +
                "edit them anytime.",
            searchKeywords = listOf("playlist", "user", "mine", "custom"),
            aliases = listOf("playlists", "my playlists"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Smart playlists will live separately.",
        )

        val PLAYLIST_AUTO = CollectionMetadata(
            title = "Smart Playlists",
            subtitle = "Maintained by RIKO",
            description = "Playlists RIKO maintains for you based on " +
                "your listening habits.",
            developerNotes = "Source: PlaylistDao where source = AUTO.",
            userExplanation = "We maintain these for you. They update " +
                "as you listen.",
            searchKeywords = listOf("smart", "auto", "playlist", "generated"),
            aliases = listOf("smart playlists", "auto playlists"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Allow user to convert smart playlist to user " +
                "playlist (fork).",
        )

        // ─────────────────────────────────────────────────────────
        // STRUCTURAL
        // ─────────────────────────────────────────────────────────

        val ALL_SONGS = CollectionMetadata(
            title = "All Songs",
            subtitle = "Everything in your library",
            description = "Every single track in your library, in one " +
                "scrollable list.",
            developerNotes = "Source: Song table. Engine: none.",
            userExplanation = "Every track you own, in one list.",
            searchKeywords = listOf("all", "songs", "tracks", "library", "everything"),
            aliases = listOf("all songs", "everything"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Add fast-scroller with letter index.",
        )

        val ALL_ALBUMS = CollectionMetadata(
            title = "Albums",
            subtitle = "By album",
            description = "Your library organized by album.",
            developerNotes = "Source: Album table.",
            userExplanation = "Your library, organized by album.",
            searchKeywords = listOf("album", "albums", "records"),
            aliases = listOf("albums", "records"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Add year-of-release grouping.",
        )

        val ALL_ARTISTS = CollectionMetadata(
            title = "Artists",
            subtitle = "By artist",
            description = "Your library organized by artist.",
            developerNotes = "Source: Artist table.",
            userExplanation = "Your library, organized by artist.",
            searchKeywords = listOf("artist", "artists", "performer", "musician"),
            aliases = listOf("artists", "performers"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Add 'featured in' count for collaborations.",
        )

        val ALL_FOLDERS = CollectionMetadata(
            title = "Folders",
            subtitle = "By folder",
            description = "Your library mirroring your on-device folder " +
                "structure.",
            developerNotes = "Source: Song.folderPath grouped.",
            userExplanation = "Your library, organized the way you " +
                "stored it on disk.",
            searchKeywords = listOf("folder", "folders", "directory", "files"),
            aliases = listOf("folders", "directories"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Allow folder pinning.",
        )

        // ─────────────────────────────────────────────────────────
        // DEEP DIVE
        // ─────────────────────────────────────────────────────────

        val ARTIST_SPOTLIGHT = CollectionMetadata(
            title = "Artist Spotlight",
            subtitle = "A closer look",
            description = "A featured artist with their top tracks, " +
                "albums, and similar artists.",
            developerNotes = "Engine: SpotlightStrategy. Source: " +
                "rotating selection from top-played artists.",
            userExplanation = "Today's spotlight: a deeper look at an " +
                "artist you've been listening to.",
            searchKeywords = listOf("spotlight", "artist", "feature", "deep dive"),
            aliases = listOf("spotlight", "featured artist"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Allow user to pin a spotlight for the week.",
        )

        val GENRE_DEEP_DIVE = CollectionMetadata(
            title = "Genre Deep Dive",
            subtitle = "Explore a sound",
            description = "A deep dive into a specific genre in your " +
                "library.",
            developerNotes = "Engine: GenreStrategy. Source: " +
                "Song.genre grouped.",
            userExplanation = "A tour through a genre you have music in.",
            searchKeywords = listOf("genre", "deep dive", "explore", "category"),
            aliases = listOf("genre", "deep dive"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Needs genre detection from ID3 + audio features.",
        )

        val DECADE_MIX = CollectionMetadata(
            title = "Decade Mix",
            subtitle = "Sound of an era",
            description = "Tracks from a specific decade in your library.",
            developerNotes = "Engine: DecadeStrategy. Source: " +
                "Song.year grouped by decade.",
            userExplanation = "The sound of an era — from your library.",
            searchKeywords = listOf("decade", "era", "year", "90s", "80s", "2000s"),
            aliases = listOf("decade", "era", "by year"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Detect decade from file metadata fallback.",
        )

        val TIME_TRAVEL = CollectionMetadata(
            title = "Time Travel",
            subtitle = "What you played, when",
            description = "Your listening history, organized by time.",
            developerNotes = "Engine: HistoryStrategy. Source: " +
                "PlaybackHistory grouped by time bucket.",
            userExplanation = "Revisit what you were playing a week, " +
                "month, or year ago.",
            searchKeywords = listOf("time travel", "history", "past", "rewind"),
            aliases = listOf("time travel", "rewind", "back then"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Add 'on this day' variant for daily nostalgia.",
        )

        val HISTORY_FULL = CollectionMetadata(
            title = "Full History",
            subtitle = "Every play, ever",
            description = "Your complete playback history, newest first.",
            developerNotes = "Source: PlaybackHistory table.",
            userExplanation = "Every track you've ever played, in order.",
            searchKeywords = listOf("history", "log", "playback", "all plays"),
            aliases = listOf("history", "playback log"),
            version = "1.0.0",
            createdFor = "B1",
            futureNotes = "Allow export to CSV for personal analytics.",
        )

        // ─────────────────────────────────────────────────────────
        // EXPERIMENTAL / FUTURE
        // ─────────────────────────────────────────────────────────

        val RIKO_RADIO = CollectionMetadata(
            title = "RIKO Radio",
            subtitle = "Endless, evolving",
            description = "An endless stream of music generated from your " +
                "library — always playing, always changing.",
            developerNotes = "Engine: RadioStrategy. Continuous " +
                "generation based on listening patterns.",
            userExplanation = "An endless radio station built from your " +
                "library. It never stops, never repeats.",
            searchKeywords = listOf("radio", "endless", "stream", "station"),
            aliases = listOf("radio", "station", "endless"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Allow thumbs-up/down to refine future picks.",
        )

        val ON_THIS_DAY = CollectionMetadata(
            title = "On This Day",
            subtitle = "What you played today, in years past",
            description = "Tracks you played on this date in previous years.",
            developerNotes = "Engine: HistoryStrategy. Source: " +
                "PlaybackHistory filtered by month and day.",
            userExplanation = "A little nostalgia: what you were playing " +
                "on this day in years past.",
            searchKeywords = listOf("on this day", "anniversary", "memory", "nostalgia"),
            aliases = listOf("on this day", "memory"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Surface year-badge on cards.",
        )

        val DIAGNOSTIC_ORPHANS = CollectionMetadata(
            title = "Orphan Tracks",
            subtitle = "Diagnostic only",
            description = "Tracks with missing metadata, missing art, or " +
                "corrupt audio. Not for user consumption.",
            developerNotes = "Engine: DiagnosticStrategy. Source: " +
                "integrity check pass over Song table.",
            userExplanation = "(developer only) Tracks that need attention.",
            searchKeywords = listOf("orphan", "broken", "missing", "diagnostic"),
            aliases = listOf("orphan", "broken"),
            version = "0.1.0",
            createdFor = "B3",
            futureNotes = "Add 'repair' action per orphan.",
        )
    }
}
