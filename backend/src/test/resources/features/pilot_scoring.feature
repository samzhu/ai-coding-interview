Feature: Pilot Score is computed when an interview completes

  As a hiring manager
  I want every completed interview to be scored on Outcome and Pilot axes
  So I can decide hire / no hire from the dual-axis verdict at a glance

  Scenario: Zero-interaction interview is scored as PASSENGER without LLM call
    # 候選人開了 link 但什麼都沒做 — 不浪費 Gemini 成本，直接落地 PASSENGER
    Given a completed interview with no AI conversations and no checkpoints
    When the InterviewCompletedEvent is published
    Then within 30 seconds an InterviewScore row exists for that interview
    And the pilot_verdict is "PASSENGER"
    And the outcome_score is 0.0
    And the judge_error_reason is null

  Scenario: Driver-pattern interview is scored as DRIVER and hire-recommended
    # 高手駕駛 AI：具體 prompt、check 後 accept、跑測試、全 checkpoint 通過
    # → outcome=1.0 + pilot=DRIVER → 雙軸都高 → strong hire
    Given a completed interview with the candidate
    And the candidate sent 3 USER prompts
    And the interview has 3 PASSED checkpoints out of 3
    And Gemini Judge will return verdict "DRIVER" with pilot score 4.0 and headline "Strong driver — hire"
    When the InterviewCompletedEvent is published
    Then within 30 seconds an InterviewScore row exists for that interview
    And the pilot_verdict is "DRIVER"
    And the outcome_score is 1.0
    And the pilot_score is 4.0
    And the pilot_headline contains "hire"
    And the judge_error_reason is null

  Scenario: Passenger-pattern interview is scored as PASSENGER and no-hire
    # 被 AI 拖著走：vague prompt、無 review、checkpoint 全失敗
    # → outcome=0.0 + pilot=PASSENGER → 雙軸都低 → no hire
    Given a completed interview with the candidate
    And the candidate sent 4 USER prompts
    And the interview has 0 PASSED checkpoints out of 3
    And Gemini Judge will return verdict "PASSENGER" with pilot score 1.0 and headline "Passenger — no hire"
    When the InterviewCompletedEvent is published
    Then within 30 seconds an InterviewScore row exists for that interview
    And the pilot_verdict is "PASSENGER"
    And the outcome_score is 0.0
    And the pilot_score is 1.0

  Scenario: Mixed-pattern interview is scored as MIXED for human review
    # 混合：前期 driver 後期 passenger，2/3 checkpoint 通過
    # → outcome=0.67 + pilot=MIXED → 雙軸不一致 → 留給人工面試官判斷
    Given a completed interview with the candidate
    And the candidate sent 5 USER prompts
    And the interview has 2 PASSED checkpoints out of 3
    And Gemini Judge will return verdict "MIXED" with pilot score 2.5 and headline "Mixed — needs human review"
    When the InterviewCompletedEvent is published
    Then within 30 seconds an InterviewScore row exists for that interview
    And the pilot_verdict is "MIXED"
    And the pilot_score is 2.5

  Scenario: High outcome but Passenger pilot exposes the cheating-by-AI risk
    # 關鍵情境：所有 checkpoint 都過但 prompt/行為都是 passenger
    # 雙軸設計就是要捕捉這種 case — 單分數系統會誤判此人為 hire
    Given a completed interview with the candidate
    And the candidate sent 3 USER prompts
    And the interview has 3 PASSED checkpoints out of 3
    And Gemini Judge will return verdict "PASSENGER" with pilot score 1.5 and headline "Outcome high but pure passenger — no hire"
    When the InterviewCompletedEvent is published
    Then within 30 seconds an InterviewScore row exists for that interview
    And the outcome_score is 1.0
    And the pilot_verdict is "PASSENGER"
    And the pilot_score is 1.5

  Scenario: Gemini judge failure preserves outcome and records error reason
    # Resilience：LLM 呼叫失敗（timeout/quota/JSON parse 失敗）不應讓整個評分失敗
    # outcome 仍寫入，pilot 欄位 null，error reason 留給管理員人工 retry
    Given a completed interview with the candidate
    And the candidate sent 2 USER prompts
    And the interview has 2 PASSED checkpoints out of 3
    And Gemini Judge will throw "Gemini API timeout after 3 retries"
    When the InterviewCompletedEvent is published
    Then within 30 seconds an InterviewScore row exists for that interview
    And the outcome_score is 0.67
    And the pilot_score is null
    And the judge_error_reason contains "Gemini API timeout"
