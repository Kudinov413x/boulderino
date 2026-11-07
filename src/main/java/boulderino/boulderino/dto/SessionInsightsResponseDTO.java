package boulderino.boulderino.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionInsightsResponseDTO {

    private Long sessionId;

    private SessionAttemptInsightDTO mostEfficientSend;
    private List<SessionAttemptInsightDTO> efficientSends;

    private SessionAttemptInsightDTO mostDifficultProject;
    private List<SessionAttemptInsightDTO> highGradeRoutes;

    private List<SessionPerformanceInsightDTO> performanceInsights;
    private List<BoulderAttributeInsightDTO> untoppedAttributeInsights;
    private List<GradeDistributionInsightDTO> gradeDistributionInsights;

    private List<NewHighlightDTO> newHighlights;
    private List<FormerWeaknessTopDTO> formerWeaknessTops;
}