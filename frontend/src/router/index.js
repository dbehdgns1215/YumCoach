import { createRouter, createWebHistory } from "vue-router";

import HomePage from "@/pages/HomePage.vue";
import LogPage from "@/pages/LogPage.vue";
import ReportPage from "@/pages/ReportPage.vue";
import CoachPage from "@/pages/CoachPage.vue";
import LandingPage from "@/pages/LandingPage.vue";
import LoginPage from "@/pages/LoginPage.vue";
import SignupPage from "@/pages/SignupPage.vue";
import MyPage from "@/pages/MyPage.vue";
import CommunityPage from "@/pages/CommunityPage.vue";
import ChallengePage from "@/pages/ChallengePage.vue";

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
  { path: "/login", component: LoginPage },
  { path: "/signup", component: SignupPage },
  {
    path: "/home",
    component: HomePage,
    meta: { requiresAuth: true, navKey: "home", navLabel: "오늘", navItems },
  },
  {
    path: "/log",
    component: LogPage,
    meta: { requiresAuth: true, navKey: "log", navLabel: "기록", navItems },
  },
  {
    path: "/report",
    component: ReportPage,
    meta: { requiresAuth: true, navKey: "report", navLabel: "리포트", navItems },
  },
  {
    path: "/coach",
    component: CoachPage,
    meta: { requiresAuth: true, navKey: "coach", navLabel: "코치", navItems },
  },
  {
    path: "/mypage",
    component: MyPage,
    meta: { requiresAuth: true },
  },
  {
    path: "/community",
    component: CommunityPage,
  },
  {
    path: "/challenge",
    component: ChallengePage,
  },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
