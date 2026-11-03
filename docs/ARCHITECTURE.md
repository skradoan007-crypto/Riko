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

catalog.json is the unchanged source of truth, parsed with kotlinx.serialization on Dispatchers.IO and cached under a Mutex. The UI mapping adds chips/vehicle-shortcut grouping without rewriting source categories. Code is always String. Item ID, never code, is used for favorites, recents and LazyColumn keys. Duplicate 6666 entries remain separate.

DataStore provides transactional edits for small user collections and preferences. History stores the newest copy time for each item and caps the list at 50. Favorites store stable IDs. This scale does not need a mutable Room catalog, database migrations or a server. The catalog itself is immutable and shipped as an app asset.

A corrupt DataStore file resets to safe defaults; ordinary I/O read failures expose defaults. Failed writes emit a visible snackbar. Uninstalling removes local data; Android cloud backup is disabled in the manifest.

## UI and performance

LazyColumn renders cards with stable IDs; search is memoized by catalog/query/category. Catalog parsing is off the main thread. Coil uses local asset URIs and its shared loader/cache; thumbnail bounds are reserved. Special filename characters such as # are encoded by Uri.Builder. Missing images retain a neutral placeholder.

UI follows the supplied light purple-blue reference with system typography, rounded surfaces, restrained borders and Material interactions. No web rendering, remote fonts, perpetual decorative animations or blur. Content width is capped on larger displays. Text can grow vertically; controls retain Android touch targets. Settings persist the accent, not the entire theme tree.

## Updates

PlayUpdates is Activity-owned so registration occurs before STARTED and its install listener is unregistered on destroy. It requests fresh update info per check/retry and starts only the Flexible flow. Download status is restored on resume; completion requires an explicit restart action. The manager can be replaced with Google's FakeAppUpdateManager in tests. This fake does not establish real Play eligibility.

Content changes require an ordinary app release. There is no Firebase, remote catalog fetch, account system, ad SDK or custom backend.
