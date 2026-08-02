const { chromium } = require("playwright");
const path = require("path");

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  await page.goto("http://localhost:3002/login", { waitUntil: "networkidle", timeout: 15000 });
  await page.waitForTimeout(2000);
  
  // Dump page info
  console.log("URL:", page.url());
  console.log("Title:", await page.title());
  
  // Find all input elements
  const inputs = await page.locator("input").all();
  console.log(`\nFound ${inputs.length} input elements:`);
  for (const [i, inp] of inputs.entries()) {
    const name = await inp.getAttribute("name") || "";
    const placeholder = await inp.getAttribute("placeholder") || "";
    const type = await inp.getAttribute("type") || "";
    const id = await inp.getAttribute("id") || "";
    const cls = await inp.getAttribute("class") || "";
    console.log(`  [${i}] type=${type} name=${name} placeholder=${placeholder} id=${id}`);
  }
  
  // Find buttons
  const buttons = await page.locator("button, .el-button, [role='button']").all();
  console.log(`\nFound ${buttons.length} buttons:`);
  for (const [i, btn] of buttons.entries()) {
    const text = await btn.textContent() || "";
    const cls = await btn.getAttribute("class") || "";
    console.log(`  [${i}] text="${text.trim()}" class=${cls.slice(0,50)}`);
  }
  
  await page.screenshot({ path: "P:\\my-new-project\\frontend\\e2e\\test-results\\login-page.png" });
  console.log("\nScreenshot saved");
  
  await browser.close();
})();
