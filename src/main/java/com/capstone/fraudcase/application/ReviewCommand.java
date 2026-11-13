package com.capstone.fraudcase.application;

import com.capstone.fraudcase.domain.Disposition;
import com.capstone.fraudcase.domain.Route;

public record ReviewCommand(Disposition disposition, Route route, String reason, boolean missingInformationAcknowledged) {
}
