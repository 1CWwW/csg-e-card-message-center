package com.csg.ecard.messagecenter.module.template.service;

import com.csg.ecard.messagecenter.common.enums.ErrorCode;
import com.csg.ecard.messagecenter.common.exception.BizException;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneMapper;
import com.csg.ecard.messagecenter.module.scene.mapper.MsgSceneParamMapper;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyJsonValidator;
import com.csg.ecard.messagecenter.module.template.blockly.BlocklyRenderer;
import com.csg.ecard.messagecenter.module.template.entity.MsgTemplate;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateMapper;
import com.csg.ecard.messagecenter.module.template.mapper.MsgTemplateUnitMapper;
import com.csg.ecard.messagecenter.module.template.service.impl.MsgTemplateServiceImpl;
import org.apache.ibatis.annotations.Delete;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息模板删除事务与数据隔离测试。
 */
@ExtendWith(MockitoExtension.class)
class MsgTemplateDeleteTest {

    private static final Long TEMPLATE_ID = 2085629226374467586L;

    @Mock
    private MsgTemplateMapper msgTemplateMapper;
    @Mock
    private MsgTemplateUnitMapper msgTemplateUnitMapper;
    @Mock
    private MsgSceneMapper msgSceneMapper;
    @Mock
    private MsgSceneParamMapper msgSceneParamMapper;
    @Mock
    private BlocklyJsonValidator blocklyJsonValidator;
    @Mock
    private BlocklyRenderer blocklyRenderer;

    private MsgTemplateServiceImpl msgTemplateService;

    @BeforeEach
    void setUp() {
        msgTemplateService = new MsgTemplateServiceImpl(
                msgTemplateMapper,
                msgTemplateUnitMapper,
                msgSceneMapper,
                msgSceneParamMapper,
                blocklyJsonValidator,
                blocklyRenderer);
    }

    @Test
    void shouldDeleteTemplateAndUnitsButKeepHistoricalRecords() {
        AtomicBoolean templateExists = new AtomicBoolean(true);
        AtomicReference<List<String>> units =
                new AtomicReference<>(new ArrayList<>(List.of("unit-a", "unit-b")));
        List<Long> historicalRecordTemplateIds = new ArrayList<>(List.of(TEMPLATE_ID));
        stubMutableDelete(templateExists, units);

        msgTemplateService.delete(TEMPLATE_ID);

        assertThat(templateExists).isFalse();
        assertThat(units.get()).isEmpty();
        assertThat(historicalRecordTemplateIds).containsExactly(TEMPLATE_ID);
        InOrder order = org.mockito.Mockito.inOrder(
                msgTemplateMapper, msgTemplateUnitMapper, msgTemplateMapper);
        order.verify(msgTemplateMapper).selectById(TEMPLATE_ID);
        order.verify(msgTemplateUnitMapper).deleteByTemplateId(TEMPLATE_ID);
        order.verify(msgTemplateMapper).deleteById(TEMPLATE_ID);
    }

    @Test
    void shouldReturnClearBusinessErrorWhenTemplateDoesNotExist() {
        when(msgTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(null);

        assertThatThrownBy(() -> msgTemplateService.delete(TEMPLATE_ID))
                .isInstanceOf(BizException.class)
                .hasMessage("模板不存在")
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());

        verify(msgTemplateUnitMapper, never()).deleteByTemplateId(any());
        verify(msgTemplateMapper, never()).deleteById(any());
    }

    @Test
    void shouldNotReportSuccessWhenConcurrentDeleteWins() {
        when(msgTemplateMapper.selectById(TEMPLATE_ID)).thenReturn(template());
        when(msgTemplateUnitMapper.deleteByTemplateId(TEMPLATE_ID)).thenReturn(1);
        when(msgTemplateMapper.deleteById(TEMPLATE_ID)).thenReturn(0);

        assertThatThrownBy(() -> msgTemplateService.delete(TEMPLATE_ID))
                .isInstanceOf(BizException.class)
                .hasMessage("模板不存在或已被删除")
                .extracting("code")
                .isEqualTo(ErrorCode.DATA_NOT_FOUND.getCode());
    }

    @Test
    void unitDeleteSqlShouldBeParameterizedAndLimitedByTemplateId() throws Exception {
        Delete delete = MsgTemplateUnitMapper.class
                .getMethod("deleteByTemplateId", Long.class)
                .getAnnotation(Delete.class);
        String sql = String.join("\n", delete.value());

        assertThat(sql).contains("WHERE template_id = #{templateId}");
        assertThat(sql).doesNotContain("${templateId}");
    }

    @Test
    void shouldRollbackUnitDeletionWhenTemplateDeletionFails() {
        AtomicBoolean templateExists = new AtomicBoolean(true);
        AtomicReference<List<String>> units =
                new AtomicReference<>(new ArrayList<>(List.of("unit-a", "unit-b")));
        List<Long> historicalRecordTemplateIds = new ArrayList<>(List.of(TEMPLATE_ID));
        AtomicBoolean templateSnapshot = new AtomicBoolean();
        AtomicReference<List<String>> unitsSnapshot = new AtomicReference<>();

        when(msgTemplateMapper.selectById(TEMPLATE_ID))
                .thenAnswer(invocation -> templateExists.get() ? template() : null);
        when(msgTemplateUnitMapper.deleteByTemplateId(TEMPLATE_ID)).thenAnswer(invocation -> {
            int deleted = units.get().size();
            units.set(new ArrayList<>());
            return deleted;
        });
        when(msgTemplateMapper.deleteById(TEMPLATE_ID))
                .thenThrow(new IllegalStateException("template delete failed"));

        PlatformTransactionManager transactionManager =
                org.mockito.Mockito.mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = org.mockito.Mockito.mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any())).thenAnswer(invocation -> {
            templateSnapshot.set(templateExists.get());
            unitsSnapshot.set(new ArrayList<>(units.get()));
            return transactionStatus;
        });
        doAnswer(invocation -> {
            templateExists.set(templateSnapshot.get());
            units.set(new ArrayList<>(unitsSnapshot.get()));
            return null;
        }).when(transactionManager).rollback(transactionStatus);

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTarget(msgTemplateService);
        proxyFactory.setInterfaces(MsgTemplateService.class);
        proxyFactory.addAdvice(new TransactionInterceptor(
                transactionManager, new AnnotationTransactionAttributeSource()));
        MsgTemplateService transactionalService = (MsgTemplateService) proxyFactory.getProxy();

        assertThatThrownBy(() -> transactionalService.delete(TEMPLATE_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("template delete failed");

        assertThat(templateExists).isTrue();
        assertThat(units.get()).containsExactly("unit-a", "unit-b");
        assertThat(historicalRecordTemplateIds).containsExactly(TEMPLATE_ID);
        verify(transactionManager).rollback(transactionStatus);
        verify(transactionManager, never()).commit(any());
    }

    private void stubMutableDelete(AtomicBoolean templateExists,
                                   AtomicReference<List<String>> units) {
        when(msgTemplateMapper.selectById(TEMPLATE_ID))
                .thenAnswer(invocation -> templateExists.get() ? template() : null);
        when(msgTemplateUnitMapper.deleteByTemplateId(TEMPLATE_ID)).thenAnswer(invocation -> {
            int deleted = units.get().size();
            units.set(new ArrayList<>());
            return deleted;
        });
        when(msgTemplateMapper.deleteById(TEMPLATE_ID)).thenAnswer(invocation -> {
            if (!templateExists.compareAndSet(true, false)) {
                return 0;
            }
            return 1;
        });
    }

    private MsgTemplate template() {
        MsgTemplate template = new MsgTemplate();
        template.setId(TEMPLATE_ID);
        return template;
    }
}
