package org.fireflink.cloud.Caps;

import com.sun.management.OperatingSystemMXBean;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Open_Browser_ParallelRun {
    private static final String HUB_URL = "http://103.182.210.85:4444";
    private static final String TEST_URL = "https://demowebshop.tricentis.com/";
    private static final String PLATFORM_NAME = "Windows 10";
    private static final String BROWSER_VERSION = "136";
    private static final int INVOCATION_COUNT = 1;
    private static final int THREAD_POOL_SIZE = 1;
    private static final int IMPLICIT_WAIT = 10;
    private static final int EXPLICIT_WAIT = 10;
//    private static final String LOG_FILE_PATH = "C:\\Selenium Grid\\Log folder\\ExecutionLogforheadless.log";
   private static final String LOG_FILE_PATH = "C:\\Selenium Grid\\Log folder\\ExecutionLogforGUI.log";

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();

    private WebDriver initializeDriver() throws MalformedURLException {
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.setBrowserVersion(BROWSER_VERSION);
        chromeOptions.setCapability(CapabilityType.PLATFORM_NAME, PLATFORM_NAME);
        WebDriver driver = new RemoteWebDriver(new URL(HUB_URL), chromeOptions);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT));
        driverThreadLocal.set(driver);
        return driver;
    }
    private WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    @Test(invocationCount = INVOCATION_COUNT, threadPoolSize = THREAD_POOL_SIZE)
    public void openBrowserAndNavigate() throws MalformedURLException {
        logResourceUsage("Before Execution");
        initializeDriver();
        WebDriver driver = getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(EXPLICIT_WAIT));
        try {
            driver.manage().window().maximize();
            driver.get(TEST_URL);
            logResourceUsage("After Page Load");
            WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(By.id("small-searchterms")));
            searchBox.click();
            searchBox.sendKeys("Laptop");
            Thread.sleep(2000);
            driver.findElement(By.xpath("//*[@class=\"button-1 search-box-button\"]")).click();
            logResourceUsage("After Search");

            String element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("Q"))).getText();
            Select select = new Select(driver.findElement(By.id("products-orderby")));
            select.selectByVisibleText("Price: Low to High");

            WebElement productElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//a[text()='14.1-inch Laptop']")));
            String productInPlp = productElement.getText();
            driver.findElement(By.xpath("//input[@value='Add to cart']")).click();
            driver.findElement(By.xpath("//span[text()='Shopping cart']")).click();
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[text()='Shopping cart']")));
            String productInCart = driver.findElement(By.xpath("//a[text()='14.1-inch Laptop']")).getText();
            System.out.println(productInPlp.contains(productInCart));
            logResourceUsage("After Adding to Cart");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
        } finally {
            logResourceUsage("After Execution");
            tearDown();
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        WebDriver driver = getDriver();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }

    private static void logResourceUsage(String stage) {
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long totalMemory = osBean.getTotalPhysicalMemorySize() / (1024 * 1024); // Convert to MB
        long freeMemory = osBean.getFreePhysicalMemorySize() / (1024 * 1024); // Convert to MB
        long usedMemory = totalMemory - freeMemory;
        double cpuLoad = osBean.getSystemCpuLoad() * 100; // Convert to percentage

        String logEntry = stage + " - CPU Usage: " + String.format("%.2f", cpuLoad) + "%, RAM Usage: " + usedMemory + "MB / " + totalMemory + "MB";

        System.out.println(logEntry);
        writeLogToFile(logEntry);
    }

    private static void writeLogToFile(String logEntry) {
        File logFile = new File(LOG_FILE_PATH);
        try {
            logFile.getParentFile().mkdirs();
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " - " + logEntry + "\n");
            }
        } catch (IOException e) {
            System.out.println("Failed to write log file: " + e.getMessage());
        }
    }
}