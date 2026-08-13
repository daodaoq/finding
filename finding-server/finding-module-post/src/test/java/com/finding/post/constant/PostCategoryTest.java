package com.finding.post.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostCategoryTest {

    @Test
    void descOf_knownCode_returnsChinese() {
        assertEquals("学习交流", PostCategory.descOf("study"));
        assertEquals("失物招领", PostCategory.descOf("lostfound"));
        assertEquals("其他", PostCategory.descOf("other"));
    }

    @Test
    void descOf_unknownOrNull_returnsEmpty() {
        assertEquals("", PostCategory.descOf("nope"));
        assertEquals("", PostCategory.descOf(null));
    }

    @Test
    void supported_containsAllCodes_andRejectsUnknown() {
        for (PostCategory c : PostCategory.values()) {
            assertTrue(PostCategory.SUPPORTED.contains(c.getCode()));
        }
        assertFalse(PostCategory.SUPPORTED.contains("nope"));
    }
}
