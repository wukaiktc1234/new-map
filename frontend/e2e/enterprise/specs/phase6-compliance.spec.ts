import { test, expect } from "@playwright/test";
const BASE="http://localhost:3002";
const TS=Date.now();

async function login(page) { await page.goto(BASE+"/login",{waitUntil:"networkidle"}); await page.waitForTimeout(1500); const inputs=page.locator("input"); const cnt=await inputs.count(); if(cnt>=2){await inputs.nth(0).fill("admin");await inputs.nth(1).fill("Admin@123");} const lb=page.locator("button").filter({hasText:/login/i}).first(); if(await lb.isVisible({timeout:3000}).catch(()=>false)){await lb.click();await page.waitForTimeout(3000);} await page.waitForURL(BASE+"/**",{timeout:15000}).catch(()=>{}); }

const P=[["6.0 Traceability","/traceability"],["6.1 Trace List","/traceability/list"],["6.2 Device List","/device/list"],["6.3 Device Monitor","/device/monitor"],["6.4 Seal","/seal"]];
test.describe("Phase6 Traceability Device Seal",()=>{test.beforeEach(async({page})=>{await login(page);});for(const[n,p]of P){test(n,async({page})=>{await page.goto(BASE+p,{waitUntil:"domcontentloaded"});await page.waitForTimeout(3000);const b=await page.locator("body").innerText().catch(()=>"");expect(b.length).toBeGreaterThan(0);expect(b.includes("404")||b.includes("Cannot GET")).toBe(false);await page.screenshot({path:"e2e/test-results/p6-"+n.replace(/\s+/g,"-")+"-"+TS+".png"});});}});
