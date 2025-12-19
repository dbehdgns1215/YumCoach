import { createRouter, createWebHistory } from "vue-router";

import HomePage from "@/pages/HomePage.vue";
import LogPage from "@/pages/LogPage.vue";
import ReportPage from "@/pages/ReportPage.vue";
import CoachPage from "@/pages/CoachPage.vue";
import LandingPage from "@/pages/LandingPage.vue";

const navItems = [
  { key: "home", path: "/home", label: "오늘" },
  { key: "log", path: "/log", label: "기록" },
  { key: "report", path: "/report", label: "리포트" },
  { key: "coach", path: "/coach", label: "코치" },
];

const routes = [
  // Landing is standalone (no AppShell nav)
  { path: "/landing", component: LandingPage },
  { path: "/", redirect: "/landing" },
  {
    path: "/home",
    component: HomePage,
    meta: { navKey: "home", navLabel: "오늘", navItems },
  },
  {
    path: "/log",
    component: LogPage,
    meta: { navKey: "log", navLabel: "기록", navItems },
  },
  {
    path: "/report",
    component: ReportPage,
    meta: { navKey: "report", navLabel: "리포트", navItems },
  },
  {
    path: "/coach",
    component: CoachPage,
    meta: { navKey: "coach", navLabel: "코치", navItems },
  },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
