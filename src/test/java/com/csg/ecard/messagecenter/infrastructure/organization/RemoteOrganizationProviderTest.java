package com.csg.ecard.messagecenter.infrastructure.organization;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.codec.DecodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class RemoteOrganizationProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JadpOrganizationClient client;
    private OrganizationProperties properties;
    private RemoteOrganizationProvider provider;

    @BeforeEach
    void setUp() {
        client = mock(JadpOrganizationClient.class);
        properties = new OrganizationProperties();
        provider = new RemoteOrganizationProvider(client, properties,
                mock(ApplicationEventPublisher.class));
    }

    @Test
    void shouldAllowJdkProxyToReturnUnitTreeDto() throws Exception {
        List<RemoteOrganizationDTO> root = readUnitTree("[{\"orgId\":\"1\",\"state\":1}]");
        // 保留真实 JDK 代理调用回归，验证新的数组返回契约。
        JadpOrganizationClient proxy = (JadpOrganizationClient) Proxy.newProxyInstance(
                JadpOrganizationClient.class.getClassLoader(),
                new Class<?>[]{JadpOrganizationClient.class},
                (instance, method, args) -> {
                    if ("queryCorpTree".equals(method.getName())) {
                        assertThat(args[0]).isEqualTo("1");
                        return root;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        RemoteOrganizationProvider proxyProvider = new RemoteOrganizationProvider(
                proxy, properties, mock(ApplicationEventPublisher.class));

        assertThat(proxyProvider.children(null)).singleElement()
                .extracting(OrganizationNode::getOrgId).isEqualTo("1");
        assertThat(proxyProvider.resolve(List.of("1"))).singleElement()
                .extracting(path -> path.node().getOrgId()).isEqualTo("1");
    }

    @Test
    void shouldUseFlatUnitParentsAndReuseOneSnapshotForDisplayQueries() throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":"3","orgName":"乙单位","orgCode":"UNIT-B","parentOrgId":"1",
                   "sortNo":2,"state":1,"children":[]},
                  {"orgId":"1","orgName":"根单位","parentOrgId":"0","state":1,"children":[]},
                  {"orgId":"2","orgName":"甲单位","orgCode":"UNIT-A","parentOrgId":"1",
                   "sortNo":1,"state":1,"children":[]},
                  {"orgId":"4","orgName":"子单位","parentOrgId":"2","state":1,"children":[]}]
                """));

        assertThat(provider.children(null)).singleElement().satisfies(root -> {
            assertThat(root.getOrgId()).isEqualTo("1");
            assertThat(root.getParentOrgId()).isNull();
            assertThat(root.getChildren()).isEmpty();
        });
        assertThat(provider.children("1")).extracting(OrganizationNode::getOrgId).containsExactly("2", "3");
        assertThat(provider.children("2")).singleElement().satisfies(child -> {
            assertThat(child.getOrgId()).isEqualTo("4");
            assertThat(child.getParentOrgId()).isEqualTo("2");
        });
        assertThat(provider.parentOrgIdsWithChildren(List.of("1", "2", "3", "4")))
                .containsExactlyInAnyOrder("1", "2");
        assertThat(provider.resolve(List.of("4", "4", "missing"))).singleElement().satisfies(path ->
                assertThat(path.ancestors()).extracting(OrganizationNode::getOrgId).containsExactly("1", "2"));
        assertThat(provider.search("UNIT-A", 50, null)).singleElement()
                .extracting(path -> path.node().getOrgId()).isEqualTo("2");
        assertThat(provider.search("子单位", 50, "2")).hasSize(1);
        assertThat(provider.search("乙单位", 50, "2")).isEmpty();
        assertThat(provider.tree()).singleElement().satisfies(root ->
                assertThat(root.getChildren()).extracting(OrganizationNode::getOrgId).containsExactly("2", "3"));
        verify(client).queryCorpTree("1");
        verify(client, never()).queryAll();
    }

    @Test
    void shouldKeepLargeNumericIdsAsExactStrings() throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":1,"state":1},
                  {"orgId":2085629226374467586,"parentOrgId":1,"state":1}]
                """));

        List<OrganizationNode> children = provider.children("1");

        assertThat(children).singleElement().extracting(OrganizationNode::getOrgId)
                .isEqualTo("2085629226374467586");
        assertThat(objectMapper.valueToTree(children).path(0).path("orgId").isTextual()).isTrue();
    }

    @Test
    void shouldHideInactiveAndUnknownStateNodesAndNotReintroduceDepartments() throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":"1","state":1},
                  {"orgId":"2","parentOrgId":"1","orgName":"停用单位","state":0},
                  {"orgId":"3","parentOrgId":"1","orgName":"未知状态单位"}]
                """));

        assertThat(provider.children("1")).isEmpty();
        assertThat(provider.search("单位", 50, null)).isEmpty();
        assertThat(provider.parentOrgIdsWithChildren(List.of("1"))).isEmpty();
        assertThat(provider.resolve(List.of("department", "missing"))).isEmpty();
        verify(client, never()).queryAll();
    }

    @Test
    void shouldStopRepeatedIdsAndNormalizeRootParent() throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":"1","parentOrgId":"1","state":1},
                  {"orgId":"2","parentOrgId":"1","state":1},
                  {"orgId":"2","parentOrgId":"1","state":1}]
                """));

        assertThat(provider.tree()).singleElement().satisfies(root ->
                assertThat(root.getChildren()).singleElement().satisfies(child -> {
                    assertThat(child.getOrgId()).isEqualTo("2");
                    assertThat(child.getChildren()).isEmpty();
                }));
    }

    @Test
    void shouldIgnoreUnknownFieldsWithoutLoggingResponseDetails(CapturedOutput output) throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":1,"state":1,"accessToken":"DO-NOT-LOG-ROOT-TOKEN"},
                  {"orgId":"2","parentOrgId":"1","state":1,"employeeInfo":"DO-NOT-LOG-PERSONAL-DATA"}]
                """));

        assertThat(provider.children(null)).singleElement().extracting(OrganizationNode::getOrgId).isEqualTo("1");
        assertThat(provider.children("1")).singleElement().extracting(OrganizationNode::getOrgId).isEqualTo("2");

        assertThat(output.getOut()).doesNotContain("request started", "response received", "response structure",
                "firstChildFields", "rootSample", "nodeCount=2", "DO-NOT-LOG-ROOT-TOKEN", "DO-NOT-LOG-PERSONAL-DATA");
    }

    @Test
    void shouldRejectUnexpectedEnvelopeWithoutCachingOrFallingBack() throws Exception {
        assertThatThrownBy(() -> readUnitTree("""
                {"code":0,"result":[{"orgId":"1","state":1}]}
                """)).isInstanceOf(JsonProcessingException.class);
        when(client.queryCorpTree("1")).thenThrow(mock(DecodeException.class)).thenReturn(readUnitTree("""
                [{"orgId":"1","state":1,"children":[]}]
                """));

        assertThatThrownBy(() -> provider.children(null)).isInstanceOf(BizException.class)
                .hasMessage("单位树服务调用失败");
        assertThat(provider.children(null)).hasSize(1);
        verify(client, times(2)).queryCorpTree("1");
        verify(client, never()).queryAll();
    }

    @Test
    void shouldRejectNullEmptyObjectAndWrongRootResponses() throws Exception {
        assertThatThrownBy(() -> readUnitTree("{\"orgId\":\"1\"}"))
                .isInstanceOf(JsonProcessingException.class);
        // 非数组响应由 Feign 的 JSON 解码器拒绝，Provider 仍转换为统一业务异常。
        when(client.queryCorpTree("1")).thenReturn(null)
                .thenReturn(List.of())
                .thenThrow(mock(DecodeException.class))
                .thenReturn(readUnitTree("[{\"orgId\":\"999\",\"state\":1}]"));

        assertThatThrownBy(() -> provider.children(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> provider.children(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> provider.children(null)).isInstanceOf(BizException.class);
        assertThatThrownBy(() -> provider.children(null)).isInstanceOf(BizException.class)
                .hasMessageContaining("根单位ID不匹配");
    }

    @Test
    void shouldTranslateFeignFailureWithoutLeakingResponseBody(CapturedOutput output) {
        FeignException error = mock(FeignException.class);
        when(error.status()).thenReturn(503);
        when(error.getMessage()).thenReturn("DO-NOT-LOG-ERROR-BODY");
        when(client.queryCorpTree("1")).thenThrow(error);

        assertThatThrownBy(() -> provider.children(null)).isInstanceOf(BizException.class)
                .hasMessage("单位树服务调用失败")
                .satisfies(ex -> assertThat(((BizException) ex).getCode())
                        .isEqualTo(ErrorCode.EXTERNAL_SERVICE_ERROR.getCode()));
        assertThat(output.getOut()).contains("status=503").doesNotContain("DO-NOT-LOG-ERROR-BODY");
    }

    @Test
    void shouldPreserveLegacyDepartmentPathAndIsolateItFromUnitDisplay() throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":"1","state":1},{"orgId":"2","parentOrgId":"1","state":1}]
                """));
        RemoteOrganizationDTO root = objectMapper.readValue("{\"orgId\":\"1\"}", RemoteOrganizationDTO.class);
        RemoteOrganizationDTO unit = objectMapper.readValue(
                "{\"orgId\":\"2\",\"parentOrgId\":\"1\"}", RemoteOrganizationDTO.class);
        RemoteOrganizationDTO department = objectMapper.readValue(
                "{\"orgId\":\"department\",\"parentOrgId\":\"2\"}", RemoteOrganizationDTO.class);
        when(client.queryAll()).thenReturn(List.of(root, unit, department));

        assertThat(provider.resolveUnitPath("department")).containsExactly("department", "2", "1");
        assertThat(provider.resolveUnitPath("department")).containsExactly("department", "2", "1");
        assertThat(provider.children("1")).extracting(OrganizationNode::getOrgId).containsExactly("2");
        assertThat(provider.children("2")).isEmpty();
        assertThat(provider.resolve(List.of("department"))).isEmpty();
        verify(client).queryAll();
        verify(client).queryCorpTree("1");
    }

    @Test
    void shouldSupportConfiguredRootAndDisabledCache(CapturedOutput output) throws Exception {
        properties.getRemote().setRootOrgId("root-2");
        properties.getRemote().setCacheTtl(Duration.ZERO);
        when(client.queryCorpTree("root-2")).thenReturn(readUnitTree(
                "[{\"orgId\":\"root-2\",\"parentOrgId\":\"1\",\"state\":1}]"));

        assertThat(provider.children(null)).singleElement().extracting(OrganizationNode::getOrgId).isEqualTo("root-2");
        provider.children(null);

        verify(client, times(2)).queryCorpTree("root-2");
        assertThat(output.getOut()).doesNotContain("request started", "response structure");
    }

    @Test
    void shouldKeepUnitsUnderMultipleVirtualGroups() throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":"unit","parentOrgId":"virtual-2","orgName":"下属单位","state":1,"children":[]},
                 {"orgId":"virtual-2","parentOrgId":"virtual-1","state":1,"children":[]},
                 {"orgId":"1","parentOrgId":"0","state":1,"children":[]},
                 {"orgId":"virtual-1","parentOrgId":"1","state":1,"children":[]}]
                """));

        assertThat(provider.children(null)).extracting(OrganizationNode::getOrgId).containsExactly("1");
        assertThat(provider.children("1")).extracting(OrganizationNode::getOrgId).containsExactly("virtual-1");
        assertThat(provider.children("virtual-1")).extracting(OrganizationNode::getOrgId)
                .containsExactly("virtual-2");
        assertThat(provider.children("virtual-2")).singleElement().satisfies(node -> {
            assertThat(node.getOrgId()).isEqualTo("unit");
            assertThat(node.getChildren()).isEmpty();
        });
        assertThat(provider.parentOrgIdsWithChildren(List.of("1", "virtual-1", "virtual-2", "unit")))
                .containsExactlyInAnyOrder("1", "virtual-1", "virtual-2");
        assertThat(provider.resolve(List.of("unit", "unit", "missing"))).singleElement().satisfies(path ->
                assertThat(path.ancestors()).extracting(OrganizationNode::getOrgId)
                        .containsExactly("1", "virtual-1", "virtual-2"));
        assertThat(provider.search("下属单位", 50, "virtual-1")).hasSize(1);
        assertThat(provider.search("下属单位", 50, "other-unit")).isEmpty();
        assertThat(provider.tree().get(0).getChildren().get(0).getChildren().get(0).getChildren())
                .extracting(OrganizationNode::getOrgId).containsExactly("unit");
        verify(client).queryCorpTree("1");
        verify(client, never()).queryAll();
    }

    @Test
    void shouldRejectCyclicOrDisconnectedFlatListsWithoutCaching() throws Exception {
        when(client.queryCorpTree("1")).thenReturn(readUnitTree("""
                [{"orgId":"1","state":1},
                 {"orgId":"2","parentOrgId":"3","state":1},
                 {"orgId":"3","parentOrgId":"2","state":1}]
                """), readUnitTree("""
                [{"orgId":"1","state":1},{"orgId":"2","parentOrgId":"missing","state":1}]
                """), readUnitTree("""
                [{"orgId":"1","state":1},{"orgId":"2","parentOrgId":"2","state":1}]
                """), readUnitTree("""
                [{"orgId":"1","state":1},{"parentOrgId":"1","state":1}]
                """), readUnitTree("""
                [{"orgId":"1","state":1},{"orgId":"2","parentOrgId":"1","state":1}]
                """));

        assertThatThrownBy(() -> provider.tree()).isInstanceOf(BizException.class).hasMessageContaining("循环");
        assertThatThrownBy(() -> provider.tree()).isInstanceOf(BizException.class).hasMessageContaining("缺失的父节点");
        assertThatThrownBy(() -> provider.tree()).isInstanceOf(BizException.class).hasMessageContaining("循环");
        assertThatThrownBy(() -> provider.tree()).isInstanceOf(BizException.class).hasMessageContaining("缺少orgId");
        assertThat(provider.children("1")).extracting(OrganizationNode::getOrgId).containsExactly("2");
        verify(client, times(5)).queryCorpTree("1");
        verify(client, never()).queryAll();
    }

    @Test
    void shouldFailBeforeCallingFeignWhenRootIsNotConfigured() {
        properties.getRemote().setRootOrgId(" ");

        assertThatThrownBy(() -> provider.children(null)).isInstanceOf(BizException.class)
                .hasMessage("未配置单位树根单位ID");
        verifyNoInteractions(client);
    }

    private List<RemoteOrganizationDTO> readUnitTree(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, new TypeReference<List<RemoteOrganizationDTO>>() { });
    }
}
