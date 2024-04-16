
import com.microsoft.playwright.*
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import java.util.regex.Pattern


object App {
    @JvmStatic
    fun main(args: Array<String>) {
        Playwright.create().use { playwright ->
            val browser = playwright.chromium().launch()
            val page = browser.newPage()
            page.navigate("http://playwright.dev")

            // Expect a title "to contain" a substring.
            assertThat(page).hasTitle(Pattern.compile("Playwright"))

            // create a locator
            val getStarted = page.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Get Started"))

            // Expect an attribute "to be strictly equal" to the value.
            assertThat(getStarted).hasAttribute("href", "/docs/intro")

            // Click the get started link.
            getStarted.click()

            // Expects page to have a heading with the name of Installation.
            assertThat(
                page.getByRole(
                    AriaRole.HEADING,
                    Page.GetByRoleOptions().setName("Installation")
                )
            ).isVisible()
        }
    }
}

