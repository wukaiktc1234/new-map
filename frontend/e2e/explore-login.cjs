const { chromium } = require("playwright");

(async () => {
  const browser = await chromium.launch({ headless: true, channel: "chrome" });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  await page.goto("http://localhost:3002/login", { waitUntil: "networkidle", timeout: 15000 });
  await page.waitForTimeout(2000);
  
  console.log("URL:", page.url());
  console.log("Title:", await page.title());
  
  const inputs = await page.locator("input").all();
  console.log(`\nFound ${inputs.length} inputs:`);
  for (const [i, inp] of inputs.entries()) {
    const placeholder = await inp.getAttribute("placeholder") || "";
    const type = await inp.getAttribute("type") || "";
    console.log(`  [${i}] type=${type} placeholder="${placeholder}"`);
  }
  
  const buttons = await page.locator("button").all();
  console.log(`\nFound ${buttons.length} buttons:`);
  for (const [i, btn] of buttons.entries()) {
    const text = await btn.textContent() || "";
    if (text.trim()) console.log(`  [${i}] text="${text.trim().slice(0,30)}"`);
  }
  
  await page.screenshot({ path: "P:\\my-new-project\\frontend\\e2e\\test-results\\login-page.png" });
  console.log("Screenshot saved!");
  
  await browser.close();
})();
