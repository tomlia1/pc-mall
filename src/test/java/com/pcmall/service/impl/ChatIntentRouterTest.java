package com.pcmall.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.List;

class ChatIntentRouterTest {
    private final ChatIntentRouter router = new ChatIntentRouter();

    @Test
    void routesLaptopWordsBeforeGamingBudgetBuildPlan() {
        ChatIntentRouter.RoutedIntent intent = router.route("8000 游戏本");

        assertEquals(ChatIntentRouter.Route.LAPTOP_SEARCH, intent.route());
        assertEquals("LAPTOP", intent.criteria().productType());
        assertEquals("游戏", intent.criteria().category());
        assertEquals("8000", intent.criteria().maxBudget().toPlainString());
    }

    @Test
    void routesOfficeLearningAndCodingToOfficeLaptopSearch() {
        for (String message : new String[]{"8000 办公", "8000 学习", "8000 写代码"}) {
            ChatIntentRouter.RoutedIntent intent = router.route(message);
            assertEquals(ChatIntentRouter.Route.LAPTOP_SEARCH, intent.route());
            assertEquals("LAPTOP", intent.criteria().productType());
            assertEquals("办公/编程", intent.criteria().category());
        }
    }

    @Test
    void routesBudgetGamingWithoutLaptopOrPartToBuildPlan() {
        assertEquals(ChatIntentRouter.Route.BUILD_PLAN, router.route("3000 吃鸡").route());
        assertEquals(ChatIntentRouter.Route.BUILD_PLAN, router.route("8000 玩 3A").route());
        assertEquals(ChatIntentRouter.Route.BUILD_PLAN, router.route("8000 DIY 装机").route());
    }

    @Test
    void routesExplicitPartAndDoesNotApplyLaptopCategory() {
        for (String message : new String[]{"5000 显卡", "推荐游戏显卡"}) {
            ChatIntentRouter.RoutedIntent intent = router.route(message);
            assertEquals(ChatIntentRouter.Route.PART_SEARCH, intent.route());
            assertEquals("GPU", intent.criteria().productType());
            assertNull(intent.criteria().category());
        }
    }

    @Test
    void routesUnspecifiedAccessoryRequestToClarifyWithoutCriteria() {
        ChatIntentRouter.RoutedIntent intent = router.route("推荐配件");

        assertEquals(ChatIntentRouter.Route.CLARIFY, intent.route());
        assertNull(intent.criteria());
    }

    @Test
    void normalizesDesignAndGamingLaptopUsesToTheCatalogCategories() {
        ChatIntentRouter.RoutedIntent design = router.route("9000 笔记本做剪辑");
        ChatIntentRouter.RoutedIntent gaming = router.route("9000 游戏本");

        assertEquals("设计/创作", design.criteria().category());
        assertEquals("游戏", gaming.criteria().category());
        assertEquals("LAPTOP", design.criteria().productType());
        assertEquals("LAPTOP", gaming.criteria().productType());
    }

    @Test
    void preservesAnUnnormalizedLaptopUseAsAKeywordForAlternativeMatching() {
        ChatIntentRouter.RoutedIntent intent = router.route("8000 CAD 笔记本");

        assertEquals(ChatIntentRouter.Route.LAPTOP_SEARCH, intent.route());
        assertNull(intent.criteria().category());
        assertEquals("cad", intent.criteria().keyword());
    }

    @Test
    void treatsBudgetOnlyQuestionsAsClarificationInsteadOfSearchingAnUnspecifiedCatalog() {
        assertEquals(ChatIntentRouter.Route.BUDGET_CLARIFY, router.route("2000可以干嘛").route());
        assertEquals(ChatIntentRouter.Route.BUDGET_CLARIFY, router.route("5000呢").route());
    }

    @Test
    void carriesTheLatestBudgetIntoAnExplicitLaptopFollowUp() {
        ChatIntentRouter.RoutedIntent intent = router.route("笔记本也没有？", List.of("2000可以干嘛", "5000呢"));

        assertEquals(ChatIntentRouter.Route.LAPTOP_SEARCH, intent.route());
        assertEquals("LAPTOP", intent.criteria().productType());
        assertEquals(new BigDecimal("5000"), intent.criteria().maxBudget());
        assertNull(intent.criteria().keyword());
    }
}
