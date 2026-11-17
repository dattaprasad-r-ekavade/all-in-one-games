# Google Play policy form drafts

Status: Working draft for the first closed-test build. Recheck against the final release bundle and Play Console before submission.
Last checked: 17 November 2026

These are draft answers, not submitted declarations. Google Play requires the Data safety form to include data handled by third-party SDKs. Keep this document aligned with the published privacy policy and all SDKs in the final artifact.

## App content and ads declaration

| Play Console field | Draft answer | Evidence / follow-up |
|---|---|---|
| Does the app contain ads? | Yes | The app integrates the Google Mobile Ads Next-Gen SDK. Debug and closed-test builds use Google's public test IDs; use live IDs only for the production build after H6. |
| Ads in the app | Interstitial ads may appear after the player taps Next on a result screen. Optional rewarded ads provide a named reward. No ad appears while a puzzle is active. | `DEVELOPMENT_PLAN.md` sections 8.1 and 8.2; `GoogleAdService` and the ad-flow screens. |
| In-app purchases | Yes: optional one-time, non-consumable Premium purchase through Google Play Billing. | `DEVELOPMENT_PLAN.md` section 8.5; `PlayBillingService`. Premium activation still depends on H4 creating the Play product and tester. |
| Target audience | 13 and older; do not select any age group under 13. | Product plan section 8.7. Confirm the final age-group choices in Play Console. |
| App access | No sign-in or special access is required. | Current app has no account flow. |
| Website and privacy policy | Pending H0.1. | Add the public HTTPS privacy-policy URL and support contact before submitting a form or release. |

Keep ad content at or below the eventual IARC rating. Set the AdMob content rating controls after the account and ad units exist.

## Data safety draft

### What the current app does

- The app has no account, profile, chat, user-posting, photo, contacts, or location feature.
- Puzzle state, level progress, times, daily completion history, streaks, settings, and the local Premium entitlement are stored in Room or DataStore. The app does not send these values to a Witgrid server.
- Android cloud backup and device transfer are enabled for the Room database and settings file. Confirm how the final Play form treats this system backup path and describe it consistently in the privacy policy.
- The merged app includes Google Mobile Ads Next-Gen and UMP. The GMA SDK automatically collects and shares the data types below when an ad request is made and the SDK is allowed to request ads. UMP controls consent and privacy choices; its exact production message configuration is not set up until H4.
- The app also uses Google Play Billing for the optional Premium transaction. The app stores only the resulting Premium entitlement locally. Review the final Billing flow and Play Console wording for purchase-history disclosure; the app does not read or store card numbers or billing addresses.
- Firebase Analytics, Crashlytics, and Remote Config are not integrated in the current app. M7 will add them. Analytics and Crashlytics must remain disabled until the player opts in, as decided for M7. Update this form after M7 and inspect the release dependency/manifest reports.

### Current SDK data to include in the draft

| Play data type | Draft collection / sharing | Purpose | SDK evidence |
|---|---|---|---|
| Approximate location | Collected and shared by the GMA SDK through IP address processing. | Advertising, analytics, fraud prevention | Google’s Next-Gen disclosure says IP address may be used to estimate general location. |
| App interactions | Collected and shared by the GMA SDK, including app launches, taps, and video views. | Advertising, analytics, fraud prevention | Google’s Next-Gen disclosure. |
| Diagnostics | Collected and shared by the GMA SDK, including app/SDK performance information. | Advertising, analytics, fraud prevention | Google’s Next-Gen disclosure. |
| Device or other IDs | Collected and shared by the GMA SDK, including Android Advertising ID, App Set ID, and applicable signed-in account identifiers. | Advertising, analytics, fraud prevention | Google’s Next-Gen disclosure. |
| Financial info — purchase history | Review and likely include for the Billing transaction/entitlement flow. The exact collection/sharing answer must match the final Play Billing flow and questionnaire. | App functionality (Premium entitlement) | Play’s data-use guide lists use of the Billing Library as a financial-data review point. The app does not collect payment-card details. |

For GMA data, Google says the SDK encrypts data in transit with TLS. Confirm the form’s encryption answer against every transmitted data path in the release bundle. Do not mark GMA data “optional” just because ads are optional to the player: use the Play form’s definition and the final consent/ad-serving behavior across regions.

### Future M7 data to add after implementation

- **Analytics:** after explicit Settings opt-in, the planned events describe app/game interactions (game, pack, level ID, elapsed time, hints, mistakes, ad/reward actions, and daily/streak events). Firebase Analytics also has automatic collection and identifiers; include those according to the current Firebase disclosure and actual configuration. Keep all collection disabled before opt-in and after opt-out.
- **Crash reports:** after explicit Settings opt-in, Crashlytics can send crash stacks, relevant application/device state, and an installation identifier. Do not attach free-text, personal identifiers, or puzzle-state contents to crash reports.
- **Remote Config:** determine whether it makes network requests before Analytics/Crashlytics opt-in. Its SDK has its own data disclosures, including app/device configuration and installation data. Keep the app’s bundled defaults usable offline.
- Rebuild this table from the final dependency graph, SDK versions, merged manifest, and observed settings behavior before the Play declaration is submitted.

### Draft answers for the form summary

| Form question | Draft |
|---|---|
| Does the app collect or share required user data types? | Yes, due to the ads SDK; review purchase-history treatment for Billing. |
| Is data encrypted in transit? | Yes for GMA SDK data per Google's disclosure. Verify against the complete final artifact before selecting Yes for the app as a whole. |
| Can users request deletion of collected data? | No developer account or server-side profile exists. Set a privacy contact in H0.1 and explain local-data removal and third-party SDK controls in the published privacy policy. Recheck the final form’s deletion question after SDK integration. |
| Is data collection optional? | Do not finalize yet. GMA collection depends on ad requests and consent, while Play requires the answer to cover all regions and distributed versions. Follow the final Play form guidance and final UMP/ad behavior. M7 analytics and Crashlytics are opt-in only. |
| Is any data used for advertising, analytics, or fraud prevention? | Yes for GMA SDK data. Analytics/Crashlytics purposes apply only after the M7 opt-in implementation and must be disclosed before release. |

## Content-rating questionnaire draft

The IARC questionnaire determines the displayed regional rating; do not enter a predicted rating as an answer. These drafts describe the current product content and must be checked against the live questionnaire before submission.

| Topic | Draft answer |
|---|---|
| App category | Game; puzzle. |
| Violence, fear, sexual content, nudity, profanity, drugs, tobacco, and alcohol | None. |
| Gambling or simulated gambling | None. No chance-based prize, betting, or gambling mechanic. |
| User-generated content or user-to-user communication | None. |
| Location sharing | None. The app requests no location permission. |
| Ads | Yes; ads are served by Google Mobile Ads. Keep ads suitable for the final IARC rating. |
| Purchases | Yes; a single optional non-consumable Premium upgrade. No loot boxes or randomized paid items. |

## Finalization checklist

- [ ] H0.1 supplies the developer name, public support contact, published privacy policy, and developer website.
- [ ] H4 supplies the production AdMob configuration, privacy message, Play product, and licence tester. Keep test IDs in closed-test builds.
- [ ] M7 is merged; Analytics and Crashlytics remain off until explicit opt-in, and Remote Config behavior is documented.
- [ ] Inspect the final release bundle's merged manifest and dependencies; confirm all SDK data disclosures and the Billing answer.
- [ ] Complete the live target-audience and content forms, then review the rating assigned by IARC.
- [ ] Ensure the privacy policy and Data safety answers describe the same collection, sharing, retention, deletion, and backup behavior.

## Official references

- [Play Data safety form requirements](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Google Mobile Ads Next-Gen Play data disclosure](https://developers.google.com/admob/android/next-gen/privacy/play-data-disclosure)
- [Android guidance for declaring app data use](https://developer.android.com/privacy-and-security/declare-data-use)
- [Firebase Android SDK data disclosures](https://firebase.google.com/docs/android/play-data-disclosure)
- [Play content-rating requirements](https://support.google.com/googleplay/android-developer/answer/9898843)
