import { test, expect } from "@playwright/test";
const BASE="http://localhost:3002";
const TS=Date.now();

async function login(page) { await page.goto(BASE+'/login',{waitUntil:"networkidle"}); await page.waitForTimeout(1500); const inputs=page.locator("input"); const cnt=await inputs.count(); if(cnt>=2){await inputs.nth(0).fill("admin");await inputs.nth(1).fill("Admin@123");} const lb=page.locator("button").filter({hasText:/login/i}).first(); if(await lb.isVisible({timeout:3000}).catch(()=>false)){await lb.click();await page.waitForTimeout(3000);} await page.waitForURL(BASE+'/**',{timeout:15000}).catch(()=>{}); }

const PAGES=[["4.0 Overview","/operations"],["4.1 LiveMonitor","/operations/live-monitor"],["4.2 DecisionBoard","/operations/decision-board"],["4.3 Strategy","/operations/strategy-workshop"],["4.4 AlertCenter","/operations/alert-command-center"],["4.5 Reports","/operations/reports"],["4.6 OrderQuery","/order/query"],["4.7 OrderStats","/order/statistics"],["4.8 Refund","/order/refund"],["4.9 Reservation","/order/reservation"],["4.10 MemberOverview","/marketing/member-overview"],["4.11 MemberList","/marketing/member-list"],["4.12 MemberLevel","/marketing/member-level"],["4.13 Recharge","/marketing/recharge"],["4.14 RechargeSettings","/marketing/recharge-settings"]];

test.describe("Phase 4 Operations Orders Members",()=>{
  test.beforeEach(async({page})=>{await login(page);});
  for(const[name,path]of PAGES){
    test(name,async({page})=>{
      await page.goto(BASE+path,{waitUntil:"domcontentloaded"});
      await page.waitForTimeout(3000);
      const body=await page.locator("body").innerText().catch(()=>"");
      expect(body.length).toBeGreaterThan(0);
      const isErr=body.includes("404")||body.includes("Cannot GET");
      expect(isErr).toBe(false);
      await page.screenshot({path:"e2e/test-results/p4-"+name.replace(/\s+/g,"-")+"-"+TS+".png"});
    });
  }
});
