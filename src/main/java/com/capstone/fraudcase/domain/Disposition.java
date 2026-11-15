package com.capstone.fraudcase.domain;

public enum Disposition {
    /** Accept the recommended route. */
    ACCEPT,
    /** Revise the recommendation by routing to a different queue. */
    REROUTE,
    /** Escalate to fraud investigation with raised priority. */
    ESCALATE,
    /** Send back for more information / re-enrichment. */
    REQUEST_INFORMATION,
    /** Reject the recommendation and close without routing (lead only). */
    REJECT_AND_CLOSE,
    /** Close a routed or incomplete case. */
    CLOSE
}
