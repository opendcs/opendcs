package selenium;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Disabled("For demonstration purposes currently.")
@Tag("integration")
public class LoginTest {

    @Test
    public void main() {
        String browsersToTest = System.getProperty("opendcs.selenium.browsers");
        List<String> browsers = Arrays.asList(browsersToTest.split(","));

        for (String browser : browsers) {

            //This is just for demonstration purposes that the different browsers can be tested.  We can do this through
            //JUnit Parameterized Tests.
            //ALSO It would be nice to use WebDriverManager to automoatically download the version of webdriver needed
            WebDriver driver = null;
            if (browser.equalsIgnoreCase("firefox")) {
                System.setProperty("webdriver.gecko.driver", System.getProperty("opendcs.selenium.webdriver.firefox.path"));// "C:\\Users\\jonas\\.cache\\selenium\\geckodriver\\win64\\0.34.0\\geckodriver.exe");
                driver = new FirefoxDriver();
            }
            else if (browser.equalsIgnoreCase("chrome"))
            {
                System.setProperty("webdriver.chrome.driver", System.getProperty("opendcs.selenium.webdriver.chrome.path"));
                driver = new ChromeDriver();
            }
            else
            {
                System.out.println("Basic Authentication login test failed!  You need to give it a browser to test.\n"
                    + "It never started the test.");
            }

            // Set implicit wait to handle any delays in finding elements
            driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

            // Open the login page
            String baseUrl = System.getProperty("opendcs.webclient.url") + "/portal/login"; // Change to your actual URL
            driver.get(baseUrl);

            try {
                // Attempt to locate the SSO login button by its ID
                WebElement ssoLoginButton = driver.findElement(By.id("ssoLoginButton"));
                System.out.println("SSO login detected. Testing SSO login...");

                // Click the SSO login button
                ssoLoginButton.click();

                // Pause to allow redirection (adjust time as necessary)
                Thread.sleep(3000);

                // Verify redirection to the expected URL (change "/platforms" based on your app)
                if (driver.getCurrentUrl().contains("/platforms")) {
                    System.out.println("SSO login test passed!");
                } else {
                    System.out.println("SSO login test failed!");
                }
            } catch (Exception e) {
                System.out.println("No SSO login detected. Testing Basic Authentication...");

                // Fall back to testing Basic Authentication
                try {
                    // Locate username and password fields by their IDs
                    WebElement usernameField = driver.findElement(By.id("id_username"));
                    WebElement passwordField = driver.findElement(By.id("id_password"));

                    // Input test credentials (replace with valid credentials)
                    usernameField.sendKeys(System.getProperty("opendcs.webclient.authentication.basic.username"));  // Replace with actual username
                    passwordField.sendKeys(System.getProperty("opendcs.webclient.authentication.basic.password"));  // Replace with actual password

                    // Submit the login form by clicking the login button
                    WebElement loginButton = driver.findElement(By.id("loginButton"));
                    loginButton.click();

                    // Pause to allow the next page to load
                    Thread.sleep(3000);

                    // Verify successful login by checking the URL
                    if (driver.getCurrentUrl().contains("/platforms")) {
                        System.out.println("Basic Authentication login test passed!");
                    } else {
                        System.out.println("Basic Authentication login test failed!");
                    }
                } catch (Exception ex) {
                    System.out.println("Basic Authentication login test encountered an error: " + ex.getMessage());
                }
            } finally {
                // Close the browser
                driver.quit();
            }
        }
    }
}