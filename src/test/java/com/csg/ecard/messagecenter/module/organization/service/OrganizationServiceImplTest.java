package com.csg.ecard.messagecenter.module.organization.service;

import com.csg.ecard.messagecenter.common.utils.RedisUtil;
import com.csg.ecard.messagecenter.framework.context.CurrentUserContext;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationNode;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationPath;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProperties;
import com.csg.ecard.messagecenter.infrastructure.organization.OrganizationProvider;
import com.csg.ecard.messagecenter.module.organization.vo.OrganizationLazyNodeVO;
import com.csg.ecard.messagecenter.module.organization.dto.OrganizationResolveDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrganizationServiceImplTest {

    private OrganizationProvider provider;
    private OrganizationServiceImpl service;

    @BeforeEach
    void setUp() {
        provider = mock(OrganizationProvider.class);
        OrganizationProperties properties = new OrganizationProperties();
        @SuppressWarnings("unchecked")
        ObjectProvider<RedisTemplate<String, Object>> redisProvider = mock(ObjectProvider.class);
        RedisUtil redisUtil = new RedisUtil(redisProvider);
        service = new OrganizationServiceImpl(provider, properties, redisUtil, new ObjectMapper());
        when(provider.parentOrgIdsWithChildren(anyCollection())).thenReturn(Set.of());
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void shouldQueryRootNodesAndBatchResolveHasChildren() {
        OrganizationNode root = node("100", null);
        when(provider.children(null)).thenReturn(List.of(root));
        when(provider.parentOrgIdsWithChildren(List.of("100"))).thenReturn(Set.of("100"));

        List<OrganizationLazyNodeVO> result = service.children(null);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getOrgId()).isEqualTo("100");
            assertThat(item.isHasChildren()).isTrue();
        });
        verify(provider).children(null);
        verify(provider).parentOrgIdsWithChildren(List.of("100"));
    }

    @Test
    void shouldQueryOnlyDirectChildren() {
        OrganizationNode inactive = node("103", "100");
        inactive.setState(0);
        when(provider.children("100")).thenReturn(List.of(
                node("101", "100"), node("102", "100"), inactive));

        List<OrganizationLazyNodeVO> result = service.children("100");

        assertThat(result).extracting(OrganizationLazyNodeVO::getOrgId).containsExactly("101", "102");
        verify(provider).children("100");
    }

    @Test
    void shouldMarkLeafNodeAsHavingNoChildren() {
        when(provider.children("100")).thenReturn(List.of(node("101", "100")));

        List<OrganizationLazyNodeVO> result = service.children("100");

        assertThat(result).singleElement().extracting(OrganizationLazyNodeVO::isHasChildren).isEqualTo(false);
    }

    @Test
    void shouldReturnEmptyForEmptyOrInvalidParent() {
        when(provider.children("leaf")).thenReturn(List.of());
        when(provider.children("missing")).thenReturn(List.of());

        assertThat(service.children("leaf")).isEmpty();
        assertThat(service.children("missing")).isEmpty();
    }

    @Test
    void shouldRestrictNodesToCurrentUserOrganizationScope() {
        CurrentUserContext.set(new CurrentUserContext.UserInfo("user", "用户", "100", "单位", null, null));
        OrganizationNode scopeRoot = node("100", "1");
        when(provider.resolve(List.of("100"))).thenReturn(List.of(new OrganizationPath(scopeRoot, List.of())));
        when(provider.isWithinScope("100", "100")).thenReturn(true);
        when(provider.isWithinScope("outside", "100")).thenReturn(false);

        assertThat(service.children(null)).extracting(OrganizationLazyNodeVO::getOrgId).containsExactly("100");
        assertThat(service.children("outside")).isEmpty();
        verify(provider, never()).children("outside");
    }

    @Test
    void shouldUseSingleBatchQueryForLargeSiblingSet() {
        List<OrganizationNode> nodes = IntStream.range(0, 1000)
                .mapToObj(index -> node(String.valueOf(1000 + index), "100"))
                .toList();
        when(provider.children("100")).thenReturn(nodes);

        assertThat(service.children("100")).hasSize(1000);

        verify(provider).children("100");
        verify(provider).parentOrgIdsWithChildren(anyCollection());
    }

    @Test
    void shouldHitCacheAndInvalidateChangedParent() {
        when(provider.children("100")).thenReturn(List.of(node("101", "100")));

        service.children("100");
        service.children("100");
        verify(provider).children("100");

        service.invalidateChildren(List.of("100"));
        service.children("100");
        verify(provider, times(2)).children("100");
    }

    @Test
    void shouldAvoidDuplicateQueryForConcurrentRequests() throws Exception {
        when(provider.children("100")).thenAnswer(invocation -> {
            Thread.sleep(30);
            return List.of(node("101", "100"));
        });
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch ready = new CountDownLatch(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            for (int i = 0; i < 8; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    service.children("100");
                    return null;
                });
            }
            assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
            start.countDown();
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        verify(provider).children("100");
    }

    @Test
    void shouldResolveMultipleOrganizationsAsFlatNodesAndIgnoreDuplicatesAndMissingIds() {
        OrganizationNode root = node("100", null);
        OrganizationNode child = node("101", "100");
        OrganizationNode sibling = node("102", "100");
        when(provider.resolve(List.of("101", "missing", "102"))).thenReturn(List.of(
                new OrganizationPath(child, List.of(root)),
                new OrganizationPath(sibling, List.of(root))));
        when(provider.parentOrgIdsWithChildren(List.of("101", "102"))).thenReturn(Set.of("101"));
        OrganizationResolveDTO request = new OrganizationResolveDTO();
        request.setOrgIds(List.of("101", "101", "missing", "102"));

        List<OrganizationLazyNodeVO> result = service.resolve(request);

        assertThat(result).extracting(OrganizationLazyNodeVO::getOrgId)
                .containsExactly("101", "102");
        assertThat(result).first().satisfies(item -> {
            assertThat(item.isHasChildren()).isTrue();
            assertThat(item.getChildren()).isEmpty();
        });

        verify(provider).resolve(List.of("101", "missing", "102"));
    }

    @Test
    void shouldNotQueryProviderWhenSearchKeywordIsBlank() {
        assertThat(service.search("  ")).isEmpty();
        verify(provider, never()).search(any(), org.mockito.ArgumentMatchers.anyInt(), any());
    }

    @Test
    void shouldOmitInactiveAndOutOfScopeNodesFromResolveAndSearch() {
        CurrentUserContext.set(new CurrentUserContext.UserInfo("user", "用户", "100", "单位", null, null));
        OrganizationNode allowed = node("101", "100");
        OrganizationNode inactive = node("102", "100");
        inactive.setState(0);
        OrganizationNode outside = node("201", "200");
        List<OrganizationPath> paths = List.of(
                new OrganizationPath(allowed, List.of()),
                new OrganizationPath(inactive, List.of()),
                new OrganizationPath(outside, List.of()));
        when(provider.resolve(List.of("101", "102", "201"))).thenReturn(paths);
        when(provider.search("单位", 50, "100")).thenReturn(paths);
        when(provider.isWithinScope("101", "100")).thenReturn(true);
        when(provider.isWithinScope("201", "100")).thenReturn(false);
        OrganizationResolveDTO request = new OrganizationResolveDTO();
        request.setOrgIds(List.of("101", "102", "201"));

        assertThat(service.resolve(request)).extracting(OrganizationLazyNodeVO::getOrgId)
                .containsExactly("101");
        assertThat(service.search(" 单位 ")).extracting(OrganizationLazyNodeVO::getOrgId)
                .containsExactly("101");
    }

    private OrganizationNode node(String orgId, String parentOrgId) {
        OrganizationNode node = new OrganizationNode();
        node.setOrgId(orgId);
        node.setOrgName("单位" + orgId);
        node.setParentOrgId(parentOrgId);
        node.setState(1);
        return node;
    }
}
