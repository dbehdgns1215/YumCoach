import { createRouter, createWebHistory } from "vue-router";

import HiddenPage from "@/pages/HiddenPage.vue";
import LogPage from "@/pages/LogPage.vue";
import ReportPage from "@/pages/ReportPage.vue";
import CoachPage from "@/pages/CoachPage.vue";
import LandingPage from "@/pages/LandingPage.vue";
import LoginPage from "@/pages/LoginPage.vue";
import SignupPage from "@/pages/SignupPage.vue";
import MyPage from "@/pages/MyPage.vue";
import CommunityPage from "@/pages/CommunityPage.vue";
import CommunityPostDetail from "@/components/community/CommunityPostDetail.vue";
import CommunityWrite from "@/components/community/CommunityWrite.vue";
import ChallengePage from "@/pages/ChallengePage.vue";
import PaymentSuccessPage from "@/pages/PaymentSuccessPage.vue";
import PaymentFailPage from "@/pages/PaymentFailPage.vue";

const navItems = [
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
    path: "/mini",
    component: HiddenPage,
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
    meta: {
      requiresAuth: true,
      navKey: "report",
      navLabel: "리포트",
      navItems,
    },
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
    path: "/community/write",
    component: CommunityWrite,
    meta: { requiresAuth: true },
  },
  {
    path: "/community/:id",
    component: CommunityPostDetail,
    meta: { requiresAuth: true },
  },
  {
    path: "/challenge",
    component: ChallengePage,
  },
  {
    path: "/payment/success",
    name: "PaymentSuccess",
    component: PaymentSuccessPage,
  },
  {
    path: "/payment/fail",
    name: "PaymentFail",
    component: PaymentFailPage,
  },
  {
    path: "/auth/kakao/callback",
    component: () => import("@/pages/KakaoCallbackPage.vue"),
  },
];

export default createRouter({
  history: createWebHistory(),
  routes,
});
