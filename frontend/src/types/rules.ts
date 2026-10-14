export type RuleCategory = "FRAUD" | "ATM" | "NETWORK";

export type RiskLevel =
  | "TYPIQUE"
  | "MOINS_CRITIQUE"
  | "MOYEN"
  | "CRITIQUE";

export interface MonitoringRule {
  id: number;
  code: string;
  name: string;
  description?: string;
  category: RuleCategory;
  riskLevel: RiskLevel;
  riskPercentage: number;
  enabled: boolean;
  notificationEnabled: boolean;
  soundEnabled: boolean;
  visualAlertEnabled: boolean;
  alertCount: number;
  alertPercentage: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MonitoringRuleRequest {
  code: string;
  name: string;
  description?: string;
  category: RuleCategory;
  riskLevel: RiskLevel;
  riskPercentage: number;
  enabled: boolean;
  notificationEnabled: boolean;
  soundEnabled: boolean;
  visualAlertEnabled: boolean;
}
