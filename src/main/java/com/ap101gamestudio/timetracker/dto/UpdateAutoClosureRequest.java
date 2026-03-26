package com.ap101gamestudio.timetracker.dto;

import com.ap101gamestudio.timetracker.model.enums.ClosureCountType;
import com.ap101gamestudio.timetracker.model.enums.ClosurePendingStrategy;
import com.ap101gamestudio.timetracker.model.enums.ClosureShiftRule;

public record UpdateAutoClosureRequest(
        boolean autoClosureEnabled,
        Integer closureTargetDay,
        ClosureCountType closureCountType,
        ClosureShiftRule closureShiftRule,
        ClosurePendingStrategy closurePendingStrategy
) {}