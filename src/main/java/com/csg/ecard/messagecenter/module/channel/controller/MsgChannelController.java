package com.csg.ecard.messagecenter.module.channel.controller;

import com.csg.ecard.messagecenter.common.page.PageResult;
import com.csg.ecard.messagecenter.common.result.CommonResult;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelCreateDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelPageQueryDTO;
import com.csg.ecard.messagecenter.module.channel.dto.ChannelUpdateDTO;
import com.csg.ecard.messagecenter.module.channel.service.MsgChannelService;
import com.csg.ecard.messagecenter.module.channel.vo.MsgChannelVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息渠道管理接口。
 */
@Validated
@RestController
@RequiredArgsConstructor
@Tag(name = "消息渠道管理")
@RequestMapping("/api/msg/channel")
public class MsgChannelController {

    private final MsgChannelService msgChannelService;

    /**
     * 分页查询渠道。
     *
     * @param query 查询条件
     * @return 渠道分页结果
     */
    @GetMapping("/list")
    @Operation(summary = "渠道分页查询")
    public CommonResult<PageResult<MsgChannelVO>> list(@ParameterObject @Valid ChannelPageQueryDTO query) {
        return CommonResult.success(msgChannelService.page(query));
    }

    /**
     * 查询渠道详情。
     *
     * @param id 渠道ID
     * @return 渠道详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "渠道详情查询")
    public CommonResult<MsgChannelVO> detail(@PathVariable Long id) {
        return CommonResult.success(msgChannelService.detail(id));
    }

    /**
     * 新增渠道。
     *
     * @param request 新增请求
     * @return 新增后的渠道
     */
    @PostMapping
    @Operation(summary = "新增渠道")
    public CommonResult<MsgChannelVO> create(@RequestBody @Valid ChannelCreateDTO request) {
        return CommonResult.success(msgChannelService.create(request));
    }

    /**
     * 编辑渠道。
     *
     * @param id      渠道ID
     * @param request 编辑请求
     * @return 编辑后的渠道
     */
    @PutMapping("/{id}")
    @Operation(summary = "编辑渠道")
    public CommonResult<MsgChannelVO> update(@PathVariable Long id, @RequestBody @Valid ChannelUpdateDTO request) {
        return CommonResult.success(msgChannelService.update(id, request));
    }

    /**
     * 删除渠道。
     *
     * @param id 渠道ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除渠道")
    public CommonResult<Void> delete(@PathVariable Long id) {
        msgChannelService.delete(id);
        return CommonResult.success();
    }

    /**
     * 启停渠道。
     *
     * @param id 渠道ID
     * @return 切换后的渠道
     */
    @PutMapping("/{id}/toggle")
    @Operation(summary = "渠道启停切换")
    public CommonResult<MsgChannelVO> toggle(@PathVariable Long id) {
        return CommonResult.success(msgChannelService.toggle(id));
    }
}
