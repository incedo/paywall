package nl.incedo.paywall.brands

import nl.incedo.paywall.core.DomainEvent

/** ADM-10: projection for a single brand — folds creation and theme events. */
class BrandDecision {
    var name: String = ""
        private set
    var domain: String = ""
        private set
    var locale: String = "nl-NL"
        private set
    var themeJson: String = "{}"
        private set

    val exists: Boolean get() = name.isNotEmpty()

    fun apply(event: DomainEvent) {
        when (event) {
            is BrandCreated -> {
                name = event.name
                domain = event.domain
                locale = event.locale
                themeJson = event.themeJson
            }
            is BrandThemeUpdated -> themeJson = event.themeJson
            else -> Unit
        }
    }

    fun applyAll(events: Iterable<DomainEvent>) = events.forEach(::apply)
}
