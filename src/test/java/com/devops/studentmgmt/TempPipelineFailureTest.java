package com.devops.studentmgmt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TEMPORARY - used only to prove the Jenkins pipeline stops before deploying
 * when a test fails. Deleted immediately after the demonstration.
 */
class TempPipelineFailureTest {

    @Test
    @DisplayName("Intentional failure to verify the pipeline halts before deployment")
    void pipelineShouldNotDeployWhenThisFails() {
        assertThat("pipeline gate").isEqualTo("must block deployment");
    }
}
