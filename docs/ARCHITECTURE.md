# Architecture decisions

## Boundaries

| Module | Responsibility |
|---|---|
| app | Activity/application, Hilt wiring, ViewModel, navigation, screens, clipboard and Play update lifecycle |
| core:model | Serializable catalog contracts, source badges, filtering, ID-based history rules and validation |
| core:data | Read-only bundled assets, cached catalog load and local DataStore persistence |
| core:designsystem | Semantic light palette, typography, shapes and shared Compose dependencies |

The application graph is Activity → Compose navigation → screen callbacks → ViewModel → repository. The repository exposes preferences through Flow; screen collection follows the lifecycle. Search/category values use SavedStateHandle. Navigation restores tab state and lazy-list position. Modal selection uses rememberSaveable.

## Data and persistence

catalog.json is the packaged source of truth, parsed with kotlinx.serialization on Dispatchers.IO and cached under a Mutex. The UI mapping adds chips/vehicle-shortcut grouping without rewriting source categories. Code is always String. Item ID, never code, is used for favorites, recents and LazyColumn keys. Superseded same-code IDs remain archived and migrate saved IDs to canonical entries; meaningful duplicate weather stages remain separate.

DataStore provides transactional edits for small user collections and preferences. History stores the newest copy time for each item and caps the list at 50. Favorites store stable IDs. This scale does not need a mutable Room catalog, database migrations or a server. The catalog itself is immutable and shipped as an app asset.

A corrupt DataStore file resets to safe defaults; ordinary I/O read failures expose defaults. Failed writes emit a visible snackbar. Uninstalling removes local data; Android cloud backup is disabled in the manifest.

## UI and performance

LazyColumn renders cards with stable IDs; search is memoized by catalog/query/category. Catalog parsing is off the main thread. Coil uses local asset URIs and its shared loader/cache; thumbnail bounds are reserved. Special filename characters such as # are encoded by Uri.Builder. Missing images retain a neutral placeholder.

UI follows the supplied light purple-blue reference with system typography, rounded surfaces, restrained borders and Material interactions. No web rendering, remote fonts, perpetual decorative animations or blur. Content width is capped on larger displays. Text can grow vertically; controls retain Android touch targets. Settings persist the accent, not the entire theme tree.

## Updates

PlayUpdates is Activity-owned so registration occurs before STARTED and its install listener is unregistered on destroy. It requests fresh update info per check/retry and starts only the Flexible flow. Download status is restored on resume; completion requires an explicit restart action. The manager can be replaced with Google's FakeAppUpdateManager in tests. This fake does not establish real Play eligibility.

Content changes require an ordinary app release. There is no Firebase, remote catalog fetch, account system, ad SDK or custom backend.

## Accuracy and motion revision

CatalogData creates a cached ID index on the repository IO dispatcher. UI memoizes collection previews and uses stable lazy keys/content types. Cheat metadata includes evidence/verification without a schema break. Wrong-game records stay addressable for stored IDs while filterCodes excludes them from browsing. GameStore isolates external listing intents and fallback from UI for testing. Compose animation handles motion duration scaling; no animation logic lives in the ViewModel.

## Resource, notice and reminder integration

Section separates codes, RGS commands, plugin URLs and local models. DataStore persists notice acknowledgement and reminder preference. First-use countdown runs only while RESUMED; About retains the notice. Acknowledgement grants no asset rights.

ModelFiles streams on IO with cancellation/256 MiB limit. UnityFS and GLB2 headers are checked; bytes are not rendered or executed. Large assets are reassembled on demand under a mutex into versioned cache. Read-only DocumentsProvider exposes packaged files via SAF; FileProvider grants a selected file to a matching game ACTION_VIEW receiver if advertised. Custom models stay private and are exportable through CreateDocument. Successful intent dispatch does not prove import. No AccessibilityService, overlay, root or game UI automation is used.

CopyReminder keeps only the latest in-app copy in memory. MainActivity cancels on foreground and consumes copies aged at most ten seconds on background, excluding configuration changes. Same notification ID posts with seven-second timeout, updates at two seconds and silently at four, then cancels at seven. Return/manual dismissal is respected. Delayed callbacks are best effort under process suspension/death; OS timeout is the fallback. Android controls actual heads-up/DND/cooldown.
