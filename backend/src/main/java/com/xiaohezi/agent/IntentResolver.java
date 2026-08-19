package com.xiaohezi.agent;

import static com.xiaohezi.agent.AgentModels.Intent;

public interface IntentResolver {
    IntentClassifier.Result classify(String input, Intent activeIntent);
}
