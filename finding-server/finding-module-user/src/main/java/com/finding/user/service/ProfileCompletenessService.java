package com.finding.user.service;

import com.finding.user.vo.ProfileCompletenessVO;

/** 资料完整度 —— 用户端自我引导 */
public interface ProfileCompletenessService {

    ProfileCompletenessVO completeness(Long userId);
}
