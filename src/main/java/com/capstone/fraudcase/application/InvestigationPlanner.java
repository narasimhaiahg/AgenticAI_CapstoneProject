package com.capstone.fraudcase.application;

import com.capstone.fraudcase.agent.common.CaseFacts;
import com.capstone.fraudcase.tools.ToolNames;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Builds a bounded, deterministic evidence plan from the validated case type. */
@Component
public class InvestigationPlanner {

    public static final int MAX_STEPS = 8;

    public record PlannedStep(int order, String toolName, String purpose) {
    }

    public List<PlannedStep> plan(CaseFacts facts) {
        List<String[]> steps = new ArrayList<>();
        steps.add(new String[]{ToolNames.TRANSACTION, "Confirm the transaction exists and read status, channel and authentication outcome"});
        switch (facts.issueType()) {
            case MERCHANT_NOT_RECOGNISED, OTHER -> {
                steps.add(new String[]{ToolNames.DEVICE, "Check whether the device was new or trusted"});
                steps.add(new String[]{ToolNames.LOCATION, "Compare location with recent trusted activity"});
                steps.add(new String[]{ToolNames.MERCHANT, "Read the training merchant risk profile"});
                steps.add(new String[]{ToolNames.PRIOR_DISPUTES, "Check earlier similar claims"});
            }
            case GOODS_NOT_RECEIVED, CANCELLED_NOT_REFUNDED, INCORRECT_AMOUNT -> {
                steps.add(new String[]{ToolNames.MERCHANT, "Read the training merchant risk profile"});
                steps.add(new String[]{ToolNames.PRIOR_DISPUTES, "Check earlier disputes with this customer"});
            }
            case DUPLICATE_CHARGE -> steps.add(new String[]{ToolNames.PRIOR_DISPUTES, "Check earlier disputes with this customer"});
        }
        steps.add(new String[]{ToolNames.POLICY, "Retrieve applicable policy passages above the similarity threshold"});
        steps.add(new String[]{ToolNames.SLA, "Calculate a training SLA target for the recommended route"});
        List<PlannedStep> plan = new ArrayList<>();
        for (int i = 0; i < steps.size() && i < MAX_STEPS; i++) {
            plan.add(new PlannedStep(i + 1, steps.get(i)[0], steps.get(i)[1]));
        }
        return plan;
    }
}
