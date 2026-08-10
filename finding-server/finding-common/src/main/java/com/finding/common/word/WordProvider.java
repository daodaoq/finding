package com.finding.common.word;

import java.util.List;

/**
 * 违禁词来源提供者 —— 由具备数据库访问能力的模块实现。
 * <p>
 * 使 finding-common 中的 {@link SensitiveWordFilter} 无需依赖任何持久层即可拿到词表,
 * 具体实现(查 forbidden_word 表)放在 finding-framework 的 {@code ForbiddenWordServiceImpl}。
 */
@FunctionalInterface
public interface WordProvider {

    /** 返回当前启用的违禁词规则列表(词 + 动作:0=拦截, 1=送审)。 */
    List<SensitiveWordRule> enabledRules();
}
