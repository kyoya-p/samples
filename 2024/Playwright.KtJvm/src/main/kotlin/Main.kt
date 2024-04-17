import com.microsoft.playwright.*
import com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat
import com.microsoft.playwright.options.AriaRole
import java.util.regex.Pattern


fun main(args: Array<String>) {
    Playwright.create().use { playwright ->
        val browser = playwright.chromium().launch(BrowserType.LaunchOptions().apply { headless = false })
        val page = browser.newPage()
        page.navigate("http://playwright.dev")

        assertThat(page).hasTitle(Pattern.compile("Playwright"))

        val getStarted = page.getByRole(AriaRole.LINK, Page.GetByRoleOptions().setName("Get Started"))

        assertThat(getStarted).hasAttribute("href", "/docs/intro")

        getStarted.click()

        assertThat(
            page.getByRole(
                AriaRole.HEADING,
                Page.GetByRoleOptions().setName("Installation")
            )
        ).isVisible()
    }
}