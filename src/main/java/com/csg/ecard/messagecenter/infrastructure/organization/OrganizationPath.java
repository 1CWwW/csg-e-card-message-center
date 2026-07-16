package com.csg.ecard.messagecenter.infrastructure.organization;

import java.util.List;

/**
 * 组织节点及其从根节点开始的祖先路径。
 *
 * @param node      当前组织节点
 * @param ancestors 祖先节点，不包含当前节点
 */
public record OrganizationPath(OrganizationNode node, List<OrganizationNode> ancestors) {
}
