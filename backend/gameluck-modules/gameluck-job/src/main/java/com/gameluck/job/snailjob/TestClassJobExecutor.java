package com.gameluck.job.snailjob;

import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.job.core.executor.AbstractJobExecutor;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import com.gameluck.common.core.utils.MessageUtils;
import org.springframework.stereotype.Component;

/**
 * @author opensnail
 * @date 2024-05-17
 */
@Component
public class TestClassJobExecutor extends AbstractJobExecutor {

    @Override
    protected ExecuteResult doJobExecute(JobArgs jobArgs) {
        return ExecuteResult.success(MessageUtils.message("job.test.executor.success"));
    }
}
