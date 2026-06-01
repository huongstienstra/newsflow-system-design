# Technical Decisions

## 1. Use MVVM With a Repository Boundary

Decision: keep screen state in ViewModels and data orchestration in a repository.

Reason: feed loading, pagination, cache replacement, and offline fallback are product rules. Putting those rules in the repository keeps Compose screens simple and makes behavior easier to test.

## 2. Normalize Articles in Room

Decision: store article content once and connect it to feeds through a join table.

Reason: the same article can appear in multiple searches or pages. Normalization avoids duplicate article records and lets favorite state stay stable.

## 3. Keep Favorites Separate From Cache Membership

Decision: store favorites in their own table.

Reason: users expect favorites to survive refreshes, failed network calls, and feed cache replacement.

## 4. Hide Firebase Behind an Interface

Decision: ViewModels call `AnalyticsTracker`, not Firebase directly.

Reason: analytics should be replaceable, optional in local builds, and easy to verify in unit tests.

## 5. Treat Analytics as a Privacy Boundary

Decision: do not send raw search terms or full article URLs.

Reason: analytics should answer product questions without collecting unnecessary user input.

## 6. Make Firebase Optional

Decision: apply the Google Services plugin only when `app/google-services.json` exists.

Reason: the open-source repo should build without private Firebase configuration.

## 7. Map Errors Into Domain Failures

Decision: convert Retrofit and API exceptions into app-specific failure types.

Reason: UI code should not need to understand HTTP exceptions or API payload details.

## 8. Use a Coverage Gate

Decision: enforce an 80% minimum line coverage gate with Kover.

Reason: this project is meant to show engineering discipline, so tests should be visible and repeatable.
