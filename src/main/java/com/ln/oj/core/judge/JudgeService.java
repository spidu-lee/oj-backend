package com.ln.oj.core.judge;

import com.ln.oj.model.entity.QuestionSubmit;

/**
 * @author ln
 */
public interface JudgeService {

    QuestionSubmit doJudge(long questionSubmitId);

}
