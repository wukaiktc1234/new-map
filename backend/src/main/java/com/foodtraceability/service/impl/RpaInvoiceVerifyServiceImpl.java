package com.foodtraceability.service.impl;

import com.foodtraceability.dto.InvoiceVerifyRequestDTO;
import com.foodtraceability.dto.InvoiceVerifyResultDTO;
import com.foodtraceability.service.InvoiceVerifyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * RPA发票验真服务实现
 * 
 * 使用Selenium自动化访问国家税务总局发票查验平台
 * 网址: https://inv-veri.chinatax.gov.cn/
 * 
 * 支持的发票类型:
 * - 增值税电子普通发票(10): 校验码后6位
 * - 增值税普通发票(04): 校验码后6位
 * - 增值税专用发票(01): 金额(不含税)
 * - 增值税电子专用发票(08): 金额(不含税)
 * - 全电发票(09): 价税合计
 */
@Service
public class RpaInvoiceVerifyServiceImpl implements InvoiceVerifyService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RpaInvoiceVerifyServiceImpl.class);
    private static final String SERVICE_NAME = "RPA-TaxVerify";
    private static final String VERIFY_URL = "https://inv-veri.chinatax.gov.cn/";
    @Value("${invoice.verify.rpa.enabled:true}")
    private boolean rpaEnabled;
    @Value("${invoice.verify.rpa.headless:true}")
    private boolean headless;
    @Value("${invoice.verify.rpa.timeout:30}")
    private int timeoutSeconds;
    private volatile WebDriver driver;
    private volatile WebDriverWait wait;
    private volatile boolean driverInitialized = false;
    private final Object driverLock = new Object();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (!rpaEnabled) {
            log.info("[RPA验真] RPA验真服务已禁用");
            return;
        }
        log.info("[RPA验真] RPA验真服务已启用，WebDriver将在首次使用时初始化");
    }

    private void ensureDriverInitialized() {
        if (!driverInitialized && rpaEnabled) {
            synchronized (driverLock) {
                if (!driverInitialized && rpaEnabled) {
                    try {
                        log.info("[RPA验真] 正在初始化WebDriver...");
                        WebDriverManager.chromedriver().setup();
                        ChromeOptions options = new ChromeOptions();
                        if (headless) {
                            options.addArguments("--headless=new");
                        }
                        options.addArguments("--no-sandbox");
                        options.addArguments("--disable-dev-shm-usage");
                        options.addArguments("--disable-gpu");
                        options.addArguments("--window-size=1920,1080");
                        options.addArguments("--disable-blink-features=AutomationControlled");
                        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                        driver = new ChromeDriver(options);
                        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
                        driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
                        wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
                        driverInitialized = true;
                        log.info("[RPA验真] WebDriver初始化成功");
                    } catch (Exception e) {
                        log.error("[RPA验真] WebDriver初始化失败: {}", e.getMessage());
                        rpaEnabled = false;
                    }
                }
            }
        }
    }

    @PreDestroy
    public void destroy() {
        if (driver != null) {
            try {
                driver.quit();
                log.info("[RPA验真] WebDriver已关闭");
            } catch (Exception e) {
                log.error("[RPA验真] WebDriver关闭失败: {}", e.getMessage());
            }
        }
    }

    @Override
    public InvoiceVerifyResultDTO verify(InvoiceVerifyRequestDTO request) {
        ensureDriverInitialized();
        if (!rpaEnabled || driver == null) {
            return InvoiceVerifyResultDTO.failed("RPA验真服务不可用");
        }
        log.info("[RPA验真] 开始验真发票: 代码={}, 号码={}", request.getInvoiceCode(), request.getInvoiceNo());
        try {
            String verifyValue = determineVerifyValue(request);
            if (verifyValue == null || verifyValue.isEmpty()) {
                return InvoiceVerifyResultDTO.failed("缺少必要的验真参数");
            }
            driver.get(VERIFY_URL);
            Thread.sleep(1000);
            selectInvoiceType(request.getInvoiceType());
            WebElement codeInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("fpdm")));
            codeInput.clear();
            codeInput.sendKeys(request.getInvoiceCode() != null ? request.getInvoiceCode() : "");
            WebElement noInput = driver.findElement(By.id("fphm"));
            noInput.clear();
            noInput.sendKeys(request.getInvoiceNo());
            WebElement dateInput = driver.findElement(By.id("kprq"));
            dateInput.clear();
            String dateStr = formatDate(request.getIssueDate());
            dateInput.sendKeys(dateStr);
            WebElement valueInput = driver.findElement(By.id("kjje"));
            valueInput.clear();
            valueInput.sendKeys(verifyValue);
            WebElement captchaInput = driver.findElement(By.id("yzm"));
            captchaInput.click();
            log.info("[RPA验真] 请在浏览器中输入验证码...");
            Thread.sleep(5000);
            WebElement submitBtn = driver.findElement(By.id("checkfp"));
            submitBtn.click();
            Thread.sleep(3000);
            return parseVerifyResult();
        } catch (TimeoutException e) {
            log.error("[RPA验真] 页面加载超时: {}", e.getMessage());
            return InvoiceVerifyResultDTO.failed("页面加载超时，请重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return InvoiceVerifyResultDTO.failed("验真过程被中断");
        } catch (Exception e) {
            log.error("[RPA验真] 验真失败: {}", e.getMessage(), e);
            return InvoiceVerifyResultDTO.failed("验真失败: " + e.getMessage());
        }
    }

    private void selectInvoiceType(String invoiceType) {
        try {
            WebElement typeSelect = driver.findElement(By.id("fplx"));
            typeSelect.click();
            Thread.sleep(500);
            String optionValue = mapInvoiceTypeToOption(invoiceType);
            WebElement option = driver.findElement(By.cssSelector("option[value=\'" + optionValue + "\']"));
            option.click();
        } catch (Exception e) {
            log.warn("[RPA验真] 选择发票类型失败，使用默认类型");
        }
    }

    private String mapInvoiceTypeToOption(String invoiceType) {
        if (invoiceType == null) return "10";
        return switch (invoiceType) {
            case "01", "02" -> "01";
            case "03" -> "03";
            case "04" -> "04";
            case "08" -> "08";
            case "10" -> "10";
            case "11" -> "11";
            case "14" -> "14";
            case "15" -> "15";
            case "09" -> "09";
            default -> "10";
        };
    }

    private String determineVerifyValue(InvoiceVerifyRequestDTO request) {
        String invoiceType = request.getInvoiceType();
        if ("09".equals(invoiceType)) {
            if (request.getTotalAmount() != null) {
                return request.getTotalAmount().toString();
            }
        }
        if ("01".equals(invoiceType) || "02".equals(invoiceType) || "08".equals(invoiceType)) {
            if (request.getAmountWithoutTax() != null) {
                return request.getAmountWithoutTax().toString();
            }
        }
        if ("03".equals(invoiceType)) {
            if (request.getAmountWithoutTax() != null) {
                return request.getAmountWithoutTax().toString();
            }
        }
        if ("15".equals(invoiceType)) {
            if (request.getTotalAmount() != null) {
                return request.getTotalAmount().toString();
            }
        }
        if (request.getCheckCode() != null && request.getCheckCode().length() >= 6) {
            return request.getCheckCode().substring(request.getCheckCode().length() - 6);
        }
        if (request.getAmountWithoutTax() != null) {
            return request.getAmountWithoutTax().toString();
        }
        return null;
    }

    private String formatDate(String date) {
        if (date == null) return "";
        date = date.replace("年", "").replace("月", "").replace("日", "").replace("-", "").replace("/", "");
        if (date.length() == 8) {
            return date;
        }
        return date;
    }

    private InvoiceVerifyResultDTO parseVerifyResult() {
        try {
            WebElement resultDiv = driver.findElement(By.id("result"));
            String resultText = resultDiv.getText();
            if (resultText.contains("发票信息不存在") || resultText.contains("查无此票")) {
                return InvoiceVerifyResultDTO.failed("发票信息不存在");
            }
            if (resultText.contains("超过") && resultText.contains("查验次数")) {
                return InvoiceVerifyResultDTO.failed("超过最大查验次数");
            }
            InvoiceVerifyResultDTO result = InvoiceVerifyResultDTO.success();
            try {
                WebElement fpdmEl = driver.findElement(By.xpath("//td[contains(text(),\'发票代码\')]/following-sibling::td"));
                result.setInvoiceCode(fpdmEl.getText().trim());
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement fphmEl = driver.findElement(By.xpath("//td[contains(text(),\'发票号码\')]/following-sibling::td"));
                result.setInvoiceNo(fphmEl.getText().trim());
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement kprqEl = driver.findElement(By.xpath("//td[contains(text(),\'开票日期\')]/following-sibling::td"));
                result.setIssueDate(kprqEl.getText().trim());
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement gfmcEl = driver.findElement(By.xpath("//td[contains(text(),\'购买方名称\')]/following-sibling::td"));
                result.setBuyerName(gfmcEl.getText().trim());
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement gfshEl = driver.findElement(By.xpath("//td[contains(text(),\'购买方纳税人识别号\')]/following-sibling::td"));
                result.setBuyerTaxNo(gfshEl.getText().trim());
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement xfmcEl = driver.findElement(By.xpath("//td[contains(text(),\'销售方名称\')]/following-sibling::td"));
                result.setSellerName(xfmcEl.getText().trim());
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement xfshEl = driver.findElement(By.xpath("//td[contains(text(),\'销售方纳税人识别号\')]/following-sibling::td"));
                result.setSellerTaxNo(xfshEl.getText().trim());
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement jeEl = driver.findElement(By.xpath("//td[contains(text(),\'金额\')]/following-sibling::td"));
                String jeStr = jeEl.getText().trim().replace("¥", "").replace(",", "").replace("元", "");
                result.setAmountWithoutTax(new BigDecimal(jeStr));
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement seEl = driver.findElement(By.xpath("//td[contains(text(),\'税额\')]/following-sibling::td"));
                String seStr = seEl.getText().trim().replace("¥", "").replace(",", "").replace("元", "");
                result.setTaxAmount(new BigDecimal(seStr));
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement jshjEl = driver.findElement(By.xpath("//td[contains(text(),\'价税合计\')]/following-sibling::td"));
                String jshjStr = jshjEl.getText().trim().replace("¥", "").replace(",", "").replace("元", "");
                result.setTotalAmount(new BigDecimal(jshjStr));
            } catch (NoSuchElementException ignored) {
            }
            try {
                WebElement zfbzEl = driver.findElement(By.xpath("//td[contains(text(),\'状态\')]/following-sibling::td"));
                String zfbz = zfbzEl.getText().trim();
                result.setInvalidFlag(zfbz.contains("作废") || zfbz.contains("红冲") ? "Y" : "N");
            } catch (NoSuchElementException ignored) {
            }
            result.setRawResponse(resultText);
            result.setVerifyTime(LocalDateTime.now());
            log.info("[RPA验真] 发票验真成功: {}", result.getInvoiceNo());
            return result;
        } catch (Exception e) {
            log.error("[RPA验真] 解析验真结果失败: {}", e.getMessage());
            return InvoiceVerifyResultDTO.failed("解析验真结果失败: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        ensureDriverInitialized();
        return rpaEnabled && driver != null;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }
}
