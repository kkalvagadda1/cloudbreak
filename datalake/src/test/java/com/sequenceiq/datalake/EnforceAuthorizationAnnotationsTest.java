package com.sequenceiq.datalake;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.EnforceAuthorizationLogicsUtil;

public class EnforceAuthorizationAnnotationsTest {

    @Test
    public void testIfControllerClassHasProperAnnotation() {
        EnforceAuthorizationLogicsUtil.testIfControllerClassHasProperAnnotation();
    }

    @Test
    public void testIfControllerClassHasAuthorizationAnnotation() {
        EnforceAuthorizationLogicsUtil.testIfControllerClassHasAuthorizationAnnotation();
    }

    @Test
    public void testIfControllerMethodsHaveProperAuthorizationAnnotation() {
        EnforceAuthorizationLogicsUtil.testIfControllerMethodsHaveProperAuthorizationAnnotation();
    }
}
