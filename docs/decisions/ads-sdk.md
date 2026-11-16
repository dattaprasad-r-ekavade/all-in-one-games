# Decision memo: Ads SDK (T0.3)

Status: recommendation, the human decides. Written 16 Nov 2026.
All facts below were read on **2026-11-16** from the linked page. Items marked **[Unverified]** have no clear source and need a check.

## 1. Context

- Plan 7.1 asks us to choose between `play-services-ads` (now "Legacy") and the Next-Gen SDK `ads-mobile-sdk`.
- Launch is AdMob only. Mediation with other networks is a later option, after about 1,000 to 2,000 daily players (plan 12).
- Needs: UMP consent before the first ad (8.7), one interstitial after "Next" (8.1), rewarded ads for hints and lives (8.2), offline-safe behaviour (8.3), test ad units only (AGENTS.md rule 5.2).
- Team: one developer. Income goal: $100 per month. Avoiding a forced rewrite matters more than small feature gaps now.

## 2. Facts

| Topic | Legacy `play-services-ads` | Next-Gen `ads-mobile-sdk` | Source |
|---|---|---|---|
| Status | "Legacy". Google calls Next-Gen the preferred SDK (6 Jul 2026). | Preferred SDK for new work. | [Google blog, 6 Jul 2026](https://developers.google.com/ads/blog/posts/2026/07/announcing-google-mobile-ads-next-gen) |
| Timeline (25.x.x) | Supported from 1 Jul 2026. Deprecated 30 Jun 2027 (ads serve, no support). Sunset 30 Jun 2028 (ads at risk, SDK reported outdated, blocks app releases). | Not stated by Google. A third-party article estimates deprecation Q1 2028 and sunset Q2 2029 for 1.x.x. **[Unverified]** | Google blog above; [eCorpIT](https://ecorpit.com/gma-next-gen-sdk-migration-android-ios-flutter-2026/) |
| Latest version | 25.5.0 (17 Sep 2026) | 1.6.0 (22 Oct 2026) | [Legacy release notes](https://developers.google.com/admob/android/rel-notes); [Next-Gen release notes](https://developers.google.com/admob/android/next-gen/rel-notes); [Next-Gen quick start](https://developers.google.com/admob/android/next-gen/quick-start) |
| Coordinates | `com.google.android.gms:play-services-ads` | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` | Quick starts: [legacy](https://developers.google.com/admob/android/quick-start), [Next-Gen](https://developers.google.com/admob/android/next-gen/quick-start) |
| Min SDK | 24 (raised in 25.5.0) | 24 | Release notes and quick starts above |
| Compile SDK | 35 or higher | 35 or higher (quick start). The migration page says 34. Our target is 36, so both pass. | [Next-Gen migration](https://developers.google.com/admob/android/next-gen/migration) |
| Kotlin | Kotlin or Java | Kotlin 1.9 or higher, or Java | Quick starts above |
| Initialise | Optional but advised. App ID in manifest. | Required before any ad load. Run on a background thread. App ID goes in `InitializationConfig`. UMP still needs the app ID in the manifest. | Next-Gen migration page |
| Interstitial | `InterstitialAd.load(...)`, then `show(activity)`. Main thread. Ads expire after one hour. | `InterstitialAdPreloader.start / pollAd / isAdAvailable / destroy`. Manual load is also available. Callbacks may run on a background thread. | [Legacy interstitial](https://developers.google.com/admob/android/interstitial); [Next-Gen interstitial](https://developers.google.com/admob/android/next-gen/interstitial); [eCorpIT](https://ecorpit.com/gma-next-gen-sdk-migration-android-ios-flutter-2026/) |
| Rewarded | `RewardedAd.load(...)`, then `show(activity, listener)`. | `RewardedAd.load(...)` or `RewardedAdPreloader`. `ad.show(activity) { reward -> }`. | [Next-Gen rewarded](https://developers.google.com/admob/android/next-gen/rewarded) |
| Preload | Manual cache. Reload each hour. | Built in (labelled beta on the rewarded page). SDK reloads after each poll, retries failures and refreshes ads before they expire. Google advises a buffer of 2. App-wide cap is 6 preloaded ads. | Next-Gen interstitial and rewarded pages |
| Ad expiry | One hour (documented). | The pages do not state a time. They say the SDK refreshes ads before expiry. Plan 8.3 "one hour" is documented for legacy only. | Pages above |
| UMP consent | UMP SDK. Check `canRequestAds()` before requests. | Same UMP flow. The page lists no UMP version. App ID needed in the manifest. | [Next-Gen privacy page](https://developers.google.com/admob/android/next-gen/privacy) |
| Test app ID | `ca-app-pub-3940256099942544~3347511713` | Same value | [Next-Gen quick start](https://developers.google.com/admob/android/next-gen/quick-start) |
| Test interstitial ID | `ca-app-pub-3940256099942544/1033173712` | Same value | Interstitial pages above |
| Test rewarded ID | `ca-app-pub-3940256099942544/5224354917` (not re-read on the legacy page **[Unverified]**) | `ca-app-pub-3940256099942544/5224354917` | [Next-Gen rewarded](https://developers.google.com/admob/android/next-gen/rewarded) |
| AdMob mediation | Works with all AdMob adapters. | Supported. Adapters still depend on legacy classes. Exclude `play-services-ads` and `play-services-ads-lite` globally to avoid duplicate-symbol errors. Add each partner to the privacy and messaging partner list. | [Next-Gen mediation](https://developers.google.com/admob/android/next-gen/mediation) |
| Third-party mediation platforms | Works. | Compatible: AdMost, TopOn, TradPlus VisiM, Unity LevelPlay. **Not compatible: AppLovin MAX, Chartboost Mediation, DT Fairbid, Gravite, Nimbus.** | Next-Gen migration page |
| Individual adapter pages | Many. | Pages exist for Unity Ads, Mintegral and Liftoff Monetize (found by search). I did not confirm pages for AppLovin or Meta under Next-Gen. **[Unverified]** | [Unity](https://developers.google.com/admob/android/next-gen/mediation/unity), [Mintegral](https://developers.google.com/admob/android/next-gen/mediation/mintegral) |

Known issues found:
- Next-Gen 1.6.0 (22 Oct 2026) fixed a bug where the reward callback could fire after the dismiss callback. Use 1.6.0 or later. Source: [Next-Gen release notes](https://developers.google.com/admob/android/next-gen/rel-notes).
- Google reported an incident from 20 Aug 2026: rewarded video failed on Android, with a partial mitigation on 28 Aug. The affected SDK is not named. Source: [PPC Land](https://ppc.land/google-admob-bug-halts-rewarded-video-on-android-apps-for-eight-days/). Check that it is closed before launch.
- Next-Gen features still in beta: picture-in-picture, swipeable interstitial. We do not need them. Source: Next-Gen release notes.
- Next-Gen general availability was planned for July 2026 ([Jan 2026 announcement](https://developers.google.com/ads/blog/posts/2026/01/announcing-google-mobile-ads-next-gen), seen in a search snippet). The July 2026 blog calls it preferred, so I treat it as released. I found no explicit "GA" line. **[Unverified]**

## 3. Options

| | A. Legacy 25.5.0 | B. Next-Gen 1.6.0 |
|---|---|---|
| Works today | Yes | Yes |
| Support window | Deprecated 30 Jun 2027, sunset 30 Jun 2028 | Long. New releases about each month. |
| Migration cost later | Forced rewrite before mid 2028, with more games added meanwhile | None |
| Preload code we write | Own cache plus one-hour expiry timer | Use built-in preloaders. Smaller `AdService`. |
| AdMob mediation later | Yes | Yes, with the exclude rule |
| AppLovin MAX later | Yes | **No** (not compatible today) |
| Doc and sample quality | Mature | Newer, some pages still thin |

## 4. Recommendation

**Use the Next-Gen SDK (`ads-mobile-sdk` 1.6.0 or later) for Witgrid.**

Reasons:
1. Legacy is deprecated in 7 months and sunsets in 19 months. A new app should not start on it. One developer cannot afford a forced rewrite.
2. The plan stays with AdMob only, then AdMob mediation. That path is supported on Next-Gen.
3. The built-in preloaders fit task M3. They reload and refresh ads, so we write less expiry code.
4. Test ad IDs, the UMP flow, min API 24 and target API 36 work the same. The cost is small.

Design rule for M3: wrap the SDK behind the `AdService` interface (AGENTS.md section 4). Keep our own "drop ads older than one hour" check (task M3 text). Then the SDK choice stays hidden and we can swap it if a problem appears.

Mediation rule for later: use **AdMob mediation only**. Do not plan on AppLovin MAX. Add networks through AdMob adapters, one at a time, after checking each adapter page.

## 5. Risks

| Risk | Impact | Handling |
|---|---|---|
| Preload API is labelled beta and its expiry time is not documented | An expired ad may show as "loaded" | Keep our one-hour check. Treat a null `pollAd` as "no ad" and continue with no delay (plan 8.1). |
| Callbacks may run on a background thread | UI crash | Post to the main thread inside `AdService`. |
| Fewer samples and answers online than legacy | Slower debugging | Use Google's official example repo. Keep the wrapper thin. |
| Adapters still pull legacy classes | Build fails with duplicate symbols | Add the global exclude rule when the first adapter is added. |
| AppLovin MAX is not compatible | Fewer network choices later | Accept. AdMob mediation covers the $100 per month goal. |
| Next-Gen bug fixes are still frequent (reward callback order fixed in 1.6.0) | Rewards double-count or drop | Make the reward grant idempotent. Store the reward when the reward callback fires (plan 8.2). |

Fallback: if Next-Gen blocks us with a bug we cannot work around, switch the `AdService` implementation to legacy 25.5.0. The interface does not change.

## 6. Verify again before launch

1. Current versions of both SDKs, and the Next-Gen release notes since 1.6.0.
2. The legacy timeline (1 Jul 2026, 30 Jun 2027, 30 Jun 2028) is unchanged.
3. Whether the Next-Gen docs now state the preloaded ad expiry time and drop the "beta" label on preload.
4. The Aug 2026 rewarded video incident is closed, and whether it touched Next-Gen.
5. The UMP SDK version that the Next-Gen docs advise, and a full consent test (EEA test device, reset consent).
6. The mediation compatibility table, and the adapter page for each network we want (AppLovin, Meta, Mintegral, Unity Ads, LevelPlay).
7. Test ads show on a real device with test IDs only. No live ID is in the repository.
8. The Play Data safety form and `app-ads.txt` match the SDK we ship.

## 7. Sources (all accessed 2026-11-16)

- https://developers.google.com/ads/blog/posts/2026/07/announcing-google-mobile-ads-next-gen
- https://developers.google.com/ads/blog/posts/2026/01/announcing-google-mobile-ads-next-gen
- https://developers.google.com/admob/android/next-gen/quick-start
- https://developers.google.com/admob/android/next-gen/migration
- https://developers.google.com/admob/android/next-gen/rel-notes
- https://developers.google.com/admob/android/next-gen/interstitial
- https://developers.google.com/admob/android/next-gen/rewarded
- https://developers.google.com/admob/android/next-gen/privacy
- https://developers.google.com/admob/android/next-gen/mediation
- https://developers.google.com/admob/android/quick-start
- https://developers.google.com/admob/android/interstitial
- https://developers.google.com/admob/android/rel-notes
- https://ecorpit.com/gma-next-gen-sdk-migration-android-ios-flutter-2026/ (third party)
- https://ppc.land/google-admob-bug-halts-rewarded-video-on-android-apps-for-eight-days/ (third party)
